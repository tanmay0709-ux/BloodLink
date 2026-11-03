package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.NotificationResponseDto;
import com.bloodlink.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * Retrieve paginated notifications for the current authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<NotificationResponseDto> list = notificationService.getMyNotifications(unreadOnly, limit);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * GET /api/notifications/unread-count
     * Returns count of unread notifications for current user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    /**
     * PATCH /api/notifications/{id}/read
     * Mark a single notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(@PathVariable Long id) {
        NotificationResponseDto updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", updated));
    }

    /**
     * PATCH /api/notifications/mark-all-read
     * Mark all notifications for the current authenticated user as read.
     */
    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        int count = notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", Map.of("markedCount", count)));
    }
}
