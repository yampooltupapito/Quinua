<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Auditoría</title>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<%-- Direcciones de paginación y de exportación (c:url codifica los filtros) --%>
<c:url var="urlAnterior" value="/auditoria">
    <c:param name="desde" value="${desdeTexto}"/><c:param name="hasta" value="${hastaTexto}"/>
    <c:param name="usuario" value="${f.username}"/><c:param name="accion" value="${f.accion}"/>
    <c:param name="entidad" value="${f.entidad}"/><c:param name="q" value="${f.texto}"/>
    <c:param name="pagina" value="${pagina - 1}"/>
</c:url>
<c:url var="urlSiguiente" value="/auditoria">
    <c:param name="desde" value="${desdeTexto}"/><c:param name="hasta" value="${hastaTexto}"/>
    <c:param name="usuario" value="${f.username}"/><c:param name="accion" value="${f.accion}"/>
    <c:param name="entidad" value="${f.entidad}"/><c:param name="q" value="${f.texto}"/>
    <c:param name="pagina" value="${pagina + 1}"/>
</c:url>
<c:url var="urlCsv" value="/auditoria">
    <c:param name="formato" value="csv"/>
    <c:param name="desde" value="${desdeTexto}"/><c:param name="hasta" value="${hastaTexto}"/>
    <c:param name="usuario" value="${f.username}"/><c:param name="accion" value="${f.accion}"/>
    <c:param name="entidad" value="${f.entidad}"/><c:param name="q" value="${f.texto}"/>
</c:url>

<main id="contenido" tabindex="-1" class="container-xl py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-journal-text"></i> Auditoría de cambios</h1>
        <span class="text-body-secondary small"><i class="bi bi-lock"></i> Solo lectura: los registros no se pueden editar ni eliminar</span>
    </div>

    <form id="formAuditoria" class="card card-sigmud shadow-sm mb-3" method="get" action="${pageContext.request.contextPath}/auditoria" novalidate>
        <div class="card-body row g-2 align-items-end">
            <div class="col-6 col-md-2">
                <label for="desde" class="form-label mb-0 small">Desde</label>
                <input type="date" class="form-control" id="desde" name="desde" value="<c:out value='${desdeTexto}'/>">
            </div>
            <div class="col-6 col-md-2">
                <label for="hasta" class="form-label mb-0 small">Hasta</label>
                <input type="date" class="form-control" id="hasta" name="hasta" value="<c:out value='${hastaTexto}'/>">
            </div>
            <div class="col-6 col-md-2">
                <label for="usuario" class="form-label mb-0 small">Usuario</label>
                <input type="text" class="form-control" id="usuario" name="usuario" maxlength="40" value="<c:out value='${f.username}'/>">
            </div>
            <div class="col-6 col-md-2">
                <label for="accion" class="form-label mb-0 small">Acción</label>
                <select class="form-select" id="accion" name="accion">
                    <option value="">Todas</option>
                    <c:forEach var="a" items="${acciones}">
                        <option value="${a}" ${f.accion == a ? 'selected' : ''}><c:out value="${a}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <label for="entidad" class="form-label mb-0 small">Entidad</label>
                <select class="form-select" id="entidad" name="entidad">
                    <option value="">Todas</option>
                    <c:forEach var="e" items="${entidades}">
                        <option value="${e}" ${f.entidad == e ? 'selected' : ''}><c:out value="${e}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <label for="q" class="form-label mb-0 small">Texto en la descripción</label>
                <input type="text" class="form-control" id="q" name="q" maxlength="100" value="<c:out value='${f.texto}'/>">
            </div>
            <div class="col-12 d-flex flex-wrap gap-2 justify-content-between">
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-sigmud"><i class="bi bi-search"></i> Filtrar</button>
                    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/auditoria">Quitar filtros</a>
                </div>
                <a id="btnCsv" class="btn btn-outline-success ${total == 0 || not empty errores ? 'disabled' : ''}" href="${urlCsv}">
                    <i class="bi bi-file-earmark-spreadsheet"></i> Exportar CSV (máx. 10000 filas)
                </a>
            </div>
            <div class="col-12"><div id="errorFechas" class="alert alert-danger py-2 mb-0 d-none" role="alert"></div></div>
        </div>
    </form>

    <c:if test="${not empty errores}">
        <div class="alert alert-danger" role="alert">
            <ul class="mb-0">
                <c:forEach var="e" items="${errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 small text-body-secondary">
        <span><strong><c:out value="${total}"/></strong> registros encontrados</span>
        <span>Página <c:out value="${pagina}"/> de <c:out value="${paginas}"/> (<c:out value="${tamanoPagina}"/> por página)</span>
    </div>

    <div class="table-responsive">
        <table class="table table-hover table-sm align-middle bg-body shadow-sm" id="tablaAuditoria">
            <thead class="table-light">
            <tr><th>Fecha y hora</th><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Descripción</th><th>IP</th></tr>
            </thead>
            <tbody>
            <c:forEach var="r" items="${lista}">
                <tr class="fila-auditoria" role="button" tabindex="0" title="Clic para ver el detalle"
                    data-bs-toggle="modal" data-bs-target="#modalDetalle"
                    data-fecha="<c:out value='${r.fechaTexto}'/>"
                    data-usuario="<c:out value='${r.username}'/>"
                    data-accion="<c:out value='${r.accion}'/>"
                    data-entidad="<c:out value='${r.entidad}'/>"
                    data-ident="<c:out value='${r.idEntidad}'/>"
                    data-descripcion="<c:out value='${r.descripcion}'/>"
                    data-detalle="<c:out value='${r.detalle}'/>"
                    data-ip="<c:out value='${r.ip}'/>">
                    <td class="text-nowrap"><c:out value="${r.fechaTexto}"/></td>
                    <td><c:out value="${r.username}" default="-"/></td>
                    <td><span class="badge ${r.claseInsignia}"><c:out value="${r.accion}"/></span></td>
                    <td><c:out value="${r.entidad}"/><c:if test="${not empty r.idEntidad}"> <span class="text-body-secondary">#<c:out value="${r.idEntidad}"/></span></c:if></td>
                    <td><c:out value="${r.descripcion}"/></td>
                    <td class="text-nowrap"><c:out value="${r.ip}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty lista}">
                <tr><td colspan="6" class="text-center text-body-secondary py-4">No hay registros para los filtros elegidos.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>

    <nav class="d-flex justify-content-between align-items-center" aria-label="Paginación">
        <a class="btn btn-outline-secondary ${pagina <= 1 ? 'disabled' : ''}" href="${urlAnterior}"
           ${pagina <= 1 ? 'aria-disabled="true" tabindex="-1"' : ''}><i class="bi bi-chevron-left"></i> Anterior</a>
        <span class="small text-body-secondary">Página <c:out value="${pagina}"/> de <c:out value="${paginas}"/></span>
        <a class="btn btn-outline-secondary ${pagina >= paginas ? 'disabled' : ''}" href="${urlSiguiente}"
           ${pagina >= paginas ? 'aria-disabled="true" tabindex="-1"' : ''}>Siguiente <i class="bi bi-chevron-right"></i></a>
    </nav>
</main>

<%-- Modal con el detalle completo del registro (solo lectura) --%>
<div class="modal fade" id="modalDetalle" tabindex="-1" aria-labelledby="tituloDetalle" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloDetalle"><i class="bi bi-journal-text"></i> Detalle del registro</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                <dl class="row mb-3">
                    <dt class="col-sm-3">Fecha y hora</dt><dd class="col-sm-9" id="detFecha"></dd>
                    <dt class="col-sm-3">Usuario</dt><dd class="col-sm-9" id="detUsuario"></dd>
                    <dt class="col-sm-3">Acción</dt><dd class="col-sm-9" id="detAccion"></dd>
                    <dt class="col-sm-3">Entidad</dt><dd class="col-sm-9" id="detEntidad"></dd>
                    <dt class="col-sm-3">Descripción</dt><dd class="col-sm-9" id="detDescripcion"></dd>
                    <dt class="col-sm-3">IP</dt><dd class="col-sm-9" id="detIp"></dd>
                </dl>
                <h3 class="h6">Cambios campo por campo</h3>
                <ul class="list-group" id="detDetalle"></ul>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/auditoria.js"></script>
</body>
</html>
