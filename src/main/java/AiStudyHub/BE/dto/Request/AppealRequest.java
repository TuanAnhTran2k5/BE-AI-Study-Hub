package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealRequest {

    @NotNull(message = "FIELD_REQUIRED")
    Long caseId;

    @NotBlank(message = "FIELD_REQUIRED")
    String appealReason;

    String evidenceUrl;
}
