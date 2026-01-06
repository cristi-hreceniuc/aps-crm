package rotld.apscrm.api.v1.borderou;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.borderou.dto.CrmSettingResponseDto;
import rotld.apscrm.api.v1.borderou.mapper.TypeCoercer;
import rotld.apscrm.api.v1.borderou.repository.CrmSetting;
import rotld.apscrm.api.v1.borderou.repository.CrmSettingRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class CrmSettingController {
    private final CrmSettingRepository repo;
    private static final String REQUIRE_ACTIVE_FOR_LOGIN = "require_active_for_login";

    public CrmSettingController(CrmSettingRepository repo) {
        this.repo = repo;
    }

    // doar setările folosite în XML 230; schimbă lista dacă ai altele
    private static final List<String> XML_KEYS = List.of(
            "xmlns", "schemaLocation", "xml_luna", "xml_an",
            "xml_nume", "xml_cui", "xml_cif", "form230_vizibilitate"
    );

    @GetMapping
    public List<CrmSettingResponseDto> listAll() {
        return repo.findAllByOrderByNameAsc().stream().map(CrmSettingResponseDto::of).toList();
    }

    // dacă vrei numai pe cele de XML:
    @GetMapping("/xml")
    public List<CrmSettingResponseDto> listXmlOnly() {
        return repo.findByNameInOrderByNameAsc(XML_KEYS).stream().map(CrmSettingResponseDto::of).toList();
    }

    // Update value
    @PutMapping("/{id}")
    public ResponseEntity<?> updateValue(@PathVariable Integer id, @RequestBody CrmSettingResponseDto body) {
        var s = repo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();

        if (body.value() == null) return ResponseEntity.badRequest().body("Missing value");
        // coerce/validate by type
        try {
            String coerced = TypeCoercer.coerce(s.getType(), body.value());
            s.setValue(coerced);
            repo.save(s);
            return ResponseEntity.ok(CrmSettingResponseDto.of(s));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(iae.getMessage());
        }
    }

    // Reset = copy default_value -> value
    @PostMapping("/{id}/reset")
    public ResponseEntity<?> reset(@PathVariable Integer id) {
        var s = repo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        s.setValue(s.getDefaultValue());
        repo.save(s);
        return ResponseEntity.ok(CrmSettingResponseDto.of(s));
    }

    // Get require_active_for_login setting (for mobile app login restriction)
    @GetMapping("/require-active-for-login")
    public ResponseEntity<Map<String, Boolean>> getRequireActiveForLogin() {
        var setting = repo.findByName(REQUIRE_ACTIVE_FOR_LOGIN);
        boolean value = false;
        if (setting.isPresent()) {
            String val = setting.get().getValue();
            value = val != null && ("true".equalsIgnoreCase(val) || "1".equals(val));
        }
        return ResponseEntity.ok(Map.of("requireActiveForLogin", value));
    }

    // Update require_active_for_login setting (admin only)
    @PutMapping("/require-active-for-login")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> setRequireActiveForLogin(@RequestBody Map<String, Boolean> body) {
        Boolean value = body.get("requireActiveForLogin");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        var setting = repo.findByName(REQUIRE_ACTIVE_FOR_LOGIN);
        CrmSetting s;
        if (setting.isPresent()) {
            s = setting.get();
        } else {
            // Create if doesn't exist
            s = CrmSetting.builder()
                    .name(REQUIRE_ACTIVE_FOR_LOGIN)
                    .type("boolean")
                    .defaultValue("false")
                    .build();
        }
        s.setValue(String.valueOf(value));
        repo.save(s);
        return ResponseEntity.ok(Map.of("requireActiveForLogin", value));
    }
}
