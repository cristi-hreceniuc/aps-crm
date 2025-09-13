package rotld.apscrm.api.v1.cause.offline_payment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfflinePaymentViewRepository
        extends JpaRepository<OfflinePaymentView, Integer>, JpaSpecificationExecutor<OfflinePaymentView> {}