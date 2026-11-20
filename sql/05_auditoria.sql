-- =====================================================================
-- SIGMUD - Auditoría de cambios: tabla "auditoria" (quién hizo qué y cuándo)
--
-- EJECUTAR CON EL USUARIO root (o un administrador de MySQL con permiso TRIGGER), por ejemplo en MySQL Workbench
-- o con:  mysql -u root -p < sql/05_auditoria.sql
-- Antes de desplegar la versión nueva de la aplicación (la aplicación escribe en esta tabla).
--
-- ORDEN DE EJECUCIÓN:
--   * Instalación existente:  ejecutar la SECCIÓN 1 (mun_quinua).
--   * Base de pruebas:        si la creó con el sql/03_bd_pruebas.sql NUEVO, ya tiene la tabla: no ejecute la sección 2.
--                             Si la creó con el 03 ANTERIOR, ejecute la SECCIÓN 2 (o vuelva a ejecutar el 03 nuevo).
--   * Instalación nueva:      use sql/02_tablas_sigmud.sql (ya incluye esta tabla); este archivo no hace falta.
-- Es seguro ejecutarlo dos veces (CREATE TABLE IF NOT EXISTS y triggers recreados).
--
-- El registro es INMUTABLE: los triggers impiden UPDATE y DELETE (SQLSTATE 45000).
-- (Solo un administrador de la base de datos podría quitar los triggers; la aplicación no tiene ese permiso en la pantalla.)
-- El script usa DELIMITER $$ para que funcione en MySQL Workbench y en el cliente de línea de comandos.
-- =====================================================================

-- ---------------------------------------------------------------------
-- SECCIÓN 1: base real  mun_quinua
-- ---------------------------------------------------------------------
USE mun_quinua;

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGINT NOT NULL AUTO_INCREMENT,
    fecha_hora   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    id_usuario   INT UNSIGNED NULL,                 -- NULL: acción sin sesión (por ejemplo un intento de login fallido)
    username     VARCHAR(40) NOT NULL DEFAULT '',   -- copia del nombre de usuario en ese momento
    accion       VARCHAR(30) NOT NULL,
    entidad      VARCHAR(30) NOT NULL DEFAULT '',
    id_entidad   INT NULL,
    descripcion  VARCHAR(255) NOT NULL DEFAULT '',
    detalle      TEXT NULL,                         -- cambios campo por campo
    ip           VARCHAR(45) NOT NULL DEFAULT '',
    PRIMARY KEY (id_auditoria),
    KEY ix_auditoria_fecha (fecha_hora),
    KEY ix_auditoria_accion (accion),
    KEY ix_auditoria_usuario (id_usuario),
    -- Si algún día se borrara un usuario con SQL directo, sus registros conservan el nombre (username) y el id queda NULL.
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TRIGGER IF EXISTS trg_auditoria_no_update;
DROP TRIGGER IF EXISTS trg_auditoria_no_delete;

DELIMITER $$
CREATE TRIGGER trg_auditoria_no_update BEFORE UPDATE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar ni eliminar';
END$$

CREATE TRIGGER trg_auditoria_no_delete BEFORE DELETE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar ni eliminar';
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- SECCIÓN 2: base de pruebas  mun_quinua_test  (solo si se creó con el 03 anterior)
-- ---------------------------------------------------------------------
USE mun_quinua_test;

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGINT NOT NULL AUTO_INCREMENT,
    fecha_hora   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    id_usuario   INT UNSIGNED NULL,
    username     VARCHAR(40) NOT NULL DEFAULT '',
    accion       VARCHAR(30) NOT NULL,
    entidad      VARCHAR(30) NOT NULL DEFAULT '',
    id_entidad   INT NULL,
    descripcion  VARCHAR(255) NOT NULL DEFAULT '',
    detalle      TEXT NULL,
    ip           VARCHAR(45) NOT NULL DEFAULT '',
    PRIMARY KEY (id_auditoria),
    KEY ix_auditoria_fecha (fecha_hora),
    KEY ix_auditoria_accion (accion),
    KEY ix_auditoria_usuario (id_usuario),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TRIGGER IF EXISTS trg_auditoria_no_update;
DROP TRIGGER IF EXISTS trg_auditoria_no_delete;

DELIMITER $$
CREATE TRIGGER trg_auditoria_no_update BEFORE UPDATE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar ni eliminar';
END$$

CREATE TRIGGER trg_auditoria_no_delete BEFORE DELETE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar ni eliminar';
END$$
DELIMITER ;
