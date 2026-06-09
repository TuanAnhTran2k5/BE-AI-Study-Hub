package AiStudyHub.BE.service.impl;

import java.util.concurrent.CompletableFuture;

public interface IEmail {
    CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode);
}
