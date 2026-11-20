// Citas: modales del listado, formulario con Ajax en cascada y reporte.
// El servidor vuelve a validar siempre; esto solo mejora la experiencia.
$(function () {

    // Token CSRF: se envía en la cabecera X-CSRF-Token de toda llamada Ajax
    var tokenCsrf = $('meta[name="csrf-token"]').attr('content');
    if (tokenCsrf) { $.ajaxSetup({ headers: { 'X-CSRF-Token': tokenCsrf } }); }

    // ================= Listado: modales de confirmación =================
    $('#modalCancelar').on('show.bs.modal', function (e) {
        var origen = $(e.relatedTarget);
        $('#cancelarId').val(origen.data('id'));
        $('#cancelarNombre').text(origen.data('nombre'));
    });
    $('#modalReprogramar').on('show.bs.modal', function (e) {
        var origen = $(e.relatedTarget);
        $('#reprogramarNombre').text(origen.data('nombre'));
        $('#reprogramarContinuar').attr('href', origen.data('url'));
    });

    // ================= Formulario de cita =================
    var $form = $('#formCita');
    if ($form.length) { iniciarFormulario($form); }

    // ================= Reporte =================
    var $rep = $('#formReporteCitas');
    if ($rep.length) { iniciarReporte($rep); }

    function iniciarFormulario($f) {
        var urlAjax = $f.data('urlAjax');
        var esNuevo = String($f.data('nuevo')) === 'true';
        var excluir = $f.data('excluir');
        var $buscar = $('#buscarContribuyente');
        var $lista = $('#listaContribuyentes');
        var temporizador = null;
        var peticionBusqueda = 0;
        var peticionTurnos = 0;

        // ---------- 1. Autocompletado de contribuyentes (con espera de 300 ms) ----------
        function ocultarLista() { $lista.addClass('d-none').empty(); }

        function mostrarResultados(datos) {
            $lista.empty();
            if (datos.length === 0) {
                $lista.append($('<div>').addClass('list-group-item text-muted').text('Sin resultados.'));
            }
            $.each(datos, function (i, c) {
                $lista.append($('<button>').attr('type', 'button').addClass('list-group-item list-group-item-action')
                    .data('id', c.id).data('texto', c.dniRuc + ' - ' + c.nombre)
                    .text(c.dniRuc + ' - ' + c.nombre));
            });
            $lista.removeClass('d-none');
        }

        function buscarContribuyentes(q) {
            var numero = ++peticionBusqueda;
            $.ajax({ url: urlAjax, method: 'GET', dataType: 'json', data: { accion: 'contribuyentes', q: q } })
                .done(function (r) {
                    if (numero !== peticionBusqueda) { return; }   // llegó una respuesta vieja: se ignora
                    if (r.ok) { mostrarResultados(r.datos); } else { $('#ayudaContribuyente').text(r.mensaje); }
                })
                .fail(function () {
                    $('#ayudaContribuyente').text('No se pudo completar la búsqueda.');
                    mostrarToast('error', 'No se pudo completar la búsqueda de contribuyentes.');
                });
        }

        if (esNuevo) {
            $buscar.on('input', function () {
                $('#idContribuyente').val('');           // si escribe de nuevo, debe volver a elegir
                $buscar.removeClass('is-invalid');
                clearTimeout(temporizador);
                var q = $.trim($buscar.val());
                if (q.length < 3) {
                    peticionBusqueda++;
                    ocultarLista();
                    $('#ayudaContribuyente').text('Escriba al menos 3 caracteres.');
                    return;
                }
                $('#ayudaContribuyente').text('Buscando...');
                temporizador = setTimeout(function () { buscarContribuyentes(q); }, 300);
            });
            $lista.on('click', 'button', function () {
                $('#idContribuyente').val($(this).data('id'));
                $buscar.val($(this).data('texto')).removeClass('is-invalid');
                $('#ayudaContribuyente').text('Contribuyente seleccionado.');
                ocultarLista();
            });
            $(document).on('click', function (e) {
                if (!$(e.target).closest('#listaContribuyentes, #buscarContribuyente').length) { ocultarLista(); }
            });
        }

        // ---------- 2. Área -> funcionarios (cascada) ----------
        function cargarFuncionarios() {
            var area = $('#idArea').val();
            var $fun = $('#idFuncionario');
            $fun.empty().append($('<option>').val('').text('Seleccione...'));
            limpiarTurnos();
            if (!area) { return; }
            $.ajax({ url: urlAjax, method: 'GET', dataType: 'json', data: { accion: 'funcionarios', area: area } })
                .done(function (r) {
                    if (!r.ok) { return; }
                    $.each(r.datos, function (i, u) { $fun.append($('<option>').val(u.id).text(u.nombre)); });
                    if (r.datos.length === 0) { $fun.append($('<option>').val('').text(r.mensaje).prop('disabled', true)); }
                })
                .fail(function () {
                    $fun.append($('<option>').val('').text('No se pudo cargar').prop('disabled', true));
                    mostrarToast('error', 'No se pudieron cargar los funcionarios.');
                });
        }
        if (esNuevo) { $('#idArea').on('change', cargarFuncionarios); }

        // ---------- 3. Fecha + funcionario -> cuadrícula de turnos ----------
        function limpiarTurnos() {
            $('#hora').val('');
            $('#turnos').empty().append($('<span>').addClass('text-muted small')
                .text('Elija funcionario y fecha para ver los turnos.'));
        }

        function mostrarTurnos(r, horaElegida) {
            var $caja = $('#turnos').empty();
            if (!r.ok || r.datos.length === 0) {
                $('#hora').val('');
                $caja.append($('<span>').addClass('text-danger small').text(r.mensaje || 'No hay turnos.'));
                return;
            }
            var hayElegida = false;
            $.each(r.datos, function (i, t) {
                var $b = $('<button>').attr('type', 'button').text(t.hora).data('hora', t.hora);
                if (t.libre) {
                    $b.addClass('btn btn-sm btn-outline-success turno-libre');
                    if (t.hora === horaElegida) { $b.removeClass('btn-outline-success').addClass('btn-success active'); hayElegida = true; }
                } else {
                    $b.addClass('btn btn-sm btn-secondary').prop('disabled', true).attr('title', 'Ocupado');
                }
                $caja.append($b);
            });
            if (!hayElegida) { $('#hora').val(''); }
        }

        function cargarTurnos() {
            var funcionario = $('#idFuncionario').val();
            var fecha = $('#fecha').val();
            if (!funcionario || !fecha) { limpiarTurnos(); return; }
            var horaElegida = $('#hora').val();
            var numero = ++peticionTurnos;
            $('#turnos').empty().append($('<span>').addClass('text-muted small').text('Cargando turnos...'));
            $.ajax({ url: urlAjax, method: 'GET', dataType: 'json',
                     data: { accion: 'horas', funcionario: funcionario, fecha: fecha, excluir: excluir } })
                .done(function (r) { if (numero === peticionTurnos) { mostrarTurnos(r, horaElegida); } })
                .fail(function () {
                    $('#turnos').empty().append($('<span>').addClass('text-danger small')
                        .text('No se pudo cargar los turnos.'));
                });
        }

        $('#idFuncionario').on('change', cargarTurnos);
        $('#fecha').on('change', cargarTurnos);

        // Elegir un turno libre
        $('#turnos').on('click', 'button.turno-libre', function () {
            $('#turnos button.active').removeClass('btn-success active').addClass('btn-outline-success');
            $(this).removeClass('btn-outline-success').addClass('btn-success active');
            $('#hora').val($(this).data('hora'));
            $('#errorHora').addClass('d-none');
        });

        // Al abrir (nuevo con datos repetidos tras un error, o reprogramar) se cargan los turnos
        if ($('#idFuncionario').val() && $('#fecha').val()) { cargarTurnos(); }

        // ---------- Validación previa ----------
        function esFinDeSemana(texto) {
            var d = new Date(texto + 'T00:00:00');
            return d.getDay() === 0 || d.getDay() === 6;
        }

        $f.on('submit', function (e) {
            var fecha = $('#fecha').val();
            var motivo = $.trim($('#motivo').val());
            var reglas = [
                ['#buscarContribuyente', !esNuevo || $('#idContribuyente').val() !== ''],
                ['#idArea', !!$('#idArea').val()],
                ['#idFuncionario', !!$('#idFuncionario').val()],
                ['#fecha', /^\d{4}-\d{2}-\d{2}$/.test(fecha) && !esFinDeSemana(fecha)],
                ['#ventanilla', /^([1-9]|10)$/.test($('#ventanilla').val())],
                ['#motivo', motivo.length >= 1 && motivo.length <= 200]
            ];
            var hayError = false;
            $.each(reglas, function (i, r) {
                $(r[0]).toggleClass('is-invalid', !r[1]);
                if (!r[1]) { hayError = true; }
            });
            var sinHora = $('#hora').val() === '';
            $('#errorHora').toggleClass('d-none', !sinHora);
            if (sinHora) { hayError = true; }
            if (hayError) {
                e.preventDefault();
                $('.is-invalid').first().focus();
            }
        });
    }

    function iniciarReporte($f) {
        var MAX_DIAS = 366;
        var PATRON = /^\d{4}-\d{2}-\d{2}$/;

        function fechaDe(t) { return PATRON.test(t) ? new Date(t + 'T00:00:00') : null; }

        $f.on('submit', function (e) {
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
        var sinDatos = $('#tablasReporte tbody tr.fila-dato').length === 0;
        $('#btnCsv').toggleClass('disabled', sinDatos).attr('aria-disabled', sinDatos ? 'true' : 'false')
            .on('click', function (e) { if ($(this).hasClass('disabled')) { e.preventDefault(); } });

        $('#btnImprimir').on('click', function () { window.print(); });
    }
});
