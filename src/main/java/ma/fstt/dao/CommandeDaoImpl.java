package ma.fstt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import ma.fstt.entities.Client;
import ma.fstt.entities.Commande;
import ma.fstt.entities.LigneDeCommande;
import ma.fstt.entities.StatutCommande;
import ma.fstt.service.DaoException;
import ma.fstt.service.ICommandeDao;

/**
 * JDBC implementation of {@link ICommandeDao}.
 *
 * <p>Every operation that touches an order, its lines and the product stock runs in
 * one transaction (see {@link Jdbc#write}): either everything is saved or nothing is.
 */
@ApplicationScoped
public class CommandeDaoImpl implements ICommandeDao {

    private static final String SELECT_COMMANDES =
            "SELECT c.id, c.date_commande, c.statut, "
                    + "cl.id AS client_id, cl.nom, cl.prenom, cl.email, cl.telephone, cl.adresse "
                    + "FROM commande c JOIN client cl ON cl.id = c.client_id ";
    private static final String ORDER_BY = " ORDER BY c.date_commande DESC, c.id DESC";

    private DBConnection db;

    /** Required by CDI to build the client proxy of an {@code @ApplicationScoped} bean. */
    protected CommandeDaoImpl() {
    }

    @Inject
    public CommandeDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void create(Commande commande) {
        validateNewOrder(commande);
        Jdbc.writeVoid(db, "création de la commande", c -> {
            long id = insertHeader(c, commande);

            // Process products in a fixed order (by id) so that two concurrent orders
            // lock the product rows in the same sequence and cannot deadlock.
            List<LigneDeCommande> sorted = new ArrayList<>(commande.getLignes());
            sorted.sort(Comparator.comparing((LigneDeCommande l) -> l.getProduit().getId()));
            for (LigneDeCommande ligne : sorted) {
                LigneCommandeDaoImpl.upsert(c, id, ligne.getProduit().getId(), ligne.getQuantite());
            }

            // Hand back what is really in the database (ids, price snapshots, merged duplicates).
            Commande saved = loadOne(c, id);
            commande.setId(saved.getId());
            commande.setDateCommande(saved.getDateCommande());
            commande.setStatut(saved.getStatut());
            commande.setLignes(saved.getLignes());
        });
    }

    @Override
    public Optional<Commande> findById(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.read(db, "lecture de la commande", c -> {
            List<Commande> found = fetch(c, "WHERE c.id = ?", ps -> ps.setLong(1, id));
            return found.isEmpty() ? Optional.<Commande>empty() : Optional.of(found.get(0));
        });
    }

    @Override
    public List<Commande> findAll() {
        return Jdbc.read(db, "liste des commandes", c -> fetch(c, "", ps -> {
        }));
    }

    @Override
    public List<Commande> findByClient(Long clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return Jdbc.read(db, "liste des commandes du client",
                c -> fetch(c, "WHERE c.client_id = ?", ps -> ps.setLong(1, clientId)));
    }

    @Override
    public boolean update(Commande commande) {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(commande.getId(), "commande.id");
        if (commande.getClient() == null || commande.getClient().getId() == null) {
            throw new IllegalArgumentException("Le client de la commande est obligatoire.");
        }
        LocalDate date = commande.getDateCommande() != null ? commande.getDateCommande() : LocalDate.now();
        return Jdbc.write(db, "modification de la commande", c -> {
            StatutCommande statut = lockStatut(c, commande.getId());
            if (statut == null) {
                return false;
            }
            if (statut != StatutCommande.EN_ATTENTE) {
                throw new DaoException("Seule une commande en attente peut être modifiée.");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE commande SET date_commande = ?, client_id = ? WHERE id = ?")) {
                ps.setObject(1, date);
                ps.setLong(2, commande.getClient().getId());
                ps.setLong(3, commande.getId());
                ps.executeUpdate();
            }
            return true;
        });
    }

    @Override
    public boolean delete(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.write(db, "suppression de la commande", c -> {
            StatutCommande statut = lockStatut(c, id);
            if (statut == null) {
                return false;
            }
            if (statut == StatutCommande.VALIDEE || statut == StatutCommande.EXPEDIEE) {
                throw new DaoException("Seules les commandes en attente ou annulées peuvent être supprimées.");
            }
            if (statut == StatutCommande.EN_ATTENTE) {
                releaseStock(c, id); // a cancelled order has already given its stock back
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM commande WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate(); // the lines go away with it (ON DELETE CASCADE)
            }
            return true;
        });
    }

    @Override
    public boolean changerStatut(Long commandeId, StatutCommande nouveauStatut) {
        Objects.requireNonNull(commandeId, "commandeId");
        Objects.requireNonNull(nouveauStatut, "nouveauStatut");
        return Jdbc.write(db, "changement de statut", c -> {
            StatutCommande actuel = lockStatut(c, commandeId);
            if (actuel == null) {
                return false;
            }
            if (!actuel.peutPasserA(nouveauStatut)) {
                throw new DaoException("Changement de statut interdit : " + actuel.getLibelle()
                        + " → " + nouveauStatut.getLibelle() + ".");
            }
            if (nouveauStatut == StatutCommande.ANNULEE) {
                releaseStock(c, commandeId);
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE commande SET statut = ? WHERE id = ?")) {
                ps.setString(1, nouveauStatut.name());
                ps.setLong(2, commandeId);
                ps.executeUpdate();
            }
            return true;
        });
    }

    // ------------------------------------------------------------------
    // Package-private helper shared with LigneCommandeDaoImpl
    // ------------------------------------------------------------------

    /**
     * Reads the status of an order and locks its row until the end of the transaction.
     *
     * @return the status, or null if the order does not exist
     */
    static StatutCommande lockStatut(Connection c, long commandeId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT statut FROM commande WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? StatutCommande.valueOf(rs.getString(1)) : null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private static void validateNewOrder(Commande commande) {
        if (commande == null) {
            throw new IllegalArgumentException("La commande est obligatoire.");
        }
        Client client = commande.getClient();
        if (client == null || client.getId() == null) {
            throw new IllegalArgumentException("Le client de la commande est obligatoire.");
        }
        if (commande.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Une commande doit contenir au moins une ligne.");
        }
        for (LigneDeCommande ligne : commande.getLignes()) {
            if (ligne.getProduit() == null || ligne.getProduit().getId() == null) {
                throw new IllegalArgumentException("Chaque ligne doit référencer un produit existant.");
            }
            if (ligne.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité doit être strictement positive.");
            }
        }
    }

    /** A new order always starts as EN_ATTENTE, whatever the caller put in the entity. */
    private static long insertHeader(Connection c, Commande commande) throws SQLException {
        LocalDate date = commande.getDateCommande() != null ? commande.getDateCommande() : LocalDate.now();
        String sql = "INSERT INTO commande (date_commande, statut, client_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, date);
            ps.setString(2, StatutCommande.EN_ATTENTE.name());
            ps.setLong(3, commande.getClient().getId());
            ps.executeUpdate();
            return Jdbc.generatedId(ps);
        }
    }

    private static Commande loadOne(Connection c, long id) throws SQLException {
        List<Commande> found = fetch(c, "WHERE c.id = ?", ps -> ps.setLong(1, id));
        if (found.isEmpty()) {
            throw new DaoException("Commande introuvable après création : " + id);
        }
        return found.get(0);
    }

    /**
     * Loads orders matching {@code where} with their client, then all their lines in one
     * extra query (2 queries in total, whatever the number of orders: no N+1 problem).
     */
    private static List<Commande> fetch(Connection c, String where, Jdbc.SqlBinder binder) throws SQLException {
        Map<Long, Commande> byId = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_COMMANDES + where + ORDER_BY)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commande commande = mapHeader(rs);
                    byId.put(commande.getId(), commande);
                }
            }
        }
        LigneCommandeDaoImpl.loadInto(c, byId);
        return new ArrayList<>(byId.values());
    }

    /** Gives the quantities of all the lines of an order back to the product stock. */
    private static void releaseStock(Connection c, long commandeId) throws SQLException {
        List<long[]> lines = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT produit_id, quantite FROM ligne_commande WHERE commande_id = ? ORDER BY produit_id")) {
            ps.setLong(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new long[] { rs.getLong(1), rs.getLong(2) });
                }
            }
        }
        for (long[] line : lines) {
            Jdbc.adjustStock(c, line[0], (int) line[1]);
        }
    }

    private static Commande mapHeader(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("client_id"));
        client.setNom(rs.getString("nom"));
        client.setPrenom(rs.getString("prenom"));
        client.setEmail(rs.getString("email"));
        client.setTelephone(rs.getString("telephone"));
        client.setAdresse(rs.getString("adresse"));

        Commande commande = new Commande();
        commande.setId(rs.getLong("id"));
        commande.setDateCommande(rs.getObject("date_commande", LocalDate.class));
        commande.setStatut(StatutCommande.valueOf(rs.getString("statut")));
        commande.setClient(client);
        return commande;
    }
}
