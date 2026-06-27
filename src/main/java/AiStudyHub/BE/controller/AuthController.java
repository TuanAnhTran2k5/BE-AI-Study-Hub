package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ForgotPasswordRequest;
import AiStudyHub.BE.dto.Request.ResetPasswordRequest;
import AiStudyHub.BE.dto.Request.ResendOtpRequest;
import AiStudyHub.BE.dto.Response.ForgotPasswordResponse;
import AiStudyHub.BE.dto.Response.ResendOtpResponse;
import AiStudyHub.BE.dto.Request.GoogleLoginRequest;
import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.LogoutRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Request.VerifyTokenRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.RegisterResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.service.IAuthentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
public class AuthController {
    @Autowired
    IAuthentication authenticationService;

    @PostMapping("register")
    public ResponseEntity<APIResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authenticationService.register(registerRequest);
        return ResponseEntity.status(201)
                .body(
                        APIResponse.response(
                                201,"Register Successfully, Please Verify Email", response
                        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<APIResponse<UserResponse>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        UserResponse userResponse = authenticationService.verifyEmail(request);

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Verify Email Successfully", userResponse
                        ));
    }

    @PostMapping("login")
    public ResponseEntity<APIResponse<UserResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        UserResponse userResponse = authenticationService.login(loginRequest);
        return ResponseEntity.status(201)
                .body(
                        APIResponse.response(
                                201,"Login Successfully",userResponse
                        ));
    }

    @PostMapping("google-login")
    public ResponseEntity<APIResponse<UserResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        UserResponse userResponse = authenticationService.googleLogin(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Login Google Successfully", userResponse
                        ));
    }

    @PostMapping("verify-token")
    public ResponseEntity<APIResponse<UserResponse>> verifyToken(@Valid @RequestBody VerifyTokenRequest request) {
        UserResponse userResponse = authenticationService.verifyToken(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Token is valid", userResponse
                        ));
    }

    @PostMapping("logout")
    public ResponseEntity<APIResponse<Boolean>> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        boolean result = authenticationService.logout(logoutRequest);
        return ResponseEntity.ok(
                APIResponse.response(200, "Logout Successfully", result)
        );
    }

    @PostMapping("forgot-password")
    public ResponseEntity<APIResponse<ForgotPasswordResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authenticationService.forgotPassword(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Send OTP to email successfully", response
                        ));
    }

    @PostMapping("forgot-password/verify-otp")
    public ResponseEntity<APIResponse<Boolean>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean result = authenticationService.verifyForgotPasswordOtp(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Verify OTP successfully", result
                        ));
    }

    @PostMapping("forgot-password/reset")
    public ResponseEntity<APIResponse<Boolean>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean result = authenticationService.resetPassword(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Change Password Successfully", result
                        ));
    }

    @PostMapping("resend-otp")
    public ResponseEntity<APIResponse<ResendOtpResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        ResendOtpResponse response = authenticationService.resendOtp(request);
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Resend OTP successfully", response
                        ));
    }
}
