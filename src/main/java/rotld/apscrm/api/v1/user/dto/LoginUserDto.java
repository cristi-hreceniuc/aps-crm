package rotld.apscrm.api.v1.user.dto;

public record LoginUserDto(
        String email,
        String password,
        String platform  // "MOBILE" or "WEB" - identifies which client is logging in
) {}
