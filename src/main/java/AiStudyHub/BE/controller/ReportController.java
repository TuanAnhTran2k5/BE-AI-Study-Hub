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
import AiStudyHub.BE.entity.ReportReason;
import java.util.List;

import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.service.ISupabaseStorage;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/reports")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@Tag(name = "report-controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportController {

    IReport reportService;
    ISupabaseStorage supabaseStorage;

    @PostMapping(value = "/upload-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload evidence image for document report to Supabase Storage")
    public ResponseEntity<APIResponse<FileUploadResponse>> uploadEvidence(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) throws Exception {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }
        if (file == null || file.isEmpty()) {
            throw new GlobalException(400, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GlobalException(400, "Only image files (PNG, JPG, WEBP, etc.) are allowed for evidence proof.");
        }

        FileUploadResponse uploadRes = supabaseStorage.uploadFile(file, "report-evidences");

        return ResponseEntity.ok(
                APIResponse.response(200, "Upload evidence image successfully", uploadRes)
        );
    }

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
}
