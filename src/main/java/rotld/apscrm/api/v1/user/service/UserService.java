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
import rotld.apscrm.api.v1.auth.repository.RefreshTokenRepository;
import rotld.apscrm.api.v1.logopedy.repository.ProfileLessonStatusRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileProgressRepo;
import rotld.apscrm.api.v1.logopedy.repository.ProfileRepo;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.mapper.UserMapper;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.repository.UserRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileRepo profileRepo;
    private final ProfileLessonStatusRepo profileLessonStatusRepo;
    private final ProfileProgressRepo profileProgressRepo;

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
        return page.map(UserMapper::toDto);
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
        if (userRepository.updatePremium(id.toString(), premium) == 0)
            throw new IllegalArgumentException("User not found: " + id);
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
}