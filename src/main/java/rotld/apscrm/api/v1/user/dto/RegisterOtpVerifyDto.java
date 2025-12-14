package rotld.apscrm.api.v1.user.dto;

public record RegisterOtpVerifyDto(
        String email,
        String otp
) {}
