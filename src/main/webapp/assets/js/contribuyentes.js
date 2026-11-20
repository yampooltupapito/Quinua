// Validaciones en el navegador para Contribuyentes.
// El servidor vuelve a validar siempre; esto solo mejora la experiencia.
$(function () {

    // ---------- Listado: modal de confirmación de baja ----------
    $('#modalBaja').on('show.bs.modal', function (e) {
        var boton = $(e.relatedTarget);
        $('#bajaId').val(boton.data('id'));
        $('#bajaNombre').text(boton.data('nombre'));
    });

    // Reactivar (listado y ficha): el modal recibe el id y el nombre del botón pulsado
    $('#modalReactivar').on('show.bs.modal', function (e) {
        var boton = $(e.relatedTarget);
        $('#reactivarId').val(boton.data('id'));
        $('#reactivarNombre').text(boton.data('nombre'));
    });

    // Interruptor "Ver también dados de baja": vuelve a consultar al cambiarlo
    $('#verInactivos').on('change', function () { $(this).closest('form').trigger('submit'); });

    // Ficha: imprimir. Al imprimir se escribe la fecha y hora reales de ese momento en el encabezado de impresión.
    $('#btnImprimir').on('click', function () { window.print(); });
    window.addEventListener('beforeprint', function () {
        $('#impresionFecha').text(new Date().toLocaleString('es-PE'));
    });

    // ---------- Formulario ----------
    var $form = $('#formContribuyente');
    if ($form.length === 0) { return; }

    var PATRON_DNI = /^\d{8}$/;
    var PATRON_RUC = /^(10|20)\d{9}$/;
    var PATRON_TELEFONO = /^9\d{8}$/;
    var PATRON_EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    function esNatural() {
        return $('#tipoPersona').val() === 'NATURAL';
    }

    // Al cambiar el tipo: etiqueta y longitud del documento, y fecha de nacimiento.
    function aplicarTipo() {
        var natural = esNatural();
        var $doc = $('#dniRuc');
        $('#etiquetaDniRuc').text(natural ? 'DNI' : 'RUC');
        $('#etiquetaNombre').text(natural ? 'Nombres y apellidos' : 'Razón social');
        var max = natural ? 8 : 11;
        $doc.attr('maxlength', max).val($doc.val().substring(0, max));
        $('#grupoFecha').toggleClass('d-none', !natural);
        if (!natural) { $('#fechaNacimiento').val('').removeClass('is-invalid'); }
    }

    // Solo números en DNI/RUC y teléfono.
    $('#dniRuc, #telefono').on('input', function () {
        $(this).val($(this).val().replace(/\D/g, ''));
    });

    $('#tipoPersona').on('change', aplicarTipo);
    aplicarTipo();

    function fechaValida(valor) {
        if (valor === '') { return true; }
        var d = new Date();
        var hoy = d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2);
        return valor >= '1900-01-01' && valor <= hoy;
    }

    $form.on('submit', function (e) {
        var natural = esNatural();
        var nombre = $.trim($('#nombresRazonSocial').val());
        var email = $.trim($('#email').val());
        var telefono = $('#telefono').val();

        var reglas = [
            ['#dniRuc', natural ? PATRON_DNI.test($('#dniRuc').val()) : PATRON_RUC.test($('#dniRuc').val())],
            ['#nombresRazonSocial', nombre.length >= 3 && nombre.length <= 150],
            ['#telefono', telefono === '' || PATRON_TELEFONO.test(telefono)],
            ['#email', email === '' || PATRON_EMAIL.test(email)],
            ['#direccion', $('#direccion').val().length <= 200],
            ['#fechaNacimiento', !natural || fechaValida($('#fechaNacimiento').val())]
        ];

        $('#errorDniRuc').text(natural
            ? 'El DNI debe tener exactamente 8 dígitos.'
            : 'El RUC debe tener 11 dígitos y empezar con 10 o 20.');

        var hayError = false;
        $.each(reglas, function (i, regla) {
            $(regla[0]).toggleClass('is-invalid', !regla[1]);
            if (!regla[1]) { hayError = true; }
        });

        if (hayError) {
            e.preventDefault();
            $('.is-invalid').first().focus();
        }
    });
});
