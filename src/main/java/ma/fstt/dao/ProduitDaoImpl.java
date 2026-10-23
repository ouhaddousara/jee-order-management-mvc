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

import ma.fstt.entities.Produit;
import ma.fstt.service.IProduitDao;

/** JDBC implementation of {@link IProduitDao}. */
@ApplicationScoped
public class ProduitDaoImpl implements IProduitDao {

    private static final String COLUMNS = "id, designation, description, prix, stock";

    private DBConnection db;

    /** Required by CDI to build the client proxy of an {@code @ApplicationScoped} bean. */
    protected ProduitDaoImpl() {
    }

    @Inject
    public ProduitDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void create(Produit produit) {
        Objects.requireNonNull(produit, "produit");
        String sql = "INSERT INTO produit (designation, description, prix, stock) VALUES (?, ?, ?, ?)";
        Jdbc.writeVoid(db, "création du produit", c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, produit);
                ps.executeUpdate();
                produit.setId(Jdbc.generatedId(ps));
            }
        });
    }

    @Override
    public Optional<Produit> findById(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.read(db, "lecture du produit", c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM produit WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Produit>empty();
                }
            }
        });
    }

    @Override
    public List<Produit> findAll() {
        return query("liste des produits", "SELECT " + COLUMNS + " FROM produit ORDER BY designation");
    }

    @Override
    public List<Produit> findAvailable() {
        return query("liste des produits disponibles",
                "SELECT " + COLUMNS + " FROM produit WHERE stock > 0 ORDER BY designation");
    }

    @Override
    public boolean update(Produit produit) {
        Objects.requireNonNull(produit, "produit");
        Objects.requireNonNull(produit.getId(), "produit.id");
        // Changing the price here never touches existing order lines: they keep their own price snapshot.
        String sql = "UPDATE produit SET designation = ?, description = ?, prix = ?, stock = ? WHERE id = ?";
        return Jdbc.write(db, "modification du produit", c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, produit);
                ps.setLong(5, produit.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean delete(Long id) {
        Objects.requireNonNull(id, "id");
        return Jdbc.write(db, "suppression du produit", c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM produit WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private List<Produit> query(String action, String sql) {
        return Jdbc.read(db, action, c -> {
            List<Produit> produits = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produits.add(map(rs));
                }
            }
            return produits;
        });
    }

    private static void bind(PreparedStatement ps, Produit produit) throws SQLException {
        ps.setString(1, produit.getDesignation());
        ps.setString(2, produit.getDescription());
        ps.setBigDecimal(3, produit.getPrix());
        ps.setInt(4, produit.getStock());
    }

    /** Maps the columns of the {@code produit} table (no alias). */
    private static Produit map(ResultSet rs) throws SQLException {
        Produit produit = new Produit();
        produit.setId(rs.getLong("id"));
        produit.setDesignation(rs.getString("designation"));
        produit.setDescription(rs.getString("description"));
        produit.setPrix(rs.getBigDecimal("prix"));
        produit.setStock(rs.getInt("stock"));
        return produit;
    }
}
