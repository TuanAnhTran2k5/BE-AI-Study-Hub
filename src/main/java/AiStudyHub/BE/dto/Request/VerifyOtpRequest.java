package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    
    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    private String email;
    
    @NotBlank(message = "FIELD_REQUIRED")
    private String otpCode;
}
