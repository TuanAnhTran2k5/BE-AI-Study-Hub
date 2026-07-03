package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    private String question;

    @Positive(message = "Session ID must be positive")
    private Long sessionId;

    private List<Long> documentIds;
}
