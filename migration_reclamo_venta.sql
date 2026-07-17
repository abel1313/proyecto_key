-- ============================================================
-- Migración: Reclamo de venta por el cliente (para rifas)
--
-- Caso: cliente compra en mostrador pero la venta queda con datos de
-- ClienteSinRegistro (no con su cuenta real). Se le envía un código UUID
-- por correo; al loguearse en la app y capturarlo, la venta (y el pedido
-- que la respalda) se vincula a su Cliente real, para que aparezca en
-- /concursante/clientesPorMes al armar la rifa.
-- ============================================================

ALTER TABLE ventas
    ADD COLUMN codigo_reclamo VARCHAR(36)  NULL,
    ADD COLUMN correo_reclamo VARCHAR(150) NULL,
    ADD COLUMN reclamado_en  DATETIME      NULL,
    ADD CONSTRAINT uq_ventas_codigo_reclamo UNIQUE (codigo_reclamo);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE ventas;
