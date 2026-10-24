<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="status" value="${pageContext.errorData.statusCode}"/>
<t:layout title="Erreur">
    <div class="card narrow center">
        <p class="error-code"><c:out value="${status}"/></p>
        <c:choose>
            <c:when test="${status == 404}">
                <h1>Page ou enregistrement introuvable</h1>
                <p class="muted">L'adresse demandée n'existe pas, ou l'élément a été supprimé.</p>
            </c:when>
            <c:when test="${status == 403}">
                <h1>Action refusée</h1>
                <p class="muted">Votre session a expiré ou le formulaire n'est plus valide. Rechargez la page et recommencez.</p>
            </c:when>
            <c:when test="${status == 400}">
                <h1>Requête invalide</h1>
                <p class="muted">Un paramètre est manquant ou incorrect.</p>
            </c:when>
            <c:otherwise>
                <h1>Une erreur est survenue</h1>
                <p class="muted">Le problème a été enregistré. Réessayez dans un instant ; si cela persiste, vérifiez que la base de données est démarrée.</p>
            </c:otherwise>
        </c:choose>
        <a class="btn btn-primary" href="${ctx}/home">Retour à l'accueil</a>
    </div>
</t:layout>
