package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.AdminDecision;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.ReportCase;

public interface IReport {
    Report createReport(Long reporterId, Long documentId, Long reasonId, String description, String evidenceUrl);
    ReportCase claimCase(Long caseId, Long adminId);
    ReportCase unclaimCase(Long caseId, Long adminId);
    ReportCase adminResolveCase(Long caseId, Long adminId, AdminDecision decision, String note);
}
