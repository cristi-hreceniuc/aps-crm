package rotld.apscrm.api.v1.kids.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HomeworkDTO(
    Long id,
    Long profileId,
    Long moduleId,
    String moduleName,
    Long submoduleId,
    String submoduleName,
    Long partId,
    String partName,
    LocalDateTime assignedAt,
    LocalDate dueDate,
    String notes
) {}

