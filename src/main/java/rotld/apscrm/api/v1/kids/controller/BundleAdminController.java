package rotld.apscrm.api.v1.kids.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kids.dto.*;
import rotld.apscrm.api.v1.kids.service.SpecialistBundleService;
import rotld.apscrm.api.v1.user.repository.User;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bundles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class BundleAdminController {

    private final SpecialistBundleService bundleService;

    /**
     * List all specialists with bundles
     */
    @GetMapping
    public ResponseEntity<List<BundleListDTO>> listBundles() {
        return ResponseEntity.ok(bundleService.listBundles());
    }

    /**
     * Get bundle details for a specialist
     */
    @GetMapping("/{specialistId}")
    public ResponseEntity<BundleDetailDTO> getBundleDetail(@PathVariable String specialistId) {
        return ResponseEntity.ok(bundleService.getBundleDetail(specialistId));
    }

    /**
     * Assign a bundle to a specialist (SPECIALIST → SPECIALIST_BUNDLE)
     */
    @PostMapping
    public ResponseEntity<Void> assignBundle(
            @RequestBody AssignBundleRequest request,
            @AuthenticationPrincipal User admin) {
        bundleService.assignBundle(
                request.specialistId(),
                request.keyCount(),
                admin.getId(),
                request.notes()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Toggle premium status for a specialist
     */
    @PutMapping("/{specialistId}/premium")
    public ResponseEntity<Void> togglePremium(
            @PathVariable String specialistId,
            @RequestBody TogglePremiumRequest request) {
        bundleService.togglePremium(specialistId, request.isPremium());
        return ResponseEntity.ok().build();
    }

    /**
     * Revoke bundle (SPECIALIST_BUNDLE → SPECIALIST)
     */
    @DeleteMapping("/{specialistId}")
    public ResponseEntity<Void> revokeBundle(@PathVariable String specialistId) {
        bundleService.revokeBundle(specialistId);
        return ResponseEntity.ok().build();
    }
}

