package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.UpdateProfileResponse;

public interface IUser {
    UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
