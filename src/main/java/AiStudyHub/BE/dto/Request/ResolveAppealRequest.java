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
public class ResolveAppealRequest {

    @NotNull(message = "FIELD_REQUIRED")
    Boolean approve;

    @NotBlank(message = "FIELD_REQUIRED")
    String adminNote;
}
