package rotld.apscrm.api.v1.volunteer.repository;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "wp_postmeta", catalog = "wordpress")
@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class VolunteerMeta {
    @Id
    @Column(name = "meta_id")
    private Integer id;
    private String metaKey;
    private String metaValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    private Volunteer volunteer;
}
