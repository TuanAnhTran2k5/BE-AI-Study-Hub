package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepo extends JpaRepository<OtpVerification, Long> {
    // Check OTP code from user's input
    Optional<OtpVerification> findByUserAndOtpCodeAndPurposeAndIsUseFalse(
            User user,
            String otpCode,
            OtpPurpose purpose
    );
}
