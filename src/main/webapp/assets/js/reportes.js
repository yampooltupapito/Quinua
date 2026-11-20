// Reportes del almacén: filtros por reporte, validación de fechas, exportar CSV e imprimir.
// El servidor vuelve a validar siempre; esto solo mejora la experiencia.
$(function () {
    var $form = $('#formReportes');
    if ($form.length === 0) { return; }

    var MAX_DIAS = 366;
    var PATRON_FECHA = /^\d{4}-\d{2}-\d{2}$/;

    function reporteElegido() {
        return $form.find('input[name="reporte"]:checked').val();
    }

    // Muestra solo los filtros que aplican al reporte elegido (los ocultos no se envían).
    function aplicarReporte() {
        var rep = reporteElegido();
        $('.filtro').each(function () {
            var aplica = (' ' + $(this).data('reportes') + ' ').indexOf(' ' + rep + ' ') >= 0;
            $(this).toggleClass('d-none', !aplica);
            $(this).find('input, select').prop('disabled', !aplica);
        });
        // Opciones de "Ordenar por": solo las del reporte elegido.
        var $orden = $('#orden');
        $orden.find('option[data-reporte]').each(function () {
            var propia = $(this).data('reporte') === rep;
            $(this).prop('hidden', !propia).prop('disabled', !propia);
        });
        if ($orden.find('option:selected').is(':disabled')) { $orden.val(''); }
        $('#errorFechas').addClass('d-none');
    }

    // Al elegir otro reporte se ajustan los filtros y se consulta enseguida
    // (así la tabla cambia al instante; sin JavaScript queda el botón "Consultar").
    $form.find('input[name="reporte"]').on('change', function () {
        aplicarReporte();
        $form.trigger('submit');
    });
    aplicarReporte();

    function fechaDe(texto) {
        return PATRON_FECHA.test(texto) ? new Date(texto + 'T00:00:00') : null;
    }

    // Valida las fechas visibles antes de enviar.
    $form.on('submit', function (e) {
        if (reporteElegido() === 'stock') { return; }
        var desde = fechaDe($('#desde').val());
        var hasta = fechaDe($('#hasta').val());
        var mensaje = '';
        if (!desde || !hasta) {
            mensaje = 'Ingrese las fechas "desde" y "hasta".';
        } else if (desde > hasta) {
            mensaje = 'La fecha "desde" no puede ser mayor que la fecha "hasta".';
        } else if (Math.round((hasta - desde) / 86400000) + 1 > MAX_DIAS) {
            mensaje = 'El periodo no puede pasar de ' + MAX_DIAS + ' días.';
        }
        $('#desde, #hasta').toggleClass('is-invalid', mensaje !== '');
        $('#errorFechas').text(mensaje).toggleClass('d-none', mensaje === '');
        if (mensaje !== '') { e.preventDefault(); }
    });

    // Exportar CSV deshabilitado si no hay resultados.
    var sinDatos = $('#tablaReporte tbody tr.fila-dato').length === 0;
    $('#btnCsv').toggleClass('disabled', sinDatos).attr('aria-disabled', sinDatos ? 'true' : 'false');
    $('#btnCsv').on('click', function (e) {
        if ($(this).hasClass('disabled')) { e.preventDefault(); }
    });

    $('#btnImprimir').on('click', function () { window.print(); });
});
