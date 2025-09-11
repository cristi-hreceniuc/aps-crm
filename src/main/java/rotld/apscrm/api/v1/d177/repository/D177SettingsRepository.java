package rotld.apscrm.api.v1.d177.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface D177SettingsRepository extends JpaRepository<D177Settings, Integer> {
    List<D177Settings> findByPostIdIn(Collection<Integer> ids);
    Optional<D177Settings> findByPostId(Integer id);

    @Modifying
    @Query(value = """
    INSERT INTO wordpress.wp_posts_settings (post_id, is_downloaded, is_verified, is_corrupt)
    VALUES (
      :id,
      IFNULL(:downloaded, 0),
      IFNULL(:verified,   0),
      IFNULL(:corrupt,    0)
    )
    ON DUPLICATE KEY UPDATE
      is_downloaded = IFNULL(:downloaded, is_downloaded),
      is_verified   = IFNULL(:verified,   is_verified),
      is_corrupt    = IFNULL(:corrupt,    is_corrupt),
      updated_at    = CURRENT_TIMESTAMP
    """, nativeQuery = true)
    int upsertFlags(@Param("id") Integer id,
                    @Param("downloaded") Boolean downloaded,
                    @Param("verified")   Boolean verified,
                    @Param("corrupt")    Boolean corrupt);
}
