package ma.fstt.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Small helpers shared by the controllers: view rendering, redirects, flash messages
 * and tolerant parsing of request parameters (every parse method returns null on
 * invalid input, and the servlet turns that into a field error).
 */
final class Web {

    /** JSP pages live under WEB-INF so they can only be reached through a servlet. */
    private static final String VIEWS = "/WEB-INF/views/";

    private Web() {
    }

    static void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEWS + view).forward(req, resp);
    }

    /** Redirects to an application path (the context path is added here). */
    static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    static void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flash", new Flash(type, message));
    }

    /** The part of the URL after the servlet mapping, "/" when there is none. */
    static String action(HttpServletRequest req) {
        String path = req.getPathInfo();
        return (path == null || path.isEmpty()) ? "/" : path;
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static Long parseLong(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Integer parseInt(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Accepts "12.5" and "12,5" (French keyboards); returns null if it is not a number. */
    static BigDecimal parseMoney(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses an ISO date (yyyy-MM-dd, the format of {@code <input type="date">}). */
    static LocalDate parseDate(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
