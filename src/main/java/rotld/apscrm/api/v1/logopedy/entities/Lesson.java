package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.logopedy.enums.LessonType;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity
public class Lesson {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false) Submodule submodule;
    @ManyToOne(optional=false) Part part;
    String code;
    @Column(nullable=false) String title;
    @Lob String hint;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    LessonType lessonType;
    @Column(nullable=false) Integer position;
    @Column(nullable=false) boolean isActive = true;
    
    /** Order of screens in this lesson (list of screen IDs) */
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    List<Long> screenOrder = new ArrayList<>();
}