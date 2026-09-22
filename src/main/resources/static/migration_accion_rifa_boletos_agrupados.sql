-- ============================================================
-- Migracion: acciones de boletos agrupados en rifas/boletos (2026-09-22).
--
-- Que resuelve: hoy cada participacion en redes obliga a cargar de nuevo nombre, plataforma y
-- perfil, y despues se ven como filas sueltas ("Facebook · juan · like" y aparte
-- "Facebook · juan · compartio", cuando es la misma persona en la misma red). El formato nuevo
-- carga la cabecera UNA vez y suma participaciones; cada url de participacion es un boleto.
--
-- Backend: BoletoAgrupadoController (/v1/rifas/{id}/boletos-agrupados).
--
-- OJO -- esto NO cambia como se sortea. El sorteo elige FILAS al azar
-- (BoletoRifaServiceImpl.sortear), asi que por debajo se sigue creando una fila por
-- participacion. Nadie cambia de probabilidad y los boletos viejos no se migran.
-- Ver hexagonal/rifa/README.md, decisiones D1 y D4.
-- ============================================================

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'cargar-boletos-agrupado', 'Cargar participaciones de un perfil (➕)',
       'Da de alta un perfil de red social con todas sus participaciones de una sola pasada. El nombre, la plataforma y el link del perfil se llenan una vez; cada url de lo que hizo el cliente suma un boleto.',
       'Boletos por redes', 20
FROM submenu s
WHERE s.ruta = 'rifas/boletos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'cargar-boletos-agrupado');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'agregar-participacion', 'Sumar una participacion (🎟️)',
       'Agrega una url de participacion a un perfil que ya esta cargado, sin volver a llenar nombre, plataforma ni perfil. Suma un boleto.',
       'Boletos por redes', 21
FROM submenu s
WHERE s.ruta = 'rifas/boletos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'agregar-participacion');

INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, categoria, orden)
SELECT s.id, 'quitar-participacion', 'Quitar una participacion (🗑️)',
       'Quita una url de participacion de un perfil. Resta un boleto. El perfil puede quedar sin participaciones y no pasa nada: el cliente sigue en la rifa por sus otras redes.',
       'Boletos por redes', 22
FROM submenu s
WHERE s.ruta = 'rifas/boletos'
  AND NOT EXISTS (SELECT 1 FROM accion_submenu e WHERE e.submenu_id = s.id AND e.clave = 'quitar-participacion');

-- Solo ROLE_ADMIN de arranque. Cargar y quitar boletos mueve las probabilidades del sorteo, asi
-- que no se reparte sola a todo rol con Ver en la pantalla. Si el negocio quiere darsela a quien
-- administra las redes, se hace desde Gestion de roles, sin tocar codigo.
INSERT INTO rol_accion (rol_id, accion_submenu_id)
SELECT r.id, a.id
FROM roles r
CROSS JOIN accion_submenu a
JOIN submenu s ON s.id = a.submenu_id AND s.ruta = 'rifas/boletos'
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND a.clave IN ('cargar-boletos-agrupado', 'agregar-participacion', 'quitar-participacion')
  AND NOT EXISTS (
    SELECT 1 FROM rol_accion e WHERE e.rol_id = r.id AND e.accion_submenu_id = a.id
  );

-- ============================================================
-- VERIFICACION -- tiene que devolver 3 filas
-- ============================================================
-- SELECT r.nombre_rol, a.clave, a.etiqueta, a.orden
-- FROM rol_accion ra
-- JOIN roles r ON r.id = ra.rol_id
-- JOIN accion_submenu a ON a.id = ra.accion_submenu_id
-- JOIN submenu s ON s.id = a.submenu_id
-- WHERE s.ruta = 'rifas/boletos'
--   AND a.clave IN ('cargar-boletos-agrupado', 'agregar-participacion', 'quitar-participacion')
-- ORDER BY a.orden;
--
-- Si devuelve 0 filas: revisar que exista el submenu.
--   SELECT id, ruta FROM submenu WHERE ruta LIKE 'rifas%';
-- Si la ruta de la pantalla de boletos no es 'rifas/boletos', hay que corregirla en este script
-- ANTES de correrlo: un INSERT ... SELECT sobre una ruta que no existe inserta 0 filas y NO marca
-- error (es lo que paso con migration_submenu_ayuda_contextual.sql).
