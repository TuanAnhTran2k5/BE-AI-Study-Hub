package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.UploadDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;

/**
 * Service interface for managing RAG documents, extracting text, chunking, and indexing vectors.
 */
public interface RagDocumentService {

    /**
     * Extracts text, chunks, embeds, and indexes an existing document.
     *
     * @param documentId the ID of the document to index
     * @return response DTO showing updated document status
     */
    UploadDocumentResponse indexDocument(Long documentId);

    /**
     * Deletes a document, its metadata, database chunks, and corresponding vectors from Qdrant.
     *
     * @param documentId the ID of the document to delete
     */
    void deleteDocument(Long documentId);

    /**
     * Retrieves the metadata of a document.
     *
     * @param documentId the ID of the document
     * @return response DTO containing document details
     */
    UploadDocumentResponse getDocument(Long documentId);

    /**
     * Extracts text, chunks, embeds, and indexes document content from raw file bytes.
     *
     * @param document the RagDocument metadata
     * @param fileBytes the raw file bytes
     */
    void indexDocumentContent(RagDocument document, byte[] fileBytes);
}
