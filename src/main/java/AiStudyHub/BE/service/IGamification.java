package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.BadgeResponse;
import AiStudyHub.BE.dto.Response.RankingResponse;
import AiStudyHub.BE.dto.Response.RatingResponse;
import AiStudyHub.BE.dto.Response.UserBadgeResponse;
import AiStudyHub.BE.dto.Response.UserRankResponse;
import AiStudyHub.BE.dto.Response.WeeklyScoreResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;

import java.util.List;

public interface IGamification {

    // --- SCORE ---
    record ScoreTypeSpec(String code, String name, int defaultPoint, String description) {}
    int awardScore(User user, Document document, ScoreTypeSpec spec, int scoreChange, String description);

    // --- RANKING & BADGE ---
    boolean checkAndAwardBadges(Long userId);
    boolean updateUserRank(Long userId);
    boolean awardTopWeeklyContributors();
    boolean addWeeklyScore(Long userId, int scoreChange);
    List<RankingResponse> getAllRanks();
    List<BadgeResponse> getAllBadges();
    UserRankResponse getUserRank(Long userId);
    List<UserBadgeResponse> getUserBadges(Long userId);
    List<WeeklyScoreResponse> getTopWeeklyContributors(int limit);

    // --- RATING ---
    RatingResponse submitRating(Long documentId, RatingRequest request);

    // --- REPUTATION ---
    int runDailyReputation();
    boolean applyReputation(Long documentId);
    boolean recomputeAllAggregates();
}
