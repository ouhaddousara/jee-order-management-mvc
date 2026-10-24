<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ attribute name="value" required="true" type="java.math.BigDecimal" %>
<%-- Amount with 2 decimals and the currency, e.g. "1 600,00 MAD" (locale set by layout.tag). --%>
<fmt:formatNumber value="${value}" minFractionDigits="2" maxFractionDigits="2"/>&#160;MAD
