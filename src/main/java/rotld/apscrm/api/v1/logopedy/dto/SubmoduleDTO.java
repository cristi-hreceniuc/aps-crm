package rotld.apscrm.api.v1.logopedy.dto;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record SubmoduleDTO(
        Long id, String title, String introText, Integer position, List<LessonDTO> lessons
) {}