package rotld.apscrm.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = org.springframework.http.HttpStatus.FORBIDDEN)
public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException() { super("PREMIUM_REQUIRED"); }
}
