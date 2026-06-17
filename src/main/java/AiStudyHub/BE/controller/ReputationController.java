package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.service.RankingBadgeService;
import AiStudyHub.BE.service.ReputationService;
import AiStudyHub.BE.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reputation")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
@Tag(name = "reputation-controller")
public class ReputationController {

    @Autowired
    private ReputationService reputationService;
    @Autowired
    private RankingBadgeService rankingBadgeService;

    @Operation(summary = "Run the daily reputation job immediately (admin only)")
    @PostMapping("/run")
    public ResponseEntity<APIResponse<Integer>> runNow() {
        int processed = reputationService.runDailyReputation();
        return ResponseEntity.ok(
                APIResponse.response(200, "Reputation job executed", processed)
        );
    }

    @PostMapping("/admin/ranks/update/{userId}")
    public ResponseEntity<APIResponse<Boolean>> updateRank(@PathVariable Long userId) {

        return ResponseEntity.ok(APIResponse.response(200, "Reputation job executed", rankingBadgeService.updateUserRank(userId)));
    }
}
