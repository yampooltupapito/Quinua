<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - Usuarios</title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<span id="contenido" tabindex="-1"></span>
<main class="container py-4" id="paginaUsuarios"
      data-url-restablecer="${pageContext.request.contextPath}/usuarios/restablecer"
      data-url-desbloquear="${pageContext.request.contextPath}/usuarios/desbloquear">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 class="h4 text-sigmud mb-0"><i class="bi bi-people"></i> Usuarios</h1>
        <a class="btn btn-sigmud" href="${pageContext.request.contextPath}/usuarios?accion=nuevo">
            <i class="bi bi-person-plus"></i> Nuevo usuario
        </a>
    </div>


    <%-- Contraseña temporal del usuario recién creado: se muestra UNA sola vez --%>
    <c:if test="${not empty credencialPassword}">
        <div class="alert alert-warning" role="alert" id="cajaCredencial">
            <div class="fw-bold mb-1"><i class="bi bi-key"></i> Contraseña temporal de <c:out value="${credencialUsuario}"/></div>
            <div class="small mb-2">Cópiela y entréguela ahora: <strong>no se volverá a mostrar</strong>. El usuario deberá cambiarla al iniciar sesión.</div>
            <div class="input-group" style="max-width: 420px;">
                <input type="text" class="form-control font-monospace" id="credencialValor" readonly
                       value="<c:out value='${credencialPassword}'/>" aria-label="Contraseña temporal">
                <button type="button" class="btn btn-sigmud btn-copiar" data-origen="credencialValor">
                    <i class="bi bi-clipboard"></i> Copiar
                </button>
            </div>
        </div>
    </c:if>

    <form class="row g-2 mb-3" method="get" action="${pageContext.request.contextPath}/usuarios">
        <input type="hidden" name="tamano" value="${pag.tamano}">
        <input type="hidden" name="orden" value="<c:out value='${ordenActual}'/>">
        <input type="hidden" name="dir" value="<c:out value='${dirActual}'/>">
        <div class="col-12 col-md-4">
            <input type="text" class="form-control" name="q" maxlength="100" placeholder="Buscar por usuario o nombre"
                   value="<c:out value='${q}'/>">
        </div>
        <div class="col-6 col-md-3">
            <select class="form-select" name="rol" aria-label="Rol">
                <option value="">Todos los roles</option>
                <c:forEach var="r" items="${roles}">
                    <option value="${r}" ${rolSel == r ? 'selected' : ''}><c:out value="${r}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-6 col-md-3">
            <select class="form-select" name="estado" aria-label="Estado">
                <option value="">Todos los estados</option>
                <option value="1" ${estadoSel == '1' ? 'selected' : ''}>Activos</option>
                <option value="0" ${estadoSel == '0' ? 'selected' : ''}>Inactivos</option>
            </select>
        </div>
        <div class="col-12 col-md-2 d-flex gap-2">
            <button class="btn btn-sigmud flex-fill" type="submit"><i class="bi bi-search"></i> Filtrar</button>
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/usuarios" title="Quitar filtros"><i class="bi bi-x-lg"></i></a>
        </div>
    </form>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-body shadow-sm" id="tablaUsuarios">
            <thead class="table-light">
            <tr>
                <s:thOrden columna="username" etiqueta="Usuario"/><s:thOrden columna="nombre" etiqueta="Nombre"/><s:thOrden columna="rol" etiqueta="Rol"/><s:thOrden columna="area" etiqueta="Área"/><s:thOrden columna="estado" etiqueta="Estado"/><s:thOrden columna="acceso" etiqueta="Último acceso"/><th scope="col" class="text-end">Acciones</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="x" items="${lista}">
                <c:set var="esYo" value="${x.idUsuario == miId}"/>
                <tr data-id="${x.idUsuario}" data-username="<c:out value='${x.username}'/>">
                    <td><strong><c:out value="${x.username}"/></strong>
                        <c:if test="${esYo}"><span class="badge text-bg-light border">usted</span></c:if>
                    </td>
                    <td><c:out value="${x.nombres}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${x.rol == 'ADMIN'}"><span class="badge text-bg-danger">ADMIN</span></c:when>
                            <c:when test="${x.rol == 'ATENCION'}"><span class="badge text-bg-primary">ATENCION</span></c:when>
                            <c:otherwise><span class="badge text-bg-warning">ALMACEN</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:out value="${x.nombreArea}" default="-"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${x.bloqueada}"><span class="badge text-bg-danger"><i class="bi bi-lock"></i> Bloqueada</span></c:when>
                            <c:when test="${x.estado}"><span class="badge text-bg-success">Activo</span></c:when>
                            <c:otherwise><span class="badge text-bg-secondary">Inactivo</span></c:otherwise>
                        </c:choose>
                        <c:if test="${x.debeCambiarPassword}"><span class="badge text-bg-warning" title="Debe cambiar su contraseña al ingresar">Clave temporal</span></c:if>
                    </td>
                    <td class="text-nowrap"><c:out value="${x.ultimoAccesoTexto}"/></td>
                    <td class="text-end text-nowrap">
                        <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/usuarios?accion=editar&id=${x.idUsuario}">
                            <i class="bi bi-pencil"></i> Editar
                        </a>
                        <button type="button" class="btn btn-outline-secondary btn-sm btn-restablecer"
                                data-id="${x.idUsuario}" data-usuario="<c:out value='${x.username}'/>" ${esYo ? 'disabled title="Use Mi perfil para cambiar su propia contraseña"' : ''}>
                            <i class="bi bi-key"></i> Restablecer clave
                        </button>
                        <c:if test="${x.bloqueada}">
                            <button type="button" class="btn btn-outline-danger btn-sm btn-desbloquear-usuario"
                                    data-id="${x.idUsuario}" data-usuario="<c:out value='${x.username}'/>">
                                <i class="bi bi-unlock"></i> Desbloquear
                            </button>
                        </c:if>
                        <button type="button" class="btn btn-sm ${x.estado ? 'btn-outline-danger' : 'btn-outline-success'}"
                                data-bs-toggle="modal" data-bs-target="#modalEstado"
                                data-id="${x.idUsuario}" data-usuario="<c:out value='${x.username}'/>"
                                data-activo="${x.estado ? 0 : 1}" ${esYo ? 'disabled title="No puede desactivar su propia cuenta"' : ''}>
                            <c:choose>
                                <c:when test="${x.estado}"><i class="bi bi-person-dash"></i> Desactivar</c:when>
                                <c:otherwise><i class="bi bi-person-check"></i> Activar</c:otherwise>
                            </c:choose>
                        </button>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty lista}">
                <tr><td colspan="7" class="text-center text-body-secondary py-4">No se encontraron usuarios.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <jsp:include page="/WEB-INF/views/fragmentos/paginacion.jsp"/>
</main>

<%-- Modal: activar / desactivar (POST) --%>
<div class="modal fade" id="modalEstado" tabindex="-1" aria-labelledby="tituloEstado" aria-hidden="true">
    <div class="modal-dialog">
        <form class="modal-content" method="post" action="${pageContext.request.contextPath}/usuarios">
            <input type="hidden" name="accion" value="estado">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" id="estadoId">
            <input type="hidden" name="activo" id="estadoActivo">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloEstado">Confirmar</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                <span id="estadoTexto"></span> <strong id="estadoUsuario"></strong>?
                <div class="text-body-secondary small mt-1">Los usuarios nunca se borran: solo se activan o desactivan.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-sigmud">Confirmar</button>
            </div>
        </form>
    </div>
</div>

<%-- Modal: contraseña temporal generada por "Restablecer clave" (se muestra una sola vez) --%>
<div class="modal fade" id="modalPassword" tabindex="-1" aria-labelledby="tituloPassword" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title h5" id="tituloPassword"><i class="bi bi-key"></i> Contraseña temporal</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body">
                <p class="mb-2">Nueva contraseña temporal de <strong id="passwordUsuario"></strong>:</p>
                <div class="input-group">
                    <input type="text" class="form-control font-monospace" id="passwordValor" readonly aria-label="Contraseña temporal">
                    <button type="button" class="btn btn-sigmud btn-copiar" data-origen="passwordValor"><i class="bi bi-clipboard"></i> Copiar</button>
                </div>
                <div class="alert alert-warning small mt-3 mb-0">
                    Cópiela y entréguela ahora: <strong>no se volverá a mostrar</strong>. El usuario deberá cambiarla al iniciar sesión.
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/usuarios.js"></script>
</body>
</html>
