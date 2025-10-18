package rotld.apscrm.api.v1.logopedy.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
public class Module {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, unique=true) String slug;
    @Column(nullable=false) String title;
    @Lob String introText;
    @Column(nullable=false) Integer position;
    @Column(nullable=false) boolean isActive = true;
    @Column(nullable=false) boolean isPremium = false;
}