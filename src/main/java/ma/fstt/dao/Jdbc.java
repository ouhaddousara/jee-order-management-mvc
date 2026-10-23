package ma.fstt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ma.fstt.service.DaoException;
import ma.fstt.service.InsufficientStockException;
import ma.fstt.service.IntegrityViolationException;

/**
 * Package-private JDBC plumbing shared by the DAO classes: connection handling,
 * transactions and translation of {@link SQLException} into the unchecked
 * {@link DaoException} hierarchy. It removes the try/catch/rollback boilerplate
 * that would otherwise be repeated in every method.
 */
final class Jdbc {

    /** A unit of work that returns a value. */
    @FunctionalInterface
    interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** A unit of work that returns nothing. */
    @FunctionalInterface
    interface SqlAction {
        void run(Connection c) throws SQLException;
    }

    /** Binds the parameters of a prepared statement. */
    @FunctionalInterface
    interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Jdbc() {
    }

    /** Runs a read-only unit of work on its own connection (auto-commit). */
    static <T> T read(DBConnection db, String action, SqlWork<T> work) {
        try (Connection c = db.getConnection()) {
            return work.run(c);
        } catch (SQLException e) {
            throw translate(action, e);
        }
    }

    /**
     * Runs a unit of work in a single transaction: commit if it succeeds, rollback
     * on any {@link SQLException} or {@link RuntimeException}, so a half-written
     * order can never be left in the database.
     */
    static <T> T write(DBConnection db, String action, SqlWork<T> work) {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw translate(action, e);
        }
    }

    /** Same as {@link #write} for work that returns nothing. */
    static void writeVoid(DBConnection db, String action, SqlAction work) {
        write(db, action, c -> {
            work.run(c);
            return null;
        });
    }

    /** Returns the auto-generated key of the row just inserted by {@code ps}. */
    static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("No generated key returned by the database");
        }
    }

    /**
     * Adds {@code delta} units to the stock of a product (negative to reserve, positive
     * to release). The condition {@code stock + delta >= 0} is evaluated by the database
     * in the same statement as the update, so two concurrent orders can never drive the
     * stock below zero.
     *
     * @throws InsufficientStockException if the stock would become negative
     */
    static void adjustStock(Connection c, long produitId, int delta) throws SQLException {
        String sql = "UPDATE produit SET stock = stock + ? WHERE id = ? AND stock + ? >= 0";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, produitId);
            ps.setInt(3, delta);
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException("Stock insuffisant pour le produit n° " + produitId);
            }
        }
    }

    /** Maps a MySQL/MariaDB error to a message that can be shown to the user. */
    static DaoException translate(String action, SQLException e) {
        switch (e.getErrorCode()) {
            case 1062: // ER_DUP_ENTRY
                return new IntegrityViolationException(
                        "Une valeur qui doit être unique existe déjà (par exemple l'e-mail d'un client).", e);
            case 1451: // ER_ROW_IS_REFERENCED_2
                return new IntegrityViolationException(
                        "Suppression impossible : cet enregistrement est encore utilisé par d'autres données.", e);
            case 1452: // ER_NO_REFERENCED_ROW_2
                return new IntegrityViolationException(
                        "L'enregistrement référencé (client, produit ou commande) n'existe pas.", e);
            case 3819: // ER_CHECK_CONSTRAINT_VIOLATED (MySQL)
            case 4025: // ER_CONSTRAINT_FAILED (MariaDB)
                return new IntegrityViolationException(
                        "Une valeur ne respecte pas les règles de la base (prix, stock ou quantité invalide).", e);
            default:
                return new DaoException("Erreur d'accès à la base de données (" + action + ") : " + e.getMessage(), e);
        }
    }

    private static void rollbackQuietly(Connection c, Exception cause) {
        try {
            c.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }
}
