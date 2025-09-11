package rotld.apscrm.api.v1.f230.repository;


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
    s.post_title                                           AS title,

    -- metas
    anul.meta_value                 AS year,
    nume.meta_value                 AS first_name,
    prenume.meta_value              AS last_name,
    email.meta_value                AS email,
    telefon.meta_value              AS phone,
    iban.meta_value                 AS iban,
    dist2.meta_value                AS distrib2,          -- '1' dacă e 2 ani
    acord.meta_value                AS acord_email,       -- '1'/'0'
    pdf_url.meta_value              AS pdf_url,

    -- link fallback (admin)
    CONCAT('http://actiunepentrusanatate.ro', '/wp-admin/post.php?post=', s.ID, '&action=edit') AS admin_edit,

    -- flags persistente
    COALESCE(ps.is_downloaded, 0) AS downloaded,
    COALESCE(ps.is_verified,   0) AS verified,
    COALESCE(ps.is_corrupt,    0) AS corrupt

  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta anul     ON anul.post_id    = s.ID AND anul.meta_key     = 'anul'
  LEFT JOIN wordpress.wp_postmeta nume     ON nume.post_id    = s.ID AND nume.meta_key     = 'nume'
  LEFT JOIN wordpress.wp_postmeta prenume  ON prenume.post_id = s.ID AND prenume.meta_key  = 'prenume'
  LEFT JOIN wordpress.wp_postmeta email    ON email.post_id   = s.ID AND email.meta_key    = 'email'
  LEFT JOIN wordpress.wp_postmeta telefon  ON telefon.post_id = s.ID AND telefon.meta_key  = 'telefon'
  LEFT JOIN wordpress.wp_postmeta iban     ON iban.post_id    = s.ID AND iban.meta_key     = 'iban'
  LEFT JOIN wordpress.wp_postmeta dist2    ON dist2.post_id   = s.ID AND dist2.meta_key    = 'distribuire2ani'
  LEFT JOIN wordpress.wp_postmeta acord    ON acord.post_id   = s.ID AND acord.meta_key    = 'acordComunicare'
  LEFT JOIN wordpress.wp_postmeta pdf_url  ON pdf_url.post_id = s.ID AND pdf_url.meta_key  = '_pdf_url'

  LEFT JOIN wordpress.wp_posts_settings ps ON ps.post_id = s.ID

  WHERE s.post_type   = 'formular230'
    AND s.post_status = 'publish'
""")
public class F230 {
    @Id @Column(name="id") private Integer id;

    @Column(name="post_date_iso") private String postDateIso;
    @Column(name="title")         private String title;

    @Column(name="year")          private String year;
    @Column(name="first_name")    private String firstName;
    @Column(name="last_name")     private String lastName;
    @Column(name="email")         private String email;
    @Column(name="phone")         private String phone;
    @Column(name="iban")          private String iban;
    @Column(name="distrib2")      private String distrib2;
    @Column(name="acord_email")   private String acordEmail;
    @Column(name="pdf_url")       private String pdfUrl;

    @Column(name="admin_edit")    private String adminEdit;

    @Column(name="downloaded")    private Boolean downloaded;
    @Column(name="verified")      private Boolean verified;
    @Column(name="corrupt")       private Boolean corrupt;
}