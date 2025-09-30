package rotld.apscrm.api.v1.user.repository;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rotld.apscrm.api.v1.user.dto.UserRole;
import rotld.apscrm.api.v1.user.dto.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Table(name = "users")
@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @Column(nullable = false)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String gender;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Column(length = 128)
    private String resetToken;              // simplu; pentru securitate maximă poți salva HASH-ul token-ului

    private Instant resetTokenExpiresAt;    // expiră după X minute

    private String otpHash;
    private Instant otpExpiresAt;
    private int otpAttempts;
    private Instant otpLockedUntil;
    @Column(name="is_premium")
    private Boolean isPremium;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userRole.toString()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}