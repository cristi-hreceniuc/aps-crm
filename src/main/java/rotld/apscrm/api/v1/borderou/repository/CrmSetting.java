package rotld.apscrm.api.v1.borderou.repository;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="crm_settings", catalog = "crm")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrmSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable=false, unique=true) private String name;
    @Column(nullable=false)              private String type;   // string/integer/boolean/date
    @Lob @Column                         private String value;
    @Lob @Column(name="default_value")   private String defaultValue;
}
