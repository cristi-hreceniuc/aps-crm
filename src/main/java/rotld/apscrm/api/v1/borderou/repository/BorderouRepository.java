package rotld.apscrm.api.v1.borderou.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BorderouRepository extends JpaRepository<Borderou, Integer> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE wordpress.wp_posts SET nr_borderou = ?2 WHERE ID = ?1", nativeQuery = true)
    void setBorderou(Integer postId, Integer borderouId);

    @Modifying @Transactional
    @Query(value = "UPDATE wordpress.wp_posts SET nr_borderou = ?2 WHERE ID IN (?1)", nativeQuery = true)
    void setBorderouForIds(java.util.List<Integer> ids, Integer borderouId);
}