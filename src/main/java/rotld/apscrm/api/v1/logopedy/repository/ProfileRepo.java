package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rotld.apscrm.api.v1.logopedy.entities.Profile;

import java.util.List;
import java.util.Optional;

public interface ProfileRepo extends JpaRepository<Profile, Long> {

    @Query("""
            SELECT p
            FROM Profile p
            WHERE p.id = :id
            AND p.user.id = :userId""")
    Optional<Profile> findByIdAndUserId(Long id, String userId);

    @Query("""
            SELECT p
            FROM Profile p
            WHERE p.user.id = :userId""")
    List<Profile> findAllByUserId(String userId);
}
