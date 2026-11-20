// Tema claro / oscuro (data-bs-theme de Bootstrap 5.3).
// Se carga en el <head> de forma SÍNCRONA, antes de pintar la página, para evitar el parpadeo del tema equivocado.
// Orden de decisión: 1) la elección guardada en localStorage, 2) la preferencia del sistema operativo.
// La impresión siempre sale en tema claro.
(function () {
    'use strict';
    var CLAVE = 'sigmud-tema';
    var raiz = document.documentElement;

    // localStorage puede lanzar excepciones (modo privado, datos bloqueados): todo acceso va en try-catch
    function leerGuardado() {
        try {
            var v = window.localStorage.getItem(CLAVE);
            return v === 'dark' || v === 'light' ? v : null;
        } catch (e) { return null; }
    }
    function guardar(tema) {
        try { window.localStorage.setItem(CLAVE, tema); } catch (e) { /* sin almacenamiento: el tema vale solo para esta página */ }
    }
    function delSistema() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    function actual() {
        return raiz.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
    }

    var haElegido = leerGuardado() !== null;

    function aplicar(tema, avisar) {
        raiz.setAttribute('data-bs-theme', tema);
        actualizarBotones();
        if (avisar) {
            // Los gráficos (Chart.js) escuchan este evento para volver a dibujarse con los colores del tema
            document.dispatchEvent(new CustomEvent('sigmud:tema', { detail: { tema: tema } }));
        }
    }

    function actualizarBotones() {
        var oscuro = actual() === 'dark';
        var botones = document.querySelectorAll('.btn-tema');
        for (var i = 0; i < botones.length; i++) {
            var b = botones[i];
            b.setAttribute('aria-label', oscuro ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro');
            b.setAttribute('title', oscuro ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro');
            var icono = b.querySelector('i');
            if (icono) { icono.className = 'bi ' + (oscuro ? 'bi-sun-fill' : 'bi-moon-stars-fill'); }
        }
    }

    // 1) Aplicar de inmediato (antes de que el navegador pinte)
    raiz.setAttribute('data-bs-theme', leerGuardado() || delSistema());

    // 2) Si no hay elección guardada, seguir los cambios del sistema operativo
    if (window.matchMedia) {
        var consulta = window.matchMedia('(prefers-color-scheme: dark)');
        var alCambiar = function () { if (!haElegido) { aplicar(delSistema(), true); } };
        if (consulta.addEventListener) { consulta.addEventListener('change', alCambiar); }
    }

    // 3) Botones interruptor (los dibuja el servidor en la barra y en el login)
    document.addEventListener('DOMContentLoaded', function () {
        actualizarBotones();
        document.addEventListener('click', function (e) {
            var boton = e.target.closest ? e.target.closest('.btn-tema') : null;
            if (!boton) { return; }
            var nuevo = actual() === 'dark' ? 'light' : 'dark';
            haElegido = true;
            guardar(nuevo);
            aplicar(nuevo, true);
        });
    });

    // 4) La impresión SIEMPRE sale en tema claro: se cambia al imprimir y se restaura al terminar
    var temaAntesDeImprimir = null;
    window.addEventListener('beforeprint', function () {
        temaAntesDeImprimir = actual();
        if (temaAntesDeImprimir === 'dark') { aplicar('light', true); }
    });
    window.addEventListener('afterprint', function () {
        if (temaAntesDeImprimir === 'dark') { aplicar('dark', true); }
        temaAntesDeImprimir = null;
    });
})();
