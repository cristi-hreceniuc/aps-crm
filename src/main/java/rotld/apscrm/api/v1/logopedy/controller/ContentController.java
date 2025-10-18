package rotld.apscrm.api.v1.logopedy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.logopedy.dto.*;
import rotld.apscrm.api.v1.logopedy.service.ContentService;
import rotld.apscrm.common.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // userId îl iei din JWT; aici îl primim pentru simplitate
    @GetMapping("/profiles/{profileId}/modules")
    public List<ModuleDTO> listModules(@PathVariable Long profileId) {
        return contentService.listModules(profileId, SecurityUtils.currentUserId());
    }

    @GetMapping("/profiles/{profileId}/modules/{moduleId}")
    public ModuleDTO getModule(@PathVariable Long profileId, @PathVariable Long moduleId) {
        return contentService.getModule(profileId, SecurityUtils.currentUserId(), moduleId);
    }

    @GetMapping("/profiles/{profileId}/submodules/{submoduleId}")
    public SubmoduleListDTO getSubmodule(@PathVariable Long profileId, @PathVariable Long submoduleId) {
        var lessons = contentService.submoduleLessonsWithProgress(profileId, submoduleId, SecurityUtils.currentUserId());

        return contentService.getSubmodule(profileId, SecurityUtils.currentUserId(), submoduleId).toBuilder()
                .lessons(lessons)
                .build();
    }

    @GetMapping("/profiles/{profileId}/lessons/{lessonId}")
    public LessonPlayDTO getLesson(@PathVariable Long profileId, @PathVariable Long lessonId) {
        return contentService.getLesson(profileId, SecurityUtils.currentUserId(), lessonId);
    }
}