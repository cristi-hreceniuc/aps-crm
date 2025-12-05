package rotld.apscrm.api.v1.user.dto;

public record UserProfileDto(
        String fullName,
        String email,
        UserRole userRole
) {
}
