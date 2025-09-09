package rotld.apscrm.api.v1.user.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        long expiresIn,
        UserProfileDto user
) {}
