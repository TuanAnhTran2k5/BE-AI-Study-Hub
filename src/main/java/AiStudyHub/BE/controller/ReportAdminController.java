package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.AdminDecision;
import AiStudyHub.BE.constraint.CaseStatus;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ReportCaseAdminResponse;
import AiStudyHub.BE.dto.Response.ReportDetailResponse;
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

import AiStudyHub.BE.dto.Request.ReportReasonRequest;
import AiStudyHub.BE.entity.ReportReason;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<APIResponse<List<ReportCaseAdminResponse>>> getPendingCases() {
        List<ReportCaseAdminResponse> pendingCases = reportCaseRepo.findAllByCaseStatus(CaseStatus.PENDING_REVIEW)
                .stream()
                .map(this::toCaseAdminView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get pending report cases successfully", pendingCases));
    }

    @GetMapping("/history")
    public ResponseEntity<APIResponse<List<ReportCaseAdminResponse>>> getHistoryCases() {
        List<CaseStatus> statuses = List.of(CaseStatus.WARNING_1, CaseStatus.WARNING_2, CaseStatus.RESOLVED, CaseStatus.REJECTED);
        List<ReportCaseAdminResponse> historyCases = reportCaseRepo.findAllByCaseStatusInOrderByResolvedAtDesc(statuses)
                .stream()
                .map(this::toCaseAdminView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get report cases history successfully", historyCases));
    }

    @GetMapping("/{caseId}/reports")
    public ResponseEntity<APIResponse<List<ReportDetailResponse>>> getReportsByCase(@PathVariable Long caseId) {
        ReportCase reportCase = reportCaseRepo.findById(caseId)
                .orElseThrow(() -> new GlobalException(404, "ReportCase not found"));

        List<ReportDetailResponse> reports = reportRepo.findAllByReportCase(reportCase)
                .stream()
                .map(this::toReportDetailView)
                .toList();
        return ResponseEntity.ok(APIResponse.response(200, "Get detailed reports of case successfully", reports));
    }

    @PostMapping("/{caseId}/claim")
    public ResponseEntity<APIResponse<ReportCaseAdminResponse>> claimCase(
            @PathVariable Long caseId,
            @RequestParam Long adminId
    ) {
        ReportCase rc = reportService.claimCase(caseId, adminId);
        return ResponseEntity.ok(APIResponse.response(200, "Claim report case successfully", toCaseAdminView(rc)));
    }

    @PostMapping("/{caseId}/unclaim")
    public ResponseEntity<APIResponse<ReportCaseAdminResponse>> unclaimCase(
            @PathVariable Long caseId,
            @RequestParam Long adminId
    ) {
        ReportCase rc = reportService.unclaimCase(caseId, adminId);
        return ResponseEntity.ok(APIResponse.response(200, "Unclaim report case successfully", toCaseAdminView(rc)));
    }

    @PostMapping("/{caseId}/resolve")
    public ResponseEntity<APIResponse<ReportCaseAdminResponse>> resolveCase(
            @PathVariable Long caseId,
            @RequestBody ResolveRequest request
    ) {
        ReportCase rc = reportService.adminResolveCase(caseId, request.getAdminId(), request.getDecision(), request.getNote());
        return ResponseEntity.ok(APIResponse.response(200, "Resolve report case successfully", toCaseAdminView(rc)));
    }

    @PostMapping("/reasons")
    @Operation(summary = "Create a new report reason (Admin)")
    public ResponseEntity<APIResponse<ReportReason>> createReason(@Valid @RequestBody ReportReasonRequest request) {
        ReportReason reason = reportService.createReason(request);
        return ResponseEntity.ok(APIResponse.response(201, "Create report reason successfully", reason));
    }

    @PutMapping("/reasons/{reasonId}")
    @Operation(summary = "Update an existing report reason (Admin)")
    public ResponseEntity<APIResponse<ReportReason>> updateReason(
            @PathVariable Long reasonId,
            @Valid @RequestBody ReportReasonRequest request) {
        ReportReason reason = reportService.updateReason(reasonId, request);
        return ResponseEntity.ok(APIResponse.response(200, "Update report reason successfully", reason));
    }

    @DeleteMapping("/reasons/{reasonId}")
    @Operation(summary = "Delete an existing report reason (Admin)")
    public ResponseEntity<APIResponse<Void>> deleteReason(@PathVariable Long reasonId) {
        reportService.deleteReason(reasonId);
        return ResponseEntity.ok(APIResponse.response(200, "Delete report reason successfully", null));
    }

    private ReportCaseAdminResponse toCaseAdminView(ReportCase rc) {
        return ReportCaseAdminResponse.builder()
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

    private ReportDetailResponse toReportDetailView(Report r) {
        return ReportDetailResponse.builder()
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
