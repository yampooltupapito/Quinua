-- =====================================================================
-- SIGMUD - Gestión de usuarios: cambios en la tabla usuario
--   (a) columna debe_cambiar_password (1 = el usuario debe cambiar su contraseña al iniciar sesión)
--   (b) password_hash pasa de CHAR(64) a VARCHAR(255) (cabe el formato nuevo pbkdf2$iteraciones$sal$hash)
--
-- ORDEN DE EJECUCIÓN (importante: ANTES de desplegar la nueva versión de la aplicación):
--   * Instalación existente:  ejecutar SOLO la SECCIÓN 1 (mun_quinua).
--   * Base de pruebas:        si la creó con el sql/03_bd_pruebas.sql NUEVO, ya tiene los cambios: no ejecute la sección 2.
--                             Si la creó con el 03 ANTERIOR, ejecute la SECCIÓN 2 (o vuelva a ejecutar el 03 nuevo).
--   * Instalación nueva:      use sql/02_tablas_sigmud.sql (ya incluye estos cambios); este archivo no hace falta.
-- Es seguro ejecutarlo dos veces: si la columna ya existe no hace nada.
-- Se ejecuta como root/administrador (por ejemplo en MySQL Workbench).
-- =====================================================================

-- ---------------------------------------------------------------------
-- SECCIÓN 1: base real  mun_quinua
-- ---------------------------------------------------------------------
USE mun_quinua;

-- (a) ALTER TABLE usuario ADD COLUMN debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0;  (solo si todavía no existe)
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'mun_quinua' AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'debe_cambiar_password');
SET @sentencia := IF(@existe = 0,
    'ALTER TABLE usuario ADD COLUMN debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT ''La columna debe_cambiar_password ya existe en mun_quinua'' AS aviso');
PREPARE paso FROM @sentencia; EXECUTE paso; DEALLOCATE PREPARE paso;

-- (b) los hashes actuales (64 caracteres) se conservan; el cambio solo amplía el tamaño
ALTER TABLE usuario MODIFY password_hash VARCHAR(255) NOT NULL;

-- ---------------------------------------------------------------------
-- SECCIÓN 2: base de pruebas  mun_quinua_test  (solo si se creó con el 03 anterior)
-- ---------------------------------------------------------------------
USE mun_quinua_test;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'mun_quinua_test' AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'debe_cambiar_password');
SET @sentencia := IF(@existe = 0,
    'ALTER TABLE usuario ADD COLUMN debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT ''La columna debe_cambiar_password ya existe en mun_quinua_test'' AS aviso');
PREPARE paso FROM @sentencia; EXECUTE paso; DEALLOCATE PREPARE paso;

ALTER TABLE usuario MODIFY password_hash VARCHAR(255) NOT NULL;
