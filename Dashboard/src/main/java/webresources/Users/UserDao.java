package webresources.Users;

// Imports ...

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(isolation=Isolation.READ_UNCOMMITTED)
public interface UserDao extends CrudRepository<UserResource, Long> {

    /**
     * This method will find an User instance in the database by its email.
     * Note that this method is not implemented and its working code will be
     * automagically generated from its signature by Spring Data JPA.
     */
    public UserResource findByEmail(String email);

    public UserResource findByEmailAndPassword(String email, String password);

    public UserResource findById(Long id);

    public List<UserResource> findAll();
}