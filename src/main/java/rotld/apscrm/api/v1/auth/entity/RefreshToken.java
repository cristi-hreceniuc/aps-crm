package rotld.apscrm.api.v1.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String userId;
    @Column(nullable = false, unique = true) private String token;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked = false;
    private String replacedBy; // pentru rotație
    @Column(nullable = false) private Instant createdAt = Instant.now();
}