package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rotld.apscrm.api.v1.logopedy.entities.Lesson;
import rotld.apscrm.api.v1.logopedy.entities.ProfileLessonKey;

import java.util.List;

public interface LessonRepo extends JpaRepository<Lesson, Long> {
    List<Lesson> findBySubmoduleIdOrderByPositionAsc(Long submoduleId);
    @Query("select count(l) from Lesson l where l.isActive=true")
    long countActive();
    @Query("""
    select l from Lesson l
    join l.submodule s
    join s.module m
    where l.isActive=true
    order by m.position, s.position, l.position
  """)
    List<Lesson> findAllActiveOrdered();

    @Query("""
  select l from Lesson l
  where l.submodule.id = :submoduleId and l.isActive = true
  order by l.position asc
""")
    List<Lesson> findBySubmoduleOrdered(Long submoduleId);

    @Query("""
     select l.id from Lesson l
     where l.submodule.id = :submoduleId and l.isActive = true
     order by l.position asc
  """)
    List<Long> findIdsBySubmoduleOrdered(Long submoduleId);
}