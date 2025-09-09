package rotld.apscrm.api.v1.d177.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface D177Repository extends JpaRepository<D177, Integer>, JpaSpecificationExecutor<D177> {

    // DELETE: întâi meta, apoi post (same as înainte)
    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_postmeta WHERE post_id = :id", nativeQuery = true)
    int deleteMeta(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM wordpress.wp_posts WHERE ID = :id AND post_type = 'aps_s177'", nativeQuery = true)
    int deletePost(@Param("id") Integer id);
}