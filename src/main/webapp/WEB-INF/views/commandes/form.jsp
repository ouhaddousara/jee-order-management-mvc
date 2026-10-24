<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Nouvelle commande" active="commandes">
    <div class="page-head">
        <h1>Nouvelle commande</h1>
    </div>

    <c:choose>
        <c:when test="${empty clients}">
            <div class="card center">
                <p class="muted">Il faut d'abord créer un client pour passer une commande.</p>
                <a class="btn btn-primary" href="${ctx}/clients/new">Créer un client</a>
            </div>
        </c:when>
        <c:when test="${empty produits}">
            <div class="card center">
                <p class="muted">Aucun produit n'est disponible en stock.</p>
                <a class="btn btn-primary" href="${ctx}/produits">Gérer les produits</a>
            </div>
        </c:when>
        <c:otherwise>
            <form method="post" action="${ctx}/commandes/create" class="card form" novalidate>
                <t:csrf/>

                <c:if test="${not empty errors.global}">
                    <div class="alert alert-error" role="alert"><c:out value="${errors.global}"/></div>
                </c:if>

                <div class="row">
                    <div class="field ${not empty errors.clientId ? 'has-error' : ''}">
                        <label for="clientId">Client *</label>
                        <select id="clientId" name="clientId" required>
                            <option value="">— Choisir un client —</option>
                            <c:forEach var="cl" items="${clients}">
                                <option value="${cl.id}" ${selectedClientId == cl.id ? 'selected' : ''}>
                                    <c:out value="${cl.nomComplet}"/> (<c:out value="${cl.email}"/>)
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.clientId}"><p class="error-text"><c:out value="${errors.clientId}"/></p></c:if>
                    </div>
                    <div class="field ${not empty errors.dateCommande ? 'has-error' : ''}">
                        <label for="dateCommande">Date</label>
                        <input id="dateCommande" name="dateCommande" type="date"
                               value="<c:out value='${dateCommande}'/>">
                        <c:if test="${not empty errors.dateCommande}"><p class="error-text"><c:out value="${errors.dateCommande}"/></p></c:if>
                    </div>
                </div>

                <fieldset class="${not empty errors.lignes ? 'has-error' : ''}">
                    <legend>Produits commandés</legend>
                    <div class="table-wrap">
                        <table class="lines">
                            <thead>
                            <tr><th>Produit</th><th class="qty">Quantité</th><th class="actions"></th></tr>
                            </thead>
                            <tbody id="lines-body">
                            <c:forEach var="line" items="${lineInputs}">
                                <tr>
                                    <td>
                                        <select name="produitId" aria-label="Produit">
                                            <option value="">— Choisir un produit —</option>
                                            <c:forEach var="p" items="${produits}">
                                                <option value="${p.id}" ${line.produitId == p.id ? 'selected' : ''}>
                                                    <c:out value="${p.designation}"/> · <t:money value="${p.prix}"/> · stock <c:out value="${p.stock}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </td>
                                    <td class="qty">
                                        <input type="number" name="quantite" min="1" step="1" aria-label="Quantité"
                                               value="<c:out value='${line.quantite}'/>">
                                    </td>
                                    <td class="actions">
                                        <button type="button" class="btn btn-sm remove-line" aria-label="Retirer cette ligne">✕</button>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${not empty errors.lignes}"><p class="error-text"><c:out value="${errors.lignes}"/></p></c:if>
                    <button type="button" id="add-line" class="btn btn-sm">+ Ajouter une ligne</button>
                    <p class="hint">Le prix de chaque ligne est fixé automatiquement au tarif du jour. Une ligne laissée vide est ignorée.</p>
                </fieldset>

                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">Créer la commande</button>
                    <a class="btn" href="${ctx}/commandes">Annuler</a>
                </div>
            </form>
        </c:otherwise>
    </c:choose>
</t:layout>
