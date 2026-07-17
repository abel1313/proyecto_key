-- ============================================================
-- MODULO: Favoritos + Reseñas (pagina publica)
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base segun el ambiente:
-- use inventario_key_qa;   <- dev y qa (misma BD)
-- use inventario_key;      <- main / prod
--
-- Contrato completo en CAMBIOS_FRONT.md.
-- Estado por ambiente: back implementado en dev (2026-07-13) — pendiente correr en dev/qa/prod.
-- ============================================================

CREATE TABLE IF NOT EXISTS favorito (
    id              INT NOT NULL AUTO_INCREMENT,
    cliente_id      INT NOT NULL,
    variante_id     INT NOT NULL,
    fecha_agregado  DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorito_cliente_variante (cliente_id, variante_id),
    CONSTRAINT fk_favorito_cliente  FOREIGN KEY (cliente_id)  REFERENCES clientes (id)  ON DELETE CASCADE,
    CONSTRAINT fk_favorito_variante FOREIGN KEY (variante_id) REFERENCES variantes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resena (
    id                INT NOT NULL AUTO_INCREMENT,
    cliente_id        INT NOT NULL,
    variante_id       INT NOT NULL,
    calificacion      INT NOT NULL COMMENT '1 a 5',
    comentario        TEXT NULL,
    fecha_creacion    DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resena_cliente_variante (cliente_id, variante_id),
    CONSTRAINT fk_resena_cliente  FOREIGN KEY (cliente_id)  REFERENCES clientes (id)  ON DELETE CASCADE,
    CONSTRAINT fk_resena_variante FOREIGN KEY (variante_id) REFERENCES variantes (id) ON DELETE CASCADE,
    CONSTRAINT chk_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);
