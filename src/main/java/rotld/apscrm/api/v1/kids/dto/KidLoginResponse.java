package rotld.apscrm.api.v1.kids.dto;

public record KidLoginResponse(
    String accessToken,
    long accessExpiresIn,
    String refreshToken,
    long refreshExpiresIn,
    Long profileId,
    String profileName,
    boolean isPremium
) {}

