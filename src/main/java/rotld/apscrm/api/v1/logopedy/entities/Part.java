package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
