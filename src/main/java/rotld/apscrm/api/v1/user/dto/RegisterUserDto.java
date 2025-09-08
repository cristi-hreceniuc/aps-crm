package rotld.apscrm.api.v1.user.dto;

public record RegisterUserDto(
        String email,
        String password,
        String firstName,
        String lastName,
        String gender
) {}
