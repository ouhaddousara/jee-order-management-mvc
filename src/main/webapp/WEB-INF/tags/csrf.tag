<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Hidden anti-CSRF field: every POST form must include it (see RequestFilter). --%>
<input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
