package rotld.apscrm.api.v1.kids.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.kids.dto.HomeworkDTO;
import rotld.apscrm.api.v1.logopedy.entities.*;
import rotld.apscrm.api.v1.logopedy.entities.Module;
import rotld.apscrm.api.v1.logopedy.repository.*;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeworkService {

    private final HomeworkAssignmentRepo homeworkRepo;
    private final ProfileRepo profileRepo;
    private final ModuleRepo moduleRepo;
    private final SubmoduleRepo submoduleRepo;
    private final PartRepo partRepo;
    private final LicenseKeyRepo keyRepo;

    /**
     * Get all homework assignments for a profile
     */
    public List<HomeworkDTO> getHomeworkForProfile(Long profileId) {
        return homeworkRepo.findByProfileIdOrderByAssignedAtDesc(profileId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Assign homework to a profile
     */
    @Transactional
    public HomeworkDTO assignHomework(Long profileId, Long moduleId, Long submoduleId, Long partId,
                                       LocalDate dueDate, String notes, String specialistId) {
        Profile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        // Verify the profile is linked to a key owned by this specialist
        validateProfileOwnership(profileId, specialistId);

        // Validate at least one target is set
        if (moduleId == null && submoduleId == null && partId == null) {
            throw new IllegalArgumentException("At least one of moduleId, submoduleId, or partId must be set");
        }

        HomeworkAssignment homework = new HomeworkAssignment();
        homework.setProfile(profile);
        homework.setDueDate(dueDate);
        homework.setNotes(notes);

        // Set the most specific level
        if (partId != null) {
            Part part = partRepo.findById(partId)
                    .orElseThrow(() -> new EntityNotFoundException("Part not found"));
            homework.setPart(part);
            // Also set parent references for easier querying
            homework.setSubmodule(part.getSubmodule());
            homework.setModule(part.getSubmodule().getModule());
        } else if (submoduleId != null) {
            Submodule submodule = submoduleRepo.findById(submoduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Submodule not found"));
            homework.setSubmodule(submodule);
            homework.setModule(submodule.getModule());
        } else {
            Module module = moduleRepo.findById(moduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Module not found"));
            homework.setModule(module);
        }

        homeworkRepo.save(homework);
        return toDTO(homework);
    }

    /**
     * Remove a homework assignment
     */
    @Transactional
    public void removeHomework(Long homeworkId, String specialistId) {
        HomeworkAssignment homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new EntityNotFoundException("Homework not found"));

        // Verify ownership
        validateProfileOwnership(homework.getProfile().getId(), specialistId);

        homeworkRepo.delete(homework);
    }

    /**
     * Validate that the profile is linked to a key owned by this specialist
     */
    private void validateProfileOwnership(Long profileId, String specialistId) {
        boolean isOwned = keyRepo.findBySpecialistIdOrderByCreatedAtDesc(specialistId).stream()
                .anyMatch(key -> key.getProfile() != null && key.getProfile().getId().equals(profileId));
        
        if (!isOwned) {
            throw new IllegalArgumentException("Profile is not linked to any of your keys");
        }
    }

    private HomeworkDTO toDTO(HomeworkAssignment hw) {
        return new HomeworkDTO(
                hw.getId(),
                hw.getProfile().getId(),
                hw.getModule() != null ? hw.getModule().getId() : null,
                hw.getModule() != null ? hw.getModule().getTitle() : null,
                hw.getSubmodule() != null ? hw.getSubmodule().getId() : null,
                hw.getSubmodule() != null ? hw.getSubmodule().getTitle() : null,
                hw.getPart() != null ? hw.getPart().getId() : null,
                hw.getPart() != null ? hw.getPart().getName() : null,
                hw.getAssignedAt(),
                hw.getDueDate(),
                hw.getNotes()
        );
    }
}

