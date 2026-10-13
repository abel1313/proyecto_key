-- ============================================================
-- Mis pedidos: el botón "📍 Entrega" de la tarjeta ahora se llama "📍 Dirección" (2026-10-13)
--
-- Pedido del dueño: en la tarjeta había dos botones con "entrega" (📍 Entrega y 📦 Entregar) y se
-- confundían. El de la dirección, el mapa, la zona y la fecha pasó a llamarse "Dirección" en el
-- front. Este script solo cambia el TEXTO del permiso en Sistema → 🛡️ Gestión de roles para que
-- diga lo mismo que el botón.
--
-- Qué NO cambia: la clave ('editar-entrega'), ni quién tiene el permiso. No hace falta volver a
-- entrar, basta recargar Gestión de roles.
--
-- Idempotente: si ya dice "Dirección" no toca nada. Correr primero en inventario_key_qa (cubre dev
-- y qa) y después en inventario_key (prod), cuando el front con el botón "Dirección" esté ahí.
-- ============================================================

SET NAMES utf8mb4;

UPDATE accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'pedidos/mis-pedidos'
SET a.etiqueta    = 'Dirección (📍 en la tarjeta)',
    a.descripcion = 'Botón "Dirección" de la tarjeta de cada pedido: a quién, dónde y cuándo se entrega (dirección, mapa, zona y fecha).'
WHERE a.id > 0
  AND a.clave = 'editar-entrega'
  AND a.etiqueta <> 'Dirección (📍 en la tarjeta)';

-- ── Verificación: tiene que devolver 1 fila con "Dirección (📍 en la tarjeta)" ─────────────
SELECT a.clave, a.etiqueta, a.descripcion
  FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id
 WHERE s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'editar-entrega';
