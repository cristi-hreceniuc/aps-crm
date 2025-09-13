package rotld.apscrm.api.v1.cause.offline_payment;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.cause.offline_payment.dto.OfflinePaymentDto;
import rotld.apscrm.api.v1.cause.offline_payment.dto.UpdateStatusRequest;
import rotld.apscrm.api.v1.cause.offline_payment.service.OfflinePaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/offline-payments")
public class OfflinePaymentController {

    private final OfflinePaymentService service;

    @GetMapping
    public Page<OfflinePaymentDto> list(@RequestParam(value="q", required=false) String q,
                                        Pageable pageable){
        return service.list(pageable, q);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Integer id,
                                             @RequestBody UpdateStatusRequest req){
        service.changeStatus(id, req.getStatus());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}