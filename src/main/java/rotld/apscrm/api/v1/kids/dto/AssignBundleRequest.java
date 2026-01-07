package rotld.apscrm.api.v1.kids.dto;

public record AssignBundleRequest(
    String specialistId,
    int keyCount,
    String notes
) {}

