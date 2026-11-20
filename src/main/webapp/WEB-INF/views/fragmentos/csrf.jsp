<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Campo oculto con el token CSRF de la sesión. Va dentro de TODO formulario POST (menos el login). --%>
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
