<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Panel del almacén</title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<span id="contenido" tabindex="-1"></span>
<main class="container py-4" id="panel" data-panel="ALMACEN"
      data-url-datos="${pageContext.request.contextPath}/dashboard/datos"
      data-contexto="${pageContext.request.contextPath}">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-speedometer2"></i> Panel del almacén</h1>
        <div class="d-flex gap-2">
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/almacen?accion=nuevo"><i class="bi bi-plus-square"></i> Nuevo material</a>
            <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/almacen/reportes"><i class="bi bi-bar-chart-line"></i> Reportes</a>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/fragmentos/tarjetas-modulos.jsp"/>

    <c:if test="${not empty avisoPanel}"><div class="alert alert-warning"><c:out value="${avisoPanel}"/></div></c:if>

    <h2 class="h5 text-sigmud mb-3">Indicadores</h2>
    <div class="row g-3 mb-4">
        <s:kpi nombre="materiales" icono="bi-box-seam" etiqueta="Materiales activos" valor="${panel.kpis.materiales}"/>
        <s:kpi nombre="criticos" icono="bi-exclamation-triangle" etiqueta="Materiales en stock crítico" valor="${panel.kpis.criticos}" alerta="true"/>
        <s:kpi nombre="movimientosHoy" icono="bi-arrow-left-right" etiqueta="Movimientos de hoy" valor="${panel.kpis.movimientosHoy}"/>
        <s:kpi nombre="entradasMes" icono="bi-box-arrow-in-down" etiqueta="Unidades que entraron este mes" valor="${panel.kpis.entradasMes}"/>
        <s:kpi nombre="salidasMes" icono="bi-box-arrow-up" etiqueta="Unidades que salieron este mes" valor="${panel.kpis.salidasMes}"/>
    </div>

    <div class="row g-3 mb-3">
        <div class="col-12 col-lg-7">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6"><i class="bi bi-exclamation-triangle"></i> Materiales para reponer (mayor faltante)</h3>
                <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                        <thead><tr><th>Material</th><th class="text-end">Stock</th><th class="text-end">Mínimo</th><th class="text-end">Faltante</th><th></th></tr></thead>
                        <tbody id="listaReponer">
                        <c:forEach var="f" items="${panel.listas.reponer}">
                            <c:url var="urlEntrada" value="/almacen"><c:param name="q" value="${f.codigo}"/></c:url>
                            <tr>
                                <td><strong><c:out value="${f.codigo}"/></strong> <c:out value="${f.nombre}"/></td>
                                <td class="text-end"><c:out value="${f.stock}"/></td>
                                <td class="text-end"><c:out value="${f.minimo}"/></td>
                                <td class="text-end text-danger fw-bold"><c:out value="${f.faltante}"/></td>
                                <td class="text-end text-nowrap">
                                    <a class="btn btn-outline-success btn-sm" href="${urlEntrada}"><i class="bi bi-box-arrow-in-down"></i> Registrar entrada</a>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty panel.listas.reponer}">
                            <tr><td colspan="5" class="text-center text-body-secondary">Ningún material necesita reposición.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div></div>
        </div>
        <div class="col-12 col-lg-5">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6">Salidas por área (este mes)</h3>
                <figure class="m-0">
                    <div class="grafico"><canvas id="graficoSalidasArea" role="img" aria-label="Gráfico de barras horizontales de salidas por área: cargando datos"></canvas></div>
                    <figcaption class="visually-hidden" id="descSalidasArea"></figcaption>
                </figure>
                <p class="sin-datos text-center text-body-secondary d-none" data-para="graficoSalidasArea">No hay datos para mostrar</p>
            </div></div>
        </div>
    </div>

    <div class="card shadow-sm mb-3"><div class="card-body">
        <h3 class="h6"><i class="bi bi-clock-history"></i> Últimos movimientos</h3>
        <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
                <thead><tr><th>Fecha</th><th>Tipo</th><th>Material</th><th class="text-end">Cantidad</th><th>Área</th><th>Registrado por</th></tr></thead>
                <tbody id="listaMovimientos">
                <c:forEach var="f" items="${panel.listas.movimientos}">
                    <tr>
                        <td class="text-nowrap"><c:out value="${f.fecha}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${f.tipo == 'ENTRADA'}"><span class="badge text-bg-success">ENTRADA</span></c:when>
                                <c:otherwise><span class="badge text-bg-danger">SALIDA</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${f.material}"/></td>
                        <td class="text-end"><c:out value="${f.cantidad}"/></td>
                        <td><c:out value="${f.area}"/></td>
                        <td><c:out value="${f.usuario}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty panel.listas.movimientos}">
                    <tr><td colspan="6" class="text-center text-body-secondary">Aún no hay movimientos.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div></div>

    <p class="text-body-secondary small mb-0">
        <i class="bi bi-arrow-repeat"></i> Última actualización: <span id="ultimaActualizacion">--:--:--</span>
        <span id="avisoActualizacion" class="text-warning-emphasis"></span>
        (se actualiza sola cada 60 segundos)
    </p>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/chart.umd.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/dashboard.js"></script>
</body>
</html>
