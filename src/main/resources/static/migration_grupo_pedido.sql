-- ============================================================
-- Migracion: unir pedidos (2026-09-23, dominio hexagonal grupopedido)
--
-- Caso real: al mismo cliente se le hacen 2 o 3 pedidos y quiere pagarlos como uno solo, o
-- alguien va a recoger el pedido de otra persona. Los pedidos NO se fusionan: se agrupan. Cada
-- uno conserva sus articulos, su cliente y sus abonos; el grupo muestra la suma y reparte cada
-- abono empezando por el pedido mas viejo. Deshacer el grupo no mueve dinero ni stock.
--
-- Backend: /v1/grupos-pedido (GrupoPedidoController).
--
-- ⚠️ CORRER ANTES DE DESPLEGAR el codigo: PUT /v1/pedidos/{id}/tipo consulta
-- grupo_pedido_miembro, asi que sin esta tabla el cambio de forma de cobro tambien falla.
--
-- Idempotente: CREATE TABLE IF NOT EXISTS + NOT EXISTS en los INSERT. Correr primero en
-- inventario_key_qa (cubre dev y qa) y despues en inventario_key (prod).
-- Despues de correrla hay que volver a entrar: los permisos viajan dentro del JWT.
-- ============================================================

-- 1. Los grupos. Los montos NO se guardan aqui: se calculan siempre desde los pedidos.
CREATE TABLE IF NOT EXISTS grupo_pedido (
    id                  INT          NOT NULL AUTO_INCREMENT,
    pedido_titular_id   INT          NOT NULL COMMENT 'Pedido cuyo cliente paga y recoge el grupo',
    activo              TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0 = se deshizo',
    fecha_creacion      DATETIME     NOT NULL,
    usuario_creo_id     INT          NULL,
    fecha_deshecho      DATETIME     NULL,
    usuario_deshizo_id  INT          NULL,
    nota                VARCHAR(200) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_grupo_pedido_titular FOREIGN KEY (pedido_titular_id) REFERENCES pedidos (id)
);

-- 2. Que pedidos estan en cada grupo. Las filas se conservan al deshacer: quedan de historial.
CREATE TABLE IF NOT EXISTS grupo_pedido_miembro (
    id         INT NOT NULL AUTO_INCREMENT,
    grupo_id   INT NOT NULL,
    pedido_id  INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_grupo_pedido_miembro (grupo_id, pedido_id),
    KEY idx_grupo_pedido_miembro_pedido (pedido_id),
    CONSTRAINT fk_grupo_miembro_grupo  FOREIGN KEY (grupo_id)  REFERENCES grupo_pedido (id),
    CONSTRAINT fk_grupo_miembro_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id)
);

-- 3. Permiso del boton "Unir pedidos" (unir, ver y deshacer). Abonar al grupo usa la accion
--    'abonar' que ya existe, asi que quien ya cobra abonos puede cobrarle al grupo.
INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'unir-pedidos', 'Unir pedidos (🔗)',
       'Boton para unir 2 o mas pedidos de la misma forma de cobro y cobrarlos como uno solo, y para deshacer la union. Cada pedido conserva sus articulos y sus abonos.',
       'Detalle del pedido', 17
FROM submenu s
WHERE s.ruta = 'pedidos/mis-pedidos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'unir-pedidos');

-- Solo ROLE_ADMIN de arranque; se le puede dar a otro rol desde Gestion de roles.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave = 'unir-pedidos'
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACION -- las dos consultas tienen que devolver filas
-- ============================================================
SHOW TABLES LIKE 'grupo_pedido%';

SELECT r.nombre_rol, a.clave, a.etiqueta
FROM rol_accion ra
JOIN roles r ON r.id = ra.rol_id
JOIN accion_submenu a ON a.id = ra.accion_submenu_id
JOIN submenu s ON s.id = a.submenu_id
WHERE s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'unir-pedidos';
