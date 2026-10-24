package ma.fstt.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Client;
import ma.fstt.service.DaoException;
import ma.fstt.service.IClientDao;
import ma.fstt.service.ICommandeDao;

/**
 * Controller of the client space.
 *
 * <pre>
 *   GET  /clients          list              POST /clients/save    create or update
 *   GET  /clients/new      empty form        POST /clients/delete  delete
 *   GET  /clients/edit?id= form
 *   GET  /clients/view?id= details + orders
 * </pre>
 * Successful writes redirect (Post/Redirect/Get) so that a browser refresh never
 * replays the form; validation errors re-display the form with the user's input.
 */
@WebServlet(name = "ClientServlet", urlPatterns = "/clients/*")
public class ClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9 +().-]+$");

    @Inject
    private IClientDao clientDao;
    @Inject
    private ICommandeDao commandeDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (Web.action(req)) {
            case "/":
                req.setAttribute("clients", clientDao.findAll());
                Web.render(req, resp, "clients/list.jsp");
                break;
            case "/new":
                showForm(req, resp, new Client(), new LinkedHashMap<>());
                break;
            case "/edit":
                Optional<Client> existing = findClient(req);
                if (existing.isPresent()) {
                    showForm(req, resp, existing.get(), new LinkedHashMap<>());
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
            case "/view":
                view(req, resp);
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

    private void view(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<Client> client = findClient(req);
        if (client.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("client", client.get());
        req.setAttribute("commandes", commandeDao.findByClient(client.get().getId()));
        Web.render(req, resp, "clients/view.jsp");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Client client = new Client();
        client.setId(Web.parseLong(req.getParameter("id")));
        client.setNom(Web.trimToNull(req.getParameter("nom")));
        client.setPrenom(Web.trimToNull(req.getParameter("prenom")));
        client.setEmail(Web.trimToNull(req.getParameter("email")));
        client.setTelephone(Web.trimToNull(req.getParameter("telephone")));
        client.setAdresse(Web.trimToNull(req.getParameter("adresse")));

        Map<String, String> errors = validate(client);
        if (errors.isEmpty()) {
            try {
                if (client.getId() == null) {
                    clientDao.create(client);
                    Web.flash(req, "success", "Client « " + client.getNomComplet() + " » créé.");
                } else if (clientDao.update(client)) {
                    Web.flash(req, "success", "Client « " + client.getNomComplet() + " » modifié.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                Web.redirect(req, resp, "/clients");
                return;
            } catch (DaoException e) {
                errors.put("global", e.getMessage()); // e.g. duplicate e-mail lost a race
            }
        }
        req.setAttribute("submitted", true);
        showForm(req, resp, client, errors);
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = Web.parseLong(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (clientDao.delete(id)) {
                Web.flash(req, "success", "Client supprimé.");
            } else {
                Web.flash(req, "error", "Ce client n'existe plus.");
            }
        } catch (DaoException e) {
            // A client who still has orders cannot be deleted (foreign key RESTRICT).
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/clients");
    }

    private Map<String, String> validate(Client c) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (c.getNom() == null) {
            errors.put("nom", "Le nom est obligatoire.");
        } else if (c.getNom().length() > 80) {
            errors.put("nom", "80 caractères maximum.");
        }
        if (c.getPrenom() == null) {
            errors.put("prenom", "Le prénom est obligatoire.");
        } else if (c.getPrenom().length() > 80) {
            errors.put("prenom", "80 caractères maximum.");
        }
        if (c.getEmail() == null) {
            errors.put("email", "L'e-mail est obligatoire.");
        } else if (c.getEmail().length() > 150 || !EMAIL.matcher(c.getEmail()).matches()) {
            errors.put("email", "Adresse e-mail invalide.");
        } else {
            Optional<Client> other = clientDao.findByEmail(c.getEmail());
            if (other.isPresent() && !other.get().getId().equals(c.getId())) {
                errors.put("email", "Cette adresse e-mail est déjà utilisée par un autre client.");
            }
        }
        if (c.getTelephone() != null && (c.getTelephone().length() > 20 || !PHONE.matcher(c.getTelephone()).matches())) {
            errors.put("telephone", "Numéro invalide (chiffres, espaces et + ( ) . - ; 20 caractères maximum).");
        }
        if (c.getAdresse() != null && c.getAdresse().length() > 255) {
            errors.put("adresse", "255 caractères maximum.");
        }
        return errors;
    }

    private Optional<Client> findClient(HttpServletRequest req) {
        Long id = Web.parseLong(req.getParameter("id"));
        return id == null ? Optional.empty() : clientDao.findById(id);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Client client,
            Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute("client", client);
        req.setAttribute("errors", errors);
        Web.render(req, resp, "clients/form.jsp");
    }
}
