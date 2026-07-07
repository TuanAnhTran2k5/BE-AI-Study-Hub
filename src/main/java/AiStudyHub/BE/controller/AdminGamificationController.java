package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.BadgeRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.BadgeResponse;
import AiStudyHub.BE.service.IGamification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gamification/badges")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "admin-gamification-controller")
public class AdminGamificationController {

    IGamification gamificationService;

    @Operation(summary = "Get all badges or search by name")
    @GetMapping
    public ResponseEntity<APIResponse<List<BadgeResponse>>> getAllBadges(
            @RequestParam(required = false) String keyword
    ) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(
                    APIResponse.response(200, "Search badges successfully", gamificationService.searchBadgesByName(keyword))
            );
        }
        return ResponseEntity.ok(
                APIResponse.response(200, "Get all badges successfully", gamificationService.getAllBadges())
        );
    }

    @Operation(summary = "Get badge by ID")
    @GetMapping("/{badgeId}")
    public ResponseEntity<APIResponse<BadgeResponse>> getBadgeById(@PathVariable Long badgeId) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get badge successfully", gamificationService.getBadgeById(badgeId))
        );
    }

    @Operation(summary = "Create a new badge")
    @RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = BadgeRequest.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<BadgeResponse>> createBadge(
            @Valid @ModelAttribute BadgeRequest request
    ) throws Exception {
        BadgeResponse response = gamificationService.createBadge(request);
        return ResponseEntity.status(201).body(
                APIResponse.response(201, "Create badge successfully", response)
        );
    }

    @Operation(summary = "Update an existing badge")
    @RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = BadgeRequest.class)))
    @PutMapping(value = "/{badgeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<BadgeResponse>> updateBadge(
            @PathVariable Long badgeId,
            @Valid @ModelAttribute BadgeRequest request
    ) throws Exception {
        BadgeResponse response = gamificationService.updateBadge(badgeId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Update badge successfully", response)
        );
    }

    @Operation(summary = "Delete a badge")
    @DeleteMapping("/{badgeId}")
    public ResponseEntity<APIResponse<BadgeResponse>> deleteBadge(@PathVariable Long badgeId) {
        BadgeResponse response = gamificationService.deleteBadge(badgeId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Delete badge successfully", response)
        );
    }
}
