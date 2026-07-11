package AiStudyHub.BE;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.ReportService;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.service.IGamification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceAppealTest {

    @Mock
    private ReportRepo reportRepo;
    @Mock
    private ReportCaseRepo reportCaseRepo;
    @Mock
    private ReportReasonRepo reportReasonRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private ScoreLogRepo scoreLogRepo;
    @Mock
    private ScoreTypeRepo scoreTypeRepo;
    @Mock
    private INotification notificationService;
    @Mock
    private IGamification rankingBadgeService;
    @Mock
    private AppealRepo appealRepo;

    @InjectMocks
    private ReportService reportService;

    private User regularUser;
    private User adminUser;
    private Document document;
    private ReportCase reportCase;
    private Appeal appeal;

    @BeforeEach
    public void setUp() {
        regularUser = User.builder()
                .userId(2L)
                .fullName("Author User")
                .email("author@studyhub.com")
                .role(UserRole.US)
                .status(UserStatus.ACTIVE)
                .totalScore(100L)
                .build();

        adminUser = User.builder()
                .userId(1L)
                .fullName("Admin User")
                .email("admin@studyhub.com")
                .role(UserRole.AD)
                .status(UserStatus.ACTIVE)
                .build();

        document = Document.builder()
                .documentId(10L)
                .title("Test Document")
                .owner(regularUser)
                .moderationStatus(ModerationStatus.REMOVED)
                .build();

        reportCase = ReportCase.builder()
                .caseId(20L)
                .document(document)
                .caseStatus(CaseStatus.RESOLVED)
                .reason(ReportReason.builder().reasonId(5L).reasonName("Copyright").build())
                .firstWarningAt(LocalDateTime.now())
                .build();

        appeal = Appeal.builder()
                .appealId(30L)
                .reportCase(reportCase)
                .user(regularUser)
                .appealReason("Oan sai quá")
                .status(AppealStatus.PENDING)
                .build();
    }

    @Test
    public void testSubmitAppeal_Success() {
        when(reportCaseRepo.findByCaseId(20L)).thenReturn(Optional.of(reportCase));
        when(userRepo.findById(2L)).thenReturn(Optional.of(regularUser));
        when(appealRepo.existsByReportCaseCaseIdAndStatus(20L, AppealStatus.PENDING)).thenReturn(false);
        when(appealRepo.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appeal result = reportService.submitAppeal(20L, 2L, "Oan sai quá", "http://evidence.url");

        assertNotNull(result);
        assertEquals("Oan sai quá", result.getAppealReason());
        assertEquals(AppealStatus.PENDING, result.getStatus());
        verify(appealRepo, times(1)).save(any(Appeal.class));
    }

    @Test
    public void testSubmitAppeal_NotResolved_ThrowsException() {
        reportCase.setCaseStatus(CaseStatus.OPEN);
        when(reportCaseRepo.findByCaseId(20L)).thenReturn(Optional.of(reportCase));
        when(userRepo.findById(2L)).thenReturn(Optional.of(regularUser));

        assertThrows(GlobalException.class, () -> {
            reportService.submitAppeal(20L, 2L, "Reason", null);
        });
    }

    @Test
    public void testResolveAppeal_Approve_Success() {
        when(appealRepo.findById(30L)).thenReturn(Optional.of(appeal));
        when(userRepo.findById(1L)).thenReturn(Optional.of(adminUser));
        when(appealRepo.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock scorelogs for point refund
        ScoreLog log1 = ScoreLog.builder().scoreChange(-20).build();
        when(scoreLogRepo.findAllByReportCase(reportCase)).thenReturn(List.of(log1));
        when(scoreTypeRepo.findByTypeCode("REPORT_PENALTY")).thenReturn(Optional.of(new ScoreType()));

        Appeal result = reportService.resolveAppeal(30L, 1L, true, "Appeal accepted, restoring document");

        assertNotNull(result);
        assertEquals(AppealStatus.APPROVED, result.getStatus());
        assertEquals(CaseStatus.REJECTED, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.NORMAL, document.getModerationStatus());
        assertEquals(120L, regularUser.getTotalScore()); // Refunded 20 points
        verify(notificationService, times(1)).sendDocumentRestoredNotification(eq(regularUser), eq(document), anyString());
    }

    @Test
    public void testResolveAppeal_Reject_Success() {
        when(appealRepo.findById(30L)).thenReturn(Optional.of(appeal));
        when(userRepo.findById(1L)).thenReturn(Optional.of(adminUser));
        when(appealRepo.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appeal result = reportService.resolveAppeal(30L, 1L, false, "Appeal rejected");

        assertNotNull(result);
        assertEquals(AppealStatus.REJECTED, result.getStatus());
        assertEquals(CaseStatus.RESOLVED, reportCase.getCaseStatus()); // Remains resolved
        verify(notificationService, never()).sendDocumentRestoredNotification(any(), any(), any());
    }
}
