package rotld.apscrm.api.v1.kpi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.kpi.dto.KpiResponseDto;
import rotld.apscrm.api.v1.kpi.service.KpiService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.kpi.dto.KpiResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kpi")
public class KpiController {

    private final KpiService service;

    @GetMapping
    public KpiResponseDto get() {
        return service.getAll();
    }
}