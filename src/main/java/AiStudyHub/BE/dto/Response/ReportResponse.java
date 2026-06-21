package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.ReportStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportResponse {
    Long reportId;
    Long reporterId;
    Long documentId;
    Long reasonId;
    String description;
    String evidenceUrl;
    LocalDateTime createdAt;
    ReportStatus status;
}
