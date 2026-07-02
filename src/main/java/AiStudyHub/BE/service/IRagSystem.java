package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Request.CreateSessionRequest;
import AiStudyHub.BE.dto.Request.SuggestPromptsRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.dto.Response.ChatSessionResponse;
import AiStudyHub.BE.dto.Response.ChatMessageResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagDocument;
import org.springframework.data.domain.Page;
import org.springframework.ai.document.Document;

import java.util.List;

public interface IRagSystem {
    // --- CHAT ---
    ChatResponse askQuestion(ChatRequest request);
    String buildContext(List<Document> documents);
    List<Document> retrieveRelevantChunks(String question);
    List<String> suggestPrompts(SuggestPromptsRequest request);

    // --- SESSION CHAT ---
    ChatSessionResponse createSession(CreateSessionRequest request);
    List<ChatSessionResponse> getSessions();
    Page<ChatMessageResponse> getSessionMessages(Long sessionId, int page, int size);
    ChatResponse deleteSession(Long sessionId);
    ChatSessionResponse updateSessionDocuments(Long sessionId, CreateSessionRequest request);
    ChatResponse askQuestionInSession(Long sessionId, ChatRequest request);

    // --- DOCUMENT ---
    RagDocumentResponse indexDocument(Long documentId);
    DeleteResponse deleteDocument(Long documentId);
    RagDocumentResponse getDocument(Long documentId);
    boolean indexDocumentContent(RagDocument document, byte[] fileBytes);
}

