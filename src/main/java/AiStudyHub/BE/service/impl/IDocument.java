package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface IDocument {
    Document uploadDocument(MultipartFile file, DocumentUploadRequest request) throws Exception;
}
