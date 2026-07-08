package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.validator.MaxFileSize;
import AiStudyHub.BE.constraint.validator.ValidImageFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BadgeRequest {

    @Schema(nullable = true)
    String badgeName;

    @Schema(nullable = true)
    String description;

    @Schema(nullable = true)
    @Min(value = 0, message = "INVALID_FORMAT")
    Integer requiredDownloads;

    @Schema(nullable = true)
    @ValidImageFile(message = "UNSUPPORTED_IMAGE_TYPE")
    @MaxFileSize(maxBytes = 5 * 1024 * 1024, message = "INVALID_IMAGE_SIZE")
    MultipartFile iconFile;
}
