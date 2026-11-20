// Paginación: el selector de tamaño (10, 20 o 50) envía su formulario al cambiar.
// El formulario ya trae como campos ocultos los filtros y el orden actuales; la página vuelve a 1.
(function () {
    'use strict';
    document.addEventListener('change', function (e) {
        var sel = e.target;
        if (sel && sel.classList && sel.classList.contains('selector-tamano') && sel.form) {
            sel.form.submit();
        }
    });
})();
