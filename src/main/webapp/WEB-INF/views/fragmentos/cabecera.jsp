<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Cabecera común de TODAS las páginas (va dentro de <head>, después de <title>): color del navegador, favicon,
     tema claro/oscuro (tema.js síncrono, para que no parpadee) y las hojas de estilo. Todo local: sin CDN ni fuentes externas.
     Favicon: si hay logo y existe assets/img/favicon.png se usa ese; si no, assets/img/favicon.svg (letras SIGMUD). --%>
<meta name="theme-color" content="#1b5e20" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#12351a" media="(prefers-color-scheme: dark)">
<c:choose>
    <c:when test="${applicationScope.faviconPng}">
        <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/img/favicon.png">
    </c:when>
    <c:otherwise>
        <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/favicon.svg">
    </c:otherwise>
</c:choose>
<script src="${pageContext.request.contextPath}/assets/js/tema.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap-icons.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sigmud.css">
