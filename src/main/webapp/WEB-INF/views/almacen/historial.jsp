<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Historial de movimientos</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-clock-history"></i> Historial de movimientos</h1>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/almacen">
            <i class="bi bi-arrow-left"></i> Volver al listado
        </a>
    </div>

    <div class="card card-sigmud shadow-sm mb-3">
        <div class="card-body d-flex flex-wrap gap-4 align-items-center">
            <div>
                <div class="text-body-secondary small">Material</div>
                <strong><c:out value="${material.codigo}"/> - <c:out value="${material.nombre}"/></strong>
                <c:if test="${not material.estado}"><span class="badge text-bg-secondary">Dado de baja</span></c:if>
            </div>
            <div>
                <div class="text-body-secondary small">Stock actual</div>
                <strong><c:out value="${material.stock}"/></strong> <c:out value="${material.unidad}"/>
            </div>
            <div>
                <div class="text-body-secondary small">Stock mínimo</div>
                <c:out value="${material.stockMinimo}"/>
            </div>
            <div>
                <c:choose>
                    <c:when test="${material.esCritico()}"><span class="badge text-bg-danger">Crítico</span></c:when>
                    <c:otherwise><span class="badge text-bg-success">Normal</span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm">
            <thead class="table-light">
            <tr>
                <s:thOrden columna="fecha" etiqueta="Fecha y hora"/>
                <s:thOrden columna="tipo" etiqueta="Tipo"/>
                <s:thOrden columna="cantidad" etiqueta="Cantidad" clase="text-end"/>
                <s:thOrden columna="area" etiqueta="Área"/>
                <s:thOrden columna="usuario" etiqueta="Registrado por"/>
                <th scope="col">Observación</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="mv" items="${movimientos}">
                <tr>
                    <td class="text-nowrap"><c:out value="${mv.fechaFormateada}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${mv.tipo == 'ENTRADA'}"><span class="badge text-bg-success">ENTRADA</span></c:when>
                            <c:otherwise><span class="badge text-bg-danger">SALIDA</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-end"><c:out value="${mv.cantidad}"/></td>
                    <td><c:out value="${mv.nombreArea}" default="-"/></td>
                    <td><c:out value="${mv.nombreUsuario}"/></td>
                    <td><c:out value="${mv.observacion}" default="-"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty movimientos}">
                <tr><td colspan="6" class="text-center text-body-secondary py-4">Este material aún no tiene movimientos.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <jsp:include page="/WEB-INF/views/fragmentos/paginacion.jsp"/>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
</body>
</html>
