package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rotld.apscrm.api.v1.user.repository.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "license_key")
public class LicenseKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_uuid", nullable = false, unique = true, length = 36)
    private String keyUuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialist_user_id", nullable = false)
    private User specialist;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
}

