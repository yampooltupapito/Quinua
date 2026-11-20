<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="u" value="${usuario}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - <c:out value="${esNuevo ? 'Nuevo usuario' : 'Editar usuario'}"/></title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 720px;">
    <h1 class="h4 text-sigmud mb-3">
        <c:choose>
            <c:when test="${esNuevo}"><i class="bi bi-person-plus"></i> Nuevo usuario</c:when>
            <c:otherwise><i class="bi bi-pencil-square"></i> Editar usuario</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty errores}">
        <div class="alert alert-danger" role="alert">
            <strong>Revise los datos:</strong>
            <ul class="mb-0">
                <c:forEach var="e" items="${errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <form id="formUsuario" class="card card-sigmud shadow-sm" method="post"
          action="${pageContext.request.contextPath}/usuarios" novalidate data-nuevo="${esNuevo}">
        <div class="card-body row g-3">
            <input type="hidden" name="accion" value="guardar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" value="${esNuevo ? '' : u.idUsuario}">

            <div class="col-md-5">
                <label for="username" class="form-label">Nombre de usuario</label>
                <input type="text" class="form-control" id="username" name="username" maxlength="30" autocomplete="off"
                       required ${esNuevo ? '' : 'readonly'} value="<c:out value='${u.username}'/>">
                <div class="invalid-feedback">De 3 a 30 caracteres: letras, números, punto, guion o guion bajo.</div>
                <c:if test="${not esNuevo}"><div class="form-text">El nombre de usuario no se puede cambiar.</div></c:if>
            </div>

            <div class="col-md-7">
                <label for="nombres" class="form-label">Nombre completo</label>
                <input type="text" class="form-control" id="nombres" name="nombres" maxlength="100" required
                       value="<c:out value='${u.nombres}'/>">
                <div class="invalid-feedback">Obligatorio, de 3 a 100 caracteres.</div>
            </div>

            <div class="col-md-5">
                <label for="rol" class="form-label">Rol</label>
                <select id="rol" class="form-select" ${esMiCuenta ? 'disabled' : 'name="rol"'}>
                    <c:forEach var="r" items="${roles}">
                        <option value="${r}" ${u.rol == r ? 'selected' : ''}><c:out value="${r}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${esMiCuenta}">
                    <input type="hidden" name="rol" value="<c:out value='${u.rol}'/>">
                    <div class="form-text">No puede cambiar su propio rol.</div>
                </c:if>
            </div>

            <div class="col-md-7">
                <label for="idArea" class="form-label">Área <span id="marcaArea" class="text-danger d-none">(obligatoria para ATENCION)</span></label>
                <select id="idArea" name="idArea" class="form-select">
                    <option value="">Sin área</option>
                    <c:forEach var="a" items="${areas}">
                        <option value="${a.idArea}" ${u.idArea == a.idArea ? 'selected' : ''}><c:out value="${a.nombre}"/></option>
                    </c:forEach>
                </select>
                <div class="invalid-feedback">Un usuario de ATENCION debe tener un área asignada.</div>
            </div>

            <c:if test="${esNuevo}">
                <div class="col-12">
                    <div class="alert alert-info small mb-0">
                        <i class="bi bi-info-circle"></i> Al guardar, el sistema genera una <strong>contraseña temporal</strong> de 12 caracteres
                        y se la muestra una sola vez. El usuario deberá cambiarla al iniciar sesión.
                    </div>
                </div>
            </c:if>
        </div>
        <div class="card-footer bg-transparent d-flex justify-content-end gap-2">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/usuarios">Cancelar</a>
            <button type="submit" class="btn btn-sigmud">
                <i class="bi bi-check-lg"></i> <c:out value="${esNuevo ? 'Crear usuario' : 'Guardar cambios'}"/>
            </button>
        </div>
    </form>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/usuarios.js"></script>
</body>
</html>
