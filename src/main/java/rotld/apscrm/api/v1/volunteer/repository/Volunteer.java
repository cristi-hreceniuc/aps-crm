package rotld.apscrm.api.v1.volunteer.repository;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "wp_posts", catalog = "wordpress")
@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Volunteer {
    @Id
    private Integer id;
    private LocalDate postDate;
    private String postTitle;
    private String postType;
    @Column(name = "guid")
    private String link;
    @OneToMany(mappedBy = "volunteer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @ToString.Exclude
    private List<VolunteerMeta> meta = new ArrayList<>();
}
