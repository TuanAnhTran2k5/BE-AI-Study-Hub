package AiStudyHub.BE.controller;

import AiStudyHub.BE.service.DocumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cloud/document")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@CrossOrigin("*")
@Tag(name = "cloud-document-controller")
public class CloudDocumentController {

    @Autowired
    DocumentService documentService;

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadMyCloudDocument(
            @PathVariable Long documentId
    ) {
        return documentService.downloadMyCloudDocument(documentId);
    }
}
