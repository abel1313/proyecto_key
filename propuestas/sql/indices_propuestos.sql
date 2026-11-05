-- PROPUESTA — NO ejecutar sin medir. Probar primero en inventario_key_qa y comparar con EXPLAIN antes y después.
-- Hoy las tablas son chicas (pedidos del orden de miles) y MySQL ya indexa las llaves foráneas; la ganancia es pequeña ahora y
-- crece con el volumen. Ver INDICES_Y_N1_2026-11-05.md.
-- MySQL no tiene CREATE INDEX IF NOT EXISTS: este bloque revisa information_schema antes de crear cada índice (se puede correr dos veces).

DROP PROCEDURE IF EXISTS crear_indice_si_falta;
DELIMITER //
CREATE PROCEDURE crear_indice_si_falta(IN tabla VARCHAR(64), IN indice VARCHAR(64), IN columnas VARCHAR(200))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = tabla AND index_name = indice) THEN
    SET @s = CONCAT('CREATE INDEX ', indice, ' ON ', tabla, ' (', columnas, ')');
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END//
DELIMITER ;

CALL crear_indice_si_falta('pedidos', 'idx_pedidos_fecha_pedido', 'fecha_pedido');
CALL crear_indice_si_falta('pedidos', 'idx_pedidos_entregado_estado', 'entregado, estado_pedido');
CALL crear_indice_si_falta('pedidos', 'idx_pedidos_lugar_fecha', 'lugar_entrega_id, fecha_pedido');
CALL crear_indice_si_falta('abono_pedido', 'idx_abono_pedido_fecha_pago', 'fecha_pago');

DROP PROCEDURE crear_indice_si_falta;

-- Para medir antes y después (en QA):
--   EXPLAIN SELECT * FROM pedidos WHERE entregado = 0 AND fecha_pedido BETWEEN '2026-11-02' AND '2026-11-08';
--   SET GLOBAL slow_query_log = 1; SET GLOBAL long_query_time = 0.5;   -- y revisar el archivo de consultas lentas
