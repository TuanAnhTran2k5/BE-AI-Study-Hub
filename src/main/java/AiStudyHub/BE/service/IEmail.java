package AiStudyHub.BE.service;

import java.util.concurrent.CompletableFuture;

public interface IEmail {
    CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode);
}
