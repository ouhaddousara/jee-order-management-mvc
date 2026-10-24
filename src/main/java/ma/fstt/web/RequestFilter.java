package ma.fstt.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Cross-cutting concerns applied to every request, in one place:
 * <ul>
 *   <li>UTF-8 decoding of form data (must happen before the first getParameter call);</li>
 *   <li>basic hardening headers;</li>
 *   <li>CSRF protection: each session gets a random token, every POST must send it back
 *       in the hidden field {@code _csrf}. A malicious site cannot read the token, so it
 *       cannot forge a delete or a status change on behalf of a logged-in user.</li>
 * </ul>
 */
@WebFilter(filterName = "RequestFilter", urlPatterns = "/*")
public class RequestFilter implements Filter {

    /** Session attribute holding the token; the forms read it as {@code ${csrfToken}}. */
    public static final String CSRF_ATTRIBUTE = "csrfToken";
    public static final String CSRF_PARAMETER = "_csrf";

    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding("UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        String path = req.getServletPath();
        if (path.startsWith("/assets/") || path.equals("/favicon.ico")) {
            chain.doFilter(req, resp); // static files: no session, no token
            return;
        }

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(CSRF_ATTRIBUTE);
        if (token == null) {
            token = newToken();
            session.setAttribute(CSRF_ATTRIBUTE, token);
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String sent = req.getParameter(CSRF_PARAMETER);
            // Constant-time comparison: do not leak how many characters matched.
            if (sent == null || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8), sent.getBytes(StandardCharsets.UTF_8))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton de sécurité invalide ou expiré.");
                return;
            }
        }
        chain.doFilter(req, resp);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
