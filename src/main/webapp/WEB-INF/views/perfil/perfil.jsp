<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="u" value="${sessionScope.usuario}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Mi perfil</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 720px;">
    <h1 class="h4 text-sigmud mb-3"><i class="bi bi-person-circle"></i> Mi perfil</h1>


    <div class="card card-sigmud shadow-sm">
        <div class="card-body">
            <dl class="row mb-0">
                <dt class="col-sm-4">Nombre</dt>
                <dd class="col-sm-8"><c:out value="${u.nombres}"/></dd>
                <dt class="col-sm-4">Usuario</dt>
                <dd class="col-sm-8"><c:out value="${u.username}"/></dd>
                <dt class="col-sm-4">Rol</dt>
                <dd class="col-sm-8"><span class="badge text-bg-secondary"><c:out value="${u.rol}"/></span></dd>
                <dt class="col-sm-4">Área</dt>
                <dd class="col-sm-8"><c:out value="${u.nombreArea}" default="Sin área"/></dd>
                <dt class="col-sm-4">Último acceso anterior</dt>
                <dd class="col-sm-8"><c:out value="${u.ultimoAccesoTexto}"/></dd>
                <dt class="col-sm-4">Inicio de esta sesión</dt>
                <dd class="col-sm-8"><c:out value="${inicioSesion}"/></dd>
            </dl>
        </div>
        <div class="card-footer bg-transparent">
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/perfil/password">
                <i class="bi bi-key"></i> Cambiar contraseña
            </a>
        </div>
    </div>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
</body>
</html>
