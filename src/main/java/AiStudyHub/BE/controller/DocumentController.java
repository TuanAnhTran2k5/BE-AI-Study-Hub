package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user/document")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Operation(summary = "Upload Document")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<DocumentUploadResponse>> uploadFile(
            @Valid @ModelAttribute DocumentUploadRequest request
    ) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        request.setOwnerId(currentUser.getUserId());

        DocumentUploadResponse response = documentService.uploadDocument(request.getFile(), request);

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Upload document successfully", response
                        ));
    }

}
