package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.GoogleUserInfo;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.User;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // --- Entity → Response ---

    UserResponse toUserResponse(User user);

    @Mapping(target = "otpExpiredAt", ignore = true)
    RegisterResponse toRegisterResponse(User user);

    // --- Request → Entity ---

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "totalScore", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "storageLimit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "banReason", ignore = true)
    @Mapping(target = "bannedAt", ignore = true)
    @Mapping(target = "bannedBy", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "googleId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "bookmarks", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "downloads", ignore = true)
    @Mapping(target = "chatSessions", ignore = true)
    @Mapping(target = "otpVerifications", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "userBadges", ignore = true)
    @Mapping(target = "userRanks", ignore = true)
    @Mapping(target = "weeklyScores", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "scoreLogs", ignore = true)
    User toUser(RegisterRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "avatarUrl", ignore = true)
    User updateUserFromRequest(UpdateProfileRequest request, @MappingTarget User user);

    @Mapping(target = "googleId", source = "sub")
    @Mapping(target = "fullName", source = "name")
    @Mapping(target = "avatarUrl", source = "picture")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "totalScore", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "storageLimit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "banReason", ignore = true)
    @Mapping(target = "bannedAt", ignore = true)
    @Mapping(target = "bannedBy", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "bookmarks", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "downloads", ignore = true)
    @Mapping(target = "chatSessions", ignore = true)
    @Mapping(target = "otpVerifications", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "userBadges", ignore = true)
    @Mapping(target = "userRanks", ignore = true)
    @Mapping(target = "weeklyScores", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "scoreLogs", ignore = true)
    User toUser(GoogleUserInfo googleUserInfo);
}
