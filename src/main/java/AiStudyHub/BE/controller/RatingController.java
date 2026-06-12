package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.RatingResponse;
import AiStudyHub.BE.service.impl.IRating;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/document")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
@Tag(name = "rating-controller")
public class RatingController {

    @Autowired
    private IRating ratingService;

    @Operation(summary = "Submit or update a rating for a public document")
    @PostMapping(value = "/{documentId}/rating", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<RatingResponse>> submitRating(
            @PathVariable Long documentId,
            @RequestBody RatingRequest request
    ) {
        RatingResponse response = ratingService.submitRating(documentId, request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Submit rating successfully", response)
        );
    }
}
