<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<%-- Paginación reutilizable. Usa lo que deja ListaVista.preparar: pag, listaBase, listaFiltros, ordenActual, dirActual.
     Primera, Anterior, números cercanos, Siguiente, Última, "Mostrando X a Y de Z" y selector de tamaño (10, 20 o 50). --%>
<c:set var="ini" value="${pag.paginaActual - 2 < 1 ? 1 : pag.paginaActual - 2}"/>
<c:set var="fin" value="${pag.paginaActual + 2 > pag.totalPaginas ? pag.totalPaginas : pag.paginaActual + 2}"/>
<nav class="paginacion d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 d-print-none" aria-label="Paginación del listado">
    <div class="small text-body-secondary" role="status">
        <c:choose>
            <c:when test="${pag.totalRegistros == 0}">Sin registros</c:when>
            <c:otherwise>Mostrando <strong><c:out value="${pag.desde}"/></strong> a <strong><c:out value="${pag.hasta}"/></strong>
                de <strong><c:out value="${pag.totalRegistros}"/></strong></c:otherwise>
        </c:choose>
    </div>

    <ul class="pagination pagination-sm mb-0 flex-wrap">
        <li class="page-item ${pag.hayAnterior ? '' : 'disabled'}">
            <a class="page-link" href="<s:urlLista pagina="1"/>" aria-label="Primera página"
               ${pag.hayAnterior ? '' : 'tabindex="-1" aria-disabled="true"'}><i class="bi bi-chevron-double-left" aria-hidden="true"></i> Primera</a>
        </li>
        <li class="page-item ${pag.hayAnterior ? '' : 'disabled'}">
            <a class="page-link" href="<s:urlLista pagina="${pag.paginaActual - 1}"/>" aria-label="Página anterior"
               ${pag.hayAnterior ? '' : 'tabindex="-1" aria-disabled="true"'}><i class="bi bi-chevron-left" aria-hidden="true"></i> Anterior</a>
        </li>
        <c:forEach var="n" begin="${ini}" end="${fin}">
            <li class="page-item ${n == pag.paginaActual ? 'active' : ''}">
                <a class="page-link" href="<s:urlLista pagina="${n}"/>" aria-label="Página ${n}"
                   ${n == pag.paginaActual ? 'aria-current="page"' : ''}><c:out value="${n}"/></a>
            </li>
        </c:forEach>
        <li class="page-item ${pag.haySiguiente ? '' : 'disabled'}">
            <a class="page-link" href="<s:urlLista pagina="${pag.paginaActual + 1}"/>" aria-label="Página siguiente"
               ${pag.haySiguiente ? '' : 'tabindex="-1" aria-disabled="true"'}>Siguiente <i class="bi bi-chevron-right" aria-hidden="true"></i></a>
        </li>
        <li class="page-item ${pag.haySiguiente ? '' : 'disabled'}">
            <a class="page-link" href="<s:urlLista pagina="${pag.totalPaginas}"/>" aria-label="Última página"
               ${pag.haySiguiente ? '' : 'tabindex="-1" aria-disabled="true"'}>Última <i class="bi bi-chevron-double-right" aria-hidden="true"></i></a>
        </li>
    </ul>

    <%-- Selector de tamaño: conserva los filtros y el orden actuales (campos ocultos) y vuelve a la página 1 --%>
    <form class="d-flex align-items-center gap-1" method="get" action="${pageContext.request.contextPath}${listaBase}">
        <c:forEach var="f" items="${listaFiltros}"><input type="hidden" name="<c:out value='${f.key}'/>" value="<c:out value='${f.value}'/>"></c:forEach>
        <input type="hidden" name="orden" value="<c:out value='${ordenActual}'/>">
        <input type="hidden" name="dir" value="<c:out value='${dirActual}'/>">
        <label for="selTamano" class="small text-body-secondary mb-0">Por página</label>
        <select id="selTamano" name="tamano" class="form-select form-select-sm w-auto selector-tamano">
            <option value="10" ${pag.tamano == 10 ? 'selected' : ''}>10</option>
            <option value="20" ${pag.tamano == 20 ? 'selected' : ''}>20</option>
            <option value="50" ${pag.tamano == 50 ? 'selected' : ''}>50</option>
        </select>
        <button type="submit" class="btn btn-sm btn-outline-secondary">Aplicar</button>
    </form>
</nav>
<script src="${pageContext.request.contextPath}/assets/js/paginacion.js"></script>
