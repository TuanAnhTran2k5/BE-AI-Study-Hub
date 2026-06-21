package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.service.IEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService implements IEmail {

    @Autowired
    private JavaMailSender mailSender;

    // Sends an OTP code to the given email address
    @Override
    @Async
    public CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Verify your AI Study Hub account");
            message.setText(
                    "Your OTP code is: " + otpCode +
                            "\nThis code will expire in 5 minutes.");
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            // Runs on an async thread: log the failure instead of silently swallowing it.
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
