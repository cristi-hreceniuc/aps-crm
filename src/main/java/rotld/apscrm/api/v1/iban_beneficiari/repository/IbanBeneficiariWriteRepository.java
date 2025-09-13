package rotld.apscrm.api.v1.iban_beneficiari.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerMeta;

public interface IbanBeneficiariWriteRepository extends CrudRepository<VolunteerMeta, Integer> {

    // UPDATE - întoarce numărul de rânduri afectate
    @Modifying
    @Transactional
    @Query("UPDATE VolunteerMeta pm SET pm.metaValue = :value WHERE pm.volunteer.id = :postId AND pm.metaKey = :key")
    int updateMeta(Integer postId, String key, String value);

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_postmeta WHERE post_id = :id", nativeQuery = true)
    int deleteMeta(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_posts WHERE ID = :id AND post_type = 'iban_beneficiar'", nativeQuery = true)
    int deletePost(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = """
      INSERT INTO wordpress.wp_posts
        (post_author, post_date, post_date_gmt, post_content, post_title,
         post_excerpt, post_status, comment_status, ping_status,
         post_name, post_type, to_ping, pinged, post_content_filtered, guid)
      VALUES
        (1, NOW(), NOW(), '', '', '', 'publish', 'closed', 'closed',
         '', 'iban_beneficiar', '', '', '', UUID())
      """, nativeQuery = true)
    int insertIbanPost();

    @Modifying
    @Transactional
    @Query(value = """
      INSERT INTO wordpress.wp_postmeta (post_id, meta_key, meta_value)
      VALUES (:postId, :key, :value)
      """, nativeQuery = true)
    void insertMeta(Integer postId, String key, String value);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    int getLastInsertId();
}