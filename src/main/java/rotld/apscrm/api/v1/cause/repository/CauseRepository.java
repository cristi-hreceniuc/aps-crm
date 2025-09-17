package rotld.apscrm.api.v1.cause.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CauseRepository extends JpaRepository<Cause, Integer>,
        JpaSpecificationExecutor<Cause> {

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE wordpress.wp_postmeta m
    SET m.meta_value = CAST(m.meta_value AS DECIMAL(20,2)) + :value
    WHERE m.post_id = :id
      AND m.meta_key = 'frmaster-donated-amount'
    """, nativeQuery = true)
    void addAmountToCause(@Param("id") Integer id, @Param("value") Double value);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE wordpress.wp_postmeta m
    SET m.meta_value = GREATEST(CAST(m.meta_value AS DECIMAL(20,2)) - :value, 0)
    WHERE m.post_id = :id
      AND m.meta_key = 'frmaster-donated-amount'
    """, nativeQuery = true)
    void subtractAmountFromCause(@Param("id") Integer id, @Param("value") Double value);

}