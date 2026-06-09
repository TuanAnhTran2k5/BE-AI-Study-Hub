package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
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

    @NotNull(message = "FIELD_REQUIRED")
    MultipartFile file;

    @NotBlank(message = "FIELD_REQUIRED")
    String title;

    Long ownerId;

    @NotNull(message = "FIELD_REQUIRED")
    Long subjectId;

    VisibilityStatus visibilityStatus = VisibilityStatus.PRIVATE;
}