<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isNew" value="${empty client.id}"/>
<t:layout title="${isNew ? 'Nouveau client' : 'Modifier le client'}" active="clients">
    <div class="page-head">
        <h1><c:out value="${isNew ? 'Nouveau client' : 'Modifier le client'}"/></h1>
    </div>

    <form method="post" action="${ctx}/clients/save" class="card form narrow" novalidate>
        <t:csrf/>
        <c:if test="${not isNew}"><input type="hidden" name="id" value="${client.id}"></c:if>

        <c:if test="${not empty errors.global}">
            <div class="alert alert-error" role="alert"><c:out value="${errors.global}"/></div>
        </c:if>

        <div class="row">
            <div class="field ${not empty errors.nom ? 'has-error' : ''}">
                <label for="nom">Nom *</label>
                <input id="nom" name="nom" type="text" maxlength="80" required
                       value="<c:out value='${submitted ? param.nom : client.nom}'/>">
                <c:if test="${not empty errors.nom}"><p class="error-text"><c:out value="${errors.nom}"/></p></c:if>
            </div>
            <div class="field ${not empty errors.prenom ? 'has-error' : ''}">
                <label for="prenom">Prénom *</label>
                <input id="prenom" name="prenom" type="text" maxlength="80" required
                       value="<c:out value='${submitted ? param.prenom : client.prenom}'/>">
                <c:if test="${not empty errors.prenom}"><p class="error-text"><c:out value="${errors.prenom}"/></p></c:if>
            </div>
        </div>

        <div class="field ${not empty errors.email ? 'has-error' : ''}">
            <label for="email">E-mail *</label>
            <input id="email" name="email" type="email" maxlength="150" required
                   value="<c:out value='${submitted ? param.email : client.email}'/>">
            <c:if test="${not empty errors.email}"><p class="error-text"><c:out value="${errors.email}"/></p></c:if>
        </div>

        <div class="field ${not empty errors.telephone ? 'has-error' : ''}">
            <label for="telephone">Téléphone</label>
            <input id="telephone" name="telephone" type="tel" maxlength="20"
                   value="<c:out value='${submitted ? param.telephone : client.telephone}'/>">
            <c:if test="${not empty errors.telephone}"><p class="error-text"><c:out value="${errors.telephone}"/></p></c:if>
        </div>

        <div class="field ${not empty errors.adresse ? 'has-error' : ''}">
            <label for="adresse">Adresse</label>
            <input id="adresse" name="adresse" type="text" maxlength="255"
                   value="<c:out value='${submitted ? param.adresse : client.adresse}'/>">
            <c:if test="${not empty errors.adresse}"><p class="error-text"><c:out value="${errors.adresse}"/></p></c:if>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Enregistrer</button>
            <a class="btn" href="${ctx}/clients">Annuler</a>
        </div>
    </form>
</t:layout>
