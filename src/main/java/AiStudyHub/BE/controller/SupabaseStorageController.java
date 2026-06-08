package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class SupabaseStorageController {

    private final DocumentService documentService;

    public SupabaseStorageController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "visibilityStatus", defaultValue = "PRIVATE") VisibilityStatus visibilityStatus
    ) throws Exception {

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle(title);
        request.setOwnerId(ownerId);
        request.setSubjectId(subjectId);
        request.setVisibilityStatus(visibilityStatus);

        Document document = documentService.uploadDocument(file, request);

        DocumentUploadResponse response = DocumentUploadResponse.builder()
                                                                .documentId(document.getDocumentId())
                                                                .title(document.getTitle())
                                                                .fileUrl(document.getFileUrl())
                                                                .message("File uploaded and metadata saved successfully")
                                                                .build();

        return ResponseEntity.ok(response);
    }
}