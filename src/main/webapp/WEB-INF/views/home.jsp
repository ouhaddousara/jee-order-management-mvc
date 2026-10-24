<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Accueil" active="home">
    <div class="page-head">
        <h1>Tableau de bord</h1>
        <a class="btn btn-primary" href="${ctx}/commandes/new">Nouvelle commande</a>
    </div>

    <section class="stats" aria-label="Chiffres clés">
        <a class="stat" href="${ctx}/clients">
            <span class="stat-value"><c:out value="${nbClients}"/></span>
            <span class="stat-label">Clients</span>
        </a>
        <a class="stat" href="${ctx}/produits">
            <span class="stat-value"><c:out value="${nbProduits}"/></span>
            <span class="stat-label">Produits</span>
        </a>
        <a class="stat" href="${ctx}/commandes">
            <span class="stat-value"><c:out value="${nbCommandes}"/></span>
            <span class="stat-label">Commandes</span>
        </a>
        <a class="stat" href="${ctx}/commandes?statut=EN_ATTENTE">
            <span class="stat-value"><c:out value="${nbEnAttente}"/></span>
            <span class="stat-label">En attente</span>
        </a>
        <div class="stat">
            <span class="stat-value stat-money"><t:money value="${chiffreAffaires}"/></span>
            <span class="stat-label">Chiffre d'affaires (validé + expédié)</span>
        </div>
    </section>

    <div class="grid-2">
        <section class="card">
            <h2>Dernières commandes</h2>
            <c:choose>
                <c:when test="${empty dernieresCommandes}">
                    <p class="muted">Aucune commande pour l'instant.</p>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead>
                            <tr><th>N°</th><th>Date</th><th>Client</th><th>Statut</th><th class="num">Total</th></tr>
                            </thead>
                            <tbody>
                            <c:forEach var="cmd" items="${dernieresCommandes}">
                                <tr>
                                    <td><a href="${ctx}/commandes/view?id=${cmd.id}">#<c:out value="${cmd.id}"/></a></td>
                                    <td><c:out value="${cmd.dateCommande}"/></td>
                                    <td><c:out value="${cmd.client.nomComplet}"/></td>
                                    <td><t:statut value="${cmd.statut}"/></td>
                                    <td class="num"><t:money value="${cmd.total}"/></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="card">
            <h2>Stock à surveiller</h2>
            <c:choose>
                <c:when test="${empty produitsStockBas}">
                    <p class="muted">Tous les produits ont un stock confortable.</p>
                </c:when>
                <c:otherwise>
                    <ul class="plain-list">
                        <c:forEach var="p" items="${produitsStockBas}">
                            <li>
                                <a href="${ctx}/produits/edit?id=${p.id}"><c:out value="${p.designation}"/></a>
                                <c:choose>
                                    <c:when test="${p.stock == 0}"><span class="badge badge-ANNULEE">Rupture</span></c:when>
                                    <c:otherwise><span class="badge badge-EN_ATTENTE">Reste <c:out value="${p.stock}"/></span></c:otherwise>
                                </c:choose>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</t:layout>
