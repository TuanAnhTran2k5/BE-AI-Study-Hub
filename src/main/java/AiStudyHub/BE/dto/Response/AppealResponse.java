package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.AppealStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealResponse {
    Long appealId;
    Long caseId;
    Long userId;
    String documentTitle;
    String appealReason;
    String evidenceUrl;
    AppealStatus status;
    LocalDateTime createdAt;
    LocalDateTime resolvedAt;
    String resolvedByAdminName;
    String adminNote;
}
