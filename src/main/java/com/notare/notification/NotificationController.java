package com.notare.notification;

import com.notare.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('TUTOR', 'STUDENT')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<ApiResponse<NotificationsResponse>> notifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.notifications(authentication.getName())));
    }

    /** Marks everything up to now as read. */
    @PostMapping("/api/notifications/seen")
    public ResponseEntity<ApiResponse<Void>> markSeen(Authentication authentication) {
        notificationService.markSeen(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PutMapping("/api/notifications/settings")
    public ResponseEntity<ApiResponse<NotificationsResponse>> updateSettings(
            @RequestBody NotificationSettingsRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.updateMuted(authentication.getName(), request.mutedKinds())));
    }

    public record NotificationSettingsRequest(List<NotificationKind> mutedKinds) {
    }
}
