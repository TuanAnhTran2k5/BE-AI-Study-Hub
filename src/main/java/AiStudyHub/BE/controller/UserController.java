package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.UpdateProfileResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
public class UserController {


    @Autowired
    IUser userService;


    @PutMapping("profile")
    public ResponseEntity<APIResponse<UpdateProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

        User currentUser = SecurityUtils.getCurrentUser();

        UpdateProfileResponse response = userService.updateProfile(currentUser.getUserId(), updateProfileRequest);

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Update profile successfully", response
                        ));
    }
}
