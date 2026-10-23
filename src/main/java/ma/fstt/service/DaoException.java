package ma.fstt.service;

/**
 * Unchecked exception raised by the persistence layer.
 *
 * <p>It is unchecked on purpose: servlets cannot recover from a broken database
 * connection, so forcing every caller to declare {@code SQLException} would only
 * leak JDBC details into the controller. The message is written to be shown to the user.
 */
public class DaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DaoException(String message) {
        super(message);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
