package AiStudyHub.BE;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.NotificationRepo;
import AiStudyHub.BE.service.IEmail;
import AiStudyHub.BE.service.impl.EmailService;
import AiStudyHub.BE.service.impl.NotificationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailAndNotificationTemplateTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private IEmail emailServiceMock;
    @Mock
    private NotificationRepo notificationRepo;

    @InjectMocks
    private EmailService emailService;
    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@aistudyhub.com");
    }

    @Test
    public void testSendOtpEmail_LoadsTemplateSuccessfully() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        CompletableFuture<Boolean> future = emailService.sendOtpEmail("user@example.com", "987654");

        assertTrue(future.join());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testSendAccountBannedNotification_LoadsBannedTemplate() {
        User owner = User.builder()
                .userId(10L)
                .fullName("John Doe")
                .email("johndoe@example.com")
                .build();
        Document document = Document.builder()
                .documentId(99L)
                .title("Infringing Notes")
                .build();

        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendAccountBannedNotification(
                owner, document, "Copyright Infringement", "Document violates copyright terms."
        );

        assertNotNull(notification);
        assertEquals("ACCOUNT_BANNED", notification.getNotificationCase());

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServiceMock, times(1)).sendEmail(eq("johndoe@example.com"), anyString(), contentCaptor.capture());

        String capturedHtml = contentCaptor.getValue();
        assertTrue(capturedHtml.contains("ACCOUNT LOCKED / BANNED"), "HTML content should contain Account Locked/Banned badge from template");
        assertTrue(capturedHtml.contains("John Doe"), "HTML content should replace userName placeholder");
        assertTrue(capturedHtml.contains("Infringing Notes"), "HTML content should replace documentTitle placeholder");
        assertTrue(capturedHtml.contains("support@aistudyhub.com"), "HTML content should contain appeal instructions");
    }

    @Test
    public void testSendAccountUnbannedNotification_LoadsUnbannedTemplate() {
        User user = User.builder()
                .userId(15L)
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.sendAccountUnbannedNotification(user);

        assertNotNull(notification);
        assertEquals("ACCOUNT_UNBANNED", notification.getNotificationCase());

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServiceMock, times(1)).sendEmail(eq("alice@example.com"), anyString(), contentCaptor.capture());

        String capturedHtml = contentCaptor.getValue();
        assertTrue(capturedHtml.contains("ACCOUNT RESTORED / ACTIVE"), "HTML content should contain Account Restored badge from template");
        assertTrue(capturedHtml.contains("Alice Smith"), "HTML content should replace userName placeholder");
        assertTrue(capturedHtml.contains("Welcome back"), "HTML content should welcome user back");
    }
}
