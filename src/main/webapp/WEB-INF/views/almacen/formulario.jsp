<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="m" value="${material}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - <c:out value="${esNuevo ? 'Nuevo material' : 'Editar material'}"/></title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 720px;">
    <h1 class="h4 text-sigmud mb-3">
        <c:choose>
            <c:when test="${esNuevo}"><i class="bi bi-plus-square"></i> Nuevo material</c:when>
            <c:otherwise><i class="bi bi-pencil-square"></i> Editar material</c:otherwise>
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

    <form id="formMaterial" class="card card-sigmud shadow-sm" method="post"
          action="${pageContext.request.contextPath}/almacen" novalidate>
        <div class="card-body row g-3">
            <input type="hidden" name="accion" value="guardar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" value="${esNuevo ? '' : m.idMaterial}">

            <div class="col-md-4">
                <label for="codigo" class="form-label">Código</label>
                <input type="text" class="form-control" id="codigo" name="codigo" maxlength="7"
                       placeholder="MAT-006" required ${esNuevo ? '' : 'readonly'}
                       value="<c:out value='${m.codigo}'/>">
                <div class="invalid-feedback">Formato AAA-999: tres letras, guion y tres dígitos (ej. MAT-006).</div>
            </div>

            <div class="col-md-8">
                <label for="nombre" class="form-label">Nombre</label>
                <input type="text" class="form-control" id="nombre" name="nombre" maxlength="100" required
                       value="<c:out value='${m.nombre}'/>">
                <div class="invalid-feedback">Obligatorio, de 3 a 100 caracteres.</div>
            </div>

            <div class="col-md-6">
                <label for="unidad" class="form-label">Unidad</label>
                <select id="unidad" name="unidad" class="form-select" required>
                    <c:forEach var="u" items="${unidades}">
                        <option value="${u}" ${m.unidad == u ? 'selected' : ''}><c:out value="${u}"/></option>
                    </c:forEach>
                </select>
                <div class="invalid-feedback">Seleccione una unidad de la lista.</div>
            </div>

            <div class="col-md-6">
                <label for="stockMinimo" class="form-label">Stock mínimo</label>
                <input type="text" class="form-control" id="stockMinimo" name="stockMinimo" inputmode="numeric"
                       maxlength="9" required value="<c:out value='${stockMinimoTexto}'/>">
                <div class="invalid-feedback">Ingrese un número entero mayor o igual a 0.</div>
            </div>

            <div class="col-12">
                <div class="form-label mb-1">Stock actual</div>
                <c:choose>
                    <c:when test="${esNuevo}">
                        <div class="form-control-plaintext text-body-secondary">
                            Empieza en 0. El stock cambia solo con entradas y salidas, no se edita a mano.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="form-control-plaintext">
                            <strong><c:out value="${m.stock}"/></strong>
                            <span class="text-body-secondary small">(solo cambia con entradas y salidas)</span>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="card-footer bg-transparent d-flex justify-content-end gap-2">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/almacen">Cancelar</a>
            <button type="submit" class="btn btn-sigmud">
                <i class="bi bi-check-lg"></i> <c:out value="${esNuevo ? 'Registrar' : 'Guardar cambios'}"/>
            </button>
        </div>
    </form>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/almacen.js"></script>
</body>
</html>
