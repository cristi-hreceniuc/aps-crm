package rotld.apscrm.api.v1.user.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponseDto (
        UUID id,
        String firstName,
        String lastName,
        String email,
        String password,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String gender,
        UserRole role,
        UserStatus status
){
}
