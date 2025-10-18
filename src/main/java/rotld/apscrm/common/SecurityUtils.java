package rotld.apscrm.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import rotld.apscrm.api.v1.user.repository.User;

public final class SecurityUtils {
    public static String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) (authentication.getPrincipal());
        return principal.getId();
    }
}
