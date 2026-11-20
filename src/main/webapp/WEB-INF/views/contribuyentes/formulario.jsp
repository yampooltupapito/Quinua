<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="c" value="${contribuyente}"/>
<c:set var="esJuridica" value="${c.tipoPersona == 'JURIDICA'}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - <c:out value="${esNuevo ? 'Nuevo contribuyente' : 'Editar contribuyente'}"/></title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 760px;">
    <h1 class="h4 text-sigmud mb-3">
        <c:choose>
            <c:when test="${esNuevo}"><i class="bi bi-person-plus"></i> Nuevo contribuyente</c:when>
            <c:otherwise><i class="bi bi-pencil-square"></i> Editar contribuyente</c:otherwise>
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

    <form id="formContribuyente" class="card card-sigmud shadow-sm" method="post"
          action="${pageContext.request.contextPath}/contribuyentes" novalidate>
        <div class="card-body row g-3">
            <input type="hidden" name="accion" value="guardar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" value="${esNuevo ? '' : c.idContribuyente}">

            <div class="col-md-4">
                <label for="tipoPersona" class="form-label">Tipo de persona</label>
                <select id="tipoPersona" class="form-select" ${esNuevo ? 'name="tipoPersona"' : 'disabled'}>
                    <option value="NATURAL" ${!esJuridica ? 'selected' : ''}>Natural</option>
                    <option value="JURIDICA" ${esJuridica ? 'selected' : ''}>Jurídica</option>
                </select>
                <c:if test="${not esNuevo}">
                    <input type="hidden" name="tipoPersona" value="<c:out value='${c.tipoPersona}'/>">
                </c:if>
            </div>

            <div class="col-md-4">
                <label for="dniRuc" class="form-label" id="etiquetaDniRuc">DNI</label>
                <input type="text" class="form-control" id="dniRuc" name="dniRuc" inputmode="numeric"
                       maxlength="11" required ${esNuevo ? '' : 'readonly'}
                       value="<c:out value='${c.dniRuc}'/>">
                <div class="invalid-feedback" id="errorDniRuc"></div>
            </div>

            <div class="col-md-4 ${esJuridica ? 'd-none' : ''}" id="grupoFecha">
                <label for="fechaNacimiento" class="form-label">Fecha de nacimiento</label>
                <input type="date" class="form-control" id="fechaNacimiento" name="fechaNacimiento"
                       min="1900-01-01" value="<c:out value='${c.fechaNacimiento}'/>">
                <div class="invalid-feedback">Ingrese una fecha entre 1900 y hoy.</div>
            </div>

            <div class="col-12">
                <label for="nombresRazonSocial" class="form-label" id="etiquetaNombre">Nombres y apellidos</label>
                <input type="text" class="form-control" id="nombresRazonSocial" name="nombresRazonSocial"
                       maxlength="150" required value="<c:out value='${c.nombresRazonSocial}'/>">
                <div class="invalid-feedback">Obligatorio, de 3 a 150 caracteres.</div>
            </div>

            <div class="col-md-5">
                <label for="telefono" class="form-label">Teléfono <span class="text-body-secondary small">(opcional)</span></label>
                <input type="text" class="form-control" id="telefono" name="telefono" inputmode="numeric"
                       maxlength="9" value="<c:out value='${c.telefono}'/>">
                <div class="invalid-feedback">Debe tener 9 dígitos y empezar con 9.</div>
            </div>

            <div class="col-md-7">
                <label for="email" class="form-label">Correo electrónico <span class="text-body-secondary small">(opcional)</span></label>
                <input type="email" class="form-control" id="email" name="email"
                       maxlength="100" value="<c:out value='${c.email}'/>">
                <div class="invalid-feedback">Ingrese un correo válido, por ejemplo nombre@dominio.com.</div>
            </div>

            <div class="col-12">
                <label for="direccion" class="form-label">Dirección <span class="text-body-secondary small">(opcional)</span></label>
                <input type="text" class="form-control" id="direccion" name="direccion"
                       maxlength="200" value="<c:out value='${c.direccion}'/>">
                <div class="invalid-feedback">Máximo 200 caracteres.</div>
            </div>
        </div>
        <div class="card-footer bg-transparent d-flex justify-content-end gap-2">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/contribuyentes">Cancelar</a>
            <button type="submit" class="btn btn-sigmud">
                <i class="bi bi-check-lg"></i> <c:out value="${esNuevo ? 'Registrar' : 'Guardar cambios'}"/>
            </button>
        </div>
    </form>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/contribuyentes.js"></script>
</body>
</html>
