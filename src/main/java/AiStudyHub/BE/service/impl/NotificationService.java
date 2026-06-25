package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.NotificationResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.NotificationRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IEmail;
import AiStudyHub.BE.service.INotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotification {

    private static final int PAGE_SIZE = 10;

    private final IEmail emailService;
    private final NotificationRepo notificationRepo;

    // =========================================================
    // Internal helpers used by other services
    // =========================================================

    @Override
    public Notification sendDocumentModerationNotification(
            User owner,
            Document document,
            String reasonName,
            int penaltyScore,
            String actionType,
            String explanation
    ) {
        log.info("Sending moderation notification to document owner {} (ID: {}) for action: {}",
                owner.getEmail(), owner.getUserId(), actionType);

        String subject = "[AI Study Hub] Document Moderation Warning: " + document.getTitle();
        String content = String.format(
                "Hello %s,\n\n" +
                "Your document \"%s\" has undergone moderation.\n" +
                "Reason: %s\n" +
                "Action taken: %s\n" +
                "Reputation points deducted: %d\n" +
                "Details/Explanation: %s\n\n" +
                "Best regards,\nAI Study Hub Administrator.",
                owner.getFullName(),
                document.getTitle(),
                reasonName,
                actionType,
                penaltyScore,
                explanation != null ? explanation : "None"
        );

        emailService.sendEmail(owner.getEmail(), subject, content);

        Notification notification = Notification.builder()
                .user(owner)
                .document(document)
                .title(subject)
                .message(content)
                .type("SYSTEM")
                .notificationCase("DOCUMENT_MODERATION")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    @Override
    public Notification sendFalseReportPenaltyNotification(
            User reporter,
            Document document,
            int penaltyScore,
            String explanation
    ) {
        log.info("Sending false report penalty notification to reporter {} (ID: {})",
                reporter.getEmail(), reporter.getUserId());

        String subject = "[AI Study Hub] Warning Penalty: False Reporting";
        String content = String.format(
                "Hello %s,\n\n" +
                "Your report regarding the document \"%s\" has been rejected after review.\n" +
                "This behavior has been determined as false reporting or spamming to disrupt the system.\n" +
                "Action taken: Deducted %d reputation points from your account.\n" +
                "Reason for rejection: %s\n\n" +
                "Please note that if your reputation score continues to drop to a negative level, " +
                "your reporting privileges will be permanently locked.\n\n" +
                "Best regards,\nAI Study Hub Administrator.",
                reporter.getFullName(),
                document.getTitle(),
                penaltyScore,
                explanation != null ? explanation : "None"
        );

        emailService.sendEmail(reporter.getEmail(), subject, content);

        Notification notification = Notification.builder()
                .user(reporter)
                .document(document)
                .title(subject)
                .message(content)
                .type("SYSTEM")
                .notificationCase("FALSE_REPORT_PENALTY")
                .isRead(false)
                .build();
        return notificationRepo.save(notification);
    }

    // =========================================================
    // REST API methods
    // =========================================================

    @Override
    public Page<NotificationResponse> getMyNotifications(int page, Boolean isRead, String type) {
        User currentUser = SecurityUtils.getCurrentUser();
        Long userId = currentUser.getUserId();

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Notification> notifications;

        boolean hasIsRead = isRead != null;
        boolean hasType = type != null && !type.isBlank();

        if (hasIsRead && hasType) {
            notifications = notificationRepo
                    .findByUserUserIdAndIsReadAndTypeOrderByCreatedAtDesc(userId, isRead, type, pageable);
        } else if (hasIsRead) {
            notifications = notificationRepo
                    .findByUserUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else if (hasType) {
            notifications = notificationRepo
                    .findByUserUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else {
            notifications = notificationRepo
                    .findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return notifications.map(this::toResponse);
    }

    @Override
    public long countUnread() {
        User currentUser = SecurityUtils.getCurrentUser();
        return notificationRepo.countByUserUserIdAndIsRead(currentUser.getUserId(), false);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new GlobalException(404, "Notification not found"));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new GlobalException(403, "You do not have permission to update this notification");
        }

        notification.setIsRead(true);
        return toResponse(notificationRepo.save(notification));
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        User currentUser = SecurityUtils.getCurrentUser();
        List<Notification> unreadList = notificationRepo.findByUserUserIdAndIsRead(currentUser.getUserId(), false);
        unreadList.forEach(n -> n.setIsRead(true));
        notificationRepo.saveAll(unreadList);
        return unreadList.size();
    }

    @Override
    @Transactional
    public DeleteResponse deleteNotification(Long notificationId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new GlobalException(404, "Notification not found"));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new GlobalException(403, "You do not have permission to delete this notification");
        }

        notificationRepo.delete(notification);

        return DeleteResponse.builder()
                .success(true)
                .message("Notification deleted successfully")
                .deletedId(notificationId)
                .entityName("Notification")
                .entityIdentifier(notification.getTitle())
                .deletedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int deleteAllRead() {
        User currentUser = SecurityUtils.getCurrentUser();
        long count = notificationRepo.deleteByUserUserIdAndIsRead(currentUser.getUserId(), true);
        return (int) count;
    }

    // =========================================================
    // Private mapper
    // =========================================================

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .notificationCase(n.getNotificationCase())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt());

        if (n.getDocument() != null) {
            builder.documentId(n.getDocument().getDocumentId())
                   .documentTitle(n.getDocument().getTitle());
        }

        if (n.getReport() != null) {
            builder.reportId(n.getReport().getReportId());
        }

        return builder.build();
    }
}
