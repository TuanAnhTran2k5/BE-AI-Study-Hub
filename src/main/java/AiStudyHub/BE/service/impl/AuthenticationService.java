package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.dto.Request.ForgotPasswordRequest;
import AiStudyHub.BE.dto.Request.ResetPasswordRequest;
import AiStudyHub.BE.dto.Request.ResendOtpRequest;
import AiStudyHub.BE.dto.Response.ForgotPasswordResponse;
import AiStudyHub.BE.dto.Response.ResendOtpResponse;
import AiStudyHub.BE.dto.Request.GoogleLoginRequest;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.LogoutRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Request.VerifyTokenRequest;
import AiStudyHub.BE.dto.Response.GoogleUserInfo;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.mapper.OtpMapper;
import AiStudyHub.BE.repository.OtpVerificationRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.IAuthentication;
import AiStudyHub.BE.service.IEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AuthenticationService implements UserDetailsService, IAuthentication {

    private SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OtpMapper otpMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private AiStudyHub.BE.service.IToken tokenService;
    @Autowired
    private OtpVerificationRepo otpVerificationRepo;
    @Autowired
    private IEmail emailService;



    @Override
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
            // Use mapper to convert Request -> Entity
            user = userMapper.toUser(registerRequest);
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setStatus(UserStatus.PENDING);
            user.setAuthProvider(AuthProvider.LOCAL);
            user.setRole(UserRole.US);
            user = userRepo.save(user);
        }

        // Invalidate any previously issued, still-unused REGISTER OTPs for this user
        // so only the newest code can be used.
        List<OtpVerification> oldOtps =
                otpVerificationRepo.findByUserAndPurposeAndIsUseFalse(user, OtpPurpose.REGISTER);
        if (!oldOtps.isEmpty()) {
            oldOtps.forEach(o -> o.setIsUse(true));
            otpVerificationRepo.saveAll(oldOtps);
        }

        // Generate a cryptographically strong 6-digit OTP code
        String otpCode = String.valueOf(secureRandom.nextInt(900000) + 100000);

        // Save OtpVerification to DB
        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .purpose(OtpPurpose.REGISTER)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUse(false)
                .build();
        otpVerificationRepo.save(otpVerification);

        // Send OTP via email
        emailService.sendOtpEmail(email, otpCode);

        // Return info the FE needs (e.g. email to pre-fill the form, OTP expiry time for countdown)
        return RegisterResponse.builder()
                .email(email)
                .otpExpiredAt(otpVerification.getExpiredAt())
                .status(user.getStatus())
                .build();
    }

    @Override
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
            if (user.getStatus() == UserStatus.BANNED) {
                throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
            }

            String accessToken = tokenService.generateAccessToken(user);

            // Verify the freshly issued access token (signature, expiry, subject, user exists)
            // so we never hand the client a token that wouldn't pass authentication.
            tokenService.verifyAccessToken(accessToken);

            UserResponse userResponse = userMapper.toUserResponse(user);
            userResponse.setAccessToken(accessToken);
            userResponse.setRole(user.getRole());

            return userResponse;

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

        OtpVerification otp = otpVerificationRepo
                .findByUserAndOtpCodeAndPurposeAndIsUseFalse(
                        user,
                        request.getOtpCode(),
                        OtpPurpose.REGISTER
                )
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }

        otp.setIsUse(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepo.save(otp);

        user.setStatus(UserStatus.ACTIVE);
        userRepo.save(user);

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse googleLogin(GoogleLoginRequest request) {
        WebClient webClient = WebClient.create();
        GoogleUserInfo googleUserInfo = null;
        String token = request.getToken();

        boolean isJwtIdToken = token != null && token.startsWith("ey") && token.split("\\.").length == 3;

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
                    
                    if (existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank() || existingUser.getAvatarUrl().contains("googleusercontent.com")) {
                        if (finalUserInfo.getPicture() != null && !finalUserInfo.getPicture().isBlank()) {
                            existingUser.setAvatarUrl(finalUserInfo.getPicture());
                        }
                    }
                    
                    return userRepo.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = userMapper.toUser(finalUserInfo);
                    if (newUser.getAvatarUrl() == null || newUser.getAvatarUrl().isBlank()) {
                        newUser.setAvatarUrl(finalUserInfo.getPicture());
                    }
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setRole(UserRole.US);
                    newUser.setStatus(UserStatus.ACTIVE);
                    return userRepo.save(newUser);
                });

        String accessToken = tokenService.generateAccessToken(user);

        // Verify the freshly issued access token (signature, expiry, subject, user exists)
        // so we never hand the client a token that wouldn't pass authentication.
        tokenService.verifyAccessToken(accessToken);

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setAccessToken(accessToken);
        userResponse.setRole(user.getRole());

        return userResponse;
    }

    @Override
    public UserResponse verifyToken(VerifyTokenRequest request) {
        User user = tokenService.verifyAccessToken(request.getToken());

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setAccessToken(request.getToken());
        userResponse.setRole(user.getRole());
        return userResponse;
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
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        // Invalidate old OTPs
        List<OtpVerification> oldOtps =
                otpVerificationRepo.findByUserAndPurposeAndIsUseFalse(user, OtpPurpose.FORGOT_PASSWORD);
        if (!oldOtps.isEmpty()) {
            oldOtps.forEach(o -> o.setIsUse(true));
            otpVerificationRepo.saveAll(oldOtps);
        }

        String otpCode = String.valueOf(secureRandom.nextInt(900000) + 100000);

        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUse(false)
                .build();
        otpVerificationRepo.save(otpVerification);

        emailService.sendOtpEmail(email, otpCode);

        return otpMapper.toForgotPasswordResponse(otpVerification);
    }

    @Override
    public boolean verifyForgotPasswordOtp(VerifyOtpRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        OtpVerification otp = otpVerificationRepo
                .findByUserAndOtpCodeAndPurposeAndIsUseFalse(
                        user,
                        request.getOtpCode(),
                        OtpPurpose.FORGOT_PASSWORD
                )
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }

        otp.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepo.save(otp);

        return true;
    }

    @Override
    public boolean resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }

        OtpVerification otp = otpVerificationRepo
                .findFirstByUserAndPurposeAndIsUseFalseAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                        user,
                        OtpPurpose.FORGOT_PASSWORD
                )
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));

        if (otp.getVerifiedAt().plusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepo.save(user);

        // Mark OTP as used
        otp.setIsUse(true);
        otpVerificationRepo.save(otp);
        
        return true;
    }

    @Override
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

        // Invalidate old OTPs for the requested purpose
        List<OtpVerification> oldOtps =
                otpVerificationRepo.findByUserAndPurposeAndIsUseFalse(user, purpose);
        if (!oldOtps.isEmpty()) {
            oldOtps.forEach(o -> o.setIsUse(true));
            otpVerificationRepo.saveAll(oldOtps);
        }

        String otpCode = String.valueOf(secureRandom.nextInt(900000) + 100000);

        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUse(false)
                .build();
        otpVerificationRepo.save(otpVerification);

        emailService.sendOtpEmail(email, otpCode);

        return otpMapper.toResendOtpResponse(otpVerification);
    }
}
