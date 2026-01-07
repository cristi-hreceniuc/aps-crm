package rotld.apscrm.api.v1.kids.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.kids.dto.BundleDetailDTO;
import rotld.apscrm.api.v1.kids.dto.BundleListDTO;
import rotld.apscrm.api.v1.kids.dto.LicenseKeyDTO;
import rotld.apscrm.api.v1.logopedy.entities.LicenseKey;
import rotld.apscrm.api.v1.logopedy.entities.SpecialistBundle;
import rotld.apscrm.api.v1.logopedy.repository.*;
import rotld.apscrm.api.v1.user.dto.UserRole;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialistBundleService {

    private final SpecialistBundleRepo bundleRepo;
    private final LicenseKeyRepo keyRepo;
    private final UserRepository userRepo;
    private final ProfileRepo profileRepo;
    private final ProfileLessonStatusRepo plsRepo;
    private final HomeworkAssignmentRepo homeworkRepo;

    /**
     * List all specialists with bundles (for admin view)
     */
    public List<BundleListDTO> listBundles() {
        return bundleRepo.findAll().stream()
                .map(bundle -> {
                    User specialist = bundle.getSpecialist();
                    long usedKeys = keyRepo.countBySpecialistIdAndProfileIsNotNull(specialist.getId());
                    return new BundleListDTO(
                            specialist.getId(),
                            specialist.getFirstName() + " " + specialist.getLastName(),
                            specialist.getEmail(),
                            bundle.getTotalKeys(),
                            usedKeys,
                            Boolean.TRUE.equals(specialist.getIsPremium()),
                            bundle.getAssignedAt()
                    );
                })
                .toList();
    }

    /**
     * Get bundle details with all keys
     */
    public BundleDetailDTO getBundleDetail(String specialistId) {
        SpecialistBundle bundle = bundleRepo.findBySpecialistId(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("Bundle not found for specialist"));

        User specialist = bundle.getSpecialist();
        long usedKeys = keyRepo.countBySpecialistIdAndProfileIsNotNull(specialistId);

        List<LicenseKeyDTO> keys = keyRepo.findBySpecialistIdOrderByCreatedAtDesc(specialistId).stream()
                .map(k -> new LicenseKeyDTO(
                        k.getId(),
                        k.getKeyUuid(),
                        k.getProfile() != null ? k.getProfile().getName() : null,
                        k.getProfile() != null ? k.getProfile().getId() : null,
                        k.isActive(),
                        k.getActivatedAt(),
                        k.getCreatedAt()
                ))
                .toList();

        return new BundleDetailDTO(
                specialist.getId(),
                specialist.getFirstName() + " " + specialist.getLastName(),
                specialist.getEmail(),
                bundle.getTotalKeys(),
                usedKeys,
                Boolean.TRUE.equals(specialist.getIsPremium()),
                bundle.getAssignedAt(),
                bundle.getAssignedBy().getFirstName() + " " + bundle.getAssignedBy().getLastName(),
                bundle.getNotes(),
                keys
        );
    }

    /**
     * Assign bundle to specialist: SPECIALIST → SPECIALIST_BUNDLE
     */
    @Transactional
    public void assignBundle(String specialistId, int keyCount, String adminId, String notes) {
        User specialist = userRepo.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("Specialist not found"));

        // Validate: must be SPECIALIST
        if (specialist.getUserRole() != UserRole.SPECIALIST) {
            throw new IllegalArgumentException("User must have SPECIALIST role to receive a bundle");
        }

        // Validate key count
        if (keyCount != 5 && keyCount != 10 && keyCount != 15 && keyCount != 25) {
            throw new IllegalArgumentException("Key count must be 5, 10, 15, or 25");
        }

        // Check if bundle already exists
        if (bundleRepo.existsBySpecialistId(specialistId)) {
            throw new IllegalStateException("Specialist already has a bundle");
        }

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found"));

        // Create bundle record
        SpecialistBundle bundle = new SpecialistBundle();
        bundle.setSpecialist(specialist);
        bundle.setTotalKeys(keyCount);
        bundle.setAssignedBy(admin);
        bundle.setNotes(notes);
        bundleRepo.save(bundle);

        // Generate keys
        for (int i = 0; i < keyCount; i++) {
            LicenseKey key = new LicenseKey();
            key.setKeyUuid(UUID.randomUUID().toString());
            key.setSpecialist(specialist);
            keyRepo.save(key);
        }

        // Promote role: SPECIALIST → SPECIALIST_BUNDLE
        specialist.setUserRole(UserRole.SPECIALIST_BUNDLE);
        userRepo.save(specialist);
    }

    /**
     * Revoke bundle: SPECIALIST_BUNDLE → SPECIALIST
     * Deletes all keys and associated profiles
     */
    @Transactional
    public void revokeBundle(String specialistId) {
        User specialist = userRepo.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("Specialist not found"));

        if (specialist.getUserRole() != UserRole.SPECIALIST_BUNDLE) {
            throw new IllegalArgumentException("User does not have a bundle to revoke");
        }

        // Delete all keys and their associated profiles
        List<LicenseKey> keys = keyRepo.findBySpecialistIdOrderByCreatedAtDesc(specialistId);
        for (LicenseKey key : keys) {
            if (key.getProfile() != null) {
                Long profileId = key.getProfile().getId();
                // Delete profile progress
                plsRepo.deleteAllByProfileId(profileId);
                // Delete homework assignments
                homeworkRepo.deleteByProfileId(profileId);
                // Delete profile
                profileRepo.deleteById(profileId);
            }
        }
        
        // Delete all keys
        keyRepo.deleteBySpecialistId(specialistId);

        // Delete bundle record
        bundleRepo.deleteBySpecialistId(specialistId);

        // Demote role: SPECIALIST_BUNDLE → SPECIALIST
        specialist.setUserRole(UserRole.SPECIALIST);
        userRepo.save(specialist);
    }

    /**
     * Toggle premium status for a specialist
     */
    @Transactional
    public void togglePremium(String specialistId, boolean isPremium) {
        User specialist = userRepo.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("Specialist not found"));
        specialist.setIsPremium(isPremium);
        userRepo.save(specialist);
    }
}

