package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationSummaryResponse {
    long pendingReportCasesCount;
    long reportedDocumentsCount;
    long pendingUploadDocumentsCount;
    long totalBannedUsersCount;
    long totalPendingUsersCount;
}
