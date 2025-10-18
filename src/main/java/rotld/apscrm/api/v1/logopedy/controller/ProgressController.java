package rotld.apscrm.api.v1.logopedy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.logopedy.dto.ProgressDTO;
import rotld.apscrm.api.v1.logopedy.dto.SummaryDTO;
import rotld.apscrm.api.v1.logopedy.service.ProgressService;
import rotld.apscrm.common.SecurityUtils;

@RestController
@RequestMapping("/api/profiles/{profileId}")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/progress")
    public ProgressDTO current(@PathVariable Long profileId) {
        return progressService.current(profileId, SecurityUtils.currentUserId());
    }

    public static record AdvanceReq(Long lessonId, Integer screenIndex, Boolean done) {}

    @PostMapping("/progress/advance")
    public ProgressDTO advance(@PathVariable Long profileId,
                               @RequestBody AdvanceReq req) {
        return progressService.advance(profileId, SecurityUtils.currentUserId(), req.lessonId(), req.screenIndex(), Boolean.TRUE.equals(req.done()));
    }

    @GetMapping("/summary")
    public SummaryDTO summary(@PathVariable Long profileId) {
        return progressService.summary(profileId);
    }
}