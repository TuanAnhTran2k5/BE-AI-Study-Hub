package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.*;
import java.util.List;

public interface IDashboard {
    SystemStatisticsResponse getSystemStatistics();
    List<SubjectDistributionResponse> getSubjectDistribution();
    SystemHealthResponse getSystemHealth();
    List<ActivityTrendResponse> getActivityTrends(int days);
    ModerationSummaryResponse getModerationSummary();
    List<TopContributorResponse> getTopContributors(int limit);
    List<PopularDocumentResponse> getPopularDocuments(int limit);
    StorageStatsResponse getStorageStats();
}
