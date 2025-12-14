package rotld.apscrm.api.v1.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import rotld.apscrm.api.v1.user.dto.UserRole;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "pending_registrations")
@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class PendingRegistration {
    @Id
    @Column(nullable = false)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String passwordHash; // Store already hashed password

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private Instant otpExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int otpAttempts = 0;

    private Instant otpLockedUntil;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}
