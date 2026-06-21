package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.ReportRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ReportResponse;
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

@RestController
@RequestMapping("/api/user/reports")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
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
}
