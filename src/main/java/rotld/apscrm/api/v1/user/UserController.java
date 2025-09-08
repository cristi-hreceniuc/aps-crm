package rotld.apscrm.api.v1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.mapper.UserMapper;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.service.UserService;

import java.util.List;

@RequestMapping("/api/v1/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDto authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UserMapper.toEntity((User) authentication.getPrincipal());
    }

    @GetMapping
    public List<UserResponseDto> allUsers() {
        return userService.allUsers().stream()
                .map(UserMapper::toEntity)
                .toList();
    }
}