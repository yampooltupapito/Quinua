# Documentación técnica de SIGMUD para la monografía

Generada el 2026-11-19 a partir del código, los scripts SQL, las pruebas y los documentos del proyecto (`CLAUDE.md`, `docs/avance.md`,
`docs/auditoria.md`). **Describe solo lo que existe**; lo que no cuadra o falta está en `observaciones.md`. Ningún archivo de código,
configuración ni SQL del proyecto fue modificado: todo lo nuevo está en `docs/monografia/`.

> Los números de capítulo y sección de la tabla siguiente son una **propuesta** según el esquema pedido (Capítulo III secciones 3.2 a 3.9,
> Capítulo IV secciones 4.1 a 4.8, Capítulo V y Anexos A a F). El documento del curso no se entregó, así que ajuste la numeración a su índice.

## 1. Índice de archivos y dónde van en la monografía

| Archivo | Contenido | Capítulo / anexo (propuesta) |
|---|---|---|
| `01_resumen_del_sistema.md` | Ficha del sistema y estadísticas reales (tablas, clases por paquete, JSP, Servlets, rutas, líneas, pruebas) | Introducción y Resumen |
| `02_requerimientos.md` | Requerimientos funcionales (RF-01 a RF-33) y no funcionales (RNF-01 a RNF-23) | Cap. III, 3.2 y 3.3 |
| `03_actores_y_casos_de_uso.md` | Actores, matriz de casos de uso y descripción detallada | Cap. III, 3.4 |
| `04_procesos_criticos.md` | Los 4 procesos críticos (salida de material, cita sin cruce, contribuyente, login) | Cap. III, 3.5 |
| `05_modelo_de_datos.md` | ER, diccionario de datos, normalización a 3FN y decisiones de diseño | Cap. III, 3.6 |
| `06_arquitectura.md` | Capas, paquetes, ciclo de vida de una petición, árbol de paquetes y tabla de rutas | Cap. III, 3.7 |
| `07_clases.md` | Guía de los 6 diagramas de clases | Cap. III, 3.8 |
| `08_fragmentos_codigo.md` | 33 fragmentos reales de código (≤ 25 líneas) con ruta y líneas | Cap. IV, 4.1 a 4.8 |
| `09_entorno_de_desarrollo.md` | Versiones exactas con su fuente | Cap. IV, 4.1 |
| `10_capturas.md` | Lista numerada de 48 capturas por tomar | Cap. IV y Anexo C |
| `observaciones.md` | Inconsistencias, pruebas faltantes, riesgos y mejoras | Cap. V (conclusiones y recomendaciones) |
| `anexos/A_script_sql_completo.sql` | Los 4 scripts de `sql/` concatenados | Anexo A |
| `anexos/B_manual_instalacion.md` | Instalación paso a paso en Windows y problemas frecuentes | Anexo B |
| `anexos/C_manual_usuario.md` | Manual por rol y mensajes de error | Anexo C |
| `anexos/D_codigo_fuente.txt` | Código fuente propio (159 archivos) con la contraseña enmascarada | Anexo D |
| `anexos/E_matriz_trazabilidad.md` | Requerimiento → clases → pruebas | Anexo E |
| `anexos/F_evidencias_pruebas.md` | Plan de pruebas, resultados reales (376 JUnit y 207 del script), casos manuales y análisis | Anexo F y Cap. V |

### Diagramas (`diagramas/*.puml`)

| Archivo | Se usa en |
|---|---|
| `casos_de_uso_general.puml`, `casos_de_uso_administrador.puml`, `casos_de_uso_atencion.puml`, `casos_de_uso_almacen.puml` | `03_actores_y_casos_de_uso.md` |
| `actividad_salida_material.puml`, `actividad_programar_cita.puml`, `actividad_registrar_contribuyente.puml`, `actividad_inicio_sesion.puml`, `secuencia_salida_material.puml` | `04_procesos_criticos.md` |
| `er.puml` | `05_modelo_de_datos.md` |
| `arquitectura_capas.puml`, `paquetes.puml` | `06_arquitectura.md` |
| `clases_contribuyentes.puml`, `clases_almacen.puml`, `clases_citas.puml`, `clases_usuarios_seguridad.puml`, `clases_auditoria_paneles.puml`, `clases_infraestructura.puml` | `07_clases.md` |

## 2. Cómo ver los diagramas
Los diagramas son texto PlantUML. Para verlos:
1. Abra https://www.plantuml.com/plantuml (o cualquier editor PlantUML en línea), borre el ejemplo y **pegue el contenido completo del `.puml`**
   (desde `@startuml` hasta `@enduml`). Se dibuja al instante.
2. Descargue la imagen en PNG o SVG con las opciones del sitio y úsela en el documento Word.
3. Alternativa sin internet: instale la extensión "PlantUML" en VS Code (necesita Java y Graphviz) y abra el archivo con "Preview".
> **Los diagramas no se han renderizado ni validado con el programa PlantUML** (no había PlantUML en el equipo): se escribieron con cuidado
> pero si alguno marca un error de sintaxis, avise con el mensaje para corregirlo.

## 3. Origen de los datos de pruebas
- **JUnit (376 pruebas, 0 fallos)** y **script de seguridad (207 de 207 aprobadas)**: ejecutados de verdad el 2026-11-19 sobre una **copia aislada**
  del proyecto con una base MySQL 8.4.9 temporal y una copia de Tomcat en el puerto 18080. No se usó la base real ni se modificó el proyecto original.
- **Casos manuales** de `docs/avance.md`: 90 siguen "(pendiente)" y figuran como **PENDIENTE DE EJECUTAR**; 48 tienen un resultado ya escrito en ese
  documento y se copian como "documentado" (no se repitieron). Ver `anexos/F_evidencias_pruebas.md`, sección 6.
- Las capturas de pantalla **no se generaron**: las debe tomar usted siguiendo `10_capturas.md`.

## 4. Archivos con datos "por confirmar" o "PENDIENTE DE EJECUTAR"

| Archivo | Qué queda pendiente |
|---|---|
| `09_entorno_de_desarrollo.md` | "Por confirmar": versión del navegador de las capturas, versión de Windows del equipo de la sustentación, y que el controlador JDBC desplegado sea exactamente el del `pom.xml` |
| `anexos/F_evidencias_pruebas.md` | **PENDIENTE DE EJECUTAR:** 90 casos manuales de `docs/avance.md`; las pruebas de usabilidad formales; no existen pruebas de rendimiento ni de concurrencia |
| `anexos/E_matriz_trazabilidad.md` | "SIN PRUEBA" en materiales (DAO), reportes de almacén y de citas, y en varios requerimientos no funcionales (RNF-09, RNF-13, RNF-15, RNF-20) |
| `10_capturas.md` | Las 48 capturas están por tomar |
| `observaciones.md` | Triggers de auditoría pendientes en la base real, logo oficial, comparación con el documento original del curso |

## 5. Advertencias
- **Contraseñas:** en todos los archivos la contraseña de `db.properties` aparece como `********` y las contraseñas de ejemplo de las cuentas de
  demostración fueron enmascaradas. No las escriba en la monografía.
- **Anexo D** (840 KB, unas 18 000 líneas): es largo; considere entregarlo como archivo adjunto o paginado.
- **Datos de ejemplo:** los nombres y documentos de los datos de demostración son ficticios; no son datos reales de la Municipalidad de Quinua.
