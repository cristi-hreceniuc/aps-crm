package rotld.apscrm.api.v1.logopedy.dto;

import java.util.List;

/**
 * DTO containing all assets for a submodule for offline caching
 */
public record SubmoduleAssetsDTO(
        Long submoduleId,
        String submoduleTitle,
        List<AssetInfoDTO> assets,
        Integer totalAssets,
        Long estimatedSizeBytes
) {}
