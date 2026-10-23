package ma.fstt.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import ma.fstt.entities.Commande;
import ma.fstt.entities.LigneDeCommande;
import ma.fstt.entities.Produit;
import ma.fstt.entities.StatutCommande;
import ma.fstt.service.DaoException;
import ma.fstt.service.ILigneCommandeDao;

/**
 * JDBC implementation of {@link ILigneCommandeDao}.
 *
 * <p>Locking convention used by the whole persistence layer, to avoid deadlocks:
 * always lock the order row first, then the product rows. Every method that changes
 * a line therefore starts with {@link #requireEditable}, which locks the order.
 */
@ApplicationScoped
public class LigneCommandeDaoImpl implements ILigneCommandeDao {

    private static final String SELECT_LIGNES =
            "SELECT l.id AS ligne_id, l.commande_id, l.quantite, l.prix_unitaire, "
                    + "p.id AS produit_id, p.designation, p.description, p.prix, p.stock "
                    + "FROM ligne_commande l JOIN produit p ON p.id = l.produit_id ";

    private DBConnection db;

    /** Required by CDI to build the client proxy of an {@code @ApplicationScoped} bean. */
    protected LigneCommandeDaoImpl() {
    }

    @Inject
    public LigneCommandeDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public List<LigneDeCommande> findByCommande(Long commandeId) {
        Objects.requireNonNull(commandeId, "commandeId");
        return Jdbc.read(db, "lecture des lignes", c -> {
            List<LigneDeCommande> lignes = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SELECT_LIGNES + "WHERE l.commande_id = ? ORDER BY l.id")) {
                ps.setLong(1, commandeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lignes.add(map(rs));
                    }
                }
            }
            return lignes;
        });
    }

    @Override
    public Optional<LigneDeCommande> findById(Long ligneId) {
        Objects.requireNonNull(ligneId, "ligneId");
        return Jdbc.read(db, "lecture de la ligne", c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_LIGNES + "WHERE l.id = ?")) {
                ps.setLong(1, ligneId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<LigneDeCommande>empty();
                }
            }
        });
    }

    @Override
    public void add(Long commandeId, Long produitId, int quantite) {
        Objects.requireNonNull(commandeId, "commandeId");
        Objects.requireNonNull(produitId, "produitId");
        requirePositive(quantite);
        Jdbc.writeVoid(db, "ajout d'une ligne", c -> {
            requireEditable(c, commandeId);
            upsert(c, commandeId, produitId, quantite);
        });
    }

    @Override
    public boolean updateQuantite(Long ligneId, int nouvelleQuantite) {
        Objects.requireNonNull(ligneId, "ligneId");
        requirePositive(nouvelleQuantite);
        return Jdbc.write(db, "modification d'une ligne", c -> {
            long[] line = readRaw(c, ligneId);
            if (line == null) {
                return false;
            }
            requireEditable(c, line[0]);
            line = readRaw(c, ligneId); // re-read now that the order is locked
            if (line == null) {
                return false;
            }
            int delta = nouvelleQuantite - (int) line[2];
            if (delta != 0) {
                Jdbc.adjustStock(c, line[1], -delta);
                try (PreparedStatement ps = c.prepareStatement("UPDATE ligne_commande SET quantite = ? WHERE id = ?")) {
                    ps.setInt(1, nouvelleQuantite);
                    ps.setLong(2, ligneId);
                    ps.executeUpdate();
                }
            }
            return true;
        });
    }

    @Override
    public boolean remove(Long ligneId) {
        Objects.requireNonNull(ligneId, "ligneId");
        return Jdbc.write(db, "suppression d'une ligne", c -> {
            long[] line = readRaw(c, ligneId);
            if (line == null) {
                return false;
            }
            requireEditable(c, line[0]);
            line = readRaw(c, ligneId); // re-read now that the order is locked
            if (line == null) {
                return false;
            }
            if (countLines(c, line[0]) <= 1) {
                throw new DaoException("Une commande doit contenir au moins une ligne : "
                        + "supprimez la commande plutôt que sa dernière ligne.");
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM ligne_commande WHERE id = ?")) {
                ps.setLong(1, ligneId);
                ps.executeUpdate();
            }
            Jdbc.adjustStock(c, line[1], (int) line[2]); // give the units back
            return true;
        });
    }

    // ------------------------------------------------------------------
    // Package-private helpers, also used by CommandeDaoImpl (same transaction)
    // ------------------------------------------------------------------

    /**
     * Adds {@code quantite} units of a product to an order and reserves them in stock.
     * The unit price is read from the product (row locked) and stored on the line as a
     * snapshot: the caller can never choose the price. If the product is already on the
     * order the quantity is increased, because the schema allows one line per product.
     */
    static void upsert(java.sql.Connection c, long commandeId, long produitId, int quantite) throws SQLException {
        BigDecimal prix;
        try (PreparedStatement ps = c.prepareStatement("SELECT prix FROM produit WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DaoException("Produit introuvable : " + produitId);
                }
                prix = rs.getBigDecimal(1);
            }
        }

        int updated;
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE ligne_commande SET quantite = quantite + ? WHERE commande_id = ? AND produit_id = ?")) {
            ps.setInt(1, quantite);
            ps.setLong(2, commandeId);
            ps.setLong(3, produitId);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            String insert = "INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire) "
                    + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(insert, Statement.NO_GENERATED_KEYS)) {
                ps.setLong(1, commandeId);
                ps.setLong(2, produitId);
                ps.setInt(3, quantite);
                ps.setBigDecimal(4, prix);
                ps.executeUpdate();
            }
        }
        Jdbc.adjustStock(c, produitId, -quantite);
    }

    /** Loads the lines (with their products) of the given orders and attaches them. */
    static void loadInto(java.sql.Connection c, Map<Long, Commande> commandes) throws SQLException {
        if (commandes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(commandes.size(), "?"));
        String sql = SELECT_LIGNES + "WHERE l.commande_id IN (" + placeholders + ") ORDER BY l.commande_id, l.id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int index = 1;
            for (Long id : commandes.keySet()) {
                ps.setLong(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    commandes.get(rs.getLong("commande_id")).getLignes().add(map(rs));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /** Locks the order and checks that its lines may still be changed. */
    private static void requireEditable(java.sql.Connection c, long commandeId) throws SQLException {
        StatutCommande statut = CommandeDaoImpl.lockStatut(c, commandeId);
        if (statut == null) {
            throw new DaoException("Commande introuvable : " + commandeId);
        }
        if (statut != StatutCommande.EN_ATTENTE) {
            throw new DaoException("Les lignes ne sont modifiables que tant que la commande est en attente.");
        }
    }

    /** Returns {commande_id, produit_id, quantite} of a line, or null if it does not exist. */
    private static long[] readRaw(java.sql.Connection c, long ligneId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT commande_id, produit_id, quantite FROM ligne_commande WHERE id = ?")) {
            ps.setLong(1, ligneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new long[] { rs.getLong(1), rs.getLong(2), rs.getLong(3) };
            }
        }
    }

    private static int countLines(java.sql.Connection c, long commandeId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ligne_commande WHERE commande_id = ?")) {
            ps.setLong(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void requirePositive(int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }
    }

    /** Maps a row of {@link #SELECT_LIGNES} (aliases ligne_id and produit_id) to an entity. */
    private static LigneDeCommande map(ResultSet rs) throws SQLException {
        Produit produit = new Produit();
        produit.setId(rs.getLong("produit_id"));
        produit.setDesignation(rs.getString("designation"));
        produit.setDescription(rs.getString("description"));
        produit.setPrix(rs.getBigDecimal("prix"));
        produit.setStock(rs.getInt("stock"));

        LigneDeCommande ligne = new LigneDeCommande();
        ligne.setId(rs.getLong("ligne_id"));
        ligne.setProduit(produit);
        ligne.setQuantite(rs.getInt("quantite"));
        ligne.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        return ligne;
    }
}
