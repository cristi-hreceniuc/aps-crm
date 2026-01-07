package rotld.apscrm.api.v1.kids.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kids.dto.ActivateKeyRequest;
import rotld.apscrm.api.v1.kids.dto.LicenseKeyDTO;
import rotld.apscrm.api.v1.kids.service.LicenseKeyService;
import rotld.apscrm.api.v1.user.repository.User;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SPECIALIST_BUNDLE')")
public class LicenseKeyController {

    private final LicenseKeyService keyService;

    /**
     * List all keys for the authenticated specialist
     */
    @GetMapping
    public ResponseEntity<List<LicenseKeyDTO>> listMyKeys(@AuthenticationPrincipal User specialist) {
        return ResponseEntity.ok(keyService.listKeys(specialist.getId()));
    }

    /**
     * Get key statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<LicenseKeyService.KeyStats> getKeyStats(@AuthenticationPrincipal User specialist) {
        return ResponseEntity.ok(keyService.getKeyStats(specialist.getId()));
    }

    /**
     * Activate a key by linking it to a profile
     */
    @PostMapping("/{keyId}/activate")
    public ResponseEntity<Void> activateKey(
            @PathVariable Long keyId,
            @RequestBody ActivateKeyRequest request,
            @AuthenticationPrincipal User specialist) {
        keyService.activateKey(keyId, request.profileId(), specialist.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Reset a key (delete profile and progress, unlink key)
     */
    @PostMapping("/{keyId}/reset")
    public ResponseEntity<Void> resetKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal User specialist) {
        keyService.resetKey(keyId, specialist.getId());
        return ResponseEntity.ok().build();
    }
}

