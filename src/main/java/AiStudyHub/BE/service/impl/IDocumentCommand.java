package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;

import java.util.List;

public interface IDocumentCommand {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception;
    DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request);
    DeleteResponse deleteDocument(Long documentId) throws Exception;
    List<DocumentUploadResponse> searchDocumentsByTitle(String keyword);
}
