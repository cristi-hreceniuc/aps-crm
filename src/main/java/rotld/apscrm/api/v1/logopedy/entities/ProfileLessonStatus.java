package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;

@Getter
@Setter
@Entity
public class ProfileLessonStatus {
    @EmbeddedId
    ProfileLessonKey id;
    @Enumerated(EnumType.STRING)
    LessonStatus status;
    Integer score;
    java.time.Instant startedAt;
    java.time.Instant finishedAt;
}