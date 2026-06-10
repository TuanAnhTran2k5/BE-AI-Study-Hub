package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.UpdateProfileResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.UserUpdateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
public class UserController {


    @Autowired
    UserUpdateService userUpdateService;


    @PutMapping("profile")
    public ResponseEntity<APIResponse<UpdateProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        UpdateProfileResponse response = userUpdateService.updateProfile(currentUser.getUserId(), updateProfileRequest);

        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Update profile successfully", response
                        ));
    }
}
