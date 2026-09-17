-- ============================================================
-- Migración: ocultar el checkbox "✏️ Editar" en las pantallas que no editan nada (2026-09-17)
--
-- Motivo (reportado por el usuario): en "Agregar Modelo" los permisos muestran una opción
-- "Editar", pero esa pantalla no edita nada — su único botón crea. El usuario no sabía qué
-- habilitaba ese permiso, porque no habilitaba nada visible.
--
-- La causa: Gestión de roles pinta "Ver" y "✏️ Editar" en TODAS las pantallas por igual, sin
-- preguntar si la pantalla tiene algo que editar. Con esta columna el front solo lo pinta
-- cuando tiene_escritura = 1.
--
-- ⚠️ REGLA AL APAGARLO: poner 0 aquí NO afloja el back. Si SecurityConfig sigue exigiendo
-- pantallaEscribir para esa ruta, quitar el checkbox deja al rol sin forma de concederlo y sus
-- botones empiezan a dar 403. Por eso sólo se apaga en pantallas donde el back YA no lo exige.
--
-- Hoy la única en esa situación es 'productos/agregar' (su alta pasó a ir con la pantalla en
-- SecurityConfig, commit "fix(permisos): Agregar Modelo pedia Escritura..."). Las demás quedan
-- en 1 a propósito: apagarlas requiere primero mover sus endpoints de escritura a acciones
-- puntuales, pantalla por pantalla. Ver la lista en ese commit.
--
-- Correr en QA (cubre dev y qa) y en prod.
-- ============================================================

ALTER TABLE submenu ADD COLUMN tiene_escritura TINYINT(1) NOT NULL DEFAULT 1;

-- Agregar Modelo: sólo crea. Crear ya va con tener la pantalla, así que "Editar" aquí no
-- controla nada.
UPDATE submenu SET tiene_escritura = 0 WHERE ruta = 'productos/agregar';

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT ruta, nombre, tiene_escritura FROM submenu ORDER BY tiene_escritura, ruta;
-- Debe salir 'productos/agregar' con 0 y el resto con 1.
