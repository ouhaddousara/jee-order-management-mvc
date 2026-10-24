<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Modifier la commande" active="commandes">
    <div class="page-head">
        <h1>Modifier la commande #<c:out value="${commande.id}"/></h1>
    </div>

    <form method="post" action="${ctx}/commandes/update" class="card form narrow" novalidate>
        <t:csrf/>
        <input type="hidden" name="id" value="${commande.id}">

        <div class="field">
            <label for="clientId">Client *</label>
            <select id="clientId" name="clientId" required>
                <c:forEach var="cl" items="${clients}">
                    <option value="${cl.id}" ${commande.client.id == cl.id ? 'selected' : ''}>
                        <c:out value="${cl.nomComplet}"/> (<c:out value="${cl.email}"/>)
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="dateCommande">Date *</label>
            <input id="dateCommande" name="dateCommande" type="date" required
                   value="<c:out value='${commande.dateCommande}'/>">
        </div>
        <p class="hint">Les produits se modifient depuis la page de la commande.</p>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Enregistrer</button>
            <a class="btn" href="${ctx}/commandes/view?id=${commande.id}">Annuler</a>
        </div>
    </form>
</t:layout>
