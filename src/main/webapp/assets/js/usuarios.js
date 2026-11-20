// Gestión de usuarios (solo ADMIN): modales, restablecer contraseña y desbloquear con Ajax, y validación del formulario.
// El servidor vuelve a validar siempre; esto solo mejora la experiencia.
$(function () {
    var token = $('meta[name="csrf-token"]').attr('content');
    if (token) { $.ajaxSetup({ headers: { 'X-CSRF-Token': token } }); }

    // ---------- Copiar al portapapeles (botones "Copiar") ----------
    function copiar(idCampo, $boton) {
        var campo = document.getElementById(idCampo);
        var avisar = function () {
            var original = $boton.html();
            $boton.html('<i class="bi bi-check2"></i> Copiado');
            setTimeout(function () { $boton.html(original); }, 1800);
        };
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(campo.value).then(avisar, function () { copiaAlternativa(campo, avisar); });
        } else {
            copiaAlternativa(campo, avisar);
        }
    }
    function copiaAlternativa(campo, avisar) {
        campo.focus();
        campo.select();
        try { if (document.execCommand('copy')) { avisar(); } } catch (e) { /* el usuario puede copiar a mano */ }
    }
    $(document).on('click', '.btn-copiar', function () { copiar($(this).data('origen'), $(this)); });

    // ---------- Listado ----------
    var $pagina = $('#paginaUsuarios');
    if ($pagina.length) {
        // Resultado de las llamadas Ajax: aviso (toast) de éxito o de error
        var mostrarMensaje = function (ok, texto) {
            mostrarToast(ok ? 'exito' : 'error', texto);
        };

        // Activar / desactivar: modal de confirmación (POST)
        $('#modalEstado').on('show.bs.modal', function (e) {
            var origen = $(e.relatedTarget);
            var activar = String(origen.data('activo')) === '1';
            $('#estadoId').val(origen.data('id'));
            $('#estadoActivo').val(activar ? 1 : 0);
            $('#estadoTexto').text(activar ? '¿Activar al usuario' : '¿Desactivar al usuario');
            $('#estadoUsuario').text(origen.data('usuario'));
            $('#tituloEstado').text(activar ? 'Activar usuario' : 'Desactivar usuario');
        });

        // Restablecer contraseña: $.ajax POST con token CSRF -> contraseña temporal en un modal (una sola vez)
        $(document).on('click', '.btn-restablecer', function () {
            var $b = $(this), usuario = $b.data('usuario');
            if (!window.confirm('¿Restablecer la contraseña de ' + usuario + '? Se generará una contraseña temporal nueva.')) { return; }
            $b.prop('disabled', true);
            $.ajax({ url: $pagina.data('urlRestablecer'), method: 'POST', dataType: 'json', data: { id: $b.data('id') } })
                .done(function (r) {
                    if (r.ok) {
                        $('#passwordUsuario').text(r.usuario);
                        $('#passwordValor').val(r.passwordTemporal);
                        bootstrap.Modal.getOrCreateInstance($('#modalPassword')[0]).show();
                        mostrarMensaje(true, r.mensaje);
                    } else {
                        mostrarMensaje(false, r.mensaje);
                    }
                })
                .fail(function () { mostrarMensaje(false, 'No se pudo completar la operación.'); })
                .always(function () { $b.prop('disabled', false); });
        });
        // Al cerrar el modal se borra la contraseña de la pantalla
        $('#modalPassword').on('hidden.bs.modal', function () { $('#passwordValor').val(''); });

        // Desbloquear: $.ajax POST con token CSRF
        $(document).on('click', '.btn-desbloquear-usuario', function () {
            var $b = $(this);
            $b.prop('disabled', true);
            $.ajax({ url: $pagina.data('urlDesbloquear'), method: 'POST', dataType: 'json', data: { id: $b.data('id') } })
                .done(function (r) {
                    mostrarMensaje(r.ok, r.mensaje);
                    if (r.ok) { setTimeout(function () { window.location.reload(); }, 1500); }
                })
                .fail(function () { mostrarMensaje(false, 'No se pudo completar la operación.'); })
                .always(function () { $b.prop('disabled', false); });
        });
    }

    // ---------- Formulario de usuario ----------
    var $form = $('#formUsuario');
    if ($form.length) {
        var esNuevo = String($form.data('nuevo')) === 'true';
        var PATRON_USUARIO = /^[A-Za-z0-9._-]{3,30}$/;

        var rolActual = function () { return $('#rol').val() || $form.find('input[name="rol"]').val(); };
        var marcarArea = function () { $('#marcaArea').toggleClass('d-none', rolActual() !== 'ATENCION'); };
        $('#rol').on('change', marcarArea);
        marcarArea();

        $form.on('submit', function (e) {
            var nombres = $.trim($('#nombres').val());
            var reglas = [
                ['#username', !esNuevo || PATRON_USUARIO.test($('#username').val())],
                ['#nombres', nombres.length >= 3 && nombres.length <= 100],
                ['#idArea', rolActual() !== 'ATENCION' || $('#idArea').val() !== '']
            ];
            var hayError = false;
            $.each(reglas, function (i, r) {
                $(r[0]).toggleClass('is-invalid', !r[1]);
                if (!r[1]) { hayError = true; }
            });
            if (hayError) {
                e.preventDefault();
                $('.is-invalid').first().focus();
            }
        });
    }
});
