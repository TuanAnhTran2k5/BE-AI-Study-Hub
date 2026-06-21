package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.ForgotPasswordRequest;
import AiStudyHub.BE.dto.Request.ResetPasswordRequest;
import AiStudyHub.BE.dto.Request.ResendOtpRequest;
import AiStudyHub.BE.dto.Response.ForgotPasswordResponse;
import AiStudyHub.BE.dto.Response.ResendOtpResponse;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;

public interface IAuthentication {
    RegisterResponse register(RegisterRequest registerRequest);

    UserResponse login(LoginRequest loginRequest);

    UserResponse verifyEmail(VerifyOtpRequest request);

    UserResponse googleLogin(AiStudyHub.BE.dto.Request.GoogleLoginRequest request);

    UserResponse verifyToken(AiStudyHub.BE.dto.Request.VerifyTokenRequest request);

    boolean logout(AiStudyHub.BE.dto.Request.LogoutRequest request);

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    boolean resetPassword(ResetPasswordRequest request);

    ResendOtpResponse resendOtp(ResendOtpRequest request);
}
