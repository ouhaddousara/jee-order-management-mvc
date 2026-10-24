<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:layout title="Produits" active="produits">
    <div class="page-head">
        <h1>Produits <span class="count"><c:out value="${produits.size()}"/></span></h1>
        <a class="btn btn-primary" href="${ctx}/produits/new">Nouveau produit</a>
    </div>

    <c:choose>
        <c:when test="${empty produits}">
            <div class="card center">
                <p class="muted">Aucun produit au catalogue.</p>
                <a class="btn btn-primary" href="${ctx}/produits/new">Ajouter le premier produit</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card flush">
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>Désignation</th>
                            <th>Description</th>
                            <th class="num">Prix</th>
                            <th class="num">Stock</th>
                            <th class="actions">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="produit" items="${produits}">
                            <tr>
                                <td><strong><c:out value="${produit.designation}"/></strong></td>
                                <td class="muted"><c:out value="${produit.description}"/></td>
                                <td class="num"><t:money value="${produit.prix}"/></td>
                                <td class="num">
                                    <c:choose>
                                        <c:when test="${produit.stock == 0}"><span class="badge badge-ANNULEE">Rupture</span></c:when>
                                        <c:when test="${produit.stock <= 5}"><span class="badge badge-EN_ATTENTE"><c:out value="${produit.stock}"/></span></c:when>
                                        <c:otherwise><c:out value="${produit.stock}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="actions">
                                    <a class="btn btn-sm" href="${ctx}/produits/edit?id=${produit.id}">Modifier</a>
                                    <form method="post" action="${ctx}/produits/delete" class="inline"
                                          data-confirm="Supprimer le produit <c:out value='${produit.designation}'/> ?">
                                        <t:csrf/>
                                        <input type="hidden" name="id" value="${produit.id}">
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
