<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Avisos (toasts): contenedor accesible (esquina superior derecha, aria-live) y, si el servidor dejó un mensaje de una
     sola vez (atributos "mensaje" y "mensajeTipo"), un elemento OCULTO con data-* que toast.js convierte en aviso al cargar.
     El mensaje NUNCA va dentro de un script: solo en un atributo, escapado con c:out. --%>
<div id="contenedorToasts" class="toast-container position-fixed top-0 end-0 p-3" aria-live="polite" aria-atomic="false"></div>
<c:if test="${not empty mensaje}">
    <c:choose>
        <c:when test="${mensajeTipo == 'success'}"><c:set var="tipoToast" value="exito"/></c:when>
        <c:when test="${mensajeTipo == 'danger'}"><c:set var="tipoToast" value="error"/></c:when>
        <c:when test="${mensajeTipo == 'warning'}"><c:set var="tipoToast" value="aviso"/></c:when>
        <c:otherwise><c:set var="tipoToast" value="info"/></c:otherwise>
    </c:choose>
    <div id="datosFlash" hidden data-tipo="<c:out value='${tipoToast}'/>" data-mensaje="<c:out value='${mensaje}'/>"></div>
</c:if>
<script src="${pageContext.request.contextPath}/assets/js/toast.js"></script>
