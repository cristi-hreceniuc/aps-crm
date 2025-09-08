package rotld.apscrm.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.user.dto.LoginUserDto;
import rotld.apscrm.api.v1.user.dto.RegisterUserDto;
import rotld.apscrm.api.v1.user.dto.UserRole;
import rotld.apscrm.api.v1.user.dto.UserStatus;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    public User signup(RegisterUserDto input) {
        User user = User.builder()
                .lastName(input.lastName())
                .firstName(input.firstName())
                .gender(input.gender())
                .email(input.email())
                .password(passwordEncoder.encode(input.password()))
                .userRole(UserRole.USER)
                .userStatus(UserStatus.PENDING)
                .build();

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        return userRepository.findByEmail(input.email())
                .orElseThrow();
    }
}