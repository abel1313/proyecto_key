-- ============================================================
-- Migración: ventana de gracia para el refresh token (2026-10-14)
--
-- Por qué: el back rota el refresh token en cada renovación y, si llega uno ya rotado, cierra la
-- sesión completa (se toma por robo). Pero dos renovaciones legítimas casi a la vez (dos pestañas,
-- o el arranque de la página junto con una petición cuyo access token venció) mandan la MISMA
-- cookie: la segunda llega "ya rotada" y sacaba al usuario de la sesión sin que hiciera nada.
--
-- Qué hace: agrega 2 columnas a sesion_refresh para recordar el jti anterior y cuándo se rotó.
-- El back acepta ese jti anterior durante 20 segundos.
--
-- ⚠️ CORRER ANTES de desplegar el back que usa estas columnas (ddl-auto está en none: sin ellas,
-- el login y el refresh fallan por "Unknown column").
-- Idempotente: se puede correr dos veces. Cubre dev y qa (misma base: inventario_key_qa).
-- ============================================================

SET NAMES utf8mb4;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sesion_refresh' AND COLUMN_NAME = 'jti_anterior');
SET @sql := IF(@existe = 0, 'ALTER TABLE sesion_refresh ADD COLUMN jti_anterior VARCHAR(36) NULL', 'SELECT 1');
PREPARE s1 FROM @sql; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sesion_refresh' AND COLUMN_NAME = 'rotado_en');
SET @sql := IF(@existe = 0, 'ALTER TABLE sesion_refresh ADD COLUMN rotado_en DATETIME NULL', 'SELECT 1');
PREPARE s2 FROM @sql; EXECUTE s2; DEALLOCATE PREPARE s2;

-- Verificación: deben salir 2 filas.
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sesion_refresh' AND COLUMN_NAME IN ('jti_anterior','rotado_en');
