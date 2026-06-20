package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.service.impl.IRagDocument;
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
@RequestMapping("/api/user/rag/documents")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RagDocumentController {

    private final IRagDocument ragDocumentService;

    /**
     * Manually triggers indexing of an existing document.
     *
     * @param id the ID of the document to index
     * @return the updated metadata details of the document
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<APIResponse<RagDocumentResponse>> indexDocument(@PathVariable Long id) {
        log.info("API Request: Trigger indexing for document ID {}", id);
        RagDocumentResponse response = ragDocumentService.indexDocument(id);
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
    public ResponseEntity<APIResponse<DeleteResponse>> deleteDocument(@PathVariable Long id) {
        log.info("API Request: Delete document ID {}", id);
        DeleteResponse deleteResponse = ragDocumentService.deleteDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document deleted successfully from RAG pipeline", deleteResponse)
        );
    }

    /**
     * Retrieves details of an indexed document.
     *
     * @param id the ID of the document
     * @return details of the document
     */
    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<RagDocumentResponse>> getDocument(@PathVariable Long id) {
        log.info("API Request: Get document ID {}", id);
        RagDocumentResponse response = ragDocumentService.getDocument(id);
        return ResponseEntity.ok(
                APIResponse.response(200, "Document retrieved successfully", response)
        );
    }
}