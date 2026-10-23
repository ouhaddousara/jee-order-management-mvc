package ma.fstt.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A product of the catalogue. The price is a {@link BigDecimal}: money must never
 * be stored in a float/double because of binary rounding errors.
 */
public class Produit implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String designation;
    private String description;
    private BigDecimal prix = BigDecimal.ZERO;
    private int stock;

    public Produit() {
    }

    public Produit(String designation, String description, BigDecimal prix, int stock) {
        this.designation = designation;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produit)) {
            return false;
        }
        Produit other = (Produit) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Produit{id=" + id + ", designation='" + designation + "', prix=" + prix + ", stock=" + stock + "}";
    }
}
