package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.logopedy.enums.ScreenType;


@Getter @Setter @Entity
public class LessonScreen {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) Lesson lesson;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    ScreenType screenType;
    @Type(JsonType.class)
    @Column(columnDefinition="json", nullable=false)
    String payload; // JSON raw
    @Column(nullable=false) Integer position;
}