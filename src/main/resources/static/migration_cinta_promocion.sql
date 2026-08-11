-- Migración 2026-08-10: cinta de promociones (letrero corrido en la parte superior de la tienda).
-- cinta_promocion: catalogo simple (id, texto, activo, orden) con CRUD generico via
-- CintaPromocionController. Solo texto de muestra por ahora -- sin destino clickeable
-- (eso se agregaria despues como columnas adicionales, sin tocar esta tabla base).

CREATE TABLE IF NOT EXISTS cinta_promocion (
    id     INT          NOT NULL AUTO_INCREMENT,
    texto  VARCHAR(120) NOT NULL,
    activo TINYINT(1)   NOT NULL DEFAULT 1,
    orden  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE cinta_promocion;
