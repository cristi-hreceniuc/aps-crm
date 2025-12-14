package rotld.apscrm.api.v1.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.notification.service.PushNotificationService;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final PushNotificationService pushNotificationService;

    /**
     * Send notification about new module to all users
     */
    @PostMapping("/new-module")
    public ResponseEntity<NotificationResponse> notifyNewModule(
            @Valid @RequestBody NewModuleRequest request
    ) {
        log.info("Sending new module notification: {}", request.title());
        
        pushNotificationService.sendNewContentNotification(
                request.title(),
                "modul",
                request.moduleId()
        );

        return ResponseEntity.ok(new NotificationResponse(
                true,
                "Notification sent to all users about new module: " + request.title()
        ));
    }

    /**
     * Send notification about new submodule to all users
     */
    @PostMapping("/new-submodule")
    public ResponseEntity<NotificationResponse> notifyNewSubmodule(
            @Valid @RequestBody NewSubmoduleRequest request
    ) {
        log.info("Sending new submodule notification: {}", request.title());
        
        pushNotificationService.sendNewContentNotification(
                request.title(),
                "submodul",
                request.submoduleId()
        );

        return ResponseEntity.ok(new NotificationResponse(
                true,
                "Notification sent to all users about new submodule: " + request.title()
        ));
    }

    /**
     * Send custom notification to all users
     */
    @PostMapping("/broadcast")
    public ResponseEntity<NotificationResponse> broadcast(
            @Valid @RequestBody BroadcastRequest request
    ) {
        log.info("Broadcasting notification: {}", request.title());
        
        pushNotificationService.sendToAllUsers(
                request.title(),
                request.body(),
                java.util.Map.of("type", "broadcast")
        );

        return ResponseEntity.ok(new NotificationResponse(
                true,
                "Broadcast notification sent to all users"
        ));
    }

    /**
     * Send notification to a specific user
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<NotificationResponse> notifyUser(
            @PathVariable String userId,
            @Valid @RequestBody UserNotificationRequest request
    ) {
        log.info("Sending notification to user {}: {}", userId, request.title());
        
        pushNotificationService.sendToUser(
                userId,
                request.title(),
                request.body(),
                java.util.Map.of("type", request.type() != null ? request.type() : "custom")
        );

        return ResponseEntity.ok(new NotificationResponse(
                true,
                "Notification sent to user: " + userId
        ));
    }

    // Request/Response DTOs
    public record NewModuleRequest(
            @NotBlank(message = "Module title is required")
            String title,
            Long moduleId
    ) {}

    public record NewSubmoduleRequest(
            @NotBlank(message = "Submodule title is required")
            String title,
            Long submoduleId
    ) {}

    public record BroadcastRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Body is required")
            String body
    ) {}

    public record UserNotificationRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Body is required")
            String body,
            String type
    ) {}

    public record NotificationResponse(
            boolean success,
            String message
    ) {}
}
