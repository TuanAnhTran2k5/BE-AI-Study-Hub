package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class DocumentUploadRequest {

    private MultipartFile file;

    private String title;

    private Long ownerId;

    private Long subjectId;

    private VisibilityStatus visibilityStatus = VisibilityStatus.PRIVATE;
}