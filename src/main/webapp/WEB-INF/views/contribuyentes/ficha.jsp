<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Ficha del contribuyente</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/impresion.css" media="print">
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<span id="contenido" tabindex="-1"></span>
<main class="container py-4" id="ficha">

    <%-- Encabezado que solo se ve al imprimir (la fecha y hora reales las escribe contribuyentes.js al imprimir) --%>
    <div class="d-none d-print-block encabezado-impresion">
        <c:if test="${applicationScope.logoDisponible}">
            <img src="${pageContext.request.contextPath}/assets/img/logo.png" alt="Logo de la Municipalidad Distrital de Quinua" height="48" class="logo-impresion">
        </c:if>
        <h1>Municipalidad Distrital de Quinua - Ficha del contribuyente</h1>
        <p>Impreso el <span id="impresionFecha"></span> por <c:out value="${sessionScope.usuario.nombres}"/>
            (<c:out value="${sessionScope.usuario.username}"/>)</p>
    </div>


    <%-- Encabezado: nombre, tipo, documento e insignia de baja --%>
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
        <div>
            <h1 class="h3 text-sigmud mb-1">
                <i class="bi bi-person-vcard d-print-none"></i> <c:out value="${c.nombresRazonSocial}"/>
                <c:if test="${not c.estado}"><span class="badge text-bg-danger fs-6 align-middle">Dado de baja</span></c:if>
            </h1>
            <span class="badge ${c.tipoPersona == 'NATURAL' ? 'text-bg-success' : 'text-bg-primary'}">
                <c:out value="${c.tipoPersona == 'NATURAL' ? 'Persona natural' : 'Persona jurídica'}"/>
            </span>
            <span class="ms-2 text-body-secondary"><c:out value="${c.tipoPersona == 'NATURAL' ? 'DNI' : 'RUC'}"/>
                <strong class="text-body"><c:out value="${c.dniRuc}"/></strong></span>
        </div>

        <%-- Botones (no se imprimen) --%>
        <div class="d-flex flex-wrap gap-2 d-print-none">
            <c:choose>
                <c:when test="${c.estado}">
                    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/contribuyentes?accion=editar&id=${c.idContribuyente}">
                        <i class="bi bi-pencil"></i> Editar</a>
                    <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/citas?accion=nuevo&contribuyente=${c.idContribuyente}">
                        <i class="bi bi-calendar-plus"></i> Nueva cita</a>
                    <button type="button" class="btn btn-outline-danger" data-bs-toggle="modal" data-bs-target="#modalBajaFicha">
                        <i class="bi bi-trash"></i> Dar de baja</button>
                </c:when>
                <c:otherwise>
                    <button type="button" class="btn btn-outline-secondary" disabled aria-disabled="true"
                            title="Un contribuyente dado de baja no se puede editar. Reactívelo primero.">
                        <i class="bi bi-pencil"></i> Editar</button>
                    <button type="button" class="btn btn-sigmud" disabled aria-disabled="true"
                            title="No se pueden registrar citas a un contribuyente dado de baja. Reactívelo primero.">
                        <i class="bi bi-calendar-plus"></i> Nueva cita</button>
                    <button type="button" class="btn btn-success" data-bs-toggle="modal" data-bs-target="#modalReactivar"
                            data-id="${c.idContribuyente}" data-nombre="<c:out value='${c.nombresRazonSocial}'/>">
                        <i class="bi bi-arrow-counterclockwise"></i> Reactivar</button>
                </c:otherwise>
            </c:choose>
            <button type="button" class="btn btn-outline-dark" id="btnImprimir"><i class="bi bi-printer"></i> Imprimir ficha</button>
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/contribuyentes"><i class="bi bi-arrow-left"></i> Volver al listado</a>
        </div>
    </div>

    <c:if test="${not c.estado}">
        <div class="alert alert-warning d-print-none">
            <i class="bi bi-info-circle"></i> Este contribuyente está <strong>dado de baja</strong>: no se puede editar ni
            registrarle citas hasta que se reactive. Su historial se conserva y puede consultarse.
        </div>
    </c:if>

    <%-- Pestañas (la de Actividad solo existe en la página si el ADMIN la pide: el servidor decide, no el navegador) --%>
    <ul class="nav nav-tabs mb-3 d-print-none" id="pestanasFicha" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="tab-ficha" data-bs-toggle="tab" data-bs-target="#panel-ficha" type="button"
                    role="tab" aria-controls="panel-ficha" aria-selected="true">Ficha e historial</button>
        </li>
        <c:if test="${verActividad}">
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="tab-actividad" data-bs-toggle="tab" data-bs-target="#panel-actividad" type="button"
                        role="tab" aria-controls="panel-actividad" aria-selected="false">Actividad</button>
            </li>
        </c:if>
    </ul>

    <div class="tab-content">
        <div class="tab-pane fade show active" id="panel-ficha" role="tabpanel" aria-labelledby="tab-ficha" tabindex="0">

            <%-- Datos personales --%>
            <div class="card card-sigmud shadow-sm mb-4">
                <div class="card-body">
                    <h2 class="h5 mb-3"><i class="bi bi-person-lines-fill d-print-none"></i> Datos personales</h2>
                    <dl class="row mb-0">
                        <dt class="col-sm-3">Teléfono</dt><dd class="col-sm-9"><c:out value="${c.telefono}" default="-"/></dd>
                        <dt class="col-sm-3">Email</dt><dd class="col-sm-9"><c:out value="${c.email}" default="-"/></dd>
                        <dt class="col-sm-3">Dirección</dt><dd class="col-sm-9"><c:out value="${c.direccion}" default="-"/></dd>
                        <c:if test="${c.tipoPersona == 'NATURAL'}">
                            <dt class="col-sm-3">Fecha de nacimiento</dt>
                            <dd class="col-sm-9">
                                <c:choose>
                                    <c:when test="${not empty c.fechaNacimiento}">
                                        <c:out value="${c.fechaNacimientoTexto}"/>
                                        <c:if test="${not empty edad}">(<c:out value="${edad}"/> años)</c:if>
                                    </c:when>
                                    <c:otherwise>-</c:otherwise>
                                </c:choose>
                            </dd>
                        </c:if>
                        <dt class="col-sm-3">Fecha de registro</dt><dd class="col-sm-9"><c:out value="${c.fechaRegistroTexto}" default="-"/></dd>
                    </dl>
                </div>
            </div>

            <%-- Indicadores --%>
            <h2 class="h5 text-sigmud mb-3">Citas</h2>
            <div class="row g-3 mb-4">
                <div class="col-6 col-md-3 col-xl">
                    <div class="card kpi-card h-100 shadow-sm"><div class="card-body">
                        <div class="kpi-numero"><c:out value="${resumen.total}"/></div><div class="kpi-etiqueta">Total de citas</div></div></div>
                </div>
                <div class="col-6 col-md-3 col-xl">
                    <div class="card kpi-card h-100 shadow-sm"><div class="card-body">
                        <div class="kpi-numero"><c:out value="${resumen.programadas}"/></div><div class="kpi-etiqueta">Programadas</div></div></div>
                </div>
                <div class="col-6 col-md-3 col-xl">
                    <div class="card kpi-card h-100 shadow-sm"><div class="card-body">
                        <div class="kpi-numero"><c:out value="${resumen.atendidas}"/></div><div class="kpi-etiqueta">Atendidas</div></div></div>
                </div>
                <div class="col-6 col-md-3 col-xl">
                    <div class="card kpi-card h-100 shadow-sm"><div class="card-body">
                        <div class="kpi-numero"><c:out value="${resumen.canceladas}"/></div><div class="kpi-etiqueta">Canceladas</div></div></div>
                </div>
                <div class="col-12 col-xl-4">
                    <div class="card kpi-card h-100 shadow-sm"><div class="card-body">
                        <c:choose>
                            <c:when test="${not empty proxima}">
                                <div class="kpi-numero fs-4"><c:out value="${proxima.fechaTexto}"/> <c:out value="${proxima.horaTexto}"/></div>
                                <div class="kpi-etiqueta">Próxima cita - <c:out value="${proxima.nombreArea}"/>, <c:out value="${proxima.nombreFuncionario}"/></div>
                            </c:when>
                            <c:otherwise>
                                <div class="kpi-numero fs-5 text-body-secondary">Sin citas programadas</div>
                                <div class="kpi-etiqueta">Próxima cita</div>
                            </c:otherwise>
                        </c:choose>
                    </div></div>
                </div>
            </div>

            <%-- Historial de citas con filtro por estado --%>
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                <h2 class="h5 text-sigmud mb-0">Historial de citas</h2>
                <form class="d-flex gap-2 d-print-none" method="get" action="${pageContext.request.contextPath}/contribuyentes">
                    <input type="hidden" name="accion" value="ficha">
                    <input type="hidden" name="id" value="${c.idContribuyente}">
                    <input type="hidden" name="tamano" value="${pag.tamano}">
                    <input type="hidden" name="orden" value="<c:out value='${ordenActual}'/>">
                    <input type="hidden" name="dir" value="<c:out value='${dirActual}'/>">
                    <label class="visually-hidden" for="estado">Estado</label>
                    <select class="form-select form-select-sm" id="estado" name="estado">
                        <option value="">Todos los estados</option>
                        <option value="PROGRAMADA" ${estadoFiltro == 'PROGRAMADA' ? 'selected' : ''}>Programadas</option>
                        <option value="ATENDIDA" ${estadoFiltro == 'ATENDIDA' ? 'selected' : ''}>Atendidas</option>
                        <option value="CANCELADA" ${estadoFiltro == 'CANCELADA' ? 'selected' : ''}>Canceladas</option>
                    </select>
                    <button type="submit" class="btn btn-sm btn-sigmud"><i class="bi bi-funnel"></i> Filtrar</button>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-sm align-middle bg-body shadow-sm">
                    <thead class="table-light">
                    <tr><s:thOrden columna="fecha" etiqueta="Fecha"/><s:thOrden columna="hora" etiqueta="Hora"/><s:thOrden columna="area" etiqueta="Área"/><s:thOrden columna="funcionario" etiqueta="Funcionario"/><s:thOrden columna="motivo" etiqueta="Motivo"/><s:thOrden columna="estado" etiqueta="Estado"/></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="ct" items="${citas}">
                        <tr>
                            <td class="text-nowrap"><c:out value="${ct.fechaTexto}"/></td>
                            <td><c:out value="${ct.horaTexto}"/></td>
                            <td><c:out value="${ct.nombreArea}"/></td>
                            <td><c:out value="${ct.nombreFuncionario}"/></td>
                            <td><c:out value="${ct.motivo}" default="-"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${ct.estado == 'PROGRAMADA'}"><span class="badge text-bg-primary">PROGRAMADA</span></c:when>
                                    <c:when test="${ct.estado == 'ATENDIDA'}"><span class="badge text-bg-success">ATENDIDA</span></c:when>
                                    <c:otherwise><span class="badge text-bg-secondary">CANCELADA</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty citas}">
                        <tr><td colspan="6" class="text-center text-body-secondary py-4">
                            <c:choose>
                                <c:when test="${empty estadoFiltro}">Este contribuyente aún no tiene citas.</c:when>
                                <c:otherwise>No hay citas con el estado elegido.</c:otherwise>
                            </c:choose>
                        </td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
    <jsp:include page="/WEB-INF/views/fragmentos/paginacion.jsp"/>
        </div>

        <c:if test="${verActividad}">
            <div class="tab-pane fade" id="panel-actividad" role="tabpanel" aria-labelledby="tab-actividad" tabindex="0">
                <h2 class="h5 text-sigmud mb-2">Actividad (últimos 10 eventos de auditoría)</h2>
                <div class="table-responsive">
                    <table class="table table-sm align-middle bg-body shadow-sm">
                        <thead class="table-light"><tr><th>Fecha y hora</th><th>Usuario</th><th>Acción</th><th>Descripción</th><th>Detalle</th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${actividad}">
                            <tr>
                                <td class="text-nowrap"><c:out value="${r.fechaTexto}"/></td>
                                <td><c:out value="${r.username}" default="-"/></td>
                                <td><span class="badge ${r.claseInsignia}"><c:out value="${r.accion}"/></span></td>
                                <td><c:out value="${r.descripcion}"/></td>
                                <td class="small"><c:out value="${r.detalle}" default="-"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty actividad}">
                            <tr><td colspan="5" class="text-center text-body-secondary py-4">Sin eventos registrados para este contribuyente.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <a class="small" href="${pageContext.request.contextPath}/auditoria?entidad=CONTRIBUYENTE">Ver toda la auditoría</a>
            </div>
        </c:if>
    </div>
</main>

<%-- Modal de baja desde la ficha (POST con token CSRF; al terminar vuelve a la ficha) --%>
<c:if test="${c.estado}">
    <div class="modal fade" id="modalBajaFicha" tabindex="-1" aria-labelledby="tituloBajaFicha" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" method="post" action="${pageContext.request.contextPath}/contribuyentes">
                <input type="hidden" name="accion" value="baja">
                <input type="hidden" name="volver" value="ficha">
                <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
                <input type="hidden" name="id" value="${c.idContribuyente}">
                <div class="modal-header">
                    <h2 class="modal-title h5" id="tituloBajaFicha">Confirmar baja</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                </div>
                <div class="modal-body">
                    ¿Dar de baja a <strong><c:out value="${c.nombresRazonSocial}"/></strong>?
                    <div class="text-body-secondary small mt-1">El registro no se borra: se conserva con su historial y se puede reactivar.</div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-danger">Dar de baja</button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<%-- Modal de reactivación (solo si está dado de baja) --%>
<c:if test="${not c.estado}">
    <div class="modal fade" id="modalReactivar" tabindex="-1" aria-labelledby="tituloReactivar" aria-hidden="true">
        <div class="modal-dialog">
            <form class="modal-content" method="post" action="${pageContext.request.contextPath}/contribuyentes">
                <input type="hidden" name="accion" value="reactivar">
                <input type="hidden" name="volver" value="ficha">
                <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
                <input type="hidden" name="id" id="reactivarId">
                <div class="modal-header">
                    <h2 class="modal-title h5" id="tituloReactivar">Confirmar reactivación</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                </div>
                <div class="modal-body">¿Reactivar a <strong id="reactivarNombre"></strong>? Volverá a aparecer en el listado.</div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-success">Reactivar</button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/contribuyentes.js"></script>
</body>
</html>
