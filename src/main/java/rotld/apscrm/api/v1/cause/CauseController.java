package rotld.apscrm.api.v1.cause;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.cause.repository.Cause;
import rotld.apscrm.api.v1.cause.service.CauseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cause")
public class CauseController {

    private final CauseService service;

    @GetMapping
    public Page<Cause> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.page(q, pageable);
    }
}