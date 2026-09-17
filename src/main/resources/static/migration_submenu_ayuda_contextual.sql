-- ============================================================
-- Migración: permiso "Ayuda contextual" — el icono "?" que explica cada pantalla del admin
-- (2026-09-17)
--
-- Motivo: pantallas con nombres parecidos ("Agregar Modelo" vs "Nuevo Producto", "Promociones"
-- vs "Gestión de promociones", "Diagnóstico" vs "Reconciliación de imágenes") no dejaban claro
-- cuál usar, y había que entrar a probar para acordarse. Se agregó un icono "?" en el header de
-- cada pantalla que abre un modal con: qué es, para qué sirve, en qué se diferencia de la
-- pantalla parecida y por qué está hecha así.
--
-- Esta fila NO es una pantalla navegable: es el interruptor que decide qué roles ven ese icono.
-- Se modela como `submenu` porque es la única unidad de permiso que entiende Gestión de roles
-- (rol_submenu), y el navbar no la va a pintar como link: su template no recorre la tabla,
-- todos sus items están escritos a mano (no hay ngFor sobre submenus en navbar.component.html).
-- Por eso la ruta 'ayuda-contextual' no corresponde a ninguna ruta real del router.
--
-- Se concede solo a ROLE_ADMIN. Cualquier otro rol que deba ver las ayudas se marca desde
-- Gestión de roles, donde aparece como un checkbox más del grupo "Sistema".
--
-- Nota: el front ya muestra el icono a ROLE_ADMIN sin depender de esta fila
-- (auth.isAdminService || auth.tienePantalla('ayuda-contextual')), así que correr o no correr
-- esta migración no rompe al admin — lo que habilita es poder dárselo a los demás roles.
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, descripcion, orden)
SELECT gr.menu_id, 'Ayuda contextual', 'ayuda-contextual', '❓',
       'Permite ver el icono "?" que explica qué hace cada pantalla del admin, para qué sirve y en qué se diferencia de las pantallas parecidas. No es una pantalla: es el permiso que muestra u oculta ese icono en todo el panel.',
       gr.orden + 1
FROM submenu gr
WHERE gr.ruta = 'gestion-menu/roles'
  AND NOT EXISTS (SELECT 1 FROM submenu existente WHERE existente.ruta = 'ayuda-contextual');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id
FROM roles r
CROSS JOIN submenu s
WHERE s.ruta = 'ayuda-contextual'
  AND r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rol_submenu existente WHERE existente.rol_id = r.id AND existente.submenu_id = s.id
  );

-- No se inserta en rol_submenu_escritura: la ayuda es de solo lectura, no hay nada que editar
-- desde el modal, así que el permiso de Escritura no aplica.

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT s.id, s.ruta, s.nombre, s.menu_id, s.orden FROM submenu s WHERE s.ruta IN ('gestion-menu/roles', 'ayuda-contextual');
-- SELECT r.nombre_rol, s.ruta FROM rol_submenu rs JOIN roles r ON r.id = rs.rol_id JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'ayuda-contextual';
