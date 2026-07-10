package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.dto.Response.GlobalLeaderboardResponse;
import AiStudyHub.BE.dto.Response.LeaderboardResponse;
import org.springframework.data.domain.Page;

public interface IUser {
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    UserResponse getProfile(Long userId);
    GlobalLeaderboardResponse getMyLeaderboardRank(Long userId);
    Page<LeaderboardResponse> getGlobalLeaderboard(int page, int size);
    UserResponse buildUserProfileResponse(AiStudyHub.BE.entity.User user, String accessToken);
    org.springframework.data.domain.Page<AiStudyHub.BE.dto.Response.AdminUserResponse> getUsersForAdmin(String search, String status, int page, int size);
    AiStudyHub.BE.dto.Response.AdminUserResponse getUserDetailForAdmin(Long userId);
    AiStudyHub.BE.dto.Response.AdminUserResponse banUser(Long targetUserId, String reason);
    AiStudyHub.BE.dto.Response.AdminUserResponse unbanUser(Long targetUserId);
}
