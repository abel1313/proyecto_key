-- ============================================================
-- MIGRACION: Rediseno de Ventas, Pedidos y Detalles
-- Fecha: 2026-05-11
-- ============================================================

-- ------------------------------------------------------------
-- 1. pedidos: hacer cliente_id nullable
-- ------------------------------------------------------------
ALTER TABLE pedidos
    MODIFY COLUMN cliente_id INT NULL;

-- ------------------------------------------------------------
-- 2. pedidos: agregar cliente_sin_registro_id
-- ------------------------------------------------------------
ALTER TABLE pedidos
    ADD COLUMN cliente_sin_registro_id INT NULL,
    ADD CONSTRAINT pedidos_csr_fk
        FOREIGN KEY (cliente_sin_registro_id)
        REFERENCES clientes_sin_registro(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

-- ------------------------------------------------------------
-- 3. detalle_pedidos: variante_id NOT NULL
-- PRECAUCION: verificar que no haya registros con variante_id NULL antes de correr
-- SELECT COUNT(*) FROM detalle_pedidos WHERE variante_id IS NULL;
-- ------------------------------------------------------------
ALTER TABLE detalle_pedidos
    MODIFY COLUMN variante_id INT NOT NULL;

-- ------------------------------------------------------------
-- 4. detalle_pedidos: cantidad NOT NULL
-- PRECAUCION: verificar que no haya registros con cantidad NULL antes de correr
-- SELECT COUNT(*) FROM detalle_pedidos WHERE cantidad IS NULL;
-- ------------------------------------------------------------
ALTER TABLE detalle_pedidos
    MODIFY COLUMN cantidad INT NOT NULL DEFAULT 0;