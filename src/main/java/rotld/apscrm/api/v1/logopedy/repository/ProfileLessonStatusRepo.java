package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rotld.apscrm.api.v1.logopedy.entities.ProfileLessonKey;
import rotld.apscrm.api.v1.logopedy.entities.ProfileLessonStatus;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;

import java.util.List;

public interface ProfileLessonStatusRepo extends JpaRepository<ProfileLessonStatus, ProfileLessonKey> {
    long countByIdProfileIdAndStatus(Long profileId, LessonStatus status);

    @Query("select count(pls) from ProfileLessonStatus pls where pls.id.profileId=:profileId and pls.status='DONE'")
    long completedCount(Long profileId);

    // toate statusurile unui profil (le mapăm în service)
    @Query("select pls from ProfileLessonStatus pls where pls.id.profileId = :profileId")
    List<ProfileLessonStatus> findAllByProfileId(Long profileId);

    // câte statusuri are profilul în submodulul X (JOIN pe Lesson prin id)
    @Query("""
     select count(pls) from ProfileLessonStatus pls, Lesson l
     where pls.id.profileId = :profileId
       and l.id = pls.id.lessonId
       and l.submodule.id = :submoduleId
  """)
    long countInSubmodule(Long profileId, Long submoduleId);

    boolean existsById(ProfileLessonKey id);

    List<ProfileLessonStatus> findAllByIdProfileId(Long profileId);

    @Modifying
    @Query("DELETE FROM ProfileLessonStatus pls WHERE pls.id.profileId = :profileId")
    void deleteAllByProfileId(@Param("profileId") Long profileId);

}
