package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.service.IDashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboard dashboardService;

    @GetMapping("/statistics")
    @Operation(summary = "Get system statistics successfully")
    public ResponseEntity<APIResponse<SystemStatisticsResponse>> getSystemStatistics() {
        return ResponseEntity.ok(APIResponse.response(200, "Get system statistics successfully", dashboardService.getSystemStatistics()));
    }

    @GetMapping("/subject-distribution")
    @Operation(summary = "Get subject distribution successfully")
    public ResponseEntity<APIResponse<List<SubjectDistributionResponse>>> getSubjectDistribution() {
        return ResponseEntity.ok(APIResponse.response(200, "Get subject distribution successfully", dashboardService.getSubjectDistribution()));
    }

    @GetMapping("/system-health")
    @Operation(summary = "Get system health successfully")
    public ResponseEntity<APIResponse<SystemHealthResponse>> getSystemHealth() {
        return ResponseEntity.ok(APIResponse.response(200, "Get system health successfully", dashboardService.getSystemHealth()));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get activity trends successfully")
    public ResponseEntity<APIResponse<List<ActivityTrendResponse>>> getActivityTrends(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ResponseEntity.ok(APIResponse.response(200, "Get activity trends successfully", dashboardService.getActivityTrends(days)));
    }

    @GetMapping("/moderation-summary")
    @Operation(summary = "Get moderation summary successfully")
    public ResponseEntity<APIResponse<ModerationSummaryResponse>> getModerationSummary() {
        return ResponseEntity.ok(APIResponse.response(200, "Get moderation summary successfully", dashboardService.getModerationSummary()));
    }

    @GetMapping("/top-contributors")
    @Operation(summary = "Get top contributors successfully")
    public ResponseEntity<APIResponse<List<TopContributorResponse>>> getTopContributors(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(APIResponse.response(200, "Get top contributors successfully", dashboardService.getTopContributors(limit)));
    }

    @GetMapping("/popular-documents")
    @Operation(summary = "Get popular documents successfully")
    public ResponseEntity<APIResponse<List<PopularDocumentResponse>>> getPopularDocuments(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(APIResponse.response(200, "Get popular documents successfully", dashboardService.getPopularDocuments(limit)));
    }

    @GetMapping("/storage-stats")
    @Operation(summary = "Get storage stats successfully")
    public ResponseEntity<APIResponse<StorageStatsResponse>> getStorageStats() {
        return ResponseEntity.ok(APIResponse.response(200, "Get storage stats successfully", dashboardService.getStorageStats()));
    }
}
