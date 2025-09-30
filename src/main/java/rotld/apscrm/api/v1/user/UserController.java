package rotld.apscrm.api.v1.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.user.dto.UserResponseDto;
import rotld.apscrm.api.v1.user.mapper.UserMapper;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.api.v1.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api/v1/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDto authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UserMapper.toDto((User) authentication.getPrincipal());
    }

    @GetMapping
    public List<UserResponseDto> allUsers() {
        return userService.allUsers().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @GetMapping("/search")
    public Page<UserResponseDto> search(@RequestParam(name="q", required=false) String q, Pageable pageable){
        return userService.search(pageable, q);
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
}