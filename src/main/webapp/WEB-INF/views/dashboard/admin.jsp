<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Panel del administrador</title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<%-- data-*: direcciones que usa dashboard.js (el JavaScript va en un archivo externo, nunca dentro del HTML) --%>
<span id="contenido" tabindex="-1"></span>
<main class="container py-4" id="panel" data-panel="ADMIN"
      data-url-datos="${pageContext.request.contextPath}/dashboard/datos"
      data-url-desbloquear="${pageContext.request.contextPath}/dashboard/desbloquear"
      data-contexto="${pageContext.request.contextPath}">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-speedometer2"></i> Panel del administrador</h1>
        <span class="text-body-secondary small">
            <i class="bi bi-people"></i> Sesiones activas:
            <strong><c:out value="${applicationScope.sesionesActivas}" default="0"/></strong>
        </span>
    </div>

    <jsp:include page="/WEB-INF/views/fragmentos/tarjetas-modulos.jsp"/>

    <c:if test="${not empty avisoPanel}"><div class="alert alert-warning"><c:out value="${avisoPanel}"/></div></c:if>

    <h2 class="h5 text-sigmud mb-3">Indicadores</h2>
    <div class="row g-3 mb-4">
        <s:kpi nombre="usuarios" icono="bi-person-check" etiqueta="Usuarios activos" valor="${panel.kpis.usuarios}"/>
        <s:kpi nombre="contribuyentes" icono="bi-person-vcard" etiqueta="Contribuyentes activos" valor="${panel.kpis.contribuyentes}"/>
        <s:kpi nombre="materiales" icono="bi-box-seam" etiqueta="Materiales activos" valor="${panel.kpis.materiales}"/>
        <s:kpi nombre="criticos" icono="bi-exclamation-triangle" etiqueta="Materiales en stock crítico" valor="${panel.kpis.criticos}" alerta="true"/>
        <s:kpi nombre="citasHoy" icono="bi-calendar-check" etiqueta="Citas de hoy" valor="${panel.kpis.citasHoy}"/>
        <s:kpi nombre="movimientosHoy" icono="bi-arrow-left-right" etiqueta="Movimientos de hoy" valor="${panel.kpis.movimientosHoy}"/>
        <s:kpi nombre="bloqueadas" icono="bi-lock" etiqueta="Cuentas bloqueadas" valor="${panel.kpis.bloqueadas}" alerta="true"/>
        <s:kpi nombre="loginFallidos" icono="bi-shield-exclamation" etiqueta="Intentos de acceso fallidos en las últimas 24 horas" valor="${panel.kpis.loginFallidos}" alerta="true"/>
    </div>

    <h2 class="h5 text-sigmud mb-3">Gráficos</h2>
    <div class="row g-3 mb-4">
        <div class="col-12 col-lg-4">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6">Citas por estado (últimos 30 días)</h3>
                <figure class="m-0">
                    <div class="grafico"><canvas id="graficoCitasEstado" role="img" aria-label="Gráfico de dona de citas por estado: cargando datos"></canvas></div>
                    <figcaption class="visually-hidden" id="descCitasEstado"></figcaption>
                </figure>
                <p class="sin-datos text-center text-body-secondary d-none" data-para="graficoCitasEstado">No hay datos para mostrar</p>
            </div></div>
        </div>
        <div class="col-12 col-lg-8">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6">Movimientos de stock por día (últimos 14 días)</h3>
                <figure class="m-0">
                    <div class="grafico"><canvas id="graficoMovimientos" role="img" aria-label="Gráfico de barras de entradas y salidas de stock por día: cargando datos"></canvas></div>
                    <figcaption class="visually-hidden" id="descMovimientos"></figcaption>
                </figure>
                <p class="sin-datos text-center text-body-secondary d-none" data-para="graficoMovimientos">No hay datos para mostrar</p>
            </div></div>
        </div>
        <div class="col-12">
            <div class="card shadow-sm"><div class="card-body">
                <h3 class="h6">Contribuyentes registrados por mes (últimos 6 meses)</h3>
                <figure class="m-0">
                    <div class="grafico"><canvas id="graficoContribuyentes" role="img" aria-label="Gráfico de línea de contribuyentes registrados por mes: cargando datos"></canvas></div>
                    <figcaption class="visually-hidden" id="descContribuyentes"></figcaption>
                </figure>
                <p class="sin-datos text-center text-body-secondary d-none" data-para="graficoContribuyentes">No hay datos para mostrar</p>
            </div></div>
        </div>
    </div>

    <div class="row g-3 mb-3">
        <div class="col-12 col-lg-6">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6"><i class="bi bi-lock"></i> Cuentas bloqueadas</h3>
                <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                        <thead><tr><th>Usuario</th><th>Nombre</th><th class="text-end">Intentos</th><th></th></tr></thead>
                        <tbody id="listaBloqueadas">
                        <c:forEach var="f" items="${panel.listas.bloqueadas}">
                            <tr>
                                <td><c:out value="${f.username}"/></td>
                                <td><c:out value="${f.nombres}"/></td>
                                <td class="text-end"><c:out value="${f.intentos}"/></td>
                                <td class="text-end">
                                    <button type="button" class="btn btn-outline-danger btn-sm btn-desbloquear"
                                            data-id="${f.id}" data-usuario="<c:out value='${f.username}'/>">
                                        <i class="bi bi-unlock"></i> Desbloquear
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty panel.listas.bloqueadas}">
                            <tr><td colspan="4" class="text-center text-body-secondary">No hay cuentas bloqueadas.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div></div>
        </div>
        <div class="col-12 col-lg-6">
            <div class="card shadow-sm h-100"><div class="card-body">
                <h3 class="h6"><i class="bi bi-clock-history"></i> Últimos accesos</h3>
                <div class="table-responsive">
                    <table class="table table-sm align-middle mb-0">
                        <thead><tr><th>Usuario</th><th>Rol</th><th>Último acceso</th></tr></thead>
                        <tbody id="listaAccesos">
                        <c:forEach var="f" items="${panel.listas.accesos}">
                            <tr>
                                <td><c:out value="${f.nombres}"/> <span class="text-body-secondary small">(<c:out value="${f.username}"/>)</span></td>
                                <td><c:out value="${f.rol}"/></td>
                                <td><c:out value="${f.fecha}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty panel.listas.accesos}">
                            <tr><td colspan="3" class="text-center text-body-secondary">No hay accesos registrados.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div></div>
        </div>
    </div>

    <div class="card shadow-sm mb-3"><div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <h3 class="h6 mb-0"><i class="bi bi-journal-text"></i> Actividad reciente</h3>
            <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/auditoria">Ver auditoría completa</a>
        </div>
        <ul class="list-group list-group-flush" id="listaActividad">
            <c:forEach var="f" items="${panel.listas.actividad}">
                <li class="list-group-item px-0">
                    <span class="badge ${f.clase}"><c:out value="${f.accion}"/></span>
                    <span class="text-body-secondary small"><c:out value="${f.fecha}"/> · <c:out value="${f.usuario}"/></span>
                    <div class="small"><c:out value="${f.descripcion}"/></div>
                </li>
            </c:forEach>
            <c:if test="${empty panel.listas.actividad}">
                <li class="list-group-item px-0 text-body-secondary">Todavía no hay actividad registrada.</li>
            </c:if>
        </ul>
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
