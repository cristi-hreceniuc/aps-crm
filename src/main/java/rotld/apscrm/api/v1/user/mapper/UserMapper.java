package rotld.apscrm.api.v1.user.mapper;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import rotld.apscrm.api.v1.logopedy.service.S3Service;
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

    public static UserResponseDto toDto(User user, S3Service s3Service) {
        // Convert S3 key to presigned URL if it exists
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty() && s3Service.isS3Key(profileImageUrl)) {
            profileImageUrl = s3Service.generatePresignedUrl(profileImageUrl);
        }

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
                .profileImageUrl(profileImageUrl)
                .build();
    }

}
