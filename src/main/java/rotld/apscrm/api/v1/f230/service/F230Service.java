package rotld.apscrm.api.v1.f230.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.d177.repository.D177Settings;
import rotld.apscrm.api.v1.d177.repository.D177SettingsRepository;
import rotld.apscrm.api.v1.f230.dto.F230ResponseDto;
import rotld.apscrm.api.v1.f230.repository.F230;
import rotld.apscrm.api.v1.f230.repository.F230Repository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class F230Service {
    private final F230Repository repo;
    private final D177SettingsRepository settingsRepo;

    private static final Map<String, String> SORT_MAP = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("anFiscal", "year"),
            Map.entry("submittedAt", "postDateIso"),
            Map.entry("period", "distrib2"),
            Map.entry("iban", "iban"),
            Map.entry("beneficiary", "lastName"), // aproximare; poți schimba pe first/last concatenat
            Map.entry("emailContrib", "email"),
            Map.entry("acordEmail", "acordEmail"),
            Map.entry("downloaded", "downloaded"),
            Map.entry("verified", "verified"),
            Map.entry("corrupt", "corrupt")
    );
    private static final Set<String> ALLOWED = Set.of(
            "id", "year", "postDateIso", "distrib2", "iban", "firstName", "lastName",
            "email", "acordEmail", "downloaded", "verified", "corrupt"
    );

    private Pageable remap(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) return pageable;
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            String p = SORT_MAP.getOrDefault(o.getProperty(), o.getProperty());
            if (!ALLOWED.contains(p)) continue;
            orders.add(new Sort.Order(o.getDirection(), p, o.getNullHandling()));
        }
        return orders.isEmpty() ? pageable :
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    public Page<F230ResponseDto> list(Pageable pageable) {
        Pageable p = remap(pageable);
        Page<F230> page = repo.findAll(p);
        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    public Page<F230ResponseDto> search(Pageable pageable, String q) {
        Pageable p = remap(pageable);
        Specification<F230> spec = buildSpec(q);
        Page<F230> page = repo.findAll(spec, p);
        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    private Specification<F230> buildSpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = q.trim().toLowerCase();
            String like = "%" + term + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("year")), like));
            ors.add(cb.like(cb.lower(root.get("iban")), like));
            ors.add(cb.like(cb.lower(root.get("email")), like));
            ors.add(cb.like(cb.lower(root.get("firstName")), like));
            ors.add(cb.like(cb.lower(root.get("lastName")), like));
            if (term.chars().allMatch(Character::isDigit)) {
                try {
                    ors.add(cb.equal(root.get("id"), Integer.parseInt(term)));
                } catch (Exception ignored) {
                }
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    private Map<Integer, D177Settings> fetchFlags(List<F230> rows) {
        if (rows.isEmpty()) return Map.of();
        List<Integer> ids = rows.stream().map(F230::getId).toList();
        return settingsRepo.findByPostIdIn(ids).stream()
                .collect(Collectors.toMap(D177Settings::getPostId, x -> x));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private F230ResponseDto toDto(F230 r, D177Settings ps) {
        boolean twoYears = "1".equals(nz(r.getDistrib2()));
        boolean acord = "1".equals(nz(r.getAcordEmail()));
        return F230ResponseDto.builder()
                .id(r.getId())
                .year(nz(r.getYear()))
                .submittedAt(nz(r.getPostDateIso()))
                .period(twoYears ? "2 ani" : "1 an")
                .iban(nz(r.getIban()))
                .beneficiary((nz(r.getFirstName()) + " " + nz(r.getLastName())).trim())
                .emailContrib(nz(r.getEmail()))
                .nrBorderou("") // gol
                .downloaded(Boolean.TRUE.equals(ps != null ? ps.getDownloaded() : r.getDownloaded()))
                .verified(Boolean.TRUE.equals(ps != null ? ps.getVerified() : r.getVerified()))
                .corrupt(Boolean.TRUE.equals(ps != null ? ps.getCorrupt() : r.getCorrupt()))
                .acordDate(acord ? "Da" : "Nu")
                .pdfUrl(nz(r.getPdfUrl()))
                .detail(nz(r.getAdminEdit()))
                .nrBorderou(r.getNrBorderou() != null ? String.valueOf(r.getNrBorderou()) : "")
                .build();
    }

    public List<F230> getAll() {
        return repo.findAll(Pageable.ofSize(9999)).getContent();
    }

    @Transactional
    public void delete(Integer id) {
        repo.deleteMeta(id);
        int affected = repo.deletePost(id);
        if (affected == 0) throw new IllegalArgumentException("F230 record not found: " + id);
    }
}