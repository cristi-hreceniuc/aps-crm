package rotld.apscrm.api.v1.kids.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.kids.dto.LicenseKeyDTO;
import rotld.apscrm.api.v1.logopedy.entities.LicenseKey;
import rotld.apscrm.api.v1.logopedy.entities.Profile;
import rotld.apscrm.api.v1.logopedy.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LicenseKeyService {

    private final LicenseKeyRepo keyRepo;
    private final ProfileRepo profileRepo;
    private final ProfileLessonStatusRepo plsRepo;
    private final ProfileProgressRepo progressRepo;
    private final HomeworkAssignmentRepo homeworkRepo;

    /**
     * List all keys for a specialist
     */
    public List<LicenseKeyDTO> listKeys(String specialistId) {
        return keyRepo.findBySpecialistIdOrderByCreatedAtDesc(specialistId).stream()
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
    }

    /**
     * Activate a key by linking it to a profile
     */
    @Transactional
    public void activateKey(Long keyId, Long profileId, String specialistId) {
        LicenseKey key = keyRepo.findById(keyId)
                .orElseThrow(() -> new EntityNotFoundException("Key not found"));

        validateOwnership(key, specialistId);

        if (!key.isActive()) {
            throw new IllegalStateException("Key is not active");
        }

        if (key.getProfile() != null) {
            throw new IllegalStateException("Key is already activated");
        }

        Profile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        // Verify profile belongs to the specialist
        if (!profile.getUser().getId().equals(specialistId)) {
            throw new IllegalArgumentException("Profile does not belong to this specialist");
        }

        // Check if profile is already linked to another key
        if (keyRepo.existsByProfileId(profileId)) {
            throw new IllegalStateException("Profile is already linked to a key");
        }

        key.setProfile(profile);
        key.setActivatedAt(LocalDateTime.now());
        keyRepo.save(key);
    }

    /**
     * Reset a key: delete profile + progress, unlink key (key becomes reusable)
     */
    @Transactional
    public void resetKey(Long keyId, String specialistId) {
        LicenseKey key = keyRepo.findById(keyId)
                .orElseThrow(() -> new EntityNotFoundException("Key not found"));

        validateOwnership(key, specialistId);

        if (key.getProfile() != null) {
            Long profileId = key.getProfile().getId();

            // Delete profile progress
            plsRepo.deleteAllByProfileId(profileId);
            progressRepo.deleteAllByProfileId(profileId);
            
            // Delete homework assignments
            homeworkRepo.deleteByProfileId(profileId);

            // Unlink key from profile
            key.setProfile(null);
            key.setActivatedAt(null);
            keyRepo.save(key);

            // Delete the profile
            profileRepo.deleteById(profileId);
        }
    }

    /**
     * Get key stats for a specialist
     */
    public KeyStats getKeyStats(String specialistId) {
        long total = keyRepo.countBySpecialistId(specialistId);
        long used = keyRepo.countBySpecialistIdAndProfileIsNotNull(specialistId);
        return new KeyStats(total, used);
    }

    private void validateOwnership(LicenseKey key, String specialistId) {
        if (!key.getSpecialist().getId().equals(specialistId)) {
            throw new IllegalArgumentException("Key does not belong to this specialist");
        }
    }

    public record KeyStats(long total, long used) {}
}

