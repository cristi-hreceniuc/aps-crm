package rotld.apscrm.api.v1.kids.dto;

import java.time.LocalDateTime;

public record LicenseKeyDTO(
    Long id,
    String keyUuid,
    String profileName,
    Long profileId,
    boolean isActive,
    LocalDateTime activatedAt,
    LocalDateTime createdAt
) {}

