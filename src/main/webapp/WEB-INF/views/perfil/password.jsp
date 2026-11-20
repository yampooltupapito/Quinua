<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Cambiar contraseña</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 640px;">
    <h1 class="h4 text-sigmud mb-3"><i class="bi bi-key"></i> Cambiar contraseña</h1>

    <c:if test="${not empty errores}">
        <div class="alert alert-danger" role="alert">
            <strong>No se pudo cambiar la contraseña:</strong>
            <ul class="mb-0">
                <c:forEach var="e" items="${errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <form id="formPassword" class="card card-sigmud shadow-sm" method="post"
          action="${pageContext.request.contextPath}/perfil/password" novalidate
          data-usuario="<c:out value='${sessionScope.usuario.username}'/>">
        <div class="card-body row g-3">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>

            <div class="col-12">
                <label for="actual" class="form-label">Contraseña actual</label>
                <input type="password" class="form-control" id="actual" name="actual" maxlength="100"
                       autocomplete="current-password" required>
                <div class="invalid-feedback">Ingrese su contraseña actual.</div>
            </div>

            <div class="col-12">
                <label for="nueva" class="form-label">Nueva contraseña</label>
                <input type="password" class="form-control" id="nueva" name="nueva" maxlength="100"
                       autocomplete="new-password" required>
                <div class="progress mt-2" style="height: 8px;" aria-hidden="true">
                    <div class="progress-bar" id="barraFortaleza" role="progressbar" style="width: 0%"></div>
                </div>
                <div class="small mt-1" aria-live="polite">Fortaleza: <strong id="textoFortaleza">-</strong></div>
                <ul class="small list-unstyled mt-2 mb-0" id="requisitos">
                    <li data-req="largo"><i class="bi bi-circle"></i> Al menos 8 caracteres</li>
                    <li data-req="mayuscula"><i class="bi bi-circle"></i> Una letra mayúscula</li>
                    <li data-req="minuscula"><i class="bi bi-circle"></i> Una letra minúscula</li>
                    <li data-req="numero"><i class="bi bi-circle"></i> Un número</li>
                    <li data-req="usuario"><i class="bi bi-circle"></i> Distinta de su nombre de usuario</li>
                </ul>
                <div class="invalid-feedback">La nueva contraseña no cumple los requisitos.</div>
            </div>

            <div class="col-12">
                <label for="confirmacion" class="form-label">Confirmar nueva contraseña</label>
                <input type="password" class="form-control" id="confirmacion" name="confirmacion" maxlength="100"
                       autocomplete="new-password" required>
                <div class="invalid-feedback" id="errorConfirmacion">La confirmación no coincide.</div>
            </div>
        </div>
        <div class="card-footer bg-transparent d-flex justify-content-end gap-2">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/perfil">Cancelar</a>
            <button type="submit" class="btn btn-sigmud"><i class="bi bi-check-lg"></i> Cambiar contraseña</button>
        </div>
    </form>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/password.js"></script>
</body>
</html>
