package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import AiStudyHub.BE.constraint.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    Long userId;
    String fullName;
    String avatarUrl;
    @Builder.Default
    Long totalScore = 0L;
    String email;
    UserRole role;
    UserStatus status;
    String displayRole;
    String displayStatus;
    LocalDateTime createdAt;

    @Builder.Default
    Long storageUsed = 0L;
    @Builder.Default
    Long storageLimit = 0L;
    Long storageRemaining;
    Double storageUsagePercent;

    UserRankResponse currentRank;
    RankProgress rankProgress;
    List<UserBadgeResponse> badges;
    ProfileStatistics statistics;
    LeaderboardInfo leaderboard;
    Integer unreadNotificationCount;

    String accessToken;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProfileStatistics {
        long documents;
        long downloads;
        long bookmarks;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RankProgress {
        String nextRank;
        Integer remainingScore;
        Double progressPercent;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LeaderboardInfo {
        Integer globalRank;
        Integer weeklyRank;
    }
}
