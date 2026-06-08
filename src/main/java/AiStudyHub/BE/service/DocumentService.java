package AiStudyHub.BE.service;


import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    Document uploadDocument(
            MultipartFile file,
            DocumentUploadRequest request
    ) throws Exception;
}