package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.OtpVerificationRepo;
import AiStudyHub.BE.service.IEmail;
import AiStudyHub.BE.service.IOtpService;
import AiStudyHub.BE.utils.OtpUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpService implements IOtpService {

    OtpVerificationRepo otpVerificationRepo;
    IEmail emailService;
    OtpUtil otpUtil;

    @Override
    @Transactional
    public Integer invalidateActiveOtps(User user, OtpPurpose purpose) {
        List<OtpVerification> oldOtps =
                otpVerificationRepo.findByUserAndPurposeAndIsUseFalse(user, purpose);
        if (!oldOtps.isEmpty()) {
            oldOtps.forEach(o -> o.setIsUse(true));
            otpVerificationRepo.saveAll(oldOtps);
        }
        return oldOtps.size();
    }

    @Override
    @Transactional
    public OtpVerification generateAndSendOtp(User user, String email, OtpPurpose purpose) {
        // Invalidate old active OTPs for this purpose
        invalidateActiveOtps(user, purpose);

        // Generate 6-digit OTP code using OtpUtil
        String otpCode = otpUtil.generateSixDigitOtp();

        // Save OtpVerification to DB
        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUse(false)
                .build();
        otpVerification = otpVerificationRepo.save(otpVerification);

        // Send OTP via email
        emailService.sendOtpEmail(email, otpCode);

        log.info("Generated and sent OTP for user {} with purpose {}", email, purpose);
        return otpVerification;
    }

    @Override
    @Transactional
    public OtpVerification verifyAndConsumeOtp(User user, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepo
                .findByUserAndOtpCodeAndPurposeAndIsUseFalse(user, otpCode, purpose)
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }

        otp.setIsUse(true);
        otp.setVerifiedAt(LocalDateTime.now());
        return otpVerificationRepo.save(otp);
    }

    @Override
    public OtpVerification verifyOtpWithoutConsume(User user, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepo
                .findByUserAndOtpCodeAndPurposeAndIsUseFalse(user, otpCode, purpose)
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_OTP));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.OTP_EXPIRED);
        }

        // OTP is valid — do NOT consume yet; resetPassword will verify and consume in a single step
        return otp;
    }
}
