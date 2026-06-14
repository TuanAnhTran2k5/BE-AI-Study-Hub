package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService implements IUser {

    @Autowired
    UserRepo userRepo;
    @Autowired
    UserMapper userMapper;
    @Autowired
    AiStudyHub.BE.repository.UserRankRepo userRankRepo;
    @Autowired
    AiStudyHub.BE.repository.UserBadgeRepo userBadgeRepo;

    @Override
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromRequest(request, user);

        userRepo.save(user);

        UpdateProfileResponse response = userMapper.toUpdateProfileResponse(user);
        
        response.setRank(userRankRepo.findByUser(user).map(ur ->
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
        ).orElse(null));
        
        response.setBadges(userBadgeRepo.findByUser(user).stream().map(ub ->
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
        ).collect(java.util.stream.Collectors.toList()));

        return response;
    }
}
