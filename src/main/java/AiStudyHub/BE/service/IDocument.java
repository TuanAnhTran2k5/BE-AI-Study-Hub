package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IDocument {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception;
    DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request);
    DeleteResponse deleteDocument(Long documentId) throws Exception;
    List<DocumentUploadResponse> searchDocumentsByTitle(String keyword);

    DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception;
    ResponseEntity<Resource> downloadMyCloudDocument(Long documentId);
}
