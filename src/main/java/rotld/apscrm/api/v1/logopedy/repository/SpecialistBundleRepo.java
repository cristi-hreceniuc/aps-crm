package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rotld.apscrm.api.v1.logopedy.entities.SpecialistBundle;

import java.util.Optional;

@Repository
public interface SpecialistBundleRepo extends JpaRepository<SpecialistBundle, Long> {
    
    Optional<SpecialistBundle> findBySpecialistId(String specialistId);
    
    boolean existsBySpecialistId(String specialistId);
    
    @Modifying
    @Query("DELETE FROM SpecialistBundle sb WHERE sb.specialist.id = :specialistId")
    void deleteBySpecialistId(String specialistId);
}

