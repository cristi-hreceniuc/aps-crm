package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.entities.Lesson;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.enums.LessonType;

public record LessonDTO(Long id, String title, String hint,
                        LessonType lessonType, Integer position,
                        LessonStatus status) {

    public static LessonDTO of(Lesson l, LessonStatus st) {
        return new LessonDTO(l.getId(), l.getTitle(), l.getHint(),
                l.getLessonType(), l.getPosition(), st);
    }
}
