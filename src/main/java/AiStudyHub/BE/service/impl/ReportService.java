package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.service.IReport;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.dto.Request.ReportReasonRequest;
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

        // If this is a copy, report the original public document
        Document targetDocument = document.getSourceDocument() != null ? document.getSourceDocument() : document;

        // Lock the document to prevent concurrency issues and phantom-case races
        targetDocument = documentRepo.findByDocumentId(targetDocument.getDocumentId())
                .orElseThrow(() -> new GlobalException(404, "Target document not found"));

        ReportReason reason = reportReasonRepo.findById(reasonId)
                .orElseThrow(() -> new GlobalException(404, "ReportReason not found"));

        // 1. Validations
        if (reporter.getTotalScore() < 0) {
            throw new GlobalException(403, "Your account has been restricted from submitting reports due to a negative reputation score.");
        }
        if (targetDocument.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(400, "This document is private and cannot be reported.");
        }
        if (targetDocument.getOwner().getUserId().equals(reporter.getUserId())) {
            throw new GlobalException(400, "You cannot report your own document.");
        }

        // 2. Check Rate Limiting for HIGH severity (max 5 reports in 24 hours)
        if (reason.getSeverityLevel() == ReportSeverity.HIGH) {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            long highReportCount = reportRepo.countByReporterAndReasonSeverityLevelAndCreatedAtAfter(
                    reporter, ReportSeverity.HIGH, oneDayAgo
            );
            if (highReportCount >= 5) {
                throw new GlobalException(429, "You have exceeded the limit of 5 high-severity (HIGH) reports within 24 hours. Please try again later.");
            }
        }

        // 3. Check if user has already reported this document
        if (reportRepo.existsByReporterAndDocument(reporter, targetDocument)) {
            throw new GlobalException(400, "You have already submitted a report for this document.");
        }

        // 4. Find active case across ALL reasons to enforce one active case per document
        List<CaseStatus> activeStatuses = List.of(CaseStatus.OPEN, CaseStatus.WARNING_1, CaseStatus.PENDING_REVIEW, CaseStatus.CLAIMED);
        ReportCase reportCase = reportCaseRepo.findFirstByDocumentAndCaseStatusIn(targetDocument, activeStatuses)
                .orElseGet(() -> {
                    ReportCase newCase = ReportCase.builder()
                            .document(targetDocument)
                            .reason(reason)
                            .caseLevel(reason.getSeverityLevel())
                            .reportCount(0)
                            .requiredThreshold(reason.getReportThreshold())
                            .caseStatus(CaseStatus.OPEN)
                            .build();
                    return reportCaseRepo.save(newCase);
                });

        // Update caseLevel to HIGH if the incoming report has HIGH severity
        if (reason.getSeverityLevel() == ReportSeverity.HIGH) {
            reportCase.setCaseLevel(ReportSeverity.HIGH);
        }

        // 5. Create Report with PENDING status
        Report report = Report.builder()
                .reporter(reporter)
                .document(targetDocument)
                .reason(reason)
                .reportCase(reportCase)
                .description(description)
                .evidenceUrl(evidenceUrl)
                .status(ReportStatus.PENDING)
                .build();
        report = reportRepo.save(report);

        // Update document reportCount in DB
        long totalReports = reportRepo.countByDocument(targetDocument);
        targetDocument.setReportCount((int) totalReports);
        documentRepo.save(targetDocument);

        // 6. Process Report Case count & warnings
        reportCase.setReportCount(reportCase.getReportCount() + 1);
        processCase(reportCase);
        reportCaseRepo.save(reportCase);

        return report;
    }

    private void processCase(ReportCase reportCase) {
        // Exit immediately if PENDING_REVIEW, CLAIMED, or terminal (RESOLVED, REJECTED)
        if (reportCase.getCaseStatus() == CaseStatus.PENDING_REVIEW ||
                reportCase.getCaseStatus() == CaseStatus.CLAIMED ||
                reportCase.getCaseStatus() == CaseStatus.RESOLVED ||
                reportCase.getCaseStatus() == CaseStatus.REJECTED) {
            return;
        }

        Document document = reportCase.getDocument();
        User owner = document.getOwner();
        ReportReason reason = reportCase.getReason();

        if (reportCase.getCaseLevel() == ReportSeverity.HIGH) {
            // HIGH Severity
            // Rule 1: If currentStatus is WARNING_1, immediately escalate to PENDING_REVIEW (ignore HIGH threshold count)
            if (reportCase.getCaseStatus() == CaseStatus.WARNING_1) {
                reportCase.setCaseStatus(CaseStatus.PENDING_REVIEW);
                document.setModerationStatus(ModerationStatus.HIDDEN);
                documentRepo.save(document);
                log.info("ReportCase ID {} (HIGH upgrade from WARNING_1) escalated. Document ID {} is HIDDEN.", reportCase.getCaseId(), document.getDocumentId());
                return;
            }

            // Rule 2: If currentStatus is OPEN, check safety threshold Math.max(2, requiredThreshold)
            int threshold = Math.max(2, reportCase.getRequiredThreshold());
            if (reportCase.getReportCount() >= threshold && reportCase.getCaseStatus() != CaseStatus.PENDING_REVIEW) {
                reportCase.setCaseStatus(CaseStatus.PENDING_REVIEW);
                document.setModerationStatus(ModerationStatus.HIDDEN);
                documentRepo.save(document);
                log.info("ReportCase ID {} (HIGH) threshold met. Temporarily hid document ID {} and set to PENDING_REVIEW.", reportCase.getCaseId(), document.getDocumentId());

                notificationService.sendDocumentModerationNotification(
                        owner, document, reason.getReasonName(), 0, "HIDDEN (Pending Admin Review)",
                        "Your document received multiple high-severity (HIGH) violation reports and has been temporarily hidden pending review by our moderation team."
                );
            }
            return;
        }

        // Low / Medium severity automatic warnings
        int threshold = reportCase.getRequiredThreshold();
        int count = reportCase.getReportCount();

        if (count >= threshold * 2 && reportCase.getCaseStatus() != CaseStatus.PENDING_REVIEW) {
            // Transition to PENDING_REVIEW (WARNING_2 is historical now)
            reportCase.setCaseStatus(CaseStatus.PENDING_REVIEW);
            document.setModerationStatus(ModerationStatus.HIDDEN);
            documentRepo.save(document);

            // Double check: if case somehow skipped WARNING_1, apply warning penalty first
            if (reportCase.getFirstWarningAt() == null) {
                reportCase.setFirstWarningAt(LocalDateTime.now());
                int penalty = reason.getPenaltyScore() != null ? reason.getPenaltyScore() : 10;
                deductPoints(owner, penalty, reportCase, "Level 1 violation penalty (Document temporarily hidden): " + reason.getReasonName());
            }

            notificationService.sendDocumentModerationNotification(
                    owner, document, reason.getReasonName(), 0, "HIDDEN (Pending Admin Review)",
                    "Your document received multiple violation reports (reached WARNING_2 threshold) and has been temporarily hidden pending review by our moderation team."
            );
            log.info("ReportCase ID {} set to PENDING_REVIEW (threshold*2 met). Document ID {} is now HIDDEN.", reportCase.getCaseId(), document.getDocumentId());

        } else if (count >= threshold && reportCase.getCaseStatus() == CaseStatus.OPEN) {
            // Trigger WARNING_1: Hide temporarily (HIDDEN) and penalise points
            reportCase.setCaseStatus(CaseStatus.WARNING_1);
            reportCase.setFirstWarningAt(LocalDateTime.now());
            document.setModerationStatus(ModerationStatus.HIDDEN);
            documentRepo.save(document);

            int penalty = reason.getPenaltyScore() != null ? reason.getPenaltyScore() : 10;
            deductPoints(owner, penalty, reportCase, "Level 1 violation penalty (Document temporarily hidden): " + reason.getReasonName());

            notificationService.sendDocumentModerationNotification(
                    owner, document, reason.getReasonName(), penalty, "HIDDEN (Warning Notice)",
                    "Your document has been temporarily hidden after reaching warning threshold 1 (WARNING_1). Please review and modify your content if needed."
                );
            log.info("ReportCase ID {} set to WARNING_1. Document ID {} is now HIDDEN.", reportCase.getCaseId(), document.getDocumentId());
        }
    }

    private void deductPoints(User user, int points, ReportCase reportCase, String description) {
        long currentScore = user.getTotalScore();
        user.setTotalScore(currentScore - points);
        userRepo.save(user);

        checkAndAutoBan(user, "System Auto-Ban: Reputation score fell below -30 due to moderation violations.");

        ScoreType penaltyType = scoreTypeRepo.findByTypeCode("REPORT_PENALTY")
                .orElseThrow(() -> new GlobalException(500, "REPORT_PENALTY ScoreType not found"));

        ScoreLog logEntry = ScoreLog.builder()
                .user(user)
                .documentId(reportCase.getDocument() != null ? reportCase.getDocument().getDocumentId() : null)
                .documentTitle(reportCase.getDocument() != null ? reportCase.getDocument().getTitle() : null)
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

    private void checkAndAutoBan(User user, String reason) {
        if (user.getTotalScore() <= -30 && user.getStatus() != UserStatus.BANNED) {
            user.setStatus(UserStatus.BANNED);
            user.setBanReason(reason);
            user.setBannedAt(LocalDateTime.now());
            user.setBannedBy(null); // System automated ban
            userRepo.save(user);
            log.info("User ID {} automatically BANNED due to reputation score falling to {}. Reason: {}", user.getUserId(), user.getTotalScore(), reason);
        }
    }

    @Override
    @Transactional
    public ReportCase claimCase(Long caseId, Long adminId) {
        ReportCase rc = reportCaseRepo.findByCaseId(caseId)
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
        rc.setCaseStatus(CaseStatus.CLAIMED);
        return reportCaseRepo.save(rc);
    }

    @Override
    @Transactional
    public ReportCase unclaimCase(Long caseId, Long adminId) {
        ReportCase rc = reportCaseRepo.findByCaseId(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));

        if (rc.getClaimedBy() == null || !rc.getClaimedBy().getUserId().equals(adminId)) {
            throw new GlobalException(400, "You can only unclaim your own claimed cases");
        }
        if (rc.getCaseStatus() != CaseStatus.CLAIMED) {
            throw new GlobalException(400, "Only CLAIMED cases can be unclaimed");
        }

        rc.setClaimedBy(null);
        rc.setClaimedAt(null);
        rc.setCaseStatus(CaseStatus.PENDING_REVIEW);
        return reportCaseRepo.save(rc);
    }

    @Override
    @Transactional
    public ReportCase adminResolveCase(Long caseId, Long adminId, AdminDecision decision, String note) {
        ReportCase rc = reportCaseRepo.findByCaseId(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new GlobalException(404, "Admin not found"));

        if (!admin.getRole().equals(UserRole.AD)) {
            throw new GlobalException(403, "Only admins can resolve cases");
        }
        if (rc.getClaimedBy() == null || !rc.getClaimedBy().getUserId().equals(adminId)) {
            throw new GlobalException(400, "You must claim the case first before resolving it");
        }
        if (rc.getCaseStatus() != CaseStatus.CLAIMED) {
            throw new GlobalException(400, "Cannot resolve a case with status: " + rc.getCaseStatus());
        }

        rc.setResolvedBy(admin);
        rc.setResolvedAt(LocalDateTime.now());
        rc.setAdminNote(note);

        Document document = rc.getDocument();
        User owner = document.getOwner();
        List<Report> reports = reportRepo.findAllByReportCase(rc);

        if (decision == AdminDecision.BAN) {
            rc.setCaseStatus(CaseStatus.RESOLVED);
            document.setModerationStatus(ModerationStatus.REMOVED);
            documentRepo.save(document);

            owner.setStatus(UserStatus.BANNED);
            owner.setBanReason("Severe violation of content policy: " + note);
            owner.setBannedAt(LocalDateTime.now());
            owner.setBannedBy(admin);
            userRepo.save(owner);

            notificationService.sendAccountBannedNotification(owner, document, rc.getReason().getReasonName(), note);
            for (Report report : reports) {
                report.setStatus(ReportStatus.RESOLVED);
                reportRepo.save(report);
                notificationService.sendReportApprovedNotification(report.getReporter(), document, note);
            }
            log.info("Admin ID {} banned User ID {} and removed Document ID {}", adminId, owner.getUserId(), document.getDocumentId());

        } else if (decision == AdminDecision.REMOVE_DOCUMENT) {
            rc.setCaseStatus(CaseStatus.RESOLVED);
            document.setModerationStatus(ModerationStatus.REMOVED);
            documentRepo.save(document);

            int penalty = rc.getReason().getPenaltyScore() != null ? rc.getReason().getPenaltyScore() : 20;
            deductPoints(owner, penalty, rc, "Admin removed document due to violation: " + rc.getReason().getReasonName() + ". Details: " + note);

            notificationService.sendDocumentModerationNotification(
                    owner, document, rc.getReason().getReasonName(), penalty, "REMOVED (Permanently Removed)",
                    "Our moderation team has reviewed the case and decided to permanently remove your document due to a severe violation. Details: " + note
            );
            for (Report report : reports) {
                report.setStatus(ReportStatus.RESOLVED);
                reportRepo.save(report);
                notificationService.sendReportApprovedNotification(report.getReporter(), document, note);
            }
            log.info("Admin ID {} removed Document ID {}", adminId, document.getDocumentId());

        } else if (decision == AdminDecision.REJECT) {
            rc.setCaseStatus(CaseStatus.REJECTED);

            // Refund owner warning penalty points if warning was applied (firstWarningAt != null)
            if (rc.getFirstWarningAt() != null) {
                int penalty = rc.getReason().getPenaltyScore() != null ? rc.getReason().getPenaltyScore() : 10;
                long currentScore = owner.getTotalScore();
                owner.setTotalScore(currentScore + penalty);
                userRepo.save(owner);

                ScoreType penaltyType = scoreTypeRepo.findByTypeCode("REPORT_PENALTY")
                        .orElseThrow(() -> new GlobalException(500, "REPORT_PENALTY ScoreType not found"));

                ScoreLog refundLog = ScoreLog.builder()
                        .user(owner)
                        .documentId(document != null ? document.getDocumentId() : null)
                        .documentTitle(document != null ? document.getTitle() : null)
                        .scoreType(penaltyType)
                        .reportCase(rc)
                        .scoreChange(penalty)
                        .description("Refund of warning penalty after report case was rejected by admin: " + rc.getReason().getReasonName())
                        .build();
                scoreLogRepo.save(refundLog);

                rankingBadgeService.updateUserRank(owner.getUserId());
                rankingBadgeService.checkAndAwardBadges(owner.getUserId());
            }

            // Penalty for spam/false reporting — use configurable defaultPoint from ScoreType
            ScoreType falseReportType = scoreTypeRepo.findByTypeCode("FALSE_REPORT_PENALTY")
                    .orElseThrow(() -> new GlobalException(500, "FALSE_REPORT_PENALTY ScoreType not found"));
            int falseReportPenalty = Math.abs(falseReportType.getDefaultPoint() != null ? falseReportType.getDefaultPoint() : 10);

            for (Report report : reports) {
                report.setStatus(ReportStatus.REJECTED);
                reportRepo.save(report);

                User reporter = report.getReporter();
                long currentScore = reporter.getTotalScore();
                reporter.setTotalScore(currentScore - falseReportPenalty);
                userRepo.save(reporter);

                checkAndAutoBan(reporter, "System Auto-Ban: Reputation score fell below -30 due to false/spam reports.");

                ScoreLog logEntry = ScoreLog.builder()
                        .user(reporter)
                        .documentId(document != null ? document.getDocumentId() : null)
                        .documentTitle(document != null ? document.getTitle() : null)
                        .scoreType(falseReportType)
                        .reportCase(rc)
                        .scoreChange(-falseReportPenalty)
                        .description("Penalty for false reporting of document: " + (document != null ? document.getTitle() : ""))
                        .build();
                scoreLogRepo.save(logEntry);

                rankingBadgeService.updateUserRank(reporter.getUserId());
                rankingBadgeService.checkAndAwardBadges(reporter.getUserId());

                notificationService.sendFalseReportPenaltyNotification(
                        reporter, document, falseReportPenalty, note
                );
            }

            // Restore document status to NORMAL if and only if currently HIDDEN
            if (document.getModerationStatus() == ModerationStatus.HIDDEN) {
                document.setModerationStatus(ModerationStatus.NORMAL);
                documentRepo.save(document);
            }

            notificationService.sendDocumentRestoredNotification(owner, document, note);
            log.info("Admin ID {} rejected ReportCase ID {}. Restored document and penalized {} false reporters.", adminId, caseId, reports.size());
        }

        return reportCaseRepo.save(rc);
    }

    // Cron job to release claimed cases exceeding 24 hours back to shared queue
    @Scheduled(cron = "0 0 * * * *") // Check hourly
    @Transactional
    public void releaseExpiredClaims() {
        log.info("Running cron job to release expired claims...");
        List<ReportCase> claimedCases = reportCaseRepo.findAllByCaseStatus(CaseStatus.CLAIMED);
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(1);

        for (ReportCase rc : claimedCases) {
            if (rc.getClaimedBy() != null && rc.getClaimedAt() != null && rc.getClaimedAt().isBefore(thresholdTime)) {
                log.info("Releasing expired claim for ReportCase ID {}", rc.getCaseId());
                rc.setClaimedBy(null);
                rc.setClaimedAt(null);
                rc.setCaseStatus(CaseStatus.PENDING_REVIEW);
                reportCaseRepo.save(rc);
            }
        }
    }

    @Override
    public List<ReportReason> getAllReasons() {
        return reportReasonRepo.findAll();
    }

    @Override
    public List<Report> getReportsByReporter(Long reporterId) {
        User reporter = userRepo.findById(reporterId)
                .orElseThrow(() -> new GlobalException(404, "User not found"));
        return reportRepo.findAllByReporterOrderByCreatedAtDesc(reporter);
    }

    @Override
    @Transactional
    public ReportReason createReason(ReportReasonRequest request) {
        ReportReason reason = ReportReason.builder()
                .reasonName(request.getReasonName())
                .severityLevel(request.getSeverityLevel())
                .description(request.getDescription())
                .reportThreshold(request.getReportThreshold())
                .penaltyScore(request.getPenaltyScore())
                .build();
        return reportReasonRepo.save(reason);
    }

    @Override
    @Transactional
    public ReportReason updateReason(Long reasonId, ReportReasonRequest request) {
        ReportReason reason = reportReasonRepo.findById(reasonId)
                .orElseThrow(() -> new GlobalException(404, "Report reason not found"));
        reason.setReasonName(request.getReasonName());
        reason.setSeverityLevel(request.getSeverityLevel());
        reason.setDescription(request.getDescription());
        reason.setReportThreshold(request.getReportThreshold());
        reason.setPenaltyScore(request.getPenaltyScore());
        return reportReasonRepo.save(reason);
    }

    @Override
    @Transactional
    public void deleteReason(Long reasonId) {
        ReportReason reason = reportReasonRepo.findById(reasonId)
                .orElseThrow(() -> new GlobalException(404, "Report reason not found"));

        boolean isReferenced = reportCaseRepo.existsByReason(reason) || reportRepo.existsByReason(reason);
        if (isReferenced) {
            throw new GlobalException(400, "Cannot delete report reason because it is already associated with existing reports.");
        }

        reportReasonRepo.delete(reason);
    }
}
