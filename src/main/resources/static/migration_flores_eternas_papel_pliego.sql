-- Migración 2026-08-13 (parte 4): precio del papel por pliego (escala con la cantidad de flores)
-- en vez de un precio fijo único al superar el umbral.
-- Ver conversación con el front/dueño del negocio del 2026-08-13.

ALTER TABLE accesorio_ramo
    ADD COLUMN flores_por_pliego INT NULL COMMENT 'Solo aplica al accesorio marcado es_papel=true: cuantas flores cubre 1 pliego. Si esta configurado, "precio" pasa a interpretarse como precio por pliego (antes era precio fijo unico). NULL = se mantiene el precio fijo anterior, retrocompatible.';

-- ============================================================
-- IMPORTANTE -- accion manual pendiente despues de correr esta migracion:
-- ============================================================
-- 1. Editar el accesorio marcado "es_papel=true" y configurarle "flores_por_pliego".
-- 2. Los RamoArmado ya guardados tienen precio_papel/precio_total CONGELADOS con la formula
--    vieja (precio fijo). Hay que editarlos y volver a guardarlos (aunque sea sin cambios) para
--    que se recalculen con la formula nueva (pliegos_necesarios x precio_por_pliego).
--
-- SHOW CREATE TABLE accesorio_ramo;
