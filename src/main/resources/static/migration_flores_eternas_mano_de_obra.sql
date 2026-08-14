-- Migración 2026-08-14: accesorios "auto-incluidos" -- se cobran en TODO ramo armado sin
-- excepción, sin importar la cantidad de flores (pensado para mano de obra de armado).
-- Ver conversación con el front/dueño del negocio del 2026-08-14.

ALTER TABLE accesorio_ramo
    ADD COLUMN auto_incluido TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si esta en 1, este accesorio se agrega y cobra automatico en todo ramo armado, sin importar la cantidad de flores (a diferencia de es_papel, que depende de umbral_activacion). No es exclusivo: puede haber varios auto-incluidos activos a la vez.';

-- ============================================================
-- IMPORTANTE -- acción manual pendiente después de correr esta migración:
-- ============================================================
-- Dar de alta el accesorio "Mano de obra" (o el nombre que prefieran) en
-- Catálogo -> Accesorios, con su precio, y marcarle auto_incluido = true.
-- Mientras nadie lo dé de alta así, no cambia nada en el cobro actual.
