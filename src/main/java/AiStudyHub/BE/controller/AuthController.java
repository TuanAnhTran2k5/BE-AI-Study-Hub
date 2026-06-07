package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.LoginRequest;
import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.VerifyOtpRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.service.AuthenticationService;
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
    AuthenticationService authenticationService;

    @PostMapping("register")
    public ResponseEntity<APIResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse userResponse = authenticationService.register(registerRequest);
        return ResponseEntity.status(201)
                .body(
                        APIResponse.response(
                                201,"Register Successfully",userResponse
                        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<APIResponse<UserResponse>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        UserResponse userResponse = authenticationService.verifyEmail(request);

        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Verify Email Successfully",
                        userResponse
                )
        );
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
}
