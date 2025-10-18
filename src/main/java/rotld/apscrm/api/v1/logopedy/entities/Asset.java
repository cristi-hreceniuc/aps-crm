package rotld.apscrm.api.v1.logopedy.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.logopedy.enums.AssetKind;

@Getter @Setter @Entity
public class Asset {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    AssetKind kind;
    @Column(nullable=false) String uri;
    String mime; String checksum;
    @Type(JsonType.class) @Column(columnDefinition="json") String meta;
}