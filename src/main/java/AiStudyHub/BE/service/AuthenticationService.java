package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
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

@Service
@Slf4j
public class AuthenticationService implements UserDetailsService, IAuthentication {

    @Autowired
    UserRepo userRepo;
    @Autowired
    UserMapper userMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    TokenService tokenService;
    @Autowired
    OtpVerificationRepo  otpVerificationRepo;
    @Autowired
    EmailService emailService;

    @Override
    public UserResponse register(RegisterRequest registerRequest) {
        // Check Email đã tồn tại dưới DB chưa
        if (userRepo.existsByEmail(registerRequest.getEmail())) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Tạo User
        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .status(UserStatus.PENDING)
                .build();
        // lưu user dưới DB
        userRepo.save(user);

        // tạo OTP code
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        // tạo object OtpVerification
        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .email(user.getEmail())
                .otpCode(otpCode)
                .purpose(OtpPurpose.REGISTER)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUse(false)
                .build();
        //lưu otp code dưới DB
        otpVerificationRepo.save(otpVerification);
        //gửi OTP qua email đang được đăng ký
        emailService.sendOtpEmail(user.getEmail(), otpCode);

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse login(LoginRequest loginRequest) {
        try{
            // authenticate by srping security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // change data type authentication -> User
            User user = (User) authentication.getPrincipal();
            // Check email has verified
            if (user.getStatus() == UserStatus.PENDING) {
                throw new GlobalException(ErrorCode.EMAIL_NOT_VERIFIED);
            }
            // Account banned must not accept login
            if (user.getStatus() == UserStatus.BANNED) {
                throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
            }
            // generate access token
            String accessToken = tokenService.generateAccessToken(user);

            // Response
            UserResponse userResponse = userMapper.toUserResponse(user);
            userResponse.setAccessToken(accessToken);

            //Return role for FE
            userResponse.setRole(user.getRole());

            return userResponse;

        }catch (LockedException exception){
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }catch (BadCredentialsException exception){
            throw new GlobalException(ErrorCode.INVALID_CREDENTIALS);
        }catch (UsernameNotFoundException exception){
            log.info(exception.getMessage());
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    public UserResponse verifyEmail(VerifyOtpRequest request) {
        // Find User By Email
        User user = userRepo.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        // authentication user account
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
        }
        // Find Correct OTP in Database
        OtpVerification otp = otpVerificationRepo
                .findByUserAndOtpCodeAndPurposeAndIsUseFalse(
                        user,
                        request.getOtpCode(),
                        OtpPurpose.REGISTER
                )
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));
        // Check OTP was overdue
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }
        // Flag OTP is used
        otp.setIsUse(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepo.save(otp);
        // Change PENDING -> ACTIVE
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
