package rotld.apscrm.api.v1.kids.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kids.dto.AssignHomeworkRequest;
import rotld.apscrm.api.v1.kids.dto.HomeworkDTO;
import rotld.apscrm.api.v1.kids.service.HomeworkService;
import rotld.apscrm.api.v1.user.repository.User;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SPECIALIST_BUNDLE')")
public class HomeworkController {

    private final HomeworkService homeworkService;

    /**
     * Get homework assignments for a profile
     */
    @GetMapping("/profiles/{profileId}/homework")
    public ResponseEntity<List<HomeworkDTO>> getHomework(@PathVariable Long profileId) {
        return ResponseEntity.ok(homeworkService.getHomeworkForProfile(profileId));
    }

    /**
     * Assign homework to a profile
     */
    @PostMapping("/profiles/{profileId}/homework")
    public ResponseEntity<HomeworkDTO> assignHomework(
            @PathVariable Long profileId,
            @RequestBody AssignHomeworkRequest request,
            @AuthenticationPrincipal User specialist) {
        HomeworkDTO homework = homeworkService.assignHomework(
                profileId,
                request.moduleId(),
                request.submoduleId(),
                request.partId(),
                request.dueDate(),
                request.notes(),
                specialist.getId()
        );
        return ResponseEntity.ok(homework);
    }

    /**
     * Remove a homework assignment
     */
    @DeleteMapping("/homework/{homeworkId}")
    public ResponseEntity<Void> removeHomework(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal User specialist) {
        homeworkService.removeHomework(homeworkId, specialist.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Mark a homework assignment as DONE/CLOSED by specialist (archives it; not deleted).
     */
    @PostMapping("/homework/{homeworkId}/done")
    public ResponseEntity<HomeworkDTO> markHomeworkDone(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal User specialist) {
        return ResponseEntity.ok(homeworkService.markAsDoneBySpecialist(homeworkId, specialist.getId()));
    }
}

