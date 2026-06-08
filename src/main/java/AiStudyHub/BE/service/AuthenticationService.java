package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.AuthProvider;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.OtpVerificationRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IAuthentication;
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
    private EmailService emailService;

    // Bộ nhớ tạm lưu trữ thông tin đăng ký (OTP-first, DB-last)
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    @Getter
    @Setter
    @Builder
    public static class PendingRegistration {
        private String email;
        private String password;
        private String fullName;
        private String otpCode;
        private LocalDateTime expiredAt;
    }

    @Override
    public UserResponse register(RegisterRequest registerRequest) {
        // Kiểm tra xem email đã tồn tại dưới DB chưa
        if (userRepo.existsByEmail(registerRequest.getEmail())) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Tạo OTP code
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Lưu thông tin đăng ký vào bộ nhớ tạm thay vì Database
        PendingRegistration pendingReg = PendingRegistration.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        pendingRegistrations.put(registerRequest.getEmail(), pendingReg);

        // Gửi OTP qua email
        emailService.sendOtpEmail(registerRequest.getEmail(), otpCode);

        // Trả về thông tin cơ bản cho frontend biết đăng ký thành công (chờ xác thực)
        UserResponse response = new UserResponse();
        response.setEmail(registerRequest.getEmail());
        response.setFullName(registerRequest.getFullName());
        return response;
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

        // Ưu tiên kiểm tra trong bộ nhớ tạm (luồng mới)
        if (pendingRegistrations.containsKey(email)) {
            PendingRegistration pendingReg = pendingRegistrations.get(email);

            if (pendingReg.getExpiredAt().isBefore(LocalDateTime.now())) {
                pendingRegistrations.remove(email);
                throw new GlobalException(ErrorCode.OTP_EXPIRED);
            }

            if (!pendingReg.getOtpCode().equals(request.getOtpCode())) {
                throw new GlobalException(ErrorCode.INVALID_OTP);
            }

            // Kiểm tra xem email đã được lưu dưới DB chưa (VD: user đăng nhập GG trong lúc chờ OTP)
            if (userRepo.existsByEmail(email)) {
                pendingRegistrations.remove(email);
                throw new GlobalException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            // OTP đúng và chưa hết hạn -> Tạo User thực sự dưới DB
            User newUser = User.builder()
                    .email(pendingReg.getEmail())
                    .passwordHash(pendingReg.getPassword())
                    .fullName(pendingReg.getFullName())
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .role(UserRole.US)
                    .build();

            userRepo.save(newUser);
            pendingRegistrations.remove(email);

            return userMapper.toUserResponse(newUser);
        }

        // Fallback: Kiểm tra trong DB (trường hợp user PENDING cũ đang tồn tại dưới DB)
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
}
