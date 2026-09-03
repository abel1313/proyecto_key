-- Migración 2026-09-03: tabla unificada de pagos online (Mercado Pago Checkout Pro + PayPal).
-- No confundir con mp_payment_intent (esa es solo para Point, terminal física, flujo distinto).
-- Ejecutar manualmente en la BD de cada ambiente (ddl-auto: none).
CREATE TABLE pago_online (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  proveedor VARCHAR(20) NOT NULL,
  pedido_id INT NOT NULL,
  cliente_id INT NOT NULL,
  referencia_externa VARCHAR(120) NOT NULL,
  pago_id_externo VARCHAR(120) NULL,
  monto DOUBLE NOT NULL,
  estado VARCHAR(20) NOT NULL,
  fecha_creacion DATETIME NOT NULL,
  fecha_update DATETIME NULL,
  UNIQUE KEY uq_pago_online_proveedor_referencia (proveedor, referencia_externa),
  INDEX idx_pago_online_pedido (pedido_id)
);
