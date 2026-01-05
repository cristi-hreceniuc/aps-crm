package rotld.apscrm.api.v1.user.dto;

public record DeleteAccountConfirmDto(
        String email,
        String otp
) {}

