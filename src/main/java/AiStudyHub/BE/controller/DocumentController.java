package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.VisibilityStatus;

import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.DocumentService;
import AiStudyHub.BE.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/document")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
@Tag(name = "document-controller")
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private RatingService ratingService;

    @Operation(summary = "Upload Document")
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = DocumentUploadRequest.class)
            )
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadFile(
            @Valid @ModelAttribute DocumentUploadRequest request
    ) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        request.setOwnerId(currentUser.getUserId());
        if (request.getVisibilityStatus() == null) {
            request.setVisibilityStatus(VisibilityStatus.PRIVATE);
        }

        DocumentUploadResponse response = documentService.uploadDocument(request);

        return ResponseEntity.ok(
                APIResponse.response(200, "Upload document successfully", response)
        );
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<APIResponse<DocumentDeleteResponse>> deleteDocument(@PathVariable Long documentId) throws Exception {
        DocumentDeleteResponse response = documentService.deleteDocument(documentId);

        return ResponseEntity.ok(
                APIResponse.response(200, "Delete document successfully", response));
    }

    @PostMapping("/{documentId}/download/public")
    public ResponseEntity<APIResponse<DocumentDownloadResponse>> downloadPublicDocument(
            @PathVariable Long documentId
    ) throws Exception {

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
            @org.springframework.web.bind.annotation.RequestBody DocumentUpdateRequest request
    ) {
        DocumentUpdateResponse response = documentService.updateDocument(documentId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Update document successfully", response)
        );
    }


    @Operation(summary = "Submit or update a rating for a public document")
    @PostMapping(value = "/{documentId}/rating", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<RatingResponse>> submitRating(
            @PathVariable Long documentId,
            @org.springframework.web.bind.annotation.RequestBody RatingRequest request
    ) {
        RatingResponse response = ratingService.submitRating(documentId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Submit rating successfully", response)
        );
    }
}
