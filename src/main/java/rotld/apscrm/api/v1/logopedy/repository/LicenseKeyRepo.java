package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rotld.apscrm.api.v1.logopedy.entities.LicenseKey;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseKeyRepo extends JpaRepository<LicenseKey, Long> {

    Optional<LicenseKey> findByKeyUuidAndIsActiveTrue(String keyUuid);
    
    List<LicenseKey> findBySpecialistIdOrderByCreatedAtDesc(String specialistId);

    long countBySpecialistId(String specialistId);
    
    long countBySpecialistIdAndProfileIsNotNull(String specialistId);
    
    @Modifying
    @Query("DELETE FROM LicenseKey lk WHERE lk.specialist.id = :specialistId")
    void deleteBySpecialistId(String specialistId);
    
    boolean existsByProfileId(Long profileId);
}

