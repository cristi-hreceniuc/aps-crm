package rotld.apscrm.api.v1.user.dto;

public record ResetWithOtpDto(String email, String otp, String password, String confirmPassword) {
}
