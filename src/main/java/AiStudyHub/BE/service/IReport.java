package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.AdminDecision;
import AiStudyHub.BE.constraint.AppealStatus;
import AiStudyHub.BE.dto.Request.ReportReasonRequest;
import AiStudyHub.BE.entity.Appeal;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.ReportCase;
import AiStudyHub.BE.entity.ReportReason;
import java.util.List;

public interface IReport {
    Report createReport(Long reporterId, Long documentId, Long reasonId, String description, String evidenceUrl);
    ReportCase claimCase(Long caseId, Long adminId);
    ReportCase unclaimCase(Long caseId, Long adminId);
    ReportCase adminResolveCase(Long caseId, Long adminId, AdminDecision decision, String note);
    List<ReportReason> getAllReasons();
    List<Report> getReportsByReporter(Long reporterId);
    ReportReason createReason(ReportReasonRequest request);
    ReportReason updateReason(Long reasonId, ReportReasonRequest request);
    void deleteReason(Long reasonId);

    Appeal submitAppeal(Long caseId, Long userId, String appealReason, String evidenceUrl);
    Appeal resolveAppeal(Long appealId, Long adminId, boolean approve, String adminNote);
    List<Appeal> getAppealsByStatus(AppealStatus status);
    List<Appeal> getAppealsByUser(Long userId);
}
