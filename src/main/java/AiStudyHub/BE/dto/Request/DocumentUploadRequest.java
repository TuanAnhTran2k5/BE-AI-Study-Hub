package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUploadRequest {

    private String title;

    private Long ownerId;

    private Long subjectId;

    private VisibilityStatus visibilityStatus;
}