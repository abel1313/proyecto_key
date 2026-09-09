-- Migración 2026-09-04: distingue en el catálogo lugares_entrega cuál fila representa
-- "recoger en el local" de las zonas de entrega reales (Tejupilco, Zacazonapan, etc.).
--
-- Después de correr esto, en el admin (Lugares de entrega) marca el checkbox "Es recoger en
-- tienda" en la fila que corresponda (o crea una nueva fila, ej. "Recoger en tienda", y márcala).
-- Debe haber como mucho UNA fila marcada -- no se valida en BD, es responsabilidad de quien
-- administra el catálogo.

ALTER TABLE lugares_entrega
    ADD COLUMN es_recoger_en_tienda TINYINT(1) NOT NULL DEFAULT 0;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SHOW CREATE TABLE lugares_entrega;
-- SELECT id, nombre, es_recoger_en_tienda FROM lugares_entrega;
