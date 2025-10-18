package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class ProfileLessonKey implements java.io.Serializable {
    Long profileId; Long lessonId;
}
