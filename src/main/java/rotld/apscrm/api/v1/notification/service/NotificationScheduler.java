package rotld.apscrm.api.v1.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.logopedy.entities.Profile;
import rotld.apscrm.api.v1.logopedy.repository.ProfileRepo;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;
    private final ProfileRepo profileRepo;

    /**
     * Send daily practice reminder at 7 PM on weekdays (Monday-Friday)
     * Cron: second minute hour day-of-month month day-of-week
     * MON-FRI = Monday through Friday
     */
    @Scheduled(cron = "0 0 19 * * MON-FRI")
    @Transactional(readOnly = true)
    public void sendDailyPracticeReminder() {
        if (!pushNotificationService.isInitialized()) {
            log.warn("Firebase not initialized. Skipping daily practice reminder.");
            return;
        }

        log.info("Starting daily practice reminder job...");

        try {
            // Get start of today
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();

            // Find users who haven't had activity today
            List<User> inactiveUsers = userRepository.findUsersWithNoActivityToday(todayStart);

            log.info("Found {} users with no activity today", inactiveUsers.size());

            for (User user : inactiveUsers) {
                try {
                    // Get user's profiles to get child names
                    List<Profile> profiles = profileRepo.findAllByUserId(user.getId());
                    
                    if (profiles.isEmpty()) {
                        // No profiles, send generic message
                        pushNotificationService.sendToUser(
                                user.getId(),
                                "E timpul pentru practică! 📚",
                                "Nu ai exersat astăzi. Doar 5 minute pot face diferența!",
                                java.util.Map.of("type", "practice_reminder")
                        );
                    } else {
                        // Use first profile's name
                        String childName = profiles.get(0).getName();
                        pushNotificationService.sendPracticeReminder(user.getId(), childName);
                    }
                } catch (Exception e) {
                    log.error("Failed to send practice reminder to user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("Daily practice reminder job completed");
        } catch (Exception e) {
            log.error("Error in daily practice reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Send inactivity reminder at 10 AM daily for users inactive for 7+ days
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional(readOnly = true)
    public void sendInactivityReminder() {
        if (!pushNotificationService.isInitialized()) {
            log.warn("Firebase not initialized. Skipping inactivity reminder.");
            return;
        }

        log.info("Starting inactivity reminder job...");

        try {
            // Calculate cutoff date (7 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

            // Find users who have been inactive for 7+ days
            List<User> inactiveUsers = userRepository.findUsersInactiveFor(cutoffDate);

            log.info("Found {} users inactive for 7+ days", inactiveUsers.size());

            for (User user : inactiveUsers) {
                try {
                    // Get user's profiles to get child names
                    List<Profile> profiles = profileRepo.findAllByUserId(user.getId());
                    
                    if (profiles.isEmpty()) {
                        // No profiles, send generic message
                        pushNotificationService.sendToUser(
                                user.getId(),
                                "Ne este dor de tine! 💙",
                                "Continuă aventura logopedică. Progresul vine cu practică constantă!",
                                java.util.Map.of("type", "inactivity_reminder")
                        );
                    } else {
                        // Use first profile's name
                        String childName = profiles.get(0).getName();
                        pushNotificationService.sendInactivityReminder(user.getId(), childName);
                    }
                } catch (Exception e) {
                    log.error("Failed to send inactivity reminder to user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("Inactivity reminder job completed");
        } catch (Exception e) {
            log.error("Error in inactivity reminder job: {}", e.getMessage(), e);
        }
    }
}
