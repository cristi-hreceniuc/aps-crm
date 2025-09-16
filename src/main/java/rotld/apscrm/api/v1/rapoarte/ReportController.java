package rotld.apscrm.api.v1.rapoarte;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rotld.apscrm.api.v1.cause.repository.Cause;
import rotld.apscrm.api.v1.sponsorizare.service.SponsorizareService;
import rotld.apscrm.api.v1.volunteer.dto.VolunteerResponseDto;
import rotld.apscrm.api.v1.volunteer.service.VolunteerService;
import rotld.apscrm.api.v1.sponsorizare.dto.SponsorizareResponseDto;
import rotld.apscrm.api.v1.iban_beneficiari.dto.IbanBeneficiariResponseDto;
import rotld.apscrm.api.v1.iban_beneficiari.service.IbanBeneficiariService;
import rotld.apscrm.api.v1.f230.dto.F230ResponseDto;
import rotld.apscrm.api.v1.f230.service.F230Service;
import rotld.apscrm.api.v1.d177.dto.D177ResponseDto;
import rotld.apscrm.api.v1.d177.service.D177Service;
import rotld.apscrm.api.v1.cause.service.CauseService;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final VolunteerService volunteerService;
    private final SponsorizareService sponsorizareService;
    private final IbanBeneficiariService ibanService;
    private final F230Service f230Service;
    private final D177Service d177Service;
    private final CauseService causeService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String dataset) throws Exception {
        StringBuilder sb = new StringBuilder();

        switch (dataset) {
            case "voluntari" -> {
                List<VolunteerResponseDto> list = volunteerService.getAll();
                writeCsv(sb, list, VolunteerResponseDto.class);
            }
            case "sponsorizare" -> {
                List<SponsorizareResponseDto> list = sponsorizareService.list(PageRequest.of(0, 9999)).getContent();
                writeCsv(sb, list, SponsorizareResponseDto.class);
            }
            case "iban" -> {
                List<IbanBeneficiariResponseDto> list = ibanService.list(PageRequest.of(0, 9999)).getContent();
                writeCsv(sb, list, IbanBeneficiariResponseDto.class);
            }
            case "f230" -> {
                List<F230ResponseDto> list = f230Service.list(PageRequest.of(0, 9999)).getContent();
                writeCsv(sb, list, F230ResponseDto.class);
            }
            case "d177" -> {
                List<D177ResponseDto> list = d177Service.getPage(Pageable.ofSize(9999)).getContent();
                writeCsv(sb, list, D177ResponseDto.class);
            }
            case "cause" -> {
                List<Cause> list = causeService.page("", Pageable.ofSize(9999)).getContent();
                writeCsv(sb, list, Cause.class);
            }
            default -> throw new IllegalArgumentException("Dataset necunoscut: " + dataset);
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dataset + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    private <T> void writeCsv(StringBuilder sb, List<T> list, Class<T> clazz) {
        if (list.isEmpty()) return;

        if (clazz.isRecord()) {
            // 🔹 Record (ex: VolunteerResponseDto, D177ResponseDto)
            RecordComponent[] components = clazz.getRecordComponents();

            // antet
            for (int i = 0; i < components.length; i++) {
                sb.append(components[i].getName());
                if (i < components.length - 1) sb.append(",");
            }
            sb.append("\n");

            // date
            for (T item : list) {
                for (int c = 0; c < components.length; c++) {
                    try {
                        Object val = components[c].getAccessor().invoke(item);
                        sb.append(escape(val));
                    } catch (Exception e) {
                        sb.append("ERR");
                    }
                    if (c < components.length - 1) sb.append(",");
                }
                sb.append("\n");
            }
        } else {
            // 🔹 Clasă normală (DTO cu fields)
            Field[] fields = clazz.getDeclaredFields();

            // antet
            for (int i = 0; i < fields.length; i++) {
                sb.append(fields[i].getName());
                if (i < fields.length - 1) sb.append(",");
            }
            sb.append("\n");

            // date
            for (T item : list) {
                for (int c = 0; c < fields.length; c++) {
                    try {
                        fields[c].setAccessible(true);
                        Object val = fields[c].get(item);
                        sb.append(escape(val));
                    } catch (Exception e) {
                        sb.append("ERR");
                    }
                    if (c < fields.length - 1) sb.append(",");
                }
                sb.append("\n");
            }
        }
    }

    private String escape(Object val) {
        if (val == null) return "";
        String str = val.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
