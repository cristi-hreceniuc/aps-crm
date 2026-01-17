package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rotld.apscrm.api.v1.logopedy.entities.Submodule;

import java.util.List;

public interface SubmoduleRepo extends JpaRepository<Submodule, Long> {
    List<Submodule> findAllByModule_IdOrderByPositionAsc(Long moduleId);
    
    List<Submodule> findByModuleId(Long moduleId);
}