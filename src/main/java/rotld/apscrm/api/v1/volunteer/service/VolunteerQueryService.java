package rotld.apscrm.api.v1.volunteer.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import rotld.apscrm.api.v1.volunteer.repository.Volunteer;
import rotld.apscrm.api.v1.volunteer.repository.VolunteerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;


@Service
@RequiredArgsConstructor
public class VolunteerQueryService {

    private final EntityManager em;
    private final VolunteerRepository repo;

    /* === Chei & constante === */
    private static final String TYPE   = "aps_volunteer";

    private static final String K_LAST = "_vol_nume";        // Nume (last)
    private static final String K_FIRST= "_vol_prenume";     // Prenume (first)
    private static final String K_EMAIL= "_vol_email";
    private static final String K_DOM  = "_vol_domeniu";
    private static final String K_AGE  = "_vol_varsta";
    private static final String K_OCC  = "_vol_ocupatie";
    private static final String K_MOT  = "_vol_motivatie";
    private static final String K_EXP  = "_vol_experienta";

    public Page<Volunteer> findAllPage(Pageable pageable) {
        // 1) Age → sort numeric prin JPQL dedicat (cast(... as integer))
        var ageOrder = pageable.getSort().getOrderFor("age");
        if (ageOrder != null && isSingleField(pageable.getSort(), "age")) {
            Pageable onlyPage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return ageOrder.isAscending()
                    ? repo.findAllOrderByAgeAsc(onlyPage)
                    : repo.findAllOrderByAgeDesc(onlyPage);
        }

        // 2) Sort simplu pe coloane din wp_posts (fără join):
        if (isOnlyPostsColumns(pageable.getSort())) {
            return simplePostsQuery(pageable);
        }

        // 3) Altfel, Criteria cu join-uri lazy doar pentru câmpurile cerute:
        return criteriaWithLazyJoins(pageable);
    }

    /** True dacă sort-ul conține doar câmpuri din wp_posts: id, postName, date. */
    private boolean isOnlyPostsColumns(Sort sort) {
        if (!sort.isSorted()) return true;
        return StreamSupport.stream(sort.spliterator(), false)
                .map(Sort.Order::getProperty)
                .allMatch(p -> p.equals("id") || p.equals("postName") || p.equals("date"));
    }

    /** True dacă toate ordinele sunt pe același câmp (ex: doar "age"). */
    private boolean isSingleField(Sort sort, String field) {
        return sort.isSorted() && StreamSupport.stream(sort.spliterator(), false)
                .allMatch(o -> o.getProperty().equals(field));
    }

    /** Query fără niciun JOIN (numeric pe id, text pe postTitle, date pe postDate). */
    private Page<Volunteer> simplePostsQuery(Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        var cq = cb.createQuery(Volunteer.class);
        var v  = cq.from(Volunteer.class);

        cq.where(cb.equal(v.get("postType"), TYPE));

        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(o -> {
                boolean asc = o.isAscending();
                switch (o.getProperty()) {
                    case "id"       -> orders.add(asc ? cb.asc(v.get("id"))                 : cb.desc(v.get("id")));       // numeric
                    case "postName" -> orders.add(asc ? cb.asc(cb.lower(v.get("postTitle"))) : cb.desc(cb.lower(v.get("postTitle"))));
                    case "date"     -> orders.add(asc ? cb.asc(v.get("postDate"))           : cb.desc(v.get("postDate")));
                    default         -> orders.add(asc ? cb.asc(v.get("id"))                 : cb.desc(v.get("id")));
                }
            });
        } else {
            orders.add(cb.desc(v.get("id")));
        }
        cq.orderBy(orders);

        var query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        var content = query.getResultList();

        var cc = cb.createQuery(Long.class);
        var v2 = cc.from(Volunteer.class);
        cc.select(cb.count(v2));
        cc.where(cb.equal(v2.get("postType"), TYPE));
        long total = em.createQuery(cc).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /** Criteria cu JOIN-uri lazy doar când sunt necesare (fără DISTINCT). */
    private Page<Volunteer> criteriaWithLazyJoins(Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        var cq = cb.createQuery(Volunteer.class);
        var v  = cq.from(Volunteer.class);

        cq.where(cb.equal(v.get("postType"), TYPE));
        // NU folosim DISTINCT aici ca să nu lovim limita MySQL DISTINCT + ORDER BY pe join.

        // Join-uri lazy: create doar când sunt cerute în sort
        Join<Volunteer, ?> jLast  = null;
        Join<Volunteer, ?> jFirst = null;
        Join<Volunteer, ?> jEmail = null;
        Join<Volunteer, ?> jDom   = null;
        Join<Volunteer, ?> jOcc   = null;
        Join<Volunteer, ?> jMot   = null;
        Join<Volunteer, ?> jExp   = null;

        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();

        if (pageable.getSort().isSorted()) {
            for (Sort.Order o : pageable.getSort()) {
                boolean asc = o.isAscending();
                String p = o.getProperty();

                switch (p) {
                    case "id" -> {
                        // numeric pe id — fără joinuri
                        orders.add(asc ? cb.asc(v.get("id")) : cb.desc(v.get("id")));
                    }
                    case "postName" -> {
                        Expression<String> postTitle = cb.lower(cb.coalesce(v.get("postTitle"), cb.literal("")));
                        orders.add(asc ? cb.asc(postTitle) : cb.desc(postTitle));
                    }
                    case "date" -> {
                        orders.add(asc ? cb.asc(v.get("postDate")) : cb.desc(v.get("postDate")));
                    }
                    case "name" -> {
                        if (jLast == null)  { jLast  = v.join("meta", JoinType.LEFT);  jLast.on(cb.equal(jLast.get("metaKey"),  K_LAST)); }
                        if (jFirst == null) { jFirst = v.join("meta", JoinType.LEFT); jFirst.on(cb.equal(jFirst.get("metaKey"), K_FIRST)); }
                        Expression<String> lname = cb.lower(cb.coalesce(jLast.get("metaValue"),  cb.literal("")));
                        Expression<String> fname = cb.lower(cb.coalesce(jFirst.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(lname) : cb.desc(lname));
                        orders.add(asc ? cb.asc(fname) : cb.desc(fname));
                    }
                    case "email" -> {
                        if (jEmail == null) { jEmail = v.join("meta", JoinType.LEFT); jEmail.on(cb.equal(jEmail.get("metaKey"), K_EMAIL)); }
                        Expression<String> email = cb.lower(cb.coalesce(jEmail.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(email) : cb.desc(email));
                    }
                    case "domain" -> {
                        if (jDom == null)   { jDom   = v.join("meta", JoinType.LEFT); jDom.on(cb.equal(jDom.get("metaKey"),   K_DOM)); }
                        Expression<String> dom = cb.lower(cb.coalesce(jDom.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(dom) : cb.desc(dom));
                    }
                    case "ocupation" -> {
                        if (jOcc == null)   { jOcc   = v.join("meta", JoinType.LEFT); jOcc.on(cb.equal(jOcc.get("metaKey"),   K_OCC)); }
                        Expression<String> occ = cb.lower(cb.coalesce(jOcc.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(occ) : cb.desc(occ));
                    }
                    case "motivation" -> {
                        if (jMot == null)   { jMot   = v.join("meta", JoinType.LEFT); jMot.on(cb.equal(jMot.get("metaKey"),   K_MOT)); }
                        Expression<String> mot = cb.lower(cb.coalesce(jMot.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(mot) : cb.desc(mot));
                    }
                    case "experience" -> {
                        if (jExp == null)   { jExp   = v.join("meta", JoinType.LEFT); jExp.on(cb.equal(jExp.get("metaKey"),   K_EXP)); }
                        Expression<String> exp = cb.lower(cb.coalesce(jExp.get("metaValue"), cb.literal("")));
                        orders.add(asc ? cb.asc(exp) : cb.desc(exp));
                    }
                    case "age" -> {
                        // nu tratăm aici (numeric) — e interceptat mai sus și rutat în repo
                        // dacă ajunge aici, folosește fallback pe id ca să nu generezi ORDER BY invalid
                        orders.add(asc ? cb.asc(v.get("id")) : cb.desc(v.get("id")));
                    }
                    default -> {
                        orders.add(asc ? cb.asc(v.get("id")) : cb.desc(v.get("id")));
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            orders.add(cb.desc(v.get("id")));
        }
        cq.orderBy(orders);

        var query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        var content = query.getResultList();

        // COUNT fără join-uri (rapid și corect)
        var cb2 = em.getCriteriaBuilder();
        var cc  = cb2.createQuery(Long.class);
        var v2  = cc.from(Volunteer.class);
        cc.select(cb2.count(v2));
        cc.where(cb2.equal(v2.get("postType"), TYPE));
        long total = em.createQuery(cc).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}