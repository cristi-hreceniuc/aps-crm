package rotld.apscrm.api.v1.user.mapper;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.repository.User;

public class UserMapper {

    private UserMapper() {}

    public static UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .gender(user.getGender())
                .role(user.getUserRole())
                .status(user.getUserStatus())
                .isPremium(user.getIsPremium())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

}
