package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    String email;

    String otpCode;

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#^()_+\\-=])[A-Za-z\\d@$!%*?&.#^()_+\\-=]{8,}$",
            message = "INVALID_PASSWORD"
    )
    String password;

    @NotBlank(message = "FIELD_REQUIRED")
    String confirmPassword;

    @AssertTrue(message = "PASSWORD_NOT_MATCH")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
