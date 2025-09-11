package rotld.apscrm.api.v1.sponsorizare.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.d177.repository.D177Settings;
import rotld.apscrm.api.v1.d177.repository.D177SettingsRepository;
import rotld.apscrm.api.v1.sponsorizare.dto.SponsorizareResponseDto;
import rotld.apscrm.api.v1.sponsorizare.repository.Sponsorizare;
import rotld.apscrm.api.v1.sponsorizare.repository.SponsorizareRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorizareService {

    private final SponsorizareRepository repo;
    private final D177SettingsRepository settingsRepo;

    private static final Map<String,String> SORT_MAP = Map.ofEntries(
            Map.entry("companyName",  "companyName"),
            Map.entry("fiscalCode",   "fiscalCode"),
            Map.entry("email",        "email"),
            Map.entry("phone",        "phone"),
            Map.entry("iban",         "iban"),
            Map.entry("amount",       "amountNum"),
            Map.entry("contractDate", "contractDate"),
            Map.entry("downloaded",   "downloaded"),
            Map.entry("verified",     "verified"),
            Map.entry("corrupt",      "corrupt"),
            Map.entry("id",           "id"),
            Map.entry("date",         "postDateIso")
    );
    private static final Set<String> ALLOWED = Set.of(
            "id","postDateIso","companyName","fiscalCode","email","phone","iban",
            "amountNum","amountStr","contractDate","downloaded","verified","corrupt"
    );

    private Pageable remapSort(Pageable pageable){
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) return pageable;
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : sort){
            String be = SORT_MAP.getOrDefault(o.getProperty(), o.getProperty());
            if (!ALLOWED.contains(be)) continue;
            orders.add(new Sort.Order(o.getDirection(), be, o.getNullHandling()));
        }
        return orders.isEmpty()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    public Page<SponsorizareResponseDto> list(Pageable pageable){
        Pageable p = remapSort(pageable);
        Page<Sponsorizare> page = repo.findAll(p);
        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    public Page<SponsorizareResponseDto> search(Pageable pageable, String q){
        Pageable p = remapSort(pageable);
        Specification<Sponsorizare> spec = buildSpec(q);
        Page<Sponsorizare> page = repo.findAll(spec, p);
        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    private Map<Integer, D177Settings> fetchFlags(List<Sponsorizare> rows){
        if (rows.isEmpty()) return Map.of();
        List<Integer> ids = rows.stream().map(Sponsorizare::getId).toList();
        return settingsRepo.findByPostIdIn(ids).stream()
                .collect(Collectors.toMap(D177Settings::getPostId, x -> x));
    }

    private Specification<Sponsorizare> buildSpec(String q){
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = q.trim().toLowerCase();
            String like = "%" + term + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("companyName")), like));
            ors.add(cb.like(cb.lower(root.get("fiscalCode")),  like));
            ors.add(cb.like(cb.lower(root.get("email")),       like));
            ors.add(cb.like(cb.lower(root.get("phone")),       like));
            ors.add(cb.like(cb.lower(root.get("iban")),        like));
            if (term.chars().allMatch(Character::isDigit)) {
                try { ors.add(cb.equal(root.get("id"), Integer.parseInt(term))); } catch (Exception ignored) {}
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    private static String nz(String... v){
        for (String s : v) if (s != null && !s.isBlank()) return s.trim();
        return "";
    }

    private SponsorizareResponseDto toDto(Sponsorizare r, D177Settings ps){
        return SponsorizareResponseDto.builder()
                .id(r.getId())
                .date(r.getPostDateIso())
                .companyName(nz(r.getCompanyName(), r.getTitle()))
                .fiscalCode(nz(r.getFiscalCode()))
                .email(nz(r.getEmail()))
                .phone(nz(r.getPhone()))
                .iban(nz(r.getIban()))
                .amount(nz(r.getAmountStr()))
                .contractDate(nz(r.getContractDate()))
                .docUrl(r.getDocUrl())
                .jsonUrl(r.getJsonUrl())
                .signatureUrl(r.getSignatureUrl())
                .detail(nz(r.getDetail(), r.getAdminEdit()))
                .adminEdit(r.getAdminEdit())
                .downloaded(Boolean.TRUE.equals(ps != null ? ps.getDownloaded() : r.getDownloaded()))
                .verified(Boolean.TRUE.equals(ps != null ? ps.getVerified()   : r.getVerified()))
                .corrupt(Boolean.TRUE.equals(ps != null ? ps.getCorrupt()     : r.getCorrupt()))
                .build();
    }
}