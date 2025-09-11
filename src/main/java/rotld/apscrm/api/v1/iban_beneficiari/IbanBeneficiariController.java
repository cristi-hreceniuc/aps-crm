package rotld.apscrm.api.v1.iban_beneficiari;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.iban_beneficiari.dto.IbanBeneficiariResponseDto;
import rotld.apscrm.api.v1.iban_beneficiari.dto.UpdateIbanRequestDto;
import rotld.apscrm.api.v1.iban_beneficiari.service.IbanBeneficiariService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/iban")
public class IbanBeneficiariController {
    private final IbanBeneficiariService service;

    @GetMapping
    public Page<IbanBeneficiariResponseDto> list(Pageable pageable){ return service.list(pageable); }

    @GetMapping("/search")
    public Page<IbanBeneficiariResponseDto> search(@RequestParam(name="q", required=false) String q, Pageable pageable){
        return service.search(pageable, q);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Integer id,
                       @RequestBody Map<String, String> body) {
        // dacă există cheia "nume"
        if (body.containsKey("name") && body.get("name") != null && !body.get("name").isBlank()) {
            service.upsertMeta(id, "nume", body.get("name"));
        }

        // dacă există cheia "iban"
        if (body.containsKey("iban") && body.get("iban") != null && !body.get("iban").isBlank()) {
            service.upsertMeta(id, "iban", body.get("iban"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}