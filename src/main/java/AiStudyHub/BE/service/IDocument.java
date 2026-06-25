package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.DocumentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IDocument {
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception;

    DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request);
    DeleteResponse deleteDocument(Long documentId) throws Exception;
    List<DocumentResponse> searchDocumentsByTitle(String keyword);
    List<DocumentResponse> getMyDocuments(Long userId);
    DocumentResponse getDocumentDetail(Long documentId);

    DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception;
    ResponseEntity<Resource> downloadMyCloudDocument(Long documentId);
    ResponseEntity<Resource> viewDocumentContent(Long documentId);
}
