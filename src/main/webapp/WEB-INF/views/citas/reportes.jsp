<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Reporte de citas</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reportes.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3 no-imprimir">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-bar-chart-line"></i> Reporte de citas</h1>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/citas">
            <i class="bi bi-arrow-left"></i> Volver a citas
        </a>
    </div>

    <form id="formReporteCitas" class="card card-sigmud shadow-sm mb-3 no-imprimir" method="get"
          action="${pageContext.request.contextPath}/citas/reportes" novalidate>
        <div class="card-body row g-3 align-items-end">
            <div class="col-6 col-md-3">
                <label for="desde" class="form-label">Desde</label>
                <input type="date" class="form-control" id="desde" name="desde" value="<c:out value='${f.desdeTexto}'/>">
            </div>
            <div class="col-6 col-md-3">
                <label for="hasta" class="form-label">Hasta</label>
                <input type="date" class="form-control" id="hasta" name="hasta" value="<c:out value='${f.hastaTexto}'/>">
            </div>
            <div class="col-6 col-md-3">
                <label for="orden" class="form-label">Ordenar por</label>
                <select class="form-select" id="orden" name="orden">
                    <option value="">Orden predeterminado</option>
                    <c:forEach var="op" items="${opcionesOrden}">
                        <option value="${op.key}" ${f.orden == op.key ? 'selected' : ''}><c:out value="${op.value}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <label for="dir" class="form-label">Sentido</label>
                <select class="form-select" id="dir" name="dir">
                    <option value="asc" ${f.dir == 'asc' ? 'selected' : ''}>Ascendente</option>
                    <option value="desc" ${f.dir == 'desc' ? 'selected' : ''}>Descendente</option>
                </select>
            </div>
            <div class="col-12 col-md-1">
                <button type="submit" class="btn btn-sigmud w-100"><i class="bi bi-search"></i></button>
            </div>
            <div class="col-12"><div id="errorFechas" class="alert alert-danger py-2 mb-0 d-none" role="alert"></div></div>
        </div>
    </form>

    <c:if test="${not empty f.errores}">
        <div class="alert alert-danger no-imprimir" role="alert">
            <ul class="mb-0">
                <c:forEach var="e" items="${f.errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <c:if test="${porArea != null}">
        <c:url var="urlCsv" value="/citas/reportes">
            <c:param name="accion" value="csv"/>
            <c:param name="desde" value="${f.desdeTexto}"/>
            <c:param name="hasta" value="${f.hastaTexto}"/>
            <c:param name="orden" value="${f.orden}"/>
            <c:param name="dir" value="${f.dir}"/>
        </c:url>

        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <div>
                <div class="text-body-secondary small">Periodo: <c:out value="${f.desdeTexto}"/> al <c:out value="${f.hastaTexto}"/></div>
                <div class="solo-imprimir small">Municipalidad Distrital de Quinua - SIGMUD</div>
            </div>
            <div class="d-flex gap-2 no-imprimir">
                <a id="btnCsv" class="btn btn-outline-success ${porArea.vacia ? 'disabled' : ''}" href="${urlCsv}"
                   ${porArea.vacia ? 'aria-disabled="true" tabindex="-1"' : ''}>
                    <i class="bi bi-file-earmark-spreadsheet"></i> Exportar CSV
                </a>
                <button type="button" id="btnImprimir" class="btn btn-outline-secondary">
                    <i class="bi bi-printer"></i> Imprimir
                </button>
            </div>
        </div>

        <div id="tablasReporte">
            <c:forEach var="tabla" items="${[porArea, porFuncionario]}">
                <h2 class="h5 mt-4"><c:out value="${tabla.titulo}"/></h2>
                <div class="table-responsive">
                    <table class="table table-hover table-sm align-middle bg-body shadow-sm">
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
            </c:forEach>
        </div>
    </c:if>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/citas.js"></script>
</body>
</html>
