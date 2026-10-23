package ma.fstt.service;

import java.util.Optional;

import ma.fstt.entities.Client;

/** Persistence contract of the client space. */
public interface IClientDao extends IDao<Client> {

    /** Looks a client up by its (unique) e-mail address. */
    Optional<Client> findByEmail(String email);
}
