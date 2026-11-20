# Anexo E. Matriz de trazabilidad: requerimiento → clases → pruebas

Cómo leerla:
- **Clases implicadas:** Servlet (controlador), DAO, modelo y JSP principales de cada requerimiento (ver `02_requerimientos.md`).
- **Pruebas:** nombre de la clase JUnit (y métodos representativos) o número de caso del script de seguridad (`S-nnn`, definidos en
  `F_evidencias_pruebas.md`, sección 5). Las pruebas **JUnit de DAO** usan la base de pruebas `mun_quinua_test`.
- **"SIN PRUEBA"** = no existe ninguna prueba automática ni caso con resultado real que lo verifique. Cuando solo hay una verificación
  puntual escrita en `docs/avance.md` (hecha una vez, no repetible), se indica como nota, pero el requerimiento sigue marcado
  "SIN PRUEBA REPETIBLE".
- Ninguna clase de prueba prueba los Servlets, `AuthFilter`, `CsrfFilter` ni el JavaScript directamente: ese comportamiento solo se
  comprueba "desde afuera" con el script `pruebas-seguridad.ps1`.

## 1. Requerimientos funcionales

| Req. | Servlet / filtro | DAO / util | Modelo | JSP | Pruebas que lo verifican |
|---|---|---|---|---|---|
| RF-01 Iniciar sesión | `LoginServlet` | `UsuarioDAOImpl`, `Seguridad`, `Validador` | `Usuario` | `login.jsp`, `login.js` | JUnit `UsuarioDAOTest` (`buscaUnUsuarioExistenteConSusDatos`, `usuarioInexistenteDevuelveVacio`, `usuarioConComillasNoRompeLaConsultaNiDevuelveDatos`, `elAccesoExitosoReiniciaElContadorYGuardaLaFecha`), `SeguridadTest`, `ValidadorTest`; script S-005, S-006, S-007, S-008, S-013, S-014 |
| RF-02 Bloqueo por intentos | `LoginServlet` | `UsuarioDAOImpl` (`MAX_INTENTOS = 5`) | `Usuario` | `login.jsp` | JUnit `UsuarioDAOTest` (`cadaIntentoFallidoSumaUno`, `alQuintoIntentoFallidoLaCuentaSeBloquea`, `desbloquearReiniciaLosIntentosYReactivaUnaCuentaBloqueada`); script S-097, S-098 (el fallo queda en auditoría). *El mensaje de "cuenta bloqueada" con contraseña correcta no tiene prueba automática* |
| RF-03 Cerrar sesión | `LogoutServlet` | `Auditoria` | — | `barra.jsp` | Script S-206, S-207 |
| RF-04 Control de acceso por rol | `AuthFilter` | — | `Usuario` | `barra.jsp`, `tarjetas-modulos.jsp`, `error.jsp` | Script S-001 a S-004, S-026 a S-032, S-046, S-049, S-087 a S-091, S-107. `AuthFilter` no tiene prueba JUnit |
| RF-05 Contraseña temporal obligatoria | `AuthFilter`, `PerfilServlet` | `UsuarioDAOImpl.actualizarPassword` | `Usuario` | `perfil/password.jsp` | JUnit `UsuarioDAOTest.actualizarPasswordGuardaElHashYElIndicadorDeCambioObligatorio`; script S-067 a S-073 |
| RF-06 Mi perfil y cambio de contraseña | `PerfilServlet` | `Validador.politicaPassword`, `Seguridad`, `UsuarioDAOImpl` | `Usuario` | `perfil/perfil.jsp`, `perfil/password.jsp`, `password.js` | JUnit `PoliticaPasswordTest` (17), `SeguridadTest` (30); script S-074 a S-085 |
| RF-07 Registrar contribuyente | `ContribuyenteServlet` | `ContribuyenteValidador`, `ContribuyenteDAOImpl.insertar` | `Contribuyente` | `contribuyentes/formulario.jsp`, `contribuyentes.js` | JUnit `ContribuyenteValidadorTest` (20), `ValidadorTest` (57), `ContribuyenteDAOTest` (`insertarGuardaElContribuyenteYDevuelveSuId`, `dniDuplicadoLanzaRegistroDuplicadoException`); script S-021, S-022. *La validación jQuery del formulario no tiene prueba automática* |
| RF-08 Editar contribuyente | `ContribuyenteServlet` | `ContribuyenteDAOImpl.actualizar` | `Contribuyente` | `contribuyentes/formulario.jsp` | JUnit `ContribuyenteDAOTest` (`actualizarCambiaLosDatosPeroNoElEstado`, `actualizarConDniDeOtroContribuyenteLanzaDuplicado`, `existeDniRucExcluyeElIdIndicado`) |
| RF-09 Baja lógica de contribuyente | `ContribuyenteServlet` | `ContribuyenteDAOImpl.darDeBaja` | `Contribuyente` | `contribuyentes/listado.jsp`, `ficha.jsp` | JUnit `ContribuyenteDAOTest.darDeBajaEsLogicaYNoBorraLaFila`; script S-125, S-127 a S-131 |
| RF-10 Reactivar contribuyente | `ContribuyenteServlet` | `ContribuyenteDAOImpl.reactivar` | `Contribuyente` | `contribuyentes/ficha.jsp` | JUnit `ContribuyenteDAOTest.reactivarVuelveAPonerloActivo`, `FichaContribuyenteDAOTest.reactivarLoDevuelveAlListadoNormal`; script S-108, S-126, S-132 a S-134 |
| RF-11 Listar, buscar, ordenar y paginar contribuyentes | `ContribuyenteServlet` | `ContribuyenteDAOImpl.listarPagina`, `Paginador`, `OrdenSql` | `Contribuyente`, `Pagina` | `contribuyentes/listado.jsp`, `paginacion.jsp`, `thOrden.tag` | JUnit `ListadosPaginadosDAOTest` (`paginaYCuentaConLimitYOffset`, `ordenaPorColumnaYDireccion`, `unaColumnaConInyeccionUsaElOrdenPorDefectoYNoRompeNada`, `incluyeInactivosSoloSiSePide`, `elTextoConComodinesSeBuscaComoTexto`), `ContribuyenteDAOTest.buscarPorNombreODniEscapaLosComodinesDeLike`, `PaginaTest`, `OrdenSqlTest`; script S-140 a S-151, S-176 a S-180 |
| RF-12 Ficha del contribuyente | `ContribuyenteServlet` (`accion=ficha`) | `CitaDAOImpl` (resumen, próxima, historial), `AuditoriaDAOImpl.ultimosPorEntidad` | `Contribuyente`, `ResumenCitas`, `Cita` | `contribuyentes/ficha.jsp`, `impresion.css` | JUnit `FichaContribuyenteDAOTest` (15); script S-105 a S-124, S-135 a S-139, S-182 |
| RF-13 Registrar material | `MaterialServlet` | `MaterialValidador`, `MaterialDAOImpl.insertar` | `Material` | `almacen/formulario.jsp` | JUnit `MaterialValidadorTest` (28) solo prueba la validación. **`MaterialDAOImpl.insertar` SIN PRUEBA** (no existe `MaterialDAOTest`). Verificación puntual en `docs/avance.md` (Módulo 3, parte 1) con resultados "(pendiente)" |
| RF-14 Editar material | `MaterialServlet` | `MaterialDAOImpl.actualizar` | `Material` | `almacen/formulario.jsp` | `MaterialValidadorTest` (validación). **`MaterialDAOImpl.actualizar` SIN PRUEBA** |
| RF-15 Baja lógica de material | `MaterialServlet` | `MaterialDAOImpl.darDeBaja` | `Material` | `almacen/listado.jsp` | **SIN PRUEBA** directa de `darDeBaja`. (`MovimientoDAOTest.materialDadoDeBajaNoAdmiteMovimientos` comprueba que un material de baja no admite movimientos) |
| RF-16 Listar, buscar y paginar materiales | `MaterialServlet` | `MaterialDAOImpl.listarPagina` | `Material`, `Pagina` | `almacen/listado.jsp` | JUnit `ListadosPaginadosDAOTest.paginaYOrdenaMateriales` (paginación, orden y filtro de stock crítico); script S-152 a S-163 |
| RF-17 Entrada de stock | `MovimientoServlet` | `MovimientoDAOImpl.registrarMovimiento` | `MovimientoStock`, `Material` | `almacen/listado.jsp`, `movimientos.js` | JUnit `MovimientoDAOTest` (`laEntradaSumaStockYGuardaElMovimiento`, `laEntradaIgnoraElAreaYLaGuardaComoNull`); script S-020 |
| RF-18 Salida de stock con transacción | `MovimientoServlet` | `MovimientoDAOImpl.registrarMovimiento` | `MovimientoStock`, `Material` | `almacen/listado.jsp`, `movimientos.js` | JUnit `MovimientoDAOTest` (`laSalidaRestaStockYGuardaElArea`, `laSalidaDeTodoElStockEsValida`, `salidaMayorAlStockLanzaStockInsuficienteYNoCambiaNada`, `rollbackSiFallaElSegundoPasoNoDejaNadaGuardado`, `despuesDeUnRollbackLaSiguienteOperacionFuncionaNormalmente`, `materialInexistenteLanzaMaterialNoDisponible`, `unTipoInvalidoSeRechazaSinTocarLaBase`, `variasOperacionesSeguidasMantienenElStockConsistenteConElHistorial`). La concurrencia real (`FOR UPDATE` con dos usuarios a la vez) **SIN PRUEBA** |
| RF-19 Historial de movimientos | `MovimientoServlet` | `MovimientoDAOImpl.listarPorMaterialPagina` | `MovimientoStock`, `Pagina` | `almacen/historial.jsp` | JUnit `MovimientoDAOTest.elHistorialTraeLosMasRecientesPrimeroConNombres`, `ListadosPaginadosDAOTest.paginaElHistorialDeMovimientos`; script S-181 |
| RF-20 Reportes de almacén | `ReporteAlmacenServlet` | `ReporteAlmacenDAOImpl`, `FiltrosReporte`, `Csv` | `FilaStockCritico`, `FilaResumenMaterial`, `FilaSalidaArea`, `TablaReporte` | `almacen/reportes.jsp`, `reportes.js` | JUnit `CsvTest` (6) solo prueba el escape de CSV. **Consultas de reporte y `FiltrosReporte` SIN PRUEBA REPETIBLE** (verificación puntual con material temporal descrita en `docs/avance.md`, Módulo 3, parte 3) |
| RF-21 Programar cita | `CitaServlet` | `CitaValidador`, `CitaDAOImpl.insertar` | `Cita` | `citas/formulario.jsp`, `citas.js` | JUnit `CitaDAOTest` (`insertarGuardaLaCitaProgramadaConSuId`, `elMismoFuncionarioFechaYHoraSeRechazaPorCruce`, `otroFuncionarioALaMismaHoraSiSePermite`, `siFallaLaConsultaPreviaElUniqueDeLaBaseTambienRechazaElCruce`), `CitaValidadorTest` (44); script S-131, S-135 a S-139 |
| RF-22 Reprogramar cita | `CitaServlet` | `CitaDAOImpl.reprogramar` | `Cita` | `citas/formulario.jsp` | JUnit `CitaDAOTest` (`reprogramarAUnaHoraOcupadaSeRechaza`, `reprogramarSinCambiarLaHoraSiSePermite`, `reprogramarAUnTurnoLibreCambiaFechaHoraYVentanilla`, `unaCitaCanceladaOAtendidaNoSeReprograma`) |
| RF-23 Atender o cancelar cita | `CitaServlet` | `CitaDAOImpl.cambiarEstado` | `Cita` | `citas/listado.jsp` | JUnit `CitaDAOTest` (`cancelarUnaCitaLiberaElHorario`, `unaCitaCanceladaNoPuedePasarAAtendida`, `soloSePuedeAtenderElDiaDeLaCitaODespues`, `unEstadoNuevoInvalidoOUnaCitaInexistenteSeRechazan`) |
| RF-24 Listado del día y ayudas Ajax | `CitaServlet`, `CitaAjaxServlet` | `CitaDAOImpl.listarPorFecha`, `horasOcupadas`, `UsuarioDAOImpl.listarFuncionariosPorArea` | `Cita`, `Usuario` | `citas/listado.jsp`, `citas.js` | JUnit `CitaDAOTest` (`listarPorFechaSeOrdenaPorHoraYFiltraPorAreaOpcional`, `existeCruceYHorasOcupadasConsideranSoloCitasNoCanceladas`), `UsuarioDAOTest` (`listaFuncionariosActivosDeAtencionPorArea`, `unFuncionarioDesactivadoNoAparece`). **`CitaAjaxServlet` y el JavaScript SIN PRUEBA REPETIBLE** (verificación puntual en `docs/avance.md`, Módulo 4, parte 2) |
| RF-25 Reporte de citas | `ReporteCitasServlet` | `ReporteCitasDAOImpl`, `FiltrosReporte`, `Csv` | `FilaCitasResumen`, `TablaReporte` | `citas/reportes.jsp` | **SIN PRUEBA REPETIBLE** (no existe prueba de `ReporteCitasDAOImpl`; verificación puntual en `docs/avance.md`) |
| RF-26 Panel del administrador | `DashboardServlet`, `DashboardDatosServlet`, `DashboardDesbloquearServlet` | `PanelDatos`, `DashboardDAOImpl`, `UsuarioDAOImpl.desbloquear` | `EtiquetaValor`, `PuntoDiario` | `dashboard/admin.jsp`, `dashboard.js`, `kpi.tag` | JUnit `DashboardDAOTest` (15), `PanelDatosTest` (`elPanelDelAdminTieneSusIndicadoresGraficosYListas`, `elPanelDelAdminCuentaLosLoginFallidosDe24HorasYListaLaActividadReciente`); script S-033 a S-036, S-040, S-044, S-045, S-103 |
| RF-27 Panel de atención | idem | idem | idem | `dashboard/atencion.jsp` | JUnit `DashboardDAOTest` (`misCitasHoy...`, `misProximasCitas...`, `misCitasAtendidasPorDia...`), `PanelDatosTest` (`elPanelDeAtencionNoContieneNingunDatoDelAdminNiDelAlmacen`, `elPanelDeAtencionUsaSoloElFuncionarioIndicado`); script S-037 a S-040, S-104 |
| RF-28 Panel de almacén | idem | idem | idem | `dashboard/almacen.jsp` | JUnit `DashboardDAOTest` (`losTotalesDelAlmacen...`, `losMaterialesParaReponer...`, `salidasPorAreaMes...`, `losUltimosMovimientos...`), `PanelDatosTest.elPanelDelAlmacenTieneSusIndicadoresGraficosYListas`; script S-041 a S-043 |
| RF-29 Gestionar usuarios | `UsuarioServlet` | `UsuarioValidador`, `UsuarioDAOImpl` | `Usuario` | `usuarios/listado.jsp`, `formulario.jsp`, `usuarios.js` | JUnit `UsuarioDAOTest` (`insertarGuardaElUsuarioActivoConClaveTemporalYHashPbkdf2`, `unUsernameDuplicadoLanzaRegistroDuplicadoException`, `actualizarCambiaNombreRolYAreaPeroNoElUsernameNiLaClave`, `cambiarEstadoDesactivaYActivaSinBorrarLaFila`, `noSePuedeDesactivarAlUltimoAdminActivo`, `noSePuedeQuitarElRolAlUltimoAdminActivo`, `listarFiltraPorTextoRolYEstado`), `UsuarioValidadorTest` (8); script S-046 a S-062 |
| RF-30 Restablecer clave y desbloquear | `UsuarioAjaxServlet`, `DashboardDesbloquearServlet` | `UsuarioDAOImpl.actualizarPassword`, `desbloquear`, `Seguridad.generarPasswordTemporal` | `Usuario` | `usuarios/listado.jsp`, `usuarios.js` | JUnit `UsuarioDAOTest` (`desbloquear*`, `activarUnaCuentaBloqueadaReiniciaLosIntentos`), `SeguridadTest` (`laContrasenaTemporalTiene12CaracteresYCumpleLaPolitica`); script S-040, S-044, S-045, S-063 a S-066 |
| RF-31 Registrar auditoría | `Auditoria` (util), todos los Servlets | `AuditoriaDAOImpl` | `RegistroAuditoria` | — | JUnit `AuditoriaDAOTest` (18), `AuditoriaTest` (17), `MovimientoDAOTest` (`elMovimientoExitosoDejaUnRegistroDeAuditoriaConLaIp`, `siFallaElMovimientoNoQuedaAuditoria`); script S-098 a S-102, S-124, S-134 |
| RF-32 Consultar y exportar auditoría | `AuditoriaServlet` | `AuditoriaDAOImpl.buscar/contar`, `Csv` | `FiltrosAuditoria`, `RegistroAuditoria` | `auditoria/listado.jsp`, `auditoria.js` | JUnit `AuditoriaDAOTest` (`filtraPor*`, `combinaVariosFiltros`, `unaInyeccionSqlEnLosFiltrosSeTrataComoTexto`, `paginaYCuenta`); script S-087 a S-096 |
| RF-33 Avisos y mensajes | todos los Servlets (flash) | — | — | `toasts.jsp`, `toast.js` | Script S-183, S-184. El comportamiento de `toast.js` en el navegador **SIN PRUEBA automática** (verificado a mano según `docs/avance.md`, paquete visual) |

## 2. Requerimientos no funcionales

| Req. | Clases implicadas | Pruebas que lo verifican |
|---|---|---|
| RNF-01 SQL parametrizado y orden con lista blanca | todos los `DAOImpl`, `OrdenSql` | JUnit `OrdenSqlTest` (8), `ListadosPaginadosDAOTest.unaColumnaConInyeccionUsaElOrdenPorDefectoYNoRompeNada`, `AuditoriaDAOTest.unaInyeccionSqlEnLosFiltrosSeTrataComoTexto`, `UsuarioDAOTest.listarTrataElTextoComoTextoNoComoSql`; script S-005, S-006, S-096, S-115 a S-123, S-140 a S-182. Revisión de código en `docs/auditoria.md` §1(a) |
| RNF-02 Contraseñas con PBKDF2 | `Seguridad` | JUnit `SeguridadTest` (30), `UsuarioDAOTest.laMigracionDeUnHashAntiguoGuardaElFormatoNuevoYSigueVerificando` |
| RNF-03 CSRF | `CsrfFilter`, `csrf.jsp`, `meta-csrf.jsp` | Script S-015 a S-020, S-044, S-054, S-063, S-079, S-125, S-126 |
| RNF-04 Cabeceras y CSP | `SecurityHeadersFilter` | Script S-009 a S-012, S-193 a S-205 |
| RNF-05 Sesión segura | `LoginServlet`, `web.xml` | Script S-014, S-081 |
| RNF-06 Evitar XSS | todas las vistas (`<c:out>`), `toast.js` | Script S-021 a S-023, S-101, S-110. El escapado en cada JSP se revisó por código (`docs/auditoria.md` §1(e)) |
| RNF-07 Errores sin detalles técnicos | `web.xml`, `error.jsp`, Servlets | Script S-024, S-025 |
| RNF-08 Protección de datos personales | `ContribuyenteServlet`, `Auditoria` | Script S-124 |
| RNF-09 Interfaz adaptable | Bootstrap, vistas | **SIN PRUEBA** (solo verificación visual) |
| RNF-10 Modo claro/oscuro | `tema.js`, `sigmud.css` | Script S-185, S-188 (presencia del botón de tema y del script). El cambio de tema y su persistencia **SIN PRUEBA automática** (verificado con navegador según `docs/avance.md`) |
| RNF-11 Accesibilidad básica | `saltar.jsp`, `cabecera.jsp`, `thOrden.tag` | Script S-178, S-179, S-184, S-186. Foco, teclado y contraste **SIN PRUEBA automática** |
| RNF-12 Doble validación | validadores + jQuery | Servidor: JUnit de validadores; **jQuery SIN PRUEBA automática** |
| RNF-13 Mensajes en español | todo el sistema | **SIN PRUEBA** específica |
| RNF-14 Paginación en el servidor | `Pagina`, `Paginador`, `ListaVista` | JUnit `PaginaTest` (8), `ListadosPaginadosDAOTest` (10); script S-140 a S-177 |
| RNF-15 Índices | `sql/02_tablas_sigmud.sql` | **SIN PRUEBA** (no hay medición de rendimiento) |
| RNF-16 Arquitectura por capas | estructura de paquetes | No aplica prueba (inspección de código, ver `06_arquitectura.md`) |
| RNF-17 Pruebas automatizadas | `src/test`, `pruebas-seguridad.ps1` | Es el propio conjunto: 376 JUnit y 207 comprobaciones |
| RNF-18 Documentación | `docs/` | No aplica prueba (inspección) |
| RNF-19 Funciona sin internet | assets locales | Script S-189 a S-192, S-205 (sin CDN). No se probó con el equipo realmente desconectado |
| RNF-20 Portabilidad | `pom.xml`, `web.xml` | **SIN PRUEBA** en otras versiones de Tomcat/MySQL/JDK: solo la combinación documentada en `09_entorno_de_desarrollo.md` |
| RNF-21 Operaciones atómicas | `MovimientoDAOImpl` | JUnit `MovimientoDAOTest` (rollback), `AuditoriaDAOTest` (`registrarDentroDeUnaTransaccionConRollbackNoDejaRastro`, `...ConCommitQueda`) |
| RNF-22 Integridad de datos | restricciones SQL | JUnit `CitaDAOTest.siFallaLaConsultaPreviaElUniqueDeLaBaseTambienRechazaElCruce`, `ContribuyenteDAOTest.dniDuplicadoLanzaRegistroDuplicadoException`. Los `CHECK` (documento, stock, cantidad) **SIN PRUEBA** directa |
| RNF-23 Registro inmutable | triggers de `auditoria` | JUnit `AuditoriaDAOTest.unUpdateDirectoFallaPorElTrigger`, `unDeleteDirectoFallaPorElTrigger` |

## 3. Resumen de la matriz

| Concepto | Cantidad | Requerimientos |
|---|---:|---|
| Requerimientos funcionales | 33 | RF-01 a RF-33 |
| RF cuyo comportamiento esencial tiene prueba repetible (JUnit o script) | 27 | todos los demás |
| RF con prueba solo **parcial** | 4 | RF-13, RF-14, RF-15 (solo la validación o un efecto indirecto; el DAO de materiales no tiene prueba) y RF-24 (el DAO sí, el Servlet Ajax y el JavaScript no) |
| RF **SIN PRUEBA REPETIBLE** en lo esencial | 2 | RF-20 (reportes de almacén) y RF-25 (reporte de citas) |
| Requerimientos no funcionales | 23 | RNF-01 a RNF-23 |
| RNF con prueba automática | 14 | RNF-01 a RNF-08, RNF-14, RNF-17, RNF-19, RNF-21, RNF-22 (los CHECK sin prueba directa), RNF-23 |
| RNF con prueba solo parcial | 3 | RNF-10, RNF-11, RNF-12 |
| RNF **SIN PRUEBA** | 4 | RNF-09, RNF-13, RNF-15, RNF-20 |
| RNF que se verifican por inspección (no aplica prueba) | 2 | RNF-16, RNF-18 |
