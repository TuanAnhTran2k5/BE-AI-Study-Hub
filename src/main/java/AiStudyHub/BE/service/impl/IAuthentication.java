package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;

public interface IAuthentication {
    RegisterResponse register(RegisterRequest registerRequest);

    UserResponse login(LoginRequest loginRequest);

    UserResponse verifyEmail(VerifyOtpRequest request);
}
