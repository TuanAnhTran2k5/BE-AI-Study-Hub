package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    private String question;
}
