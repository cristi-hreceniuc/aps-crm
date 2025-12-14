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
}
