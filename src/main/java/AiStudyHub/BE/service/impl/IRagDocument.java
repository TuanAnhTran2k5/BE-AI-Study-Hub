package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;


public interface IRagDocument {

 
    RagDocumentResponse indexDocument(Long documentId);

    DeleteResponse deleteDocument(Long documentId);

    RagDocumentResponse getDocument(Long documentId);

    boolean indexDocumentContent(RagDocument document, byte[] fileBytes);
}
