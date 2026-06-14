package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.BadgeResponse;
import AiStudyHub.BE.dto.Response.RankingResponse;
import AiStudyHub.BE.dto.Response.UserBadgeResponse;
import AiStudyHub.BE.dto.Response.UserRankResponse;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.impl.IRankingBadgeService;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.exception.GlobalException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RankingBadgeService implements IRankingBadgeService {

    UserRepo userRepo;
    DocumentRepo documentRepo;
    RankingRepo rankingRepo;
    BadgeRepo badgeRepo;
    UserRankRepo userRankRepo;
    UserBadgeRepo userBadgeRepo;
    WeeklyScoreRepo weeklyScoreRepo;

    @Override
    @Transactional
    public boolean checkAndAwardBadges(Long userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return false;

        List<Document> userDocs = documentRepo.findByOwner(user);

        // 1. First Upload
        if (!userDocs.isEmpty()) {
            awardBadgeNotExist(user, "First Upload");
        }

        // 2. Helpful Student
        boolean hasHelpfulDoc = userDocs.stream().anyMatch(doc -> doc.getDownloadCount() != null && doc.getDownloadCount() >= 50);
        if (hasHelpfulDoc) {
            awardBadgeNotExist(user, "Helpful Student");
        }

        // 3. Trusted Author
        int totalDownloads = userDocs.stream().mapToInt(doc -> doc.getDownloadCount() == null ? 0 : doc.getDownloadCount()).sum();
        if (totalDownloads >= 500) {
            awardBadgeNotExist(user, "Trusted Author");
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean updateUserRank(Long userId) {
        // Find User
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return false;
        // Get total score of User
        int score = user.getTotalScore() == null ? 0 : user.getTotalScore().intValue();
        
        // Find highest rank qualified
        List<Ranking> allRanks = rankingRepo.findAll();
        Optional<Ranking> qualifiedRankOpt = allRanks.stream()
                .filter(r -> r.getMinScore() <= score)
                .max(Comparator.comparingInt(Ranking::getMinScore));
        if (qualifiedRankOpt.isEmpty()) return false;

        // Get the new rank after find
        Ranking newRank = qualifiedRankOpt.get();
        
        List<UserRank> history = userRankRepo.findByUser(user);
        Optional<UserRank> currentUserRankOpt = history.stream()
                .max(Comparator.comparing(UserRank::getAchievedAt));
        
        if (currentUserRankOpt.isPresent()) {
            UserRank current = currentUserRankOpt.get();
            // Compare priority to see if it's an upgrade
            int currentPriority = Integer.parseInt(current.getRank().getDisplayPriority());
            int newPriority = Integer.parseInt(newRank.getDisplayPriority());
            // If rank has changed save data rank in DB
            if (newPriority != currentPriority) {
                userRankRepo.save(UserRank.builder()
                        .user(user)
                        .rank(newRank)
                        .build());
                // if user have condition to upgrade rank
                if (newPriority > currentPriority) {
                    updateUserStorageLimit(user, newRank);
                    log.info("Upgraded user {} to rank {}", user.getEmail(), newRank.getRankName());
                } else {
                    log.info("Downgraded user {} to rank {}", user.getEmail(), newRank.getRankName());
                }
            }
        } else {
            userRankRepo.save(UserRank.builder()
                    .user(user)
                    .rank(newRank)
                    .build());
            updateUserStorageLimit(user, newRank);
            log.info("Assigned user {} to rank {}", user.getEmail(), newRank.getRankName());
        }
        
        return true;
    }

    @Override
    @Scheduled(cron = "0 0 23 * * SUN", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public boolean awardTopWeeklyContributors() {
        log.info("Running Top Weekly Contributor job...");

        LocalDate currentWeekStart = calculateWeekStart(LocalDate.now());
        
        List<WeeklyScore> top3 = weeklyScoreRepo.findByWeekStart(currentWeekStart).stream()
                .sorted(java.util.Comparator.comparingInt(WeeklyScore::getScore).reversed())
                .limit(3)
                .toList();
        
        for (WeeklyScore ws : top3) {
            if (ws.getScore() != null && ws.getScore() > 0) {
                awardBadgeNotExist(ws.getUser(), "Top Weekly Contributor");
            }
        }
        log.info("Top Weekly Contributor job finished.");
        return true;
    }

    @Override
    @Transactional
    public boolean addWeeklyScore(Long userId, int scoreChange) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return false;
        LocalDate currentWeekStart = calculateWeekStart(LocalDate.now());
        WeeklyScore ws = weeklyScoreRepo.findByUserAndWeekStart(user, currentWeekStart)
                .orElse(WeeklyScore.builder().user(user).weekStart(currentWeekStart).score(0).build());
        ws.setScore(ws.getScore() + scoreChange);
        weeklyScoreRepo.save(ws);
        return true;
    }

    @Override
    public List<RankingResponse> getAllRanks() {
        return rankingRepo.findAll().stream().map(rank ->
                RankingResponse.builder()
                        .rankId(rank.getRankId())
                        .rankName(rank.getRankName())
                        .minScore(rank.getMinScore())
                        .maxScore(rank.getMaxScore())
                        .storageBonus(rank.getStorageBonus())
                        .displayPriority(rank.getDisplayPriority())
                        .build()
        ).toList();
    }

    @Override
    public List<BadgeResponse> getAllBadges() {
        return badgeRepo.findAll().stream().map(badge ->
                BadgeResponse.builder()
                        .badgeId(badge.getBadgeId())
                        .badgeName(badge.getBadgeName())
                        .description(badge.getDescription())
                        .conditionText(badge.getConditionText())
                        .iconUrl(badge.getIconUrl())
                        .build()
        ).toList();
    }

    @Override
    public UserRankResponse getUserRank(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        List<UserRank> history = userRankRepo.findByUser(user);
        return history.stream()
                .max(java.util.Comparator.comparing(UserRank::getAchievedAt))
                .map(ur ->
                        UserRankResponse.builder()
                                .userRankId(ur.getUserRankId())
                                .userId(user.getUserId())
                                .achievedAt(ur.getAchievedAt())
                                .updatedAt(ur.getUpdatedAt())
                                .rank(RankingResponse.builder()
                                        .rankId(ur.getRank().getRankId())
                                        .rankName(ur.getRank().getRankName())
                                        .minScore(ur.getRank().getMinScore())
                                        .maxScore(ur.getRank().getMaxScore())
                                        .storageBonus(ur.getRank().getStorageBonus())
                                        .displayPriority(ur.getRank().getDisplayPriority())
                                        .build())
                                .build()
        ).orElse(null);
    }

    @Override
    public List<UserBadgeResponse> getUserBadges(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        return userBadgeRepo.findByUser(user).stream().map(ub ->
                UserBadgeResponse.builder()
                        .userBadgeId(ub.getUserBadgeId())
                        .userId(user.getUserId())
                        .achievedAt(ub.getAchievedAt())
                        .badge(BadgeResponse.builder()
                                .badgeId(ub.getBadge().getBadgeId())
                                .badgeName(ub.getBadge().getBadgeName())
                                .description(ub.getBadge().getDescription())
                                .conditionText(ub.getBadge().getConditionText())
                                .iconUrl(ub.getBadge().getIconUrl())
                                .build())
                        .build()
        ).toList();
    }

    // ==========================================
    //          PRIVATE HELPER METHODS
    // ==========================================

    private boolean awardBadgeNotExist(User user, String badgeName) {
        Optional<Badge> badgeOpt = badgeRepo.findByBadgeName(badgeName);
        if (badgeOpt.isEmpty()) return false;
        
        Badge badge = badgeOpt.get();
        if (!userBadgeRepo.existsByUserAndBadge(user, badge)) {
            userBadgeRepo.save(UserBadge.builder()
                    .user(user)
                    .badge(badge)
                    .build());
            log.info("Awarded badge '{}' to user {}", badgeName, user.getEmail());
            return true;
        }
        return false;
    }

    private boolean updateUserStorageLimit(User user, Ranking rank) {
        long baseLimit = 2L * 1024 * 1024 * 1024; // 2GB
        long bonus = rank.getStorageBonus() != null ? rank.getStorageBonus().longValue() : 0L;
        long newLimit = baseLimit + bonus;
        
        long currentLimit = user.getStorageLimit() == null ? baseLimit : user.getStorageLimit();
        
        if (newLimit > currentLimit) {
            user.setStorageLimit(newLimit);
            userRepo.save(user);
        }
        return true;
    }

    private LocalDate calculateWeekStart(LocalDate date) {
        // Find the previous Sunday
        LocalDate current = date;
        while (current.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }
        return current;
    }
}
