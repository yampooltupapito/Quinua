<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Página de entrada: envía al menú (el AuthFilter manda al login si no hay sesión). --%>
<c:redirect url="/menu"/>
