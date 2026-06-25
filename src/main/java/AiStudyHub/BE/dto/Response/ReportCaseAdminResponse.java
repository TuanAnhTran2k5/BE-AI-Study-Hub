package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.CaseStatus;
import AiStudyHub.BE.constraint.ReportSeverity;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportCaseAdminResponse {
    Long caseId;
    Long documentId;
    String documentTitle;
    String reasonName;
    ReportSeverity caseLevel;
    Integer reportCount;
    Integer requiredThreshold;
    CaseStatus caseStatus;
    LocalDateTime openedAt;
    LocalDateTime resolvedAt;
    String resolvedByName;
    String adminNote;
}
