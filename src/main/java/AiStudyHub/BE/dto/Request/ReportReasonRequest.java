package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.ReportSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportReasonRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    String reasonName;

    @NotNull(message = "FIELD_REQUIRED")
    ReportSeverity severityLevel;

    String description;

    @NotNull(message = "FIELD_REQUIRED")
    Integer reportThreshold;

    @NotNull(message = "FIELD_REQUIRED")
    Integer penaltyScore;
}
