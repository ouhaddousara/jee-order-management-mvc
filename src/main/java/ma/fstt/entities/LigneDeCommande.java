package ma.fstt.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One line of an order: a product, a quantity and the unit price that applied
 * when the line was created.
 *
 * <p>{@code prixUnitaire} is a snapshot of {@link Produit#getPrix()}. Keeping it on the
 * line means a later price change never rewrites the history of past orders.
 */
public class LigneDeCommande implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Produit produit;
    private int quantite;
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    public LigneDeCommande() {
    }

    public LigneDeCommande(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = quantite;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    /** Line total = unit price x quantity. Computed, not stored. */
    public BigDecimal getSousTotal() {
        if (prixUnitaire == null) {
            return BigDecimal.ZERO;
        }
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LigneDeCommande)) {
            return false;
        }
        LigneDeCommande other = (LigneDeCommande) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "LigneDeCommande{id=" + id + ", produit=" + (produit == null ? null : produit.getId())
                + ", quantite=" + quantite + ", prixUnitaire=" + prixUnitaire + "}";
    }
}
