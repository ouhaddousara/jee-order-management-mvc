package ma.fstt.service;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract shared by the DAO classes.
 *
 * <p>Servlets depend on these interfaces (injected with CDI), never on the JDBC
 * implementations: the storage technology can change without touching the controllers.
 *
 * @param <T> the entity type handled by the DAO
 */
public interface IDao<T> {

    /** Inserts the entity and sets its generated identifier. */
    void create(T entity);

    Optional<T> findById(Long id);

    List<T> findAll();

    /** @return true if a row was updated, false if no entity has this identifier */
    boolean update(T entity);

    /** @return true if a row was deleted, false if no entity has this identifier */
    boolean delete(Long id);
}
