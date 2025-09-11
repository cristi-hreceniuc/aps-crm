package rotld.apscrm.api.v1.sponsorizare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SponsorizareRepository
        extends JpaRepository<Sponsorizare, Integer>, JpaSpecificationExecutor<Sponsorizare> {}
