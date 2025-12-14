package rotld.apscrm.api.v1.notification.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rotld.apscrm.api.v1.user.repository.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_fcm_token")
@Getter
@Setter
@NoArgsConstructor
public class UserFcmToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;
    
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    public UserFcmToken(User user, String fcmToken, String deviceInfo) {
        this.user = user;
        this.fcmToken = fcmToken;
        this.deviceInfo = deviceInfo;
    }
}
