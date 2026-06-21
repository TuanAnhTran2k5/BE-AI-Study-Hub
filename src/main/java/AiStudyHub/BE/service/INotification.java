package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.Notification;

public interface INotification {
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
}
