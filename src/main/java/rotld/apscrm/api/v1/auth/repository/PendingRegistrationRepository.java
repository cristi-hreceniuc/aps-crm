package rotld.apscrm.api.v1.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.auth.entity.PendingRegistration;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    Optional<PendingRegistration> findByEmail(String email);
    
    void deleteByEmail(String email);
    
    /**
     * Clean up expired pending registrations (older than given timestamp)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PendingRegistration p WHERE p.otpExpiresAt < :expiredBefore")
    int deleteExpiredRegistrations(Instant expiredBefore);
}
