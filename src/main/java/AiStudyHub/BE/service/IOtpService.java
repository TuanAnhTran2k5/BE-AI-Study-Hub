package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.OtpPurpose;
import AiStudyHub.BE.entity.OtpVerification;
import AiStudyHub.BE.entity.User;

public interface IOtpService {
    Integer invalidateActiveOtps(User user, OtpPurpose purpose);

    OtpVerification generateAndSendOtp(User user, String email, OtpPurpose purpose);

    OtpVerification verifyAndConsumeOtp(User user, String otpCode, OtpPurpose purpose);

    OtpVerification verifyOtpWithoutConsume(User user, String otpCode, OtpPurpose purpose);
}
