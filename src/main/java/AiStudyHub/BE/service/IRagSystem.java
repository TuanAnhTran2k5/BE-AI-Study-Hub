package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;
import org.springframework.ai.document.Document;

import java.util.List;

public interface IRagSystem {
    // --- CHAT ---
    ChatResponse askQuestion(ChatRequest request);
    String buildContext(List<Document> documents);
    List<Document> retrieveRelevantChunks(String question);

    // --- DOCUMENT ---
    RagDocumentResponse indexDocument(Long documentId);
    DeleteResponse deleteDocument(Long documentId);
    RagDocumentResponse getDocument(Long documentId);
    boolean indexDocumentContent(RagDocument document, byte[] fileBytes);
}
