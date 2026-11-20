// Entradas y salidas de stock con Ajax (JSON). El servidor valida y decide siempre.
$(function () {
    // Token CSRF: se envía en la cabecera X-CSRF-Token de toda llamada Ajax
    var tokenCsrf = $('meta[name="csrf-token"]').attr('content');
    if (tokenCsrf) { $.ajaxSetup({ headers: { 'X-CSRF-Token': tokenCsrf } }); }

    var $form = $('#formMovimiento');
    if ($form.length === 0) { return; }

    var $modal = $('#modalMovimiento');
    var $boton = $('#movConfirmar');
    var $cargando = $('#movCargando');
    var PATRON_ENTERO = /^\d+$/;
    var MAX_CANTIDAD = 100000;

    // El área solo se pide en las salidas.
    function aplicarTipo() {
        var salida = $('#movTipo').val() === 'SALIDA';
        $('#grupoArea').toggleClass('d-none', !salida);
        if (!salida) { $('#movArea').val('').removeClass('is-invalid'); }
    }
    $('#movTipo').on('change', aplicarTipo);

    $('#movCantidad').on('input', function () {
        $(this).val($(this).val().replace(/\D/g, ''));
    });

    // Al abrir el modal desde la fila: carga el material y el tipo elegido.
    $modal.on('show.bs.modal', function (e) {
        var origen = $(e.relatedTarget);
        $form[0].reset();
        $form.find('.is-invalid').removeClass('is-invalid');
        $('#movIdMaterial').val(origen.data('id'));
        $('#movMaterial').val(origen.data('nombre'));
        $('#movTipo').val(origen.data('tipo'));
        aplicarTipo();
    });

    $modal.on('shown.bs.modal', function () { $('#movCantidad').trigger('focus'); });

    function validarFormulario() {
        var cantidad = $('#movCantidad').val();
        var cantidadOk = PATRON_ENTERO.test(cantidad) && +cantidad >= 1 && +cantidad <= MAX_CANTIDAD;
        var areaOk = $('#movTipo').val() !== 'SALIDA' || $('#movArea').val() !== '';
        var obsOk = $('#movObservacion').val().length <= 200;
        $('#movCantidad').toggleClass('is-invalid', !cantidadOk);
        $('#movArea').toggleClass('is-invalid', !areaOk);
        $('#movObservacion').toggleClass('is-invalid', !obsOk);
        return cantidadOk && areaOk && obsOk;
    }

    // Los resultados del servidor se muestran como avisos (toasts); los errores de validación quedan junto a los campos
    function mostrarError(texto) {
        mostrarToast('error', texto);
    }

    function ocupado(si) {
        $boton.prop('disabled', si);
        $cargando.toggleClass('d-none', !si);
    }

    // Actualiza stock e insignia en la misma fila, sin recargar la página.
    function actualizarFila(idMaterial, r) {
        var $fila = $('#fila-' + idMaterial);
        $fila.find('.celda-stock').text(r.stockActual);
        var $insignia = $('<span>').addClass('badge')
            .addClass(r.critico ? 'text-bg-danger' : 'text-bg-success')
            .text(r.critico ? 'Crítico' : 'Normal');
        $fila.find('.celda-estado').empty().append($insignia);
    }

    function mostrarAviso(texto) {
        mostrarToast('exito', texto);
    }

    $form.on('submit', function (e) {
        e.preventDefault();
        if (!validarFormulario()) { return; }

        var idMaterial = $('#movIdMaterial').val();
        ocupado(true);
        $.ajax({
            url: $form.attr('action'),
            method: 'POST',
            dataType: 'json',
            data: $form.serialize()
        }).done(function (r) {
            if (r.ok) {
                actualizarFila(idMaterial, r);
                bootstrap.Modal.getOrCreateInstance($modal[0]).hide();
                mostrarAviso(r.mensaje);
            } else {
                mostrarError(r.mensaje);
            }
        }).fail(function () {
            mostrarError('No se pudo completar la operación.');
        }).always(function () {
            ocupado(false);
        });
    });
});
