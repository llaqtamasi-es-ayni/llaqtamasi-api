-- Llaqtamasi es Ayni | MySQL 8.0.16 o posterior | InnoDB
-- Enviar fechas en UTC y UUID en minusculas desde la aplicacion.
-- Generar los UUID de acciones offline en el cliente.
-- Crear el cambio de negocio y su registro en accion_sync en una sola transaccion.
CREATE DATABASE IF NOT EXISTS llaqtamasi_ayni CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE llaqtamasi_ayni;

CREATE TABLE `usuario` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `alias` VARCHAR(150) NOT NULL,
    `nombre_acceso` VARCHAR(100) NULL,
    `password_hash` VARCHAR(255) NULL,
    `activo` BOOLEAN NOT NULL DEFAULT FALSE,

    `verificado` BOOLEAN NOT NULL DEFAULT FALSE,
    `nivel_confianza` DECIMAL(10,4) NOT NULL DEFAULT 0,
    `registrado_en` DATETIME(6) NOT NULL,
    UNIQUE KEY uq_usuario_nombre_acceso (nombre_acceso),
    CONSTRAINT ck_usuario_acceso CHECK (
        activo = FALSE OR (nombre_acceso IS NOT NULL AND CHAR_LENGTH(TRIM(nombre_acceso)) > 0
        AND password_hash IS NOT NULL AND CHAR_LENGTH(TRIM(password_hash)) > 0)
    ),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `categoria_trabajo` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `nombre` VARCHAR(150) NOT NULL,
    `descripcion` TEXT NULL,
    `activa` BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`id`),
    UNIQUE KEY uq_categoria_trabajo_nombre (`nombre`)
) ENGINE=InnoDB;

CREATE TABLE `perfil` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `usuario_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `categoria_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `tipo` VARCHAR(150) NOT NULL,
    `descripcion` TEXT NOT NULL,
    `area_atencion` MULTIPOLYGON NOT NULL SRID 4326,
    `visible` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_perfil_usuario_id FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_perfil_categoria_id FOREIGN KEY (`categoria_id`) REFERENCES `categoria_trabajo` (`id`),
    CONSTRAINT ck_perfil_1 CHECK (tipo IN ('SERVICIO', 'TIENDA', 'APOYO'))
) ENGINE=InnoDB;

CREATE TABLE `rol_comunitario` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `nombre` VARCHAR(150) NOT NULL,
    `descripcion` TEXT NULL,
    `activo` BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`id`),
    UNIQUE KEY uq_rol_comunitario_nombre (`nombre`)
) ENGINE=InnoDB;

CREATE TABLE `usuario_rol_comunitario` (
    `usuario_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `rol_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `asignado_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`usuario_id`, `rol_id`),
    CONSTRAINT fk_usuario_rol_comunitario_usuario_id FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_usuario_rol_comunitario_rol_id FOREIGN KEY (`rol_id`) REFERENCES `rol_comunitario` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `aval_vecinal` (
    `avalista_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `avalado_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `creado_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`avalista_id`, `avalado_id`),
    CONSTRAINT fk_aval_vecinal_avalista_id FOREIGN KEY (`avalista_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_aval_vecinal_avalado_id FOREIGN KEY (`avalado_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT ck_aval_vecinal_1 CHECK (avalista_id <> avalado_id)
) ENGINE=InnoDB;

CREATE TABLE `voto_perfil` (
    `autor_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `perfil_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `valor` SMALLINT NOT NULL,
    `creado_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`autor_id`, `perfil_id`),
    CONSTRAINT fk_voto_perfil_autor_id FOREIGN KEY (`autor_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_voto_perfil_perfil_id FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id`),
    CONSTRAINT ck_voto_perfil_1 CHECK (valor BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE `solicitud` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `vecino_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `perfil_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `mensaje_inicial` TEXT NOT NULL,
    `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    `creada_en` DATETIME(6) NOT NULL,
    `finalizada_en` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_solicitud_vecino_id FOREIGN KEY (`vecino_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_solicitud_perfil_id FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id`),
    KEY ix_solicitud_estado_finalizada (estado, finalizada_en),
    CONSTRAINT ck_solicitud_1 CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'FINALIZADA')),
    CONSTRAINT ck_solicitud_2 CHECK ((estado = 'FINALIZADA') = (finalizada_en IS NOT NULL)),
    CONSTRAINT ck_solicitud_3 CHECK (finalizada_en IS NULL OR finalizada_en >= creada_en)
) ENGINE=InnoDB;

CREATE TABLE `mensaje` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `solicitud_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `autor_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `texto` TEXT NOT NULL,
    `enviado_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_mensaje_solicitud_id FOREIGN KEY (`solicitud_id`) REFERENCES `solicitud` (`id`),
    CONSTRAINT fk_mensaje_autor_id FOREIGN KEY (`autor_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `calificacion_vecino` (
    `solicitud_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `valor` SMALLINT NOT NULL,
    `creada_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`solicitud_id`),
    CONSTRAINT fk_calificacion_vecino_solicitud_id FOREIGN KEY (`solicitud_id`) REFERENCES `solicitud` (`id`),
    CONSTRAINT ck_calificacion_vecino_1 CHECK (valor BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE `zona` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `nombre` VARCHAR(150) NOT NULL,
    `limite` MULTIPOLYGON NOT NULL SRID 4326,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `publicacion_muro` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `autor_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `zona_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `tipo` VARCHAR(150) NOT NULL,
    `categoria` VARCHAR(150) NOT NULL,
    `texto` TEXT NOT NULL,
    `estado` VARCHAR(150) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_publicacion_muro_autor_id FOREIGN KEY (`autor_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT fk_publicacion_muro_zona_id FOREIGN KEY (`zona_id`) REFERENCES `zona` (`id`),
    CONSTRAINT ck_publicacion_muro_1 CHECK (tipo IN ('OFREZCO', 'NECESITO'))
) ENGINE=InnoDB;

CREATE TABLE `reporte_publico` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `autor_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `tipo` VARCHAR(150) NOT NULL,
    `ubicacion` POINT NOT NULL SRID 4326,
    `texto` TEXT NOT NULL,
    `creado_en` DATETIME(6) NOT NULL,
    `expira_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_reporte_publico_autor_id FOREIGN KEY (`autor_id`) REFERENCES `usuario` (`id`),
    KEY ix_reporte_expira (expira_en),
    CONSTRAINT ck_reporte_publico_1 CHECK (expira_en > creado_en)
) ENGINE=InnoDB;

CREATE TABLE `respuesta_reporte` (
    `reporte_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `usuario_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `decision` VARCHAR(150) NOT NULL,
    `actualizada_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`reporte_id`, `usuario_id`),
    CONSTRAINT fk_respuesta_reporte_reporte_id FOREIGN KEY (`reporte_id`) REFERENCES `reporte_publico` (`id`),
    CONSTRAINT fk_respuesta_reporte_usuario_id FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`),
    CONSTRAINT ck_respuesta_reporte_1 CHECK (decision IN ('CONFIRMA', 'DESMIENTE'))
) ENGINE=InnoDB;

CREATE TABLE `centro` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `zona_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `nombre` VARCHAR(150) NOT NULL,
    `tipo` VARCHAR(150) NOT NULL,
    `ubicacion` POINT NOT NULL SRID 4326,
    `horario` VARCHAR(150) NULL,
    `contacto_publico` VARCHAR(150) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_centro_zona_id FOREIGN KEY (`zona_id`) REFERENCES `zona` (`id`),
    CONSTRAINT ck_centro_1 CHECK (tipo IN ('COMEDOR', 'OLLA_COMUN', 'ADULTO_MAYOR', 'NINOS', 'SALUD', 'COMISARIA', 'ACOPIO'))
) ENGINE=InnoDB;

CREATE TABLE `paquete_mapa` (
    `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `zona_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `version` INTEGER NOT NULL,
    `referencia_archivo` TEXT NOT NULL,
    `generado_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT fk_paquete_mapa_zona_id FOREIGN KEY (`zona_id`) REFERENCES `zona` (`id`),
    UNIQUE KEY uq_paquete_zona_version (zona_id, version),
    CONSTRAINT ck_paquete_mapa_1 CHECK (version >= 1)
) ENGINE=InnoDB;

CREATE TABLE `accion_sync` (
    `id_accion` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `usuario_id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `aplicada_en` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id_accion`),
    CONSTRAINT fk_accion_sync_usuario_id FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB;

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
