package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.enums.LessonType;

public record LessonListItemDTO(
        Long id, String title, String hint,
        LessonType lessonType, Integer position,
        LessonStatus status
) {}