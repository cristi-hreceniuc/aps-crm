package rotld.apscrm.api.v1.d177;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.d177.dto.D177DetailsDto;
import rotld.apscrm.api.v1.d177.dto.D177ResponseDto;
import rotld.apscrm.api.v1.d177.service.D177Service;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/formulare/d177")
public class D177Controller {

    private final D177Service service;

    @GetMapping
    public org.springframework.data.domain.Page<D177ResponseDto> list(
            @PageableDefault(size = 10, sort = "postDateIso") Pageable pageable){
        return service.getPage(pageable);
    }

    @PutMapping("/{id}/flags")
    public ResponseEntity<Void> updateFlags(@PathVariable Integer id, @RequestBody Map<String,Object> body){
        Boolean downloaded = body.containsKey("downloaded") ? (Boolean) body.get("downloaded") : null;
        Boolean verified   = body.containsKey("verified")   ? (Boolean) body.get("verified")   : null;
        Boolean corrupt    = body.containsKey("corrupt")    ? (Boolean) body.get("corrupt")    : null;
        service.updateFlags(id, downloaded, verified, corrupt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public Page<D177ResponseDto> search(
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(size = 10, sort = "postDateIso") Pageable pageable
    ){
        return service.search(pageable, q);
    }

    @GetMapping("/{id}")
    public D177DetailsDto one(@PathVariable Integer id){
        return service.getDetails(id);
    }
}