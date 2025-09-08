package rotld.apscrm.api.v1.user.mapper;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.repository.User;

public class UserMapper {

    private UserMapper() {}

    public static UserResponseDto toEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .password(new BCryptPasswordEncoder().encode(user.getPassword()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .gender(user.getGender())
                .role(user.getUserRole())
                .status(user.getUserStatus())
                .build();
    }
}
