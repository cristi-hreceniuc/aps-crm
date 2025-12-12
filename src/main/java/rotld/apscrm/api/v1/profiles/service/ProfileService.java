package rotld.apscrm.api.v1.profiles.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rotld.apscrm.api.v1.logopedy.entities.Lesson;
import rotld.apscrm.api.v1.logopedy.entities.Profile;
import rotld.apscrm.api.v1.logopedy.enums.LessonStatus;
import rotld.apscrm.api.v1.logopedy.repository.LessonRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileLessonStatusRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileProgressRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileRepo;
import rotld.apscrm.api.v1.logopedy.service.S3Service;
import rotld.apscrm.api.v1.profiles.dto.LessonProgressDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCardDTO;
import rotld.apscrm.api.v1.user.repository.User;

import java.io.IOException;
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
    private final ProfileProgressRepo profileProgressRepo;
    private final S3Service s3Service;

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
                    
                    // Convert S3 key to presigned URL if it exists
                    String avatarUrl = p.getAvatarUri();
                    if (avatarUrl != null && !avatarUrl.isEmpty() && s3Service.isS3Key(avatarUrl)) {
                        avatarUrl = s3Service.generatePresignedUrl(avatarUrl);
                    }
                    
                    return new ProfileCardDTO(
                            p.getId(), p.getName(), avatarUrl,
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

        // Convert S3 key to presigned URL if it exists
        String avatarUrl = p.getAvatarUri();
        if (avatarUrl != null && !avatarUrl.isEmpty() && s3Service.isS3Key(avatarUrl)) {
            avatarUrl = s3Service.generatePresignedUrl(avatarUrl);
        }

        long totalLessons = lessonRepo.countActive();
        return new ProfileCardDTO(p.getId(), p.getName(), avatarUrl,
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

    @Transactional
    public void delete(Long profileId, String userId) {
        Profile profile = requireOwnedProfile(profileId, userId);
        
        // Delete all related ProfileLessonStatus records directly by profile ID
        plsRepo.deleteAllByProfileId(profileId);
        
        // Delete all related ProfileProgress records directly by profile ID
        profileProgressRepo.deleteAllByProfileId(profileId);
        
        // Finally, delete the profile itself
        profileRepo.delete(profile);
    }

    @Transactional
    public String uploadProfileAvatar(Long profileId, String userId, MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Validate file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        // Check ownership
        Profile profile = requireOwnedProfile(profileId, userId);

        // Upload to S3
        String s3Key = s3Service.uploadFile(
                file.getInputStream(),
                contentType,
                file.getSize(),
                "profile-avatars",
                file.getOriginalFilename()
        );

        // Update profile record
        profile.setAvatarUri(s3Key);
        profileRepo.save(profile);

        // Return the S3 key
        return s3Key;
    }
}
