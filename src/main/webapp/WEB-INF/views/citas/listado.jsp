<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Citas</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<%-- Direcciones de día anterior/siguiente y del reporte (c:url codifica los parámetros) --%>
<c:url var="urlAnterior" value="/citas">
    <c:param name="fecha" value="${fechaAnterior}"/><c:param name="area" value="${areaSel}"/>
</c:url>
<c:url var="urlSiguiente" value="/citas">
    <c:param name="fecha" value="${fechaSiguiente}"/><c:param name="area" value="${areaSel}"/>
</c:url>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-calendar-check"></i> Citas</h1>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/citas/reportes">
                <i class="bi bi-bar-chart-line"></i> Reportes
            </a>
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/citas?accion=nuevo">
                <i class="bi bi-plus-lg"></i> Nueva cita
            </a>
        </div>
    </div>


    <form class="row g-2 align-items-end mb-3" method="get" action="${pageContext.request.contextPath}/citas">
        <div class="col-auto">
            <a class="btn btn-outline-secondary" href="${urlAnterior}" title="Día anterior">
                <i class="bi bi-chevron-left"></i> Día anterior
            </a>
        </div>
        <div class="col-6 col-md-3">
            <label for="fecha" class="form-label mb-0 small">Fecha</label>
            <input type="date" class="form-control" id="fecha" name="fecha" value="${fecha}">
        </div>
        <div class="col-6 col-md-3">
            <label for="area" class="form-label mb-0 small">Área</label>
            <select class="form-select" id="area" name="area">
                <option value="">Todas</option>
                <c:forEach var="a" items="${areas}">
                    <option value="${a.idArea}" ${areaSel == a.idArea ? 'selected' : ''}><c:out value="${a.nombre}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-auto">
            <button type="submit" class="btn btn-sigmud"><i class="bi bi-search"></i> Ver</button>
        </div>
        <div class="col-auto">
            <a class="btn btn-outline-secondary" href="${urlSiguiente}" title="Día siguiente">
                Día siguiente <i class="bi bi-chevron-right"></i>
            </a>
        </div>
    </form>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm">
            <thead class="table-light">
            <tr>
                <th>Hora</th><th>Contribuyente</th><th>DNI/RUC</th><th>Área</th><th>Funcionario</th>
                <th class="text-center">Ventanilla</th><th>Motivo</th><th>Estado</th><th class="text-end">Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="x" items="${lista}">
                <tr>
                    <td class="text-nowrap"><strong><c:out value="${x.hora}"/></strong></td>
                    <td><c:out value="${x.nombreContribuyente}"/></td>
                    <td><c:out value="${x.dniRuc}"/></td>
                    <td><c:out value="${x.nombreArea}"/></td>
                    <td><c:out value="${x.nombreFuncionario}"/></td>
                    <td class="text-center"><c:out value="${x.ventanilla}"/></td>
                    <td><c:out value="${x.motivo}" default="-"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${x.estado == 'PROGRAMADA'}"><span class="badge text-bg-primary">PROGRAMADA</span></c:when>
                            <c:when test="${x.estado == 'ATENDIDA'}"><span class="badge text-bg-success">ATENDIDA</span></c:when>
                            <c:otherwise><span class="badge text-bg-secondary">CANCELADA</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-end text-nowrap">
                        <c:if test="${x.estado == 'PROGRAMADA'}">
                            <form class="d-inline" method="post" action="${pageContext.request.contextPath}/citas">
                                <input type="hidden" name="accion" value="atender">
                                <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
                                <input type="hidden" name="id" value="${x.idCita}">
                                <input type="hidden" name="fecha" value="${fecha}">
                                <input type="hidden" name="area" value="${areaSel}">
                                <button type="submit" class="btn btn-outline-success btn-sm"
                                        ${esFutura ? 'disabled title="Solo se puede atender el día de la cita o después"' : ''}>
                                    <i class="bi bi-check2-circle"></i> Atender
                                </button>
                            </form>
                            <button type="button" class="btn btn-outline-secondary btn-sm"
                                    data-bs-toggle="modal" data-bs-target="#modalReprogramar"
                                    data-nombre="<c:out value='${x.nombreContribuyente}'/>"
                                    data-url="${pageContext.request.contextPath}/citas?accion=editar&id=${x.idCita}">
                                <i class="bi bi-calendar2-event"></i> Reprogramar
                            </button>
                            <button type="button" class="btn btn-outline-danger btn-sm"
                                    data-bs-toggle="modal" data-bs-target="#modalCancelar"
                                    data-id="${x.idCita}" data-nombre="<c:out value='${x.nombreContribuyente}'/>">
                                <i class="bi bi-x-circle"></i> Cancelar
                            </button>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty lista}">
                <tr><td colspan="9" class="text-center text-body-secondary py-4">No hay citas para este día.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</main>

<%-- Modal de confirmación: reprogramar (lleva al formulario) --%>
<div class="modal fade" id="modalReprogramar" tabindex="-1" aria-labelledby="tituloReprogramar" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloReprogramar">Reprogramar cita</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                ¿Reprogramar la cita de <strong id="reprogramarNombre"></strong>?
                <div class="text-body-secondary small mt-1">Podrá elegir una nueva fecha, hora y ventanilla.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">No</button>
                <a id="reprogramarContinuar" class="btn btn-sigmud" href="#">Continuar</a>
            </div>
        </div>
    </div>
</div>

<%-- Modal de confirmación: cancelar (POST) --%>
<div class="modal fade" id="modalCancelar" tabindex="-1" aria-labelledby="tituloCancelar" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/citas">
            <input type="hidden" name="accion" value="cancelar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" id="cancelarId">
            <input type="hidden" name="fecha" value="${fecha}">
            <input type="hidden" name="area" value="${areaSel}">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloCancelar">Cancelar cita</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                ¿Cancelar la cita de <strong id="cancelarNombre"></strong>?
                <div class="text-body-secondary small mt-1">El horario quedará libre para otra cita.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">No</button>
                <button type="submit" class="btn btn-danger">Sí, cancelar</button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/citas.js"></script>
</body>
</html>
