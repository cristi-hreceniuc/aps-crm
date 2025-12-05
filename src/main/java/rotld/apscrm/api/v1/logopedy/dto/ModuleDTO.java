package rotld.apscrm.api.v1.logopedy.dto;

import rotld.apscrm.api.v1.logopedy.enums.TargetAudience;
import java.util.List;

public record ModuleDTO(
        Long id, String title, String introText, Integer position, boolean isPremium,
        TargetAudience targetAudience,
        List<SubmoduleDTO> submodules
) {}