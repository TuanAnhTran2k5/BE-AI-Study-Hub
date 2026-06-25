package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.NotificationResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.entity.User;
import org.springframework.data.domain.Page;

public interface INotification {

    // --- Internal: used by other services to create notifications ---

    Notification sendDocumentModerationNotification(
            User owner,
            Document document,
            String reasonName,
            int penaltyScore,
            String actionType,
            String explanation
    );

    Notification sendFalseReportPenaltyNotification(
            User reporter,
            Document document,
            int penaltyScore,
            String explanation
    );

    Page<NotificationResponse> getMyNotifications(int page, Boolean isRead, String type);

    long countUnread();

    NotificationResponse markAsRead(Long notificationId);

    int markAllAsRead();

    DeleteResponse deleteNotification(Long notificationId);

    int deleteAllRead();
}
