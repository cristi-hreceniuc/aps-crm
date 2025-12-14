package rotld.apscrm.api.v1.user.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rotld.apscrm.api.v1.auth.repository.RefreshTokenRepository;
import rotld.apscrm.api.v1.logopedy.repository.ProfileLessonStatusRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileProgressRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileRepo;
import rotld.apscrm.api.v1.logopedy.service.S3Service;
import rotld.apscrm.api.v1.notification.service.PushNotificationService;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.mapper.UserMapper;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileRepo profileRepo;
    private final ProfileLessonStatusRepo profileLessonStatusRepo;
    private final ProfileProgressRepo profileProgressRepo;
    private final S3Service s3Service;
    private final PushNotificationService pushNotificationService;

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email <%s> not found.".formatted(email)));
    }

    private static final Map<String, String> SORT_MAP = Map.ofEntries(
            Map.entry("firstName", "firstName"),
            Map.entry("lastName", "lastName"),
            Map.entry("email", "email"),
            Map.entry("gender", "gender"),
            Map.entry("role", "userRole"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("status", "userStatus"),
            Map.entry("isPremium", "isPremium")
    );
    private static final Set<String> ALLOWED = Set.of(
            "firstName", "lastName", "email", "createdAt", "userStatus", "isPremium", "gender", "userRole"
    );

    private Pageable remap(Pageable p) {
        if (p == null || p.getSort() == null || p.getSort().isUnsorted()) return p;
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : p.getSort()) {
            String k = SORT_MAP.getOrDefault(o.getProperty(), o.getProperty());
            if (!ALLOWED.contains(k)) continue;
            orders.add(new Sort.Order(o.getDirection(), k, o.getNullHandling()));
        }
        return orders.isEmpty() ? p : PageRequest.of(p.getPageNumber(), p.getPageSize(), Sort.by(orders));
    }

    private Specification<User> spec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = "%" + q.trim().toLowerCase() + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("firstName")), term));
            ors.add(cb.like(cb.lower(root.get("lastName")), term));
            ors.add(cb.like(cb.lower(root.get("email")), term));
            ors.add(cb.like(cb.lower(root.get("userStatus")), term));
            ors.add(cb.like(cb.lower(root.get("userRole")), term));
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    public Page<UserResponseDto> search(Pageable pageable, String q) {
        Page<User> page = userRepository.findAll(spec(q), remap(pageable));
        return page.map(user -> UserMapper.toDto(user, s3Service));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @Transactional
    public void setStatus(UUID id, String status) {
        String S = status == null ? "PENDING" : status.toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING", "ACTIVE", "INACTIVE").contains(S))
            throw new IllegalArgumentException("Invalid status: " + status);
        if (userRepository.updateStatus(id.toString(), S) == 0) throw new IllegalArgumentException("User not found: " + id);
    }

    @Transactional
    public void setPremium(UUID id, boolean premium) {
        String userId = id.toString();
        if (userRepository.updatePremium(userId, premium) == 0)
            throw new IllegalArgumentException("User not found: " + id);
        
        // Send push notification when premium is granted
        if (premium) {
            pushNotificationService.sendPremiumGranted(userId);
        }
    }

    @Transactional
    public void delete(UUID id) {
        String userId = id.toString();
        
        // Check if user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        
        // Delete refresh tokens for this user
        refreshTokenRepository.deleteByUserId(userId);
        
        // Delete all profiles for this user (and their related data)
        var profiles = profileRepo.findAllByUserId(userId);
        for (var profile : profiles) {
            // Delete profile lesson status records
            profileLessonStatusRepo.deleteAllByProfileId(profile.getId());
            // Delete profile progress records
            profileProgressRepo.deleteAllByProfileId(profile.getId());
            // Delete the profile itself
            profileRepo.delete(profile);
        }
        
        // Finally, delete the user
        if (userRepository.hardDelete(userId) == 0) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    @Transactional
    public String uploadProfileImage(String userId, MultipartFile file) throws IOException {
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

        // Upload to S3
        String s3Key = s3Service.uploadFile(
                file.getInputStream(),
                contentType,
                file.getSize(),
                "user-profile-images",
                file.getOriginalFilename()
        );

        // Update user record
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setProfileImageUrl(s3Key);
        userRepository.save(user);

        // Return the S3 key
        return s3Key;
    }
}