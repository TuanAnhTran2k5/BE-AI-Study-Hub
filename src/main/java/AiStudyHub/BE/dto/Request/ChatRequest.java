package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO representing a user's natural language question.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    private String question;
}
