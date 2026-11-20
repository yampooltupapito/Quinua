<%@ tag language="java" pageEncoding="UTF-8" description="Dirección del listado actual con otra página y/o otro orden, conservando filtros y tamaño" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="pagina" required="true" type="java.lang.Integer" description="Número de página destino" %>
<%@ attribute name="orden" required="false" description="Columna de orden (opcional; si falta se conserva el orden actual)" %>
<%@ attribute name="dir" required="false" description="asc o desc (con orden)" %>
<%-- Usa los atributos que deja ListaVista.preparar: listaBase, listaFiltros, ordenActual, dirActual y pag. --%>
<c:url value="${listaBase}">
    <c:forEach var="f" items="${listaFiltros}"><c:param name="${f.key}" value="${f.value}"/></c:forEach>
    <c:param name="orden" value="${empty orden ? ordenActual : orden}"/>
    <c:param name="dir" value="${empty orden ? dirActual : dir}"/>
    <c:param name="tamano" value="${pag.tamano}"/>
    <c:param name="pagina" value="${pagina}"/>
</c:url>
