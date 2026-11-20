<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Tarjetas de acceso a los módulos que el rol puede usar (las mismas reglas que tenía el menú). --%>
<h2 class="h5 text-sigmud mb-3">Módulos</h2>
<div class="row g-3 mb-4">
    <c:if test="${verContribuyentes}">
        <div class="col-12 col-md-6 col-lg-4">
            <div class="card card-sigmud h-100 shadow-sm">
                <div class="card-body">
                    <h3 class="h5 card-title"><i class="bi bi-person-vcard"></i> Contribuyentes</h3>
                    <p class="card-text text-muted small">Registro y consulta de contribuyentes.</p>
                </div>
                <div class="card-footer bg-transparent border-0">
                    <a class="btn btn-sigmud btn-sm" href="${pageContext.request.contextPath}/contribuyentes">Abrir</a>
                </div>
            </div>
        </div>
    </c:if>
    <c:if test="${verAlmacen}">
        <div class="col-12 col-md-6 col-lg-4">
            <div class="card card-sigmud h-100 shadow-sm">
                <div class="card-body">
                    <h3 class="h5 card-title"><i class="bi bi-box-seam"></i> Almacén</h3>
                    <p class="card-text text-muted small">Materiales, entradas y salidas de stock.</p>
                </div>
                <div class="card-footer bg-transparent border-0">
                    <a class="btn btn-sigmud btn-sm" href="${pageContext.request.contextPath}/almacen">Abrir</a>
                </div>
            </div>
        </div>
    </c:if>
    <c:if test="${verCitas}">
        <div class="col-12 col-md-6 col-lg-4">
            <div class="card card-sigmud h-100 shadow-sm">
                <div class="card-body">
                    <h3 class="h5 card-title"><i class="bi bi-calendar-check"></i> Citas</h3>
                    <p class="card-text text-muted small">Programación de citas con funcionarios.</p>
                </div>
                <div class="card-footer bg-transparent border-0">
                    <a class="btn btn-sigmud btn-sm" href="${pageContext.request.contextPath}/citas">Abrir</a>
                </div>
            </div>
        </div>
    </c:if>
    <c:if test="${verUsuarios}">
        <div class="col-12 col-md-6 col-lg-4">
            <div class="card card-sigmud h-100 shadow-sm">
                <div class="card-body">
                    <h3 class="h5 card-title"><i class="bi bi-people"></i> Usuarios</h3>
                    <p class="card-text text-muted small">Altas, roles, estados y contraseñas de los usuarios.</p>
                </div>
                <div class="card-footer bg-transparent border-0">
                    <a class="btn btn-sigmud btn-sm" href="${pageContext.request.contextPath}/usuarios">Abrir</a>
                </div>
            </div>
        </div>
    </c:if>
    <c:if test="${verAuditoria}">
        <div class="col-12 col-md-6 col-lg-4">
            <div class="card card-sigmud h-100 shadow-sm">
                <div class="card-body">
                    <h3 class="h5 card-title"><i class="bi bi-journal-text"></i> Auditoría</h3>
                    <p class="card-text text-muted small">Historial de accesos y cambios (solo lectura).</p>
                </div>
                <div class="card-footer bg-transparent border-0">
                    <a class="btn btn-sigmud btn-sm" href="${pageContext.request.contextPath}/auditoria">Abrir</a>
                </div>
            </div>
        </div>
    </c:if>
</div>
