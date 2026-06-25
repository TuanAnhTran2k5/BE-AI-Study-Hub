package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.VisibilityStatus;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.IDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user/document")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "document-controller")
public class DocumentController {

    IDocument documentService;
    IGamification gamificationService;

    @Operation(summary = "Upload Document")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = DocumentUploadRequest.class)))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadFile(
            @Valid @ModelAttribute DocumentUploadRequest request) throws Exception {

        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();

        request.setOwnerId(currentUser.getUserId());
        if (request.getVisibilityStatus() == null) {
            request.setVisibilityStatus(VisibilityStatus.PRIVATE);
        }

        DocumentUploadResponse response = documentService.uploadDocument(request);

        return ResponseEntity.ok(
                APIResponse.response(200, "Upload document successfully", response));
    }

    @Operation(summary = "Delete Document")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteDocument(@PathVariable Long documentId) throws Exception {
        DeleteResponse response = documentService.deleteDocument(documentId);

        return ResponseEntity.ok(
                APIResponse.response(200, "Delete document successfully", response));
    }

    @PostMapping("/{documentId}/download/public")
    public ResponseEntity<APIResponse<DocumentDownloadResponse>> downloadPublicDocument(@PathVariable Long documentId) throws Exception {

        DocumentDownloadResponse response = documentService.downloadPublicDocument(documentId);

        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Download public document successfully",
                        response
                )
        );
    }

    @Operation(summary = "Update Document (partial)")
    @PatchMapping(value = "/{documentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<DocumentUpdateResponse>> updateDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentUpdateRequest request
    ) {
        DocumentUpdateResponse response = documentService.updateDocument(documentId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Update document successfully", response)
        );
    }


    @Operation(summary = "Download my own (cloud) document")
    @GetMapping("/{documentId}/cloud-download")
    public ResponseEntity<Resource> downloadMyCloudDocument(@PathVariable Long documentId) {
        return documentService.downloadMyCloudDocument(documentId);
    }

    @Operation(summary = "Submit or update a rating for a public document")
    @PostMapping(value = "/{documentId}/rating", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<RatingResponse>> submitRating(
            @PathVariable Long documentId,
            @RequestBody RatingRequest request
    ) {
        RatingResponse response = gamificationService.submitRating(documentId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Submit rating successfully", response)
        );
    }

    @Operation(summary = "Search public documents by title")
    @GetMapping("/search")
    public ResponseEntity<APIResponse<List<DocumentResponse>>> searchDocuments(
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        List<DocumentResponse> response = documentService.searchDocumentsByTitle(keyword);
        return ResponseEntity.ok(
                APIResponse.response(200, "Search documents successfully", response)
        );
    }

    @Operation(summary = "Get my documents")
    @GetMapping("/my-documents")
    public ResponseEntity<APIResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal User currentUser) {
        
        List<DocumentResponse> response = documentService.getMyDocuments(currentUser.getUserId());
        return ResponseEntity.ok(
                APIResponse.response(200, "Get my documents successfully", response)
        );
    }

    @Operation(summary = "Get document detail")
    @GetMapping("/{documentId}")
    public ResponseEntity<APIResponse<DocumentResponse>> getDocumentDetail(
            @PathVariable Long documentId) {
        
        DocumentResponse response = documentService.getDocumentDetail(documentId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Get document detail successfully", response)
        );
    }

    @Operation(summary = "View document content directly (for PDF viewers, etc.)")
    @GetMapping("/{documentId}/view-content")
    public ResponseEntity<Resource> viewDocumentContent(@PathVariable Long documentId) {
        return documentService.viewDocumentContent(documentId);
    }
}

