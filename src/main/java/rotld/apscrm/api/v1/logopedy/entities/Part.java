package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity
public class Part {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    Long id;
    
    @ManyToOne(optional = false) 
    Submodule submodule;
    
    @Column(nullable = false) 
    String name;
    
    @Column(nullable = false) 
    String slug;
    
    @Lob 
    String description;
    
    @Column(nullable = false) 
    Integer position;
    
    @Column(nullable = false) 
    boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();
    
    /** Order of lessons in this part (list of lesson IDs) */
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    List<Long> lessonOrder = new ArrayList<>();
}
