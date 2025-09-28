package rotld.apscrm.api.v1.user.dto;

public record ConfirmResetDto(String token, String password, String confirmPassword) {
}
