package rotld.apscrm.api.v1.kids.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kids.dto.KidLoginRequest;
import rotld.apscrm.api.v1.kids.dto.KidLoginResponse;
import rotld.apscrm.api.v1.logopedy.repository.LicenseKeyRepo;
import rotld.apscrm.services.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class KidAuthController {

    private final AuthenticationService authService;
    private final LicenseKeyRepo keyRepo;

    /**
     * Authenticate a kid using their license key UUID
     */
    @PostMapping("/kid-login")
    public ResponseEntity<KidLoginResponse> kidLogin(@RequestBody KidLoginRequest request) {
        var tokens = authService.authenticateKid(request.key(), keyRepo);
        
        return ResponseEntity.ok(new KidLoginResponse(
                tokens.accessToken(),
                tokens.accessExpiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn(),
                tokens.profileId(),
                tokens.profileName(),
                tokens.isPremium()
        ));
    }

    /**
     * Refresh a kid's access token using their refresh token
     */
    @PostMapping("/kid-refresh")
    public ResponseEntity<KidLoginResponse> kidRefresh(@RequestBody KidRefreshRequest request) {
        var tokens = authService.refreshKidToken(request.refreshToken(), keyRepo);
        
        return ResponseEntity.ok(new KidLoginResponse(
                tokens.accessToken(),
                tokens.accessExpiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn(),
                tokens.profileId(),
                tokens.profileName(),
                tokens.isPremium()
        ));
    }

    public record KidRefreshRequest(String refreshToken) {}
}

