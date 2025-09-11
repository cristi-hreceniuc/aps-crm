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

}