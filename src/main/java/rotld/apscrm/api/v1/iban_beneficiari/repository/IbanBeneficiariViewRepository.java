package rotld.apscrm.api.v1.iban_beneficiari.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IbanBeneficiariViewRepository
        extends JpaRepository<IbanBeneficiari, Integer>, JpaSpecificationExecutor<IbanBeneficiari> {

}
