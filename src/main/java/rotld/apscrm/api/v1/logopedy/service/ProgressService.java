package rotld.apscrm.api.v1.logopedy.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.logopedy.dto.ProgressDTO;
import rotld.apscrm.api.v1.logopedy.dto.SummaryDTO;
import rotld.apscrm.api.v1.logopedy.entities.*;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.enums.ProgressStatus;
import rotld.apscrm.api.v1.logopedy.repository.*;
import rotld.apscrm.api.v1.logopedy.entities.Module;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProfileRepo profileRepo;
    private final ProfileProgressRepo progressRepo;
    private final ProfileLessonStatusRepo lessonStatusRepo;
    private final LessonRepo lessonRepo;
    private final SubmoduleRepo submoduleRepo;
    private final ModuleRepo moduleRepo;
    private final LessonScreenRepo screenRepo;
    private final UserRepository userRepository;

    private Profile requireProfile(Long profileId, String userId) {
        return profileRepo.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile with id %s from user with id %s not found.".formatted(profileId, userId)));
    }

    @Transactional(readOnly = true)
    public ProgressDTO current(Long profileId, String userId) {
        Profile p = requireProfile(profileId, userId);
        return progressRepo.findFirstByProfileIdOrderByUpdatedAtDesc(profileId)
                .map(pp -> new ProgressDTO(pp.getModuleId(), pp.getSubmoduleId(), pp.getLessonId(), pp.getScreenIndex(), pp.getStatus()))
                .orElseGet(() -> {
                    // fallback: primul conținut disponibil
                    Module m = moduleRepo.findAllByIsActiveTrueOrderByPositionAsc().get(0);
                    Submodule s = submoduleRepo.findAllByModule_IdOrderByPositionAsc(m.getId()).get(0);
                    var lessons = lessonRepo.findBySubmoduleOrdered(s.getId());
                    if (lessons.isEmpty()) {
                        throw new EntityNotFoundException("No active lessons found");
                    }
                    Lesson l = lessons.get(0);
                    return new ProgressDTO(m.getId(), s.getId(), l.getId(), 0, ProgressStatus.IN_PROGRESS);
                });
    }

    @Transactional
    public ProgressDTO advance(Long profileId, String userId, Long lessonId, Integer screenIndex, boolean done) {
        Profile p = requireProfile(profileId, userId);
        Lesson l = lessonRepo.findById(lessonId).orElseThrow(() -> new EntityNotFoundException("Lesson"));
        Module m = l.getSubmodule().getModule();
        
        // Update user's last activity timestamp for push notification tracking
        userRepository.updateLastActivity(userId, LocalDateTime.now());

        // --- asigură status pe lecția curentă
        var key = new ProfileLessonKey(profileId, lessonId);
        var pls = lessonStatusRepo.findById(key).orElseGet(() -> {
            var x = new ProfileLessonStatus();
            x.setId(key);
            x.setStatus(LessonStatus.IN_PROGRESS);
            x.setStartedAt(Instant.now());
            return x;
        });
        if (pls.getStartedAt() == null) pls.setStartedAt(Instant.now());
        pls.setStatus(LessonStatus.IN_PROGRESS);
        lessonStatusRepo.save(pls);

        // --- calculează next
        var screens = screenRepo.findByLessonIdOrderByPositionAsc(lessonId);
        int nextScreen = (screenIndex == null ? 0 : screenIndex) + 1;

        Long nextLessonId = l.getId();
        Long nextSubId = l.getSubmodule().getId();
        Long nextModId = m.getId();

        boolean endOfLesson = false, endOfSubmodule = false, endOfModule = false;

        if (nextScreen < screens.size() && !done) {
            // rămân pe aceeași lecție, ecranul următor
        } else {
            endOfLesson = true;

            // marchează lecția curentă ca DONE
            pls.setStatus(LessonStatus.DONE);
            pls.setFinishedAt(Instant.now());
            lessonStatusRepo.save(pls);

            // next lesson în submodul
            var lessons = lessonRepo.findBySubmoduleOrdered(l.getSubmodule().getId());
            int idx = IntStream.range(0, lessons.size())
                    .filter(i -> lessons.get(i).getId().equals(lessonId)).findFirst().orElse(-1);

            if (idx >= 0 && idx + 1 < lessons.size()) {
                nextLessonId = lessons.get(idx + 1).getId();
                nextScreen = 0;
            } else {
                endOfSubmodule = true;
                // următorul submodul / modul
                var subs = submoduleRepo.findAllByModule_IdOrderByPositionAsc(m.getId());
                int sidx = IntStream.range(0, subs.size())
                        .filter(i -> subs.get(i).getId().equals(l.getSubmodule().getId())).findFirst().orElse(-1);
                if (sidx >= 0 && sidx + 1 < subs.size()) {
                    var nextSub = subs.get(sidx + 1);
                    nextSubId = nextSub.getId();
                    var nextSubLessons = lessonRepo.findBySubmoduleOrdered(nextSub.getId());
                    if (!nextSubLessons.isEmpty()) {
                        nextLessonId = nextSubLessons.get(0).getId();
                        nextScreen = 0;
                    }
                } else {
                    endOfModule = true;
                    var mods = moduleRepo.findAllByIsActiveTrueOrderByPositionAsc();
                    int midx = IntStream.range(0, mods.size())
                            .filter(i -> mods.get(i).getId().equals(m.getId())).findFirst().orElse(-1);
                    if (midx >= 0 && midx + 1 < mods.size()) {
                        var nm = mods.get(midx + 1);
                        nextModId = nm.getId();
                        var nextSubs = submoduleRepo.findAllByModule_IdOrderByPositionAsc(nm.getId());
                        if (!nextSubs.isEmpty()) {
                            var nsub = nextSubs.get(0);
                            nextSubId = nsub.getId();
                            var nsubLessons = lessonRepo.findBySubmoduleOrdered(nsub.getId());
                            if (!nsubLessons.isEmpty()) {
                                nextLessonId = nsubLessons.get(0).getId();
                                nextScreen = 0;
                            }
                        }
                    }
                }
            }
        }

        // salvează progresul curent (cursor)
        var pp = progressRepo.findFirstByProfileIdOrderByUpdatedAtDesc(profileId)
                .orElseGet(ProfileProgress::new);
        pp.setProfile(p);
        pp.setModuleId(nextModId);
        pp.setSubmoduleId(nextSubId);
        pp.setLessonId(nextLessonId);
        pp.setScreenIndex(nextScreen);
        pp.setStatus(endOfModule ? ProgressStatus.DONE : ProgressStatus.IN_PROGRESS);
        pp.setUpdatedAt(Instant.now());
        progressRepo.save(pp);

        return new ProgressDTO(nextModId, nextSubId, nextLessonId, nextScreen,
                endOfModule ? ProgressStatus.DONE : ProgressStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public SummaryDTO summary(Long profileId) {
        long doneLessons = lessonStatusRepo.countByIdProfileIdAndStatus(profileId, LessonStatus.DONE);
        long totalLessons = lessonRepo.count();
        long completedModules = 0L; // poți calcula: modul e "completat" dacă toate lecțiile sale sunt DONE
        return new SummaryDTO(completedModules, doneLessons, Map.of());
    }
}
