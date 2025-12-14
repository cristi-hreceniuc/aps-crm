package rotld.apscrm.api.v1.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    @Modifying
    @Query(value = "UPDATE users SET user_status = :status WHERE id = :id", nativeQuery = true)
    int updateStatus(String id, String status);

    @Modifying
    @Query(value = """
            UPDATE User u
            SET u.isPremium = :premium
            WHERE u.id = :id""")
    int updatePremium(@Param("id") String id, @Param("premium") boolean premium);

    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    int hardDelete(String id);

    /**
     * Find users who haven't had activity today (for daily practice reminder)
     * Only returns users who have registered FCM tokens
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN UserFcmToken t ON t.user = u
            WHERE u.lastActivityAt IS NULL 
               OR u.lastActivityAt < :todayStart
            """)
    List<User> findUsersWithNoActivityToday(@Param("todayStart") LocalDateTime todayStart);

    /**
     * Find users who have been inactive for 7+ days
     * Only returns users who have registered FCM tokens
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN UserFcmToken t ON t.user = u
            WHERE u.lastActivityAt IS NULL 
               OR u.lastActivityAt < :cutoffDate
            """)
    List<User> findUsersInactiveFor(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Update last activity timestamp for a user
     */
    @Modifying
    @Query("UPDATE User u SET u.lastActivityAt = :timestamp WHERE u.id = :userId")
    void updateLastActivity(@Param("userId") String userId, @Param("timestamp") LocalDateTime timestamp);
}