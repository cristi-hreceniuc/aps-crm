package rotld.apscrm.api.v1.iban_beneficiari.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.d177.repository.D177Settings;
import rotld.apscrm.api.v1.iban_beneficiari.dto.IbanBeneficiariResponseDto;
import rotld.apscrm.api.v1.iban_beneficiari.dto.UpdateIbanRequestDto;
import rotld.apscrm.api.v1.iban_beneficiari.repository.IbanBeneficiari;
import rotld.apscrm.api.v1.iban_beneficiari.repository.IbanBeneficiariViewRepository;
import rotld.apscrm.api.v1.iban_beneficiari.repository.IbanBeneficiariWriteRepository;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerMeta;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IbanBeneficiariService {
    private final IbanBeneficiariViewRepository viewRepo;
    private final IbanBeneficiariWriteRepository writeRepo;

    private Pageable remap(Pageable pageable){
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) return pageable;
        List<Sort.Order> good = new ArrayList<>();
        for (Sort.Order o: sort){
            String p = switch (o.getProperty()){
                case "name" -> "name";
                case "iban" -> "iban";
                case "id"   -> "id";
                default -> null;
            };
            if (p != null) good.add(new Sort.Order(o.getDirection(), p));
        }
        return good.isEmpty()? pageable :
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(good));
    }

    public Page<IbanBeneficiariResponseDto> list(Pageable pageable){
        Page<IbanBeneficiari> page = viewRepo.findAll(remap(pageable));
        return page.map(v -> IbanBeneficiariResponseDto.builder()
                .id(v.getId()).name(v.getName()).iban(v.getIban()).addedAt(v.getPostDateIso()).build());
    }

    public Page<IbanBeneficiariResponseDto> search(Pageable pageable, String q){
        Specification<IbanBeneficiari> spec = (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = q.trim().toLowerCase(), like = "%" + term + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("name")), like));
            ors.add(cb.like(cb.lower(root.get("iban")), like));
            if (term.chars().allMatch(Character::isDigit))
                ors.add(cb.equal(root.get("id"), Integer.parseInt(term)));
            return cb.or(ors.toArray(new Predicate[0]));
        };
        Page<IbanBeneficiari> page = viewRepo.findAll(spec, remap(pageable));
        return page.map(v -> IbanBeneficiariResponseDto.builder()
                .id(v.getId()).name(v.getName()).iban(v.getIban()).addedAt(v.getPostDateIso()).build());
    }

    @Transactional
    public void upsertMeta(Integer postId, String key, String value) {
        int updated = writeRepo.updateMeta(postId, key, value);
        if (updated == 0) {
            // dacă nu există, facem insert
            VolunteerMeta meta = VolunteerMeta.builder()
                    .id(postId)
                    .metaKey(key)
                    .metaValue(value)
                    .build();
            writeRepo.save(meta);
        }
    }

    @Transactional
    public void delete(Integer postId) {
        writeRepo.deleteMeta(postId);
        int affected = writeRepo.deletePost(postId);
        if (affected == 0) throw new IllegalArgumentException("Iban beneficiar  not found: " + postId);

    }
}