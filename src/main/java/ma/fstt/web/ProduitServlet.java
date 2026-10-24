package ma.fstt.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Produit;
import ma.fstt.service.DaoException;
import ma.fstt.service.IProduitDao;

/**
 * Controller of the product space.
 *
 * <pre>
 *   GET  /produits           list             POST /produits/save    create or update
 *   GET  /produits/new       empty form       POST /produits/delete  delete
 *   GET  /produits/edit?id=  form
 * </pre>
 */
@WebServlet(name = "ProduitServlet", urlPatterns = "/produits/*")
public class ProduitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    /** Largest value of the DECIMAL(10,2) column. */
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    @Inject
    private IProduitDao produitDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (Web.action(req)) {
            case "/":
                req.setAttribute("produits", produitDao.findAll());
                Web.render(req, resp, "produits/list.jsp");
                break;
            case "/new":
                showForm(req, resp, new Produit(), new LinkedHashMap<>());
                break;
            case "/edit":
                Long id = Web.parseLong(req.getParameter("id"));
                Optional<Produit> existing = id == null ? Optional.empty() : produitDao.findById(id);
                if (existing.isPresent()) {
                    showForm(req, resp, existing.get(), new LinkedHashMap<>());
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (Web.action(req)) {
            case "/save":
                save(req, resp);
                break;
            case "/delete":
                delete(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Produit produit = new Produit();
        produit.setId(Web.parseLong(req.getParameter("id")));
        produit.setDesignation(Web.trimToNull(req.getParameter("designation")));
        produit.setDescription(Web.trimToNull(req.getParameter("description")));

        Map<String, String> errors = new LinkedHashMap<>();
        validateDesignation(produit, errors);
        BigDecimal prix = Web.parseMoney(req.getParameter("prix"));
        if (prix == null) {
            errors.put("prix", "Le prix est obligatoire (nombre, par exemple 149.90).");
        } else if (prix.signum() < 0) {
            errors.put("prix", "Le prix ne peut pas être négatif.");
        } else if (prix.scale() > 2 && prix.stripTrailingZeros().scale() > 2) {
            errors.put("prix", "2 décimales maximum.");
        } else if (prix.compareTo(MAX_PRICE) > 0) {
            errors.put("prix", "Prix trop élevé.");
        } else {
            produit.setPrix(prix.setScale(2, RoundingMode.UNNECESSARY));
        }
        Integer stock = Web.parseInt(req.getParameter("stock"));
        if (stock == null || stock < 0) {
            errors.put("stock", "Le stock doit être un entier supérieur ou égal à 0.");
        } else {
            produit.setStock(stock);
        }

        if (errors.isEmpty()) {
            try {
                if (produit.getId() == null) {
                    produitDao.create(produit);
                    Web.flash(req, "success", "Produit « " + produit.getDesignation() + " » créé.");
                } else if (produitDao.update(produit)) {
                    Web.flash(req, "success", "Produit « " + produit.getDesignation() + " » modifié.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                Web.redirect(req, resp, "/produits");
                return;
            } catch (DaoException e) {
                errors.put("global", e.getMessage());
            }
        }
        req.setAttribute("submitted", true);
        showForm(req, resp, produit, errors);
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = Web.parseLong(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (produitDao.delete(id)) {
                Web.flash(req, "success", "Produit supprimé.");
            } else {
                Web.flash(req, "error", "Ce produit n'existe plus.");
            }
        } catch (DaoException e) {
            // A product that appears on an order line cannot be deleted (foreign key RESTRICT).
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/produits");
    }

    private void validateDesignation(Produit p, Map<String, String> errors) {
        if (p.getDesignation() == null) {
            errors.put("designation", "La désignation est obligatoire.");
        } else if (p.getDesignation().length() > 150) {
            errors.put("designation", "150 caractères maximum.");
        }
        if (p.getDescription() != null && p.getDescription().length() > 500) {
            errors.put("description", "500 caractères maximum.");
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Produit produit,
            Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute("produit", produit);
        req.setAttribute("errors", errors);
        Web.render(req, resp, "produits/form.jsp");
    }
}
