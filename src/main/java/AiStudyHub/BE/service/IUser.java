package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.AdminUserResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.dto.Response.GlobalLeaderboardResponse;
import AiStudyHub.BE.dto.Response.LeaderboardResponse;
import AiStudyHub.BE.entity.User;
import org.springframework.data.domain.Page;

public interface IUser {
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    UserResponse getProfile(Long userId);
    GlobalLeaderboardResponse getMyLeaderboardRank(Long userId);
    Page<LeaderboardResponse> getGlobalLeaderboard(int page, int size);
    UserResponse buildUserProfileResponse(User user, String accessToken);
    Page<AdminUserResponse> getUsersForAdmin(String search, String status, int page, int size);
    AdminUserResponse getUserDetailForAdmin(Long userId);
    AdminUserResponse banUser(Long targetUserId, String reason);
    AdminUserResponse unbanUser(Long targetUserId);
    AdminUserResponse updateUserRole(Long targetUserId, UserRole newRole);
}

