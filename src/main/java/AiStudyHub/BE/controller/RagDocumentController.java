package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.service.IRagSystem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user/rag/documents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Validated
@Slf4j
public class RagDocumentController {

    IRagSystem ragDocumentService;

    @PostMapping("/{id}/index")
    public ResponseEntity<APIResponse<RagDocumentResponse>> indexDocument(@PathVariable Long id) {
        log.info("API Request: Trigger indexing for document ID {}", id);
        RagDocumentResponse response = ragDocumentService.indexDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document index triggered successfully", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteDocument(@PathVariable Long id) {
        log.info("API Request: Delete document ID {}", id);
        DeleteResponse deleteResponse = ragDocumentService.deleteDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document deleted successfully from RAG pipeline", deleteResponse)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<RagDocumentResponse>> getDocument(@PathVariable Long id) {
        log.info("API Request: Get document ID {}", id);
        RagDocumentResponse response = ragDocumentService.getDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document retrieved successfully", response)
        );
    }
}