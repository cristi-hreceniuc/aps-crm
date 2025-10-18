package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rotld.apscrm.api.v1.logopedy.entities.ProfileProgress;

import java.util.Optional;

public interface ProfileProgressRepo extends JpaRepository<ProfileProgress, Long> {
    Optional<ProfileProgress> findFirstByProfileIdOrderByUpdatedAtDesc(Long profileId);
}
