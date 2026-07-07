package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.validator.MaxFileSize;
import AiStudyHub.BE.constraint.validator.ValidImageFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

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

    @ValidImageFile(message = "UNSUPPORTED_IMAGE_TYPE")
    @MaxFileSize(maxBytes = 5L * 1024 * 1024, message = "INVALID_IMAGE_SIZE")
    MultipartFile avatar;
}

