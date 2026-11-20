// Validación en el navegador (el servidor repite la validación).
$(function () {
    var patronUsuario = /^[A-Za-z0-9._-]{3,30}$/;

    $('#formLogin').on('submit', function (e) {
        var usuarioOk = patronUsuario.test($.trim($('#username').val()));
        var claveOk = $('#clave').val().length > 0;

        $('#username').toggleClass('is-invalid', !usuarioOk);
        $('#clave').toggleClass('is-invalid', !claveOk);

        if (!usuarioOk || !claveOk) {
            e.preventDefault();
        }
    });
});
