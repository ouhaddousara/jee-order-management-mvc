package ma.fstt.service;

import java.util.List;

import ma.fstt.entities.Produit;

/** Persistence contract of the product space. */
public interface IProduitDao extends IDao<Produit> {

    /** Products with at least one unit in stock: the choices offered on the order form. */
    List<Produit> findAvailable();
}
