package rotld.apscrm.api.v1.logopedy.dto;

import java.util.Map;

public record SummaryDTO(
        long completedModules, long completedLessons,
        Map<Long, Map<String, Integer>> perModule // moduleId -> {done,total}
) {}
