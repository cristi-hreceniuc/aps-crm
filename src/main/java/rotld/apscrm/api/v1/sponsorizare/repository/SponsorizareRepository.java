package rotld.apscrm.api.v1.sponsorizare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SponsorizareRepository
        extends JpaRepository<Sponsorizare, Integer>, JpaSpecificationExecutor<Sponsorizare> {

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_postmeta WHERE post_id = :id", nativeQuery = true)
    int deleteMeta(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_posts WHERE ID = :id AND post_type = 'aps_sponsorship'", nativeQuery = true)
    int deletePost(@Param("id") Integer id);
}
