-- Migración 2026-07-24: catálogo de lugares de entrega + datos de contacto del pedido.
-- lugares_entrega: catálogo simple (id, nombre) con CRUD generico via LugarEntregaController,
-- para seleccionar/filtrar por zona de entrega (ej. "Zacazonapan") en vez de texto libre.
-- pedidos.lugar_entrega_id: FK al catalogo, editable igual que nombre_receptor/direccion_entrega.
-- pedidos.url_facebook: link al perfil de quien hizo ESE pedido especifico -- se guarda por
-- pedido y no en Cliente/ClienteSinRegistro porque quien recibe/compra puede variar entre pedidos.

CREATE TABLE IF NOT EXISTS lugares_entrega (
    id     INT          NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lugares_entrega_nombre (nombre)
);

ALTER TABLE pedidos
    ADD COLUMN lugar_entrega_id INT NULL,
    ADD COLUMN url_facebook     VARCHAR(300) NULL,
    ADD CONSTRAINT fk_pedidos_lugar_entrega
        FOREIGN KEY (lugar_entrega_id) REFERENCES lugares_entrega (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE lugares_entrega;
-- SHOW CREATE TABLE pedidos;
