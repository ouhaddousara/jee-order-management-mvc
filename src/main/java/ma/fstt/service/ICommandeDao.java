package ma.fstt.service;

import java.util.List;

import ma.fstt.entities.Commande;
import ma.fstt.entities.StatutCommande;

/**
 * Persistence contract of the order space.
 *
 * <p>Business rules enforced by the implementation (all inside one transaction):
 * <ul>
 *   <li>a new order starts as EN_ATTENTE and must contain at least one line;</li>
 *   <li>stock is reserved when a line is created and released when the order is
 *       cancelled or deleted;</li>
 *   <li>the unit price of a line is read from the product at creation time, never
 *       from the caller;</li>
 *   <li>status changes follow {@link StatutCommande#peutPasserA(StatutCommande)}.</li>
 * </ul>
 * Orders returned by the finders are fully loaded (client and lines with their products).
 */
public interface ICommandeDao extends IDao<Commande> {

    /**
     * Inserts the order with all its lines and reserves the stock. On return the
     * order has its identifier and its lines carry their identifiers and price snapshots.
     *
     * @throws InsufficientStockException if one product does not have enough stock
     */
    @Override
    void create(Commande commande);

    /**
     * Updates the header (client and date) of a pending order. Lines are changed
     * through {@link ILigneCommandeDao}, the status through {@link #changerStatut}.
     *
     * @throws DaoException if the order is no longer EN_ATTENTE
     */
    @Override
    boolean update(Commande commande);

    /**
     * Deletes a pending (stock is released) or cancelled order.
     *
     * @throws DaoException if the order is validated or shipped
     */
    @Override
    boolean delete(Long id);

    List<Commande> findByClient(Long clientId);

    /**
     * Moves an order to a new status.
     *
     * @return false if the order does not exist
     * @throws DaoException if the transition is not allowed
     */
    boolean changerStatut(Long commandeId, StatutCommande nouveauStatut);
}
