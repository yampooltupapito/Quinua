// Avisos (toasts): mostrarToast(tipo, mensaje) con tipo 'exito', 'error', 'aviso' o 'info'.
// - Ícono, cierre manual (botón y tecla Escape) y desaparición automática a los 5 s (8 s en errores).
// - Se detiene mientras el mouse (o el teclado) esté sobre el aviso. Se apilan (máximo 5 a la vez).
// - El mensaje SIEMPRE se inserta con textContent (nunca como HTML) para evitar XSS.
// - Los mensajes "de una sola vez" (flash) los deja el servidor en data-* de #datosFlash y se muestran al cargar.
(function () {
    'use strict';

    var TIPOS = {
        exito: { fondo: 'text-bg-success', icono: 'bi-check-circle-fill', ms: 5000, rol: 'status', cierre: 'btn-close-white' },
        error: { fondo: 'text-bg-danger', icono: 'bi-x-octagon-fill', ms: 8000, rol: 'alert', cierre: 'btn-close-white' },
        aviso: { fondo: 'text-bg-warning', icono: 'bi-exclamation-triangle-fill', ms: 5000, rol: 'status', cierre: '' },
        info: { fondo: 'text-bg-info', icono: 'bi-info-circle-fill', ms: 5000, rol: 'status', cierre: '' }
    };
    var MAXIMO = 5;

    function contenedor() {
        var c = document.getElementById('contenedorToasts');
        if (!c) {
            c = document.createElement('div');
            c.id = 'contenedorToasts';
            c.className = 'toast-container position-fixed top-0 end-0 p-3';
            c.setAttribute('aria-live', 'polite');
            c.setAttribute('aria-atomic', 'false');
            document.body.appendChild(c);
        }
        return c;
    }

    function cerrar(toast) {
        if (toast.dataset.cerrando === '1') { return; }
        toast.dataset.cerrando = '1';
        clearTimeout(toast._temporizador);
        toast.classList.remove('show');
        setTimeout(function () { if (toast.parentNode) { toast.parentNode.removeChild(toast); } }, 200);
    }

    function programar(toast, ms) {
        clearTimeout(toast._temporizador);
        toast._inicio = Date.now();
        toast._restante = ms;
        toast._temporizador = setTimeout(function () { cerrar(toast); }, ms);
    }

    function pausar(toast) {
        if (toast.dataset.cerrando === '1') { return; }
        clearTimeout(toast._temporizador);
        toast._restante = Math.max(1500, toast._restante - (Date.now() - toast._inicio));
    }

    function reanudar(toast) {
        if (toast.dataset.cerrando === '1') { return; }
        programar(toast, toast._restante);
    }

    window.mostrarToast = function (tipo, mensaje) {
        var cfg = TIPOS[tipo] || TIPOS.info;
        var cont = contenedor();

        var toast = document.createElement('div');
        toast.className = 'toast fade align-items-center border-0 mb-2 ' + cfg.fondo;
        toast.setAttribute('role', cfg.rol);
        toast.setAttribute('aria-atomic', 'true');

        var fila = document.createElement('div');
        fila.className = 'd-flex';

        var cuerpo = document.createElement('div');
        cuerpo.className = 'toast-body d-flex align-items-start gap-2';
        var icono = document.createElement('i');
        icono.className = 'bi ' + cfg.icono;
        icono.setAttribute('aria-hidden', 'true');
        var texto = document.createElement('span');
        texto.textContent = String(mensaje == null ? '' : mensaje);   // textContent: nunca HTML
        cuerpo.appendChild(icono);
        cuerpo.appendChild(texto);

        var boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'btn-close me-2 m-auto ' + cfg.cierre;
        boton.setAttribute('aria-label', 'Cerrar aviso');
        boton.addEventListener('click', function () { cerrar(toast); });

        fila.appendChild(cuerpo);
        fila.appendChild(boton);
        toast.appendChild(fila);

        // Se detiene mientras el mouse o el teclado estén sobre el aviso
        toast.addEventListener('mouseenter', function () { pausar(toast); });
        toast.addEventListener('mouseleave', function () { reanudar(toast); });
        toast.addEventListener('focusin', function () { pausar(toast); });
        toast.addEventListener('focusout', function () { reanudar(toast); });
        toast.addEventListener('keydown', function (e) { if (e.key === 'Escape') { cerrar(toast); } });

        cont.appendChild(toast);
        while (cont.children.length > MAXIMO) { cont.removeChild(cont.firstChild); }
        window.requestAnimationFrame(function () { toast.classList.add('show'); });
        programar(toast, cfg.ms);
        return toast;
    };

    // Mensajes de una sola vez (flash) que dejó el servidor: <div id="datosFlash" hidden data-tipo data-mensaje>
    function mostrarFlash() {
        var datos = document.getElementById('datosFlash');
        if (!datos) { return; }
        var mensaje = datos.getAttribute('data-mensaje');
        if (mensaje) { window.mostrarToast(datos.getAttribute('data-tipo') || 'info', mensaje); }
        datos.parentNode.removeChild(datos);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mostrarFlash);
    } else {
        mostrarFlash();
    }
})();
