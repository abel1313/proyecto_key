-- ============================================================
-- MÓDULO: Habilitar/deshabilitar variantes individualmente
-- ============================================================
-- Antes solo el producto tenia columna "habilitado"; las variantes heredaban
-- la visibilidad del producto padre. Con esta columna se puede ocultar una
-- variante especifica (ej. una talla/color de prueba) sin afectar al resto
-- del producto.
--
-- Conectarse al servidor
-- sudo mysql -u root -p

-- Seleccionar la base según el ambiente:
-- use inventario_key_qa;   ← dev y qa (misma BD)
-- use inventario_key;      ← main / prod
--
-- Estado por ambiente:
--   dev  → ✅ la columna ya existia (char(1) NOT NULL DEFAULT '1') — comparte BD con qa
--   qa   → ✅ la columna ya existia — misma BD que dev, confirmado con DESCRIBE variantes (2026-07-03)
--   prod → ⏳ pendiente — correr en inventario_key cuando se suba a main (si ya existe ahi tambien,
--          el ALTER va a fallar con error 1060 "Duplicate column name" — eso significa que no hace
--          falta correrlo, no es un error real).
-- ============================================================

ALTER TABLE variantes
    ADD COLUMN habilitado CHAR(1) NOT NULL DEFAULT '1'
        COMMENT '1 = visible para clientes, 0 = oculta (independiente del producto padre)';
