package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    Document uploadDocument(
            MultipartFile file,
            String title,
            Long ownerId,
            Long subjectId,
            VisibilityStatus visibilityStatus
    ) throws Exception;
}