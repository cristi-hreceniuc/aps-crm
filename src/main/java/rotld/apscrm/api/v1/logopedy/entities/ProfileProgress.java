package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.logopedy.enums.ProgressStatus;
import rotld.apscrm.api.v1.logopedy.enums.ScreenType;

@Getter @Setter @Entity
public class ProfileProgress {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) Profile profile;
    Long moduleId; Long submoduleId; Long lessonId;
    Integer screenIndex;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    ProgressStatus status;
    @Column(nullable=false) java.time.Instant updatedAt = java.time.Instant.now();
}