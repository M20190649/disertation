package webresources.UsersLocations;

// Imports ...

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(isolation=Isolation.READ_UNCOMMITTED)
public interface UserLocationDao extends CrudRepository<UserLocationResources, Long> {

    public List<UserLocationResources> findByUserId(long id);

    public List<UserLocationResources> findByLocationId(long id);

    UserLocationResources findByUserIdAndLocationId(long userId, long locationId);
}