package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import org.springframework.ai.document.Document;

import java.util.List;

// Interface for Qdrant RAG queries & LLM prompting
public interface IRagChat {

    // Answer question using RAG
    ChatResponse askQuestion(ChatRequest request);

    // Format chunks into a context string
    String buildContext(List<Document> documents);

    // Query vector DB for matching chunks
    List<Document> retrieveRelevantChunks(String question);
}
