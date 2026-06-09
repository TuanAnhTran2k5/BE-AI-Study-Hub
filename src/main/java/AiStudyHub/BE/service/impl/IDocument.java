package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import AiStudyHub.BE.dto.Response.DocumentUploadResponse;

public interface IDocument {
    DocumentUploadResponse uploadDocument(MultipartFile file, DocumentUploadRequest request) throws Exception;
}
