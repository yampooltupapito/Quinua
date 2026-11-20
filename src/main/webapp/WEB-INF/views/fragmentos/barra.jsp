<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Primero el enlace "Saltar al contenido"; luego la barra de navegación compartida (logo o ícono, usuario, rol, tema,
     menú y cerrar sesión) y el contenedor de avisos (toasts). --%>
<jsp:include page="/WEB-INF/views/fragmentos/saltar.jsp"/>
<nav class="navbar navbar-expand navbar-sigmud" aria-label="Barra principal">
    <div class="container">
        <a class="navbar-brand mb-0 h1 d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/menu">
            <c:choose>
                <c:when test="${applicationScope.logoDisponible}">
                    <img src="${pageContext.request.contextPath}/assets/img/logo.png" alt="Logo de la Municipalidad Distrital de Quinua"
                         height="30" class="logo-barra">
                </c:when>
                <c:otherwise><i class="bi bi-building" aria-hidden="true"></i></c:otherwise>
            </c:choose>
            <span>SIGMUD</span>
        </a>
        <div class="d-flex align-items-center text-white">
            <span class="me-3 small">
                <i class="bi bi-person-circle" aria-hidden="true"></i>
                <a class="text-white text-decoration-none" href="${pageContext.request.contextPath}/perfil" title="Mi perfil"><c:out value="${sessionScope.usuario.nombres}"/></a>
                <span class="badge text-bg-light"><c:out value="${sessionScope.usuario.rol}"/></span>
            </span>
            <button type="button" class="btn btn-outline-light btn-sm me-2 btn-tema" aria-label="Cambiar a tema oscuro" title="Cambiar a tema oscuro">
                <i class="bi bi-moon-stars-fill" aria-hidden="true"></i>
            </button>
            <a class="btn btn-outline-light btn-sm me-2" href="${pageContext.request.contextPath}/menu">
                <i class="bi bi-grid" aria-hidden="true"></i> Menú
            </a>
            <a class="btn btn-outline-light btn-sm" href="${pageContext.request.contextPath}/logout">
                <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Cerrar sesión
            </a>
        </div>
    </div>
</nav>
<jsp:include page="/WEB-INF/views/fragmentos/toasts.jsp"/>
