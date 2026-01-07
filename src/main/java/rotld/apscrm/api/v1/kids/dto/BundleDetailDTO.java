package rotld.apscrm.api.v1.kids.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BundleDetailDTO(
    String specialistId,
    String specialistName,
    String specialistEmail,
    int totalKeys,
    long usedKeys,
    boolean isPremium,
    LocalDateTime assignedAt,
    String assignedByName,
    String notes,
    List<LicenseKeyDTO> keys
) {}

