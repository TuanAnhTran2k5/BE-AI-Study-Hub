package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    String email;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 5,max = 50, message = "INVALID_SIZE")
    String fullName;

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
