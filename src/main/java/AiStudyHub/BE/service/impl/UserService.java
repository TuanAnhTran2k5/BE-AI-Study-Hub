package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.UserRank;
import AiStudyHub.BE.entity.WeeklyScore;
import AiStudyHub.BE.entity.Ranking;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.IUser;
import AiStudyHub.BE.service.ISupabaseStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService implements IUser {

    UserRepo userRepo;
    UserMapper userMapper;
    UserRankRepo userRankRepo;
    UserBadgeRepo userBadgeRepo;
    DocumentRepo documentRepo;
    BookmarkRepo bookmarkRepo;
    NotificationRepo notificationRepo;
    WeeklyScoreRepo weeklyScoreRepo;
    RankingRepo rankingRepo;
    ISupabaseStorage supabaseStorage;

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromRequest(request, user);

        // Save avatar as Base64 Data URI if a new one is provided in form-data
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            MultipartFile avatarFile = request.getAvatar();
            
            try {
                String contentType = avatarFile.getContentType();
                if (contentType == null || contentType.isBlank() || contentType.equals("application/octet-stream")) {
                    contentType = "image/png";
                }
                byte[] bytes = avatarFile.getBytes();
                String base64Image = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
                
                // Delete old custom avatar from Supabase if it was previously stored there
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.isBlank() && !oldAvatarUrl.contains("googleusercontent.com") && !oldAvatarUrl.startsWith("data:")) {
                    try {
                        supabaseStorage.deleteFile(oldAvatarUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete old avatar file from storage: {}", e.getMessage());
                    }
                }
                
                user.setAvatarUrl(base64Image);
            } catch (Exception e) {
                log.error("Failed to process new avatar: {}", e.getMessage());
                throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        userRepo.save(user);

        return buildUserProfileResponse(user, null);
    }

    @Override
    public UserResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        return buildUserProfileResponse(user, null);
    }

    @Override
    public GlobalLeaderboardResponse getMyLeaderboardRank(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        int rank = (int) (userRepo.countByTotalScoreGreaterThan(user.getTotalScore() == null ? 0L : user.getTotalScore()) + 1);
        int totalUsers = (int) userRepo.count();
        return GlobalLeaderboardResponse.builder()
                .rank(rank)
                .totalUsers(totalUsers)
                .build();
    }

    @Override
    public Page<LeaderboardResponse> getGlobalLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepo.findAllByOrderByTotalScoreDescUserIdAsc(pageable);
        
        List<Ranking> allRankings = rankingRepo.findAll();
        
        List<LeaderboardResponse> entries = new ArrayList<>();
        for (int i = 0; i < userPage.getContent().size(); i++) {
            User u = userPage.getContent().get(i);
            
            // Calculate global rank for this user
            int rank = (int) (userRepo.countByTotalScoreGreaterThan(u.getTotalScore() == null ? 0L : u.getTotalScore()) + 1);
            
            // Calculate rank object based on totalScore
            long userScore = u.getTotalScore() == null ? 0L : u.getTotalScore();
            Ranking rankObj = allRankings.stream()
                    .filter(r -> r.getMinScore() <= userScore)
                    .max(Comparator.comparingLong(Ranking::getMinScore))
                    .orElseGet(() -> allRankings.stream()
                            .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                            .findFirst()
                            .orElse(null));
            
            RankingResponse mappedRank = mapToRankingResponse(rankObj);
            
            entries.add(LeaderboardResponse.builder()
                    .rank(rank)
                    .userId(u.getUserId())
                    .fullName(u.getFullName())
                    .avatarUrl(u.getAvatarUrl())
                    .totalScore(u.getTotalScore() == null ? 0L : u.getTotalScore())
                    .rankName(mappedRank != null ? mappedRank.getRankName() : "")
                    .rankIcon(mappedRank != null ? mappedRank.getIconUrl() : "")
                    .rankColor(mappedRank != null ? mappedRank.getColor() : "")
                    .rankTextColor(mappedRank != null ? mappedRank.getTextColor() : "")
                    .build());
        }
        
        return new PageImpl<>(entries, pageable, userPage.getTotalElements());
    }

    // --- HELPER METHODS ---

    public UserResponse buildUserProfileResponse(User user, String accessToken) {
        // 1. Statistics (Optimized using SQL count/sum)
        long documentCount = documentRepo.countByOwnerUserId(user.getUserId());
        long bookmarkCount = bookmarkRepo.countByUserUserId(user.getUserId());
        long downloadCount = documentRepo.sumDownloadCountByOwnerUserId(user.getUserId());
        UserResponse.ProfileStatistics stats = UserResponse.ProfileStatistics.builder()
                .documents(documentCount)
                .bookmarks(bookmarkCount)
                .downloads(downloadCount)
                .build();

        // 2. Storage limits & math
        long limit = user.getStorageLimit() == null ? 2L * 1024 * 1024 * 1024 : user.getStorageLimit();
        long used = user.getStorageUsed() == null ? 0L : user.getStorageUsed();
        long remaining = limit - used;
        if (remaining < 0) remaining = 0;
        double usagePercent = limit > 0 ? (double) used / limit * 100.0 : 0.0;
        if (usagePercent > 100.0) usagePercent = 100.0;
        if (usagePercent < 0.0) usagePercent = 0.0;

        // 3. User Role and Status text display mapping
        String displayRole = user.getRole() == null ? "USER" : (user.getRole().name().equalsIgnoreCase("AD") ? "ADMIN" : "USER");
        String displayStatus = "Pending Verification";
        if (user.getStatus() != null) {
            if (user.getStatus().name().equalsIgnoreCase("ACTIVE")) {
                displayStatus = "Active";
            } else if (user.getStatus().name().equalsIgnoreCase("BANNED")) {
                displayStatus = "Banned";
            }
        }

        // 4. Rank mapping based on totalScore (with fallback to Bronze)
        long userScore = user.getTotalScore() == null ? 0L : user.getTotalScore();
        List<Ranking> allRankings = rankingRepo.findAll();
        Ranking rankObj = allRankings.stream()
                .filter(r -> r.getMinScore() <= userScore)
                .max(Comparator.comparingLong(Ranking::getMinScore))
                .orElseGet(() -> allRankings.stream()
                        .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                        .findFirst()
                        .orElse(null));

        UserRank currentRankEntity = userRankRepo.findByUser(user).stream()
                .max(Comparator.comparing(UserRank::getAchievedAt))
                .orElse(null);

        UserRankResponse currentRankResponse = null;
        if (rankObj != null) {
            currentRankResponse = UserRankResponse.builder()
                    .userRankId(currentRankEntity != null ? currentRankEntity.getUserRankId() : null)
                    .userId(user.getUserId())
                    .achievedAt(currentRankEntity != null ? currentRankEntity.getAchievedAt() : user.getCreatedAt())
                    .updatedAt(currentRankEntity != null ? currentRankEntity.getUpdatedAt() : user.getCreatedAt())
                    .rank(mapToRankingResponse(rankObj))
                    .build();
        }

        // 5. Rank progress
        UserResponse.RankProgress progress = null;
        if (rankObj != null) {
            progress = calculateRankProgress(user.getTotalScore() == null ? 0L : user.getTotalScore(), rankObj, new ArrayList<>(allRankings));
        }

        // 6. Badges (always default to [])
        List<UserBadgeResponse> badgeResponses = userBadgeRepo.findByUser(user).stream().map(ub ->
                UserBadgeResponse.builder()
                        .userBadgeId(ub.getUserBadgeId())
                        .userId(user.getUserId())
                        .achievedAt(ub.getAchievedAt())
                        .badge(BadgeResponse.builder()
                                .badgeId(ub.getBadge().getBadgeId())
                                .badgeName(ub.getBadge().getBadgeName())
                                .description(ub.getBadge().getDescription())
                                .conditionText(ub.getBadge().getConditionText())
                                .iconUrl(ub.getBadge().getIconUrl() == null || ub.getBadge().getIconUrl().isBlank() ? null : ub.getBadge().getIconUrl())
                                .requiredDownloads(ub.getBadge().getRequiredDownloads())
                                .build())
                        .build()
        ).collect(Collectors.toList());

        // 7. Notification Count (always >= 0)
        long unreadNotifCount = notificationRepo.countByUserUserIdAndIsRead(user.getUserId(), false);

        // 8. Leaderboard Position (weekly and global)
        int globalRank = (int) (userRepo.countByTotalScoreGreaterThan(user.getTotalScore() == null ? 0L : user.getTotalScore()) + 1);
        
        // Find weekly score and rank
        LocalDate weekStart = calculateWeekStart(LocalDate.now());
        WeeklyScore weeklyScore = weeklyScoreRepo.findByUserAndWeekStart(user, weekStart).orElse(null);
        Integer weeklyRank = null;
        if (weeklyScore != null) {
            weeklyRank = (int) (weeklyScoreRepo.findByWeekStart(weekStart).stream()
                    .filter(ws -> ws.getScore() > weeklyScore.getScore())
                    .count() + 1);
        }

        UserResponse.LeaderboardInfo leaderboard = UserResponse.LeaderboardInfo.builder()
                .globalRank(globalRank)
                .weeklyRank(weeklyRank)
                .build();

        UserResponse response = userMapper.toUserResponse(user);
        if (response.getTotalScore() == null) {
            response.setTotalScore(0L);
        }
        response.setDisplayRole(displayRole);
        response.setDisplayStatus(displayStatus);
        response.setStorageUsed(used);
        response.setStorageLimit(limit);
        response.setStorageRemaining(remaining);
        response.setStorageUsagePercent(usagePercent);
        response.setCurrentRank(currentRankResponse);
        response.setRankProgress(progress);
        response.setBadges(badgeResponses);
        response.setStatistics(stats);
        response.setLeaderboard(leaderboard);
        response.setUnreadNotificationCount((int) unreadNotifCount);
        response.setAccessToken(accessToken);
        return response;
    }

    private RankingResponse mapToRankingResponse(Ranking rank) {
        if (rank == null) return null;
        
        String iconUrl = "";
        String color = "";
        String badgeColor = "";
        String textColor = "";
        
        if ("Bronze".equalsIgnoreCase(rank.getRankName())) {
            iconUrl = "bronze_rank_icon";
            color = "#CD7F32";
            badgeColor = "#E6D7C3";
            textColor = "#8B4513";
        } else if ("Silver".equalsIgnoreCase(rank.getRankName())) {
            iconUrl = "silver_rank_icon";
            color = "#C0C0C0";
            badgeColor = "#ECECEC";
            textColor = "#708090";
        } else if ("Gold".equalsIgnoreCase(rank.getRankName())) {
            iconUrl = "gold_rank_icon";
            color = "#FFD700";
            badgeColor = "#FFF8DC";
            textColor = "#B8860B";
        } else if ("Elite Scholar".equalsIgnoreCase(rank.getRankName()) || "EliteScholar".equalsIgnoreCase(rank.getRankName())) {
            iconUrl = "elite_scholar_rank_icon";
            color = "#8A2BE2";
            badgeColor = "#E6E6FA";
            textColor = "#4B0082";
        }
        
        return RankingResponse.builder()
                .rankId(rank.getRankId())
                .rankName(rank.getRankName())
                .minScore(rank.getMinScore())
                .maxScore(rank.getMaxScore())
                .storageBonus(rank.getStorageBonus())
                .displayPriority(rank.getDisplayPriority())
                .iconUrl(iconUrl)
                .color(color)
                .badgeColor(badgeColor)
                .textColor(textColor)
                .build();
    }

    private UserResponse.RankProgress calculateRankProgress(long totalScore, Ranking currentRank, List<Ranking> allRankings) {
        allRankings.sort(Comparator.comparing(Ranking::getMinScore));
        
        Ranking nextRank = null;
        for (int i = 0; i < allRankings.size(); i++) {
            if (allRankings.get(i).getRankId().equals(currentRank.getRankId())) {
                if (i + 1 < allRankings.size()) {
                    nextRank = allRankings.get(i + 1);
                }
                break;
            }
        }
        
        if (nextRank == null) {
            return UserResponse.RankProgress.builder()
                    .nextRank("Max Rank reached")
                    .remainingScore(0)
                    .progressPercent(100.0)
                    .build();
        }
        
        long minCurrent = currentRank.getMinScore();
        long minNext = nextRank.getMinScore();
        long neededRange = minNext - minCurrent;
        long currentProgress = totalScore - minCurrent;
        
        double percent = (double) currentProgress / neededRange * 100.0;
        if (percent < 0) percent = 0.0;
        if (percent > 100) percent = 100.0;
        
        return UserResponse.RankProgress.builder()
                .nextRank(nextRank.getRankName())
                .remainingScore((int) (minNext - totalScore))
                .progressPercent(percent)
                .build();
    }

    private LocalDate calculateWeekStart(LocalDate date) {
        LocalDate current = date;
        while (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }
        return current;
    }
}
