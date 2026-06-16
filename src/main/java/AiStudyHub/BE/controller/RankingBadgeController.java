package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import AiStudyHub.BE.service.impl.IRankingBadgeService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RankingBadgeController {

    IRankingBadgeService rankingBadgeService;

    @GetMapping("/ranks")
    public ResponseEntity<APIResponse<List<RankingResponse>>> getAllRanks() {
        List<RankingResponse> responses = rankingBadgeService.getAllRanks();

        return ResponseEntity.ok(APIResponse.<List<RankingResponse>>builder()
                .code(200)
                .message("Fetched all ranks successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/badges")
    public ResponseEntity<APIResponse<List<BadgeResponse>>> getAllBadges() {
        List<BadgeResponse> responses = rankingBadgeService.getAllBadges();

        return ResponseEntity.ok(APIResponse.<List<BadgeResponse>>builder()
                .code(200)
                .message("Fetched all badges successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/users/{userId}/ranks/history")
    public ResponseEntity<APIResponse<UserRankResponse>> getUserRank(@PathVariable Long userId) {
        UserRankResponse response = rankingBadgeService.getUserRank(userId);

        return ResponseEntity.ok(APIResponse.<UserRankResponse>builder()
                .code(200)
                .message("Fetched user rank successfully")
                .result(response)
                .build());
    }

    @GetMapping("/top-weekly")
    public ResponseEntity<APIResponse<List<WeeklyScoreResponse>>> getTopWeeklyContributors(
            @RequestParam(defaultValue = "10") int limit) {
        List<WeeklyScoreResponse> responses = rankingBadgeService.getTopWeeklyContributors(limit);

        return ResponseEntity.ok(APIResponse.<List<WeeklyScoreResponse>>builder()
                .code(200)
                .message("Fetched top weekly contributors successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<APIResponse<List<UserBadgeResponse>>> getUserBadges(@PathVariable Long userId) {
        List<UserBadgeResponse> responses = rankingBadgeService.getUserBadges(userId);

        return ResponseEntity.ok(APIResponse.<List<UserBadgeResponse>>builder()
                .code(200)
                .message("Fetched user badges successfully")
                .result(responses)
                .build());
    }
}
