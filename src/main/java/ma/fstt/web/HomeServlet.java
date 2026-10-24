package ma.fstt.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Commande;
import ma.fstt.entities.Produit;
import ma.fstt.entities.StatutCommande;
import ma.fstt.service.IClientDao;
import ma.fstt.service.ICommandeDao;
import ma.fstt.service.IProduitDao;

/** Dashboard: a few key figures and shortcuts. Read-only. */
@WebServlet(name = "HomeServlet", urlPatterns = "/home")
public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int RECENT_ORDERS = 5;

    @Inject
    private IClientDao clientDao;
    @Inject
    private IProduitDao produitDao;
    @Inject
    private ICommandeDao commandeDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Commande> commandes = commandeDao.findAll(); // already sorted, newest first
        List<Produit> produits = produitDao.findAll();

        long enAttente = commandes.stream().filter(c -> c.getStatut() == StatutCommande.EN_ATTENTE).count();
        // Revenue counts only orders that are confirmed or shipped (not pending, not cancelled).
        BigDecimal chiffreAffaires = commandes.stream()
                .filter(c -> c.getStatut() == StatutCommande.VALIDEE || c.getStatut() == StatutCommande.EXPEDIEE)
                .map(Commande::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        req.setAttribute("nbClients", clientDao.findAll().size());
        req.setAttribute("nbProduits", produits.size());
        req.setAttribute("nbCommandes", commandes.size());
        req.setAttribute("nbEnAttente", enAttente);
        req.setAttribute("chiffreAffaires", chiffreAffaires);
        req.setAttribute("produitsStockBas", produits.stream()
                .filter(p -> p.getStock() <= LOW_STOCK_THRESHOLD)
                .collect(Collectors.toList()));
        req.setAttribute("dernieresCommandes", commandes.stream().limit(RECENT_ORDERS).collect(Collectors.toList()));
        Web.render(req, resp, "home.jsp");
    }
}
