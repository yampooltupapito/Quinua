<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Contribuyentes</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-person-vcard"></i> Contribuyentes</h1>
        <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/contribuyentes?accion=nuevo">
            <i class="bi bi-plus-lg"></i> Nuevo contribuyente
        </a>
    </div>


    <form class="row g-2 mb-3" method="get" action="${pageContext.request.contextPath}/contribuyentes">
        <input type="hidden" name="tamano" value="${pag.tamano}">
        <input type="hidden" name="orden" value="<c:out value='${ordenActual}'/>">
        <input type="hidden" name="dir" value="<c:out value='${dirActual}'/>">
        <div class="col-12 col-md-8 col-lg-6">
            <div class="input-group">
                <input type="text" class="form-control" name="q" maxlength="100"
                       placeholder="Buscar por nombre, razón social, DNI o RUC"
                       value="<c:out value='${q}'/>">
                <button class="btn btn-sigmud" type="submit"><i class="bi bi-search"></i> Buscar</button>
                <c:if test="${not empty q}">
                    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/contribuyentes">Limpiar</a>
                </c:if>
            </div>
        </div>
        <div class="col-12 col-md-4 d-flex align-items-center">
            <div class="form-check form-switch mb-0">
                <input class="form-check-input" type="checkbox" role="switch" id="verInactivos" name="inactivos" value="1"
                       ${verInactivos ? 'checked' : ''}>
                <label class="form-check-label" for="verInactivos">Ver también dados de baja</label>
            </div>
        </div>
    </form>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm">
            <thead class="table-light">
            <tr>
                <s:thOrden columna="dniRuc" etiqueta="DNI/RUC"/>
                <s:thOrden columna="nombre" etiqueta="Nombre o razón social"/>
                <s:thOrden columna="tipo" etiqueta="Tipo"/>
                <s:thOrden columna="telefono" etiqueta="Teléfono"/>
                <s:thOrden columna="email" etiqueta="Email"/>
                <th scope="col" class="text-end">Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="x" items="${lista}">
                <tr class="${x.estado ? '' : 'table-light text-body-secondary'}">
                    <td><c:out value="${x.dniRuc}"/></td>
                    <td>
                        <a class="fw-semibold text-decoration-none"
                           href="${pageContext.request.contextPath}/contribuyentes?accion=ficha&id=${x.idContribuyente}"
                           title="Ver ficha"><c:out value="${x.nombresRazonSocial}"/></a>
                        <c:if test="${not x.estado}"><span class="badge text-bg-danger ms-1">Dado de baja</span></c:if>
                    </td>
                    <td>
                        <span class="badge ${x.tipoPersona == 'NATURAL' ? 'text-bg-success' : 'text-bg-primary'}">
                            <c:out value="${x.tipoPersona == 'NATURAL' ? 'Natural' : 'Jurídica'}"/>
                        </span>
                    </td>
                    <td><c:out value="${x.telefono}" default="-"/></td>
                    <td><c:out value="${x.email}" default="-"/></td>
                    <td class="text-end text-nowrap">
                        <a class="btn btn-outline-primary btn-sm"
                           href="${pageContext.request.contextPath}/contribuyentes?accion=ficha&id=${x.idContribuyente}">
                            <i class="bi bi-person-lines-fill"></i> Ficha
                        </a>
                        <c:choose>
                            <c:when test="${x.estado}">
                                <a class="btn btn-outline-secondary btn-sm"
                                   href="${pageContext.request.contextPath}/contribuyentes?accion=editar&id=${x.idContribuyente}">
                                    <i class="bi bi-pencil"></i> Editar
                                </a>
                                <button type="button" class="btn btn-outline-danger btn-sm"
                                        data-bs-toggle="modal" data-bs-target="#modalBaja"
                                        data-id="${x.idContribuyente}"
                                        data-nombre="<c:out value='${x.nombresRazonSocial}'/>">
                                    <i class="bi bi-trash"></i> Dar de baja
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="button" class="btn btn-outline-success btn-sm"
                                        data-bs-toggle="modal" data-bs-target="#modalReactivar"
                                        data-id="${x.idContribuyente}"
                                        data-nombre="<c:out value='${x.nombresRazonSocial}'/>">
                                    <i class="bi bi-arrow-counterclockwise"></i> Reactivar
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty lista}">
                <tr><td colspan="6" class="text-center text-body-secondary py-4">No se encontraron resultados.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <jsp:include page="/WEB-INF/views/fragmentos/paginacion.jsp"/>
</main>

<%-- Modal de confirmación de baja: se envía por POST --%>
<div class="modal fade" id="modalBaja" tabindex="-1" aria-labelledby="tituloBaja" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/contribuyentes">
            <input type="hidden" name="accion" value="baja">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" id="bajaId">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloBaja">Confirmar baja</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                ¿Dar de baja a <strong id="bajaNombre"></strong>?
                <div class="text-body-secondary small mt-1">El registro no se borra; deja de aparecer en el listado.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-danger">Dar de baja</button>
            </div>
        </form>
    </div>
</div>

<%-- Modal de confirmación de reactivación (POST con token CSRF) --%>
<div class="modal fade" id="modalReactivar" tabindex="-1" aria-labelledby="tituloReactivar" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/contribuyentes">
            <input type="hidden" name="accion" value="reactivar">
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

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/contribuyentes.js"></script>
</body>
</html>
