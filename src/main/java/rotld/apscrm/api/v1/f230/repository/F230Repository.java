package rotld.apscrm.api.v1.f230.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface F230Repository
        extends JpaRepository<F230, Integer>, JpaSpecificationExecutor<F230> {
    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_postmeta WHERE post_id = :id", nativeQuery = true)
    int deleteMeta(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_posts WHERE ID = :id AND post_type = 'formular230'", nativeQuery = true)
    int deletePost(@Param("id") Integer id);

    @Query(value = "SELECT * FROM wordpress.wp_posts WHERE post_type = 'formular230'", nativeQuery = true)
    List<F230> findAll();

    @Query(value = """
  SELECT
    s.ID AS id,
    DATE_FORMAT(s.post_date, '%Y-%m-%dT%H:%i:%s') AS post_date_iso,
    s.post_title AS title,

    anul.meta_value      AS year,
    nume.meta_value      AS first_name,
    prenume.meta_value   AS last_name,
    initiala.meta_value  AS initiala,
    cnp.meta_value       AS cnp,

    strada.meta_value    AS street,
    numar.meta_value     AS number,
    bloc.meta_value      AS block,
    scara.meta_value     AS staircase,
    etaj.meta_value      AS floor,
    apartament.meta_value AS apartment,
    judet.meta_value     AS county,
    localitate.meta_value AS city,
    codPostal.meta_value AS postal_code,

    email.meta_value     AS email,
    telefon.meta_value   AS phone,
    fax.meta_value       AS fax,
    iban.meta_value      AS iban,
    dist2.meta_value     AS distrib2,
    acord.meta_value     AS acord_email,

    pdf_url.meta_value   AS pdf_url,
    s.nr_borderou        AS nr_borderou,
    CONCAT('http://actiunepentrusanatate.ro','/wp-admin/post.php?post=', s.ID, '&action=edit') AS admin_edit

  FROM wordpress.wp_posts s
  LEFT JOIN wordpress.wp_postmeta anul       ON anul.post_id = s.ID AND anul.meta_key = 'anul'
  LEFT JOIN wordpress.wp_postmeta nume       ON nume.post_id = s.ID AND nume.meta_key = 'nume'
  LEFT JOIN wordpress.wp_postmeta prenume    ON prenume.post_id = s.ID AND prenume.meta_key = 'prenume'
  LEFT JOIN wordpress.wp_postmeta initiala   ON initiala.post_id = s.ID AND initiala.meta_key = 'initiala'
  LEFT JOIN wordpress.wp_postmeta cnp        ON cnp.post_id = s.ID AND cnp.meta_key = 'cnp'
  LEFT JOIN wordpress.wp_postmeta strada     ON strada.post_id = s.ID AND strada.meta_key = 'strada'
  LEFT JOIN wordpress.wp_postmeta numar      ON numar.post_id = s.ID AND numar.meta_key = 'numar'
  LEFT JOIN wordpress.wp_postmeta bloc       ON bloc.post_id = s.ID AND bloc.meta_key = 'bloc'
  LEFT JOIN wordpress.wp_postmeta scara      ON scara.post_id = s.ID AND scara.meta_key = 'scara'
  LEFT JOIN wordpress.wp_postmeta etaj       ON etaj.post_id = s.ID AND etaj.meta_key = 'etaj'
  LEFT JOIN wordpress.wp_postmeta apartament ON apartament.post_id = s.ID AND apartament.meta_key = 'apartament'
  LEFT JOIN wordpress.wp_postmeta judet      ON judet.post_id = s.ID AND judet.meta_key = 'judet'
  LEFT JOIN wordpress.wp_postmeta localitate ON localitate.post_id = s.ID AND localitate.meta_key = 'localitate'
  LEFT JOIN wordpress.wp_postmeta codPostal  ON codPostal.post_id = s.ID AND codPostal.meta_key = 'codPostal'
  LEFT JOIN wordpress.wp_postmeta email      ON email.post_id = s.ID AND email.meta_key = 'email'
  LEFT JOIN wordpress.wp_postmeta telefon    ON telefon.post_id = s.ID AND telefon.meta_key = 'telefon'
  LEFT JOIN wordpress.wp_postmeta fax        ON fax.post_id = s.ID AND fax.meta_key = 'fax'
  LEFT JOIN wordpress.wp_postmeta iban       ON iban.post_id = s.ID AND iban.meta_key = 'iban'
  LEFT JOIN wordpress.wp_postmeta dist2      ON dist2.post_id = s.ID AND dist2.meta_key = 'distribuire2ani'
  LEFT JOIN wordpress.wp_postmeta acord      ON acord.post_id = s.ID AND acord.meta_key = 'acordComunicare'
  LEFT JOIN wordpress.wp_postmeta pdf_url    ON pdf_url.post_id = s.ID AND pdf_url.meta_key = '_pdf_url'
  WHERE s.ID = :id AND s.post_type='formular230' AND s.post_status='publish'
""", nativeQuery = true)
    F230DetailRow findDetailById(@Param("id") Integer id);
}