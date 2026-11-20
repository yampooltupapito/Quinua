# 05. Modelo de datos (Capítulo III, sección 3.6)

Fuente: `sql/02_tablas_sigmud.sql` (instalación nueva, que ya incluye los cambios de `04_usuarios.sql` y `05_auditoria.sql`).
Motor InnoDB, juego de caracteres `utf8mb4`, ordenación `utf8mb4_0900_ai_ci`. Esquema `mun_quinua`.

## 1. Diagrama entidad-relación
Archivo: `diagramas/er.puml`.

Relaciones (todas de uno a muchos):
| Padre | Hijo | Clave foránea | Obligatoria |
|---|---|---|---|
| `area` | `usuario` | `usuario.id_area` | No (puede ser nula) |
| `area` | `movimiento_stock` | `movimiento_stock.id_area` | No (solo las salidas llevan área) |
| `area` | `cita` | `cita.id_area` | Sí |
| `usuario` | `movimiento_stock` | `movimiento_stock.id_usuario` | Sí |
| `usuario` | `cita` | `cita.id_funcionario` | Sí |
| `usuario` | `auditoria` | `auditoria.id_usuario` (`ON DELETE SET NULL`) | No |
| `material` | `movimiento_stock` | `movimiento_stock.id_material` | Sí |
| `contribuyente` | `cita` | `cita.id_contribuyente` | Sí |

## 2. Diccionario de datos

### 2.1 `area` — áreas o gerencias de la municipalidad
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_area` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `nombre` | VARCHAR(80) | No | UNIQUE `uq_area_nombre` | Nombre del área |
| `estado` | TINYINT(1), por defecto 1 | No | | 1 = activa |

### 2.2 `usuario` — cuentas del sistema
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_usuario` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `username` | VARCHAR(30) | No | UNIQUE `uq_usuario_username` | Nombre de usuario |
| `password_hash` | VARCHAR(255) | No | | `pbkdf2$iteraciones$sal$hash` o, en cuentas antiguas, SHA-256 de 64 hexadecimales |
| `nombres` | VARCHAR(100) | No | | Nombre completo |
| `rol` | ENUM('ADMIN','ATENCION','ALMACEN') | No | | Rol del usuario |
| `id_area` | INT UNSIGNED | Sí | FK `fk_usuario_area` → `area` | Área a la que pertenece |
| `estado` | TINYINT(1), por defecto 1 | No | | 1 = activa; 0 = desactivada o **bloqueada** por intentos |
| `intentos_fallidos` | TINYINT UNSIGNED, por defecto 0 | No | | A los 5 se bloquea la cuenta |
| `ultimo_acceso` | DATETIME | Sí | | Último inicio de sesión correcto |
| `debe_cambiar_password` | TINYINT(1), por defecto 0 | No | | 1 = contraseña temporal |

### 2.3 `contribuyente` — padrón de contribuyentes
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_contribuyente` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `tipo_persona` | ENUM('NATURAL','JURIDICA') | No | | Tipo de persona |
| `dni_ruc` | VARCHAR(11) | No | UNIQUE `uq_contribuyente_dni_ruc` | DNI (8) o RUC (11) |
| `nombres_razon_social` | VARCHAR(150) | No | | Nombres o razón social |
| `fecha_nacimiento` | DATE | Sí | | Solo personas naturales |
| `telefono` | VARCHAR(9) | Sí | | 9 dígitos (lo valida la aplicación) |
| `email` | VARCHAR(100) | Sí | | Correo |
| `direccion` | VARCHAR(200) | Sí | | Dirección |
| `estado` | TINYINT(1), por defecto 1 | No | | 1 = activo; 0 = dado de baja (baja lógica) |
| `fecha_registro` | DATETIME, por defecto `CURRENT_TIMESTAMP` | No | | Fecha de alta |

**CHECK `ck_contrib_doc`:** `(tipo_persona='NATURAL' AND CHAR_LENGTH(dni_ruc)=8) OR (tipo_persona='JURIDICA' AND CHAR_LENGTH(dni_ruc)=11)`.
(Que el RUC empiece con 10 o 20 y que el DNI solo tenga dígitos lo exige la aplicación, no la base.)

### 2.4 `material` — materiales del almacén
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_material` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `codigo` | VARCHAR(15) | No | UNIQUE `uq_material_codigo` | Código (la aplicación exige el formato `AAA-999`) |
| `nombre` | VARCHAR(100) | No | | Nombre del material |
| `unidad` | VARCHAR(20), por defecto 'UNIDAD' | No | | Unidad de medida (la lista permitida está en la aplicación) |
| `stock` | INT, por defecto 0 | No | | Stock actual |
| `stock_minimo` | INT, por defecto 0 | No | | Umbral de stock crítico |
| `estado` | TINYINT(1), por defecto 1 | No | | 1 = activo; 0 = de baja |

**CHECK `ck_material_stock`:** `stock >= 0 AND stock_minimo >= 0`.

### 2.5 `movimiento_stock` — entradas y salidas
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_movimiento` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `id_material` | INT UNSIGNED | No | FK `fk_mov_material` → `material` | Material movido |
| `tipo` | ENUM('ENTRADA','SALIDA') | No | | Tipo de movimiento |
| `cantidad` | INT | No | | **CHECK `ck_mov_cantidad`: `cantidad > 0`** |
| `fecha` | DATETIME, por defecto `CURRENT_TIMESTAMP` | No | | Fecha y hora |
| `id_area` | INT UNSIGNED | Sí | FK `fk_mov_area` → `area` | Área solicitante (solo salidas) |
| `id_usuario` | INT UNSIGNED | No | FK `fk_mov_usuario` → `usuario` | Quien lo registró |
| `observacion` | VARCHAR(200) | Sí | | Comentario |

Índice: `ix_mov_material_fecha (id_material, fecha)`; además los índices de las tres claves foráneas.

### 2.6 `cita` — citas de atención
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_cita` | INT UNSIGNED, AUTO_INCREMENT | No | PK | Identificador |
| `id_contribuyente` | INT UNSIGNED | No | FK `fk_cita_contrib` → `contribuyente` | Contribuyente atendido |
| `id_area` | INT UNSIGNED | No | FK `fk_cita_area` → `area` | Área de atención |
| `id_funcionario` | INT UNSIGNED | No | FK `fk_cita_func` → `usuario` | Funcionario que atiende |
| `ventanilla` | TINYINT UNSIGNED | No | | Número de ventanilla (la aplicación exige 1 a 10) |
| `fecha` | DATE | No | | Fecha de la cita |
| `hora` | TIME | No | | Hora de la cita |
| `motivo` | VARCHAR(200) | Sí | | Motivo (la aplicación lo exige) |
| `estado` | ENUM('PROGRAMADA','ATENDIDA','CANCELADA'), por defecto 'PROGRAMADA' | No | | Estado |
| `hora_activa` | TIME, **columna generada STORED**: `IF(estado='CANCELADA', NULL, hora)` | Sí | | Hora que ocupa el horario (NULL si está cancelada) |

Restricciones e índices: **UNIQUE `uq_cita_funcionario_horario (id_funcionario, fecha, hora_activa)`** (no cruce de horarios);
índice `ix_cita_fecha_area (fecha, id_area)`; índices de las claves foráneas.

### 2.7 `auditoria` — registro inmutable de cambios
| Columna | Tipo | Nulo | Clave | Descripción / restricciones |
|---|---|---|---|---|
| `id_auditoria` | BIGINT, AUTO_INCREMENT | No | PK | Identificador |
| `fecha_hora` | DATETIME(3), por defecto `CURRENT_TIMESTAMP(3)` | No | Índice `ix_auditoria_fecha` | Momento exacto (milisegundos) |
| `id_usuario` | INT UNSIGNED | Sí | FK `fk_auditoria_usuario` → `usuario` (`ON DELETE SET NULL`); índice `ix_auditoria_usuario` | Usuario (nulo si no había sesión, por ejemplo un login fallido) |
| `username` | VARCHAR(40), por defecto '' | No | | Copia del nombre de usuario en ese momento |
| `accion` | VARCHAR(30) | No | Índice `ix_auditoria_accion` | Tipo de acción (`LOGIN_OK`, `CREAR`, `BAJA`, `MOVIMIENTO_STOCK`, ...) |
| `entidad` | VARCHAR(30), por defecto '' | No | | `SESION`, `CONTRIBUYENTE`, `MATERIAL`, `CITA` o `USUARIO` |
| `id_entidad` | INT | Sí | | Id del registro afectado |
| `descripcion` | VARCHAR(255), por defecto '' | No | | Texto legible |
| `detalle` | TEXT | Sí | | Cambios campo por campo |
| `ip` | VARCHAR(45), por defecto '' | No | | Dirección IP (`getRemoteAddr()`) |

**Triggers:** `trg_auditoria_no_update` (BEFORE UPDATE) y `trg_auditoria_no_delete` (BEFORE DELETE) lanzan `SIGNAL SQLSTATE '45000'`
con el mensaje "La auditoría no se puede modificar ni eliminar".

## 3. Análisis de normalización hasta la 3FN

Criterio: **1FN** (valores atómicos, sin grupos repetidos), **2FN** (sin dependencias parciales de una parte de la clave; todas las
claves primarias son de una sola columna, así que se cumple de forma directa) y **3FN** (ningún atributo no clave depende de otro atributo
no clave).

| Tabla | Dependencias funcionales (DF) | 1FN | 2FN | 3FN | Justificación |
|---|---|---|---|---|---|
| `area` | `id_area → nombre, estado` | Sí | Sí | Sí | Solo dos atributos descriptivos; `nombre` es además clave candidata (UNIQUE) |
| `usuario` | `id_usuario → username, password_hash, nombres, rol, id_area, estado, intentos_fallidos, ultimo_acceso, debe_cambiar_password`; `username → id_usuario` (clave candidata) | Sí | Sí | Sí | El nombre del área no se repite: se guarda solo `id_area` (FK). No hay dependencias entre atributos no clave |
| `contribuyente` | `id_contribuyente → todos`; `dni_ruc → id_contribuyente` (clave candidata) | Sí | Sí | Sí | Teléfono, correo y dirección dependen solo de la persona. `fecha_nacimiento` puede ser nula (solo aplica a naturales): es opcionalidad, no una dependencia transitiva |
| `material` | `id_material → codigo, nombre, unidad, stock, stock_minimo, estado`; `codigo → id_material` | Sí | Sí | Sí | Ver la nota sobre `stock` (dato derivado) |
| `movimiento_stock` | `id_movimiento → id_material, tipo, cantidad, fecha, id_area, id_usuario, observacion` | Sí | Sí | Sí | Nombre del material, del área y del usuario no se repiten: solo las FK |
| `cita` | `id_cita → id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo, estado, hora_activa`; `(id_funcionario, fecha, hora_activa) → id_cita` (clave candidata condicionada) | Sí | Sí | Sí, con una salvedad | Ver notas `hora_activa` e `id_area` |
| `auditoria` | `id_auditoria → todos` | Sí | Sí | Sí, con una salvedad | Ver nota sobre `username` |

### Notas de normalización (decisiones y excepciones controladas)
1. **`material.stock` es un dato derivado:** en teoría es la suma de las entradas menos las salidas de `movimiento_stock`. Se guarda a
   propósito para consultar el stock sin recalcular y para poder aplicar `CHECK (stock >= 0)` y el bloqueo `FOR UPDATE`. Se mantiene
   consistente porque solo cambia dentro de la transacción de un movimiento. El reporte "resumen por material" compara `stock_actual`
   contra `saldo_historico` (suma de todo el historial).
2. **`cita.hora_activa`** es una columna generada a partir de `estado` y `hora`: depende de atributos no clave, pero la calcula el propio
   motor (no es un dato que se pueda desactualizar) y existe solo para que el `UNIQUE` ignore las citas canceladas.
3. **`cita.id_area` y `usuario.id_area`:** el funcionario ya tiene un área (`usuario.id_area`) y la cita guarda también `id_area`. La
   aplicación comprueba que el funcionario pertenezca al área elegida (`CitaValidador`), pero **la base no lo impone**. Es una
   redundancia deliberada (el área de una cita no cambia si el funcionario se mueve de área), anotada en `observaciones.md`.
4. **`auditoria.username`** duplica el nombre del usuario. Es intencional: el registro es inmutable y debe conservar quién hizo la acción
   aunque el usuario cambie o se borre (`id_usuario` pasa a NULL con `ON DELETE SET NULL`).
5. **`material.unidad`** es texto libre en la base (la lista de unidades vive en `MaterialValidador.UNIDADES`); no hay tabla de
   unidades. Cumple la 3FN, pero la base por sí sola aceptaría cualquier texto.

## 4. Decisiones de diseño
| Decisión | Detalle y motivo |
|---|---|
| **Baja lógica** | `contribuyente`, `material`, `area` y `usuario` usan el campo `estado`; no hay `DELETE` físico en el sistema. Así se conservan las citas, los movimientos y la auditoría que los referencian |
| **Bloqueo de cuenta sin columna aparte** | No existe una columna "bloqueado": la cuenta se bloquea poniendo `estado = 0` al llegar a 5 `intentos_fallidos` |
| **Columna generada `hora_activa` + `UNIQUE`** | La base misma impide dos citas no canceladas del mismo funcionario en la misma fecha y hora, incluso con usuarios simultáneos (defensa 2) |
| **Triggers de auditoría inmutable** | `BEFORE UPDATE` y `BEFORE DELETE` rechazan cualquier cambio; la aplicación solo inserta y consulta |
| **Hash de contraseñas** | PBKDF2-HMAC-SHA256, sal aleatoria de 16 bytes, 120 000 iteraciones, formato `pbkdf2$iteraciones$salBase64$hashBase64` en `VARCHAR(255)`. Los hashes antiguos (SHA-256 de 64 caracteres) se aceptan y se migran al iniciar sesión |
| **CHECK de integridad** | Documento coherente con el tipo de persona, stock y mínimo no negativos, cantidad de movimiento positiva |
| **Claves foráneas sin cascada** | Excepto `auditoria.id_usuario` (`SET NULL`), no hay borrado en cascada |
| **Índices** | Por búsqueda frecuente: `movimiento_stock (id_material, fecha)`, `cita (fecha, id_area)`, `auditoria` por fecha, acción y usuario |
| **Datos de ejemplo** | `02_tablas_sigmud.sql` incluye 6 áreas, 4 usuarios, 4 contribuyentes, 5 materiales, 5 movimientos iniciales y 3 citas de demostración |
