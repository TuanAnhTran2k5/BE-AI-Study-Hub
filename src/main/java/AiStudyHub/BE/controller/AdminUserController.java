package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.BanUserRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.AdminUserResponse;
import AiStudyHub.BE.service.IUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUser userService;

    @GetMapping
    @Operation(summary = "Get list of users for admin management")
    public ResponseEntity<APIResponse<Page<AdminUserResponse>>> getUsersForAdmin(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(APIResponse.response(200, "Get users list successfully", userService.getUsersForAdmin(search, status, page, size)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get single user detail for admin")
    public ResponseEntity<APIResponse<AdminUserResponse>> getUserDetailForAdmin(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(APIResponse.response(200, "Get user details successfully", userService.getUserDetailForAdmin(userId)));
    }

    @PostMapping("/{userId}/ban")
    @Operation(summary = "Ban a user")
    public ResponseEntity<APIResponse<AdminUserResponse>> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody BanUserRequest request
    ) {
        return ResponseEntity.ok(APIResponse.response(200, "Ban user successfully", userService.banUser(userId, request.getReason())));
    }

    @PostMapping("/{userId}/unban")
    @Operation(summary = "Unban a user")
    public ResponseEntity<APIResponse<AdminUserResponse>> unbanUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(APIResponse.response(200, "Unban user successfully", userService.unbanUser(userId)));
    }

    @PostMapping("/{userId}/role")
    @Operation(summary = "Update a user's role (promote US to AD, demote AD to US)")
    public ResponseEntity<APIResponse<AdminUserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AiStudyHub.BE.dto.Request.UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(APIResponse.response(200, "Update user role successfully", userService.updateUserRole(userId, request.getRole())));
    }
}

