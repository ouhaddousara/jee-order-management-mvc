package ma.fstt.entities;

/**
 * Lifecycle of an order.
 *
 * <p>The allowed transitions are encoded here (and not in the servlets) so that
 * the rule lives in one place:
 * <pre>
 *   EN_ATTENTE -> VALIDEE -> EXPEDIEE
 *        |           |
 *        +---------> ANNULEE   (terminal, releases the reserved stock)
 * </pre>
 * The enum names match the values accepted by the CHECK constraint of the
 * {@code commande.statut} column (see db/schema.sql).
 */
public enum StatutCommande {

    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    EXPEDIEE("Expédiée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutCommande(String libelle) {
        this.libelle = libelle;
    }

    /** Human readable label, used by the JSP pages. */
    public String getLibelle() {
        return libelle;
    }

    /** Returns true if an order currently in this status may move to {@code cible}. */
    public boolean peutPasserA(StatutCommande cible) {
        switch (this) {
            case EN_ATTENTE:
                return cible == VALIDEE || cible == ANNULEE;
            case VALIDEE:
                return cible == EXPEDIEE || cible == ANNULEE;
            default:
                return false; // EXPEDIEE and ANNULEE are terminal
        }
    }
}
