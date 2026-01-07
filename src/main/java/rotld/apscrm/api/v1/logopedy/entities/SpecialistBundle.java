package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rotld.apscrm.api.v1.user.repository.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "specialist_bundle")
public class SpecialistBundle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "specialist_user_id", nullable = false, unique = true)
    private User specialist;

    @Column(name = "total_keys", nullable = false)
    private int totalKeys;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

