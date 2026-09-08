-- ============================================================
-- Migración: hora y punto de encuentro visibles en el pedido del cliente (2026-09-08)
--
-- Pedido explícito del usuario: cuando se programa una entrega de zona (pantalla "Entregas por
-- zona"), hoy la fecha se guarda en el pedido (fecha_recogida) pero la HORA y el PUNTO DE
-- ENCUENTRO solo se mandan en el correo de aviso y se pierden -- el cliente no los puede volver
-- a consultar en su pedido. Se agregan las 2 columnas para que EntregaZonaServiceImpl las guarde
-- también (ver cambio en programarEntrega) y el detalle del pedido las muestre.
--
-- No aplica a pedidos de ramo de flores eternas -- esos usan su propio campo
-- RamoPedidoDetalle.fecha_hora_entrega (ya existía, no se toca aquí -- el fix de exponerlo al
-- cliente es solo del lado del DTO/response, sin cambio de esquema).
-- ============================================================

ALTER TABLE pedidos
  ADD COLUMN hora_recogida VARCHAR(10) NULL,
  ADD COLUMN punto_encuentro VARCHAR(255) NULL;
