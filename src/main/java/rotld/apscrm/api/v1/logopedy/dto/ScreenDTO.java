package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.enums.ScreenType;

import java.util.Map;

public record ScreenDTO(
        Long id, ScreenType type,
        Map<String, Object> payload,
        Integer position
) {}