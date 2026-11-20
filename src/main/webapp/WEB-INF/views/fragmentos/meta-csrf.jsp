<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Token CSRF para las llamadas Ajax: los scripts lo leen de esta etiqueta y lo envían en X-CSRF-Token. --%>
<meta name="csrf-token" content="<c:out value='${sessionScope.csrfToken}'/>">
