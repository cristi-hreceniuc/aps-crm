package rotld.apscrm.api.v1.profiles;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.profiles.dto.LessonProgressDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCardDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCreateReq;
import rotld.apscrm.api.v1.profiles.service.ProfileService;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;
import rotld.apscrm.common.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepo;
    private final ProfileService profileService;

    @GetMapping
    public List<ProfileCardDTO> myProfiles() {
        User user = userRepo.findById(SecurityUtils.currentUserId()).orElseThrow();
        return profileService.listForUser(user);
    }

    @PostMapping
    public ProfileCardDTO create(@RequestBody @jakarta.validation.Valid ProfileCreateReq req) {
        User user = userRepo.findById(SecurityUtils.currentUserId()).orElseThrow();
        return profileService.createForUser(user, req.name(), req.avatarUri(), req.birthday(), req.gender());
    }

    @GetMapping("/{profileId}/lessons-progress")
    public List<LessonProgressDTO> lessonProgress(@PathVariable Long profileId) {
        return profileService.lessonProgress(profileId, SecurityUtils.currentUserId());
    }
}
