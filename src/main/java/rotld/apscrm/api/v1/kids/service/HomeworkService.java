package rotld.apscrm.api.v1.kids.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.kids.dto.HomeworkDTO;
import rotld.apscrm.api.v1.logopedy.entities.*;
import rotld.apscrm.api.v1.logopedy.entities.Module;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.repository.*;
import rotld.apscrm.api.v1.notification.service.PushNotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LessonRepo lessonRepo;
    private final ProfileLessonStatusRepo profileLessonStatusRepo;
    private final PushNotificationService pushNotificationService;

    /**
     * Get all homework assignments for a profile
     */
    public List<HomeworkDTO> getHomeworkForProfile(Long profileId) {
        return homeworkRepo.findByProfileIdAndSpecialistDoneAtIsNullOrderByAssignedAtDesc(profileId).stream()
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
     * Mark homework as completed (for kids)
     */
    @Transactional
    public HomeworkDTO markAsComplete(Long homeworkId, Long profileId) {
        HomeworkAssignment homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new EntityNotFoundException("Homework not found"));

        // Verify the homework belongs to this profile
        if (!homework.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Homework does not belong to this profile");
        }

        // If specialist already closed it, kid cannot change it anymore.
        if (homework.getSpecialistDoneAt() != null) {
            throw new IllegalStateException("Homework already closed by specialist");
        }

        // Idempotent: if already completed by kid, keep existing timestamp.
        if (homework.getCompletedAt() == null) {
            homework.setCompletedAt(LocalDateTime.now());
        }
        homeworkRepo.save(homework);

        // Notify the specialist (profile owner) that the kid marked homework as complete
        try {
            Profile profile = homework.getProfile();
            String specialistUserId = profile.getUser().getId();
            String title = "Temă completată";
            String body = profile.getName() + " a marcat o temă ca fiind completă.";
            pushNotificationService.sendToUser(
                    specialistUserId,
                    title,
                    body,
                    java.util.Map.of(
                            "type", "homework_completed",
                            "profileId", String.valueOf(profile.getId()),
                            "homeworkId", String.valueOf(homework.getId())
                    )
            );
        } catch (Exception ignored) {
            // Notifications are best-effort; do not block completion if push fails.
        }

        return toDTO(homework);
    }

    /**
     * Unmark homework as completed (for kids or specialist)
     */
    @Transactional
    public HomeworkDTO markAsIncomplete(Long homeworkId, Long profileId) {
        HomeworkAssignment homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new EntityNotFoundException("Homework not found"));

        // Verify the homework belongs to this profile
        if (!homework.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Homework does not belong to this profile");
        }

        // Once the kid marked it complete, kid should not be able to reopen it.
        // (Specialist can still close it or remove it via specialist actions.)
        if (homework.getCompletedAt() != null) {
            throw new IllegalStateException("Homework already marked complete by kid");
        }

        homework.setCompletedAt(null);
        homeworkRepo.save(homework);
        return toDTO(homework);
    }

    /**
     * Specialist closes homework (archives it). After this it disappears from active lists.
     */
    @Transactional
    public HomeworkDTO markAsDoneBySpecialist(Long homeworkId, String specialistId) {
        HomeworkAssignment homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new EntityNotFoundException("Homework not found"));

        validateProfileOwnership(homework.getProfile().getId(), specialistId);

        if (homework.getSpecialistDoneAt() == null) {
            homework.setSpecialistDoneAt(LocalDateTime.now());
            homeworkRepo.save(homework);
        }
        return toDTO(homework);
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

    /**
     * Calculate progress for a homework assignment
     */
    private int[] calculateProgress(HomeworkAssignment hw) {
        Long profileId = hw.getProfile().getId();
        List<Lesson> lessons;

        // Get lessons based on homework level
        if (hw.getPart() != null) {
            lessons = lessonRepo.findByPartIdAndIsActiveTrue(hw.getPart().getId());
        } else if (hw.getSubmodule() != null) {
            lessons = lessonRepo.findBySubmoduleIdAndIsActiveTrue(hw.getSubmodule().getId());
        } else if (hw.getModule() != null) {
            // Get all lessons in all submodules of this module
            List<Long> submoduleIds = submoduleRepo.findByModuleId(hw.getModule().getId()).stream()
                    .map(Submodule::getId)
                    .toList();
            lessons = lessonRepo.findBySubmoduleIdInAndIsActiveTrue(submoduleIds);
        } else {
            return new int[]{0, 0};
        }

        int total = lessons.size();
        if (total == 0) return new int[]{0, 0};

        // Get completed lessons for this profile
        Set<Long> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        long completed = profileLessonStatusRepo.countByProfileIdAndLessonIdInAndStatus(
                profileId, lessonIds, LessonStatus.DONE);

        return new int[]{total, (int) completed};
    }

    private HomeworkDTO toDTO(HomeworkAssignment hw) {
        int[] progress = calculateProgress(hw);
        
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
                hw.getNotes(),
                progress[0], // totalLessons
                progress[1], // completedLessons
                hw.getCompletedAt(),
                hw.getSpecialistDoneAt()
        );
    }
}

