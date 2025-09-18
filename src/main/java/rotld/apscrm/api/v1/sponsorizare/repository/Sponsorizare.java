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
    CONCAT('http://actiunepentrusanatate.ro','/wp-content/uploads/', docfile.meta_value)  AS doc_url,
    CONCAT('http://actiunepentrusanatate.ro','/wp-content/uploads/', jsonfile.meta_value) AS json_url,
    CONCAT('http://actiunepentrusanatate.ro','/wp-content/uploads/', sigfile.meta_value)  AS signature_url,

    /* meta brute */
    firma.meta_value       AS firma_data,
    coresp.meta_value      AS coresp_data,
    reprez.meta_value      AS reprezentant_data,
    banca.meta_value       AS banca_data,
    contract.meta_value    AS contract_data,
    sigb64.meta_value      AS signature_b64,
    CASE WHEN sendmail.meta_value = '1' THEN 1 ELSE 0 END  AS send_email,

    /* câmpuri firmă (derivate) */
    CASE WHEN firma.meta_value IS NULL OR LOCATE('"denumire";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"denumire";s:', firma.meta_value))+2), '"', 1) END AS company_name,

    CASE WHEN firma.meta_value IS NULL OR LOCATE('"cui";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"cui";s:', firma.meta_value))+2), '"', 1) END AS fiscal_code,

    CASE WHEN firma.meta_value IS NULL OR LOCATE('"regcom";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"regcom";s:', firma.meta_value))+2), '"', 1) END AS company_regcom,

    CASE WHEN firma.meta_value IS NULL OR LOCATE('"adresa";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"adresa";s:', firma.meta_value))+2), '"', 1) END AS company_address,

    CASE WHEN firma.meta_value IS NULL OR LOCATE('"judet";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"judet";s:', firma.meta_value))+2), '"', 1) END AS company_county,

    CASE WHEN firma.meta_value IS NULL OR LOCATE('"oras";s:', firma.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(firma.meta_value,
                LOCATE(':"', firma.meta_value, LOCATE('"oras";s:', firma.meta_value))+2), '"', 1) END AS company_city,

    /* reprezentant */
    CASE WHEN reprez.meta_value IS NULL OR LOCATE('"email";s:', reprez.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(reprez.meta_value,
                LOCATE(':"', reprez.meta_value, LOCATE('"email";s:', reprez.meta_value))+2), '"', 1) END AS email,

    CASE WHEN reprez.meta_value IS NULL OR LOCATE('"tel";s:', reprez.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(reprez.meta_value,
                LOCATE(':"', reprez.meta_value, LOCATE('"tel";s:', reprez.meta_value))+2), '"', 1) END AS phone,

    CASE WHEN reprez.meta_value IS NULL OR LOCATE('"prenume";s:', reprez.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(reprez.meta_value,
                LOCATE(':"', reprez.meta_value, LOCATE('"prenume";s:', reprez.meta_value))+2), '"', 1) END AS rep_first_name,

    CASE WHEN reprez.meta_value IS NULL OR LOCATE('"nume";s:', reprez.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(reprez.meta_value,
                LOCATE(':"', reprez.meta_value, LOCATE('"nume";s:', reprez.meta_value))+2), '"', 1) END AS rep_last_name,

    CASE WHEN reprez.meta_value IS NULL OR LOCATE('"pozitie";s:', reprez.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(reprez.meta_value,
                LOCATE(':"', reprez.meta_value, LOCATE('"pozitie";s:', reprez.meta_value))+2), '"', 1) END AS rep_role,

    /* corespondență */
    CASE WHEN coresp.meta_value IS NULL OR LOCATE('"adresa";s:', coresp.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(coresp.meta_value,
                LOCATE(':"', coresp.meta_value, LOCATE('"adresa";s:', coresp.meta_value))+2), '"', 1) END AS corr_address,

    CASE WHEN coresp.meta_value IS NULL OR LOCATE('"judet";s:', coresp.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(coresp.meta_value,
                LOCATE(':"', coresp.meta_value, LOCATE('"judet";s:', coresp.meta_value))+2), '"', 1) END AS corr_county,

    CASE WHEN coresp.meta_value IS NULL OR LOCATE('"oras";s:', coresp.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(coresp.meta_value,
                LOCATE(':"', coresp.meta_value, LOCATE('"oras";s:', coresp.meta_value))+2), '"', 1) END AS corr_city,

    /* bancă */
    CASE WHEN banca.meta_value IS NULL OR LOCATE('"iban";s:', banca.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(banca.meta_value,
                LOCATE(':"', banca.meta_value, LOCATE('"iban";s:', banca.meta_value))+2), '"', 1) END AS iban,

    CASE WHEN banca.meta_value IS NULL OR LOCATE('"banca";s:', banca.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(banca.meta_value,
                LOCATE(':"', banca.meta_value, LOCATE('"banca";s:', banca.meta_value))+2), '"', 1) END AS bank_name,

    /* contract */
    CASE WHEN contract.meta_value IS NULL OR LOCATE('"suma";s:', contract.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(contract.meta_value,
                LOCATE(':"', contract.meta_value, LOCATE('"suma";s:', contract.meta_value))+2), '"', 1) END AS amount_str,

    CAST(
      CASE WHEN contract.meta_value IS NULL OR LOCATE('"suma";s:', contract.meta_value)=0 THEN NULL
           ELSE SUBSTRING_INDEX(SUBSTRING(contract.meta_value,
                  LOCATE(':"', contract.meta_value, LOCATE('"suma";s:', contract.meta_value))+2), '"', 1)
      END AS UNSIGNED) AS amount_num,

    CASE WHEN contract.meta_value IS NULL OR LOCATE('"data";s:', contract.meta_value)=0 THEN NULL
         ELSE SUBSTRING_INDEX(SUBSTRING(contract.meta_value,
                LOCATE(':"', contract.meta_value, LOCATE('"data";s:', contract.meta_value))+2), '"', 1) END AS contract_date,

    /* linkuri */
    CONCAT('http://actiunepentrusanatate.ro','/wp-json/aps/v1/sponsorships/', s.ID) AS detail,
    CONCAT('http://actiunepentrusanatate.ro','/wp-admin/post.php?post=', s.ID, '&action=edit') AS admin_edit,

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
  LEFT JOIN wordpress.wp_postmeta sigb64    ON sigb64.post_id   = s.ID AND sigb64.meta_key   = '_aps_signature_b64'
  LEFT JOIN wordpress.wp_postmeta sendmail  ON sendmail.post_id = s.ID AND sendmail.meta_key = '_aps_send_email'

  LEFT JOIN wordpress.wp_posts_settings ps  ON ps.post_id = s.ID
  WHERE s.post_type = 'aps_sponsorship'
    AND s.post_status = 'publish'
""")
public class Sponsorizare {
    @Id @Column(name="id") private Integer id;

    @Column(name="post_date_iso") private String postDateIso;
    @Column(name="title")         private String title;

    @Column(name="doc_url")       private String docUrl;
    @Column(name="json_url")      private String jsonUrl;
    @Column(name="signature_url") private String signatureUrl;

    // brute
    @Column(name="firma_data", columnDefinition="text")        private String firmaData;
    @Column(name="coresp_data", columnDefinition="text")       private String corespData;
    @Column(name="reprezentant_data", columnDefinition="text") private String reprezentantData;
    @Column(name="banca_data", columnDefinition="text")        private String bancaData;
    @Column(name="contract_data", columnDefinition="text")     private String contractData;
    @Column(name="signature_b64", columnDefinition="longtext") private String signatureB64;
    @Column(name="send_email")                                 private Boolean sendEmail;

    // firmă
    @Column(name="company_name")    private String companyName;
    @Column(name="fiscal_code")     private String fiscalCode;
    @Column(name="company_regcom")  private String companyRegCom;
    @Column(name="company_address") private String companyAddress;
    @Column(name="company_county")  private String companyCounty;
    @Column(name="company_city")    private String companyCity;

    // reprezentant
    @Column(name="email")           private String email;
    @Column(name="phone")           private String phone;
    @Column(name="rep_first_name")  private String repFirstName;
    @Column(name="rep_last_name")   private String repLastName;
    @Column(name="rep_role")        private String repRole;

    // corespondență
    @Column(name="corr_address")    private String corrAddress;
    @Column(name="corr_county")     private String corrCounty;
    @Column(name="corr_city")       private String corrCity;

    // bancă / contract
    @Column(name="iban")            private String iban;
    @Column(name="bank_name")       private String bankName;
    @Column(name="amount_str")      private String amountStr;
    @Column(name="amount_num")      private Long   amountNum;
    @Column(name="contract_date")   private String contractDate;

    @Column(name="detail")          private String detail;
    @Column(name="admin_edit")      private String adminEdit;

    @Column(name="downloaded")      private Boolean downloaded;
    @Column(name="verified")        private Boolean verified;
    @Column(name="corrupt")         private Boolean corrupt;
}
