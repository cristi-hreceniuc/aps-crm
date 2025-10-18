package rotld.apscrm.api.v1.logopedy.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record SubmoduleListDTO(
        Long id, String title, String introText, Integer position,
        java.util.List<LessonListItemDTO> lessons
) {}