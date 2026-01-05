package rotld.apscrm.api.v1.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.notification.entities.UserFcmToken;
import rotld.apscrm.api.v1.notification.repository.UserFcmTokenRepo;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final UserFcmTokenRepo fcmTokenRepo;
    private final UserRepository userRepository;

    /**
     * Register FCM token for push notifications
     */
    @PostMapping("/register-token")
    @Transactional
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterTokenRequest request
    ) {
        log.info("Registering FCM token for user: {}", user.getId());

        // Check if token already exists
        Optional<UserFcmToken> existingToken = fcmTokenRepo.findByFcmToken(request.fcmToken());

        if (existingToken.isPresent()) {
            // Update existing token's user and last used timestamp
            UserFcmToken token = existingToken.get();
            token.setUser(user);
            token.setDeviceInfo(request.deviceInfo());
            token.setLastUsedAt(LocalDateTime.now());
            fcmTokenRepo.save(token);
            log.info("Updated existing FCM token for user: {}", user.getId());
        } else {
            // Delete old tokens for this user to prevent duplicate notifications
            // This handles the case where FCM token changes (app reinstall, token refresh, etc.)
            // We keep only the new token - if multi-device support is needed, this can be adjusted
            List<UserFcmToken> oldTokens = fcmTokenRepo.findByUserId(user.getId());
            if (!oldTokens.isEmpty()) {
                log.info("Removing {} old FCM token(s) for user: {}", oldTokens.size(), user.getId());
                fcmTokenRepo.deleteAll(oldTokens);
            }
            
            // Create new token
            UserFcmToken newToken = new UserFcmToken(user, request.fcmToken(), request.deviceInfo());
            fcmTokenRepo.save(newToken);
            log.info("Created new FCM token for user: {}", user.getId());
        }

        // Update user's last activity
        userRepository.updateLastActivity(user.getId(), LocalDateTime.now());

        return ResponseEntity.ok().build();
    }

    /**
     * Unregister FCM token (called on logout)
     */
    @DeleteMapping("/unregister-token")
    @Transactional
    public ResponseEntity<Void> unregisterToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UnregisterTokenRequest request
    ) {
        log.info("Unregistering FCM token for user: {}", user.getId());

        fcmTokenRepo.deleteByFcmToken(request.fcmToken());

        return ResponseEntity.ok().build();
    }

    /**
     * Update last activity (can be called when user opens app)
     */
    @PostMapping("/activity")
    @Transactional
    public ResponseEntity<Void> updateActivity(@AuthenticationPrincipal User user) {
        userRepository.updateLastActivity(user.getId(), LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    // Request DTOs
    public record RegisterTokenRequest(
            @NotBlank(message = "FCM token is required")
            String fcmToken,
            String deviceInfo
    ) {}

    public record UnregisterTokenRequest(
            @NotBlank(message = "FCM token is required")
            String fcmToken
    ) {}
}
