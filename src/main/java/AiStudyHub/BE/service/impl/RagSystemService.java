package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.constraint.SenderType;
import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Request.CreateSessionRequest;
import AiStudyHub.BE.dto.Request.SuggestPromptsRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.dto.Response.ChatSessionResponse;
import AiStudyHub.BE.dto.Response.ChatMessageResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.RagDocumentResponse;
import AiStudyHub.BE.entity.RagChunk;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.ChatSession;
import AiStudyHub.BE.entity.ChatMessage;
import AiStudyHub.BE.entity.ChatSessionDocument;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.mapper.RagDocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagChunkRepository;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.ChatSessionRepository;
import AiStudyHub.BE.repository.ChatMessageRepository;
import AiStudyHub.BE.repository.ChatSessionDocumentRepository;
import AiStudyHub.BE.service.IRagSystem;
import AiStudyHub.BE.service.ISupabaseStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RagSystemService implements IRagSystem {

    VectorStore vectorStore;
    ChatClient chatClient;
    DocumentRepo documentRepo;
    RagDocumentRepository ragDocumentRepository;
    RagChunkRepository ragChunkRepository;
    TokenTextSplitter textSplitter;
    RagDocumentMapper ragDocumentMapper;
    ISupabaseStorage supabaseStorageService;

    ChatSessionRepository chatSessionRepository;
    ChatMessageRepository chatMessageRepository;
    ChatSessionDocumentRepository chatSessionDocumentRepository;

    private static final String RAG_PROMPT_TEMPLATE = """
            You are an AI Study Assistant.
            Current System Date & Time: {currentDate}

            Your responsibilities & Knowledge Rules:
            1. Help users understand, summarize, explain, and analyze information from uploaded documents as well as general academic and study topics.
            2. Prioritize Provided Context: Treat the provided Context as the primary and authoritative reference when answering questions related to the user's documents.
            3. Combine & Expand: You are strongly encouraged to combine the provided Context with your pre-trained general knowledge (knowledge trained by your developers) to explain concepts more clearly, provide relevant examples, draw connections, and answer related or extended questions.
            4. Flexible Answering: If the Context is "EMPTY_CONTEXT_NO_DOCUMENTS_RETRIEVED" or does not contain enough information to answer a general, academic, conceptual, or practical question, answer fully and accurately using your pre-trained general knowledge. Do NOT refuse to answer or state that information is missing just because it is not in the Context.
            5. Document-Specific Queries: Only if the user specifically asks about private or unique details of an un-retrieved document (e.g., "What does my uploaded report say in section 2?"), state clearly what is in the Context or politely inform them if that specific document content is missing while offering relevant general knowledge.
            6. Temporal Accuracy: The current system date and time is {currentDate}. Always use this exact current date when answering any time-related questions (such as "today", "current year", "now", or comparing dates). Never claim your knowledge or date is restricted to an old pre-training cutoff date.

            Language Rules (CRITICAL & HIGHEST PRIORITY):
            1. Conversational Language: Analyze the User Question to determine its dominant language (e.g., Vietnamese, English, Japanese, Korean, Spanish, etc.). You MUST write all explanations, analysis, commentary, and conversational phrases strictly in that DOMINANT LANGUAGE.
            2. Target Language Exception (Multilingual Study & Examples): When the user uploads or discusses a document written in or containing foreign language vocabulary, grammar, texts, or code (such as Japanese vocabulary, English grammar, Chinese idioms, programming code, etc.) and asks to create example sentences, explain usage, quote text, or generate exercises:
               - You MUST generate or preserve those vocabulary terms, example sentences, quotes, or technical snippets directly in their ORIGINAL / TARGET LANGUAGE (e.g., Japanese sentences in Japanese with Kanji/Hiragana, English quotes in English).
               - Accompany the target language examples with clear translations and detailed explanations in the dominant conversational language of the User Question.
               - Never force-translate the foreign language example sentences solely into the conversational language!

            Formatting & Summary Rules:
            1. ONLY when the user explicitly requests to summarize a document or topic (using words like "summarize", "tóm tắt", "tóm lược"), structure your response into 4 numbered parts dynamically translated into the dominant language of the User Question:
                1. [Translate heading meaning: Main Topic]
                2. [Translate heading meaning: Purpose]
                3. [Translate heading meaning: Key Points]
                4. [Translate heading meaning: Conclusion]
            2. For all regular questions, conversational messages, explanations, or Q&A that are NOT explicit summary requests, DO NOT structure your answer with those 4 headings. Respond naturally using paragraphs, clear bullet points, or conversational style suitable for the prompt.

            Conversation Rules:
            1. If the user's message is a general conversation (e.g., greetings, introductions, small talk) or general inquiry, answer naturally and helpfully without mentioning the Context.
            
            Context:
            {context}
            
            User Question:
            {question}
            
            IMPORTANT FINAL LANGUAGE OVERRIDE:
            Look strictly at the "User Question" above ("{question}"). Determine its dominant language.
            Write your conversational explanations and commentary strictly in that exact dominant language of "{question}". However, whenever the question asks for example sentences, vocabulary usage, quotes, or exercises related to a foreign language document (e.g. Japanese, English, Chinese, etc.), ALWAYS output the example sentences/quotes in that target foreign language accompanied by explanations/translations in the dominant language of "{question}".
            """;

    private static final String RAG_WITH_HISTORY_PROMPT_TEMPLATE = """
            You are an AI Study Assistant.
            Current System Date & Time: {currentDate}
            
            Language Rules (CRITICAL & HIGHEST PRIORITY):          
            1. Conversational Language: Analyze the Current User Question to determine its dominant language (e.g., Vietnamese, English, Japanese, Korean, Spanish, etc.). You MUST write all explanations, analysis, commentary, and conversational phrases strictly in that DOMINANT LANGUAGE.
            2. Target Language Exception (Multilingual Study & Examples): When the user uploads or discusses a document written in or containing foreign language vocabulary, grammar, texts, or code (such as Japanese vocabulary, English grammar, Chinese idioms, programming code, etc.) and asks to create example sentences, explain usage, quote text, or generate exercises:
               - You MUST generate or preserve those vocabulary terms, example sentences, quotes, or technical snippets directly in their ORIGINAL / TARGET LANGUAGE (e.g., Japanese sentences in Japanese with Kanji/Hiragana, English quotes in English).
               - Accompany the target language examples with clear translations and detailed explanations in the dominant conversational language of the Current User Question.
               - Never force-translate the foreign language example sentences solely into the conversational language!
            
            Knowledge & Flexibility Rules:            
            1. Prioritize Provided Context: Treat the provided Context as the primary reference when answering questions related to user documents.
            2. Combine with General Training Knowledge: You are strongly encouraged to combine the Context with your pre-trained general AI knowledge (knowledge trained by your developers) to provide deeper explanations, analogies, practical examples, or answer related questions that extend beyond the exact wording of the documents.
            3. Flexible Answering: If the Context is "EMPTY_CONTEXT_NO_DOCUMENTS_RETRIEVED" or does not contain enough information for a general or academic study question, answer fully and accurately using your general training knowledge. Do NOT refuse or say "I do not have data" for general study/knowledge questions.
            4. Document-Specific Queries: Only if the user specifically inquires about private/specific content from a document not present in Context, politely inform them that the specific document section is unavailable while offering any relevant general knowledge.
            5. Conversation History: Use Previous Conversation History to maintain conversation flow, understand references ("this", "that", "the previous topic"), and support a natural multi-turn chat experience.
            6. Temporal Accuracy: The current system date and time is {currentDate}. Always use this exact current date when answering any time-related questions (such as "today", "current year", "now"). Never claim your knowledge or date is restricted to an old pre-training cutoff date when discussing dates.
            
            Formatting & Conversation Rules:            
            1. If the user's message is a general greeting or casual conversation (e.g., "Hello", "How are you?", "What is your name?"), respond naturally and warmly without forcing document structure.
            2. ONLY when the user explicitly asks to summarize a document or topic (using words like "summarize", "tóm tắt"), provide 4 sections dynamically translated into the dominant language of the Current User Question:
                2.1 [Translate heading meaning: Main Topic]
                2.2 [Translate heading meaning: Purpose]
                2.3 [Translate heading meaning: Key Points]
                2.4 [Translate heading meaning: Conclusion]
            3. For all other questions or conversational turns, DO NOT structure your answer with those 4 headings. Answer naturally in paragraphs or bullet points appropriate for the question.
            
            Previous Conversation History:
            {history}
            
            Context:
            {context}
            
            Current User Question:
            {question}
            
            IMPORTANT FINAL LANGUAGE OVERRIDE:
            Look strictly at the "Current User Question" above ("{question}"). Determine its dominant language.
            Write your conversational explanations and commentary strictly in that exact dominant language of "{question}". However, whenever the question asks for example sentences, vocabulary usage, quotes, or exercises related to a foreign language document (e.g. Japanese, English, Chinese, etc.), ALWAYS output the example sentences/quotes in that target foreign language accompanied by explanations/translations in the dominant language of "{question}".
            
            Answer:
            """;


    // ==========================================
    //                   CHAT
    // ==========================================

    @Override
    @Transactional
    public ChatResponse askQuestion(ChatRequest request) {
        String question = request.getQuestion();
        log.info("Processing user question: {}", question);

        User currentUser = null;
        try {
            currentUser = SecurityUtils.getCurrentUser();
        } catch (Exception ignored) {
        }

        if (currentUser != null) {
            if (request.getSessionId() != null) {
                return askQuestionInSession(request.getSessionId(), request);
            }

            String title = question.length() > 80 ? question.substring(0, 77) + "..." : question;
            LocalDateTime now = LocalDateTime.now();

            ChatSession session = ChatSession.builder()
                    .user(currentUser)
                    .sessionTitle(title)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            session = chatSessionRepository.save(session);

            if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
                for (Long docId : request.getDocumentIds()) {
                    AiStudyHub.BE.entity.Document document = documentRepo.findById(docId).orElse(null);
                    if (document != null) {
                        ChatSessionDocument sessionDoc = ChatSessionDocument.builder()
                                .session(session)
                                .document(document)
                                .build();
                        chatSessionDocumentRepository.saveAndFlush(sessionDoc);
                    }
                }
            }

            return askQuestionInSession(session.getSessionId(), request);
        }

        List<Document> relevantChunks = retrieveRelevantChunks(question);
        log.info("Retrieved {} relevant chunks from vector store", relevantChunks.size());

        String context;
        List<String> sources;
        if (relevantChunks.isEmpty()) {
            context = "EMPTY_CONTEXT_NO_DOCUMENTS_RETRIEVED";
            sources = List.of();
        } else {
            context = buildContext(relevantChunks);
            sources = relevantChunks.stream()
                    .map(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();
                        if (metadata != null && metadata.containsKey("originalFileName")) {
                            return (String) metadata.get("originalFileName");
                        }
                        return "Unknown Source";
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }

        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss"));
            promptParameters.put("currentDate", currentDate);
            promptParameters.put("context", context);
            promptParameters.put("question", question);
            Prompt prompt = promptTemplate.create(promptParameters);

            log.info("Compiled RAG Context length: {} characters", context.length());
            log.info("Calling OpenAI chat model...");
            String answer = chatClient.prompt(prompt).call().content();
            log.info("Received answer from OpenAI");

            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .build();
        } catch (Exception e) {
            log.error("Error generating answer from LLM", e);
            throw new GlobalException(500, "Failed to generate answer from chat model", e);
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
        return retrieveRelevantChunksWithFilters(question, null);
    }

    private List<Document> retrieveRelevantChunksWithFilters(String question, List<Long> sessionDocIds) {
        try {
            log.info("Searching vector store for query: {}", question);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<Long> accessibleIds;
            if (auth != null && auth.getPrincipal() instanceof User currentUser) {
                accessibleIds = documentRepo.findByOwnerUserIdOrVisibilityStatus(currentUser.getUserId(), VisibilityStatus.PUBLIC)
                        .stream()
                        .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                        .map(AiStudyHub.BE.entity.Document::getDocumentId)
                        .toList();
            } else {
                accessibleIds = documentRepo.findByVisibilityStatus(VisibilityStatus.PUBLIC)
                        .stream()
                        .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                        .map(AiStudyHub.BE.entity.Document::getDocumentId)
                        .toList();
            }

            if (accessibleIds.isEmpty()) {
                log.info("No documents are accessible to the user. Returning empty chunks.");
                return List.of();
            }

            List<Long> targetIds;
            if (sessionDocIds != null) {
                if (sessionDocIds.isEmpty()) {
                    log.info("Session has no attached documents. Returning empty chunks for general AI mode.");
                    return List.of();
                }
                targetIds = sessionDocIds.stream()
                        .filter(accessibleIds::contains)
                        .toList();
            } else {
                targetIds = accessibleIds;
            }

            if (targetIds.isEmpty()) {
                log.info("No matching target documents for similarity search. Returning empty chunks.");
                return List.of();
            }

            List<String> targetIdStrings = targetIds.stream()
                    .map(Object::toString)
                    .toList();

            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            Filter.Expression filterExpression = filterBuilder.in("documentId", targetIdStrings.toArray(new String[0])).build();

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .filterExpression(filterExpression)
                    .similarityThreshold(0.0)
                    .topK(10)
                    .build();
            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("Error searching vector store for query: {}", question, e);
            throw new GlobalException(500, "Failed to search similar chunks in vector store", e);
        }
    }

    // ==========================================
    //               SESSION CHAT
    // ==========================================

    @Override
    @Transactional
    public ChatSessionResponse createSession(CreateSessionRequest request) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        log.info("Creating chat session for user: {}", currentUser.getUserId());

        LocalDateTime now = LocalDateTime.now();
        ChatSession session = ChatSession.builder()
                .user(currentUser)
                .sessionTitle("Cuộc trò chuyện mới")
                .createdAt(now)
                .updatedAt(now)
                .build();
        session = chatSessionRepository.save(session);

        List<Long> documentIds = new ArrayList<>();
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            for (Long docId : request.getDocumentIds()) {
                AiStudyHub.BE.entity.Document document = documentRepo.findById(docId)
                        .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + docId));

                // Security check: must be public or owned by user
                boolean isPublic = document.getVisibilityStatus() == VisibilityStatus.PUBLIC;
                boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
                if (!isPublic && !isOwner) {
                    throw new GlobalException(403, "You do not have permission to access document with ID: " + docId);
                }

                ChatSessionDocument sessionDoc = ChatSessionDocument.builder()
                        .session(session)
                        .document(document)
                        .build();
                chatSessionDocumentRepository.saveAndFlush(sessionDoc);
                documentIds.add(docId);
            }
        }

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionTitle(session.getSessionTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .documentIds(documentIds)
                .build();
    }

    @Override
    public List<ChatSessionResponse> getSessions() {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        log.info("Retrieving chat sessions for user: {}", currentUser.getUserId());

        List<ChatSession> sessions = chatSessionRepository.findByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId());

        return sessions.stream().map(session -> {
            List<Long> documentIds = chatSessionDocumentRepository.findBySessionSessionId(session.getSessionId())
                    .stream()
                    .map(sd -> sd.getDocument().getDocumentId())
                    .collect(Collectors.toList());

            Page<ChatMessage> latestMsgPage = chatMessageRepository.findBySession_SessionIdOrderByCreatedAtDesc(
                    session.getSessionId(), PageRequest.of(0, 1));
            LocalDateTime updatedTime = !latestMsgPage.isEmpty() && latestMsgPage.getContent().get(0).getCreatedAt() != null
                    ? latestMsgPage.getContent().get(0).getCreatedAt()
                    : (session.getUpdatedAt() != null ? session.getUpdatedAt() : session.getCreatedAt());

            return ChatSessionResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionTitle(session.getSessionTitle())
                    .createdAt(session.getCreatedAt())
                    .updatedAt(updatedTime)
                    .documentIds(documentIds)
                    .build();
        })
        .sorted(Comparator.comparing(ChatSessionResponse::getUpdatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
        .collect(Collectors.toList());
    }

    @Override
    public Page<ChatMessageResponse> getSessionMessages(Long sessionId, int page, int size) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Retrieving messages for session: {}", sessionId);

        ChatSession session = chatSessionRepository.findBySessionIdAndUserUserId(sessionId, currentUser.getUserId())
                .orElseThrow(() -> new GlobalException(404, "Chat session not found or you don't have access"));

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> msgPage = chatMessageRepository.findBySession_SessionIdOrderByCreatedAtDesc(sessionId, pageable);

        List<ChatMessageResponse> content = msgPage.getContent().stream()
                .map(msg -> ChatMessageResponse.builder()
                        .messageId(msg.getMessageId())
                        .senderType(msg.getSenderType())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        Collections.reverse(content);

        return new PageImpl<>(content, pageable, msgPage.getTotalElements());
    }

    @Override
    @Transactional
    public ChatResponse deleteSession(Long sessionId) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Deleting session: {}", sessionId);

        ChatSession session = chatSessionRepository.findBySessionIdAndUserUserId(sessionId, currentUser.getUserId())
                .orElseThrow(() -> new GlobalException(404, "Chat session not found or you don't have access"));

        chatSessionRepository.delete(session);

        return ChatResponse.builder()
                .answer("Chat session deleted successfully")
                .build();
    }

    @Override
    @Transactional
    public ChatSessionResponse updateSessionDocuments(Long sessionId, CreateSessionRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Updating documents for session {}: {}", sessionId, request.getDocumentIds());

        ChatSession session = chatSessionRepository.findBySessionIdAndUserUserId(sessionId, currentUser.getUserId())
                .orElseThrow(() -> new GlobalException(404, "Chat session not found or you don't have access"));

        chatSessionDocumentRepository.deleteBySessionSessionId(sessionId);
        chatSessionDocumentRepository.flush();

        List<Long> documentIds = new ArrayList<>();
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            for (Long docId : request.getDocumentIds()) {
                AiStudyHub.BE.entity.Document document = documentRepo.findById(docId)
                        .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + docId));

                boolean isPublic = document.getVisibilityStatus() == VisibilityStatus.PUBLIC;
                boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
                if (!isPublic && !isOwner) {
                    throw new GlobalException(403, "You do not have permission to access document with ID: " + docId);
                }

                ChatSessionDocument sessionDoc = ChatSessionDocument.builder()
                        .session(session)
                        .document(document)
                        .build();
                chatSessionDocumentRepository.saveAndFlush(sessionDoc);
                documentIds.add(docId);
            }
        }

        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionTitle(session.getSessionTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .documentIds(documentIds)
                .build();
    }

    @Override
    @Transactional
    public ChatResponse askQuestionInSession(Long sessionId, ChatRequest request) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        ChatSession session = chatSessionRepository.findBySessionIdAndUserUserId(sessionId, currentUser.getUserId())
                .orElseThrow(() -> new GlobalException(404, "Chat session not found or you don't have access"));

        String question = request.getQuestion();
        log.info("Processing question in session {}: {}", sessionId, question);

        // 1. Get history (top 10 messages) before saving current message
        List<ChatMessage> historyMessages = new ArrayList<>(chatMessageRepository.findTop10BySession_SessionIdOrderByCreatedAtDesc(sessionId));
        Collections.reverse(historyMessages);

        String history = historyMessages.stream()
                .map(msg -> (msg.getSenderType() == SenderType.USER ? "User: " : "AI: ") + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 2. Auto rename if this is the first message
        long messageCount = chatMessageRepository.countBySession_SessionId(sessionId);
        if (messageCount == 0) {
            String title = question.length() > 80 ? question.substring(0, 77) + "..." : question;
            // session is a managed entity — Hibernate will flush this setter
            // at end of @Transactional without touching lazy sessionDocuments collection
            session.setSessionTitle(title);
        }

        // 3. Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .senderType(SenderType.USER)
                .content(question)
                .build();
        chatMessageRepository.save(userMsg);

        // 4. Sync session documents if documentIds is provided in the request
        if (request.getDocumentIds() != null) {
            chatSessionDocumentRepository.deleteBySessionSessionId(sessionId);
            chatSessionDocumentRepository.flush();

            for (Long docId : request.getDocumentIds()) {
                AiStudyHub.BE.entity.Document document = documentRepo.findById(docId).orElse(null);
                if (document != null) {
                    boolean isPublic = document.getVisibilityStatus() == VisibilityStatus.PUBLIC;
                    boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
                    if (isPublic || isOwner) {
                        ChatSessionDocument sessionDoc = ChatSessionDocument.builder()
                                .session(session)
                                .document(document)
                                .build();
                        chatSessionDocumentRepository.saveAndFlush(sessionDoc);
                    }
                }
            }
        }

        // 5. Retrieve session document ids
        List<Long> sessionDocIds = chatSessionDocumentRepository.findBySessionSessionId(sessionId)
                .stream()
                .map(sd -> sd.getDocument().getDocumentId())
                .collect(Collectors.toList());

        // 5. Similarity search filtering by session documents
        List<Document> relevantChunks = retrieveRelevantChunksWithFilters(question, sessionDocIds);
        log.info("Retrieved {} relevant chunks for session {}", relevantChunks.size(), sessionId);

        String context;
        List<String> sources;
        if (relevantChunks.isEmpty()) {
            context = "EMPTY_CONTEXT_NO_DOCUMENTS_RETRIEVED";
            sources = List.of();
        } else {
            context = buildContext(relevantChunks);
            sources = relevantChunks.stream()
                    .map(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();
                        if (metadata != null && metadata.containsKey("originalFileName")) {
                            return (String) metadata.get("originalFileName");
                        }
                        return "Unknown Source";
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }

        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_WITH_HISTORY_PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss"));
            promptParameters.put("currentDate", currentDate);
            promptParameters.put("history", history);
            promptParameters.put("context", context);
            promptParameters.put("question", question);
            Prompt prompt = promptTemplate.create(promptParameters);

            log.info("Calling OpenAI model with history and context...");
            String answer = chatClient.prompt(prompt).call().content();

            // 6. Save AI answer to DB
            ChatMessage aiMsg = ChatMessage.builder()
                    .session(session)
                    .senderType(SenderType.AI)
                    .content(answer)
                    .build();
            aiMsg = chatMessageRepository.save(aiMsg);

            session.setUpdatedAt(aiMsg.getCreatedAt() != null ? aiMsg.getCreatedAt() : LocalDateTime.now());
            chatSessionRepository.save(session);

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .sessionTitle(session.getSessionTitle())
                    .createdAt(session.getCreatedAt())
                    .updatedAt(session.getUpdatedAt())
                    .documentIds(sessionDocIds)
                    .answer(answer)
                    .sources(sources)
                    .build();
        } catch (Exception e) {
            log.error("Error generating answer from LLM in session", e);
            throw new GlobalException(500, "Failed to generate answer from chat model", e);
        }
    }


    // ==========================================
    //                 DOCUMENT
    // ==========================================

    private void checkAuthorization(AiStudyHub.BE.entity.Document mainDoc, String action) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("AD");
        boolean isOwner = mainDoc.getOwner().getUserId().equals(currentUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new GlobalException(403, "You do not have permission to " + action + " this document");
        }
    }

    @Override
    @Transactional
    public RagDocumentResponse indexDocument(Long documentId) {
        log.info("Triggering manual indexing for document ID: {}", documentId);

        AiStudyHub.BE.entity.Document mainDoc = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + documentId));

        checkAuthorization(mainDoc, "index");

        RagDocument ragDoc = ragDocumentRepository.findByDocumentDocumentId(documentId)
                .orElse(null);

        if (ragDoc == null) {
            ragDoc = RagDocument.builder()
                    .document(mainDoc)
                    .originalFileName(mainDoc.getFileName())
                    .contentType(mainDoc.getFileType())
                    .fileSize(mainDoc.getFileSize())
                    .uploadedBy(mainDoc.getOwner().getEmail())
                    .status("PENDING")
                    .build();
            ragDoc = ragDocumentRepository.save(ragDoc);
        }

        if (mainDoc.getFileUrl() == null || mainDoc.getFileUrl().isEmpty()) {
            throw new GlobalException(400, "Main document file URL is empty");
        }

        try {
            log.info("Downloading document content via Supabase Storage service from URL: {}", mainDoc.getFileUrl());
            byte[] fileBytes = supabaseStorageService.downloadFile(mainDoc.getFileUrl());
            
            cleanExistingRagResources(ragDoc);
            indexDocumentContent(ragDoc, fileBytes);
            ragDoc.setStatus("INDEXED");
            ragDoc = ragDocumentRepository.save(ragDoc);
        } catch (Exception e) {
            log.error("Failed to manually index document: {}", mainDoc.getFileName(), e);
            ragDoc.setStatus("FAILED");
            ragDocumentRepository.save(ragDoc);
            throw new GlobalException(500, "Failed to index document contents", e);
        }

        return ragDocumentMapper.toRagDocumentResponse(ragDoc);
    }

    @Override
    @Transactional
    public DeleteResponse deleteDocument(Long documentId) {
        log.info("Deleting RAG resources for document ID: {}", documentId);
        
        AiStudyHub.BE.entity.Document mainDoc = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + documentId));

        checkAuthorization(mainDoc, "delete RAG index for");

        RagDocument document = ragDocumentRepository.findByDocumentDocumentId(documentId)
                .orElseThrow(() -> new GlobalException(404, "RagDocument not found with document ID: " + documentId));

        cleanExistingRagResources(document);
        ragDocumentRepository.delete(document);
        log.info("Deleted RagDocument metadata");
        return ragDocumentMapper.toDeleteResponse(document, LocalDateTime.now());
    }

    @Override
    public RagDocumentResponse getDocument(Long documentId) {
        log.info("Retrieving RAG document info for ID: {}", documentId);

        AiStudyHub.BE.entity.Document mainDoc = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + documentId));

        boolean isPublic = mainDoc.getVisibilityStatus() == VisibilityStatus.PUBLIC;
        if (!isPublic) {
            checkAuthorization(mainDoc, "view");
        }

        RagDocument document = ragDocumentRepository.findByDocumentDocumentId(documentId)
                .orElseThrow(() -> new GlobalException(404, "RagDocument not found with document ID: " + documentId));
        return ragDocumentMapper.toRagDocumentResponse(document);
    }

    @Transactional
    public boolean indexDocumentContent(RagDocument document, byte[] fileBytes) {
        log.info("Extracting and indexing text for RagDocument ID: {}", document.getId());

        try {
            Resource resource = new ByteArrayResource(fileBytes);
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            List<Document> documents = tikaReader.read();

            if (documents.isEmpty()) {
                log.warn("No text extracted from document ID: {}", document.getId());
                return false;
            }

            String fullText = documents.stream()
                    .map(Document::getText)
                    .filter(text -> text != null && !text.trim().isEmpty())
                    .collect(Collectors.joining("\n"));

            if (fullText.trim().isEmpty()) {
                log.warn("Extracted text is empty for document ID: {}, skipping vector store indexing.", document.getId());
                return false;
            }

            Document parentDoc = new Document(fullText);
            List<Document> chunks = textSplitter.apply(List.of(parentDoc));
            log.info("Split document ID: {} into {} chunks", document.getId(), chunks.size());

            if (chunks.isEmpty()) {
                log.warn("No text chunks generated for document ID: {}, skipping vector store indexing.", document.getId());
                return false;
            }

            List<RagChunk> ragChunks = new ArrayList<>();
            List<Document> docsToVectorStore = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);

                RagChunk ragChunk = RagChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunk.getText())
                        .embeddingCreated(true)
                        .build();
                ragChunks.add(ragChunk);

                Map<String, Object> metadata = new HashMap<>();
                Long linkedDocumentId = document.getDocument() != null ? document.getDocument().getDocumentId() : null;
                metadata.put("documentId", linkedDocumentId != null ? linkedDocumentId.toString() : document.getId().toString());
                metadata.put("originalFileName", document.getOriginalFileName());
                metadata.put("uploadedBy", document.getUploadedBy());
                metadata.put("chunkIndex", i);

                String vectorId = UUID.nameUUIDFromBytes((document.getId().toString() + "_" + i).getBytes()).toString();
                Document vectorDoc = new Document(vectorId, chunk.getText(), metadata);
                docsToVectorStore.add(vectorDoc);
            }

            ragChunkRepository.saveAll(ragChunks);
            log.info("Sending chunks to Qdrant vector store...");
            vectorStore.add(docsToVectorStore);
            log.info("Successfully indexed chunks in Qdrant.");
            return true;

        } catch (Exception e) {
            log.error("Error during ETL index process for document ID {}", document.getId(), e);
            throw new GlobalException(500, "Failed to parse and index document content", e);
        }
    }

    private void cleanExistingRagResources(RagDocument document) {
        List<RagChunk> chunks = ragChunkRepository.findByDocumentId(document.getId());
        if (!chunks.isEmpty()) {
            List<String> vectorIds = chunks.stream()
                    .map(chunk -> UUID.nameUUIDFromBytes((document.getId().toString() + "_" + chunk.getChunkIndex()).getBytes()).toString())
                    .toList();
            try {
                log.info("Deleting {} existing vectors from Qdrant for document ID: {}...", vectorIds.size(), document.getId());
                vectorStore.delete(vectorIds);
                log.info("Successfully deleted existing vectors from Qdrant");
            } catch (Exception e) {
                log.error("Failed to delete vectors from Qdrant for document ID: {}", document.getId(), e);
                throw new GlobalException(500, "Failed to delete vectors from Qdrant", e);
            }

            ragChunkRepository.deleteByDocumentId(document.getId());
            log.info("Deleted existing chunks from database");
        }
    }

    @Override
    public List<String> suggestPrompts(SuggestPromptsRequest request) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        List<Long> docIds = request.getDocumentIds();
        log.info("Generating prompt suggestions for user {} on documents {}", currentUser.getUserId(), docIds);

        StringBuilder contextBuilder = new StringBuilder();
        for (Long docId : docIds) {
            AiStudyHub.BE.entity.Document document = documentRepo.findById(docId)
                    .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + docId));

            // Check document access permissions
            boolean isPublic = document.getVisibilityStatus() == VisibilityStatus.PUBLIC;
            boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
            if (!isPublic && !isOwner) {
                throw new GlobalException(403, "You do not have permission to access document with ID: " + docId);
            }

            // Retrieve RagDocument and chunks
            RagDocument ragDoc = ragDocumentRepository.findByDocumentDocumentId(docId)
                    .orElseThrow(() -> new GlobalException(404, "RagDocument not found for document ID: " + docId));
            
            // Get up to 5 chunks from each document to form the context
            List<RagChunk> chunks = ragDoc.getChunks();
            if (chunks != null) {
                chunks.stream()
                      .sorted(Comparator.comparing(RagChunk::getChunkIndex))
                      .limit(5)
                      .forEach(chunk -> {
                          if (contextBuilder.length() < 12000) { // Limit total context size
                              contextBuilder.append(chunk.getContent()).append("\n");
                          }
                      });
            }
        }

        String context = contextBuilder.toString().trim();
        if (context.isEmpty()) {
            throw new GlobalException(400, "Documents do not have indexed content for suggestion.");
        }

        String promptText = """
                You are an AI Study Assistant.
                Based on the provided document context below, generate exactly 5 relevant, diverse, and specific suggested prompts (questions or requests) that a student might want to ask to study or test themselves on this material.
                
                Rules:
                1. The suggestions must be directly relevant to the document content.
                2. The output MUST be a JSON array of strings, containing exactly 5 prompts. Do not include any markdown formatting (like ```json), explanations, or additional text.
                3. The prompts must be in the dominant language of the document context (e.g. Vietnamese if the text is in Vietnamese, English if the text is in English).
                
                Example Output:
                ["Giải thích khái niệm tính kế thừa kèm ví dụ", "Tính đa hình trong Java là gì?", "Sự khác biệt giữa abstract class và interface", "Tạo một sơ đồ lớp UML cho hệ thống thư viện", "Tạo câu hỏi trắc nghiệm về OOP trong Java"]
                
                Document Context:
                {context}
                """;

        try {
            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Map<String, Object> params = Map.of("context", context);
            Prompt prompt = promptTemplate.create(params);

            log.info("Calling LLM to generate suggested prompts...");
            String responseContent = chatClient.prompt(prompt).call().content();
            
            // Clean up and parse JSON response
            if (responseContent != null) {
                responseContent = responseContent.trim();
                if (responseContent.startsWith("```json")) {
                    responseContent = responseContent.substring(7);
                }
                if (responseContent.startsWith("```")) {
                    responseContent = responseContent.substring(3);
                }
                if (responseContent.endsWith("```")) {
                    responseContent = responseContent.substring(0, responseContent.length() - 3);
                }
                responseContent = responseContent.trim();

                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(responseContent, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            }
            
            throw new GlobalException(500, "AI returned empty response");
        } catch (Exception e) {
            log.error("Error generating suggested prompts from LLM", e);
            // Return default prompts if parsing fails to avoid breaking user experience
            return List.of(
                "Summarize the main content of this document",
                "Explain the key concepts in the document",
                "Create study quiz questions from the material",
                "List key terms and definitions",
                "Create a mind map or section summary"
            );
        }
    }
}