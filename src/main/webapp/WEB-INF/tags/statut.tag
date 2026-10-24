<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="value" required="true" type="ma.fstt.entities.StatutCommande" %>
<%-- Status marker (coloured dot + label) for an order status (styles: .badge-EN_ATTENTE, .badge-VALIDEE, ...). --%>
<span class="badge badge-${value}"><c:out value="${value.libelle}"/></span>
