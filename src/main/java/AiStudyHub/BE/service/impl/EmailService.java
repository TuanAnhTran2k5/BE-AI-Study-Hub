package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.service.IEmail;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService implements IEmail {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

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

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7f6; color: #333333; margin: 0; padding: 0; }
                            .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border: 1px solid #e1e8ed; }
                            .header { background: #0056b3; color: #ffffff; padding: 25px 20px; text-align: center; }
                            .header h1 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 1px; }
                            .content { padding: 40px 30px; line-height: 1.6; font-size: 16px; }
                            .otp-box { background: #f0f7ff; border: 2px dashed #0056b3; border-radius: 6px; padding: 20px; margin: 30px 0; font-size: 32px; font-weight: 700; text-align: center; letter-spacing: 8px; color: #0056b3; }
                            .footer { background: #f8f9fa; border-top: 1px solid #eeeeee; text-align: center; padding: 20px; font-size: 13px; color: #888888; }
                            p { margin: 0 0 15px 0; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>AI Study Hub</h1>
                            </div>
                            <div class="content">
                                <p>Hello,</p>
                                <p>You recently requested to verify your account or reset your password. Please use the following OTP code to complete the process:</p>
                                <div class="otp-box">%s</div>
                                <p>This code is valid for <strong>5 minutes</strong>. If you did not request this, please ignore this email to ensure your account's security.</p>
                                <p style="margin-top: 30px;">Best regards,<br/><strong>The AI Study Hub Team</strong></p>
                            </div>
                            <div class="footer">
                                &copy; 2026 AI Study Hub. All rights reserved. <br/>
                                This is an automated message, please do not reply.
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(otpCode);

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
