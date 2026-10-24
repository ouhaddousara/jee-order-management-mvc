<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="${client.nomComplet}" active="clients">
    <div class="page-head">
        <h1><c:out value="${client.nomComplet}"/></h1>
        <div class="head-actions">
            <a class="btn" href="${ctx}/clients/edit?id=${client.id}">Modifier</a>
            <a class="btn btn-primary" href="${ctx}/commandes/new?client=${client.id}">Nouvelle commande</a>
        </div>
    </div>

    <section class="card">
        <dl class="details">
            <dt>E-mail</dt><dd><c:out value="${client.email}"/></dd>
            <dt>Téléphone</dt><dd><c:out value="${empty client.telephone ? '—' : client.telephone}"/></dd>
            <dt>Adresse</dt><dd><c:out value="${empty client.adresse ? '—' : client.adresse}"/></dd>
        </dl>
    </section>

    <h2>Commandes du client</h2>
    <c:choose>
        <c:when test="${empty commandes}">
            <div class="card center"><p class="muted">Ce client n'a pas encore passé de commande.</p></div>
        </c:when>
        <c:otherwise>
            <div class="card flush">
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr><th>N°</th><th>Date</th><th>Statut</th><th class="num">Total</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach var="cmd" items="${commandes}">
                            <tr>
                                <td><a href="${ctx}/commandes/view?id=${cmd.id}">#<c:out value="${cmd.id}"/></a></td>
                                <td><c:out value="${cmd.dateCommande}"/></td>
                                <td><t:statut value="${cmd.statut}"/></td>
                                <td class="num"><t:money value="${cmd.total}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <p><a href="${ctx}/clients">← Retour à la liste des clients</a></p>
</t:layout>
