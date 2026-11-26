-- ============================================================================
-- WhatsApp con la API de Meta (Cloud API): clientes, historial y pantalla "WhatsApp"
-- Fecha: 2026-11-26
--
-- Crea las dos tablas del dominio hexagonal/whatsapp y da de alta la pantalla en el menú:
--
--   whatsapp_cliente  -> un número con quien hay conversación. Guarda la marca del ÚLTIMO mensaje
--                        entrante (ultimo_entrante_at, la hora que manda Meta): de ahí se cuenta la
--                        ventana de 24 h. También quién atiende (bot o persona) y si requiere atención.
--   whatsapp_mensaje  -> el historial, separado por cliente. wa_message_id es ÚNICO: así no se procesa
--                        dos veces el mismo mensaje (Meta reenvía) aunque lleguen a la vez.
--
-- TODAS las fechas de estas tablas están en UTC (el dominio trabaja con Instant), sin importar la zona
-- del servidor. El front recibe ISO-8601 con "Z" y las convierte a la hora local.
--
-- utf8mb4: los mensajes llevan emojis; con utf8 (3 bytes) MySQL los rechaza o los corta.
--
-- Pantalla: ruta 'admin/whatsapp' en el menú «Sistema», junto a «Chat en vivo» ('admin/chat'). Se da a
-- ROLE_ADMIN con permiso de Ver y de Editar (contestar es escribir). Los demás roles, desde Gestión de roles.
--
-- Idempotente: se puede correr dos veces. Después de correrla: volver a entrar (las pantallas viajan en el JWT).
-- Sin la migración, el resto del sistema sigue igual; solo falla WhatsApp.
-- ============================================================================

CREATE TABLE IF NOT EXISTS whatsapp_cliente (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    telefono            VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(100) NULL,
    ultimo_entrante_at  DATETIME     NULL,
    ultima_actividad_at DATETIME     NOT NULL,
    pausa_bot_desde     DATETIME     NULL,
    bot_fijo_humano     TINYINT(1)   NOT NULL DEFAULT 0,
    requiere_atencion   TINYINT(1)   NOT NULL DEFAULT 0,
    ultimo_mensaje      VARCHAR(200) NULL,
    creado_at           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_whatsapp_cliente_telefono (telefono),
    KEY idx_whatsapp_cliente_actividad (ultima_actividad_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS whatsapp_mensaje (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    cliente_id    BIGINT       NOT NULL,
    wa_message_id VARCHAR(128) NULL,
    direccion     VARCHAR(10)  NOT NULL,
    origen        VARCHAR(10)  NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    contenido     TEXT         NULL,
    estado        VARCHAR(30)  NOT NULL,
    motivo        VARCHAR(255) NULL,
    codigo_error  INT          NULL,
    plantilla     VARCHAR(100) NULL,
    enviado_en    DATETIME     NOT NULL,
    creado_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_whatsapp_mensaje_wa_id (wa_message_id),
    KEY idx_whatsapp_mensaje_cliente (cliente_id, id),
    CONSTRAINT fk_whatsapp_mensaje_cliente FOREIGN KEY (cliente_id) REFERENCES whatsapp_cliente (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Pantalla "WhatsApp" en el menú Sistema
-- ----------------------------------------------------------------------------
INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden, tiene_escritura)
SELECT m.id, 'WhatsApp', 'admin/whatsapp', '🟢',
       'Mensajes de WhatsApp de los clientes: ver cada conversación, contestar a mano (dentro de las 24 h desde su último mensaje), tomar la conversación o devolvérsela al bot. Vive en el menú: Sistema → WhatsApp.',
       4, 1
FROM menu m
WHERE m.nombre = 'Sistema'
  AND NOT EXISTS (SELECT 1 FROM submenu e WHERE e.ruta = 'admin/whatsapp');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'admin/whatsapp'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

INSERT INTO rol_submenu_escritura (rol_id, submenu_id)
SELECT ro.id, s.id
FROM roles ro
CROSS JOIN submenu s
WHERE s.ruta = 'admin/whatsapp'
  AND ro.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu_escritura e WHERE e.rol_id = ro.id AND e.submenu_id = s.id);

-- ============================================================================
-- VERIFICACIÓN (cada una debe dar al menos 1 fila)
-- ============================================================================
-- SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('whatsapp_cliente', 'whatsapp_mensaje');
-- SELECT s.id, s.nombre, s.ruta, s.menu_id, s.tiene_escritura FROM submenu s WHERE s.ruta = 'admin/whatsapp';
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'admin/whatsapp';
-- SELECT ro.nombre_rol, s.ruta FROM rol_submenu_escritura rs JOIN roles ro ON ro.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'admin/whatsapp';

-- ----------------------------------------------------------------------------
-- Si hay que echarla para atrás (se pierde el historial de WhatsApp):
-- DELETE FROM rol_submenu_escritura WHERE submenu_id IN (SELECT id FROM submenu WHERE ruta = 'admin/whatsapp');
-- DELETE FROM rol_submenu WHERE submenu_id IN (SELECT id FROM submenu WHERE ruta = 'admin/whatsapp');
-- DELETE FROM submenu WHERE ruta = 'admin/whatsapp';
-- DROP TABLE whatsapp_mensaje;
-- DROP TABLE whatsapp_cliente;
-- ----------------------------------------------------------------------------
