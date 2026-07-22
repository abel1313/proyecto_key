-- ============================================================
-- MÓDULO: Limpieza automática de clientes sin registro huérfanos
-- ============================================================
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente:
--   dev  → ✅ ya corrido (2026-07-22) — comparte BD inventario_key_qa con qa
--   qa   → ✅ ya corrido (2026-07-22) — misma BD que dev
--   prod → ⏳ pendiente — correr en inventario_key cuando se suba a main
-- ============================================================

-- Fecha de creación del registro. Sirve para que el job de limpieza (medianoche) solo borre
-- huerfanos con varias horas de antigüedad, y nunca toque una venta que se esté capturando
-- justo en ese momento (ver ClienteSinRegistroLimpiezaScheduler).
ALTER TABLE clientes_sin_registro
    ADD COLUMN creado_en DATETIME NULL
        COMMENT 'Fecha de creacion del registro, usada por el job de limpieza de huerfanos';

-- Filas existentes (creadas antes de esta migracion) no tienen fecha -- se les asigna la fecha
-- actual para que no se consideren "recien creadas" ni caigan fuera del filtro por error.
UPDATE clientes_sin_registro SET creado_en = NOW() WHERE creado_en IS NULL;
