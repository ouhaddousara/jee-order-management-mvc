package ma.fstt.service;

/**
 * Raised when the database rejects a write because of an integrity rule:
 * duplicate unique value (e.g. client e-mail), foreign key still referenced
 * (e.g. deleting a client who has orders) or a CHECK constraint.
 */
public class IntegrityViolationException extends DaoException {

    private static final long serialVersionUID = 1L;

    public IntegrityViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
