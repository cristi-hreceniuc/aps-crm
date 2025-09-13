package rotld.apscrm.api.v1.cause.offline_payment.repository;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OfflinePaymentWriteRepository extends CrudRepository<OfflinePaymentView, Integer> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE wordpress.wp_frmaster_order SET order_status = :status WHERE id = :id", nativeQuery = true)
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM wordpress.wp_frmaster_order WHERE id = :id", nativeQuery = true)
    int hardDelete(@Param("id") Integer id);
}