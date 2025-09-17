package rotld.apscrm.api.v1.cause.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.cause.repository.Cause;
import rotld.apscrm.api.v1.cause.repository.CauseRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CauseService {

    private final CauseRepository repo;

    public Page<Cause> page(String q, Pageable pageable){
        if (q == null || q.isBlank()) {
            return repo.findAll(pageable);
        }
        final String like = "%" + q.trim().toLowerCase() + "%";

        Specification<Cause> spec = (root, cq, cb) -> {
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("title")),   like));
            ors.add(cb.like(cb.lower(root.get("excerpt")), like));
            try {
                ors.add(cb.equal(root.get("id"), Integer.parseInt(q.trim())));
            } catch (Exception ignored) {}
            return cb.or(ors.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageable);
    }

    public void updateCauseAmount(Integer id, Double value, String operation) {
        if ("+".equals(operation)) {
            repo.addAmountToCause(id, value);
        } else repo.subtractAmountFromCause(id, value);
    }
}