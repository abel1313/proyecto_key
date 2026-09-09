-- ============================================================
-- Migración: Anillos de cobro por distancia dentro de una zona
--
-- Ver DISENO_ZONAS_POR_ANILLO.md (repo compartido) para el diseño completo.
-- Cada LugarEntrega puede tener 0 (comportamiento actual, costo_envio fijo) o varios anillos
-- concéntricos, cada uno con su propio radio (metros desde el centro de la zona) y precio.
-- variante_id es la variante "sombra" que se vende como línea del pedido (mismo patrón que
-- lugares_entrega.variante_id) -- se deja sin ON DELETE CASCADE hacia variantes a propósito,
-- igual que el resto de catálogos "sombra": un pedido ya creado no debe perder su línea.
-- ============================================================

CREATE TABLE lugar_entrega_anillo (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    lugar_entrega_id  INT NOT NULL,
    radio_metros      DOUBLE NOT NULL,
    costo_envio       DOUBLE NOT NULL,
    orden             INT NULL,
    variante_id       INT NULL,
    CONSTRAINT fk_anillo_lugar_entrega FOREIGN KEY (lugar_entrega_id)
        REFERENCES lugares_entrega (id) ON DELETE CASCADE,
    CONSTRAINT fk_anillo_variante FOREIGN KEY (variante_id)
        REFERENCES variantes (id)
);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE lugar_entrega_anillo;
