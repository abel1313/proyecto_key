-- ============================================================
-- Migración: cobro con tarjeta (terminal y en línea) — tablas de intentos y devoluciones, casilla de MSI
-- Fecha: 2026-10-21 · Rama feature/pago-tarjeta · Plan: PLAN_PAGO_TARJETA.md · Dominio: hexagonal/pagotarjeta
--
-- Qué hace:
--   1. Crea pago_tarjeta_intento (entidad PagoTarjetaIntento): un renglón por cada intento de cobro con
--      tarjeta del dominio nuevo. NO toca mp_payment_intent, que sigue siendo del flujo viejo de la terminal.
--      Lleva DOS candados que defienden el dinero aunque dos peticiones lleguen a la vez:
--        * version                → candado optimista (@Version): dos procesos que cambian el mismo cobro
--                                   a la vez no se pisan; el segundo falla.
--        * uq_pti_activo          → índice ÚNICO sobre una columna generada: un pedido NO puede tener dos
--                                   cobros activos (CREADO o ENVIADO) al mismo tiempo (regla T1).
--      Más: id_externo único, montos con validación y fechas DATETIME(3) en UTC.
--   2. Crea pago_tarjeta_devolucion: una fila por cada devolución (quién, cuánto, cuándo y su estado
--      PENDIENTE/CONFIRMADA: se reserva antes de hablar con Mercado Pago). La columna `clave` es única:
--      es la clave de idempotencia, así repetir la misma devolución no se registra dos veces.
--   3. Agrega la casilla "Acepta meses sin intereses" (regla M1), APAGADA por default:
--        producto.acepta_msi   → 0/1, aplica a todos sus artículos
--        variantes.acepta_msi  → NULL = hereda del producto; 0/1 = decisión propia del artículo
--
-- Idempotente: se puede correr varias veces (también sobre la versión anterior de este script, que ya
-- corrió en QA el 2026-10-21: agrega las columnas que le faltan a pago_tarjeta_devolucion). No cambia datos.
-- Correr primero en inventario_key_qa (cubre dev y qa); en inventario_key (prod) solo después de validar en QA,
-- de preferencia en horario de poco tráfico (por eso el lock_wait_timeout de abajo).
-- ============================================================

SET NAMES utf8mb4;
SET SESSION lock_wait_timeout = 5;

-- ── 0. ¿Estás en la base correcta? ──────────────────────────────────────────────
SELECT DATABASE() AS base_en_uso;

-- ── 1. Intentos de cobro ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pago_tarjeta_intento (
    id             INT           NOT NULL AUTO_INCREMENT,
    pedido_id      INT           NOT NULL,
    cliente_id     INT           NULL     COMMENT 'Dueño del pedido, si es un cliente registrado',
    canal          VARCHAR(20)   NOT NULL COMMENT 'TERMINAL | EN_LINEA',
    monto          DECIMAL(12,2) NOT NULL COMMENT 'Siempre el total del pedido calculado por el back',
    cuotas         INT           NOT NULL DEFAULT 1 COMMENT '1 = contado; mas = meses sin intereses',
    estado         VARCHAR(20)   NOT NULL COMMENT 'CREADO|ENVIADO|APROBADO|COBRADO_SIN_APLICAR|RECHAZADO|CANCELADO|EXPIRADO|DEVUELTO_PARCIAL|DEVUELTO',
    id_externo     VARCHAR(100)  NULL     COMMENT 'Identificador del cobro en la pasarela (Mercado Pago)',
    iniciado_por   VARCHAR(100)  NOT NULL,
    devuelto       DECIMAL(12,2) NOT NULL DEFAULT 0,
    creado_en      DATETIME(3)   NOT NULL COMMENT 'UTC',
    actualizado_en DATETIME(3)   NOT NULL COMMENT 'UTC',
    version        BIGINT        NOT NULL DEFAULT 0 COMMENT 'Candado optimista (@Version)',
    activo_pedido  INT GENERATED ALWAYS AS (CASE WHEN estado IN ('CREADO', 'ENVIADO') THEN pedido_id ELSE NULL END) STORED
                   COMMENT 'Lo calcula la base: el pedido mientras el cobro esta activo, NULL si ya termino',
    PRIMARY KEY (id),
    UNIQUE KEY uq_pti_activo (activo_pedido),
    UNIQUE KEY uq_pti_externo (id_externo),
    KEY idx_pti_pedido_estado (pedido_id, estado),
    KEY idx_pti_estado_creado (estado, creado_en),
    CONSTRAINT chk_pti_monto   CHECK (monto > 0),
    CONSTRAINT chk_pti_cuotas  CHECK (cuotas >= 1),
    CONSTRAINT chk_pti_devuelto CHECK (devuelto >= 0 AND devuelto <= monto)
);

-- ── 2. Devoluciones (una fila por devolución) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS pago_tarjeta_devolucion (
    id          INT           NOT NULL AUTO_INCREMENT,
    intento_id  INT           NOT NULL,
    monto       DECIMAL(12,2) NOT NULL,
    hecha_por   VARCHAR(100)  NOT NULL,
    clave       VARCHAR(100)  NOT NULL COMMENT 'Clave de idempotencia (la misma que se manda a la pasarela)',
    estado      VARCHAR(12)   NOT NULL DEFAULT 'CONFIRMADA' COMMENT 'PENDIENTE = reservada, falta confirmar con la pasarela | CONFIRMADA',
    creado_en   DATETIME(3)   NOT NULL COMMENT 'UTC',
    confirmada_en DATETIME(3) NULL COMMENT 'UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ptd_clave (clave),
    KEY idx_ptd_intento (intento_id),
    CONSTRAINT fk_ptd_intento FOREIGN KEY (intento_id) REFERENCES pago_tarjeta_intento (id),
    CONSTRAINT chk_ptd_monto  CHECK (monto > 0)
);

-- ── 2b. Si la tabla de devoluciones ya existía (versión anterior), se le agregan las columnas nuevas.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_devolucion' AND column_name = 'estado') = 0,
    'ALTER TABLE pago_tarjeta_devolucion ADD COLUMN estado VARCHAR(12) NOT NULL DEFAULT ''CONFIRMADA'' AFTER clave',
    'SELECT ''pago_tarjeta_devolucion.estado ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_devolucion' AND column_name = 'confirmada_en') = 0,
    'ALTER TABLE pago_tarjeta_devolucion ADD COLUMN confirmada_en DATETIME(3) NULL COMMENT ''UTC''',
    'SELECT ''pago_tarjeta_devolucion.confirmada_en ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- El comentario de la columna estado del intento incluye COBRADO_SIN_APLICAR (cobro que no se aplicó al pedido).
ALTER TABLE pago_tarjeta_intento MODIFY COLUMN estado VARCHAR(20) NOT NULL
    COMMENT 'CREADO|ENVIADO|APROBADO|COBRADO_SIN_APLICAR|RECHAZADO|CANCELADO|EXPIRADO|DEVUELTO_PARCIAL|DEVUELTO';

-- ── 3. Casilla "Acepta MSI" (apagada por default) ──────────────────────────────
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'producto' AND column_name = 'acepta_msi') = 0,
    'ALTER TABLE producto ADD COLUMN acepta_msi TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT ''producto.acepta_msi ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'variantes' AND column_name = 'acepta_msi') = 0,
    'ALTER TABLE variantes ADD COLUMN acepta_msi TINYINT(1) NULL COMMENT ''NULL = hereda del producto''',
    'SELECT ''variantes.acepta_msi ya existe''');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ── 4. Verificación (cada una dice qué debe salir) ─────────────────────────────
-- Debe devolver 2 filas: producto (NO, default 0) y variantes (YES, default NULL)
SELECT table_name, column_name, column_type, is_nullable, column_default
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND column_name = 'acepta_msi' AND table_name IN ('producto', 'variantes');

-- Debe ser 14 (12 columnas + version + la generada activo_pedido)
SELECT COUNT(*) AS columnas_de_pago_tarjeta_intento
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_intento';

-- Debe devolver 3 filas: uq_pti_activo, uq_pti_externo y PRIMARY marcados como únicos (non_unique = 0)
SELECT DISTINCT index_name, non_unique
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_intento'
   AND index_name IN ('PRIMARY', 'uq_pti_activo', 'uq_pti_externo');

-- Debe devolver 3 restricciones CHECK de pago_tarjeta_intento
SELECT constraint_name FROM information_schema.table_constraints
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_intento' AND constraint_type = 'CHECK';

-- Debe devolver 3 filas: la tabla de devoluciones con clave, estado y confirmada_en
SELECT column_name, column_type, column_default FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_devolucion' AND column_name IN ('clave', 'estado', 'confirmada_en');

-- Debe devolver 2 filas: uq_ptd_clave (clave única) y fk_ptd_intento
SELECT constraint_name, constraint_type FROM information_schema.table_constraints
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_devolucion' AND constraint_name IN ('uq_ptd_clave', 'fk_ptd_intento');

-- Debe devolver 1 fila: la columna generada que impide dos cobros activos del mismo pedido
SELECT column_name, generation_expression FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_intento' AND column_name = 'activo_pedido';

-- Debe devolver 1 fila: el estado del intento ya menciona COBRADO_SIN_APLICAR
SELECT column_comment FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'pago_tarjeta_intento' AND column_name = 'estado'
   AND column_comment LIKE '%COBRADO_SIN_APLICAR%';

-- Nada se activó solo: debe salir 0 (nadie acepta MSI hasta que el dueño lo marque)
SELECT COUNT(*) AS productos_con_msi_activado FROM producto WHERE acepta_msi = 1;

-- ============================================================
-- PARA VOLVER ATRÁS (solo si se pide; no correr junto con lo de arriba). Primero la tabla hija.
-- ============================================================
-- DROP TABLE IF EXISTS pago_tarjeta_devolucion;
-- DROP TABLE IF EXISTS pago_tarjeta_intento;
-- ALTER TABLE producto  DROP COLUMN acepta_msi;
-- ALTER TABLE variantes DROP COLUMN acepta_msi;
