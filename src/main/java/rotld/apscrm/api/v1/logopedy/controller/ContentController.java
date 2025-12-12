package rotld.apscrm.api.v1.logopedy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.logopedy.dto.*;
import rotld.apscrm.api.v1.logopedy.service.ContentService;
import rotld.apscrm.common.SecurityUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // userId îl iei din JWT; aici îl primim pentru simplitate
    @GetMapping("/profiles/{profileId}/modules")
    public List<ModuleDTO> listModules(
            @PathVariable Long profileId,
            @RequestParam(required = false) String targetAudience) {
        return contentService.listModules(profileId, SecurityUtils.currentUserId(), targetAudience);
    }

    @GetMapping("/profiles/{profileId}/modules/{moduleId}")
    public ModuleDTO getModule(@PathVariable Long profileId, @PathVariable Long moduleId) {
        return contentService.getModule(profileId, SecurityUtils.currentUserId(), moduleId);
    }

    @GetMapping("/profiles/{profileId}/submodules/{submoduleId}")
    public SubmoduleListDTO getSubmodule(@PathVariable Long profileId, @PathVariable Long submoduleId) {
        return contentService.getSubmodule(profileId, SecurityUtils.currentUserId(), submoduleId);
    }
    
    @GetMapping("/profiles/{profileId}/parts/{partId}")
    public PartDTO getPart(@PathVariable Long profileId, @PathVariable Long partId) {
        return contentService.getPart(profileId, SecurityUtils.currentUserId(), partId);
    }

    @GetMapping("/profiles/{profileId}/lessons/{lessonId}")
    public LessonPlayDTO getLesson(
            @PathVariable Long profileId,
            @PathVariable Long lessonId,
            @RequestParam(required = false, defaultValue = "false") boolean skipAssetUrls) {
        return contentService.getLesson(profileId, SecurityUtils.currentUserId(), lessonId, skipAssetUrls);
    }

    /**
     * Get all assets for a submodule for offline caching
     * Returns S3 keys (not presigned URLs) to allow mobile app to download and cache
     */
    @GetMapping("/profiles/{profileId}/submodules/{submoduleId}/assets")
    public SubmoduleAssetsDTO getSubmoduleAssets(
            @PathVariable Long profileId,
            @PathVariable Long submoduleId) {
        return contentService.getSubmoduleAssets(profileId, SecurityUtils.currentUserId(), submoduleId);
    }

    /**
     * Generate a presigned URL for an S3 asset
     * Used by mobile app for offline asset downloading
     */
    @PostMapping("/profiles/{profileId}/assets/presign")
    public Map<String, String> generatePresignedUrl(
            @PathVariable Long profileId,
            @RequestBody Map<String, String> request) {
        String s3Key = request.get("s3Key");
        String presignedUrl = contentService.generatePresignedUrl(profileId, SecurityUtils.currentUserId(), s3Key);
        return Map.of("url", presignedUrl);
    }
}