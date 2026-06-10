package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.LogoutRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.OtpVerificationRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IAuthentication;
import AiStudyHub.BE.service.impl.IEmail;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AuthenticationService implements UserDetailsService, IAuthentication {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private OtpVerificationRepo otpVerificationRepo;
    @Autowired
    private IEmail emailService;



    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        Optional<User> existingUserOpt = userRepo.findUserByEmail(email);
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

        // Generate OTP code
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);

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
        User user = userRepo.findUserByEmail(email)
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepo.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public void logout(LogoutRequest request) {
        // Do nothing on backend for stateless JWT.
        // Frontend is responsible for discarding the token.
    }
}
