package rotld.apscrm.api.v1.borderou.repository;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity @Table(name="borderouri", catalog="crm")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Borderou {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="borderou_date", nullable=false)
    private LocalDate dataBorderou;

    @Lob @Column(name = "value", nullable=false)
    private String xml;
}