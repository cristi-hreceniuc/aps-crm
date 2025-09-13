package rotld.apscrm.api.v1.cause.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface CauseRepository extends JpaRepository<Cause, Integer>,
        JpaSpecificationExecutor<Cause> {}