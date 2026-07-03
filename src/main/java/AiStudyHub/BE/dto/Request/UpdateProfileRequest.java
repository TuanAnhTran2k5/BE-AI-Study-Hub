package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 5,max = 50, message = "INVALID_SIZE")
    String fullName;

    org.springframework.web.multipart.MultipartFile avatar;
}
