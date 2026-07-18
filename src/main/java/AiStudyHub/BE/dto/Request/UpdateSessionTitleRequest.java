package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionTitleRequest {
    @NotBlank(message = "FIELD_REQUIRED")
    private String sessionTitle;
}
