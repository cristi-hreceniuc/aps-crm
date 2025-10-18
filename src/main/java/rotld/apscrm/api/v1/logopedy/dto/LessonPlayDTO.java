package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.enums.LessonType;

import java.util.List;

public record LessonPlayDTO(
        Long id, String title, String hint,
        LessonType lessonType, Integer position,
        List<ScreenDTO> screens
) {}