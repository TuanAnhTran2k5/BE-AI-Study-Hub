package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;

public interface INotification {
    void sendDocumentModerationNotification(
            User owner,
            Document document,
            String reasonName,
            int penaltyScore,
            String actionType,
            String explanation
    );

    void sendFalseReportPenaltyNotification(
            User reporter,
            Document document,
            int penaltyScore,
            String explanation
    );
}
