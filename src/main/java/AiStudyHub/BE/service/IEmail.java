package AiStudyHub.BE.service;

import java.util.concurrent.CompletableFuture;

public interface IEmail {
    CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode);
    CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content);
}
