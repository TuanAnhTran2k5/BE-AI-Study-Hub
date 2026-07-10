package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.NotificationResponse;
import AiStudyHub.BE.entity.Badge;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.entity.Ranking;
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

    Notification sendAccountBannedNotification(
            User owner,
            Document document,
            String reasonName,
            String explanation
    );

    Notification sendAccountUnbannedNotification(User user);

    Notification sendReportApprovedNotification(
            User reporter,
            Document document,
            String explanation
    );

    Notification sendDocumentRestoredNotification(
            User owner,
            Document document,
            String explanation
    );

    Notification sendDocumentDownloadNotification(
            User owner,
            User downloader,
            Document document,
            int pointsAwarded
    );

    Notification sendDocumentBookmarkNotification(
            User owner,
            User bookmarker,
            Document document,
            int pointsAwarded
    );

    Notification sendDocumentRatingNotification(
            User owner,
            User rater,
            Document document,
            int ratingValue
    );

    Notification sendRankUpNotification(
            User user,
            Ranking newRank
    );

    Notification sendBadgeAwardedNotification(
            User user,
            Badge badge
    );

    Page<NotificationResponse> getMyNotifications(int page, Boolean isRead, String type);

    long countUnread();

    NotificationResponse markAsRead(Long notificationId);

    int markAllAsRead();

    DeleteResponse deleteNotification(Long notificationId);

    int deleteAllRead();
}
