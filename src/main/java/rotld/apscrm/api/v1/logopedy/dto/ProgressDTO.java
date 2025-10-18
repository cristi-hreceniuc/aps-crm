package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.enums.ProgressStatus;

public record ProgressDTO(
        Long moduleId, Long submoduleId, Long lessonId, Integer screenIndex, ProgressStatus status
) {}