package rotld.apscrm.api.v1.profiles;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rotld.apscrm.api.v1.profiles.dto.LessonProgressDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCardDTO;
import rotld.apscrm.api.v1.profiles.dto.ProfileCreateReq;
import rotld.apscrm.api.v1.profiles.service.ProfileService;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;
import rotld.apscrm.common.SecurityUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

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
    public ProfileCardDTO create(@RequestBody ProfileCreateReq req) {
        User user = userRepo.findById(SecurityUtils.currentUserId()).orElseThrow();
        LocalDate birthday = parseBirthday(req.birthday());
        return profileService.createForUser(user, req.name(), req.avatarUri(), birthday, req.gender());
    }

    private LocalDate parseBirthday(String birthdayString) {
        if (birthdayString == null || birthdayString.trim().isEmpty()) {
            throw new IllegalArgumentException("Birthday cannot be null or empty");
        }

        String trimmed = birthdayString.trim();
        
        // Try to parse as LocalDateTime first (handles formats like "2020-10-10T00:00:00")
        // This will extract only the date part from datetime strings
        try {
            LocalDateTime dateTime = LocalDateTime.parse(trimmed);
            return dateTime.toLocalDate();
        } catch (DateTimeParseException e) {
            // Try with ISO_LOCAL_DATE_TIME formatter (handles formats with or without seconds)
            try {
                LocalDateTime dateTime = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.toLocalDate();
            } catch (DateTimeParseException e1) {
                // If that fails, try parsing as LocalDate directly (ISO date format)
                try {
                    return LocalDate.parse(trimmed);
                } catch (DateTimeParseException e2) {
                    // Try with common date formats
                    String[] patterns = {
                        "yyyy-MM-dd",
                        "dd/MM/yyyy",
                        "MM/dd/yyyy",
                        "yyyy/MM/dd"
                    };
                    
                    for (String pattern : patterns) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                            return LocalDate.parse(trimmed, formatter);
                        } catch (DateTimeParseException ignored) {
                            // Continue to next pattern
                        }
                    }
                    
                    // If all parsing attempts fail, throw an exception
                    throw new IllegalArgumentException("Unable to parse birthday: " + birthdayString + 
                        ". Supported formats: ISO date-time (e.g., 2020-10-10T00:00:00), ISO date (e.g., 2020-10-10), or dd/MM/yyyy");
                }
            }
        }
    }

    @GetMapping("/{profileId}/lessons-progress")
    public List<LessonProgressDTO> lessonProgress(@PathVariable Long profileId) {
        return profileService.lessonProgress(profileId, SecurityUtils.currentUserId());
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> delete(@PathVariable Long profileId) {
        profileService.delete(profileId, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{profileId}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long profileId,
            @RequestParam("file") MultipartFile file) {
        try {
            String userId = SecurityUtils.currentUserId();
            String s3Key = profileService.uploadProfileAvatar(profileId, userId, file);
            return ResponseEntity.ok(Map.of("s3Key", s3Key, "message", "Avatar uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
