package com.myshop.controller.v1;

import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.NotificationResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.service.NotificationService;
import com.myshop.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(email, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        NotificationResponse response = notificationService.markAsRead(email, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
