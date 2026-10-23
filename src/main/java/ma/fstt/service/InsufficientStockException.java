package ma.fstt.service;

/** Raised when an order line asks for more units than the product has in stock. */
public class InsufficientStockException extends DaoException {

    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super(message);
    }
}
