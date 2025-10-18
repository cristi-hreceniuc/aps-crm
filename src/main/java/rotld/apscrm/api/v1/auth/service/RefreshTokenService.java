package rotld.apscrm.api.v1.auth.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.auth.entity.RefreshToken;
import rotld.apscrm.api.v1.auth.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    public RefreshToken create(String userId, long millis) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString()); // random string, nu JWT
        rt.setExpiresAt(Instant.now().plusMillis(millis));
        return repo.save(rt);
    }

    public RefreshToken validateUsable(String token) {
        var rt = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("invalid_refresh"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("expired_or_revoked");
        }
        return rt;
    }

    public RefreshToken rotate(RefreshToken old, String userId, long millis) {
        old.setRevoked(true);
        var newRt = create(userId, millis);
        old.setReplacedBy(newRt.getToken());
        repo.save(old);
        return newRt;
    }

    public void revokeAllForUser(String userId) { repo.deleteByUserId(userId); }
}