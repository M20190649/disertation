package webresources.Locations;

// Imports ...

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(isolation=Isolation.READ_UNCOMMITTED)
public interface LocationDao extends CrudRepository<LocationResource, Long> {

    public LocationResource findById(long id);

    public List<LocationResource> findAll();
}