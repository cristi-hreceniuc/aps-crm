package rotld.apscrm.api.v1.sponsorizare.repository;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Immutable
@Subselect("""
  SELECT
    s.ID                                                   AS id,
    DATE_FORMAT(s.post_date, '%Y-%m-%dT%H:%i:%s')          AS post_date_iso,
    s.post_title                                           AS title,

    /* fișiere */
    CONCAT('http://actiunepentrusanatate.ro', '/wp-content/uploads/', docfile.meta_value)  AS doc_url,
    CONCAT('http://actiunepentrusanatate.ro', '/wp-content/uploads/', jsonfile.meta_value) AS json_url,
    CONCAT('http://actiunepentrusanatate.ro', '/wp-content/uploads/', sigfile.meta_value)  AS signature_url,

    /* meta brute */
    firma.meta_value      AS firma_data,
    coresp.meta_value     AS coresp_data,
    reprez.meta_value     AS reprezentant_data,
    banca.meta_value      AS banca_data,
    contract.meta_value   AS contract_data,

    /* câmpuri derivate (căutare/sort) */
    CASE
      WHEN firma.meta_value IS NULL THEN NULL
      WHEN LOCATE('"denumire";s:', firma.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(firma.meta_value,
                       LOCATE(':"', firma.meta_value, LOCATE('"denumire";s:', firma.meta_value)) + 2),
             '"', 1)
    END AS company_name,

    CASE
      WHEN firma.meta_value IS NULL THEN NULL
      WHEN LOCATE('"cui";s:', firma.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(firma.meta_value,
                       LOCATE(':"', firma.meta_value, LOCATE('"cui";s:', firma.meta_value)) + 2),
             '"', 1)
    END AS fiscal_code,

    CASE
      WHEN reprez.meta_value IS NULL THEN NULL
      WHEN LOCATE('"email";s:', reprez.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(reprez.meta_value,
                       LOCATE(':"', reprez.meta_value, LOCATE('"email";s:', reprez.meta_value)) + 2),
             '"', 1)
    END AS email,

    CASE
      WHEN reprez.meta_value IS NULL THEN NULL
      WHEN LOCATE('"tel";s:', reprez.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(reprez.meta_value,
                       LOCATE(':"', reprez.meta_value, LOCATE('"tel";s:', reprez.meta_value)) + 2),
             '"', 1)
    END AS phone,

    CASE
      WHEN banca.meta_value IS NULL THEN NULL
      WHEN LOCATE('"iban";s:', banca.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(banca.meta_value,
                       LOCATE(':"', banca.meta_value, LOCATE('"iban";s:', banca.meta_value)) + 2),
             '"', 1)
    END AS iban,

    CASE
      WHEN contract.meta_value IS NULL THEN NULL
      WHEN LOCATE('"suma";s:', contract.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(contract.meta_value,
                       LOCATE(':"', contract.meta_value, LOCATE('"suma";s:', contract.meta_value)) + 2),
             '"', 1)
    END AS amount_str,

    CAST(
      CASE
        WHEN contract.meta_value IS NULL THEN NULL
        WHEN LOCATE('"suma";s:', contract.meta_value) = 0 THEN NULL
        ELSE SUBSTRING_INDEX(
               SUBSTRING(contract.meta_value,
                         LOCATE(':"', contract.meta_value, LOCATE('"suma";s:', contract.meta_value)) + 2),
               '"', 1)
      END AS UNSIGNED
    ) AS amount_num,

    CASE
      WHEN contract.meta_value IS NULL THEN NULL
      WHEN LOCATE('"data";s:', contract.meta_value) = 0 THEN NULL
      ELSE SUBSTRING_INDEX(
             SUBSTRING(contract.meta_value,
                       LOCATE(':"', contract.meta_value, LOCATE('"data";s:', contract.meta_value)) + 2),
             '"', 1)
    END AS contract_date,

    /* linkuri */
    CONCAT('http://actiunepentrusanatate.ro', '/wp-json/aps/v1/sponsorships/', s.ID)        AS detail,
    CONCAT('http://actiunepentrusanatate.ro', '/wp-admin/post.php?post=', s.ID, '&action=edit') AS admin_edit,

    /* flags persistente */
    COALESCE(ps.is_downloaded, 0) AS downloaded,
    COALESCE(ps.is_verified,   0) AS verified,
    COALESCE(ps.is_corrupt,    0) AS corrupt

  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta d        ON d.post_id  = s.ID AND d.meta_key  = '_aps_doc_id'
  LEFT JOIN wordpress.wp_postmeta j        ON j.post_id  = s.ID AND j.meta_key  = '_aps_json_id'
  LEFT JOIN wordpress.wp_postmeta sg       ON sg.post_id = s.ID AND sg.meta_key = '_aps_signature_id'

  LEFT JOIN wordpress.wp_postmeta docfile  ON docfile.post_id  = d.meta_value  AND docfile.meta_key  = '_wp_attached_file'
  LEFT JOIN wordpress.wp_postmeta jsonfile ON jsonfile.post_id = j.meta_value  AND jsonfile.meta_key = '_wp_attached_file'
  LEFT JOIN wordpress.wp_postmeta sigfile  ON sigfile.post_id  = sg.meta_value AND sigfile.meta_key  = '_wp_attached_file'

  LEFT JOIN wordpress.wp_postmeta firma     ON firma.post_id    = s.ID AND firma.meta_key    = '_aps_firma'
  LEFT JOIN wordpress.wp_postmeta coresp    ON coresp.post_id   = s.ID AND coresp.meta_key   = '_aps_coresp'
  LEFT JOIN wordpress.wp_postmeta reprez    ON reprez.post_id   = s.ID AND reprez.meta_key   = '_aps_reprez'
  LEFT JOIN wordpress.wp_postmeta banca     ON banca.post_id    = s.ID AND banca.meta_key    = '_aps_banca'
  LEFT JOIN wordpress.wp_postmeta contract  ON contract.post_id = s.ID AND contract.meta_key = '_aps_contract'

  LEFT JOIN wordpress.wp_posts_settings ps  ON ps.post_id = s.ID
  WHERE s.post_type = 'aps_sponsorship'
    AND s.post_status = 'publish'
""")
public class Sponsorizare {
    @Id
    @Column(name="id") private Integer id;

    @Column(name="post_date_iso") private String postDateIso;
    @Column(name="title")         private String title;

    @Column(name="doc_url")       private String docUrl;
    @Column(name="json_url")      private String jsonUrl;
    @Column(name="signature_url") private String signatureUrl;

    @Column(name="firma_data", columnDefinition="text")      private String firmaData;
    @Column(name="coresp_data", columnDefinition="text")     private String corespData;
    @Column(name="reprezentant_data", columnDefinition="text") private String reprezentantData;
    @Column(name="banca_data", columnDefinition="text")      private String bancaData;
    @Column(name="contract_data", columnDefinition="text")   private String contractData;

    @Column(name="company_name")  private String companyName;
    @Column(name="fiscal_code")   private String fiscalCode;
    @Column(name="email")         private String email;
    @Column(name="phone")         private String phone;
    @Column(name="iban")          private String iban;

    @Column(name="amount_str")    private String amountStr;
    @Column(name="amount_num")    private Long   amountNum;
    @Column(name="contract_date") private String contractDate;

    @Column(name="detail")        private String detail;
    @Column(name="admin_edit")    private String adminEdit;

    @Column(name="downloaded")    private Boolean downloaded;
    @Column(name="verified")      private Boolean verified;
    @Column(name="corrupt")       private Boolean corrupt;
}