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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/document")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
@Tag(name = "document-controller")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Operation(summary = "Upload Document")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadFile(
            @Parameter(description = "Document file to upload", required = true)
            @RequestPart("file") @NotNull MultipartFile file,

            @Parameter(description = "Document title", required = true)
            @RequestParam("title") @NotBlank String title,

            @Parameter(description = "Subject ID", required = true)
            @RequestParam("subjectId") @NotNull Long subjectId,

            @Parameter(description = "Visibility (PUBLIC / PRIVATE)", required = false)
            @RequestParam(value = "visibilityStatus", required = false) VisibilityStatus visibilityStatus
    ) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .file(file)
                .title(title)
                .ownerId(currentUser.getUserId())
                .subjectId(subjectId)
                .visibilityStatus(visibilityStatus != null ? visibilityStatus : VisibilityStatus.PRIVATE)
                .build();

        DocumentUploadResponse response = documentService.uploadDocument(file, request);

        return ResponseEntity.ok(
                APIResponse.response(200, "Upload document successfully", response)
        );
    }

}
