-- Ejecutar UNA VEZ sobre la version original de 17 tablas.
-- Las cuentas existentes quedan inactivas hasta configurar sus credenciales.
-- El backend debe generar el hash y habilitar la cuenta tras completar el registro.
USE llaqtamasi_ayni;
ALTER TABLE usuario
    ADD COLUMN nombre_acceso VARCHAR(100) NULL,
    ADD COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD UNIQUE KEY uq_usuario_nombre_acceso (nombre_acceso),
    ADD CONSTRAINT ck_usuario_acceso CHECK (
        activo = FALSE OR (nombre_acceso IS NOT NULL AND CHAR_LENGTH(TRIM(nombre_acceso)) > 0
        AND password_hash IS NOT NULL AND CHAR_LENGTH(TRIM(password_hash)) > 0)
    );

CREATE TABLE rol_sistema (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rol_sistema_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE usuario_rol_sistema (
    usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rol_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    asignado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_rol_sistema_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_rol_sistema_rol FOREIGN KEY (rol_id) REFERENCES rol_sistema (id)
) ENGINE=InnoDB;
