package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.constraint.ScoreTypeCode;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.RatingMapper;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.impl.ReputationPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GamificationService implements IGamification {

    static final String RATING_REPUTATION = "RATING_REPUTATION";

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private DocumentRepo documentRepo;
    @Autowired
    private RankingRepo rankingRepo;
    @Autowired
    private BadgeRepo badgeRepo;
    @Autowired
    private UserRankRepo userRankRepo;
    @Autowired
    private UserBadgeRepo userBadgeRepo;
    @Autowired
    private WeeklyScoreRepo weeklyScoreRepo;
    @Autowired
    private ScoreTypeRepo scoreTypeRepo;
    @Autowired
    private ScoreLogRepo scoreLogRepo;
    @Autowired
    private RatingRepo ratingRepo;
    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    @Lazy
    private IGamification self;

    private Map<String, ScoreType> scoreTypeCache = new ConcurrentHashMap<>();

    // ==========================================
    //                 SCORE
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int awardScore(ScoreContextResponse context) {
        ScoreType type = scoreTypeCache.computeIfAbsent(context.getSpec().code(), code ->
                scoreTypeRepo.findByTypeCode(code)
                        .orElseGet(() -> scoreTypeRepo.save(ScoreType.builder()
                                .typeCode(code)
                                .typeName(context.getSpec().name())
                                .defaultPoint(context.getSpec().defaultPoint())
                                .description(context.getSpec().description())
                                .build())));

        User receiver = userRepo.findById(context.getReceiverUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        long current = receiver.getTotalScore() == null ? 0L : receiver.getTotalScore();
        receiver.setTotalScore(current + context.getScoreChange());
        userRepo.save(receiver);

        // Generate uniqueActionKey for non-repeatable score types
        String uniqueKey = null;
        if (ScoreTypeCode.BOOKMARK.name().equals(context.getSpec().code()) || ScoreTypeCode.DOC_DOWNLOAD.name().equals(context.getSpec().code())) {
            uniqueKey = context.getSpec().code() + ":" + context.getActorUserId() + ":" + context.getDocumentId();
        }

        scoreLogRepo.save(ScoreLog.builder()
                .user(receiver)
                .documentId(context.getDocumentId())
                .documentTitle(context.getDocumentTitle())
                .scoreType(type)
                .scoreChange(context.getScoreChange())
                .description(context.getDescription())
                .actorUserId(context.getActorUserId())
                .uniqueActionKey(uniqueKey)
                .build());

        return context.getScoreChange();
    }

    // ==========================================
    //             RANKING & BADGE
    // ==========================================

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
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return false;
        
        int score = user.getTotalScore() == null ? 0 : user.getTotalScore().intValue();

        List<Ranking> allRanks = rankingRepo.findAll();
        Optional<Ranking> qualifiedRankOpt = allRanks.stream()
                .filter(r -> r.getMinScore() <= score)
                .max(Comparator.comparingInt(Ranking::getMinScore));
        if (qualifiedRankOpt.isEmpty()) return false;

        Ranking newRank = qualifiedRankOpt.get();

        List<UserRank> history = userRankRepo.findByUser(user);
        Optional<UserRank> currentUserRankOpt = history.stream()
                .max(Comparator.comparing(UserRank::getAchievedAt));

        if (currentUserRankOpt.isPresent()) {
            UserRank current = currentUserRankOpt.get();
            int currentPriority = Integer.parseInt(current.getRank().getDisplayPriority());
            int newPriority = Integer.parseInt(newRank.getDisplayPriority());
            
            if (newPriority != currentPriority) {
                userRankRepo.save(UserRank.builder()
                        .user(user)
                        .rank(newRank)
                        .build());
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

        topWeeklyScores(currentWeekStart).stream()
                .limit(3)
                .forEach(ws -> awardBadgeNotExist(ws.getUser(), "Top Weekly Contributor"));

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
                        .iconUrl(badge.getIconUrl() == null || badge.getIconUrl().isBlank() ? null : badge.getIconUrl())
                        .build()
        ).toList();
    }

    @Override
    public UserRankResponse getUserRank(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        long score = user.getTotalScore() == null ? 0L : user.getTotalScore();
        List<Ranking> allRanks = rankingRepo.findAll();
        Ranking rankObj = allRanks.stream()
                .filter(r -> r.getMinScore() <= score)
                .max(Comparator.comparingLong(Ranking::getMinScore))
                .orElseGet(() -> allRanks.stream()
                        .filter(r -> r.getMinScore() == 0 || "Bronze".equalsIgnoreCase(r.getRankName()))
                        .findFirst()
                        .orElse(null));

        List<UserRank> history = userRankRepo.findByUser(user);
        UserRank currentRankEntity = history.stream()
                .max(Comparator.comparing(UserRank::getAchievedAt))
                .orElse(null);

        if (rankObj == null) return null;

        return UserRankResponse.builder()
                .userRankId(currentRankEntity != null ? currentRankEntity.getUserRankId() : null)
                .userId(user.getUserId())
                .achievedAt(currentRankEntity != null ? currentRankEntity.getAchievedAt() : user.getCreatedAt())
                .updatedAt(currentRankEntity != null ? currentRankEntity.getUpdatedAt() : user.getCreatedAt())
                .rank(RankingResponse.builder()
                        .rankId(rankObj.getRankId())
                        .rankName(rankObj.getRankName())
                        .minScore(rankObj.getMinScore())
                        .maxScore(rankObj.getMaxScore())
                        .storageBonus(rankObj.getStorageBonus())
                        .displayPriority(rankObj.getDisplayPriority())
                        .build())
                .build();
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
                                .iconUrl(ub.getBadge().getIconUrl() == null || ub.getBadge().getIconUrl().isBlank() ? null : ub.getBadge().getIconUrl())
                                .build())
                        .build()
        ).toList();
    }

    @Override
    public List<WeeklyScoreResponse> getTopWeeklyContributors(int limit) {
        LocalDate currentWeekStart = calculateWeekStart(LocalDate.now());

        return topWeeklyScores(currentWeekStart).stream()
                .limit(limit)
                .map(ws -> WeeklyScoreResponse.builder()
                        .userId(ws.getUser().getUserId())
                        .fullName(ws.getUser().getFullName())
                        .avatarUrl(ws.getUser().getAvatarUrl())
                        .email(ws.getUser().getEmail())
                        .score(ws.getScore())
                        .weekStart(ws.getWeekStart())
                        .build())
                .toList();
    }

    // ==========================================
    //                 RATING
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RatingResponse submitRating(Long documentId, RatingRequest request) {
        User authUser = AiStudyHub.BE.security.SecurityUtils.getCurrentUser();

        Integer ratingValue = request.getRatingValue();
        if (ratingValue == null) {
            throw new GlobalException(ErrorCode.FIELD_REQUIRED);
        }
        if (ratingValue < 1 || ratingValue > 5) {
            throw new GlobalException(ErrorCode.INVALID_RATING_VALUE);
        }

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
        }

        User user = userRepo.findById(authUser.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (document.getOwner().getUserId().equals(user.getUserId())) {
            throw new GlobalException(ErrorCode.CANNOT_RATE_OWN_DOCUMENT);
        }

        Rating rating = ratingRepo.findByUserAndDocument(user, document)
                .orElseGet(() -> Rating.builder()
                        .user(user)
                        .document(document)
                        .build());
        rating.setRatingValue(ratingValue);
        rating.setComment(request.getComment());
        rating = ratingRepo.save(rating);

        return ratingMapper.toRatingResponse(rating, document);
        RatingResponse response = ratingMapper.toRatingResponse(rating, document);
        response.setMyRating(ratingValue);
        return response;
    }

    @Override
    public int getPoints(String typeCode, int defaultFallback) {
        return scoreTypeRepo.findByTypeCode(typeCode)
                .map(ScoreType::getDefaultPoint)
                .orElse(defaultFallback);
    }

    @Override
    @Transactional
    public int awardBookmarkScore(Long actorUserId, String actorFullName, Long receiverUserId, Long documentId, String documentTitle, String visibilityStatus) {
        // 1. Kiểm tra lịch sử đã được cộng điểm cho tài liệu này chưa
        if (scoreLogRepo.existsByActorUserIdAndDocumentIdAndScoreTypeTypeCode(
                actorUserId, documentId, ScoreTypeCode.BOOKMARK.name())) {
            return 0; 
        }

        // 2. Lấy cấu hình điểm động từ DB
        int points = this.getPoints(ScoreTypeCode.BOOKMARK.name(), 3);
        String desc = "Awarded " + points + " points because " + actorFullName + " bookmarked your document: " + documentTitle;

        // 3. Thực hiện cộng điểm qua ScoreContext decoupled
        this.awardScore(ScoreContextResponse.builder()
                .receiverUserId(receiverUserId)
                .documentId(documentId)
                .documentTitle(documentTitle)
                .spec(new IGamification.ScoreTypeSpec(ScoreTypeCode.BOOKMARK.name(), "Document bookmarked", points, "Score awarded when another user bookmarks your document"))
                .scoreChange(points)
                .description(desc)
                .actorUserId(actorUserId)
                .build()
        );
        this.updateUserRank(receiverUserId);
        this.addWeeklyScore(receiverUserId, points);

        return points;
    }

    // ==========================================
    //               REPUTATION
    // ==========================================

    @Override
    @Scheduled(cron = "${reputation.job.cron:0 0 2 * * *}")
    public int runDailyReputation() {
        self.recomputeAllAggregates();

        List<Document> docs = documentRepo.findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus.PUBLIC, 10)
                .stream()
                .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                .toList();
        log.info("Reputation job started: {} eligible document(s)", docs.size());

        int processed = 0;
        for (Document doc : docs) {
            try {
                self.applyReputation(doc.getDocumentId());
                processed++;
            } catch (Exception ex) {
                log.error("Reputation failed for documentId={}: {}", doc.getDocumentId(), ex.getMessage(), ex);
            }
        }

        log.info("Reputation job finished: {}/{} document(s) processed", processed, docs.size());
        return processed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyReputation(Long documentId) {
        Document doc = documentRepo.findById(documentId).orElse(null);
        if (doc == null) return false;

        int count = doc.getRatingCount() == null ? 0 : doc.getRatingCount();
        if (count < 10) return false;

        double avg = doc.getAverageRating() == null ? 0.0 : doc.getAverageRating();
        int change = ReputationPolicy.tierScore(avg, count);

        User owner = doc.getOwner();
        self.awardScore(ScoreContextResponse.builder()
                .receiverUserId(owner.getUserId())
                .documentId(doc.getDocumentId())
                .documentTitle(doc.getTitle())
                .spec(new IGamification.ScoreTypeSpec(ScoreTypeCode.RATING_REPUTATION.name(), "Rating Reputation", 0, "Daily reputation score derived from document ratings"))
                .scoreChange(change)
                .description("Reputation " + (change >= 0 ? "+" : "") + change + " for avg=" + avg + ", count=" + count)
                .build()
        );

        self.updateUserRank(owner.getUserId());
        self.addWeeklyScore(owner.getUserId(), change);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recomputeAllAggregates() {
        List<Document> docsWithRatings = documentRepo.findByVisibilityStatus(VisibilityStatus.PUBLIC)
                .stream()
                .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                .toList();
        for (Document doc : docsWithRatings) {
            List<Rating> ratings = ratingRepo.findByDocument(doc);
            int count = ratings.size();
            if (count == 0) {
                doc.setRatingCount(0);
                doc.setAverageRating(0.0);
            } else {
                double avg = ratings.stream().mapToDouble(Rating::getRatingValue).average().orElse(0.0);
                doc.setRatingCount(count);
                doc.setAverageRating(round2(avg));
            }
            documentRepo.save(doc);
        }
        return true;
    }

    // ==========================================
    //             PRIVATE HELPERS
    // ==========================================

    private List<WeeklyScore> topWeeklyScores(LocalDate weekStart) {
        return weeklyScoreRepo.findByWeekStart(weekStart).stream()
                .filter(ws -> ws.getScore() != null && ws.getScore() > 0)
                .sorted(Comparator.comparingInt(WeeklyScore::getScore).reversed())
                .toList();
    }

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
        LocalDate current = date;
        while (current.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }
        return current;
    }

    private static double round2(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public long getEventCount(ScoreTypeCode code) {
        return scoreLogRepo.countByScoreTypeTypeCode(code.name());
    }
}
