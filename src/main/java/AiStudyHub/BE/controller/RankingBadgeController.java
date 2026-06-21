package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.BadgeResponse;
import AiStudyHub.BE.dto.Response.RankingResponse;
import AiStudyHub.BE.dto.Response.UserBadgeResponse;
import AiStudyHub.BE.dto.Response.UserRankResponse;
import AiStudyHub.BE.dto.Response.WeeklyScoreResponse;
import AiStudyHub.BE.service.impl.IRankingBadgeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RankingBadgeController {

    IRankingBadgeService rankingBadgeService;

    @GetMapping("/ranks")
    public ResponseEntity<APIResponse<List<RankingResponse>>> getAllRanks() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Fetched all ranks successfully", rankingBadgeService.getAllRanks()));
    }

    @GetMapping("/badges")
    public ResponseEntity<APIResponse<List<BadgeResponse>>> getAllBadges() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Fetched all badges successfully", rankingBadgeService.getAllBadges()));
    }

    @GetMapping("/users/{userId}/ranks/history")
    public ResponseEntity<APIResponse<UserRankResponse>> getUserRank(@PathVariable Long userId) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Fetched user rank successfully", rankingBadgeService.getUserRank(userId)));
    }

    @GetMapping("/top-weekly")
    public ResponseEntity<APIResponse<List<WeeklyScoreResponse>>> getTopWeeklyContributors(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Fetched top weekly contributors successfully",
                        rankingBadgeService.getTopWeeklyContributors(limit)));
    }

    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<APIResponse<List<UserBadgeResponse>>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Fetched user badges successfully", rankingBadgeService.getUserBadges(userId)));
    }
}
