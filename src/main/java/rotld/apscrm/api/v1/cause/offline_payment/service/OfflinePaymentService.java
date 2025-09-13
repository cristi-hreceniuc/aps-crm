package rotld.apscrm.api.v1.cause.offline_payment.service;


import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rotld.apscrm.api.v1.cause.offline_payment.dto.OfflinePaymentDto;
import rotld.apscrm.api.v1.cause.offline_payment.repository.OfflinePaymentView;
import rotld.apscrm.api.v1.cause.offline_payment.repository.OfflinePaymentViewRepository;
import rotld.apscrm.api.v1.cause.offline_payment.repository.OfflinePaymentWriteRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfflinePaymentService {
    private final OfflinePaymentViewRepository viewRepo;
    private final OfflinePaymentWriteRepository writeRepo;

    private Pageable remap(Pageable pageable){
        if (pageable.getSort().isUnsorted()) return pageable;
        List<Sort.Order> good = new ArrayList<>();
        for (Sort.Order o: pageable.getSort()){
            String p = switch (o.getProperty()){
                case "id"            -> "id";
                case "date","bookingDate" -> "bookingDate";
                case "title","causeTitle" -> "causeTitle";
                case "status"        -> "orderStatus";
                case "amount"        -> "amount";
                case "paymentDate"   -> "paymentDate";
                case "paymentMethod" -> "paymentMethod";
                default -> null;
            };
            if (p != null) good.add(new Sort.Order(o.getDirection(), p));
        }
        return good.isEmpty()? pageable :
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(good));
    }

    public Page<OfflinePaymentDto> list(Pageable pageable, String q){
        Specification<OfflinePaymentView> spec = (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String term = "%" + q.trim().toLowerCase() + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("causeTitle")), term));
            ors.add(cb.like(cb.lower(root.get("orderStatus")), term));
            ors.add(cb.like(cb.lower(root.get("paymentMethod")), term));
            // dacă e doar cifre -> caută și după id/causeId
            if (q.chars().allMatch(Character::isDigit)){
                ors.add(cb.equal(root.get("id"), Integer.parseInt(q)));
                ors.add(cb.equal(root.get("causeId"), Integer.parseInt(q)));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };

        Page<OfflinePaymentView> page = viewRepo.findAll(spec, remap(pageable));
        return page.map(v -> OfflinePaymentDto.builder()
                .id(v.getId())
                .causeId(v.getCauseId())
                .causeTitle(v.getCauseTitle())
                .bookingDate(v.getBookingDate() == null ? null : v.getBookingDate())
                .status(v.getOrderStatus())
                .amount(v.getAmount())
                .paymentDate(v.getPaymentDate() == null ? null : v.getPaymentDate())
                .paymentMethod(v.getPaymentMethod())
                .build());
    }

    @Transactional
    public void changeStatus(Integer id, String newStatus){
        // nu permitem modificarea dacă este online-paid
        OfflinePaymentView v = viewRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
        if ("online-paid".equalsIgnoreCase(v.getOrderStatus()))
            throw new IllegalStateException("Online paid cannot be modified");

        int n = writeRepo.updateStatus(id, newStatus);
        if (n == 0) throw new IllegalArgumentException("Not found for update: " + id);
    }

    @Transactional
    public void delete(Integer id){
        int n = writeRepo.hardDelete(id);
        if (n == 0) throw new IllegalArgumentException("Not found for delete: " + id);
    }
}
