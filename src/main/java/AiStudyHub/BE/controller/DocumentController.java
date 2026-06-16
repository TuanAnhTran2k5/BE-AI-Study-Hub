package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.VisibilityStatus;

import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.DocumentService;
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

    @Operation(summary = "Upload Document")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = DocumentUploadRequest.class)))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadFile(
            @Valid @ModelAttribute DocumentUploadRequest request) throws Exception {

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
                APIResponse.response(200, "Upload document successfully", response));
    }

    @Operation(summary = "Delete Document")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<APIResponse<Void>> deleteDocument(@PathVariable Long documentId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        documentService.deleteDocument(documentId, currentUser);

        return ResponseEntity.ok(
                APIResponse.response(200, "Delete document successfully", null));
    }

}
