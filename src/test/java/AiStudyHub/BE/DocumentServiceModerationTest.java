package AiStudyHub.BE;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.dto.Response.DocumentResponse;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.DocumentService;
import AiStudyHub.BE.service.impl.DocumentRagIndexer;
import AiStudyHub.BE.service.*;
import AiStudyHub.BE.utils.SimHashUtil;
import AiStudyHub.BE.utils.TextExtractionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceModerationTest {

    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private SubjectRepo subjectRepo;
    @Mock
    private DownloadRepo downloadRepo;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private IStorageService storageService;
    @Mock
    private ISupabaseStorage supabaseStorageService;
    @Mock
    private IDuplicateCheck duplicateCheckService;
    @Mock
    private IGamification gamificationService;
    @Mock
    private DocumentRagIndexer documentRagIndexer;
    @Mock
    private RagDocumentRepository ragDocumentRepository;
    @Mock
    private IRagSystem ragSystemService;
    @Mock
    private RatingRepo ratingRepo;
    @Mock
    private BookmarkRepo bookmarkRepo;
    @Mock
    private ReportRepo reportRepo;
    @Mock
    private ReportCaseRepo reportCaseRepo;
    @Mock
    private ScoreLogRepo scoreLogRepo;
    @Mock
    private NotificationRepo notificationRepo;
    @Mock
    private ChatSessionDocumentRepo chatSessionDocumentRepo;
    @Mock
    private IUser userService;
    @Mock
    private INotification notificationService;
    @Mock
    private SimHashUtil simHashUtil;
    @Mock
    private TextExtractionUtil textExtractionUtil;

    @InjectMocks
    private DocumentService documentService;

    private User ownerUser;
    private User adminUser;
    private User anotherAdminUser;
    private User otherUser;
    private Document normalDoc;
    private Document hiddenDoc;
    private Document deletedDoc;

    @BeforeEach
    public void setUp() {
        ownerUser = User.builder()
                .userId(100L)
                .fullName("Owner Student")
                .email("owner@aistudyhub.com")
                .role(UserRole.US)
                .build();

        otherUser = User.builder()
                .userId(101L)
                .fullName("Other Student")
                .email("other@aistudyhub.com")
                .role(UserRole.US)
                .build();

        adminUser = User.builder()
                .userId(200L)
                .fullName("Moderator Admin")
                .email("admin@aistudyhub.com")
                .role(UserRole.AD)
                .build();

        anotherAdminUser = User.builder()
                .userId(201L)
                .fullName("Senior Admin")
                .email("senior-admin@aistudyhub.com")
                .role(UserRole.AD)
                .build();

        normalDoc = Document.builder()
                .documentId(1L)
                .owner(ownerUser)
                .title("Normal Study Materials")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.NORMAL)
                .build();

        hiddenDoc = Document.builder()
                .documentId(2L)
                .owner(ownerUser)
                .title("Spam Study Materials")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.HIDDEN)
                .build();

        deletedDoc = Document.builder()
                .documentId(3L)
                .owner(ownerUser)
                .title("Deleted Study Materials")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.NORMAL)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    private void mockMapper() {
        lenient().when(documentMapper.toDocumentResponse(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            return DocumentResponse.builder()
                    .documentId(doc.getDocumentId())
                    .title(doc.getTitle())
                    .visibilityStatus(doc.getVisibilityStatus())
                    .moderationStatus(doc.getModerationStatus())
                    .uploadStatus(doc.getUploadStatus())
                    .ownerId(doc.getOwner() != null ? doc.getOwner().getUserId() : null)
                    .build();
        });
    }

    @Test
    public void testSearchDocumentsByTitle_FiltersOutNonNormalAndDeleted() {
        // Arrange
        mockMapper();
        when(documentRepo.findByTitleContainingIgnoreCase("Study")).thenReturn(
                Arrays.asList(normalDoc, hiddenDoc, deletedDoc)
        );

        // Act
        List<DocumentResponse> results = documentService.searchDocumentsByTitle("Study");

        // Assert
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getDocumentId());
        assertEquals("Normal Study Materials", results.get(0).getTitle());
    }

    @Test
    public void testGetDocumentDetail_OwnerAccess_Success() {
        // Arrange
        mockMapper();
        when(documentRepo.findById(2L)).thenReturn(Optional.of(hiddenDoc));
        
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(ownerUser);
            
            // Act
            DocumentResponse result = documentService.getDocumentDetail(2L);
            
            // Assert
            assertNotNull(result);
            assertEquals(2L, result.getDocumentId());
            assertEquals(ModerationStatus.HIDDEN, result.getModerationStatus());
        }
    }

    @Test
    public void testGetDocumentDetail_AdminAccess_Success() {
        // Arrange
        mockMapper();
        when(documentRepo.findById(2L)).thenReturn(Optional.of(hiddenDoc));
        
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(adminUser);
            
            // Act
            DocumentResponse result = documentService.getDocumentDetail(2L);
            
            // Assert
            assertNotNull(result);
            assertEquals(2L, result.getDocumentId());
        }
    }

    @Test
    public void testGetDocumentDetail_GuestAccess_ThrowsNotFound() {
        // Arrange
        when(documentRepo.findById(2L)).thenReturn(Optional.of(hiddenDoc));
        
        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(otherUser);
            
            // Act & Assert
            GlobalException exception = assertThrows(GlobalException.class, () -> {
                documentService.getDocumentDetail(2L);
            });
            
            assertEquals(HttpStatus.NOT_FOUND.value(), exception.getCode());
            assertEquals("Document not found", exception.getMessage());
            
            // Confirm mapping was NEVER called for unauthorized guest, avoiding PII leak
            verify(documentMapper, never()).toDocumentResponse(any());
        }
    }

    @Test
    public void testGetDocumentDetail_LatestResolvedCaseSelected() {
        // Arrange
        mockMapper();
        when(documentRepo.findById(2L)).thenReturn(Optional.of(hiddenDoc));
        
        ReportCase oldCase = ReportCase.builder()
                .caseId(10L)
                .caseStatus(CaseStatus.RESOLVED)
                .resolvedBy(adminUser)
                .resolvedAt(LocalDateTime.now().minusDays(1))
                .adminNote("Old Note")
                .build();

        ReportCase latestCase = ReportCase.builder()
                .caseId(11L)
                .caseStatus(CaseStatus.RESOLVED)
                .resolvedBy(anotherAdminUser)
                .resolvedAt(LocalDateTime.now())
                .adminNote("Latest Note")
                .build();
                
        when(reportCaseRepo.findByDocumentDocumentIdOrderByResolvedAtDesc(2L)).thenReturn(
                Arrays.asList(latestCase, oldCase)
        );

        try (MockedStatic<AiStudyHub.BE.security.SecurityUtils> mockedSecurity = mockStatic(AiStudyHub.BE.security.SecurityUtils.class)) {
            mockedSecurity.when(AiStudyHub.BE.security.SecurityUtils::getCurrentUser).thenReturn(ownerUser);
            
            // Act
            DocumentResponse result = documentService.getDocumentDetail(2L);
            
            // Assert
            assertNotNull(result);
            assertEquals("senior-admin@aistudyhub.com", result.getModeratedByEmail());
            assertEquals("Latest Note", result.getModerationNote());
        }
    }
}
