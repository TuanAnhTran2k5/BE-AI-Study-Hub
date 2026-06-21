package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.service.impl.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {

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

    private User reporter;
    private User owner;
    private User admin;
    private Document document;
    private ReportReason reasonLow;
    private ReportReason reasonHigh;
    private ScoreType reportPenaltyScoreType;
    private ScoreType falseReportPenaltyScoreType;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        reporter = User.builder()
                .userId(1L)
                .email("reporter@test.com")
                .fullName("Reporter User")
                .totalScore(100L)
                .status(UserStatus.ACTIVE)
                .role(UserRole.US)
                .build();

        owner = User.builder()
                .userId(2L)
                .email("owner@test.com")
                .fullName("Owner User")
                .totalScore(100L)
                .status(UserStatus.ACTIVE)
                .role(UserRole.US)
                .build();

        admin = User.builder()
                .userId(3L)
                .email("admin@test.com")
                .fullName("Admin User")
                .totalScore(100L)
                .status(UserStatus.ACTIVE)
                .role(UserRole.AD)
                .build();

        document = Document.builder()
                .documentId(10L)
                .title("Tài liệu nghiên cứu")
                .owner(owner)
                .moderationStatus(ModerationStatus.NORMAL)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build();

        reasonLow = ReportReason.builder()
                .reasonId(100L)
                .reasonName("Spam nội dung")
                .severityLevel(ReportSeverity.LOW)
                .reportThreshold(5)
                .penaltyScore(10)
                .build();

        reasonHigh = ReportReason.builder()
                .reasonId(101L)
                .reasonName("Nội dung đồi trụy")
                .severityLevel(ReportSeverity.HIGH)
                .reportThreshold(1)
                .penaltyScore(null)
                .build();

        reportPenaltyScoreType = ScoreType.builder()
                .typeCode("REPORT_PENALTY")
                .typeName("Report Penalty")
                .build();

        falseReportPenaltyScoreType = ScoreType.builder()
                .typeCode("FALSE_REPORT_PENALTY")
                .typeName("False Report Penalty")
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(reporter));
        when(userRepo.findById(2L)).thenReturn(Optional.of(owner));
        when(userRepo.findById(3L)).thenReturn(Optional.of(admin));
        when(documentRepo.findById(10L)).thenReturn(Optional.of(document));
        when(reportReasonRepo.findById(100L)).thenReturn(Optional.of(reasonLow));
        when(reportReasonRepo.findById(101L)).thenReturn(Optional.of(reasonHigh));
        
        when(scoreTypeRepo.findByTypeCode("REPORT_PENALTY")).thenReturn(Optional.of(reportPenaltyScoreType));
        when(scoreTypeRepo.findByTypeCode("FALSE_REPORT_PENALTY")).thenReturn(Optional.of(falseReportPenaltyScoreType));
    }

    @Test
    void createReportWhenUserMutedShouldThrowForbidden() {
        reporter.setTotalScore(-5L); // negative score

        GlobalException exception = assertThrows(GlobalException.class, () -> {
            reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");
        });

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("khóa tính năng gửi báo cáo"));
    }

    @Test
    void createReportWhenRateLimitExceededShouldThrowTooManyRequests() {
        when(reportRepo.countByReporterAndReasonSeverityLevelAndCreatedAtAfter(any(), eq(ReportSeverity.HIGH), any()))
                .thenReturn(5L);

        GlobalException exception = assertThrows(GlobalException.class, () -> {
            reportService.createReport(1L, 10L, 101L, "Nội dung xấu", "http://evidence");
        });

        assertEquals(429, exception.getCode());
        assertTrue(exception.getMessage().contains("vượt quá giới hạn"));
    }

    @Test
    void createReportWhenDuplicateReportShouldThrowBadRequest() {
        when(reportRepo.existsByReporterAndDocument(reporter, document)).thenReturn(true);

        GlobalException exception = assertThrows(GlobalException.class, () -> {
            reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("đã gửi báo cáo"));
    }

    @Test
    void createReportLowSeverityIncreasesCountAndTriggersWarning1() {
        ReportCase reportCase = ReportCase.builder()
                .caseId(200L)
                .document(document)
                .reason(reasonLow)
                .caseLevel(ReportSeverity.LOW)
                .reportCount(4) // 4 reports so far
                .caseStatus(CaseStatus.OPEN)
                .requiredThreshold(5)
                .build();

        when(reportCaseRepo.findFirstByDocumentAndReasonAndCaseStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(reportCase));
        when(reportCaseRepo.save(any(ReportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepo.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");

        assertNotNull(result);
        assertEquals(5, reportCase.getReportCount());
        assertEquals(CaseStatus.WARNING_1, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.HIDDEN, document.getModerationStatus());
        assertEquals(90L, owner.getTotalScore()); // -10 points penalty
        verify(notificationService).sendDocumentModerationNotification(eq(owner), eq(document), eq("Spam nội dung"), eq(10), eq("HIDDEN"), any());
    }

    @Test
    void createReportLowSeverityWarning1ToWarning2() {
        ReportCase reportCase = ReportCase.builder()
                .caseId(200L)
                .document(document)
                .reason(reasonLow)
                .caseLevel(ReportSeverity.LOW)
                .reportCount(9) // already warning 1, total threshold to warning 2 is threshold * 2 = 10
                .caseStatus(CaseStatus.WARNING_1)
                .firstWarningAt(LocalDateTime.now().minusDays(1))
                .requiredThreshold(5)
                .build();

        document.setModerationStatus(ModerationStatus.HIDDEN);

        when(reportCaseRepo.findFirstByDocumentAndReasonAndCaseStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(reportCase));
        when(reportCaseRepo.save(any(ReportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepo.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");

        assertNotNull(result);
        assertEquals(10, reportCase.getReportCount());
        assertEquals(CaseStatus.WARNING_2, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.REMOVED, document.getModerationStatus());
        assertEquals(90L, owner.getTotalScore()); // -10 points penalty
        verify(notificationService).sendDocumentModerationNotification(eq(owner), eq(document), eq("Spam nội dung"), eq(10), eq("REMOVED"), any());
    }

    @Test
    void createReportHighSeverityTriggersImmediatePendingReview() {
        ReportCase reportCase = ReportCase.builder()
                .caseId(201L)
                .document(document)
                .reason(reasonHigh)
                .caseLevel(ReportSeverity.HIGH)
                .reportCount(0)
                .caseStatus(CaseStatus.OPEN)
                .build();

        when(reportCaseRepo.findFirstByDocumentAndReasonAndCaseStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(reportCase));
        when(reportCaseRepo.save(any(ReportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepo.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.createReport(1L, 10L, 101L, "Nội dung xấu", "http://evidence");

        assertNotNull(result);
        assertEquals(1, reportCase.getReportCount());
        assertEquals(CaseStatus.PENDING_REVIEW, reportCase.getCaseStatus());
        assertEquals(ModerationStatus.HIDDEN, document.getModerationStatus()); // Hidden temporarily
        assertEquals(100L, owner.getTotalScore()); // no auto penalty yet for HIGH
    }

    @Test
    void adminResolveCaseWhenRejectAppliesCounterPenalty() {
        ReportCase reportCase = ReportCase.builder()
                .caseId(202L)
                .document(document)
                .reason(reasonHigh)
                .caseLevel(ReportSeverity.HIGH)
                .caseStatus(CaseStatus.PENDING_REVIEW)
                .claimedBy(admin)
                .resolvedBy(admin)
                .build();

        Report report1 = Report.builder().reportId(301L).reporter(reporter).document(document).build();

        when(reportCaseRepo.findById(202L)).thenReturn(Optional.of(reportCase));
        when(reportRepo.findAllByReportCase(reportCase)).thenReturn(List.of(report1));
        when(reportCaseRepo.save(any(ReportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportCase result = reportService.adminResolveCase(202L, 3L, AdminDecision.REJECT, "Báo cáo sai sự thật");

        assertNotNull(result);
        assertEquals(CaseStatus.REJECTED, result.getCaseStatus());
        assertEquals(ModerationStatus.NORMAL, document.getModerationStatus()); // Restored to Normal
        assertEquals(90L, reporter.getTotalScore()); // Reporter penalised -10 points!
        verify(notificationService).sendFalseReportPenaltyNotification(eq(reporter), eq(document), eq(10), eq("Báo cáo sai sự thật"));
    }

    @Test
    void createReportWhenDocumentNotPublicShouldThrowBadRequest() {
        document.setVisibilityStatus(VisibilityStatus.PRIVATE);

        GlobalException exception = assertThrows(GlobalException.class, () -> {
            reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("chế độ riêng tư"));
    }

    @Test
    void createReportWhenSelfReportShouldThrowBadRequest() {
        document.setOwner(reporter);

        GlobalException exception = assertThrows(GlobalException.class, () -> {
            reportService.createReport(1L, 10L, 100L, "Spam", "http://evidence");
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("báo cáo tài liệu của chính mình"));
    }
}
