// Auditoría (solo lectura): modal de detalle y validación de fechas. Todo texto se inserta con .text() (nunca html).
$(function () {
    var $modal = $('#modalDetalle');

    // Rellena el modal con los data-* de la fila pulsada
    $modal.on('show.bs.modal', function (e) {
        var d = $(e.relatedTarget).data();
        $('#detFecha').text(d.fecha || '');
        $('#detUsuario').text(d.usuario || '-');
        $('#detAccion').text(d.accion || '');
        $('#detEntidad').text((d.entidad || '') + (d.ident !== undefined && d.ident !== '' ? ' #' + d.ident : ''));
        $('#detDescripcion').text(d.descripcion || '');
        $('#detIp').text(d.ip || '');
        var $lista = $('#detDetalle').empty();
        var detalle = String(d.detalle || '').trim();
        if (!detalle) {
            $('<li class="list-group-item text-muted"></li>').text('Este registro no tiene detalle adicional.').appendTo($lista);
            return;
        }
        $.each(detalle.split('; '), function (i, parte) {
            $('<li class="list-group-item"></li>').text(parte).appendTo($lista);
        });
    });

    // Enter o espacio sobre una fila abre el detalle (accesibilidad con teclado)
    $('#tablaAuditoria').on('keydown', '.fila-auditoria', function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            $(this).trigger('click');
        }
    });

    // Validación de fechas antes de enviar (el servidor vuelve a validar)
    $('#formAuditoria').on('submit', function (e) {
        var desde = $('#desde').val(), hasta = $('#hasta').val(), $err = $('#errorFechas');
        var msg = '';
        if (desde && hasta) {
            var dias = (new Date(hasta) - new Date(desde)) / 86400000 + 1;
            if (dias < 1) { msg = 'La fecha "desde" no puede ser mayor que la fecha "hasta".'; }
            else if (dias > 366) { msg = 'El periodo no puede pasar de 366 días.'; }
        }
        if (msg) {
            e.preventDefault();
            $err.text(msg).removeClass('d-none');
        } else {
            $err.addClass('d-none');
        }
    });
});
