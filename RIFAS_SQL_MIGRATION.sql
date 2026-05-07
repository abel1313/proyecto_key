-- =====================================================
-- Migración: Rifas con múltiples productos y giro configurable
-- Ejecutar en orden en la BD
-- =====================================================

-- 1. Nueva tabla de productos por rifa
CREATE TABLE configurar_rifa_producto (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    configurar_rifa_id INT NOT NULL,
    producto_id     INT NOT NULL,
    orden           INT NOT NULL,
    giro_ganador    INT NOT NULL DEFAULT 1,
    permitir_nuevos TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (configurar_rifa_id) REFERENCES configurar_rifa(id),
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- 2. Migrar el producto existente de cada rifa a la nueva tabla
INSERT INTO configurar_rifa_producto (configurar_rifa_id, producto_id, orden, giro_ganador, permitir_nuevos)
SELECT id, producto_id, 1, 1, 0
FROM configurar_rifa
WHERE producto_id IS NOT NULL;

-- 3. Quitar producto_id de configurar_rifa (ya no se usa)
ALTER TABLE configurar_rifa DROP FOREIGN KEY configurar_rifa_ibfk_1;
ALTER TABLE configurar_rifa DROP COLUMN producto_id;

-- 4. Agregar orden_desde a concursantes (de qué producto en adelante participa)
ALTER TABLE concursantes ADD COLUMN orden_desde INT NOT NULL DEFAULT 1;

-- 5. Actualizar ganador_rifa para apuntar a configurar_rifa_producto en lugar de producto
ALTER TABLE ganador_rifa ADD COLUMN configurar_rifa_producto_id INT;

-- Migrar datos existentes de ganador_rifa
UPDATE ganador_rifa gr
JOIN concursantes c ON gr.concursante_id = c.id
JOIN configurar_rifa_producto crp ON crp.configurar_rifa_id = c.configurar_rifa_id AND crp.orden = 1
SET gr.configurar_rifa_producto_id = crp.id;

-- Hacer el campo NOT NULL y agregar FK (solo después de confirmar que todos los registros migrados)
ALTER TABLE ganador_rifa MODIFY COLUMN configurar_rifa_producto_id INT NOT NULL;
ALTER TABLE ganador_rifa ADD FOREIGN KEY (configurar_rifa_producto_id) REFERENCES configurar_rifa_producto(id);

-- Quitar producto_id de ganador_rifa (ya no se usa)
ALTER TABLE ganador_rifa DROP FOREIGN KEY ganador_rifa_ibfk_2;
ALTER TABLE ganador_rifa DROP COLUMN producto_id;