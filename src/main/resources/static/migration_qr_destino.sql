-- ============================================================
-- Generador de QR con varios destinos configurables (P3 del "para después", 2026-09-17)
--
-- Antes: la pantalla "Código QR de la tienda" tenía UNA sola URL escrita a mano en el HTML
-- (qr-ventas-jade.component.html), así que agregar un QR nuevo o cambiar a dónde apunta pedía
-- tocar código y desplegar. Con esta tabla los destinos se dan de alta desde el admin.
--
-- El QR se sigue dibujando en el front (angularx-qrcode); aquí sólo vive a dónde apunta.
--
-- OJO: ddl-auto está en 'none' en todos los perfiles, así que esta tabla NO se crea sola —
-- hay que ejecutar este script en cada ambiente ANTES de desplegar.
--
-- Permisos: cuelga de la pantalla 'qr' que ya existe en el menú, no se crea pantalla nueva.
--   Ver    → ver la lista de destinos y sus QR
--   Editar → dar de alta, modificar y borrar destinos
-- ============================================================

CREATE TABLE IF NOT EXISTS qr_destino (
    id          INT           NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100)  NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    descripcion VARCHAR(300)  NULL,
    icono       VARCHAR(10)   NULL,
    activo      TINYINT(1)    NOT NULL DEFAULT 1,
    orden       INT           NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

-- ─── Destinos iniciales ───
-- Se siembra sólo el de la tienda, que es el que ya estaba escrito a mano en el componente.
-- WhatsApp y Facebook NO se siembran a propósito: esas URLs ya viven en configuracion_negocio
-- y cambian por negocio, así que se dan de alta desde la pantalla para no dejar aquí una URL
-- de ejemplo que apunte a la cuenta equivocada.
--
-- El FROM (SELECT 1) dummy hace que inserte aunque la tabla esté vacía, sin depender de
-- ninguna fila ancla (ver la lección anotada en CLAUDE.md sobre INSERT ... SELECT).
INSERT INTO qr_destino (nombre, url, descripcion, icono, activo, orden)
SELECT 'Tienda en línea',
       'https://shop.novedades-jade.com.mx',
       'Lleva al catálogo público. Es el QR para imprimir en tarjetas, bolsas y el mostrador.',
       '🛍️', 1, 1
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT url FROM qr_destino) AS existente
    WHERE existente.url = 'https://shop.novedades-jade.com.mx'
);

-- ============================================================
-- VERIFICACIÓN (debe devolver al menos 1 fila)
-- ============================================================
-- SELECT id, nombre, url, icono, activo, orden FROM qr_destino ORDER BY orden, id;
