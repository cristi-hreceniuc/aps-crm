package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rotld.apscrm.api.v1.logopedy.entities.LessonScreen;

import java.util.List;

public interface LessonScreenRepo extends JpaRepository<LessonScreen, Long> {
    List<LessonScreen> findByLessonIdOrderByPositionAsc(Long lessonId);
}