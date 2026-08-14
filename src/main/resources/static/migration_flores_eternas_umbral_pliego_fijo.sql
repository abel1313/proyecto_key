-- Migración 2026-08-14: umbral configurable para forzar 1 pliego fijo en ventas chicas/sueltas
-- (ej. de 1 a 5 flores), sin importar floresPorPliego ni el pliegos explícito del ramo.
-- Ver conversación con el front/dueño del negocio del 2026-08-14.

ALTER TABLE accesorio_ramo
    ADD COLUMN umbral_pliego_fijo INT NULL COMMENT 'Solo aplica al accesorio marcado es_papel=true: hasta esta cantidad de flores (inclusive), si el cliente elige papel se cobra fijo 1 pliego, ganandole a floresPorPliego y al pliegos explicito de CantidadFlorValida. NULL = esta regla nunca se dispara.';

-- ============================================================
-- IMPORTANTE -- acción manual pendiente después de correr esta migración:
-- ============================================================
-- Editar el accesorio marcado "es_papel=true" y configurarle "umbral_pliego_fijo" (ej. 5, para
-- que de 1 a 5 flores siempre sea 1 pliego). Mientras nadie lo configure, no cambia nada.
