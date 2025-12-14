package rotld.apscrm.api.v1.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rotld.apscrm.api.v1.notification.entities.UserFcmToken;
import rotld.apscrm.api.v1.user.repository.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepo extends JpaRepository<UserFcmToken, Long> {
    
    List<UserFcmToken> findByUser(User user);
    
    List<UserFcmToken> findByUserId(String userId);
    
    Optional<UserFcmToken> findByFcmToken(String fcmToken);
    
    @Modifying
    @Query("DELETE FROM UserFcmToken t WHERE t.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);
    
    @Modifying
    @Query("DELETE FROM UserFcmToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") String userId);
    
    @Query("SELECT DISTINCT t.fcmToken FROM UserFcmToken t")
    List<String> findAllTokens();
    
    @Query("SELECT t.fcmToken FROM UserFcmToken t WHERE t.user.id = :userId")
    List<String> findTokensByUserId(@Param("userId") String userId);
    
    /**
     * Find FCM tokens for users with specific role
     */
    @Query("SELECT DISTINCT t.fcmToken FROM UserFcmToken t WHERE t.user.userRole = :role")
    List<String> findTokensByUserRole(@Param("role") rotld.apscrm.api.v1.user.dto.UserRole role);
    
    /**
     * Find FCM tokens for premium users
     */
    @Query("SELECT DISTINCT t.fcmToken FROM UserFcmToken t WHERE t.user.isPremium = true")
    List<String> findTokensForPremiumUsers();
    
    /**
     * Find FCM tokens for non-premium users
     */
    @Query("SELECT DISTINCT t.fcmToken FROM UserFcmToken t WHERE t.user.isPremium = false OR t.user.isPremium IS NULL")
    List<String> findTokensForNonPremiumUsers();
    
    /**
     * Find FCM tokens for users with specific role AND premium status
     */
    @Query("SELECT DISTINCT t.fcmToken FROM UserFcmToken t WHERE t.user.userRole = :role AND t.user.isPremium = :isPremium")
    List<String> findTokensByRoleAndPremium(
            @Param("role") rotld.apscrm.api.v1.user.dto.UserRole role, 
            @Param("isPremium") boolean isPremium
    );
}
