<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Almacén</title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-box-seam"></i> Almacén: materiales</h1>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/almacen/reportes">
                <i class="bi bi-bar-chart-line"></i> Reportes
            </a>
            <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/almacen?accion=nuevo">
                <i class="bi bi-plus-lg"></i> Nuevo material
            </a>
        </div>
    </div>

    <form class="row g-2 mb-3" method="get" action="${pageContext.request.contextPath}/almacen">
        <input type="hidden" name="tamano" value="${pag.tamano}">
        <input type="hidden" name="orden" value="<c:out value='${ordenActual}'/>">
        <input type="hidden" name="dir" value="<c:out value='${dirActual}'/>">
        <c:if test="${critico}"><input type="hidden" name="critico" value="1"></c:if>
        <div class="col-12 col-md-7 col-lg-6">
            <div class="input-group">
                <input type="text" class="form-control" name="q" maxlength="100"
                       placeholder="Buscar por código o nombre"
                       value="<c:out value='${q}'/>">
                <button class="btn btn-sigmud" type="submit"><i class="bi bi-search"></i> Buscar</button>
            </div>
        </div>
        <div class="col-12 col-md-5 col-lg-6 d-flex gap-2">
            <%-- c:url codifica el texto buscado para que sea seguro dentro de la dirección --%>
            <c:url var="urlTodos" value="/almacen"><c:param name="q" value="${q}"/></c:url>
            <c:url var="urlCritico" value="/almacen">
                <c:param name="critico" value="1"/><c:param name="q" value="${q}"/>
            </c:url>
            <c:choose>
                <c:when test="${critico}">
                    <a class="btn btn-outline-secondary" href="${urlTodos}">
                        <i class="bi bi-list-ul"></i> Ver todos
                    </a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-outline-danger" href="${urlCritico}">
                        <i class="bi bi-exclamation-triangle"></i> Ver solo stock crítico
                    </a>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty q}">
                <a class="btn btn-outline-secondary"
                   href="${pageContext.request.contextPath}/almacen${critico ? '?critico=1' : ''}">Limpiar</a>
            </c:if>
        </div>
    </form>

    <c:if test="${critico}">
        <div class="alert alert-warning py-2">
            <i class="bi bi-funnel"></i> Mostrando solo materiales con stock crítico (stock menor o igual al mínimo).
        </div>
    </c:if>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm">
            <thead class="table-light">
            <tr>
                <s:thOrden columna="codigo" etiqueta="Código"/>
                <s:thOrden columna="nombre" etiqueta="Nombre"/>
                <s:thOrden columna="unidad" etiqueta="Unidad"/>
                <s:thOrden columna="stock" etiqueta="Stock" clase="text-end"/>
                <s:thOrden columna="minimo" etiqueta="Stock mínimo" clase="text-end"/>
                <th scope="col">Estado</th>
                <th scope="col">Movimientos</th>
                <th scope="col" class="text-end">Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="x" items="${lista}">
                <tr id="fila-${x.idMaterial}">
                    <td><c:out value="${x.codigo}"/></td>
                    <td><c:out value="${x.nombre}"/></td>
                    <td><c:out value="${x.unidad}"/></td>
                    <td class="text-end celda-stock"><c:out value="${x.stock}"/></td>
                    <td class="text-end"><c:out value="${x.stockMinimo}"/></td>
                    <td class="celda-estado">
                        <c:choose>
                            <c:when test="${x.esCritico()}"><span class="badge text-bg-danger">Crítico</span></c:when>
                            <c:otherwise><span class="badge text-bg-success">Normal</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-nowrap">
                        <button type="button" class="btn btn-outline-success btn-sm"
                                data-bs-toggle="modal" data-bs-target="#modalMovimiento"
                                data-id="${x.idMaterial}" data-tipo="ENTRADA"
                                data-nombre="<c:out value='${x.codigo} - ${x.nombre}'/>">
                            <i class="bi bi-box-arrow-in-down"></i> Entrada
                        </button>
                        <button type="button" class="btn btn-outline-danger btn-sm"
                                data-bs-toggle="modal" data-bs-target="#modalMovimiento"
                                data-id="${x.idMaterial}" data-tipo="SALIDA"
                                data-nombre="<c:out value='${x.codigo} - ${x.nombre}'/>">
                            <i class="bi bi-box-arrow-up"></i> Salida
                        </button>
                        <a class="btn btn-outline-secondary btn-sm"
                           href="${pageContext.request.contextPath}/almacen/movimiento?accion=historial&id=${x.idMaterial}">
                            <i class="bi bi-clock-history"></i> Historial
                        </a>
                    </td>
                    <td class="text-end text-nowrap">
                        <a class="btn btn-outline-secondary btn-sm"
                           href="${pageContext.request.contextPath}/almacen?accion=editar&id=${x.idMaterial}">
                            <i class="bi bi-pencil"></i> Editar
                        </a>
                        <button type="button" class="btn btn-outline-danger btn-sm"
                                data-bs-toggle="modal" data-bs-target="#modalBaja"
                                data-id="${x.idMaterial}"
                                data-nombre="<c:out value='${x.nombre}'/>">
                            <i class="bi bi-trash"></i> Dar de baja
                        </button>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty lista}">
                <tr><td colspan="8" class="text-center text-body-secondary py-4">No se encontraron resultados.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <jsp:include page="/WEB-INF/views/fragmentos/paginacion.jsp"/>
</main>

<%-- Modal de confirmación de baja: se envía por POST --%>
<div class="modal fade" id="modalBaja" tabindex="-1" aria-labelledby="tituloBaja" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/almacen">
            <input type="hidden" name="accion" value="baja">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" id="bajaId">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloBaja">Confirmar baja</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                ¿Dar de baja el material <strong id="bajaNombre"></strong>?
                <div class="text-body-secondary small mt-1">El registro no se borra; deja de aparecer en el listado.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-danger">Dar de baja</button>
            </div>
        </form>
    </div>
</div>

<%-- Modal de entrada/salida: se envía con Ajax (movimientos.js) --%>
<div class="modal fade" id="modalMovimiento" tabindex="-1" aria-labelledby="tituloMovimiento" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" id="formMovimiento" method="post"
              action="${pageContext.request.contextPath}/almacen/movimiento" novalidate>
            <input type="hidden" name="accion" value="registrar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="idMaterial" id="movIdMaterial">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloMovimiento">Registrar movimiento</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body row g-3">

                <div class="col-12">
                    <label for="movMaterial" class="form-label">Material</label>
                    <input type="text" class="form-control" id="movMaterial" readonly>
                </div>
                <div class="col-md-6">
                    <label for="movTipo" class="form-label">Tipo</label>
                    <select class="form-select" id="movTipo" name="tipo">
                        <option value="ENTRADA">Entrada</option>
                        <option value="SALIDA">Salida</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label for="movCantidad" class="form-label">Cantidad</label>
                    <input type="text" class="form-control" id="movCantidad" name="cantidad"
                           inputmode="numeric" maxlength="6" required>
                    <div class="invalid-feedback">Ingrese un entero entre 1 y 100000.</div>
                </div>
                <div class="col-12 d-none" id="grupoArea">
                    <label for="movArea" class="form-label">Área solicitante</label>
                    <select class="form-select" id="movArea" name="idArea">
                        <option value="">Seleccione...</option>
                        <c:forEach var="a" items="${areas}">
                            <option value="${a.idArea}"><c:out value="${a.nombre}"/></option>
                        </c:forEach>
                    </select>
                    <div class="invalid-feedback">Seleccione el área solicitante.</div>
                </div>
                <div class="col-12">
                    <label for="movObservacion" class="form-label">Observación <span class="text-body-secondary small">(opcional)</span></label>
                    <textarea class="form-control" id="movObservacion" name="observacion" rows="2" maxlength="200"></textarea>
                    <div class="invalid-feedback">Máximo 200 caracteres.</div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-sigmud" id="movConfirmar">
                    <span class="spinner-border spinner-border-sm d-none" id="movCargando" role="status" aria-hidden="true"></span>
                    Confirmar
                </button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/almacen.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/movimientos.js"></script>
</body>
</html>
