package rotld.apscrm.api.v1.health;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public String checkHealth() {
        log.debug("Getting current user.");
        return "OK - asociatia pentru suflet.";
    }
}