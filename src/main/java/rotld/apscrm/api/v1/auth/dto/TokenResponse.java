package rotld.apscrm.api.v1.auth.dto;

import lombok.Builder;
@Builder
public record TokenResponse(
        String token, long expiresIn,
        String refreshToken, long refreshExpiresIn
) {}