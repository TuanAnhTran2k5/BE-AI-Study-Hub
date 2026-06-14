package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.BadgeResponse;
import AiStudyHub.BE.dto.Response.RankingResponse;
import AiStudyHub.BE.dto.Response.UserBadgeResponse;
import AiStudyHub.BE.dto.Response.UserRankResponse;

import java.util.List;

public interface IRankingBadgeService {
    boolean checkAndAwardBadges(Long userId);
    boolean updateUserRank(Long userId);
    boolean awardTopWeeklyContributors();
    boolean addWeeklyScore(Long userId, int scoreChange);

    List<RankingResponse> getAllRanks();
    List<BadgeResponse> getAllBadges();
    UserRankResponse getUserRank(Long userId);
    List<UserBadgeResponse> getUserBadges(Long userId);
}
