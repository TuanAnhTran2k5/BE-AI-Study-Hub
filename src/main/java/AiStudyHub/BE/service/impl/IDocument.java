package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;

public interface IDocument {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception;
}

