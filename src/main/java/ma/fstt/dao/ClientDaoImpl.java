package ma.fstt.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import ma.fstt.entities.Client;
import ma.fstt.service.IClientDao;

/** JDBC implementation of {@link IClientDao}. */
@ApplicationScoped
public class ClientDaoImpl implements IClientDao {

    private static final String COLUMNS = "id, nom, prenom, email, telephone, adresse";

    private DBConnection db;

    /** Required by CDI to build the client proxy of an {@code @ApplicationScoped} bean. */
    protected ClientDaoImpl() {
    }

    @Inject
    public ClientDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void create(Client client) {
        Objects.requireNonNull(client, "client");
        String sql = "INSERT INTO client (nom, prenom, email, telephone, adresse) VALUES (?, ?, ?, ?, ?)";
        Jdbc.writeVoid(db, "création du client", c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, client);
                ps.executeUpdate();
                client.setId(Jdbc.generatedId(ps));
            }
        });
    }

    @Override
    public Optional<Client> findById(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.read(db, "lecture du client", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM client WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Client>empty();
                }
            }
        });
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        Objects.requireNonNull(email, "email");
        return Jdbc.read(db, "recherche du client par e-mail", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM client WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Client>empty();
                }
            }
        });
    }

    @Override
    public List<Client> findAll() {
        return Jdbc.read(db, "liste des clients", c -> {
            List<Client> clients = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM client ORDER BY nom, prenom");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(map(rs));
                }
            }
            return clients;
        });
    }

    @Override
    public boolean update(Client client) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(client.getId(), "client.id");
        String sql = "UPDATE client SET nom = ?, prenom = ?, email = ?, telephone = ?, adresse = ? WHERE id = ?";
        return Jdbc.write(db, "modification du client", c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, client);
                ps.setLong(6, client.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean delete(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.write(db, "suppression du client", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM client WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private static void bind(PreparedStatement ps, Client client) throws SQLException {
        ps.setString(1, client.getNom());
        ps.setString(2, client.getPrenom());
        ps.setString(3, client.getEmail());
        ps.setString(4, client.getTelephone());
        ps.setString(5, client.getAdresse());
    }

    private static Client map(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setNom(rs.getString("nom"));
        client.setPrenom(rs.getString("prenom"));
        client.setEmail(rs.getString("email"));
        client.setTelephone(rs.getString("telephone"));
        client.setAdresse(rs.getString("adresse"));
        return client;
    }
}
