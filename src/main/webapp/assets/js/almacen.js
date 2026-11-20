// Validaciones en el navegador para el Almacén (materiales).
// El servidor vuelve a validar siempre; esto solo mejora la experiencia.
$(function () {

    // ---------- Listado: modal de confirmación de baja ----------
    $('#modalBaja').on('show.bs.modal', function (e) {
        var boton = $(e.relatedTarget);
        $('#bajaId').val(boton.data('id'));
        $('#bajaNombre').text(boton.data('nombre'));
    });

    // ---------- Formulario ----------
    var $form = $('#formMaterial');
    if ($form.length === 0) { return; }

    var PATRON_CODIGO = /^[A-Z]{3}-\d{3}$/;
    var PATRON_ENTERO = /^\d+$/;

    // Código en mayúsculas mientras se escribe.
    $('#codigo').on('input', function () {
        $(this).val($(this).val().toUpperCase());
    });

    // Solo números en el stock mínimo.
    $('#stockMinimo').on('input', function () {
        $(this).val($(this).val().replace(/\D/g, ''));
    });

    $form.on('submit', function (e) {
        var nombre = $.trim($('#nombre').val());

        var reglas = [
            ['#codigo', PATRON_CODIGO.test($('#codigo').val())],
            ['#nombre', nombre.length >= 3 && nombre.length <= 100],
            ['#unidad', $('#unidad').val() !== null && $('#unidad').val() !== ''],
            ['#stockMinimo', PATRON_ENTERO.test($('#stockMinimo').val())]
        ];

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
