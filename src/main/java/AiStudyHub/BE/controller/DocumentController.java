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
import org.springframework.web.multipart.MultipartFile;

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
            @io.swagger.v3.oas.annotations.Parameter(description = "File tài liệu cần upload") 
            @RequestPart("file") MultipartFile file,
            
            @io.swagger.v3.oas.annotations.Parameter(description = "Tiêu đề tài liệu") 
            @RequestParam("title") String title,
            
            @io.swagger.v3.oas.annotations.Parameter(description = "ID của môn học") 
            @RequestParam("subjectId") Long subjectId,
            
            @io.swagger.v3.oas.annotations.Parameter(description = "Trạng thái hiển thị (PUBLIC/PRIVATE)") 
            @RequestParam(value = "visibilityStatus", defaultValue = "PRIVATE") VisibilityStatus visibilityStatus
    ) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle(title);
        request.setOwnerId(currentUser.getUserId());
        request.setSubjectId(subjectId);
        request.setVisibilityStatus(visibilityStatus);

        Document document = documentService.uploadDocument(file, request);

        DocumentUploadResponse response = DocumentUploadResponse.builder()
                .documentId(document.getDocumentId())
                .ownerId(document.getOwner().getUserId())
                .title(document.getTitle())
                .fileUrl(document.getFileUrl())
                .message("File uploaded and metadata saved successfully")
                .build();

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Upload document successfully", response
                        ));
    }

}
