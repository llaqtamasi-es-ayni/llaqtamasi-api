-- Ejecutar en el mismo esquema que usa la API.
CREATE TABLE IF NOT EXISTS chat_muro (
    publicacion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    interesado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (publicacion_id, interesado_id),
    FOREIGN KEY (publicacion_id) REFERENCES publicacion_muro(id),
    FOREIGN KEY (interesado_id) REFERENCES usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mensaje_muro (
    orden BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    publicacion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    interesado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    autor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    texto VARCHAR(2000) NOT NULL,
    enviado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX (publicacion_id, interesado_id, orden),
    FOREIGN KEY (publicacion_id, interesado_id) REFERENCES chat_muro(publicacion_id, interesado_id),
    FOREIGN KEY (autor_id) REFERENCES usuario(id)
) ENGINE=InnoDB;
