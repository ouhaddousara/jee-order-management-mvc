<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="active" required="false" %>
<%-- Common page frame: head, navigation, flash message, footer. Every view wraps its content in it. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<fmt:setLocale value="fr_FR" scope="request"/>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${title}"/> · Gestion des commandes</title>
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<div class="shell">
    <aside class="sidebar">
        <a class="brand" href="${ctx}/home" aria-label="Gestion des commandes, accueil">
            <span class="brand-mark" aria-hidden="true"></span>
            <span class="brand-name">Gestion<br>commandes</span>
        </a>
        <nav aria-label="Navigation principale">
            <a href="${ctx}/home" class="${active == 'home' ? 'active' : ''}">Accueil</a>
            <a href="${ctx}/commandes" class="${active == 'commandes' ? 'active' : ''}">Commandes</a>
            <a href="${ctx}/clients" class="${active == 'clients' ? 'active' : ''}">Clients</a>
            <a href="${ctx}/produits" class="${active == 'produits' ? 'active' : ''}">Produits</a>
        </nav>
        <p class="sidebar-note">Atelier JEE<br>FST Tanger, LSI</p>
    </aside>

    <main class="container">
        <%-- One-shot message put in the session by a servlet before a redirect. --%>
        <c:if test="${not empty sessionScope.flash}">
            <div class="alert alert-${sessionScope.flash.type}" role="status">
                <c:out value="${sessionScope.flash.message}"/>
            </div>
            <c:remove var="flash" scope="session"/>
        </c:if>

        <jsp:doBody/>
    </main>
</div>
<script src="${ctx}/assets/js/app.js" defer></script>
</body>
</html>
