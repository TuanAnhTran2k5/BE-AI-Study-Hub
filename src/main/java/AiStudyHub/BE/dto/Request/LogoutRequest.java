package AiStudyHub.BE.dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogoutRequest {
    @NotBlank(message = "Token cannot be blank")
    String token;
}
