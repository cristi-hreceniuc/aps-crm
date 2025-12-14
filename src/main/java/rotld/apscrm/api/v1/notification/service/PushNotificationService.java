package rotld.apscrm.api.v1.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.notification.repository.UserFcmTokenRepo;
import rotld.apscrm.api.v1.user.dto.UserRole;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final UserFcmTokenRepo fcmTokenRepo;
    private final ResourceLoader resourceLoader;

    @Value("${firebase.config-path}")
    private String firebaseConfigPath;

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Resource resource = resourceLoader.getResource(firebaseConfigPath);
                
                if (!resource.exists()) {
                    log.warn("Firebase config file not found at: {}. Push notifications will be disabled.", firebaseConfigPath);
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                        .build();

                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                log.info("Firebase initialized successfully for push notifications");
            } else {
                firebaseInitialized = true;
                log.info("Firebase already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    /**
     * Send notification to a specific FCM token
     */
    public boolean sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!firebaseInitialized) {
            log.warn("Firebase not initialized. Cannot send notification.");
            return false;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setChannelId("logopedy_notifications")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Successfully sent notification to token: {}", response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token {}: {}", token, e.getMessage());
            
            // Remove invalid tokens
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Removing invalid FCM token: {}", token);
                fcmTokenRepo.deleteByFcmToken(token);
            }
            return false;
        }
    }

    /**
     * Send notification to a user (all their registered devices)
     */
    public void sendToUser(String userId, String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenRepo.findTokensByUserId(userId);
        
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for user: {}", userId);
            return;
        }

        for (String token : tokens) {
            sendToToken(token, title, body, data);
        }
    }

    /**
     * Send notification to multiple tokens (batch)
     */
    public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (!firebaseInitialized) {
            log.warn("Firebase not initialized. Cannot send notifications.");
            return;
        }

        if (tokens.isEmpty()) {
            return;
        }

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setChannelId("logopedy_notifications")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());
            log.info("Sent multicast notification. Success: {}, Failure: {}", 
                    response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        FirebaseMessagingException exception = responses.get(i).getException();
                        if (exception != null && 
                            (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                             exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                            String invalidToken = tokens.get(i);
                            log.info("Removing invalid FCM token: {}", invalidToken);
                            fcmTokenRepo.deleteByFcmToken(invalidToken);
                        }
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification to all registered users
     */
    public void sendToAllUsers(String title, String body, Map<String, String> data) {
        List<String> allTokens = fcmTokenRepo.findAllTokens();
        
        if (allTokens.isEmpty()) {
            log.info("No FCM tokens registered. No notifications sent.");
            return;
        }

        // FCM allows max 500 tokens per multicast
        int batchSize = 500;
        for (int i = 0; i < allTokens.size(); i += batchSize) {
            List<String> batch = allTokens.subList(i, Math.min(i + batchSize, allTokens.size()));
            sendToTokens(batch, title, body, data);
        }
    }

    // Convenience methods for specific notification types

    /**
     * Send practice reminder notification
     */
    public void sendPracticeReminder(String userId, String childName) {
        String title = "E timpul pentru practică! 📚";
        String body = childName + " nu a exersat astăzi. Doar 5 minute pot face diferența!";
        Map<String, String> data = Map.of("type", "practice_reminder");
        sendToUser(userId, title, body, data);
    }

    /**
     * Send inactivity reminder notification
     */
    public void sendInactivityReminder(String userId, String childName) {
        String title = "Ne este dor de " + childName + "! 💙";
        String body = "Continuă aventura logopedică. Progresul vine cu practică constantă!";
        Map<String, String> data = Map.of("type", "inactivity_reminder");
        sendToUser(userId, title, body, data);
    }

    /**
     * Send premium access granted notification
     */
    public void sendPremiumGranted(String userId) {
        String title = "Bun venit în Premium! 🌟";
        String body = "Tot conținutul este acum deblocat. Explorează toate lecțiile!";
        Map<String, String> data = Map.of("type", "premium_granted");
        sendToUser(userId, title, body, data);
    }

    /**
     * Send new content notification to all users
     */
    public void sendNewContentNotification(String contentTitle, String contentType, Long contentId) {
        String title = "Conținut nou disponibil! 🎉";
        String body = "Nou " + contentType + ": " + contentTitle;
        Map<String, String> data = Map.of(
                "type", "new_content",
                "contentType", contentType,
                "contentId", String.valueOf(contentId)
        );
        sendToAllUsers(title, body, data);
    }

    public boolean isInitialized() {
        return firebaseInitialized;
    }

    // ============ Targeted notification methods ============

    /**
     * Send notification to users by role
     */
    public int sendToUsersByRole(UserRole role, String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenRepo.findTokensByUserRole(role);
        
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for role: {}", role);
            return 0;
        }

        log.info("Sending notification to {} users with role: {}", tokens.size(), role);
        sendToTokensBatched(tokens, title, body, data);
        return tokens.size();
    }

    /**
     * Send notification to premium users only
     */
    public int sendToPremiumUsers(String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenRepo.findTokensForPremiumUsers();
        
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for premium users");
            return 0;
        }

        log.info("Sending notification to {} premium users", tokens.size());
        sendToTokensBatched(tokens, title, body, data);
        return tokens.size();
    }

    /**
     * Send notification to non-premium users only
     */
    public int sendToNonPremiumUsers(String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenRepo.findTokensForNonPremiumUsers();
        
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for non-premium users");
            return 0;
        }

        log.info("Sending notification to {} non-premium users", tokens.size());
        sendToTokensBatched(tokens, title, body, data);
        return tokens.size();
    }

    /**
     * Send notification to users by role AND premium status
     */
    public int sendToUsersByRoleAndPremium(UserRole role, boolean isPremium, String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenRepo.findTokensByRoleAndPremium(role, isPremium);
        
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for role: {} and premium: {}", role, isPremium);
            return 0;
        }

        log.info("Sending notification to {} users with role: {} and premium: {}", tokens.size(), role, isPremium);
        sendToTokensBatched(tokens, title, body, data);
        return tokens.size();
    }

    /**
     * Send to all users and return count
     */
    public int sendToAllUsersAndCount(String title, String body, Map<String, String> data) {
        List<String> allTokens = fcmTokenRepo.findAllTokens();
        
        if (allTokens.isEmpty()) {
            log.info("No FCM tokens registered. No notifications sent.");
            return 0;
        }

        sendToTokensBatched(allTokens, title, body, data);
        return allTokens.size();
    }

    /**
     * Helper to send to tokens in batches of 500
     */
    private void sendToTokensBatched(List<String> tokens, String title, String body, Map<String, String> data) {
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            sendToTokens(batch, title, body, data);
        }
    }
}
