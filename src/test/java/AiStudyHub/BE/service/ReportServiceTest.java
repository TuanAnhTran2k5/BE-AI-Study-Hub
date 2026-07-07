package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ReportSeverity;
import AiStudyHub.BE.dto.Request.ReportReasonRequest;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.ReportReason;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

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

    @Test
    void getReportsByReporter_success() {
        User reporter = User.builder().userId(1L).build();
        Report report = Report.builder().reportId(10L).reporter(reporter).build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(reporter));
        when(reportRepo.findAllByReporterOrderByCreatedAtDesc(reporter))
                .thenReturn(List.of(report));

        List<Report> result = reportService.getReportsByReporter(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getReportId());
    }

    @Test
    void getReportsByReporter_userNotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(GlobalException.class, () -> reportService.getReportsByReporter(1L));
    }

    @Test
    void createReason_success() {
        ReportReasonRequest request = ReportReasonRequest.builder()
                .reasonName("Spam")
                .severityLevel(ReportSeverity.LOW)
                .description("Spammy content")
                .reportThreshold(5)
                .penaltyScore(10)
                .build();

        ReportReason savedReason = ReportReason.builder()
                .reasonId(100L)
                .reasonName("Spam")
                .severityLevel(ReportSeverity.LOW)
                .description("Spammy content")
                .reportThreshold(5)
                .penaltyScore(10)
                .build();

        when(reportReasonRepo.save(any(ReportReason.class))).thenReturn(savedReason);

        ReportReason result = reportService.createReason(request);

        assertNotNull(result);
        assertEquals(100L, result.getReasonId());
        assertEquals("Spam", result.getReasonName());
    }

    @Test
    void updateReason_success() {
        ReportReason existingReason = ReportReason.builder()
                .reasonId(100L)
                .reasonName("Old Name")
                .build();

        ReportReasonRequest request = ReportReasonRequest.builder()
                .reasonName("New Name")
                .severityLevel(ReportSeverity.HIGH)
                .description("New Description")
                .reportThreshold(2)
                .penaltyScore(20)
                .build();

        when(reportReasonRepo.findById(100L)).thenReturn(Optional.of(existingReason));
        when(reportReasonRepo.save(any(ReportReason.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportReason result = reportService.updateReason(100L, request);

        assertEquals("New Name", result.getReasonName());
        assertEquals(ReportSeverity.HIGH, result.getSeverityLevel());
        assertEquals("New Description", result.getDescription());
        assertEquals(2, result.getReportThreshold());
        assertEquals(20, result.getPenaltyScore());
    }

    @Test
    void deleteReason_success() {
        ReportReason existingReason = ReportReason.builder().reasonId(100L).build();

        when(reportReasonRepo.findById(100L)).thenReturn(Optional.of(existingReason));
        when(reportCaseRepo.existsByReason(existingReason)).thenReturn(false);
        when(reportRepo.existsByReason(existingReason)).thenReturn(false);

        reportService.deleteReason(100L);

        verify(reportReasonRepo, times(1)).delete(existingReason);
    }

    @Test
    void deleteReason_referencedInReports_throwsException() {
        ReportReason existingReason = ReportReason.builder().reasonId(100L).build();

        when(reportReasonRepo.findById(100L)).thenReturn(Optional.of(existingReason));
        when(reportCaseRepo.existsByReason(existingReason)).thenReturn(false);
        when(reportRepo.existsByReason(existingReason)).thenReturn(true);

        GlobalException exception = assertThrows(GlobalException.class, () -> reportService.deleteReason(100L));
        assertEquals(400, exception.getCode());
        verify(reportReasonRepo, never()).delete(any());
    }
}
