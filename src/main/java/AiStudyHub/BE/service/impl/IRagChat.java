package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Service interface for querying the vector database and executing LLM prompting to answer user questions.
 */
public interface IRagChat {

    /**
     * Answers a user's question by performing retrieval-augmented generation.
     *
     * @param request the ChatRequest containing the user's question
     * @return the ChatResponse containing the generated answer and references/sources
     */
    ChatResponse askQuestion(ChatRequest request);

    /**
     * Formats and concatenates retrieved chunk documents into a single context string.
     *
     * @param documents the list of relevant chunks retrieved from the vector store
     * @return the formatted context string
     */
    String buildContext(List<Document> documents);

    /**
     * Queries the vector database for relevant text chunks matching the question.
     *
     * @param question the user's question
     * @return a list of top-K relevant chunk documents
     */
    List<Document> retrieveRelevantChunks(String question);
}
