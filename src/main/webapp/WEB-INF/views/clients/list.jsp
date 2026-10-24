<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Clients" active="clients">
    <div class="page-head">
        <h1>Clients <span class="count"><c:out value="${clients.size()}"/></span></h1>
        <a class="btn btn-primary" href="${ctx}/clients/new">Nouveau client</a>
    </div>

    <c:choose>
        <c:when test="${empty clients}">
            <div class="card center">
                <p class="muted">Aucun client enregistré.</p>
                <a class="btn btn-primary" href="${ctx}/clients/new">Ajouter le premier client</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card flush">
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>Client</th>
                            <th>E-mail</th>
                            <th>Téléphone</th>
                            <th>Adresse</th>
                            <th class="actions">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="client" items="${clients}">
                            <tr>
                                <td><a href="${ctx}/clients/view?id=${client.id}"><c:out value="${client.nomComplet}"/></a></td>
                                <td><c:out value="${client.email}"/></td>
                                <td><c:out value="${client.telephone}"/></td>
                                <td><c:out value="${client.adresse}"/></td>
                                <td class="actions">
                                    <a class="btn btn-sm" href="${ctx}/clients/edit?id=${client.id}">Modifier</a>
                                    <form method="post" action="${ctx}/clients/delete" class="inline"
                                          data-confirm="Supprimer le client <c:out value='${client.nomComplet}'/> ?">
                                        <t:csrf/>
                                        <input type="hidden" name="id" value="${client.id}">
                                        <button type="submit" class="btn btn-sm btn-danger">Supprimer</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</t:layout>
