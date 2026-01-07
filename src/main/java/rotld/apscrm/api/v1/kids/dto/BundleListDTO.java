package rotld.apscrm.api.v1.kids.dto;

import java.time.LocalDateTime;

public record BundleListDTO(
    String specialistId,
    String specialistName,
    String specialistEmail,
    int totalKeys,
    long usedKeys,
    boolean isPremium,
    LocalDateTime assignedAt
) {}

