package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.service.IEmail;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService implements IEmail {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private String loadOtpEmailTemplate(String otpCode) {
        try {
            ClassPathResource resource = new ClassPathResource("template/otp_email_template.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{otpCode}}", otpCode != null ? otpCode : "");
        } catch (Exception e) {
            log.warn("Could not load template/otp_email_template.html, falling back to basic HTML format: {}", e.getMessage());
            return """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"></head>
                    <body style="font-family: Arial, sans-serif; padding: 20px;">
                        <h2>AI Study Hub Verification</h2>
                        <p>Your verification code is: <strong style="font-size: 24px; color: #0056b3;">%s</strong></p>
                        <p>This code expires in 5 minutes.</p>
                    </body>
                    </html>
                    """.formatted(otpCode);
        }
    }

    // Sends an OTP code to the given email address
    @Override
    @Async
    public CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "AI Study Hub");
            helper.setTo(toEmail);
            helper.setSubject("Verify your AI Study Hub account");

            String htmlContent = loadOtpEmailTemplate(otpCode);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "AI Study Hub");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
