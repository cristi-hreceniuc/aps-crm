package rotld.apscrm.api.v1.logopedy.dto;

import java.util.List;

public record ModuleDTO(
        Long id, String title, String introText, Integer position, boolean isPremium,
        List<SubmoduleDTO> submodules
) {}