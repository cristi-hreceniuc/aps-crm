package rotld.apscrm.api.v1.kids.dto;

import java.time.LocalDate;

public record AssignHomeworkRequest(
    Long moduleId,
    Long submoduleId,
    Long partId,
    LocalDate dueDate,
    String notes
) {}

