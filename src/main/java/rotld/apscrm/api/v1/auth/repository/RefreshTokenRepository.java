package rotld.apscrm.api.v1.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rotld.apscrm.api.v1.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    long deleteByUserId(String userId);
}