package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepo extends JpaRepository<OtpVerification, Long> {
    // Check OTP code from user's input
    Optional<OtpVerification> findByUserAndOtpCodeAndPurposeAndIsUseFalse(
            User user,
            String otpCode,
            OtpPurpose purpose
    );

    // All still-valid (unused) OTPs of a user for a given purpose, used to invalidate
    // previously issued codes before a new one is generated.
    List<OtpVerification> findByUserAndPurposeAndIsUseFalse(
            User user,
            OtpPurpose purpose
    );
}
