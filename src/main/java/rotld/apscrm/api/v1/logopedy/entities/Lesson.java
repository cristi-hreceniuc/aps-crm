package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rotld.apscrm.api.v1.logopedy.enums.LessonType;

@Getter @Setter @Entity
public class Lesson {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) Submodule submodule;
    String code;
    @Column(nullable=false) String title;
    @Lob String hint;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    LessonType lessonType;
    @Column(nullable=false) Integer position;
    @Column(nullable=false) boolean isActive = true;
}