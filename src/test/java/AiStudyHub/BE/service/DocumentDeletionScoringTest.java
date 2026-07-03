package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ScoreTypeCode;
import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.IStorageService;
import AiStudyHub.BE.service.ISupabaseStorage;
import AiStudyHub.BE.service.IRagSystem;
import AiStudyHub.BE.service.impl.DocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentDeletionScoringTest {

    @Mock private DocumentRepo documentRepo;
    @Mock private UserRepo userRepo;
    @Mock private ScoreLogRepo scoreLogRepo;
    @Mock private BookmarkRepo bookmarkRepo;
    @Mock private RatingRepo ratingRepo;
    @Mock private DownloadRepo downloadRepo;
    @Mock private ReportRepo reportRepo;
    @Mock private ReportCaseRepo reportCaseRepo;
    @Mock private NotificationRepo notificationRepo;
    @Mock private ChatSessionDocumentRepo chatSessionDocumentRepo;
    @Mock private RagDocumentRepository ragDocumentRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private IStorageService storageService;
    @Mock private ISupabaseStorage supabaseStorageService;
    @Mock private IRagSystem ragSystemService;
    @Mock private IGamification gamificationService;

    @InjectMocks
    private DocumentService documentService;

    private User owner;
    private Document document;
    private List<ScoreLog> bookmarkLogs;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .userId(1L)
                .fullName("Owner User")
                .role(UserRole.US)
                .totalScore(10L)
                .build();

        document = Document.builder()
                .documentId(100L)
                .title("Spring Guide")
                .owner(owner)
                .fileSize(500L)
                .fileUrl("http://supabase.com/file")
                .build();

        ScoreLog log1 = ScoreLog.builder()
                .scoreLogId(1001L)
                .documentId(100L)
                .scoreChange(3)
                .build();

        ScoreLog log2 = ScoreLog.builder()
                .scoreLogId(1002L)
                .documentId(100L)
                .scoreChange(3)
                .build();

        bookmarkLogs = Arrays.asList(log1, log2);

        // Setup security context mock
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteDocument_shouldDeductPointsAndCleanLogs() throws Exception {
        // Arrange
        when(documentRepo.findById(eq(100L))).thenReturn(Optional.of(document));
        when(documentMapper.toDeleteResponse(any(), any())).thenReturn(DeleteResponse.builder().success(true).message("Document deleted successfully").build());
        when(ragDocumentRepository.findByDocumentDocumentId(eq(100L))).thenReturn(Optional.empty());
        when(documentRepo.deleteByDocumentId(eq(100L))).thenReturn(1L);

        when(scoreLogRepo.findByDocumentIdAndScoreTypeTypeCode(eq(100L), eq(ScoreTypeCode.BOOKMARK.name())))
                .thenReturn(bookmarkLogs);

        // Act
        DeleteResponse response = documentService.deleteDocument(100L);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(4L, owner.getTotalScore()); // 10L - (3+3) = 4L
        verify(userRepo, times(2)).save(owner); // decreaseStorage and decrease totalScore
        verify(scoreLogRepo, times(1)).deleteAll(bookmarkLogs);
        verify(gamificationService, times(1)).addWeeklyScore(eq(1L), eq(-6));
        verify(gamificationService, times(1)).updateUserRank(eq(1L));
        verify(supabaseStorageService, times(1)).deleteFile(eq("http://supabase.com/file"));
    }
}
