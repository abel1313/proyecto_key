-- ============================================================
-- configurar_rifa.publica -- cuál rifa sirve el link público /ruleta/{id}
--
-- Por qué: el id de la rifa va en la URL del link que el negocio comparte, así que es
-- adivinable. Con /ruleta/48 a la vista bastaba cambiar el número a 47 para entrar a la
-- rifa de otro mes y ver sus participantes. Ahora el negocio marca a mano cuál es la que
-- todos pueden ver; el resto responde 404.
--
-- No basta con `activa`: puede haber varias rifas activas a la vez (se arma la del mes que
-- entra mientras corre la de este mes), pero publicada hay una sola.
--
-- ⚠️ Después de correr esto NINGUNA rifa queda publicada (DEFAULT 0). Es a propósito --
-- se prefiere que el link no abra a que abra la rifa equivocada. Hay que entrar a
-- Rifas → Boletos, elegir la rifa y darle "📢 Publicar esta rifa".
--
-- Idempotente: se puede correr varias veces sin error.
-- Correr en: inventario_key_qa (dev/qa) y en inventario_key (main/prod).
-- ============================================================

SET @dbname = DATABASE();

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = @dbname AND table_name = 'configurar_rifa' AND column_name = 'publica');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE configurar_rifa ADD COLUMN publica TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''La rifa que sirve el link publico /ruleta/{id}. Solo una a la vez''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- OPCIONAL -- publicar de una vez la rifa que ya se estaba compartiendo.
-- Descomenta y pon el id real para no tener que entrar a la pantalla:
--
--   UPDATE configurar_rifa SET publica = 0 WHERE publica = 1;
--   UPDATE configurar_rifa SET publica = 1 WHERE id = 48;
-- ============================================================

-- ============================================================
-- VERIFICACIÓN -- debe haber como mucho una fila con publica = 1
-- ============================================================
-- SELECT id, tipo, activa, publica, fecha_inicio_boletos, fecha_fin_boletos
--   FROM configurar_rifa ORDER BY id DESC LIMIT 10;
