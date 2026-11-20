// Panel (dashboard) de cada rol: indicadores, gráficos con Chart.js y listas.
// Pide /dashboard/datos al cargar y cada 60 segundos, sin recargar la página.
// El rol y el usuario los decide el servidor con la sesión; este script solo dibuja lo que recibe.
$(function () {
    var $panel = $('#panel');
    if ($panel.length === 0) { return; }

    var urlDatos = $panel.data('urlDatos');
    var urlDesbloquear = $panel.data('urlDesbloquear');
    var contexto = $panel.data('contexto');
    var token = $('meta[name="csrf-token"]').attr('content');
    if (token) { $.ajaxSetup({ headers: { 'X-CSRF-Token': token } }); }

    // Paleta verde institucional (+ colores de apoyo). Cambia con el tema: en oscuro se usan tonos más claros
    // para que los gráficos se lean bien sobre fondo oscuro.
    var VERDE, VERDE_CLARO, DORADO, AZUL, GRIS, VERDE_RELLENO;
    function esOscuro() { return document.documentElement.getAttribute('data-bs-theme') === 'dark'; }
    function aplicarPaleta() {
        if (esOscuro()) {
            VERDE = '#66bb6a'; VERDE_CLARO = '#a5d6a7'; DORADO = '#e0b84a'; AZUL = '#64b5f6'; GRIS = '#9aa5a0';
            VERDE_RELLENO = 'rgba(102, 187, 106, 0.22)';
        } else {
            VERDE = '#1b5e20'; VERDE_CLARO = '#66bb6a'; DORADO = '#c9a227'; AZUL = '#1565c0'; GRIS = '#9e9e9e';
            VERDE_RELLENO = 'rgba(27, 94, 32, 0.15)';
        }
        if (typeof Chart !== 'undefined') {
            // Texto, cuadrícula y bordes de los gráficos toman los colores del tema actual
            var estilos = getComputedStyle(document.documentElement);
            Chart.defaults.color = estilos.getPropertyValue('--bs-body-color').trim() || '#212529';
            Chart.defaults.borderColor = esOscuro() ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.1)';
        }
    }
    aplicarPaleta();
    var ultimosGraficos = null;                          // datos del último refresco (para redibujar al cambiar el tema)
    var INTERVALO_MS = 60000;
    var graficos = {};                                   // id del canvas -> instancia de Chart

    if (typeof Chart !== 'undefined') {
        Chart.defaults.locale = 'es-PE';
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    }

    // ---------------------------------------------------------------- gráficos

    function suma(arreglo) {
        return arreglo.reduce(function (a, b) { return a + b; }, 0);
    }

    // Texto para lectores de pantalla: qué muestra el gráfico (se actualiza con cada refresco)
    function describir(titulo, etiquetas, datasets) {
        if (datasets.length === 1 && etiquetas.length <= 8) {
            var partes = etiquetas.map(function (e, i) { return e + ': ' + datasets[0].data[i]; });
            return titulo + '. ' + partes.join('; ') + '.';
        }
        var totales = datasets.map(function (d) { return d.label + ': total ' + suma(d.data); });
        return titulo + '. ' + totales.join('; ') + '.';
    }

    // Dibuja o actualiza un gráfico. Si no hay datos, muestra "No hay datos para mostrar".
    function mostrarGrafico(id, tipo, titulo, etiquetas, datasets, opciones) {
        var $canvas = $('#' + id);
        if ($canvas.length === 0) { return; }
        var $contenedor = $canvas.parent();
        var $vacio = $('.sin-datos[data-para="' + id + '"]');
        var hayDatos = etiquetas.length > 0 && datasets.some(function (d) { return suma(d.data) > 0; });
        var descripcion = hayDatos ? describir(titulo, etiquetas, datasets) : titulo + ': no hay datos para mostrar.';

        $canvas.attr('aria-label', descripcion);
        $('#' + $canvas.attr('id').replace(/^grafico/, 'desc')).text(descripcion);
        $contenedor.toggleClass('d-none', !hayDatos);
        $vacio.toggleClass('d-none', hayDatos);

        if (!hayDatos) {
            if (graficos[id]) { graficos[id].destroy(); delete graficos[id]; }
            return;
        }
        if (typeof Chart === 'undefined') { return; }
        if (graficos[id]) {                                // ya existe: solo se actualizan los datos
            graficos[id].data.labels = etiquetas;
            graficos[id].data.datasets = datasets;
            graficos[id].update();
            return;
        }
        graficos[id] = new Chart($canvas[0].getContext('2d'), {
            type: tipo,
            data: { labels: etiquetas, datasets: datasets },
            options: $.extend(true, {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } }
            }, opciones || {})
        });
    }

    var EJE_ENTERO = { beginAtZero: true, ticks: { precision: 0 } };

    function dibujarGraficos(g) {
        if (g.citasPorEstado) {
            mostrarGrafico('graficoCitasEstado', 'doughnut', 'Citas por estado de los últimos 30 días',
                g.citasPorEstado.etiquetas,
                [{ label: 'Citas', data: g.citasPorEstado.valores, backgroundColor: [AZUL, VERDE, GRIS] }],
                { cutout: '60%' });
        }
        if (g.movimientosPorDia) {
            mostrarGrafico('graficoMovimientos', 'bar', 'Entradas y salidas de stock por día, últimos 14 días',
                g.movimientosPorDia.etiquetas,
                [{ label: 'Entradas', data: g.movimientosPorDia.entradas, backgroundColor: VERDE },
                 { label: 'Salidas', data: g.movimientosPorDia.salidas, backgroundColor: DORADO }],
                { scales: { y: EJE_ENTERO } });
        }
        if (g.contribuyentesPorMes) {
            mostrarGrafico('graficoContribuyentes', 'line', 'Contribuyentes registrados por mes, últimos 6 meses',
                g.contribuyentesPorMes.etiquetas,
                [{ label: 'Contribuyentes registrados', data: g.contribuyentesPorMes.valores, borderColor: VERDE,
                   backgroundColor: VERDE_RELLENO, fill: true, tension: 0.3, pointBackgroundColor: VERDE }],
                { scales: { y: EJE_ENTERO } });
        }
        if (g.atendidasPorDia) {
            mostrarGrafico('graficoAtendidas', 'bar', 'Mis citas atendidas por día, últimos 7 días',
                g.atendidasPorDia.etiquetas,
                [{ label: 'Citas atendidas', data: g.atendidasPorDia.valores, backgroundColor: VERDE }],
                { plugins: { legend: { display: false } }, scales: { y: EJE_ENTERO } });
        }
        if (g.salidasPorArea) {
            mostrarGrafico('graficoSalidasArea', 'bar', 'Unidades entregadas por área este mes',
                g.salidasPorArea.etiquetas,
                [{ label: 'Unidades entregadas', data: g.salidasPorArea.valores, backgroundColor: VERDE_CLARO, borderColor: VERDE, borderWidth: 1 }],
                { indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: EJE_ENTERO } });
        }
    }

    // ---------------------------------------------------------------- indicadores

    function actualizarKpis(kpis) {
        $.each(kpis, function (nombre, valor) {
            $('[data-kpi="' + nombre + '"]').text(valor);
            // Los indicadores de alerta se ponen en rojo cuando son mayores que 0
            $('[data-kpi-tarjeta="' + nombre + '"][data-alerta="true"]').toggleClass('kpi-alerta', valor > 0);
        });
    }

    // ---------------------------------------------------------------- listas (se arman con .text(): nunca con HTML)

    function celda(texto, clase) {
        return $('<td>').addClass(clase || '').text(texto);
    }

    function filaVacia(columnas, mensaje) {
        return $('<tr>').append($('<td>').attr('colspan', columnas).addClass('text-center text-muted').text(mensaje));
    }

    function reemplazar(idTbody, filas, columnas, mensajeVacio) {
        var $tbody = $('#' + idTbody);
        if ($tbody.length === 0) { return; }
        $tbody.empty();
        if (filas.length === 0) { $tbody.append(filaVacia(columnas, mensajeVacio)); } else { $tbody.append(filas); }
    }

    function boton(clase, icono, texto) {
        return $('<button>').attr('type', 'button').addClass('btn btn-sm ' + clase)
            .append($('<i>').addClass('bi ' + icono)).append(document.createTextNode(' ' + texto));
    }

    function listaBloqueadas(lista) {
        var filas = lista.map(function (f) {
            return $('<tr>').append(celda(f.username), celda(f.nombres), celda(f.intentos, 'text-end'),
                $('<td>').addClass('text-end').append(
                    boton('btn-outline-danger btn-desbloquear', 'bi-unlock', 'Desbloquear')
                        .attr('data-id', f.id).attr('data-usuario', f.username)));
        });
        reemplazar('listaBloqueadas', filas, 4, 'No hay cuentas bloqueadas.');
    }

    function listaAccesos(lista) {
        var filas = lista.map(function (f) {
            return $('<tr>').append(celda(f.nombres + ' (' + f.username + ')'), celda(f.rol), celda(f.fecha));
        });
        reemplazar('listaAccesos', filas, 3, 'No hay accesos registrados.');
    }

    // ADMIN: actividad reciente (texto siempre con .text(), nunca html)
    function listaActividad(lista) {
        var $ul = $('#listaActividad').empty();
        if (!lista.length) {
            $('<li>').addClass('list-group-item px-0 text-muted').text('Todavía no hay actividad registrada.').appendTo($ul);
            return;
        }
        lista.forEach(function (f) {
            var $li = $('<li>').addClass('list-group-item px-0');
            $li.append($('<span>').addClass('badge ' + String(f.clase).replace(/[^a-z0-9 -]/gi, '')).text(f.accion));
            $li.append(' ').append($('<span>').addClass('text-muted small').text(f.fecha + ' · ' + f.usuario));
            $li.append($('<div>').addClass('small').text(f.descripcion));
            $ul.append($li);
        });
    }

    // Botón "Atender": un formulario POST a /citas (acción que ya existe), con el token CSRF
    function formularioAtender(f) {
        var $form = $('<form>').attr({ method: 'post', action: contexto + '/citas' }).addClass('d-inline');
        $.each({ accion: 'atender', csrf: token, id: f.id, fecha: f.fecha, area: f.idArea }, function (nombre, valor) {
            $form.append($('<input>').attr({ type: 'hidden', name: nombre }).val(valor));
        });
        return $form.append($('<button>').attr('type', 'submit').addClass('btn btn-outline-success btn-sm')
            .append($('<i>').addClass('bi bi-check2-circle')).append(document.createTextNode(' Atender')));
    }

    function listaProximas(lista) {
        var filas = lista.map(function (f) {
            var $contribuyente = $('<td>').text(f.contribuyente)
                .append($('<div>').addClass('text-muted small').text(f.dniRuc));
            return $('<tr>').append(
                $('<td>').addClass('text-nowrap').append($('<strong>').text(f.hora)),
                $contribuyente, celda(f.motivo), celda(f.ventanilla, 'text-center'),
                $('<td>').addClass('text-end').append(formularioAtender(f)));
        });
        reemplazar('listaProximas', filas, 5, 'No tiene más citas programadas para hoy.');
    }

    function listaReponer(lista) {
        var filas = lista.map(function (f) {
            var url = contexto + '/almacen?q=' + encodeURIComponent(f.codigo);
            var $enlace = $('<a>').attr('href', url).addClass('btn btn-outline-success btn-sm')
                .append($('<i>').addClass('bi bi-box-arrow-in-down')).append(document.createTextNode(' Registrar entrada'));
            return $('<tr>').append(
                $('<td>').append($('<strong>').text(f.codigo), document.createTextNode(' ' + f.nombre)),
                celda(f.stock, 'text-end'), celda(f.minimo, 'text-end'),
                celda(f.faltante, 'text-end text-danger fw-bold'),
                $('<td>').addClass('text-end text-nowrap').append($enlace));
        });
        reemplazar('listaReponer', filas, 5, 'Ningún material necesita reposición.');
    }

    function listaMovimientos(lista) {
        var filas = lista.map(function (f) {
            var entrada = f.tipo === 'ENTRADA';
            var $tipo = $('<span>').addClass('badge ' + (entrada ? 'text-bg-success' : 'text-bg-danger')).text(f.tipo);
            return $('<tr>').append(celda(f.fecha, 'text-nowrap'), $('<td>').append($tipo), celda(f.material),
                celda(f.cantidad, 'text-end'), celda(f.area), celda(f.usuario));
        });
        reemplazar('listaMovimientos', filas, 6, 'Aún no hay movimientos.');
    }

    function dibujarListas(l) {
        if (l.bloqueadas) { listaBloqueadas(l.bloqueadas); }
        if (l.accesos) { listaAccesos(l.accesos); }
        if (l.actividad) { listaActividad(l.actividad); }
        if (l.proximas) { listaProximas(l.proximas); }
        if (l.reponer) { listaReponer(l.reponer); }
        if (l.movimientos) { listaMovimientos(l.movimientos); }
    }

    // ---------------------------------------------------------------- carga y actualización automática

    function aviso(texto) {
        $('#avisoActualizacion').text(texto);
    }

    function cargar() {
        $.ajax({ url: urlDatos, method: 'GET', dataType: 'json', cache: false })
            .done(function (r) {
                if (!r || !r.ok) { aviso('No se pudo actualizar. Se muestran los últimos datos.'); return; }
                actualizarKpis(r.kpis || {});
                ultimosGraficos = r.graficos || {};
                dibujarGraficos(ultimosGraficos);
                dibujarListas(r.listas || {});
                $('#ultimaActualizacion').text(r.actualizado);
                aviso('');
            })
            .fail(function (xhr) {
                // Se conservan los datos anteriores; solo se deja un aviso discreto
                aviso(xhr.status === 401
                    ? '· La sesión expiró: recargue la página e inicie sesión.'
                    : '· No se pudo actualizar; se muestran los últimos datos.');
            });
    }

    // Al cambiar el tema (claro/oscuro) los gráficos se vuelven a dibujar con los colores nuevos, sin recargar
    document.addEventListener('sigmud:tema', function () {
        aplicarPaleta();
        if (ultimosGraficos) {
            Object.keys(graficos).forEach(function (id) { graficos[id].destroy(); delete graficos[id]; });
            dibujarGraficos(ultimosGraficos);
        }
    });

    cargar();
    setInterval(function () { if (!document.hidden) { cargar(); } }, INTERVALO_MS);
    $(document).on('visibilitychange', function () { if (!document.hidden) { cargar(); } });

    // ---------------------------------------------------------------- ADMIN: desbloquear una cuenta

    $(document).on('click', '.btn-desbloquear', function () {
        var $boton = $(this);
        var usuario = $boton.data('usuario');
        if (!window.confirm('¿Desbloquear la cuenta de ' + usuario + '?')) { return; }
        $boton.prop('disabled', true);
        $.ajax({ url: urlDesbloquear, method: 'POST', dataType: 'json', data: { id: $boton.data('id') } })
            .done(function (r) {
                mostrarToast(r.ok ? 'exito' : 'error', r.mensaje);
                cargar();                                   // actualiza la lista y el indicador
            })
            .fail(function () {
                mostrarToast('error', 'No se pudo completar la operación.');
            })
            .always(function () { $boton.prop('disabled', false); });
    });
});
