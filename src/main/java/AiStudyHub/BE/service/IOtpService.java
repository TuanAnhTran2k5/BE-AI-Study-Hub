package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;

public interface IOtpService {
    /**
     * Invalidate all existing active OTPs for the user and purpose.
     * @return count of invalidated records
     */
    Integer invalidateActiveOtps(User user, OtpPurpose purpose);

    /**
     * Generate, save, and send a new OTP code via email.
     * @return created OtpVerification entity
     */
    OtpVerification generateAndSendOtp(User user, String email, OtpPurpose purpose);

    /**
     * Verify OTP and mark it as consumed (isUse = true, verifiedAt = now).
     * @return verified and consumed OtpVerification entity
     */
    OtpVerification verifyAndConsumeOtp(User user, String otpCode, OtpPurpose purpose);

    /**
     * Verify OTP validity and mark verifiedAt = now without consuming isUse (for forgot-password multi-step flow).
     * @return verified OtpVerification entity
     */
    OtpVerification verifyOtpWithoutConsume(User user, String otpCode, OtpPurpose purpose);
}
