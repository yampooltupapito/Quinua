// Cambio de contraseña: medidor de fortaleza y validación previa. El servidor vuelve a validar siempre.
$(function () {
    var $form = $('#formPassword');
    if ($form.length === 0) { return; }
    var usuario = String($form.data('usuario') || '').toLowerCase();

    // Los 5 requisitos de la política
    function requisitos(p) {
        return {
            largo: p.length >= 8,
            mayuscula: /[A-ZÁÉÍÓÚÑ]/.test(p),
            minuscula: /[a-záéíóúñ]/.test(p),
            numero: /\d/.test(p),
            usuario: p.length > 0 && p.toLowerCase() !== usuario
        };
    }
    function cumpleTodo(p) {
        var r = requisitos(p);
        return r.largo && r.mayuscula && r.minuscula && r.numero && r.usuario;
    }

    // Fortaleza: puntos por requisito cumplido (5), largo de 12 o más y símbolo (máximo 7)
    function fortaleza(p) {
        if (p.length === 0) { return { texto: '-', clase: 'bg-secondary', ancho: 0 }; }
        var puntos = 0;
        $.each(requisitos(p), function (k, ok) { if (ok) { puntos++; } });
        if (p.length >= 12) { puntos++; }
        if (/[^A-Za-z0-9]/.test(p)) { puntos++; }
        if (!cumpleTodo(p)) { puntos = Math.min(puntos, 3); }   // sin cumplir la política nunca pasa de "Débil"
        var niveles = [
            { texto: 'Muy débil', clase: 'bg-danger' }, { texto: 'Muy débil', clase: 'bg-danger' },
            { texto: 'Débil', clase: 'bg-danger' }, { texto: 'Débil', clase: 'bg-warning' },
            { texto: 'Aceptable', clase: 'bg-warning' }, { texto: 'Buena', clase: 'bg-success' },
            { texto: 'Fuerte', clase: 'bg-success' }, { texto: 'Muy fuerte', clase: 'bg-success' }
        ];
        var n = niveles[Math.min(puntos, niveles.length - 1)];
        return { texto: n.texto, clase: n.clase, ancho: Math.round(puntos * 100 / 7) };
    }

    function comprobarConfirmacion() {
        var coincide = $('#nueva').val() === $('#confirmacion').val();
        $('#confirmacion').toggleClass('is-invalid', !coincide)
            .toggleClass('is-valid', coincide && $('#confirmacion').val() !== '');
        return coincide;
    }

    function actualizar() {
        var p = $('#nueva').val(), r = requisitos(p), f = fortaleza(p);
        $('#barraFortaleza').removeClass('bg-danger bg-warning bg-success bg-secondary').addClass(f.clase).css('width', f.ancho + '%');
        $('#textoFortaleza').text(f.texto);
        $('#requisitos li').each(function () {
            var ok = r[$(this).data('req')];
            $(this).toggleClass('text-success', ok).find('i').attr('class', ok ? 'bi bi-check-circle-fill' : 'bi bi-circle');
        });
        // Si el campo estaba marcado en rojo y ya cumple, se quita la marca
        if ($('#nueva').hasClass('is-invalid') && cumpleTodo(p) && p !== $('#actual').val()) { $('#nueva').removeClass('is-invalid'); }
        if ($('#confirmacion').val() !== '') { comprobarConfirmacion(); }
    }
    $('#actual').on('input', function () {
        if ($(this).val() !== '') { $(this).removeClass('is-invalid'); }
    });

    $('#nueva').on('input', actualizar);
    $('#confirmacion').on('input', comprobarConfirmacion);
    actualizar();

    $form.on('submit', function (e) {
        var nueva = $('#nueva').val(), actual = $('#actual').val();
        var okActual = actual !== '';
        var okNueva = cumpleTodo(nueva) && nueva !== actual;
        var okConf = comprobarConfirmacion();
        $('#actual').toggleClass('is-invalid', !okActual);
        $('#nueva').toggleClass('is-invalid', !okNueva);
        if (!okActual || !okNueva || !okConf) {
            e.preventDefault();
            $('.is-invalid').first().focus();
        }
    });
});
