package rotld.apscrm.api.v1.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.user.dto.LoginResponse;
import rotld.apscrm.api.v1.user.dto.LoginUserDto;
import rotld.apscrm.api.v1.user.dto.RegisterUserDto;
import rotld.apscrm.api.v1.user.dto.UserProfileDto;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.services.AuthenticationService;
import rotld.apscrm.services.JwtService;

@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public User register(@RequestBody RegisterUserDto registerUserDto) {
        return authenticationService.signup(registerUserDto);
    }

    @PostMapping("/login")
    public LoginResponse authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        return LoginResponse.builder()
                .token(jwtToken)
                .expiresIn(jwtService.getExpirationTime())
                .user(new UserProfileDto(authenticatedUser.getFirstName() + " " + authenticatedUser.getLastName(), authenticatedUser.getEmail()))
                .build();
    }
}