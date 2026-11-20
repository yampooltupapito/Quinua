<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ct" value="${cita}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SIGMUD - <c:out value="${esNuevo ? 'Nueva cita' : 'Reprogramar cita'}"/></title>
    <jsp:include page="/WEB-INF/views/fragmentos/meta-csrf.jsp"/>
    <jsp:include page="/WEB-INF/views/fragmentos/cabecera.jsp"/>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragmentos/barra.jsp"/>

<main id="contenido" tabindex="-1" class="container py-4" style="max-width: 820px;">
    <h1 class="h4 text-sigmud mb-3">
        <c:choose>
            <c:when test="${esNuevo}"><i class="bi bi-calendar-plus"></i> Nueva cita</c:when>
            <c:otherwise><i class="bi bi-calendar2-event"></i> Reprogramar cita</c:otherwise>
        </c:choose>
    </h1>

    <c:if test="${not empty errores}">
        <div class="alert alert-danger" role="alert">
            <strong>Revise los datos:</strong>
            <ul class="mb-0">
                <c:forEach var="e" items="${errores}"><li><c:out value="${e}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <%-- data-*: dirección del Ajax, cita que se reprograma (para no marcar su turno como ocupado) y hora elegida --%>
    <form id="formCita" class="card card-sigmud shadow-sm" method="post"
          action="${pageContext.request.contextPath}/citas" novalidate
          data-url-ajax="${pageContext.request.contextPath}/citas/ajax"
          data-excluir="${esNuevo ? '' : ct.idCita}"
          data-nuevo="${esNuevo}">
        <div class="card-body row g-3">
            <input type="hidden" name="accion" value="guardar">
            <jsp:include page="/WEB-INF/views/fragmentos/csrf.jsp"/>
            <input type="hidden" name="id" value="${esNuevo ? '' : ct.idCita}">
            <input type="hidden" name="hora" id="hora" value="<c:out value='${ct.hora}'/>">

            <%-- 1. Contribuyente --%>
            <div class="col-12 position-relative">
                <label for="buscarContribuyente" class="form-label">1. Contribuyente</label>
                <input type="hidden" name="idContribuyente" id="idContribuyente"
                       value="${ct.idContribuyente > 0 ? ct.idContribuyente : ''}">
                <c:choose>
                    <c:when test="${esNuevo and not contribuyenteFijo}">
                        <input type="text" class="form-control" id="buscarContribuyente" autocomplete="off"
                               maxlength="100" placeholder="Escriba al menos 3 letras del nombre o el DNI/RUC"
                               value="<c:out value='${ct.dniRuc}${empty ct.dniRuc ? "" : " - "}${ct.nombreContribuyente}'/>">
                        <div class="invalid-feedback">Busque y seleccione un contribuyente de la lista.</div>
                        <div id="listaContribuyentes" class="list-group position-absolute w-100 shadow d-none" style="z-index: 1050;"></div>
                        <div id="ayudaContribuyente" class="form-text">Escriba al menos 3 caracteres.</div>
                    </c:when>
                    <c:otherwise>
                        <input type="text" class="form-control" id="buscarContribuyente" readonly
                               value="<c:out value='${ct.dniRuc} - ${ct.nombreContribuyente}'/>">
                        <c:if test="${contribuyenteFijo}">
                            <div class="form-text"><i class="bi bi-lock"></i> Contribuyente elegido desde su ficha.
                                <a href="${pageContext.request.contextPath}/citas?accion=nuevo">Elegir otro</a></div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- 2. Área y funcionario --%>
            <div class="col-md-6">
                <label for="idArea" class="form-label">2. Área</label>
                <select id="idArea" class="form-select" ${esNuevo ? 'name="idArea"' : 'disabled'}>
                    <option value="">Seleccione...</option>
                    <c:forEach var="a" items="${areas}">
                        <option value="${a.idArea}" ${ct.idArea == a.idArea ? 'selected' : ''}><c:out value="${a.nombre}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not esNuevo}"><input type="hidden" name="idArea" value="${ct.idArea}"></c:if>
                <div class="invalid-feedback">Seleccione el área.</div>
            </div>
            <div class="col-md-6">
                <label for="idFuncionario" class="form-label">Funcionario</label>
                <select id="idFuncionario" class="form-select" ${esNuevo ? 'name="idFuncionario"' : 'disabled'}>
                    <option value="">Seleccione...</option>
                    <c:forEach var="u" items="${funcionarios}">
                        <option value="${u.idUsuario}" ${ct.idFuncionario == u.idUsuario ? 'selected' : ''}><c:out value="${u.nombres}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not esNuevo}"><input type="hidden" name="idFuncionario" value="${ct.idFuncionario}"></c:if>
                <div class="invalid-feedback">Seleccione el funcionario.</div>
            </div>

            <%-- 3. Fecha y turnos --%>
            <div class="col-md-4">
                <label for="fecha" class="form-label">3. Fecha</label>
                <input type="date" class="form-control" id="fecha" name="fecha" value="<c:out value='${ct.fecha}'/>">
                <div class="invalid-feedback" id="errorFecha">Elija una fecha de lunes a viernes.</div>
            </div>
            <div class="col-12">
                <label class="form-label">Turno (de 08:00 a 16:30, cada 30 minutos)</label>
                <div id="turnos" class="d-flex flex-wrap gap-2">
                    <span class="text-body-secondary small">Elija funcionario y fecha para ver los turnos.</span>
                </div>
                <div id="errorHora" class="text-danger small mt-1 d-none">Seleccione un turno libre.</div>
            </div>

            <%-- 4. Ventanilla y motivo --%>
            <div class="col-md-4">
                <label for="ventanilla" class="form-label">4. Ventanilla</label>
                <select id="ventanilla" name="ventanilla" class="form-select">
                    <option value="">Seleccione...</option>
                    <c:forEach var="n" begin="1" end="10">
                        <option value="${n}" ${ct.ventanilla == n ? 'selected' : ''}>${n}</option>
                    </c:forEach>
                </select>
                <div class="invalid-feedback">Seleccione una ventanilla del 1 al 10.</div>
            </div>
            <div class="col-12">
                <label for="motivo" class="form-label">Motivo</label>
                <textarea id="motivo" class="form-control" rows="2" maxlength="200"
                          ${esNuevo ? 'name="motivo"' : 'readonly'}><c:out value="${ct.motivo}"/></textarea>
                <c:if test="${not esNuevo}"><input type="hidden" name="motivo" value="<c:out value='${ct.motivo}'/>"></c:if>
                <div class="invalid-feedback">Obligatorio, máximo 200 caracteres.</div>
            </div>
        </div>
        <div class="card-footer bg-transparent d-flex justify-content-end gap-2">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/citas">Cancelar</a>
            <button type="submit" class="btn btn-sigmud">
                <i class="bi bi-check-lg"></i> <c:out value="${esNuevo ? 'Registrar cita' : 'Guardar cambios'}"/>
            </button>
        </div>
    </form>
</main>

<script src="${pageContext.request.contextPath}/assets/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/citas.js"></script>
</body>
</html>
