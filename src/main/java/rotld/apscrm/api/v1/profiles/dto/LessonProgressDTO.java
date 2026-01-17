package rotld.apscrm.api.v1.profiles.dto;

public record LessonProgressDTO(
        Long moduleId, String moduleTitle,
        Long submoduleId, String submoduleTitle,
        Long partId, String partTitle,
        Long lessonId, String lessonTitle,
        String status // LOCKED/UNLOCKED/DONE
) {}