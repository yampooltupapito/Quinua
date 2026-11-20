-- =====================================================================
-- SIGMUD - Instalación NUEVA de la base de datos real (mun_quinua)
-- Crea las 7 tablas (ya con la columna debe_cambiar_password y password_hash VARCHAR(255), es decir, con los
-- cambios de sql/04_usuarios.sql, y con la tabla de auditoría de sql/05_auditoria.sql) y los datos de ejemplo.
-- IMPORTANTE: contiene triggers (DELIMITER $$): ejecútelo con el usuario root.
--
-- USO: solo en una instalación nueva. Si la base mun_quinua ya existe, NO ejecute este archivo: ejecute
--      sql/04_usuarios.sql (sección 1) y sql/05_auditoria.sql (sección 1). Ejecutar como root/administrador de MySQL.
--
-- Contraseñas de ejemplo: admin = Admin123, atencion1 y atencion2 = Atencion123, almacen1 = Almacen123.
-- Los hashes de ejemplo son del formato antiguo (SHA-256 de 64 caracteres): la aplicación los acepta y los
-- convierte automáticamente a PBKDF2 con sal la primera vez que cada usuario inicia sesión.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS mun_quinua CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mun_quinua;

CREATE TABLE `area` (
  `id_area` int unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(80) NOT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_area`),
  UNIQUE KEY `uq_area_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuario` (
  `id_usuario` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `password_hash` varchar(255) NOT NULL,                       -- PBKDF2 (pbkdf2$iteraciones$sal$hash) o SHA-256 antiguo de 64 caracteres
  `nombres` varchar(100) NOT NULL,
  `rol` enum('ADMIN','ATENCION','ALMACEN') NOT NULL,
  `id_area` int unsigned DEFAULT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT '1',
  `intentos_fallidos` tinyint unsigned NOT NULL DEFAULT '0',
  `ultimo_acceso` datetime DEFAULT NULL,
  `debe_cambiar_password` tinyint(1) NOT NULL DEFAULT '0',      -- 1 = debe cambiar su contraseña al iniciar sesión
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuario_username` (`username`),
  KEY `fk_usuario_area` (`id_area`),
  CONSTRAINT `fk_usuario_area` FOREIGN KEY (`id_area`) REFERENCES `area` (`id_area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `contribuyente` (
  `id_contribuyente` int unsigned NOT NULL AUTO_INCREMENT,
  `tipo_persona` enum('NATURAL','JURIDICA') NOT NULL,
  `dni_ruc` varchar(11) NOT NULL,
  `nombres_razon_social` varchar(150) NOT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `telefono` varchar(9) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `direccion` varchar(200) DEFAULT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT '1',
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_contribuyente`),
  UNIQUE KEY `uq_contribuyente_dni_ruc` (`dni_ruc`),
  CONSTRAINT `ck_contrib_doc` CHECK ((((`tipo_persona` = _utf8mb4'NATURAL') and (char_length(`dni_ruc`) = 8)) or ((`tipo_persona` = _utf8mb4'JURIDICA') and (char_length(`dni_ruc`) = 11))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `material` (
  `id_material` int unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(15) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `unidad` varchar(20) NOT NULL DEFAULT 'UNIDAD',
  `stock` int NOT NULL DEFAULT '0',
  `stock_minimo` int NOT NULL DEFAULT '0',
  `estado` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_material`),
  UNIQUE KEY `uq_material_codigo` (`codigo`),
  CONSTRAINT `ck_material_stock` CHECK (((`stock` >= 0) and (`stock_minimo` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `movimiento_stock` (
  `id_movimiento` int unsigned NOT NULL AUTO_INCREMENT,
  `id_material` int unsigned NOT NULL,
  `tipo` enum('ENTRADA','SALIDA') NOT NULL,
  `cantidad` int NOT NULL,
  `fecha` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id_area` int unsigned DEFAULT NULL,
  `id_usuario` int unsigned NOT NULL,
  `observacion` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id_movimiento`),
  KEY `ix_mov_material_fecha` (`id_material`,`fecha`),
  KEY `fk_mov_area` (`id_area`),
  KEY `fk_mov_usuario` (`id_usuario`),
  CONSTRAINT `fk_mov_area` FOREIGN KEY (`id_area`) REFERENCES `area` (`id_area`),
  CONSTRAINT `fk_mov_material` FOREIGN KEY (`id_material`) REFERENCES `material` (`id_material`),
  CONSTRAINT `fk_mov_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`),
  CONSTRAINT `ck_mov_cantidad` CHECK ((`cantidad` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cita` (
  `id_cita` int unsigned NOT NULL AUTO_INCREMENT,
  `id_contribuyente` int unsigned NOT NULL,
  `id_area` int unsigned NOT NULL,
  `id_funcionario` int unsigned NOT NULL,
  `ventanilla` tinyint unsigned NOT NULL,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `motivo` varchar(200) DEFAULT NULL,
  `estado` enum('PROGRAMADA','ATENDIDA','CANCELADA') NOT NULL DEFAULT 'PROGRAMADA',
  `hora_activa` time GENERATED ALWAYS AS (if((`estado` = _utf8mb4'CANCELADA'),NULL,`hora`)) STORED,
  PRIMARY KEY (`id_cita`),
  UNIQUE KEY `uq_cita_funcionario_horario` (`id_funcionario`,`fecha`,`hora_activa`),
  KEY `ix_cita_fecha_area` (`fecha`,`id_area`),
  KEY `fk_cita_contrib` (`id_contribuyente`),
  KEY `fk_cita_area` (`id_area`),
  CONSTRAINT `fk_cita_area` FOREIGN KEY (`id_area`) REFERENCES `area` (`id_area`),
  CONSTRAINT `fk_cita_contrib` FOREIGN KEY (`id_contribuyente`) REFERENCES `contribuyente` (`id_contribuyente`),
  CONSTRAINT `fk_cita_func` FOREIGN KEY (`id_funcionario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Auditoría de cambios (inmutable: triggers que impiden UPDATE y DELETE). Ver sql/05_auditoria.sql.
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
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
-- Datos de prueba (los mismos de ejemplo del sistema)
-- Contraseñas: admin = Admin123, atencion1 y atencion2 = Atencion123, almacen1 = Almacen123
-- ---------------------------------------------------------------------
INSERT INTO area (id_area, nombre) VALUES
  (1, 'Mesa de Partes'), (2, 'Rentas y Tributacion'), (3, 'Desarrollo Urbano'),
  (4, 'Registro Civil'), (5, 'Logistica y Almacen'), (6, 'Gerencia Municipal');

INSERT INTO usuario (id_usuario, username, password_hash, nombres, rol, id_area) VALUES
  (1, 'admin',     '3b612c75a7b5048a435fb6ec81e52ff92d6d795a8b5a9c17070f6a63c97a53b2', 'Administrador del Sistema', 'ADMIN', 6),
  (2, 'atencion1', 'a5cb85509524f764b8b0cd39bca85832b0bfd91cdf4034a68bdf890d25a693e7', 'Funcionario de Atencion 1', 'ATENCION', 1),
  (3, 'atencion2', 'a5cb85509524f764b8b0cd39bca85832b0bfd91cdf4034a68bdf890d25a693e7', 'Funcionario de Atencion 2', 'ATENCION', 2),
  (4, 'almacen1',  '559df5fbdeb9ae47b149e5ec6917d93cebd3948fe8326dbda939595177b75f5a', 'Encargado de Almacen', 'ALMACEN', 5);

INSERT INTO contribuyente (id_contribuyente, tipo_persona, dni_ruc, nombres_razon_social, fecha_nacimiento, telefono, email, direccion) VALUES
  (1, 'NATURAL',  '70000001',    'Maria Quispe Huaman',              '1985-03-14', '966000001', 'maria.quispe@example.com', 'Jr. Principal 123, Quinua'),
  (2, 'NATURAL',  '70000002',    'Juan Ccasani Flores',              '1978-11-02', '966000002', NULL, 'Barrio San Cristobal, Quinua'),
  (3, 'NATURAL',  '70000003',    'Rosa Gutierrez Pariona',           '1992-07-21', '966000003', NULL, 'Av. Sucre 45, Quinua'),
  (4, 'JURIDICA', '20000000001', 'Asociacion de Artesanos de Quinua', NULL,        '966000004', 'artesanos@example.com', 'Plaza Principal, Quinua');

INSERT INTO material (id_material, codigo, nombre, unidad, stock, stock_minimo) VALUES
  (1, 'MAT-001', 'Papel bond A4 (paquete de 500)', 'PAQUETE', 40, 10),
  (2, 'MAT-002', 'Lapicero azul',                  'UNIDAD',  200, 50),
  (3, 'MAT-003', 'Detergente 1 kg',                'BOLSA',   25, 8),
  (4, 'MAT-004', 'Cemento (bolsa de 42.5 kg)',     'BOLSA',   60, 20),
  (5, 'MAT-005', 'Toner para impresora',           'UNIDAD',  4, 3);

INSERT INTO movimiento_stock (id_material, tipo, cantidad, id_usuario, observacion) VALUES
  (1, 'ENTRADA', 40,  4, 'Stock inicial'),
  (2, 'ENTRADA', 200, 4, 'Stock inicial'),
  (3, 'ENTRADA', 25,  4, 'Stock inicial'),
  (4, 'ENTRADA', 60,  4, 'Stock inicial'),
  (5, 'ENTRADA', 4,   4, 'Stock inicial');

INSERT INTO cita (id_contribuyente, id_area, id_funcionario, ventanilla, fecha, hora, motivo) VALUES
  (1, 1, 2, 1, '2026-11-15', '09:00:00', 'Consulta de expediente'),
  (2, 2, 3, 2, '2026-11-15', '10:30:00', 'Pago de arbitrios'),
  (3, 4, 2, 1, '2026-11-16', '11:00:00', 'Tramite de partida');


-- ---------------------------------------------------------------------
-- Usuario de la aplicación (cambie CAMBIAR_AQUI por su contraseña y use la misma en db.properties).
-- Si el usuario "sigmud" ya existe, la primera sentencia no hace nada.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'sigmud'@'localhost' IDENTIFIED BY 'CAMBIAR_AQUI';
GRANT ALL PRIVILEGES ON mun_quinua.* TO 'sigmud'@'localhost';
FLUSH PRIVILEGES;
