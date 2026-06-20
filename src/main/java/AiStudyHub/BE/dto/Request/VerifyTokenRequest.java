package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
