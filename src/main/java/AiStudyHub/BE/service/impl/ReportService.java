package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.service.IReport;
import AiStudyHub.BE.service.IGamification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService implements IReport {

    private final ReportRepo reportRepo;
    private final ReportCaseRepo reportCaseRepo;
    private final ReportReasonRepo reportReasonRepo;
    private final UserRepo userRepo;
    private final DocumentRepo documentRepo;
    private final ScoreLogRepo scoreLogRepo;
    private final ScoreTypeRepo scoreTypeRepo;
    private final INotification notificationService;
    private final IGamification rankingBadgeService;

    @Override
    @Transactional
    public Report createReport(Long reporterId, Long documentId, Long reasonId, String description, String evidenceUrl) {
        User reporter = userRepo.findById(reporterId)
                .orElseThrow(() -> new GlobalException(404, "Reporter not found"));
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found"));
        ReportReason reason = reportReasonRepo.findById(reasonId)
                .orElseThrow(() -> new GlobalException(404, "ReportReason not found"));

        // 1. Validations
        if (reporter.getTotalScore() < 0) {
            throw new GlobalException(403, "Tài khoản của bạn đã bị khóa tính năng gửi báo cáo do điểm uy tín âm.");
        }
        if (document.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(400, "Tài liệu này đang ở chế độ riêng tư, không thể báo cáo.");
        }
        if (document.getOwner().getUserId().equals(reporter.getUserId())) {
            throw new GlobalException(400, "Bạn không thể báo cáo tài liệu của chính mình.");
        }

        // 2. Check Rate Limiting for HIGH severity (max 5 reports in 24 hours)
        if (reason.getSeverityLevel() == ReportSeverity.HIGH) {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            long highReportCount = reportRepo.countByReporterAndReasonSeverityLevelAndCreatedAtAfter(
                    reporter, ReportSeverity.HIGH, oneDayAgo
            );
            if (highReportCount >= 5) {
                throw new GlobalException(429, "Bạn đã vượt quá giới hạn gửi 5 báo cáo nghiêm trọng (HIGH) trong vòng 24 giờ. Vui lòng thử lại sau.");
            }
        }

        // 3. Check if user has already reported this document
        if (reportRepo.existsByReporterAndDocument(reporter, document)) {
            throw new GlobalException(400, "Bạn đã gửi báo cáo cho tài liệu này rồi.");
        }

        // 4. Find open case with Pessimistic Lock to prevent concurrency issues
        List<CaseStatus> openStatuses = List.of(CaseStatus.OPEN, CaseStatus.WARNING_1, CaseStatus.WARNING_2);
        ReportCase reportCase = reportCaseRepo.findFirstByDocumentAndReasonAndCaseStatusIn(document, reason, openStatuses)
                .orElseGet(() -> {
                    ReportCase newCase = ReportCase.builder()
                            .document(document)
                            .reason(reason)
                            .caseLevel(reason.getSeverityLevel())
                            .reportCount(0)
                            .requiredThreshold(reason.getReportThreshold())
                            .caseStatus(CaseStatus.OPEN)
                            .build();
                    return reportCaseRepo.save(newCase);
                });

        // 5. Create Report
        Report report = Report.builder()
                .reporter(reporter)
                .document(document)
                .reason(reason)
                .reportCase(reportCase)
                .description(description)
                .evidenceUrl(evidenceUrl)
                .status(ReportStatus.APPROVED)
                .build();
        report = reportRepo.save(report);

        // 6. Process Report Case count & warnings
        reportCase.setReportCount(reportCase.getReportCount() + 1);
        processCase(reportCase);
        reportCaseRepo.save(reportCase);

        return report;
    }

    private void processCase(ReportCase reportCase) {
        Document document = reportCase.getDocument();
        User owner = document.getOwner();
        ReportReason reason = reportCase.getReason();

        if (reportCase.getCaseLevel() == ReportSeverity.HIGH) {
            // High severity auto-hides document and goes to Admin PENDING_REVIEW immediately
            if (reportCase.getCaseStatus() != CaseStatus.PENDING_REVIEW) {
                reportCase.setCaseStatus(CaseStatus.PENDING_REVIEW);
                document.setModerationStatus(ModerationStatus.HIDDEN);
                documentRepo.save(document);
                log.info("ReportCase ID {} (HIGH) threshold met. Temporarily hid document ID {} and set to PENDING_REVIEW.", reportCase.getCaseId(), document.getDocumentId());
            }
            return;
        }

        // Low / Medium severity automatic warnings
        int threshold = reportCase.getRequiredThreshold();
        int count = reportCase.getReportCount();

        if (count >= threshold * 2 && reportCase.getCaseStatus() != CaseStatus.WARNING_2) {
            // Trigger WARNING_2: Hide permanently (REMOVED) and penalise points
            reportCase.setCaseStatus(CaseStatus.WARNING_2);
            document.setModerationStatus(ModerationStatus.REMOVED);
            documentRepo.save(document);

            int penalty = reason.getPenaltyScore() != null ? reason.getPenaltyScore() : 10;
            deductPoints(owner, penalty, reportCase, "Phạt vi phạm cấp độ 2 (Tài liệu bị gỡ vĩnh viễn): " + reason.getReasonName());

            notificationService.sendDocumentModerationNotification(
                    owner, document, reason.getReasonName(), penalty, "REMOVED",
                    "Tài liệu của bạn bị báo cáo vi phạm quá nhiều lần (đạt ngưỡng WARNING_2) và đã bị gỡ bỏ vĩnh viễn."
            );
            log.info("ReportCase ID {} set to WARNING_2. Document ID {} is now REMOVED.", reportCase.getCaseId(), document.getDocumentId());

        } else if (count >= threshold && reportCase.getCaseStatus() == CaseStatus.OPEN) {
            // Trigger WARNING_1: Hide temporarily (HIDDEN) and penalise points
            reportCase.setCaseStatus(CaseStatus.WARNING_1);
            reportCase.setFirstWarningAt(LocalDateTime.now());
            document.setModerationStatus(ModerationStatus.HIDDEN);
            documentRepo.save(document);

            int penalty = reason.getPenaltyScore() != null ? reason.getPenaltyScore() : 10;
            deductPoints(owner, penalty, reportCase, "Phạt vi phạm cấp độ 1 (Tài liệu bị ẩn tạm thời): " + reason.getReasonName());

            notificationService.sendDocumentModerationNotification(
                    owner, document, reason.getReasonName(), penalty, "HIDDEN",
                    "Tài liệu của bạn bị ẩn tạm thời do bị báo cáo vi phạm (đạt ngưỡng WARNING_1). Vui lòng điều chỉnh lại nội dung."
            );
            log.info("ReportCase ID {} set to WARNING_1. Document ID {} is now HIDDEN.", reportCase.getCaseId(), document.getDocumentId());
        }
    }

    private void deductPoints(User user, int points, ReportCase reportCase, String description) {
        long currentScore = user.getTotalScore();
        user.setTotalScore(currentScore - points);
        userRepo.save(user);

        ScoreType penaltyType = scoreTypeRepo.findByTypeCode("REPORT_PENALTY")
                .orElseThrow(() -> new GlobalException(500, "REPORT_PENALTY ScoreType not found"));

        ScoreLog logEntry = ScoreLog.builder()
                .user(user)
                .document(reportCase.getDocument())
                .scoreType(penaltyType)
                .reportCase(reportCase)
                .scoreChange(-points)
                .description(description)
                .build();
        scoreLogRepo.save(logEntry);

        // Update rank and badge for user
        rankingBadgeService.updateUserRank(user.getUserId());
        rankingBadgeService.checkAndAwardBadges(user.getUserId());
    }

    @Override
    @Transactional
    public ReportCase claimCase(Long caseId, Long adminId) {
        ReportCase rc = reportCaseRepo.findById(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new GlobalException(404, "Admin not found"));

        if (!admin.getRole().equals(UserRole.AD)) {
            throw new GlobalException(403, "Only admins can claim cases");
        }
        if (rc.getCaseStatus() != CaseStatus.PENDING_REVIEW) {
            throw new GlobalException(400, "Only PENDING_REVIEW cases can be claimed");
        }
        if (rc.getClaimedBy() != null) {
            throw new GlobalException(400, "This case is already claimed by another admin");
        }

        rc.setClaimedBy(admin);
        rc.setClaimedAt(LocalDateTime.now());
        return reportCaseRepo.save(rc);
    }

    @Override
    @Transactional
    public ReportCase unclaimCase(Long caseId, Long adminId) {
        ReportCase rc = reportCaseRepo.findById(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));

        if (rc.getClaimedBy() == null || !rc.getClaimedBy().getUserId().equals(adminId)) {
            throw new GlobalException(400, "You can only unclaim your own claimed cases");
        }
        if (rc.getCaseStatus() != CaseStatus.PENDING_REVIEW) {
            throw new GlobalException(400, "Only PENDING_REVIEW cases can be unclaimed");
        }

        rc.setClaimedBy(null);
        rc.setClaimedAt(null);
        return reportCaseRepo.save(rc);
    }

    @Override
    @Transactional
    public ReportCase adminResolveCase(Long caseId, Long adminId, AdminDecision decision, String note) {
        ReportCase rc = reportCaseRepo.findById(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new GlobalException(404, "Admin not found"));

        if (!admin.getRole().equals(UserRole.AD)) {
            throw new GlobalException(403, "Only admins can resolve cases");
        }
        if (rc.getClaimedBy() == null || !rc.getClaimedBy().getUserId().equals(adminId)) {
            throw new GlobalException(400, "You must claim the case first before resolving it");
        }

        rc.setResolvedBy(admin);
        rc.setResolvedAt(LocalDateTime.now());
        rc.setAdminNote(note);

        Document document = rc.getDocument();
        User owner = document.getOwner();

        if (decision == AdminDecision.BAN) {
            rc.setCaseStatus(CaseStatus.RESOLVED);
            document.setModerationStatus(ModerationStatus.REMOVED);
            documentRepo.save(document);

            owner.setStatus(UserStatus.BANNED);
            owner.setBanReason("Vi phạm nghiêm trọng chính sách nội dung: " + note);
            owner.setBannedAt(LocalDateTime.now());
            owner.setBannedBy(admin);
            userRepo.save(owner);

            log.info("Admin ID {} banned User ID {} and removed Document ID {}", adminId, owner.getUserId(), document.getDocumentId());

        } else if (decision == AdminDecision.REMOVE_DOCUMENT) {
            rc.setCaseStatus(CaseStatus.RESOLVED);
            document.setModerationStatus(ModerationStatus.REMOVED);
            documentRepo.save(document);

            int penalty = rc.getReason().getPenaltyScore() != null ? rc.getReason().getPenaltyScore() : 20;
            deductPoints(owner, penalty, rc, "Admin gỡ tài liệu do vi phạm: " + rc.getReason().getReasonName() + ". Chi tiết: " + note);

            notificationService.sendDocumentModerationNotification(
                    owner, document, rc.getReason().getReasonName(), penalty, "REMOVED",
                    "Admin đã duyệt và quyết định gỡ tài liệu của bạn do vi phạm nghiêm trọng. Chi tiết: " + note
            );
            log.info("Admin ID {} removed Document ID {}", adminId, document.getDocumentId());

        } else if (decision == AdminDecision.REJECT) {
            rc.setCaseStatus(CaseStatus.REJECTED);
            document.setModerationStatus(ModerationStatus.NORMAL); // Restore visibility
            documentRepo.save(document);

            // Counter penalty for spam/false reporting
            List<Report> reports = reportRepo.findAllByReportCase(rc);
            ScoreType falseReportType = scoreTypeRepo.findByTypeCode("FALSE_REPORT_PENALTY")
                    .orElseThrow(() -> new GlobalException(500, "FALSE_REPORT_PENALTY ScoreType not found"));

            for (Report report : reports) {
                User reporter = report.getReporter();
                long currentScore = reporter.getTotalScore();
                reporter.setTotalScore(currentScore - 10);
                userRepo.save(reporter);

                ScoreLog logEntry = ScoreLog.builder()
                        .user(reporter)
                        .document(document)
                        .scoreType(falseReportType)
                        .reportCase(rc)
                        .scoreChange(-10)
                        .description("Phạt cảnh cáo gửi báo cáo sai sự thật tài liệu: " + document.getTitle())
                        .build();
                scoreLogRepo.save(logEntry);

                rankingBadgeService.updateUserRank(reporter.getUserId());
                rankingBadgeService.checkAndAwardBadges(reporter.getUserId());

                notificationService.sendFalseReportPenaltyNotification(
                        reporter, document, 10, note
                );
            }
            log.info("Admin ID {} rejected ReportCase ID {}. Restored document and penalized {} false reporters.", adminId, caseId, reports.size());
        }

        return reportCaseRepo.save(rc);
    }

    // Cron job to release claimed cases exceeding 24 hours back to shared queue
    @Scheduled(cron = "0 0 * * * *") // Check hourly
    @Transactional
    public void releaseExpiredClaims() {
        log.info("Running cron job to release expired claims...");
        List<ReportCase> pendingCases = reportCaseRepo.findAllByCaseStatus(CaseStatus.PENDING_REVIEW);
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(1);

        for (ReportCase rc : pendingCases) {
            if (rc.getClaimedBy() != null && rc.getClaimedAt() != null && rc.getClaimedAt().isBefore(thresholdTime)) {
                log.info("Releasing expired claim for ReportCase ID {}", rc.getCaseId());
                rc.setClaimedBy(null);
                rc.setClaimedAt(null);
                reportCaseRepo.save(rc);
            }
        }
    }
}
