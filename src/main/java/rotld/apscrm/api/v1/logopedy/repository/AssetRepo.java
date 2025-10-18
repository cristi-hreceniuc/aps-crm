package rotld.apscrm.api.v1.logopedy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rotld.apscrm.api.v1.logopedy.entities.Asset;
import rotld.apscrm.api.v1.logopedy.entities.Module;

import java.util.List;

public interface AssetRepo extends JpaRepository<Asset, Long> {
}