package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
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

    MultipartFile file;

    String title;

    Long ownerId;

    Long subjectId;

    VisibilityStatus visibilityStatus = VisibilityStatus.PRIVATE;
}