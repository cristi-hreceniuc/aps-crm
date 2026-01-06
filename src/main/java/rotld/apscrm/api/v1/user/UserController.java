package rotld.apscrm.api.v1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rotld.apscrm.api.v1.logopedy.service.S3Service;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.mapper.UserMapper;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.service.UserService;
import rotld.apscrm.common.SecurityUtils;

import rotld.apscrm.api.v1.user.dto.UserRole;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api/v1/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final S3Service s3Service;

    @GetMapping("/me")
    public UserResponseDto authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UserMapper.toDto((User) authentication.getPrincipal(), s3Service);
    }

    @GetMapping
    public List<UserResponseDto> allUsers() {
        return userService.allUsers().stream()
                .map(user -> UserMapper.toDto(user, s3Service))
                .toList();
    }

    @GetMapping("/search")
    public Page<UserResponseDto> search(@RequestParam(name="q", required=false) String q, Pageable pageable){
        return userService.search(pageable, q);
    }

    /**
     * Search only web users (ADMIN, VOLUNTEER) - for CRM settings page
     */
    @GetMapping("/web/search")
    public Page<UserResponseDto> searchWebUsers(@RequestParam(name="q", required=false) String q, Pageable pageable){
        return userService.searchByRoles(pageable, q, List.of(UserRole.ADMIN, UserRole.VOLUNTEER));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestBody Map<String,String> body){
        userService.setStatus(id, body.get("status"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/premium")
    public ResponseEntity<Void> updatePremium(@PathVariable UUID id, @RequestBody Map<String,Boolean> body){
        Boolean p = body.get("premium");
        userService.setPremium(id, p);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            String userId = SecurityUtils.currentUserId();
            String s3Key = userService.uploadProfileImage(userId, file);
            return ResponseEntity.ok(Map.of("s3Key", s3Key, "message", "Profile image uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}