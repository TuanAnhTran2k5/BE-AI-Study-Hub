package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    Long notificationId;

    // Related entities (nullable)
    Long documentId;
    String documentTitle;

    Long reportId;

    String title;
    String message;

    // "SYSTEM" | "REWARD" | "SOCIAL" | ...
    String type;

    // e.g. "DOCUMENT_MODERATION", "FALSE_REPORT_PENALTY", "BADGE_AWARDED", ...
    String notificationCase;

    Boolean isRead;
    LocalDateTime createdAt;
}
