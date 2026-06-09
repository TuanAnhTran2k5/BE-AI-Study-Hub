package AiStudyHub.BE.service;

import AiStudyHub.BE.service.impl.IEmail;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your AI Study Hub account");
        message.setText(
                "Your OTP code is: " + otpCode +
                        "\nThis code will expire in 5 minutes."
        );
        mailSender.send(message);
        return CompletableFuture.completedFuture(true);
    }

}
