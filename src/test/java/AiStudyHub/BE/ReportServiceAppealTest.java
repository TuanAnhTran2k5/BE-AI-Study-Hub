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

    @InjectMocks
    private ReportService reportService;

    private User regularUser;
    private User adminUser;
    private Document document;
    private ReportCase reportCase;

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
    }

    @Test
    public void testRefundAppeal_Success() {
        when(reportCaseRepo.findByCaseId(20L)).thenReturn(Optional.of(reportCase));
        when(userRepo.findById(1L)).thenReturn(Optional.of(adminUser));
        when(reportCaseRepo.save(any(ReportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock scorelogs for point refund
        ScoreLog log1 = ScoreLog.builder().scoreChange(-20).build();
        when(scoreLogRepo.findAllByReportCase(reportCase)).thenReturn(List.of(log1));
        when(scoreTypeRepo.findByTypeCode("REPORT_PENALTY")).thenReturn(Optional.of(new ScoreType()));

        ReportCase result = reportService.refundAppeal(20L, 1L, "Appeal approved, restoring document and score");

        assertNotNull(result);
        assertEquals(CaseStatus.REJECTED, result.getCaseStatus());
        assertEquals(ModerationStatus.NORMAL, document.getModerationStatus());
        assertEquals(120L, regularUser.getTotalScore()); // Refunded 20 points
        verify(notificationService, times(1)).sendDocumentRestoredNotification(eq(regularUser), eq(document),
                anyString());
    }

    @Test
    public void testRefundAppeal_NotResolved_ThrowsException() {
        reportCase.setCaseStatus(CaseStatus.OPEN);
        when(reportCaseRepo.findByCaseId(20L)).thenReturn(Optional.of(reportCase));
        when(userRepo.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(GlobalException.class, () -> {
            reportService.refundAppeal(20L, 1L, "Reason");
        });
    }

    @Test
    public void testRefundAppeal_NotAdmin_ThrowsException() {
        User regularUserAsAdmin = User.builder().userId(1L).role(UserRole.US).build();
        when(reportCaseRepo.findByCaseId(20L)).thenReturn(Optional.of(reportCase));
        when(userRepo.findById(1L)).thenReturn(Optional.of(regularUserAsAdmin));

        assertThrows(GlobalException.class, () -> {
            reportService.refundAppeal(20L, 1L, "Reason");
        });
    }

    @Test
    public void testCheckAndEscalateWarningCases_UserHasUpdated_ResolvesCase() {
        // Prepare warning_1 case where firstWarningAt is 4 days ago
        reportCase.setCaseStatus(CaseStatus.WARNING_1);
        reportCase.setFirstWarningAt(LocalDateTime.now().minusDays(4));
        
        // Document updated after warning (e.g. 10 hours ago)
        document.setModerationStatus(ModerationStatus.NORMAL);
        document.setUpdatedAt(LocalDateTime.now().minusHours(10));

        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_1)).thenReturn(List.of(reportCase));
        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_2)).thenReturn(Collections.emptyList());
        
        Report report = Report.builder().status(ReportStatus.PENDING).build();
        when(reportRepo.findAllByReportCase(reportCase)).thenReturn(List.of(report));

        reportService.checkAndEscalateWarningCases();

        assertEquals(CaseStatus.REJECTED, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.NORMAL, document.getModerationStatus());
        assertEquals(ReportStatus.REJECTED, report.getStatus());
        verify(documentRepo, times(1)).save(document);
        verify(reportCaseRepo, times(1)).save(reportCase);
        verify(notificationService, times(1)).sendDocumentRestoredNotification(eq(regularUser), eq(document), anyString());
    }

    @Test
    public void testCheckAndEscalateWarningCases_UserHasNotUpdated_EscalatesToWarning2() {
        // Prepare warning_1 case where firstWarningAt is 4 days ago
        reportCase.setCaseStatus(CaseStatus.WARNING_1);
        reportCase.setFirstWarningAt(LocalDateTime.now().minusDays(4));
        
        // Document not updated (e.g. 5 days ago)
        document.setModerationStatus(ModerationStatus.NORMAL);
        document.setUpdatedAt(LocalDateTime.now().minusDays(5));

        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_1)).thenReturn(List.of(reportCase));
        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_2)).thenReturn(Collections.emptyList());
        when(userRepo.save(any(User.class))).thenReturn(regularUser);
        
        ScoreType penaltyType = ScoreType.builder().defaultPoint(-10).build();
        when(scoreTypeRepo.findByTypeCode("REPORT_PENALTY")).thenReturn(Optional.of(penaltyType));

        reportService.checkAndEscalateWarningCases();

        assertEquals(CaseStatus.WARNING_2, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.HIDDEN, document.getModerationStatus());
        assertNotNull(reportCase.getSecondWarningAt());
        assertEquals(90L, regularUser.getTotalScore()); // Deducted 10 points
        verify(documentRepo, times(1)).save(document);
        verify(reportCaseRepo, times(1)).save(reportCase);
        verify(notificationService, times(1)).sendDocumentModerationNotification(
                eq(regularUser), eq(document), any(), eq(10), eq("WARNING_2 (Warning Notice)"), anyString()
        );
    }

    @Test
    public void testCheckAndEscalateWarningCases_Warning2Expired_AutoRemovesDocument() {
        // Prepare warning_2 case where secondWarningAt is 8 days ago
        reportCase.setCaseStatus(CaseStatus.WARNING_2);
        reportCase.setSecondWarningAt(LocalDateTime.now().minusDays(8));
        document.setModerationStatus(ModerationStatus.HIDDEN);

        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_1)).thenReturn(Collections.emptyList());
        when(reportCaseRepo.findAllByCaseStatus(CaseStatus.WARNING_2)).thenReturn(List.of(reportCase));
        
        Report report = Report.builder().status(ReportStatus.PENDING).build();
        when(reportRepo.findAllByReportCase(reportCase)).thenReturn(List.of(report));

        reportService.checkAndEscalateWarningCases();

        assertEquals(CaseStatus.RESOLVED, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.REMOVED, document.getModerationStatus());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        verify(documentRepo, times(1)).save(document);
        verify(reportCaseRepo, times(1)).save(reportCase);
        verify(notificationService, times(1)).sendDocumentModerationNotification(
                eq(regularUser), eq(document), any(), eq(0), eq("REMOVED (Permanently Removed)"), anyString()
        );
    }
}
