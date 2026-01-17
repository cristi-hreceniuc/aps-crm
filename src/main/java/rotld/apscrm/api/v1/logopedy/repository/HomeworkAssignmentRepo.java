package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rotld.apscrm.api.v1.logopedy.entities.HomeworkAssignment;

import java.util.List;

@Repository
public interface HomeworkAssignmentRepo extends JpaRepository<HomeworkAssignment, Long> {
    
    /**
     * Active homework only (not closed by specialist yet).
     */
    List<HomeworkAssignment> findByProfileIdAndSpecialistDoneAtIsNullOrderByAssignedAtDesc(Long profileId);
    
    @Modifying
    @Query("DELETE FROM HomeworkAssignment ha WHERE ha.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}

