<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Iniciar sesión</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/saltar.jsp"/>
<main id="contenido" tabindex="-1" class="container">
    <div class="row justify-content-center align-items-center min-vh-100">
        <div class="col-11 col-sm-8 col-md-6 col-lg-4">
            <div class="card card-sigmud shadow-sm">
                <div class="card-body p-4">
                    <div class="text-end">
                        <button type="button" class="btn btn-outline-secondary btn-sm btn-tema" aria-label="Cambiar a tema oscuro" title="Cambiar a tema oscuro">
                            <i class="bi bi-moon-stars-fill" aria-hidden="true"></i>
                        </button>
                    </div>
                    <div class="text-center mb-2">
                        <c:choose>
                            <c:when test="${applicationScope.logoDisponible}">
                                <img src="${pageContext.request.contextPath}/assets/img/logo.png" alt="Logo de la Municipalidad Distrital de Quinua"
                                     class="logo-login">
                            </c:when>
                            <c:otherwise><i class="bi bi-building icono-login text-sigmud" aria-hidden="true"></i></c:otherwise>
                        </c:choose>
                    </div>
                    <h1 class="h4 text-center text-sigmud mb-1">SIGMUD</h1>
                    <p class="text-center text-body-secondary small mb-4">Municipalidad Distrital de Quinua</p>

                    <c:if test="${not empty mensaje}">
                        <div class="alert alert-danger py-2" role="alert">
                            <i class="bi bi-exclamation-triangle-fill"></i> <c:out value="${mensaje}"/>
                        </div>
                    </c:if>

                    <form id="formLogin" method="post" action="${pageContext.request.contextPath}/login" novalidate>
                        <div class="mb-3">
                            <label for="username" class="form-label">Usuario</label>
                            <input type="text" class="form-control" id="username" name="username"
                                   maxlength="30" autocomplete="username" required autofocus
                                   value="<c:out value='${username}'/>">
                            <div class="invalid-feedback">Use de 3 a 30 letras, números, punto, guion o guion bajo.</div>
                        </div>
                        <div class="mb-3">
                            <label for="clave" class="form-label">Contraseña</label>
                            <input type="password" class="form-control" id="clave" name="clave"
                                   maxlength="100" autocomplete="current-password" required>
                            <div class="invalid-feedback">Ingrese su contraseña.</div>
                        </div>
                        <button type="submit" class="btn btn-sigmud w-100">
                            <i class="bi bi-box-arrow-in-right"></i> Ingresar
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/login.js"></script>
</body>
</html>
