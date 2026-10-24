package ma.fstt.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Client;
import ma.fstt.entities.Commande;
import ma.fstt.entities.Produit;
import ma.fstt.entities.StatutCommande;
import ma.fstt.service.DaoException;
import ma.fstt.service.IClientDao;
import ma.fstt.service.ICommandeDao;
import ma.fstt.service.ILigneCommandeDao;
import ma.fstt.service.IProduitDao;

/**
 * Controller of the order space.
 *
 * <pre>
 *   GET  /commandes                 list (optional ?statut=)
 *   GET  /commandes/new             creation form (optional ?client=)
 *   GET  /commandes/view?id=        details, lines, status actions
 *   GET  /commandes/edit?id=        header form (client, date) of a pending order
 *
 *   POST /commandes/create          create an order with its lines
 *   POST /commandes/update          change the header of a pending order
 *   POST /commandes/delete          delete a pending or cancelled order
 *   POST /commandes/statut          change the status
 *   POST /commandes/ligne/add       add a product to an order
 *   POST /commandes/ligne/update    change the quantity of a line
 *   POST /commandes/ligne/delete    remove a line
 * </pre>
 * The business rules (stock, price snapshot, allowed transitions) live in the DAO layer;
 * this class validates the input, calls the DAO and turns {@link DaoException}s into
 * messages for the user.
 */
@WebServlet(name = "CommandeServlet", urlPatterns = "/commandes/*")
public class CommandeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    /** Number of product rows displayed on the creation form. */
    private static final int MIN_FORM_ROWS = 3;

    @Inject
    private ICommandeDao commandeDao;
    @Inject
    private ILigneCommandeDao ligneDao;
    @Inject
    private IClientDao clientDao;
    @Inject
    private IProduitDao produitDao;

    /** What the user typed for one line of the creation form, kept to redisplay it after an error. */
    public static final class LineInput {
        private final String produitId;
        private final String quantite;

        LineInput(String produitId, String quantite) {
            this.produitId = produitId;
            this.quantite = quantite;
        }

        public String getProduitId() {
            return produitId;
        }

        public String getQuantite() {
            return quantite;
        }
    }

    // ------------------------------------------------------------------
    // GET
    // ------------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (Web.action(req)) {
            case "/":
                list(req, resp);
                break;
            case "/new":
                showCreateForm(req, resp, Web.parseLong(req.getParameter("client")), LocalDate.now().toString(),
                        new ArrayList<>(), new LinkedHashMap<>());
                break;
            case "/view":
                view(req, resp);
                break;
            case "/edit":
                edit(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StatutCommande filtre = parseStatut(req.getParameter("statut"));
        List<Commande> commandes = commandeDao.findAll();
        if (filtre != null) {
            commandes = commandes.stream().filter(c -> c.getStatut() == filtre).collect(Collectors.toList());
        }
        BigDecimal total = commandes.stream().map(Commande::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        req.setAttribute("commandes", commandes);
        req.setAttribute("total", total);
        req.setAttribute("statuts", StatutCommande.values());
        req.setAttribute("filtre", filtre);
        Web.render(req, resp, "commandes/list.jsp");
    }

    private void view(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<Commande> found = findCommande(req.getParameter("id"));
        if (found.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Commande commande = found.get();
        StatutCommande statut = commande.getStatut();
        boolean editable = statut == StatutCommande.EN_ATTENTE;

        List<StatutCommande> transitions = new ArrayList<>();
        for (StatutCommande cible : StatutCommande.values()) {
            if (statut.peutPasserA(cible)) {
                transitions.add(cible);
            }
        }
        req.setAttribute("commande", commande);
        req.setAttribute("transitions", transitions);
        req.setAttribute("editable", editable);
        req.setAttribute("deletable", editable || statut == StatutCommande.ANNULEE);
        if (editable) {
            req.setAttribute("produits", produitDao.findAvailable());
        }
        Web.render(req, resp, "commandes/view.jsp");
    }

    private void edit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<Commande> found = findCommande(req.getParameter("id"));
        if (found.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (found.get().getStatut() != StatutCommande.EN_ATTENTE) {
            Web.flash(req, "error", "Seule une commande en attente peut être modifiée.");
            Web.redirect(req, resp, "/commandes/view?id=" + found.get().getId());
            return;
        }
        req.setAttribute("commande", found.get());
        req.setAttribute("clients", clientDao.findAll());
        req.setAttribute("errors", new LinkedHashMap<String, String>());
        Web.render(req, resp, "commandes/edit.jsp");
    }

    // ------------------------------------------------------------------
    // POST
    // ------------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (Web.action(req)) {
            case "/create":
                create(req, resp);
                break;
            case "/update":
                update(req, resp);
                break;
            case "/delete":
                delete(req, resp);
                break;
            case "/statut":
                changeStatut(req, resp);
                break;
            case "/ligne/add":
                addLine(req, resp);
                break;
            case "/ligne/update":
                updateLine(req, resp);
                break;
            case "/ligne/delete":
                deleteLine(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> errors = new LinkedHashMap<>();

        Long clientId = Web.parseLong(req.getParameter("clientId"));
        Client client = clientId == null ? null : clientDao.findById(clientId).orElse(null);
        if (client == null) {
            errors.put("clientId", "Choisissez un client.");
        }

        String dateParam = req.getParameter("dateCommande");
        LocalDate date = Web.parseDate(dateParam);
        if (Web.trimToNull(dateParam) != null && date == null) {
            errors.put("dateCommande", "Date invalide.");
        }

        // The form posts parallel arrays: the n-th product goes with the n-th quantity.
        String[] produitIds = valuesOf(req, "produitId");
        String[] quantites = valuesOf(req, "quantite");
        List<LineInput> inputs = new ArrayList<>();
        Commande commande = new Commande(client);
        commande.setDateCommande(date == null ? LocalDate.now() : date);
        boolean lineError = false;
        for (int i = 0; i < Math.max(produitIds.length, quantites.length); i++) {
            String rawProduit = i < produitIds.length ? Web.trimToNull(produitIds[i]) : null;
            String rawQuantite = i < quantites.length ? Web.trimToNull(quantites[i]) : null;
            inputs.add(new LineInput(rawProduit, rawQuantite));
            if (rawProduit == null && rawQuantite == null) {
                continue; // an empty row is simply ignored
            }
            Long produitId = Web.parseLong(rawProduit);
            Integer quantite = Web.parseInt(rawQuantite);
            if (produitId == null || quantite == null || quantite <= 0) {
                lineError = true;
            } else {
                Produit ref = new Produit();
                ref.setId(produitId);
                commande.ajouterLigne(ref, quantite);
            }
        }
        if (lineError) {
            errors.put("lignes", "Chaque ligne doit avoir un produit et une quantité entière positive.");
        } else if (commande.getLignes().isEmpty()) {
            errors.put("lignes", "Ajoutez au moins un produit à la commande.");
        }

        if (errors.isEmpty()) {
            try {
                commandeDao.create(commande);
                Web.flash(req, "success", "Commande n° " + commande.getId() + " créée.");
                Web.redirect(req, resp, "/commandes/view?id=" + commande.getId());
                return;
            } catch (DaoException e) { // includes InsufficientStockException
                errors.put("global", e.getMessage());
            }
        }
        req.setAttribute("submitted", true);
        showCreateForm(req, resp, clientId, dateParam, inputs, errors);
    }

    private void update(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long id = Web.parseLong(req.getParameter("id"));
        Long clientId = Web.parseLong(req.getParameter("clientId"));
        LocalDate date = Web.parseDate(req.getParameter("dateCommande"));
        Client client = clientId == null ? null : clientDao.findById(clientId).orElse(null);
        if (id == null || client == null || date == null) {
            Web.flash(req, "error", "Client ou date invalide.");
            Web.redirect(req, resp, id == null ? "/commandes" : "/commandes/edit?id=" + id);
            return;
        }
        Commande commande = new Commande(client);
        commande.setId(id);
        commande.setDateCommande(date);
        try {
            if (commandeDao.update(commande)) {
                Web.flash(req, "success", "Commande n° " + id + " modifiée.");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (DaoException e) {
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/commandes/view?id=" + id);
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = Web.parseLong(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (commandeDao.delete(id)) {
                Web.flash(req, "success", "Commande n° " + id + " supprimée.");
            } else {
                Web.flash(req, "error", "Cette commande n'existe plus.");
            }
        } catch (DaoException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, "/commandes/view?id=" + id);
            return;
        }
        Web.redirect(req, resp, "/commandes");
    }

    private void changeStatut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = Web.parseLong(req.getParameter("id"));
        StatutCommande cible = parseStatut(req.getParameter("statut"));
        if (id == null || cible == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (commandeDao.changerStatut(id, cible)) {
                Web.flash(req, "success", "Commande n° " + id + " : statut « " + cible.getLibelle() + " ».");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (DaoException e) {
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/commandes/view?id=" + id);
    }

    private void addLine(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long commandeId = Web.parseLong(req.getParameter("commandeId"));
        Long produitId = Web.parseLong(req.getParameter("produitId"));
        Integer quantite = Web.parseInt(req.getParameter("quantite"));
        if (commandeId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (produitId == null || quantite == null || quantite <= 0) {
            Web.flash(req, "error", "Choisissez un produit et une quantité entière positive.");
        } else {
            try {
                ligneDao.add(commandeId, produitId, quantite);
                Web.flash(req, "success", "Produit ajouté à la commande.");
            } catch (DaoException e) {
                Web.flash(req, "error", e.getMessage());
            }
        }
        Web.redirect(req, resp, "/commandes/view?id=" + commandeId);
    }

    private void updateLine(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long commandeId = Web.parseLong(req.getParameter("commandeId"));
        Long ligneId = Web.parseLong(req.getParameter("ligneId"));
        Integer quantite = Web.parseInt(req.getParameter("quantite"));
        if (commandeId == null || ligneId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (quantite == null || quantite <= 0) {
            Web.flash(req, "error", "La quantité doit être un entier positif.");
        } else {
            try {
                if (ligneDao.updateQuantite(ligneId, quantite)) {
                    Web.flash(req, "success", "Quantité modifiée (le stock a été remis à jour).");
                } else {
                    Web.flash(req, "error", "Cette ligne n'existe plus.");
                }
            } catch (DaoException e) {
                Web.flash(req, "error", e.getMessage());
            }
        }
        Web.redirect(req, resp, "/commandes/view?id=" + commandeId);
    }

    private void deleteLine(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long commandeId = Web.parseLong(req.getParameter("commandeId"));
        Long ligneId = Web.parseLong(req.getParameter("ligneId"));
        if (commandeId == null || ligneId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (ligneDao.remove(ligneId)) {
                Web.flash(req, "success", "Ligne supprimée (le stock a été remis à jour).");
            } else {
                Web.flash(req, "error", "Cette ligne n'existe plus.");
            }
        } catch (DaoException e) {
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/commandes/view?id=" + commandeId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void showCreateForm(HttpServletRequest req, HttpServletResponse resp, Long selectedClientId,
            String date, List<LineInput> inputs, Map<String, String> errors) throws ServletException, IOException {
        List<LineInput> rows = new ArrayList<>(inputs);
        while (rows.size() < MIN_FORM_ROWS) {
            rows.add(new LineInput(null, null));
        }
        req.setAttribute("clients", clientDao.findAll());
        req.setAttribute("produits", produitDao.findAvailable());
        req.setAttribute("selectedClientId", selectedClientId);
        req.setAttribute("dateCommande", date);
        req.setAttribute("lineInputs", rows);
        req.setAttribute("errors", errors);
        Web.render(req, resp, "commandes/form.jsp");
    }

    private Optional<Commande> findCommande(String idParam) {
        Long id = Web.parseLong(idParam);
        return id == null ? Optional.empty() : commandeDao.findById(id);
    }

    /** Returns the enum for a status name, or null if absent or unknown. */
    private static StatutCommande parseStatut(String value) {
        String v = Web.trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return StatutCommande.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String[] valuesOf(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        return values == null ? new String[0] : values;
    }
}
