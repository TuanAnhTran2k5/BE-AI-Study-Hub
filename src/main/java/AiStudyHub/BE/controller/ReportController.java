package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.ReportRequest;
import AiStudyHub.BE.dto.Request.AppealRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ReportResponse;
import AiStudyHub.BE.dto.Response.AppealResponse;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.IReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import AiStudyHub.BE.entity.ReportReason;
import java.util.List;

@RestController
@RequestMapping("/api/user/reports")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@Tag(name = "report-controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    IReport reportService;

    @PostMapping
    @Operation(summary = "Submit a report for a document violation")
    public ResponseEntity<APIResponse<ReportResponse>> submitReport(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ReportRequest request) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        Report report = reportService.createReport(
                currentUser.getUserId(),
                request.getDocumentId(),
                request.getReasonId(),
                request.getDescription(),
                request.getEvidenceUrl()
        );

        ReportResponse response = ReportResponse.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporter().getUserId())
                .documentId(report.getDocument().getDocumentId())
                .reasonId(report.getReason().getReasonId())
                .description(report.getDescription())
                .evidenceUrl(report.getEvidenceUrl())
                .createdAt(report.getCreatedAt())
                .status(report.getStatus())
                .build();

        return ResponseEntity.ok(
                APIResponse.response(200, "Report submitted successfully", response)
        );
    }

    @GetMapping("/reasons")
    @Operation(summary = "Get list of all report reasons")
    public ResponseEntity<APIResponse<List<ReportReason>>> getAllReasons() {
        List<ReportReason> reasons = reportService.getAllReasons();
        return ResponseEntity.ok(
                APIResponse.response(200, "Get list of report reasons successfully", reasons)
        );
    }

    @GetMapping
    @Operation(summary = "Get list of reports submitted by the current user")
    public ResponseEntity<APIResponse<List<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        List<ReportResponse> responses = reportService.getReportsByReporter(currentUser.getUserId())
                .stream()
                .map(report -> ReportResponse.builder()
                        .reportId(report.getReportId())
                        .reporterId(report.getReporter().getUserId())
                        .documentId(report.getDocument().getDocumentId())
                        .reasonId(report.getReason().getReasonId())
                        .description(report.getDescription())
                        .evidenceUrl(report.getEvidenceUrl())
                        .createdAt(report.getCreatedAt())
                        .status(report.getStatus())
                        .build())
                .toList();

        return ResponseEntity.ok(
                APIResponse.response(200, "Get your reports successfully", responses)
        );
    }

    @PostMapping("/appeals")
    @Operation(summary = "Submit an appeal request for a resolved report case")
    public ResponseEntity<APIResponse<AppealResponse>> submitAppeal(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AppealRequest request) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        AiStudyHub.BE.entity.Appeal appeal = reportService.submitAppeal(
                request.getCaseId(),
                currentUser.getUserId(),
                request.getAppealReason(),
                request.getEvidenceUrl()
        );

        AppealResponse response = toAppealResponse(appeal);

        return ResponseEntity.ok(
                APIResponse.response(200, "Appeal submitted successfully", response)
        );
    }

    @GetMapping("/appeals")
    @Operation(summary = "Get list of appeals submitted by the current user")
    public ResponseEntity<APIResponse<List<AppealResponse>>> getMyAppeals(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        List<AppealResponse> responses = reportService.getAppealsByUser(currentUser.getUserId())
                .stream()
                .map(this::toAppealResponse)
                .toList();

        return ResponseEntity.ok(
                APIResponse.response(200, "Get your appeals successfully", responses)
        );
    }

    private AppealResponse toAppealResponse(AiStudyHub.BE.entity.Appeal appeal) {
        return AppealResponse.builder()
                .appealId(appeal.getAppealId())
                .caseId(appeal.getReportCase().getCaseId())
                .userId(appeal.getUser().getUserId())
                .documentTitle(appeal.getReportCase().getDocument().getTitle())
                .appealReason(appeal.getAppealReason())
                .evidenceUrl(appeal.getEvidenceUrl())
                .status(appeal.getStatus())
                .createdAt(appeal.getCreatedAt())
                .resolvedAt(appeal.getResolvedAt())
                .resolvedByAdminName(appeal.getResolvedBy() != null ? appeal.getResolvedBy().getFullName() : null)
                .adminNote(appeal.getAdminNote())
                .build();
    }
}
