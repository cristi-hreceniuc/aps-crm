package rotld.apscrm.api.v1.logopedy.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import rotld.apscrm.api.v1.user.repository.User;
import com.vladmihalcea.hibernate.type.json.JsonType;

@Getter
@Setter
@Entity
public class Profile {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false)
    User user;
    @Column(nullable=false) String name;
    String avatarUri;
    @Type(JsonType.class)
    @Column(columnDefinition="json")
    String settings;
}