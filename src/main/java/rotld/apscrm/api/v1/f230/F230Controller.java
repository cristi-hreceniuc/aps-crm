package rotld.apscrm.api.v1.f230;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.d177.repository.D177Settings;
import rotld.apscrm.api.v1.d177.repository.D177SettingsRepository;
import rotld.apscrm.api.v1.f230.dto.F230DetailDto;
import rotld.apscrm.api.v1.f230.dto.F230ResponseDto;
import rotld.apscrm.api.v1.f230.service.F230Service;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/f230")
public class F230Controller {
    private final F230Service service;
    private final D177SettingsRepository settingsRepo;

    @GetMapping
    public Page<F230ResponseDto> list(Pageable pageable){ return service.list(pageable); }

    @GetMapping("/search")
    public Page<F230ResponseDto> search(@RequestParam(name="q", required=false) String q, Pageable pageable){
        return service.search(pageable, q);
    }

    @PutMapping("/{id}/flags")
    @Transactional
    public ResponseEntity<Void> upsertFlags(@PathVariable Integer id, @RequestBody Map<String, Boolean> body){
        Boolean downloaded = body.containsKey("downloaded") ? (Boolean) body.get("downloaded") : null;
        Boolean verified   = body.containsKey("verified")   ? (Boolean) body.get("verified")   : null;
        Boolean corrupt    = body.containsKey("corrupt")    ? (Boolean) body.get("corrupt")    : null;
        settingsRepo.upsertFlags(id, downloaded, verified, corrupt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public F230DetailDto getOne(@PathVariable Integer id){
        return service.detail(id);
    }
}