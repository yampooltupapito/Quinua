<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="codigo" value="${pageContext.errorData.statusCode}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Error</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/saltar.jsp"/>
<main id="contenido" tabindex="-1" class="container">
    <div class="row justify-content-center align-items-center min-vh-100">
        <div class="col-11 col-md-6 text-center">
            <h1 class="display-6 text-sigmud">
                <c:choose>
                    <c:when test="${codigo == 403}"><i class="bi bi-shield-lock"></i> Acceso denegado</c:when>
                    <c:when test="${codigo == 404}"><i class="bi bi-search"></i> Página no encontrada</c:when>
                    <c:otherwise><i class="bi bi-exclamation-octagon"></i> Ocurrió un error</c:otherwise>
                </c:choose>
            </h1>
            <p class="text-body-secondary">
                <c:choose>
                    <c:when test="${codigo == 403}">No tiene permiso para esta sección o acción. Si el problema continúa, vuelva al menú e inicie sesión de nuevo.</c:when>
                    <c:when test="${codigo == 404}">La dirección solicitada no existe.</c:when>
                    <c:otherwise>No se pudo completar la operación. Intente de nuevo o avise al administrador.</c:otherwise>
                </c:choose>
            </p>
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/menu">
                <i class="bi bi-house"></i> Volver al menú
            </a>
        </div>
    </div>
</main>
</body>
</html>
