package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.NotificationResponse;
import AiStudyHub.BE.service.INotification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "notification-controller")
public class NotificationController {

    INotification notificationService;

    @Operation(summary = "Get my notifications (paginated, optional filter by isRead & type)")
    @GetMapping
    public ResponseEntity<APIResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type
    ) {
        Page<NotificationResponse> result = notificationService.getMyNotifications(page, isRead, type);
        return ResponseEntity.ok(
                APIResponse.response(200, "Get notifications successfully", result)
        );
    }


    @Operation(summary = "Get count of unread notifications")
    @GetMapping("/unread-count")
    public ResponseEntity<APIResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.countUnread();
        return ResponseEntity.ok(
                APIResponse.response(200, "Get unread count successfully", Map.of("unreadCount", count))
        );
    }

    @Operation(summary = "Mark a notification as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<APIResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId
    ) {
        NotificationResponse response = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Notification marked as read", response)
        );
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<APIResponse<Map<String, Integer>>> markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(
                APIResponse.response(200, "All notifications marked as read", Map.of("updatedCount", updated))
        );
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteNotification(
            @PathVariable Long notificationId
    ) {
        DeleteResponse response = notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Notification deleted successfully", response)
        );
    }

    @Operation(summary = "Delete all read notifications")
    @DeleteMapping("/read")
    public ResponseEntity<APIResponse<Map<String, Integer>>> deleteAllRead() {
        int deleted = notificationService.deleteAllRead();
        return ResponseEntity.ok(
                APIResponse.response(200, "All read notifications deleted", Map.of("deletedCount", deleted))
        );
    }
}
