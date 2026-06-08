package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.service.SupabaseStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@SecurityRequirement(name = "api")
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

    @Operation(
            summary = "Thông tin Storage API",
            description = "Upload tài liệu vui lòng dùng endpoint: `POST /api/user/document/upload`"
    )
    @GetMapping("/info")
    public ResponseEntity<APIResponse<Map<String, String>>> info() {
        Map<String, String> data = Map.of(
                "message", "Vui lòng dùng POST /api/user/document/upload để upload tài liệu",
                "uploadEndpoint", "/api/user/document/upload"
        );
        
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Thông tin API Storage", data
                        ));
    }
}