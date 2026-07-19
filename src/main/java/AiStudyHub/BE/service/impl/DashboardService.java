package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.IDashboard;
import io.qdrant.client.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService implements IDashboard {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepo userRepo;
    private final DocumentRepo documentRepo;
    private final DownloadRepo downloadRepo;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportCaseRepo reportCaseRepo;
    private final SubjectRepo subjectRepo;
    private final RankingRepo rankingRepo;
    private final QdrantClient qdrantClient;
    private final DataSource dataSource;
    private final CacheManager cacheManager;

    private final AtomicLong lastAccessedTime = new AtomicLong(0L);

    @Scheduled(fixedDelay = 60000)
    public void refreshDashboardCaches() {
        long current = System.currentTimeMillis();
        long diff = current - lastAccessedTime.get();
        if (diff < 600000) { // 10 minutes
            log.info("Refreshing dashboard caches in background (Cache Warming)...");
            try {
                SystemStatisticsResponse stats = calculateSystemStatistics();
                cacheManager.getCache("dashboard_statistics").put(org.springframework.cache.interceptor.SimpleKey.EMPTY, stats);

                List<SubjectDistributionResponse> subjects = calculateSubjectDistribution();
                cacheManager.getCache("dashboard_subject_dist").put(org.springframework.cache.interceptor.SimpleKey.EMPTY, subjects);

                StorageStatsResponse storage = calculateStorageStats();
                cacheManager.getCache("dashboard_storage").put(org.springframework.cache.interceptor.SimpleKey.EMPTY, storage);

                log.info("Dashboard caches successfully warmed.");
            } catch (Exception e) {
                log.error("Failed to warm dashboard caches: {}", e.getMessage(), e);
            }
        } else {
            log.debug("Inactivity threshold reached (last accessed {}s ago). Skipping dashboard cache warming.", diff / 1000);
        }
    }

    @Override
    @Cacheable("dashboard_statistics")
    public SystemStatisticsResponse getSystemStatistics() {
        lastAccessedTime.set(System.currentTimeMillis());
        return calculateSystemStatistics();
    }

    private SystemStatisticsResponse calculateSystemStatistics() {
        ZonedDateTime vnNow = ZonedDateTime.now(VN_ZONE);
        LocalDateTime now = vnNow.toLocalDateTime();
        LocalDateTime thirtyDaysAgo = vnNow.minusDays(30).toLocalDateTime();
        LocalDateTime sixtyDaysAgo = vnNow.minusDays(60).toLocalDateTime();

        // 1. Active Users
        long currentUsers = userRepo.countByStatus(UserStatus.ACTIVE);
        long prevUsers = userRepo.countByStatusAndCreatedAtBetween(UserStatus.ACTIVE, sixtyDaysAgo, thirtyDaysAgo);
        long currentPeriodUsers = userRepo.countByStatusAndCreatedAtBetween(UserStatus.ACTIVE, thirtyDaysAgo, now);
        
        // 2. Documents (uploadStatus=COMPLETED, moderationStatus=NORMAL, deletedAt IS NULL)
        long currentDocs = documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNull(
                UploadStatus.COMPLETED, ModerationStatus.NORMAL);
        long prevDocs = documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNullAndCreatedAtBetween(
                UploadStatus.COMPLETED, ModerationStatus.NORMAL, sixtyDaysAgo, thirtyDaysAgo);
        long currentPeriodDocs = documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNullAndCreatedAtBetween(
                UploadStatus.COMPLETED, ModerationStatus.NORMAL, thirtyDaysAgo, now);

        // 3. Downloads
        long currentDownloads = downloadRepo.count();
        long prevDownloads = downloadRepo.countByDownloadedAtBetween(sixtyDaysAgo, thirtyDaysAgo);
        long currentPeriodDownloads = downloadRepo.countByDownloadedAtBetween(thirtyDaysAgo, now);

        // 4. AI Queries (senderType=USER)
        long currentQueries = chatMessageRepository.countBySenderType(SenderType.USER);
        long prevQueries = chatMessageRepository.countBySenderTypeAndCreatedAtBetween(SenderType.USER, sixtyDaysAgo, thirtyDaysAgo);
        long currentPeriodQueries = chatMessageRepository.countBySenderTypeAndCreatedAtBetween(SenderType.USER, thirtyDaysAgo, now);

        return SystemStatisticsResponse.builder()
                .totalActiveUsers(SystemStatisticsResponse.StatisticCard.builder()
                        .value(currentUsers)
                        .growthRate(calculateGrowthRate(currentPeriodUsers, prevUsers))
                        .build())
                .totalDocuments(SystemStatisticsResponse.StatisticCard.builder()
                        .value(currentDocs)
                        .growthRate(calculateGrowthRate(currentPeriodDocs, prevDocs))
                        .build())
                .totalDownloads(SystemStatisticsResponse.StatisticCard.builder()
                        .value(currentDownloads)
                        .growthRate(calculateGrowthRate(currentPeriodDownloads, prevDownloads))
                        .build())
                .totalAiQueries(SystemStatisticsResponse.StatisticCard.builder()
                        .value(currentQueries)
                        .growthRate(calculateGrowthRate(currentPeriodQueries, prevQueries))
                        .build())
                .build();
    }

    @Override
    @Cacheable("dashboard_subject_dist")
    public List<SubjectDistributionResponse> getSubjectDistribution() {
        lastAccessedTime.set(System.currentTimeMillis());
        return calculateSubjectDistribution();
    }

    private List<SubjectDistributionResponse> calculateSubjectDistribution() {
        long totalActiveDocs = documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNull(
                UploadStatus.COMPLETED, ModerationStatus.NORMAL);

        if (totalActiveDocs == 0) {
            return Collections.emptyList();
        }

        List<Object[]> rawList = documentRepo.countDocumentsGroupBySubject();
        List<SubjectDistributionResponse> distribution = new ArrayList<>();

        for (Object[] row : rawList) {
            Long subjectId = (Long) row[0];
            long count = (Long) row[1];
            
            Subject subject = subjectRepo.findById(subjectId).orElse(null);
            if (subject != null && !Boolean.TRUE.equals(subject.getIsDeleted())) {
                String subjectDisplayName = String.format("%s (%s)", subject.getSubjectName(), subject.getSubjectCode());
                double percentage = (count * 100.0) / totalActiveDocs;
                
                distribution.add(SubjectDistributionResponse.builder()
                        .subjectName(subjectDisplayName)
                        .documentCount(count)
                        .percentage(Math.round(percentage * 100.0) / 100.0) // round to 2 decimal places
                        .build());
            }
        }

        return distribution;
    }

    @Override
    public SystemHealthResponse getSystemHealth() {
        String apiGatewayStatus = "Healthy"; // Simulated status or check
        int activeRagNodes = 0;
        
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    qdrantClient.listCollectionsAsync().get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(500, TimeUnit.MILLISECONDS);
            activeRagNodes = 1;
        } catch (Exception e) {
            log.warn("Qdrant database probe failed: {}", e.getMessage());
            activeRagNodes = 0;
        }

        double poolAvailablePercent = 100.0;
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) {
            var poolMxBean = hikari.getHikariPoolMXBean();
            if (poolMxBean != null) {
                int active = poolMxBean.getActiveConnections();
                int max = hikari.getMaximumPoolSize();
                if (max > 0) {
                    poolAvailablePercent = (1.0 - (double) active / max) * 100.0;
                }
            }
        }

        return SystemHealthResponse.builder()
                .apiGatewayStatus(apiGatewayStatus)
                .activeRagNodes(activeRagNodes)
                .poolAvailablePercent(Math.round(poolAvailablePercent * 10.0) / 10.0) // round to 1 decimal place
                .build();
    }

    @Override
    public List<ActivityTrendResponse> getActivityTrends(int days) {
        int cappedDays = Math.min(days, 90);
        if (cappedDays <= 0) cappedDays = 7;

        ZonedDateTime vnNow = ZonedDateTime.now(VN_ZONE);
        LocalDateTime since = vnNow.minusDays(cappedDays - 1).toLocalDate().atStartOfDay();

        // Query raw daily counts
        List<Object[]> rawSignups = userRepo.countSignupsByDate(since);
        List<Object[]> rawDocs = documentRepo.countNewDocumentsByDate(since);
        List<Object[]> rawDownloads = downloadRepo.countDownloadsByDate(since);
        List<Object[]> rawAiQueries = chatMessageRepository.countAiQueriesByDate(since);

        // Convert query results to maps: Date (Date or LocalDate) -> Count
        Map<LocalDate, Long> signupMap = toDateMap(rawSignups);
        Map<LocalDate, Long> docMap = toDateMap(rawDocs);
        Map<LocalDate, Long> downloadMap = toDateMap(rawDownloads);
        Map<LocalDate, Long> queryMap = toDateMap(rawAiQueries);

        List<ActivityTrendResponse> trends = new ArrayList<>();
        LocalDate startLocalDate = vnNow.minusDays(cappedDays - 1).toLocalDate();
        LocalDate endLocalDate = vnNow.toLocalDate();

        for (LocalDate date = startLocalDate; !date.isAfter(endLocalDate); date = date.plusDays(1)) {
            trends.add(ActivityTrendResponse.builder()
                    .date(date)
                    .newUsers(signupMap.getOrDefault(date, 0L))
                    .newDocuments(docMap.getOrDefault(date, 0L))
                    .newDownloads(downloadMap.getOrDefault(date, 0L))
                    .aiQueries(queryMap.getOrDefault(date, 0L))
                    .build());
        }

        return trends;
    }

    @Override
    public ModerationSummaryResponse getModerationSummary() {
        long pendingReportCases = reportCaseRepo.countByCaseStatusIn(
                List.of(CaseStatus.PENDING_REVIEW, CaseStatus.CLAIMED)
        );
        long reportedDocs = documentRepo.countByReportCountGreaterThanAndModerationStatusAndDeletedAtIsNull(
                0, ModerationStatus.NORMAL);
        long pendingUploads = documentRepo.countByUploadStatusAndDeletedAtIsNull(UploadStatus.PENDING);
        long totalBannedUsers = userRepo.countByStatus(UserStatus.BANNED);
        long totalPendingUsers = userRepo.countByStatus(UserStatus.PENDING);

        return ModerationSummaryResponse.builder()
                .pendingReportCasesCount(pendingReportCases)
                .reportedDocumentsCount(reportedDocs)
                .pendingUploadDocumentsCount(pendingUploads)
                .totalBannedUsersCount(totalBannedUsers)
                .totalPendingUsersCount(totalPendingUsers)
                .build();
    }

    @Override
    public List<TopContributorResponse> getTopContributors(int limit) {
        int cappedLimit = Math.min(limit, 100);
        if (cappedLimit <= 0) cappedLimit = 5;

        Page<User> topUsers = userRepo.findAllByStatusOrderByTotalScoreDescUserIdAsc(
                UserStatus.ACTIVE, PageRequest.of(0, cappedLimit));
        List<Ranking> allRankings = rankingRepo.findAll();

        List<TopContributorResponse> contributors = new ArrayList<>();
        for (User u : topUsers.getContent()) {
            long userScore = u.getTotalScore() == null ? 0L : u.getTotalScore();
            
            // Map Rank Name
            Ranking rankObj = allRankings.stream()
                    .filter(r -> r.getMinScore() <= userScore)
                    .max(Comparator.comparingLong(Ranking::getMinScore))
                    .orElseGet(() -> allRankings.stream()
                            .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                            .findFirst()
                            .orElse(null));
            String rankName = rankObj != null ? rankObj.getRankName() : "Bronze";

            // Count Active Documents
            long docCount = documentRepo.countByOwnerUserId(u.getUserId()); // Or countActiveDocumentsGroupByOwnerIds batch if query-heavy, but limit is small

            contributors.add(TopContributorResponse.builder()
                    .userId(u.getUserId())
                    .fullName(u.getFullName())
                    .avatarUrl(u.getAvatarUrl())
                    .totalScore(userScore)
                    .activeDocumentCount(docCount)
                    .rankName(rankName)
                    .build());
        }

        return contributors;
    }

    @Override
    public List<PopularDocumentResponse> getPopularDocuments(int limit) {
        int cappedLimit = Math.min(limit, 100);
        if (cappedLimit <= 0) cappedLimit = 5;

        Page<Document> popDocs = documentRepo.findPopularDocuments(PageRequest.of(0, cappedLimit));
        List<PopularDocumentResponse> list = new ArrayList<>();

        for (Document d : popDocs.getContent()) {
            list.add(PopularDocumentResponse.builder()
                    .documentId(d.getDocumentId())
                    .title(d.getTitle())
                    .ownerName(d.getOwner().getFullName())
                    .subjectName(d.getSubject() != null ? String.format("%s (%s)", d.getSubject().getSubjectName(), d.getSubject().getSubjectCode()) : "")
                    .downloadCount(d.getDownloadCount() != null ? d.getDownloadCount().longValue() : 0L)
                    .averageRating(d.getAverageRating() != null ? d.getAverageRating() : 0.0)
                    .build());
        }

        return list;
    }

    @Override
    @Cacheable("dashboard_storage")
    public StorageStatsResponse getStorageStats() {
        lastAccessedTime.set(System.currentTimeMillis());
        return calculateStorageStats();
    }

    private StorageStatsResponse calculateStorageStats() {
        long totalStorageUsed = documentRepo.sumFileSizeOfActiveDocuments();
        long totalActiveDocuments = documentRepo.countByUploadStatusAndModerationStatusAndDeletedAtIsNull(
                UploadStatus.COMPLETED, ModerationStatus.NORMAL);

        double averageSize = totalActiveDocuments > 0 ? (double) totalStorageUsed / totalActiveDocuments : 0.0;

        List<Object[]> rawTypes = documentRepo.countActiveDocumentsGroupByFileType();
        Map<String, Long> distribution = new HashMap<>();

        for (Object[] row : rawTypes) {
            String fileType = (String) row[0];
            long count = (Long) row[1];
            if (fileType == null || fileType.isBlank()) {
                fileType = "UNKNOWN";
            }
            distribution.put(fileType.toUpperCase(), count);
        }

        return StorageStatsResponse.builder()
                .totalStorageUsed(totalStorageUsed)
                .totalActiveDocuments(totalActiveDocuments)
                .averageDocumentSize(Math.round(averageSize * 100.0) / 100.0)
                .fileTypeDistribution(distribution)
                .build();
    }

    private String calculateGrowthRate(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? "+100.0%" : "+0.0%";
        }
        double rate = ((currentCount - previousCount) * 100.0) / previousCount;
        return (rate >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", rate) + "%";
    }

    private Map<LocalDate, Long> toDateMap(List<Object[]> queryResults) {
        Map<LocalDate, Long> dateMap = new HashMap<>();
        for (Object[] row : queryResults) {
            if (row[0] != null) {
                LocalDate date;
                if (row[0] instanceof Date sqlDate) {
                    date = sqlDate.toLocalDate();
                } else if (row[0] instanceof LocalDate localDate) {
                    date = localDate;
                } else {
                    date = LocalDate.parse(row[0].toString());
                }
                long count = ((Number) row[1]).longValue();
                dateMap.put(date, count);
            }
        }
        return dateMap;
    }
}
