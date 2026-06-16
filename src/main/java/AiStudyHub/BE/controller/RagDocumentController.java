package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.UploadDocumentResponse;
import AiStudyHub.BE.service.RagDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing documents in the RAG pipeline.
 * All endpoints are secured by default and require a valid JWT token.
 */
@RestController
@RequestMapping("/api/v1/rag/documents")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RagDocumentController {

    private final RagDocumentService ragDocumentService;

    /**
     * Manually triggers indexing of an existing document.
     *
     * @param id the ID of the document to index
     * @return the updated metadata details of the document
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<APIResponse<UploadDocumentResponse>> indexDocument(@PathVariable Long id) {
        log.info("API Request: Trigger indexing for document ID {}", id);
        UploadDocumentResponse response = ragDocumentService.indexDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document index triggered successfully", response)
        );
    }

    /**
     * Deletes a document from the RAG pipeline.
     *
     * @param id the ID of the document to delete
     * @return clean response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteDocument(@PathVariable Long id) {
        log.info("API Request: Delete document ID {}", id);
        ragDocumentService.deleteDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document deleted successfully from RAG pipeline", null)
        );
    }

    /**
     * Retrieves details of an indexed document.
     *
     * @param id the ID of the document
     * @return details of the document
     */
    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<UploadDocumentResponse>> getDocument(@PathVariable Long id) {
        log.info("API Request: Get document ID {}", id);
        UploadDocumentResponse response = ragDocumentService.getDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document retrieved successfully", response)
        );
    }
}