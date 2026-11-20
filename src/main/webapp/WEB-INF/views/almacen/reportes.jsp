<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="rep" value="${f.reporte}"/>
<c:set var="conFechas" value="${rep != 'stock'}"/>
<c:set var="conMovimientos" value="${rep == 'movimientos'}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Reportes del almacén</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reportes.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3 no-imprimir">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-bar-chart-line"></i> Reportes del almacén</h1>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/almacen">
            <i class="bi bi-arrow-left"></i> Volver al almacén
        </a>
    </div>

    <form id="formReportes" class="card card-sigmud shadow-sm mb-3 no-imprimir" method="get"
          action="${pageContext.request.contextPath}/almacen/reportes" novalidate>
        <div class="card-body">
            <%-- Elección del reporte: botones de opción con aspecto de pestañas --%>
            <div class="btn-group flex-wrap mb-3" role="group" aria-label="Reporte">
                <input type="radio" class="btn-check" name="reporte" id="repStock" value="stock" ${rep == 'stock' ? 'checked' : ''}>
                <label class="btn btn-outline-success" for="repStock"><i class="bi bi-exclamation-triangle"></i> Stock crítico</label>
                <input type="radio" class="btn-check" name="reporte" id="repMov" value="movimientos" ${rep == 'movimientos' ? 'checked' : ''}>
                <label class="btn btn-outline-success" for="repMov"><i class="bi bi-arrow-left-right"></i> Movimientos</label>
                <input type="radio" class="btn-check" name="reporte" id="repRes" value="resumen" ${rep == 'resumen' ? 'checked' : ''}>
                <label class="btn btn-outline-success" for="repRes"><i class="bi bi-table"></i> Resumen por material</label>
                <input type="radio" class="btn-check" name="reporte" id="repAreas" value="areas" ${rep == 'areas' ? 'checked' : ''}>
                <label class="btn btn-outline-success" for="repAreas"><i class="bi bi-building"></i> Salidas por área</label>
            </div>

            <div class="row g-3 align-items-end">
                <div class="col-6 col-md-3 filtro ${conFechas ? '' : 'd-none'}" data-reportes="movimientos resumen areas">
                    <label for="desde" class="form-label">Desde</label>
                    <input type="date" class="form-control" id="desde" name="desde"
                           value="<c:out value='${f.desdeTexto}'/>" ${conFechas ? '' : 'disabled'}>
                </div>
                <div class="col-6 col-md-3 filtro ${conFechas ? '' : 'd-none'}" data-reportes="movimientos resumen areas">
                    <label for="hasta" class="form-label">Hasta</label>
                    <input type="date" class="form-control" id="hasta" name="hasta"
                           value="<c:out value='${f.hastaTexto}'/>" ${conFechas ? '' : 'disabled'}>
                </div>
                <div class="col-6 col-md-3 filtro ${conMovimientos ? '' : 'd-none'}" data-reportes="movimientos">
                    <label for="tipo" class="form-label">Tipo</label>
                    <select class="form-select" id="tipo" name="tipo" ${conMovimientos ? '' : 'disabled'}>
                        <option value="">Todos</option>
                        <option value="ENTRADA" ${f.tipo == 'ENTRADA' ? 'selected' : ''}>Entrada</option>
                        <option value="SALIDA" ${f.tipo == 'SALIDA' ? 'selected' : ''}>Salida</option>
                    </select>
                </div>
                <div class="col-6 col-md-3 filtro ${conMovimientos ? '' : 'd-none'}" data-reportes="movimientos">
                    <label for="area" class="form-label">Área</label>
                    <select class="form-select" id="area" name="area" ${conMovimientos ? '' : 'disabled'}>
                        <option value="">Todas</option>
                        <c:forEach var="a" items="${areas}">
                            <option value="${a.idArea}" ${f.idArea == a.idArea ? 'selected' : ''}><c:out value="${a.nombre}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-12 col-md-6 filtro ${conMovimientos ? '' : 'd-none'}" data-reportes="movimientos">
                    <label for="material" class="form-label">Material</label>
                    <select class="form-select" id="material" name="material" ${conMovimientos ? '' : 'disabled'}>
                        <option value="">Todos</option>
                        <c:forEach var="m" items="${materiales}">
                            <option value="${m.idMaterial}" ${f.idMaterial == m.idMaterial ? 'selected' : ''}>
                                <c:out value="${m.codigo} - ${m.nombre}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-6 col-md-3">
                    <label for="orden" class="form-label">Ordenar por</label>
                    <select class="form-select" id="orden" name="orden">
                        <option value="">Orden predeterminado</option>
                        <c:forEach var="grupo" items="${opcionesOrden}">
                            <c:forEach var="op" items="${grupo.value}">
                                <option value="${op.key}" data-reporte="${grupo.key}"
                                        ${f.orden == op.key && rep == grupo.key ? 'selected' : ''}
                                        ${rep == grupo.key ? '' : 'hidden disabled'}>
                                    <c:out value="${op.value}"/>
                                </option>
                            </c:forEach>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-6 col-md-3">
                    <label for="dir" class="form-label">Sentido</label>
                    <select class="form-select" id="dir" name="dir">
                        <option value="asc" ${f.dir == 'asc' ? 'selected' : ''}>Ascendente</option>
                        <option value="desc" ${f.dir == 'desc' ? 'selected' : ''}>Descendente</option>
                    </select>
                </div>
                <div class="col-12 col-md-auto ms-md-auto">
                    <button type="submit" class="btn btn-sigmud w-100"><i class="bi bi-search"></i> Consultar</button>
                </div>
            </div>
            <div id="errorFechas" class="alert alert-danger py-2 mt-3 mb-0 d-none" role="alert"></div>
        </div>
    </form>

    <c:if test="${not empty f.errores}">
        <div class="alert alert-danger no-imprimir" role="alert">
            <ul class="mb-0">
                <c:forEach var="e" items="${f.errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <c:if test="${tabla != null}">
        <c:url var="urlCsv" value="/almacen/reportes">
            <c:param name="accion" value="csv"/>
            <c:param name="reporte" value="${rep}"/>
            <c:param name="desde" value="${f.desdeTexto}"/>
            <c:param name="hasta" value="${f.hastaTexto}"/>
            <c:param name="tipo" value="${f.tipo}"/>
            <c:param name="area" value="${f.idArea}"/>
            <c:param name="material" value="${f.idMaterial}"/>
            <c:param name="orden" value="${f.orden}"/>
            <c:param name="dir" value="${f.dir}"/>
        </c:url>

        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <div>
                <h2 class="h5 mb-0"><c:out value="${tabla.titulo}"/></h2>
                <c:if test="${conFechas}">
                    <div class="text-body-secondary small">Periodo: <c:out value="${f.desdeTexto}"/> al <c:out value="${f.hastaTexto}"/></div>
                </c:if>
                <div class="solo-imprimir small">Municipalidad Distrital de Quinua - SIGMUD</div>
            </div>
            <div class="d-flex gap-2 no-imprimir">
                <a id="btnCsv" class="btn btn-outline-success ${tabla.vacia ? 'disabled' : ''}" href="${urlCsv}"
                   ${tabla.vacia ? 'aria-disabled="true" tabindex="-1"' : ''}>
                    <i class="bi bi-file-earmark-spreadsheet"></i> Exportar CSV
                </a>
                <button type="button" id="btnImprimir" class="btn btn-outline-secondary">
                    <i class="bi bi-printer"></i> Imprimir
                </button>
            </div>
        </div>

        <div class="table-responsive">
            <table id="tablaReporte" class="table table-hover table-sm align-middle bg-body shadow-sm">
                <thead class="table-light">
                <tr>
                    <c:forEach var="col" items="${tabla.columnas}" varStatus="st">
                        <th class="${tabla.numericas[st.index] ? 'text-end' : ''}"><c:out value="${col}"/></th>
                    </c:forEach>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="fila" items="${tabla.filas}">
                    <tr class="fila-dato">
                        <c:forEach var="celda" items="${fila}" varStatus="cs">
                            <td class="${tabla.numericas[cs.index] ? 'text-end' : ''}"><c:out value="${celda}"/></td>
                        </c:forEach>
                    </tr>
                </c:forEach>
                <c:if test="${tabla.vacia}">
                    <tr><td colspan="${tabla.columnas.size()}" class="text-center text-body-secondary py-4">
                        No hay datos para los filtros elegidos.
                    </td></tr>
                </c:if>
                </tbody>
                <c:if test="${not tabla.vacia}">
                    <tfoot class="table-light fw-bold">
                    <c:forEach var="fila" items="${tabla.filasTotales}">
                        <tr>
                            <c:forEach var="celda" items="${fila}" varStatus="cs">
                                <td class="${tabla.numericas[cs.index] ? 'text-end' : ''}"><c:out value="${celda}"/></td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                    </tfoot>
                </c:if>
            </table>
        </div>
    </c:if>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/reportes.js"></script>
</body>
</html>
