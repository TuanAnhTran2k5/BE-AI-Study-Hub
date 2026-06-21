package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface IDocumentDownload {
    DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception;
    ResponseEntity<Resource> downloadMyCloudDocument(Long documentId);
}
