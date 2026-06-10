package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentUploadRequest {

    @Schema(type = "string", format = "binary", description = "Document file to upload")
    @NotNull(message = "FIELD_REQUIRED")
    MultipartFile file;

    @NotBlank(message = "FIELD_REQUIRED")
    String title;

    @Schema(hidden = true)
    Long ownerId;

    @NotNull(message = "FIELD_REQUIRED")
    Long subjectId;

    VisibilityStatus visibilityStatus = VisibilityStatus.PRIVATE;
}