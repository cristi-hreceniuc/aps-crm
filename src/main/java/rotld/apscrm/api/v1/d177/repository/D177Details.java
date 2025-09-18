package rotld.apscrm.api.v1.d177.repository;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

@Getter
@NoArgsConstructor
@Entity
@Immutable
@Subselect("""
  SELECT
    s.ID                                                   AS id,
    DATE_FORMAT(s.post_date, '%Y-%m-%dT%H:%i:%s')          AS post_date_iso,
    s.post_title                                           AS title,

    /* atașamente */
    CONCAT('http://actiunepentrusanatate.ro', '/wp-content/uploads/', docfile.meta_value) AS doc_url,
    CONCAT('http://actiunepentrusanatate.ro', '/wp-content/uploads/', sigfile.meta_value) AS sig_url,

    /* meta brute (PHP serialize) */
    firma.meta_value      AS firma_data,
    coresp.meta_value     AS coresp_data,
    reprez.meta_value     AS reprezentant_data,
    contract.meta_value   AS contract_data,

    /* html generat (dacă vrei să-l arăți în modal) */
    doc_html.meta_value   AS doc_html,

    /* linkuri utile */
    CONCAT('http://actiunepentrusanatate.ro','/wp-admin/post.php?post=', s.ID, '&action=edit') AS admin_edit,

    /* flags persistente */
    COALESCE(ps.is_downloaded, 0) AS downloaded,
    COALESCE(ps.is_verified,   0) AS verified,
    COALESCE(ps.is_corrupt,    0) AS corrupt

  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta d        ON d.post_id  = s.ID AND d.meta_key  = '_aps177_doc_id'
  LEFT JOIN wordpress.wp_postmeta sg       ON sg.post_id = s.ID AND sg.meta_key = '_aps177_signature_id'

  LEFT JOIN wordpress.wp_postmeta docfile  ON docfile.post_id  = d.meta_value  AND docfile.meta_key  = '_wp_attached_file'
  LEFT JOIN wordpress.wp_postmeta sigfile  ON sigfile.post_id  = sg.meta_value AND sigfile.meta_key  = '_wp_attached_file'

  LEFT JOIN wordpress.wp_postmeta firma     ON firma.post_id    = s.ID AND firma.meta_key    = '_aps177_firma'
  LEFT JOIN wordpress.wp_postmeta coresp    ON coresp.post_id   = s.ID AND coresp.meta_key   = '_aps177_coresp'
  LEFT JOIN wordpress.wp_postmeta reprez    ON reprez.post_id   = s.ID AND reprez.meta_key   = '_aps177_reprez'
  LEFT JOIN wordpress.wp_postmeta contract  ON contract.post_id = s.ID AND contract.meta_key = '_aps177_contract'
  LEFT JOIN wordpress.wp_postmeta doc_html  ON doc_html.post_id = s.ID AND doc_html.meta_key = '_aps177_doc_html'

  LEFT JOIN wordpress.wp_posts_settings ps  ON ps.post_id = s.ID

  WHERE s.post_type  = 'aps_s177'
    AND s.post_status = 'publish'
""")
@Synchronize({
        "wordpress.wp_posts","wordpress.wp_postmeta","wordpress.wp_posts_settings"
})
public class D177Details {
    @Id @Column(name="id")                  private Integer id;
    @Column(name="post_date_iso")           private String  postDateIso;
    @Column(name="title")                   private String  title;

    @Column(name="doc_url")                 private String  docUrl;
    @Column(name="sig_url")                 private String  sigUrl;

    @Column(name="firma_data",       columnDefinition="text") private String firmaData;
    @Column(name="coresp_data",      columnDefinition="text") private String corespData;
    @Column(name="reprezentant_data",columnDefinition="text") private String reprezentantData;
    @Column(name="contract_data",    columnDefinition="text") private String contractData;

    @Column(name="doc_html",         columnDefinition="longtext") private String docHtml;

    @Column(name="admin_edit")              private String  adminEdit;
    @Column(name="downloaded")              private Boolean downloaded;
    @Column(name="verified")                private Boolean verified;
    @Column(name="corrupt")                 private Boolean corrupt;
}
