<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Mi panel de atención</title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<span id="contenido" tabindex="-1"></span>
<main class="container py-4" id="panel" data-panel="ATENCION"
      data-url-datos="${pageContext.request.contextPath}/dashboard/datos"
      data-contexto="${pageContext.request.contextPath}">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-speedometer2"></i> Mi panel de atención</h1>
        <div class="d-flex gap-2">
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/citas?accion=nuevo"><i class="bi bi-calendar-plus"></i> Nueva cita</a>
            <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/contribuyentes?accion=nuevo"><i class="bi bi-person-plus"></i> Nuevo contribuyente</a>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/fragmentos/tarjetas-modulos.jsp"/>

    <c:if test="${not empty avisoPanel}"><div class="alert alert-warning"><c:out value="${avisoPanel}"/></div></c:if>

    <h2 class="h5 text-sigmud mb-3">Mis citas de hoy</h2>
    <div class="row g-3 mb-4">
        <s:kpi nombre="total" icono="bi-calendar-day" etiqueta="Total de hoy" valor="${panel.kpis.total}"/>
        <s:kpi nombre="programadas" icono="bi-hourglass-split" etiqueta="Programadas" valor="${panel.kpis.programadas}"/>
        <s:kpi nombre="atendidas" icono="bi-check2-circle" etiqueta="Atendidas" valor="${panel.kpis.atendidas}"/>
        <s:kpi nombre="canceladas" icono="bi-x-circle" etiqueta="Canceladas" valor="${panel.kpis.canceladas}"/>
    </div>

    <div class="row g-3 mb-3">
        <div class="col-12 col-lg-7">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6">Mis próximas citas de hoy</h3>
                <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                        <thead><tr><th>Hora</th><th>Contribuyente</th><th>Motivo</th><th class="text-center">Vent.</th><th></th></tr></thead>
                        <tbody id="listaProximas">
                        <c:forEach var="f" items="${panel.listas.proximas}">
                            <tr>
                                <td class="text-nowrap"><strong><c:out value="${f.hora}"/></strong></td>
                                <td><c:out value="${f.contribuyente}"/><div class="text-body-secondary small"><c:out value="${f.dniRuc}"/></div></td>
                                <td><c:out value="${f.motivo}"/></td>
                                <td class="text-center"><c:out value="${f.ventanilla}"/></td>
                                <td class="text-end">
                                    <%-- Usa la acción "atender" que ya existe en /citas --%>
                                    <form class="d-inline" method="post" action="${pageContext.request.contextPath}/citas">
                                        <input type="hidden" name="accion" value="atender">
                                        <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
                                        <input type="hidden" name="id" value="${f.id}">
                                        <input type="hidden" name="fecha" value="${f.fecha}">
                                        <input type="hidden" name="area" value="${f.idArea}">
                                        <button type="submit" class="btn btn-outline-success btn-sm"><i class="bi bi-check2-circle"></i> Atender</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty panel.listas.proximas}">
                            <tr><td colspan="5" class="text-center text-body-secondary">No tiene más citas programadas para hoy.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div></div>
        </div>
        <div class="col-12 col-lg-5">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6">Mis citas atendidas (últimos 7 días)</h3>
                <figure class="m-0">
                    <div class="grafico"><canvas id="graficoAtendidas" role="img" aria-label="Gráfico de barras de mis citas atendidas por día: cargando datos"></canvas></div>
                    <figcaption class="visually-hidden" id="descAtendidas"></figcaption>
                </figure>
                <p class="sin-datos text-center text-body-secondary d-none" data-para="graficoAtendidas">No hay datos para mostrar</p>
            </div></div>
        </div>
    </div>

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
