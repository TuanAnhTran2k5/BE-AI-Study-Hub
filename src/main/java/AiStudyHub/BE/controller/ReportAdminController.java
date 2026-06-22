package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.AdminDecision;
import AiStudyHub.BE.constraint.CaseStatus;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ReportCaseAdminView;
import AiStudyHub.BE.dto.Response.ReportDetailView;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.ReportCase;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.ReportCaseRepo;
import AiStudyHub.BE.repository.ReportRepo;
import AiStudyHub.BE.service.IReport;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/admin/report-cases")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportCaseRepo reportCaseRepo;
    private final ReportRepo reportRepo;
    private final IReport reportService;

    @GetMapping("/pending")
    public ResponseEntity<APIResponse<List<ReportCaseAdminView>>> getPendingCases() {
        List<ReportCaseAdminView> pendingCases = reportCaseRepo.findAllByCaseStatus(CaseStatus.PENDING_REVIEW)
                .stream()
                .map(this::toCaseAdminView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get pending report cases successfully", pendingCases));
    }

    @GetMapping("/history")
    public ResponseEntity<APIResponse<List<ReportCaseAdminView>>> getHistoryCases() {
        List<CaseStatus> statuses = List.of(CaseStatus.WARNING_1, CaseStatus.WARNING_2, CaseStatus.RESOLVED, CaseStatus.REJECTED);
        List<ReportCaseAdminView> historyCases = reportCaseRepo.findAllByCaseStatusInOrderByResolvedAtDesc(statuses)
                .stream()
                .map(this::toCaseAdminView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get report cases history successfully", historyCases));
    }

    @GetMapping("/{caseId}/reports")
    public ResponseEntity<APIResponse<List<ReportDetailView>>> getReportsByCase(@PathVariable Long caseId) {
        ReportCase reportCase = reportCaseRepo.findById(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));

        List<ReportDetailView> reports = reportRepo.findAllByReportCase(reportCase)
                .stream()
                .map(this::toReportDetailView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get detailed reports of case successfully", reports));
    }

    @PostMapping("/{caseId}/claim")
    public ResponseEntity<APIResponse<ReportCaseAdminView>> claimCase(
            @PathVariable Long caseId,
            @RequestParam Long adminId
    ) {
        ReportCase rc = reportService.claimCase(caseId, adminId);
        return ResponseEntity.ok(APIResponse.response(200, "Claim report case successfully", toCaseAdminView(rc)));
    }

    @PostMapping("/{caseId}/unclaim")
    public ResponseEntity<APIResponse<ReportCaseAdminView>> unclaimCase(
            @PathVariable Long caseId,
            @RequestParam Long adminId
    ) {
        ReportCase rc = reportService.unclaimCase(caseId, adminId);
        return ResponseEntity.ok(APIResponse.response(200, "Unclaim report case successfully", toCaseAdminView(rc)));
    }

    @PostMapping("/{caseId}/resolve")
    public ResponseEntity<APIResponse<ReportCaseAdminView>> resolveCase(
            @PathVariable Long caseId,
            @RequestBody ResolveRequest request
    ) {
        ReportCase rc = reportService.adminResolveCase(caseId, request.getAdminId(), request.getDecision(), request.getNote());
        return ResponseEntity.ok(APIResponse.response(200, "Resolve report case successfully", toCaseAdminView(rc)));
    }

    private ReportCaseAdminView toCaseAdminView(ReportCase rc) {
        return ReportCaseAdminView.builder()
                .caseId(rc.getCaseId())
                .documentId(rc.getDocument().getDocumentId())
                .documentTitle(rc.getDocument().getTitle())
                .reasonName(rc.getReason().getReasonName())
                .caseLevel(rc.getCaseLevel())
                .reportCount(rc.getReportCount())
                .requiredThreshold(rc.getRequiredThreshold())
                .caseStatus(rc.getCaseStatus())
                .openedAt(rc.getOpenedAt())
                .resolvedAt(rc.getResolvedAt())
                .resolvedByName(rc.getResolvedBy() != null ? rc.getResolvedBy().getFullName() : null)
                .adminNote(rc.getAdminNote())
                .build();
    }

    private ReportDetailView toReportDetailView(Report r) {
        return ReportDetailView.builder()
                .reportId(r.getReportId())
                .reporterName(r.getReporter().getFullName())
                .description(r.getDescription())
                .evidenceUrl(r.getEvidenceUrl())
                .createdAt(r.getCreatedAt())
                .status(r.getStatus())
                .build();
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResolveRequest {
        Long adminId;
        AdminDecision decision;
        String note;
    }
}
