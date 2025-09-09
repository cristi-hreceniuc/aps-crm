package rotld.apscrm.api.v1.d177.repository;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wp_posts_settings", catalog = "wordpress")
public class D177Settings {
    @Id
    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "is_downloaded") private Boolean downloaded;
    @Column(name = "is_verified")   private Boolean verified;
    @Column(name = "is_corrupt")    private Boolean corrupt;

    @Column(name = "updated_at")    private Instant updatedAt;
}