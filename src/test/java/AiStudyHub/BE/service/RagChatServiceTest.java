package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.SenderType;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.ChatRequest;
import AiStudyHub.BE.dto.Request.CreateSessionRequest;
import AiStudyHub.BE.dto.Response.ChatResponse;
import AiStudyHub.BE.dto.Response.ChatSessionResponse;
import AiStudyHub.BE.dto.Response.ChatMessageResponse;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.RagSystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagChatServiceTest {

    @Mock
    private VectorStore vectorStore;
    @Mock
    private ChatClient chatClient;
    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private RagDocumentRepository ragDocumentRepository;
    @Mock
    private RagChunkRepository ragChunkRepository;
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatSessionDocumentRepository chatSessionDocumentRepository;

    @InjectMocks
    private RagSystemService ragSystemService;

    private User currentUser;
    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currentUser = User.builder()
                .userId(1L)
                .email("testuser@test.com")
                .fullName("Test User")
                .build();

        testSession = ChatSession.builder()
                .sessionId(100L)
                .user(currentUser)
                .sessionTitle("Cuộc trò chuyện mới")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateSession_Success() {
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> securityUtilsMock = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            securityUtilsMock.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(currentUser);

            when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

            CreateSessionRequest request = CreateSessionRequest.builder()
                    .documentIds(new ArrayList<>())
                    .build();

            ChatSessionResponse response = ragSystemService.createSession(request);

            assertNotNull(response);
            assertEquals(100L, response.getSessionId());
            assertEquals("Cuộc trò chuyện mới", response.getSessionTitle());
            verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
        }
    }

    @Test
    void testGetSessions_Success() {
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> securityUtilsMock = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            securityUtilsMock.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(currentUser);

            when(chatSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId()))
                    .thenReturn(List.of(testSession));
            when(chatSessionDocumentRepository.findBySession_SessionId(100L))
                    .thenReturn(List.of());

            List<ChatSessionResponse> responses = ragSystemService.getSessions();

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(100L, responses.get(0).getSessionId());
        }
    }

    @Test
    void testGetSessionMessages_Success() {
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> securityUtilsMock = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            securityUtilsMock.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(currentUser);

            when(chatSessionRepository.findBySessionIdAndUser_UserId(100L, currentUser.getUserId()))
                    .thenReturn(Optional.of(testSession));

            ChatMessage m1 = ChatMessage.builder().messageId(1L).senderType(SenderType.USER).content("Hello").createdAt(LocalDateTime.now()).build();
            ChatMessage m2 = ChatMessage.builder().messageId(2L).senderType(SenderType.AI).content("Hi").createdAt(LocalDateTime.now().plusSeconds(1)).build();

            Page<ChatMessage> dbPage = new PageImpl<>(List.of(m2, m1), PageRequest.of(0, 20), 2);
            when(chatMessageRepository.findBySession_SessionIdOrderByCreatedAtDesc(eq(100L), any()))
                    .thenReturn(dbPage);

            Page<ChatMessageResponse> resultPage = ragSystemService.getSessionMessages(100L, 0, 20);

            assertNotNull(resultPage);
            assertEquals(2, resultPage.getContent().size());
            assertEquals("Hello", resultPage.getContent().get(0).getContent());
            assertEquals("Hi", resultPage.getContent().get(1).getContent());
        }
    }

    @Test
    void testDeleteSession_Success() {
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> securityUtilsMock = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            securityUtilsMock.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(currentUser);

            when(chatSessionRepository.findBySessionIdAndUser_UserId(100L, currentUser.getUserId()))
                    .thenReturn(Optional.of(testSession));

            ragSystemService.deleteSession(100L);

            verify(chatSessionRepository, times(1)).delete(testSession);
        }
    }

    @Test
    void testAskQuestionInSession_Success() {
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> securityUtilsMock = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            securityUtilsMock.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(currentUser);

            // Mock SecurityContextHolder
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(currentUser);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            // Mock DB & Repositories
            when(chatSessionRepository.findBySessionIdAndUser_UserId(100L, currentUser.getUserId()))
                    .thenReturn(Optional.of(testSession));
            when(chatMessageRepository.findTop10BySession_SessionIdOrderByCreatedAtDesc(100L))
                    .thenReturn(new ArrayList<>());
            when(chatMessageRepository.countBySession_SessionId(100L))
                    .thenReturn(0L); // First message
            when(chatSessionDocumentRepository.findBySession_SessionId(100L))
                    .thenReturn(new ArrayList<>());

            // Mock DocumentRepo accessible docs
            AiStudyHub.BE.entity.Document mainDoc = AiStudyHub.BE.entity.Document.builder()
                    .documentId(200L)
                    .fileName("test.pdf")
                    .uploadStatus(AiStudyHub.BE.constraint.UploadStatus.COMPLETED)
                    .owner(currentUser)
                    .visibilityStatus(VisibilityStatus.PRIVATE)
                    .build();
            when(documentRepo.findByOwnerUserIdOrVisibilityStatus(currentUser.getUserId(), VisibilityStatus.PUBLIC))
                    .thenReturn(List.of(mainDoc));

            // Mock similaritySearch
            org.springframework.ai.document.Document chunk = new org.springframework.ai.document.Document("chunk text", Map.of("originalFileName", "test.pdf"));
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));

            // Mock fluent ChatClient API
            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
            when(chatClient.prompt(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(responseSpec);
            when(responseSpec.content()).thenReturn("AI Answer regarding test.pdf");

            ChatRequest chatRequest = ChatRequest.builder()
                    .question("Summarize test.pdf")
                    .build();

            ChatResponse response = ragSystemService.askQuestionInSession(100L, chatRequest);

            assertNotNull(response);
            assertEquals("AI Answer regarding test.pdf", response.getAnswer());
            assertTrue(response.getSources().contains("test.pdf"));

            // Check auto-renaming: title should be updated to question
            verify(chatSessionRepository, times(1)).save(testSession);
            assertEquals("Summarize test.pdf", testSession.getSessionTitle());

            // Check save user and AI messages in DB
            verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
