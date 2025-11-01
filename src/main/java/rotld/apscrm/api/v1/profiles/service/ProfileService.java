package rotld.apscrm.api.v1.profiles.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.logopedy.entities.Lesson;
import rotld.apscrm.api.v1.logopedy.entities.Profile;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.repository.LessonRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileLessonStatusRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileRepo;
import rotld.apscrm.api.v1.profiles.dto.LessonProgressDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCardDTO;
import rotld.apscrm.api.v1.user.repository.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepo profileRepo;
    private final LessonRepo lessonRepo;
    private final ProfileLessonStatusRepo plsRepo;

    private Profile requireOwnedProfile(Long profileId, String userId) {
        return profileRepo.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
    }

    public List<ProfileCardDTO> listForUser(User user) {
        long totalLessons = lessonRepo.countActive(); // toate lecțiile active
        return profileRepo.findAllByUserId(user.getId())
                .stream()
                .map(p -> {
                    long done = plsRepo.completedCount(p.getId());
                    int percent = totalLessons == 0 ? 0 : (int)Math.round(done * 100.0 / totalLessons);
                    return new ProfileCardDTO(
                            p.getId(), p.getName(), p.getAvatarUri(),
                            user.getIsPremium(), percent, done, totalLessons,
                            p.getBirthday(), p.getGender()
                    );
                }).toList();
    }

    @Transactional
    public ProfileCardDTO createForUser(User user, String name, String avatarUri, LocalDate birthday, String gender) {
        Profile p = new Profile();
        p.setUser(user);
        p.setName(name);
        p.setAvatarUri(avatarUri);
        p.setBirthday(birthday);
        p.setGender(gender);
        profileRepo.save(p);

        long totalLessons = lessonRepo.countActive();
        return new ProfileCardDTO(p.getId(), p.getName(), p.getAvatarUri(),
                user.getIsPremium(), 0, 0, totalLessons,
                p.getBirthday(), p.getGender());
    }

    public List<LessonProgressDTO> lessonProgress(Long profileId, String userId) {
        var profile = requireOwnedProfile(profileId, userId);

        // map status by lessonId
        var statuses = plsRepo.findAllByProfileId(profile.getId())
                .stream().collect(java.util.stream.Collectors.toMap(
                        s -> s.getId().getLessonId(),
                        s -> s.getStatus().name()
                ));

        var lessons = lessonRepo.findAllActiveOrdered();
        List<LessonProgressDTO> list = new ArrayList<>();
        for (Lesson l : lessons) {
            var s = l.getSubmodule(); var m = s.getModule();
            String st = statuses.getOrDefault(l.getId(), LessonStatus.LOCKED.name());
            list.add(new LessonProgressDTO(
                    m.getId(), m.getTitle(),
                    s.getId(), s.getTitle(),
                    l.getId(), l.getTitle(),
                    st
            ));
        }
        return list;
    }
}
