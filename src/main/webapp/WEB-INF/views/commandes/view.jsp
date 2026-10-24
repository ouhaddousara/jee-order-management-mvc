<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Commande ${commande.id}" active="commandes">
    <div class="page-head">
        <h1>Commande #<c:out value="${commande.id}"/> <t:statut value="${commande.statut}"/></h1>
        <div class="head-actions">
            <c:if test="${editable}">
                <a class="btn" href="${ctx}/commandes/edit?id=${commande.id}">Modifier l'en-tête</a>
            </c:if>
            <c:if test="${deletable}">
                <form method="post" action="${ctx}/commandes/delete" class="inline"
                      data-confirm="Supprimer définitivement la commande n° <c:out value='${commande.id}'/> ?">
                    <t:csrf/>
                    <input type="hidden" name="id" value="${commande.id}">
                    <button type="submit" class="btn btn-danger">Supprimer</button>
                </form>
            </c:if>
        </div>
    </div>

    <%-- Progress of the order: the three steps are a real sequence; a cancelled order shows a notice instead. --%>
    <c:choose>
        <c:when test="${commande.statut == 'ANNULEE'}">
            <p class="track-cancelled">Commande annulée. Le stock réservé a été remis à disposition.</p>
        </c:when>
        <c:otherwise>
            <ol class="track track-${commande.statut}" aria-label="Avancement de la commande">
                <li><span>En attente</span></li>
                <li><span>Validée</span></li>
                <li><span>Expédiée</span></li>
            </ol>
        </c:otherwise>
    </c:choose>

    <section class="card">
        <dl class="details">
            <dt>Client</dt>
            <dd>
                <a href="${ctx}/clients/view?id=${commande.client.id}"><c:out value="${commande.client.nomComplet}"/></a>
                · <c:out value="${commande.client.email}"/>
            </dd>
            <dt>Date</dt><dd><c:out value="${commande.dateCommande}"/></dd>
            <dt>Total</dt><dd><strong><t:money value="${commande.total}"/></strong></dd>
        </dl>

        <c:if test="${not empty transitions}">
            <div class="transitions">
                <span class="muted">Changer le statut :</span>
                <c:forEach var="s" items="${transitions}">
                    <form method="post" action="${ctx}/commandes/statut" class="inline"
                          <c:if test="${s == 'ANNULEE'}">data-confirm="Annuler cette commande ? Le stock des produits sera remis à jour."</c:if>>
                        <t:csrf/>
                        <input type="hidden" name="id" value="${commande.id}">
                        <input type="hidden" name="statut" value="${s}">
                        <button type="submit" class="btn btn-sm ${s == 'ANNULEE' ? 'btn-danger' : 'btn-primary'}">
                            <c:choose>
                                <c:when test="${s == 'VALIDEE'}">Valider</c:when>
                                <c:when test="${s == 'EXPEDIEE'}">Marquer comme expédiée</c:when>
                                <c:otherwise>Annuler la commande</c:otherwise>
                            </c:choose>
                        </button>
                    </form>
                </c:forEach>
            </div>
        </c:if>
    </section>

    <h2>Lignes de la commande</h2>
    <div class="card flush">
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Produit</th>
                    <th class="num">Prix unitaire</th>
                    <th class="num">Quantité</th>
                    <th class="num">Sous-total</th>
                    <c:if test="${editable}"><th class="actions">Actions</th></c:if>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="ligne" items="${commande.lignes}">
                    <tr>
                        <td><c:out value="${ligne.produit.designation}"/></td>
                        <td class="num"><t:money value="${ligne.prixUnitaire}"/></td>
                        <c:choose>
                            <c:when test="${editable}">
                                <td class="num">
                                    <form method="post" action="${ctx}/commandes/ligne/update" class="inline qty-form">
                                        <t:csrf/>
                                        <input type="hidden" name="commandeId" value="${commande.id}">
                                        <input type="hidden" name="ligneId" value="${ligne.id}">
                                        <input type="number" name="quantite" min="1" step="1" value="${ligne.quantite}"
                                               aria-label="Quantité de <c:out value='${ligne.produit.designation}'/>">
                                        <button type="submit" class="btn btn-sm">Mettre à jour</button>
                                    </form>
                                </td>
                            </c:when>
                            <c:otherwise>
                                <td class="num"><c:out value="${ligne.quantite}"/></td>
                            </c:otherwise>
                        </c:choose>
                        <td class="num"><t:money value="${ligne.sousTotal}"/></td>
                        <c:if test="${editable}">
                            <td class="actions">
                                <form method="post" action="${ctx}/commandes/ligne/delete" class="inline"
                                      data-confirm="Retirer ce produit de la commande ?">
                                    <t:csrf/>
                                    <input type="hidden" name="commandeId" value="${commande.id}">
                                    <input type="hidden" name="ligneId" value="${ligne.id}">
                                    <button type="submit" class="btn btn-sm btn-danger">Retirer</button>
                                </form>
                            </td>
                        </c:if>
                    </tr>
                </c:forEach>
                </tbody>
                <tfoot>
                <tr>
                    <td colspan="3" class="num">Total</td>
                    <td class="num"><strong><t:money value="${commande.total}"/></strong></td>
                    <c:if test="${editable}"><td></td></c:if>
                </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <c:if test="${editable}">
        <section class="card">
            <h2>Ajouter un produit</h2>
            <c:choose>
                <c:when test="${empty produits}">
                    <p class="muted">Aucun produit disponible en stock.</p>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/commandes/ligne/add" class="inline-form">
                        <t:csrf/>
                        <input type="hidden" name="commandeId" value="${commande.id}">
                        <select name="produitId" aria-label="Produit" required>
                            <option value="">— Choisir un produit —</option>
                            <c:forEach var="p" items="${produits}">
                                <option value="${p.id}">
                                    <c:out value="${p.designation}"/> · <t:money value="${p.prix}"/> · stock <c:out value="${p.stock}"/>
                                </option>
                            </c:forEach>
                        </select>
                        <input type="number" name="quantite" min="1" step="1" value="1" aria-label="Quantité" required>
                        <button type="submit" class="btn btn-primary">Ajouter</button>
                    </form>
                    <p class="hint">Si le produit est déjà dans la commande, la quantité est augmentée.</p>
                </c:otherwise>
            </c:choose>
        </section>
    </c:if>

    <p><a href="${ctx}/commandes">← Retour à la liste des commandes</a></p>
</t:layout>
