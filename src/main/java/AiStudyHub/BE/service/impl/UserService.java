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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements IUser {

    @Autowired
    UserRepo userRepo;
    @Autowired
    UserMapper userMapper;
    @Autowired
    UserRankRepo userRankRepo;
    @Autowired
    UserBadgeRepo userBadgeRepo;
    @Autowired
    DocumentRepo documentRepo;
    @Autowired
    BookmarkRepo bookmarkRepo;
    @Autowired
    NotificationRepo notificationRepo;
    @Autowired
    WeeklyScoreRepo weeklyScoreRepo;
    @Autowired
    RankingRepo rankingRepo;
    @Autowired
    ISupabaseStorage supabaseStorage;

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromRequest(request, user);

        // Upload avatar if a new one is provided in form-data
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            org.springframework.web.multipart.MultipartFile avatarFile = request.getAvatar();
            
            // Validate content type
            String contentType = avatarFile.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
                throw new GlobalException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
            }
            // Validate file size (max 5MB)
            if (avatarFile.getSize() > 5L * 1024 * 1024) {
                throw new GlobalException(ErrorCode.INVALID_IMAGE_SIZE);
            }
            
            try {
                // Upload file to Supabase in the avatars directory
                FileUploadResponse uploadRes = supabaseStorage.uploadFile(avatarFile, "avatars");
                
                // Delete old custom avatar if it exists
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.isBlank() && !oldAvatarUrl.contains("googleusercontent.com")) {
                    try {
                        supabaseStorage.deleteFile(oldAvatarUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete old avatar file from storage: {}", e.getMessage());
                    }
                }
                
                user.setAvatarUrl(uploadRes.getPublicUrl());
            } catch (Exception e) {
                log.error("Failed to upload new avatar: {}", e.getMessage());
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
            
            // Find current rank object for style
            UserRank currentRankEntity = userRankRepo.findByUser(u).stream()
                    .max(Comparator.comparing(UserRank::getAchievedAt))
                    .orElse(null);
            Ranking rankObj = null;
            if (currentRankEntity != null) {
                rankObj = currentRankEntity.getRank();
            } else {
                rankObj = allRankings.stream()
                        .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                        .findFirst()
                        .orElse(null);
            }
            
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

        // 4. Rank mapping with fallback to Bronze
        UserRank currentRankEntity = userRankRepo.findByUser(user).stream()
                .max(Comparator.comparing(UserRank::getAchievedAt))
                .orElse(null);
                
        Ranking rankObj = null;
        if (currentRankEntity != null) {
            rankObj = currentRankEntity.getRank();
        } else {
            // Find default Bronze rank
            rankObj = rankingRepo.findAll().stream()
                    .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                    .findFirst()
                    .orElse(null);
        }

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
            progress = calculateRankProgress(user.getTotalScore() == null ? 0L : user.getTotalScore(), rankObj, new ArrayList<>(rankingRepo.findAll()));
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
                                .iconUrl(ub.getBadge().getIconUrl() == null || ub.getBadge().getIconUrl().isBlank() ? "default_badge_icon" : ub.getBadge().getIconUrl())
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

        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .totalScore(user.getTotalScore() == null ? 0L : user.getTotalScore())
                .role(user.getRole())
                .status(user.getStatus())
                .displayRole(displayRole)
                .displayStatus(displayStatus)
                .createdAt(user.getCreatedAt())
                .storageUsed(used)
                .storageLimit(limit)
                .storageRemaining(remaining)
                .storageUsagePercent(usagePercent)
                .currentRank(currentRankResponse)
                .rankProgress(progress)
                .badges(badgeResponses)
                .statistics(stats)
                .leaderboard(leaderboard)
                .unreadNotificationCount((int) unreadNotifCount)
                .accessToken(accessToken)
                .build();
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
        while (current.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }
        return current;
    }
}
