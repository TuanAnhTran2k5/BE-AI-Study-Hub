package AiStudyHub.BE.controller;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.BookmarkRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.BookmarkResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.IBookmark;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/bookmarks")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@Tag(name = "bookmark-controller")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookmarkController {

    IBookmark bookmarkService;

    @PostMapping
    @Operation(summary = "Add a bookmark for a document")
    public ResponseEntity<APIResponse<BookmarkResponse>> createBookmark(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BookmarkRequest request) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        BookmarkResponse response = bookmarkService.addBookmark(currentUser.getUserId(), request);
        return ResponseEntity.ok(
                APIResponse.response(200, "Bookmark added successfully", response)
        );
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Remove a bookmark for a document")
    public ResponseEntity<APIResponse<BookmarkResponse>> deleteBookmark(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long documentId) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        BookmarkResponse response = bookmarkService.removeBookmark(currentUser.getUserId(), documentId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Bookmark removed successfully", response)
        );
    }

    @GetMapping
    @Operation(summary = "Get all bookmarks of the current user")
    public ResponseEntity<APIResponse<List<BookmarkResponse>>> getUserBookmarks(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }

        List<BookmarkResponse> response = bookmarkService.getBookmarksByUser(currentUser.getUserId());
        return ResponseEntity.ok(
                APIResponse.response(200, "Get user bookmarks successfully", response)
        );
    }
}
