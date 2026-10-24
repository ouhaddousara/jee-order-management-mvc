package ma.fstt.web;

import java.io.Serializable;

/**
 * One-shot message shown after a redirect (Post/Redirect/Get pattern). It is stored in
 * the session by a servlet and removed by the layout once displayed.
 * Public with getters so that the JSP can read it with EL: {@code ${sessionScope.flash.message}}.
 */
public final class Flash implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String message;

    Flash(String type, String message) {
        this.type = type;
        this.message = message;
    }

    /** CSS class suffix: "success", "error" or "info". */
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
