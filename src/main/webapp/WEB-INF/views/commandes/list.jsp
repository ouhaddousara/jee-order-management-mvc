<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Commandes" active="commandes">
    <div class="page-head">
        <h1>Commandes <span class="count"><c:out value="${commandes.size()}"/></span></h1>
        <a class="btn btn-primary" href="${ctx}/commandes/new">Nouvelle commande</a>
    </div>

    <form method="get" action="${ctx}/commandes" class="filter">
        <label for="statut">Statut</label>
        <select id="statut" name="statut" data-autosubmit>
            <option value="">Tous</option>
            <c:forEach var="s" items="${statuts}">
                <option value="${s}" ${filtre == s ? 'selected' : ''}><c:out value="${s.libelle}"/></option>
            </c:forEach>
        </select>
        <noscript><button type="submit" class="btn btn-sm">Filtrer</button></noscript>
    </form>

    <c:choose>
        <c:when test="${empty commandes}">
            <div class="card center">
                <p class="muted">Aucune commande<c:if test="${not empty filtre}"> avec ce statut</c:if>.</p>
                <a class="btn btn-primary" href="${ctx}/commandes/new">Créer une commande</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card flush">
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>N°</th>
                            <th>Date</th>
                            <th>Client</th>
                            <th>Statut</th>
                            <th class="num">Lignes</th>
                            <th class="num">Total</th>
                            <th class="actions">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="cmd" items="${commandes}">
                            <tr>
                                <td><a href="${ctx}/commandes/view?id=${cmd.id}">#<c:out value="${cmd.id}"/></a></td>
                                <td><c:out value="${cmd.dateCommande}"/></td>
                                <td><a href="${ctx}/clients/view?id=${cmd.client.id}"><c:out value="${cmd.client.nomComplet}"/></a></td>
                                <td><t:statut value="${cmd.statut}"/></td>
                                <td class="num"><c:out value="${cmd.lignes.size()}"/></td>
                                <td class="num"><t:money value="${cmd.total}"/></td>
                                <td class="actions">
                                    <a class="btn btn-sm" href="${ctx}/commandes/view?id=${cmd.id}">Ouvrir</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                        <tfoot>
                        <tr>
                            <td colspan="5" class="num">Total affiché</td>
                            <td class="num"><strong><t:money value="${total}"/></strong></td>
                            <td></td>
                        </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</t:layout>
