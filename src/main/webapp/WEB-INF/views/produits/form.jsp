<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isNew" value="${empty produit.id}"/>
<t:layout title="${isNew ? 'Nouveau produit' : 'Modifier le produit'}" active="produits">
    <div class="page-head">
        <h1><c:out value="${isNew ? 'Nouveau produit' : 'Modifier le produit'}"/></h1>
    </div>

    <form method="post" action="${ctx}/produits/save" class="card form narrow" novalidate>
        <t:csrf/>
        <c:if test="${not isNew}"><input type="hidden" name="id" value="${produit.id}"></c:if>

        <c:if test="${not empty errors.global}">
            <div class="alert alert-error" role="alert"><c:out value="${errors.global}"/></div>
        </c:if>

        <div class="field ${not empty errors.designation ? 'has-error' : ''}">
            <label for="designation">Désignation *</label>
            <input id="designation" name="designation" type="text" maxlength="150" required
                   value="<c:out value='${submitted ? param.designation : produit.designation}'/>">
            <c:if test="${not empty errors.designation}"><p class="error-text"><c:out value="${errors.designation}"/></p></c:if>
        </div>

        <div class="field ${not empty errors.description ? 'has-error' : ''}">
            <label for="description">Description</label>
            <textarea id="description" name="description" rows="3" maxlength="500"><c:out value="${submitted ? param.description : produit.description}"/></textarea>
            <c:if test="${not empty errors.description}"><p class="error-text"><c:out value="${errors.description}"/></p></c:if>
        </div>

        <div class="row">
            <div class="field ${not empty errors.prix ? 'has-error' : ''}">
                <label for="prix">Prix (MAD) *</label>
                <input id="prix" name="prix" type="text" inputmode="decimal" required
                       value="<c:out value='${submitted ? param.prix : (isNew ? "" : produit.prix)}'/>">
                <c:if test="${not empty errors.prix}"><p class="error-text"><c:out value="${errors.prix}"/></p></c:if>
            </div>
            <div class="field ${not empty errors.stock ? 'has-error' : ''}">
                <label for="stock">Stock *</label>
                <input id="stock" name="stock" type="number" min="0" step="1" required
                       value="<c:out value='${submitted ? param.stock : (isNew ? "0" : produit.stock)}'/>">
                <c:if test="${not empty errors.stock}"><p class="error-text"><c:out value="${errors.stock}"/></p></c:if>
            </div>
        </div>
        <c:if test="${not isNew}">
            <p class="hint">Modifier le prix n'affecte pas les commandes déjà passées : chaque ligne conserve le prix du jour de la commande.</p>
        </c:if>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Enregistrer</button>
            <a class="btn" href="${ctx}/produits">Annuler</a>
        </div>
    </form>
</t:layout>
