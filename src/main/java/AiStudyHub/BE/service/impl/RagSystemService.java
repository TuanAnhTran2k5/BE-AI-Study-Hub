package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.constraint.SenderType;
import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Request.CreateSessionRequest;
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

import AiStudyHub.BE.mapper.RagDocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagChunkRepository;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.ChatSessionRepository;
import AiStudyHub.BE.repository.ChatMessageRepository;
import AiStudyHub.BE.repository.ChatSessionDocumentRepository;
import AiStudyHub.BE.service.IRagSystem;
import AiStudyHub.BE.service.ISupabaseStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    private static final String RAG_WITH_HISTORY_PROMPT_TEMPLATE = """
            Bạn là một trợ lý học tập AI hữu ích.
            
            Lịch sử cuộc hội thoại trước đó:
            {history}
            
            Ngữ cảnh lấy từ tài liệu:
            {context}
            
            Câu hỏi hiện tại của người dùng:
            {question}
            
            Trả lời:
            """;


    // ==========================================
    //                   CHAT
    // ==========================================

    @Override
    public ChatResponse askQuestion(ChatRequest request) {
        String question = request.getQuestion();
        log.info("Processing user question: {}", question);

        List<Document> relevantChunks = retrieveRelevantChunks(question);
        log.info("Retrieved {} relevant chunks from vector store", relevantChunks.size());

        if (relevantChunks.isEmpty()) {
            return ChatResponse.builder()
                    .answer("No relevant information found in the documents to answer this question.")
                    .sources(List.of())
                    .build();
        }

        String context = buildContext(relevantChunks);

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

        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("context", context);
            promptParameters.put("question", question);
            org.springframework.ai.chat.prompt.Prompt prompt = promptTemplate.create(promptParameters);

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
            if (sessionDocIds != null && !sessionDocIds.isEmpty()) {
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
            Filter.Expression filterExpression = filterBuilder.in("documentId", targetIdStrings.toArray()).build();

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .filterExpression(filterExpression)
                    .topK(5)
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

        ChatSession session = ChatSession.builder()
                .user(currentUser)
                .sessionTitle("Cuộc trò chuyện mới")
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
                chatSessionDocumentRepository.save(sessionDoc);
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

        List<ChatSession> sessions = chatSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId());

        return sessions.stream().map(session -> {
            List<Long> documentIds = chatSessionDocumentRepository.findBySession_SessionId(session.getSessionId())
                    .stream()
                    .map(sd -> sd.getDocument().getDocumentId())
                    .collect(Collectors.toList());

            return ChatSessionResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionTitle(session.getSessionTitle())
                    .createdAt(session.getCreatedAt())
                    .updatedAt(session.getUpdatedAt())
                    .documentIds(documentIds)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public Page<ChatMessageResponse> getSessionMessages(Long sessionId, int page, int size) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        log.info("Retrieving messages for session: {}", sessionId);

        ChatSession session = chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, currentUser.getUserId())
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
    public void deleteSession(Long sessionId) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        log.info("Deleting session: {}", sessionId);

        ChatSession session = chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, currentUser.getUserId())
                .orElseThrow(() -> new GlobalException(404, "Chat session not found or you don't have access"));

        chatSessionRepository.delete(session);
    }

    @Override
    @Transactional
    public ChatResponse askQuestionInSession(Long sessionId, ChatRequest request) {
        User currentUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
        ChatSession session = chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, currentUser.getUserId())
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
            session.setSessionTitle(title);
            chatSessionRepository.save(session);
        }

        // 3. Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .senderType(SenderType.USER)
                .content(question)
                .build();
        chatMessageRepository.save(userMsg);

        // 4. Retrieve session document ids
        List<Long> sessionDocIds = chatSessionDocumentRepository.findBySession_SessionId(sessionId)
                .stream()
                .map(sd -> sd.getDocument().getDocumentId())
                .collect(Collectors.toList());

        // 5. Similarity search filtering by session documents
        List<Document> relevantChunks = retrieveRelevantChunksWithFilters(question, sessionDocIds);
        log.info("Retrieved {} relevant chunks for session {}", relevantChunks.size(), sessionId);

        if (relevantChunks.isEmpty()) {
            String aiAnswer = "No relevant information found in the documents to answer this question.";
            ChatMessage aiMsg = ChatMessage.builder()
                    .session(session)
                    .senderType(SenderType.AI)
                    .content(aiAnswer)
                    .build();
            chatMessageRepository.save(aiMsg);

            return ChatResponse.builder()
                    .answer(aiAnswer)
                    .sources(List.of())
                    .build();
        }

        String context = buildContext(relevantChunks);

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

        try {
            PromptTemplate promptTemplate = new PromptTemplate(RAG_WITH_HISTORY_PROMPT_TEMPLATE);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("history", history);
            promptParameters.put("context", context);
            promptParameters.put("question", question);
            org.springframework.ai.chat.prompt.Prompt prompt = promptTemplate.create(promptParameters);

            log.info("Calling OpenAI model with history and context...");
            String answer = chatClient.prompt(prompt).call().content();

            // 6. Save AI answer to DB
            ChatMessage aiMsg = ChatMessage.builder()
                    .session(session)
                    .senderType(SenderType.AI)
                    .content(answer)
                    .build();
            chatMessageRepository.save(aiMsg);

            return ChatResponse.builder()
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

        RagDocument ragDoc = ragDocumentRepository.findByDocument_DocumentId(documentId)
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

        RagDocument document = ragDocumentRepository.findByDocument_DocumentId(documentId)
                .orElseThrow(() -> new GlobalException(404, "RagDocument not found with document ID: " + documentId));

        cleanExistingRagResources(document);
        ragDocumentRepository.delete(document);
        log.info("Deleted RagDocument metadata");
        return ragDocumentMapper.toDeleteResponse(document, java.time.LocalDateTime.now());
    }

    @Override
    public RagDocumentResponse getDocument(Long documentId) {
        log.info("Retrieving RAG document info for ID: {}", documentId);

        AiStudyHub.BE.entity.Document mainDoc = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found with ID: " + documentId));

        boolean isPublic = mainDoc.getVisibilityStatus() == AiStudyHub.BE.constraint.VisibilityStatus.PUBLIC;
        if (!isPublic) {
            checkAuthorization(mainDoc, "view");
        }

        RagDocument document = ragDocumentRepository.findByDocument_DocumentId(documentId)
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
                    .collect(Collectors.joining("\n"));

            Document parentDoc = new Document(fullText);
            List<Document> chunks = textSplitter.apply(List.of(parentDoc));
            log.info("Split document ID: {} into {} chunks", document.getId(), chunks.size());

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
}
