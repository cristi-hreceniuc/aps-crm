package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.logopedy.enums.TargetAudience;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity
public class Module {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, unique=true) String slug;
    @Column(nullable=false) String title;
    @Lob String introText;
    @Column(nullable=false) Integer position;
    @Column(nullable=false) boolean isActive = true;
    @Column(nullable=false) boolean isPremium = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false) TargetAudience targetAudience = TargetAudience.CHILDREN;
    
    /** Order of submodules in this module (list of submodule IDs) */
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    List<Long> submoduleOrder = new ArrayList<>();
}