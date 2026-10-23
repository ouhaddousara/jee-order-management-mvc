package ma.fstt.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An order placed by one {@link Client}. It owns its {@link LigneDeCommande} lines
 * (UML composition): a line cannot exist without its order.
 */
public class Commande implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDate dateCommande = LocalDate.now();
    private StatutCommande statut = StatutCommande.EN_ATTENTE;
    private Client client;
    
    private List<LigneDeCommande> lignes = new ArrayList<>();

    public Commande() {
    }

    public Commande(Client client) {
        this.client = client;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDate dateCommande) {
        this.dateCommande = dateCommande;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<LigneDeCommande> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneDeCommande> lignes) {
        this.lignes = (lignes == null) ? new ArrayList<>() : lignes;
    }

    /** Adds a line to this order (convenience for building a new order before saving it). */
    public void ajouterLigne(Produit produit, int quantite) {
        lignes.add(new LigneDeCommande(produit, quantite));
    }

    /** Order total = sum of the line totals. Computed, not stored. */
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneDeCommande ligne : lignes) {
            total = total.add(ligne.getSousTotal());
        }
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Commande)) {
            return false;
        }
        Commande other = (Commande) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Commande{id=" + id + ", date=" + dateCommande + ", statut=" + statut
                + ", client=" + (client == null ? null : client.getId()) + ", lignes=" + lignes.size() + "}";
    }
}
