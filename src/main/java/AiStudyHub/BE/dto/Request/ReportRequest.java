package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportRequest {

    @NotNull(message = "FIELD_REQUIRED")
    Long documentId;

    @NotNull(message = "FIELD_REQUIRED")
    Long reasonId;

    String description;
    String evidenceUrl;
}
