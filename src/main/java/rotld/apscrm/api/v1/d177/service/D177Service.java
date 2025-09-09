package rotld.apscrm.api.v1.d177.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.d177.dto.D177ResponseDto;
import rotld.apscrm.api.v1.d177.repository.D177;
import rotld.apscrm.api.v1.d177.repository.D177Repository;
import rotld.apscrm.api.v1.d177.repository.D177Settings;
import rotld.apscrm.api.v1.d177.repository.D177SettingsRepository;
import rotld.apscrm.common.PhpSerialized;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class D177Service {
    private final D177Repository repo;
    private final D177SettingsRepository settingsRepo;

    // map UI -> coloane reale
    private static final Map<String,String> SORT_MAP = Map.ofEntries(
            Map.entry("companyName",  "companyName"), // 🔴 acum sortăm pe denumirea din meta
            Map.entry("fiscalCode",   "fiscalCode"),
            Map.entry("email",        "email"),
            Map.entry("amount",       "amountNum"),
            Map.entry("contractDate", "contractDate"),
            Map.entry("downloaded",   "downloaded"),
            Map.entry("verified",     "verified"),
            Map.entry("corrupt",      "corrupt"),
            Map.entry("id",           "id"),
            Map.entry("date",         "postDateIso")
    );

    private static final Set<String> ALLOWED = Set.of(
            "id","postDateIso",
            "companyName",            //
            "fiscalCode","email","phone",
            "amountNum","amountStr","contractDate",
            "downloaded","verified","corrupt"
    );

    /** Re-map sort keys de la UI la coloanele entității @Subselect */
    private Pageable remapSort(Pageable pageable){
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) return pageable;

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : sort){
            String ui = o.getProperty();
            String be = SORT_MAP.getOrDefault(ui, ui);
            if (!ALLOWED.contains(be)) continue;
            orders.add(new Sort.Order(o.getDirection(), be, o.getNullHandling()));
        }
        return orders.isEmpty() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    /** Listă fără căutare */
    public Page<D177ResponseDto> getPage(Pageable pageable){
        Pageable p = remapSort(pageable);
        Page<D177> page = repo.findAll(p);

        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    /** Search server-side (q pe mai multe câmpuri) */
    public Page<D177ResponseDto> search(Pageable pageable, String q){
        Pageable p = remapSort(pageable);
        Specification<D177> spec = buildSpec(q);

        Page<D177> page = repo.findAll(spec, p);
        Map<Integer, D177Settings> flags = fetchFlags(page.getContent());
        return page.map(r -> toDto(r, flags.get(r.getId())));
    }

    private Map<Integer, D177Settings> fetchFlags(List<D177> rows){
        if (rows.isEmpty()) return Collections.emptyMap();
        List<Integer> ids = rows.stream().map(D177::getId).toList();
        return settingsRepo.findByPostIdIn(ids).stream()
                .collect(Collectors.toMap(D177Settings::getPostId, ps -> ps));
    }

    private Specification<D177> buildSpec(String q){
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();

            String term = q.trim().toLowerCase();
            String like = "%" + term + "%";
            List<Predicate> ors = new ArrayList<>();

            // 🔴 DOAR aceste câmpuri:
            ors.add(cb.like(cb.lower(root.get("companyName")), like));
            ors.add(cb.like(cb.lower(root.get("fiscalCode")),  like));
            ors.add(cb.like(cb.lower(root.get("email")),       like));

            // opțional: dacă e numeric, permite și egalitate pe ID
            boolean numeric = term.chars().allMatch(Character::isDigit);
            if (numeric){
                try { ors.add(cb.equal(root.get("id"), Integer.parseInt(term))); } catch (Exception ignored) {}
            }

            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private D177ResponseDto toDto(D177 r, D177Settings ps){
        return D177ResponseDto.builder()
                .id(r.getId())
                .date(r.getPostDateIso())
                .companyName(firstNonEmpty(r.getCompanyName(), r.getTitle()))
                .fiscalCode(firstNonEmpty(r.getFiscalCode()))
                .email(firstNonEmpty(r.getEmail()))
                .phone(firstNonEmpty(r.getPhone()))
                .amount(firstNonEmpty(r.getAmountStr()))
                .contractDate(firstNonEmpty(r.getContractDate()))
                .docUrl(r.getDocUrl())
                .detail(firstNonEmpty(r.getDetail(), r.getAdminEdit()))
                .adminEdit(r.getAdminEdit())
                .downloaded(Boolean.TRUE.equals((ps != null ? ps.getDownloaded() : r.getDownloaded())))
                .verified(Boolean.TRUE.equals((ps != null ? ps.getVerified()   : r.getVerified())))
                .corrupt(Boolean.TRUE.equals((ps != null ? ps.getCorrupt()     : r.getCorrupt())))
                .build();
    }

    @Transactional
    public void updateFlags(Integer id, Boolean downloaded, Boolean verified, Boolean corrupt){
        settingsRepo.upsertFlags(id, downloaded, verified, corrupt);
    }

    @Transactional
    public void delete(Integer id){
        repo.deleteMeta(id);
        int affected = repo.deletePost(id);
        if (affected == 0) throw new IllegalArgumentException("D177 record not found: " + id);
    }
}