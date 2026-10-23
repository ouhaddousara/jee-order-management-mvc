package ma.fstt.service;

import java.util.List;
import java.util.Optional;

import ma.fstt.entities.LigneDeCommande;

/**
 * Persistence contract of the order lines.
 *
 * <p>Lines can only be changed while their order is EN_ATTENTE. Every method keeps
 * the product stock consistent with the quantities ordered.
 */
public interface ILigneCommandeDao {

    List<LigneDeCommande> findByCommande(Long commandeId);

    Optional<LigneDeCommande> findById(Long ligneId);

    /**
     * Adds a product to an order. If the product is already on the order, the
     * quantity is increased (the schema allows one line per product and order).
     *
     * @throws InsufficientStockException if the stock is too low
     */
    void add(Long commandeId, Long produitId, int quantite);

    /**
     * Sets the new quantity of a line and adjusts the stock by the difference.
     *
     * @return false if the line does not exist
     */
    boolean updateQuantite(Long ligneId, int nouvelleQuantite);

    /**
     * Removes a line and gives its quantity back to the stock.
     *
     * @return false if the line does not exist
     */
    boolean remove(Long ligneId);
}
