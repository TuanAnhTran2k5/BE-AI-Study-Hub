package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.exception.RagProcessingException;
import AiStudyHub.BE.exception.VectorStoreException;
import AiStudyHub.BE.service.impl.IRagChat;
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

// Exec similarity search in Qdrant, frame RAG prompt context, & handle OpenAI requests
@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService implements IRagChat {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final DocumentRepo documentRepo;

    private static final String RAG_PROMPT_TEMPLATE = """
            You are a helpful AI study assistant.
            
            The user is asking a question or making a request (e.g., summarize, explain) regarding their uploaded documents.
            The following 'Context' contains the extracted content from those documents.
            
            Instructions:
            1. Treat the provided Context as the actual content of the user's files.
            2. If the user asks to summarize the file, summarize the information present in the Context.
            3. Answer ONLY based on the provided Context. Do not use external knowledge.
            4. If the Context does not contain relevant information to answer the question, clearly state that the information is not available in the provided documents.
            
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

            log.info("Compiled RAG Context length: {} characters", context.length());
            log.debug("Compiled RAG Context:\n{}", context);

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
                .map(doc -> {
                    String fileName = "Unknown File";
                    if (doc.getMetadata() != null && doc.getMetadata().containsKey("originalFileName")) {
                        fileName = (String) doc.getMetadata().get("originalFileName");
                    }
                    return "[Source File: " + fileName + "]\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    @Override
    public List<Document> retrieveRelevantChunks(String question) {
        try {
            log.info("Searching vector store for query: {}", question);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<Long> accessibleIds;
            if (auth != null && auth.getPrincipal() instanceof User currentUser) {
                accessibleIds = documentRepo.findByOwner_UserIdOrVisibilityStatus(currentUser.getUserId(), AiStudyHub.BE.constraint.VisibilityStatus.PUBLIC)
                        .stream()
                        .map(AiStudyHub.BE.entity.Document::getDocumentId)
                        .toList();
            } else {
                accessibleIds = documentRepo.findByVisibilityStatus(AiStudyHub.BE.constraint.VisibilityStatus.PUBLIC)
                        .stream()
                        .map(AiStudyHub.BE.entity.Document::getDocumentId)
                        .toList();
            }

            if (accessibleIds.isEmpty()) {
                log.info("No documents are accessible to the user. Returning empty chunks.");
                return List.of();
            }

            List<String> accessibleIdStrings = accessibleIds.stream()
                    .map(Object::toString)
                    .toList();

            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            Filter.Expression filterExpression = filterBuilder.in("documentId", accessibleIdStrings.toArray()).build();

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