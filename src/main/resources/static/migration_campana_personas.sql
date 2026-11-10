-- ============================================================
-- Migración: personas con código de campaña (2026-11-07). Requiere migration_campana.sql.
--
-- campana_persona: una persona con su código (uuid, único y de ella para siempre), sus datos de contacto y si aceptó el aviso de privacidad.
-- campana_persona_pedido: qué pedido es de qué persona dentro de qué campaña y cuánto suma hacia la meta (solo compras de Contado pagadas).
-- Si se borra a la persona (acción del administrador «Eliminar datos»), sus filas de pedidos se borran solas; los pedidos NO se tocan.
-- Si se borra la campaña o el pedido, se borra la fila que los liga.
-- Idempotente: se puede correr dos veces.
-- ============================================================

CREATE TABLE IF NOT EXISTS campana_persona (
    id             INT          NOT NULL AUTO_INCREMENT,
    uuid           VARCHAR(36)  NOT NULL,
    nombre         VARCHAR(150) NULL,
    correo         VARCHAR(150) NULL,
    telefono       VARCHAR(30)  NULL,
    consentimiento TINYINT(1)   NOT NULL DEFAULT 0,
    fecha_creacion DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_campana_persona_uuid (uuid),
    KEY idx_campana_persona_correo (correo),
    KEY idx_campana_persona_telefono (telefono)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS campana_persona_pedido (
    id         INT           NOT NULL AUTO_INCREMENT,
    campana_id INT           NOT NULL,
    persona_id INT           NOT NULL,
    pedido_id  INT           NOT NULL,
    monto      DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_cpp_campana_persona (campana_id, persona_id),
    KEY idx_cpp_pedido (pedido_id),
    CONSTRAINT fk_cpp_campana FOREIGN KEY (campana_id) REFERENCES campana (id)         ON DELETE CASCADE,
    CONSTRAINT fk_cpp_persona FOREIGN KEY (persona_id) REFERENCES campana_persona (id) ON DELETE CASCADE,
    CONSTRAINT fk_cpp_pedido  FOREIGN KEY (pedido_id)  REFERENCES pedidos (id)         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT p.uuid, p.nombre, p.correo, SUM(x.monto) AS suma FROM campana_persona p LEFT JOIN campana_persona_pedido x ON x.persona_id = p.id GROUP BY p.id;
