package rotld.apscrm.api.v1.borderou;


import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.borderou.service.Borderou230Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/f230")
public class Borderou230Controller {

    private final Borderou230Service service;

    public static class GenerateRequest {
        public List<Integer> ids;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate date;
    }

    @PostMapping("/borderou")
    public ResponseEntity<byte[]> generate(@RequestBody GenerateRequest body){
        LocalDate date = (body.date != null) ? body.date : LocalDate.now();
        var gen = service.generateForIds(body.ids, date);

        String filename = "borderou230-" + gen.id() + ".xml";
        byte[] bytes = gen.xml().getBytes(StandardCharsets.UTF_8);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_XML);
        h.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(bytes, h, HttpStatus.OK);
    }
}