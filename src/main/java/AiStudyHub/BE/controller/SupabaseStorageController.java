package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class SupabaseStorageController {

    private final DocumentService documentService;

    public SupabaseStorageController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam(value = "visibilityStatus", defaultValue = "PRIVATE") VisibilityStatus visibilityStatus
    ) throws Exception {

        Document document = documentService.uploadDocument(
                file,
                title,
                ownerId,
                subjectId,
                visibilityStatus
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "File uploaded and metadata saved successfully",
                        "documentId", document.getDocumentId(),
                        "fileUrl", document.getFileUrl(),
                        "title", document.getTitle()
                )
        );
    }
}