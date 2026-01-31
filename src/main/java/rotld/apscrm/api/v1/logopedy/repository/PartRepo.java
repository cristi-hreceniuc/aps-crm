package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rotld.apscrm.api.v1.logopedy.entities.Part;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepo extends JpaRepository<Part, Long> {
    
    /**
     * Find all parts for a specific submodule, ordered by position
     */
    List<Part> findBySubmoduleIdAndIsActiveTrueOrderByPositionAsc(Long submoduleId);
    
    /**
     * Find a part by submodule and slug
     */
    Optional<Part> findBySubmoduleIdAndSlug(Long submoduleId, String slug);
    
    /**
     * Count active parts in a submodule
     */
    long countBySubmoduleIdAndIsActiveTrue(Long submoduleId);
    
    /**
     * Find all active parts for a submodule (without ordering)
     */
    List<Part> findBySubmoduleIdAndIsActiveTrue(Long submoduleId);
}
