package rotld.apscrm.api.v1.iban_beneficiari.repository;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Immutable
@Subselect("""
  SELECT
    s.ID                                                   AS id,
    DATE_FORMAT(s.post_date, '%Y-%m-%dT%H:%i:%s')          AS post_date_iso,
    nume.meta_value                                        AS name,
    iban.meta_value                                        AS iban
  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta nume ON nume.post_id = s.ID AND nume.meta_key = 'nume'
  LEFT JOIN wordpress.wp_postmeta iban ON iban.post_id = s.ID AND iban.meta_key = 'iban'
  WHERE s.post_type   = 'iban_beneficiar'
    AND s.post_status IN ('publish','draft')
""")
public class IbanBeneficiari {
    @Id @Column(name="id") private Integer id;
    @Column(name="post_date_iso") private String postDateIso;
    @Column(name="name") private String name;
    @Column(name="iban")  private String iban;
}