package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.dto.Response.GlobalLeaderboardResponse;
import AiStudyHub.BE.dto.Response.LeaderboardResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
public class UserController {


    @Autowired
    IUser userService;

    @GetMapping("profile")
    public ResponseEntity<APIResponse<UserResponse>> getProfile() {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = userService.getProfile(currentUser.getUserId());
        return ResponseEntity.status(200)
                .body(APIResponse.response(200, "Get profile successfully", response));
    }

    @PutMapping(value = "profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<UserResponse>> updateProfile(@Valid @ModelAttribute UpdateProfileRequest updateProfileRequest) {

        User currentUser = SecurityUtils.getCurrentUser();

        UserResponse response = userService.updateProfile(currentUser.getUserId(), updateProfileRequest);

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Update profile successfully", response
                        ));
    }

    @GetMapping("leaderboard/me")
    public ResponseEntity<APIResponse<GlobalLeaderboardResponse>> getMyLeaderboardRank() {
        User currentUser = SecurityUtils.getCurrentUser();
        GlobalLeaderboardResponse response = userService.getMyLeaderboardRank(currentUser.getUserId());
        return ResponseEntity.status(200)
                .body(APIResponse.response(200, "Get my leaderboard rank successfully", response));
    }

    @GetMapping("leaderboard")
    public ResponseEntity<APIResponse<org.springframework.data.domain.Page<LeaderboardResponse>>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Page<LeaderboardResponse> response = userService.getGlobalLeaderboard(page, size);
        return ResponseEntity.status(200)
                .body(APIResponse.response(200, "Get leaderboard successfully", response));
    }
}
