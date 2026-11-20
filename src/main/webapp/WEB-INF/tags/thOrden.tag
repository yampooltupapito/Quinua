<%@ tag language="java" pageEncoding="UTF-8" description="Encabezado de tabla ordenable: enlace con flecha y aria-sort" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags" %>
<%@ attribute name="columna" required="true" description="Nombre de la columna en la lista blanca del DAO (por ejemplo nombre)" %>
<%@ attribute name="etiqueta" required="true" description="Texto del encabezado" %>
<%@ attribute name="clase" required="false" description="Clases CSS del th (por ejemplo text-end)" %>
<c:set var="activa" value="${ordenActual == columna}"/>
<c:set var="nuevaDir" value="${activa and dirActual == 'asc' ? 'desc' : 'asc'}"/>
<th scope="col" class="${clase}" aria-sort="${activa ? (dirActual == 'asc' ? 'ascending' : 'descending') : 'none'}">
    <a class="th-orden text-reset text-decoration-none" href="<s:urlLista pagina="1" orden="${columna}" dir="${nuevaDir}"/>"
       title="Ordenar por <c:out value='${etiqueta}'/>"><c:out value="${etiqueta}"/>
        <c:choose>
            <c:when test="${activa and dirActual == 'asc'}"><i class="bi bi-sort-up" aria-hidden="true"></i></c:when>
            <c:when test="${activa}"><i class="bi bi-sort-down" aria-hidden="true"></i></c:when>
            <c:otherwise><i class="bi bi-arrow-down-up text-body-tertiary" aria-hidden="true"></i></c:otherwise>
        </c:choose>
    </a>
</th>
