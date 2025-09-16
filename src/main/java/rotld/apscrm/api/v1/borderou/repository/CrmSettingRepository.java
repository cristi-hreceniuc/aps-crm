package rotld.apscrm.api.v1.borderou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CrmSettingRepository extends JpaRepository<CrmSetting, Integer> {
    Optional<CrmSetting> findByName(String name);
    List<CrmSetting> findByNameIn(Collection<String> names);
}