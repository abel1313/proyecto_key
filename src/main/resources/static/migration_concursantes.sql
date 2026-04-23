-- ============================================================
-- PASO 1: tabla concursantes ya existe, solo agregar descartado
-- ============================================================
ALTER TABLE concursantes
    ADD COLUMN descartado TINYINT(1) NOT NULL DEFAULT 0;

-- ============================================================
-- PASO 2: Modificar ganador_rifa
--   - Eliminar FK + columna cliente_id
--   - Agregar concursante_id con FK a concursantes
--   - Agregar columna descartado
--   IMPORTANTE: si hay filas en ganador_rifa ejecutar primero:
--   TRUNCATE TABLE ganador_rifa;
-- ============================================================
ALTER TABLE ganador_rifa
    DROP FOREIGN KEY ganador_rifa_clientes_FK,
    DROP COLUMN cliente_id,
    ADD COLUMN concursante_id INT NOT NULL AFTER id,
    ADD COLUMN descartado     TINYINT(1) NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_ganador_rifa_concursante
        FOREIGN KEY (concursante_id) REFERENCES concursantes(id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE concursantes;
-- SHOW CREATE TABLE ganador_rifa;



  ALTER TABLE concursantes
      ADD COLUMN descartado TINYINT(1) NOT NULL DEFAULT 0;

  -- 2. Modificar ganador_rifa  (truncar si tiene filas primero)
  ALTER TABLE ganador_rifa
      DROP FOREIGN KEY ganador_rifa_clientes_FK,
      DROP COLUMN cliente_id,
      ADD COLUMN concursante_id INT NOT NULL AFTER id,
      ADD COLUMN descartado     TINYINT(1) NOT NULL DEFAULT 0,
      ADD CONSTRAINT fk_ganador_rifa_concursante
          FOREIGN KEY (concursante_id) REFERENCES concursantes(id);

