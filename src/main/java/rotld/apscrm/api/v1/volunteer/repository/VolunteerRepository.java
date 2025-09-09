package rotld.apscrm.api.v1.volunteer.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Integer>, JpaSpecificationExecutor<Volunteer> {

    @Query("SELECT v FROM Volunteer v WHERE v.postType = 'aps_volunteer'")
    Page<Volunteer> findAll(Pageable pageable);

    @Query(
            value = """
      select v
      from Volunteer v
      left join v.meta age with age.metaKey = '_vol_varsta'
      where v.postType = 'aps_volunteer'
      order by cast(age.metaValue as integer) asc, v.id asc
    """,
            countQuery = """
      select count(v)
      from Volunteer v
      where v.postType = 'aps_volunteer'
    """
    )
    Page<Volunteer> findAllOrderByAgeAsc(Pageable pageable);

    @Query(
            value = """
      select v
      from Volunteer v
      left join v.meta age with age.metaKey = '_vol_varsta'
      where v.postType = 'aps_volunteer'
      order by cast(age.metaValue as integer) desc, v.id desc
    """,
            countQuery = """
      select count(v)
      from Volunteer v
      where v.postType = 'aps_volunteer'
    """
    )
    Page<Volunteer> findAllOrderByAgeDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM VolunteerMeta vm WHERE vm.volunteer.id = :postId")
    int deleteAllByVolunteerId(@Param("postId") Integer postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Volunteer v WHERE v.id = :id")
    int hardDeleteById(Integer id);
}
