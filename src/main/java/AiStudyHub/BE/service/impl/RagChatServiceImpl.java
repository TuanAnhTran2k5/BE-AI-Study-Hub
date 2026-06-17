package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.exception.RagProcessingException;
import AiStudyHub.BE.exception.VectorStoreException;
import AiStudyHub.BE.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.DocumentRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for {@link RagChatService}.
 * Executes the similarity query search in Qdrant, frames the RAG prompt context,
 * and handles completion requests to OpenAI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final DocumentRepo documentRepo;

    private static final String RAG_PROMPT_TEMPLATE = """
            You are an AI assistant.
            
            Answer only using the provided context.
            
            If the answer is not found in the context, clearly state that the information is unavailable.
            
            Context:
            {context}
            
            Question:
            {question}
            
            Answer:
            """;

    @Override
    public ChatResponse askQuestion(ChatRequest request) {
        String question = request.getQuestion();
        log.info("Processing user question: {}", question);

        // 1. Retrieve relevant chunks from the vector store
        List<Document> relevantChunks = retrieveRelevantChunks(question);
        log.info("Retrieved {} relevant chunks from vector store", relevantChunks.size());

        if (relevantChunks.isEmpty()) {
            return ChatResponse.builder()
                    .answer("No relevant information found in the documents to answer this question.")
                    .sources(List.of())
                    .build();
        }

        // 2. Combine document contents for context
        String context = buildContext(relevantChunks);

        // 3. Extract unique sources (filenames) from metadata
        List<String> sources = relevantChunks.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();
                    if (metadata != null && metadata.containsKey("originalFileName")) {
                        return (String) metadata.get("originalFileName");
                    }
                    return "Unknown Source";
                })
                .distinct()
                .collect(Collectors.toList());

        // 4. Build context prompt and call LLM
        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("context", context);
            promptParameters.put("question", question);
            org.springframework.ai.chat.prompt.Prompt prompt = promptTemplate.create(promptParameters);

            log.info("Calling OpenAI chat model...");
            String answer = chatClient.prompt(prompt).call().content();
            log.info("Received answer from OpenAI");

            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .build();
        } catch (Exception e) {
            log.error("Error generating answer from LLM", e);
            throw new RagProcessingException("Failed to generate answer from chat model", e);
        }
    }

    @Override
    public String buildContext(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public List<Document> retrieveRelevantChunks(String question) {
        try {
            log.info("Searching vector store for query: {}", question);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<Long> accessibleIds;
            if (auth != null && auth.getPrincipal() instanceof User currentUser) {
                accessibleIds = documentRepo.findAccessibleDocumentIds(currentUser.getUserId());
            } else {
                accessibleIds = documentRepo.findPublicDocumentIds();
            }

            if (accessibleIds.isEmpty()) {
                log.info("No documents are accessible to the user. Returning empty chunks.");
                return List.of();
            }

            List<String> accessibleIdStrings = accessibleIds.stream()
                    .map(Object::toString)
                    .toList();

            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            Filter.Expression filterExpression = filterBuilder.in("documentId", accessibleIdStrings).build();

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .filterExpression(filterExpression)
                    .topK(5)
                    .build();
            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("Error searching vector store for query: {}", question, e);
            throw new VectorStoreException("Failed to search similar chunks in vector store", e);
        }
    }
}