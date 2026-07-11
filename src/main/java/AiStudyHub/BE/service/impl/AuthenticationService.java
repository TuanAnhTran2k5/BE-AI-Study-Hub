package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.dto.Request.ForgotPasswordRequest;
import AiStudyHub.BE.dto.Request.GoogleLoginRequest;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.LogoutRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.ResendOtpRequest;
import AiStudyHub.BE.dto.Request.ResetPasswordRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Request.VerifyTokenRequest;
import AiStudyHub.BE.dto.Response.ForgotPasswordResponse;
import AiStudyHub.BE.dto.Response.GoogleUserInfo;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.ResendOtpResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.OtpMapper;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.IAuthentication;
import AiStudyHub.BE.service.IOtpService;
import AiStudyHub.BE.service.IToken;
import AiStudyHub.BE.service.IUser;
import AiStudyHub.BE.utils.OtpUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AuthenticationService implements UserDetailsService, IAuthentication {

    final UserRepo userRepo;
    final UserMapper userMapper;
    final OtpMapper otpMapper;
    final PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    AuthenticationManager authenticationManager;

    final IToken tokenService;
    final IUser userService;
    final IOtpService otpService;
    final OtpUtil otpUtil;
    final WebClient webClient = WebClient.create();


    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        Optional<User> existingUserOpt = userRepo.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new GlobalException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            if (user.getStatus() == UserStatus.BANNED) {
                throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
            }
            // Update existing PENDING user
            user.setFullName(registerRequest.getFullName());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user = userRepo.save(user);
        } else {
            // Use mapper for Request → Entity, then set encoded password and authProvider manually
            user = userMapper.toUser(registerRequest);
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setAuthProvider(AuthProvider.LOCAL);
            user = userRepo.save(user);
        }

        // Delegate OTP creation, invalidation, and email sending to OtpService
        OtpVerification otpVerification = otpService.generateAndSendOtp(user, email, OtpPurpose.REGISTER);

        RegisterResponse response = userMapper.toRegisterResponse(user);
        response.setOtpExpiredAt(otpVerification.getExpiredAt());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            if (user.getStatus() == UserStatus.PENDING) {
                throw new GlobalException(ErrorCode.EMAIL_NOT_VERIFIED);
            }

            String accessToken = tokenService.generateAccessToken(user);

            return userService.buildUserProfileResponse(user, accessToken);

        } catch (LockedException exception) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        } catch (BadCredentialsException exception) {
            throw new GlobalException(ErrorCode.INVALID_CREDENTIALS);
        } catch (UsernameNotFoundException exception) {
            log.info(exception.getMessage());
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public UserResponse verifyEmail(VerifyOtpRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        otpService.verifyAndConsumeOtp(user, request.getOtpCode(), OtpPurpose.REGISTER);

        user.setStatus(UserStatus.ACTIVE);
        user = userRepo.save(user);

        String accessToken = tokenService.generateAccessToken(user);
        return userService.buildUserProfileResponse(user, accessToken);
    }

    @Override
    @Transactional
    public UserResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = null;
        String token = request.getToken();

        boolean isJwtIdToken = otpUtil.isJwtToken(token);

        if (isJwtIdToken) {
            try {
                googleUserInfo = webClient.get()
                        .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + token)
                        .retrieve()
                        .bodyToMono(GoogleUserInfo.class)
                        .block();
            } catch (Exception e) {
                log.debug("ID token verification failed, attempting userinfo endpoint with bearer token: {}", e.getMessage());
            }
        }

        if (googleUserInfo == null) {
            try {
                googleUserInfo = webClient.get()
                        .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(GoogleUserInfo.class)
                        .block();
            } catch (Exception ex) {
                if (!isJwtIdToken) {
                    try {
                        googleUserInfo = webClient.get()
                                .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + token)
                                .retrieve()
                                .bodyToMono(GoogleUserInfo.class)
                                .block();
                    } catch (Exception ignored) {
                    }
                }
                if (googleUserInfo == null) {
                    log.error("Error verifying Google token (both ID token and Access token endpoints failed): {}", ex.getMessage());
                    throw new GlobalException(ErrorCode.UNAUTHENTICATED);
                }
            }
        }

        if (googleUserInfo == null || googleUserInfo.getEmail() == null) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED);
        }

        final GoogleUserInfo finalUserInfo = googleUserInfo;

        User user = userRepo.findByEmail(finalUserInfo.getEmail())
                .map(existingUser -> {
                    existingUser.setGoogleId(finalUserInfo.getSub());

                    if (existingUser.getStatus() == UserStatus.PENDING) {
                        existingUser.setStatus(UserStatus.ACTIVE);
                    }

                    if(existingUser.getStatus() == UserStatus.BANNED){
                        throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
                    }

                    if (existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank() || existingUser.getAvatarUrl().contains("googleusercontent.com")) {
                        if (finalUserInfo.getPicture() != null && !finalUserInfo.getPicture().isBlank()) {
                            existingUser.setAvatarUrl(finalUserInfo.getPicture());
                        }
                    }

                    return userRepo.save(existingUser);
                })
                .orElseGet(() -> {
                    // Use mapper for GoogleUserInfo → Entity, then set Service-specific fields
                    User newUser = userMapper.toUser(finalUserInfo);
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setStatus(UserStatus.ACTIVE);
                    return userRepo.save(newUser);
                });

        String accessToken = tokenService.generateAccessToken(user);

        return userService.buildUserProfileResponse(user, accessToken);
    }

    @Override
    public UserResponse verifyToken(VerifyTokenRequest request) {
        User user = tokenService.verifyAccessToken(request.getToken());

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        return userService.buildUserProfileResponse(user, request.getToken());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public boolean logout(LogoutRequest request) {
        // Do nothing on backend for stateless JWT.
        // Frontend is responsible for discarding the token.
        return true;
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        OtpVerification otpVerification = otpService.generateAndSendOtp(user, email, OtpPurpose.FORGOT_PASSWORD);

        return otpMapper.toForgotPasswordResponse(otpVerification);
    }

    @Override
    @Transactional
    public boolean verifyForgotPasswordOtp(VerifyOtpRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        otpService.verifyOtpWithoutConsume(user, request.getOtpCode(), OtpPurpose.FORGOT_PASSWORD);

        return true;
    }

    @Override
    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        // Verify and consume OTP
        otpService.verifyAndConsumeOtp(user, request.getOtpCode(), OtpPurpose.FORGOT_PASSWORD);

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepo.save(user);

        return true;
    }

    @Override
    @Transactional
    public ResendOtpResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        OtpPurpose purpose = request.getPurpose();

        if (purpose == OtpPurpose.REGISTER && user.getStatus() != UserStatus.PENDING) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        OtpVerification otpVerification = otpService.generateAndSendOtp(user, email, purpose);

        return otpMapper.toResendOtpResponse(otpVerification);
    }
}
