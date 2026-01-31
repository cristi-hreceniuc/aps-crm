package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity
public class Submodule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne Module module;
    String slug;
    @Column(nullable=false) String title;
    @Lob String introText;
    @Column(nullable=false) Integer position;
    @Column(nullable=false) boolean isActive = true;
    
    /** Order of parts in this submodule (list of part IDs) */
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    List<Long> partOrder = new ArrayList<>();
}