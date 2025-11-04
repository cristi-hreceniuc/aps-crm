package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rotld.apscrm.api.v1.logopedy.entities.ProfileProgress;

import java.util.Optional;

public interface ProfileProgressRepo extends JpaRepository<ProfileProgress, Long> {
    Optional<ProfileProgress> findFirstByProfileIdOrderByUpdatedAtDesc(Long profileId);

    @Modifying
    @Query("DELETE FROM ProfileProgress pp WHERE pp.profile.id = :profileId")
    void deleteAllByProfileId(@Param("profileId") Long profileId);
}
