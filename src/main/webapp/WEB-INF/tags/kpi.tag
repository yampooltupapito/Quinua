<%@ tag language="java" pageEncoding="UTF-8" description="Tarjeta de indicador (KPI): ícono, número grande y etiqueta" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="nombre" required="true" description="Clave del indicador (la usa dashboard.js para actualizarlo)" %>
<%@ attribute name="icono" required="true" description="Clase de Bootstrap Icons, por ejemplo bi-people" %>
<%@ attribute name="etiqueta" required="true" %>
<%@ attribute name="valor" required="false" type="java.lang.Object" %>
<%@ attribute name="alerta" required="false" type="java.lang.Boolean" description="true = se pone en rojo cuando el valor es mayor que 0" %>
<c:set var="esAlerta" value="${alerta == true and not empty valor and valor > 0}"/>
<div class="col-6 col-md-4 col-xl">
    <div class="card kpi-card h-100 shadow-sm ${esAlerta ? 'kpi-alerta' : ''}" data-kpi-tarjeta="${nombre}" data-alerta="${alerta == true}">
        <div class="card-body d-flex align-items-center gap-3">
            <i class="bi ${icono} kpi-icono" aria-hidden="true"></i>
            <div>
                <div class="kpi-numero" data-kpi="${nombre}"><c:out value="${empty valor ? 0 : valor}"/></div>
                <div class="kpi-etiqueta"><c:out value="${etiqueta}"/></div>
            </div>
        </div>
    </div>
</div>
