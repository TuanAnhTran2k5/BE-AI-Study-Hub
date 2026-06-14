package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentDeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface IDocument {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception;
    DocumentDeleteResponse deleteDocument(Long documentId) throws Exception;
    DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception;
    ResponseEntity<Resource> downloadMyCloudDocument(Long documentId);
    DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request);
}

