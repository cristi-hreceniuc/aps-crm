package rotld.apscrm.api.v1.kids.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kids.dto.HomeworkDTO;
import rotld.apscrm.api.v1.kids.service.HomeworkService;
import rotld.apscrm.api.v1.logopedy.dto.*;
import rotld.apscrm.api.v1.logopedy.service.ContentService;
import rotld.apscrm.api.v1.logopedy.service.ProgressService;
import rotld.apscrm.config.JwtAuthenticationFilter.KidPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kid")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('KID')")
public class KidContentController {

    private final ContentService contentService;
    private final ProgressService progressService;
    private final HomeworkService homeworkService;

    /**
     * Get all children modules (non-specialist content)
     * Premium content is filtered based on the specialist's is_premium flag
     */
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDTO>> listModules(@AuthenticationPrincipal KidPrincipal kid) {
        // Pass null for targetAudience to get children content
        // The premium flag determines if premium modules are accessible
        return ResponseEntity.ok(contentService.listModulesForKid(kid.getProfileId(), kid.isPremium()));
    }

    /**
     * Get a specific module
     */
    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<ModuleDTO> getModule(
            @PathVariable Long moduleId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(contentService.getModuleForKid(kid.getProfileId(), moduleId, kid.isPremium()));
    }

    /**
     * Get a submodule with its parts
     */
    @GetMapping("/submodules/{submoduleId}")
    public ResponseEntity<SubmoduleListDTO> getSubmodule(
            @PathVariable Long submoduleId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(contentService.getSubmoduleForKid(kid.getProfileId(), submoduleId, kid.isPremium()));
    }

    /**
     * Get a part with its lessons
     */
    @GetMapping("/parts/{partId}")
    public ResponseEntity<PartDTO> getPart(
            @PathVariable Long partId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(contentService.getPartForKid(kid.getProfileId(), partId, kid.isPremium()));
    }

    /**
     * Get a lesson for playing
     */
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonPlayDTO> getLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(contentService.getLessonForKid(kid.getProfileId(), lessonId, kid.isPremium()));
    }

    /**
     * Get all homework assignments for this kid
     */
    @GetMapping("/homework")
    public ResponseEntity<List<HomeworkDTO>> getHomework(@AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(homeworkService.getHomeworkForProfile(kid.getProfileId()));
    }

    /**
     * Mark homework as complete
     */
    @PostMapping("/homework/{homeworkId}/complete")
    public ResponseEntity<HomeworkDTO> markHomeworkComplete(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(homeworkService.markAsComplete(homeworkId, kid.getProfileId()));
    }

    /**
     * Mark homework as incomplete
     */
    @PostMapping("/homework/{homeworkId}/incomplete")
    public ResponseEntity<HomeworkDTO> markHomeworkIncomplete(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal KidPrincipal kid) {
        return ResponseEntity.ok(homeworkService.markAsIncomplete(homeworkId, kid.getProfileId()));
    }

    /**
     * Get current progress
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressDTO> getProgress(@AuthenticationPrincipal KidPrincipal kid) {
        // For kids, we pass the specialistId as the userId since their profile is owned by the specialist
        return ResponseEntity.ok(progressService.current(kid.getProfileId(), kid.getSpecialistId()));
    }

    /**
     * Advance progress (complete a screen or lesson)
     */
    @PostMapping("/progress")
    public ResponseEntity<ProgressDTO> advanceProgress(
            @RequestBody AdvanceRequest request,
            @AuthenticationPrincipal KidPrincipal kid) {
        // For kids, we pass the specialistId as the userId since they don't have their own user account
        return ResponseEntity.ok(progressService.advance(
                kid.getProfileId(),
                kid.getSpecialistId(),
                request.lessonId(),
                request.screenIndex(),
                request.done()
        ));
    }

    public record AdvanceRequest(Long lessonId, Integer screenIndex, boolean done) {}
}

