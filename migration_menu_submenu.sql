-- ============================================================
-- Migración: catálogo de menú/submenú (Fase 1 de permisos por pantalla)
--
-- Ver PLAN_PERMISOS_PANTALLAS.md (repo compartido) para el diseño completo.
-- `menu` = grupo del acordeón del sidebar (ej. "Catálogo", "Envíos", "Sistema").
-- `submenu` = item real que navega a una pantalla (ej. "Modelos" -> productos/buscar).
-- menu_id NULL en submenu = item de nivel superior sin grupo (Home, Tienda, Favoritos, Chat,
-- QR, Login) -- mismo criterio que ya usa navbar.component.html para esos casos.
-- ============================================================

CREATE TABLE menu (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(60) NOT NULL UNIQUE,
    icono   VARCHAR(10) NULL,
    orden   INT NULL
);

CREATE TABLE submenu (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    menu_id INT NULL,
    nombre  VARCHAR(80) NOT NULL,
    ruta    VARCHAR(150) NOT NULL,
    icono   VARCHAR(10) NULL,
    orden   INT NULL,
    CONSTRAINT fk_submenu_menu FOREIGN KEY (menu_id) REFERENCES menu (id) ON DELETE CASCADE
);

-- ============================================================
-- SEMILLA -- volcado directo de GROUP_ROUTES (navbar.component.ts) tal como quedó tras la
-- reorganización del menú (2026-08-25, ver PROPUESTA_REORGANIZACION_MENU.md). Editable después
-- desde el admin nuevo (POST /v1/menu/save, /v1/submenu/save) -- esto es solo el punto de partida.
-- ============================================================

INSERT INTO menu (nombre, icono, orden) VALUES
    ('Catálogo', '📦', 1),
    ('Envíos', '🚚', 2),
    ('Pedidos', '📋', 3),
    ('Ventas', '💰', 4),
    ('Reportes', '📊', 5),
    ('Rifas', '🎰', 6),
    ('Flores eternas', '🌹', 7),
    ('Marketing', '📣', 8),
    ('Sistema', '🛠️', 9);

INSERT INTO submenu (menu_id, nombre, ruta, icono, orden) VALUES
    -- Catálogo
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Modelos', 'productos/buscar', '🔍', 1),
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Agregar modelo', 'productos/agregar', '➕', 2),
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Agregar producto', 'tienda/venta', '🧩', 3),
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Carga rápida de imágenes', 'carga-imagenes', '📸', 4),
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Cargar Excel', 'tienda/cargar-excel', '📂', 5),
    ((SELECT id FROM menu WHERE nombre = 'Catálogo'), 'Categorías', 'palabras-clave', '🏷️', 6),
    -- Envíos
    ((SELECT id FROM menu WHERE nombre = 'Envíos'), 'Zonas de entrega', 'lugares-entrega', '📍', 1),
    -- Pedidos
    ((SELECT id FROM menu WHERE nombre = 'Pedidos'), 'Mis pedidos', 'pedidos/mis-pedidos', NULL, 1),
    ((SELECT id FROM menu WHERE nombre = 'Pedidos'), 'Historial de pagos (MP)', 'pedidos/historial-mp', NULL, 2),
    -- Ventas
    ((SELECT id FROM menu WHERE nombre = 'Ventas'), 'Venta directa', 'tienda/venta-directa', '💰', 1),
    ((SELECT id FROM menu WHERE nombre = 'Ventas'), 'Créditos / Abonos', 'abonos', '💳', 2),
    ((SELECT id FROM menu WHERE nombre = 'Ventas'), 'Gastos', 'gastos/buscar', '💸', 3),
    -- Reportes
    ((SELECT id FROM menu WHERE nombre = 'Reportes'), 'Dashboard', 'dashboard', '🏠', 1),
    ((SELECT id FROM menu WHERE nombre = 'Reportes'), 'Reportes de ventas', 'reportes', '📈', 2),
    -- Rifas
    ((SELECT id FROM menu WHERE nombre = 'Rifas'), 'Rifa de productos', 'rifas/agregar', '🎡', 1),
    ((SELECT id FROM menu WHERE nombre = 'Rifas'), 'Rifa mensual', 'rifas/mes', '📅', 2),
    ((SELECT id FROM menu WHERE nombre = 'Rifas'), 'Ver rifas activas', 'rifas/buscar', '🔍', 3),
    -- Flores eternas
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Ramos de flores', 'flores/ramos', '🌹', 1),
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Arma tu ramo', 'flores/configurar', '🌷', 2),
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Catálogos', 'flores/catalogos', '🌸', 3),
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Entregas', 'flores/entregas', '🚚', 4),
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Frases por aprobar', 'flores/frases', '🎗️', 5),
    ((SELECT id FROM menu WHERE nombre = 'Flores eternas'), 'Administrar ramos armados', 'flores/ramos-admin', '🎁', 6),
    -- Marketing
    ((SELECT id FROM menu WHERE nombre = 'Marketing'), 'Promociones activas', 'promociones', '🎁', 1),
    ((SELECT id FROM menu WHERE nombre = 'Marketing'), 'Gestionar promociones', 'admin/promociones', '🎁', 2),
    ((SELECT id FROM menu WHERE nombre = 'Marketing'), 'Cinta de anuncios', 'admin/cinta', '📢', 3),
    ((SELECT id FROM menu WHERE nombre = 'Marketing'), 'Publicar en redes', 'admin/facebook', '📘', 4),
    ((SELECT id FROM menu WHERE nombre = 'Marketing'), 'Hashtags de redes', 'admin/hashtags', '🏷️', 5),
    -- Sistema
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Usuarios', 'usuarios/buscar', '👥', 1),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Negocio & Contactos', 'admin/negocio', '🏪', 2),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Chat en vivo', 'admin/chat', '💬', 3),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Imágenes de presentación', 'admin/presentacion', '🖼️', 4),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Diagnóstico de imágenes', 'admin/diagnostico-imagenes', '🔍', 5),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Reconciliación de imágenes', 'admin/reconciliacion-imagenes', '🔧', 6),
    ((SELECT id FROM menu WHERE nombre = 'Sistema'), 'Limpiar caché', 'admin/cache', '🗑️', 7),
    -- Clientes (link top-level, sin grupo -- ver PROPUESTA_REORGANIZACION_MENU.md)
    (NULL, 'Clientes', 'clientes/buscar', '👥', 10),
    -- Standalone (fuera de cualquier acordeón)
    (NULL, 'Home', 'home', '🏠', 1),
    (NULL, 'Tienda', 'tienda/buscar', '🛍️', 2),
    (NULL, 'Favoritos', 'favoritos', '❤️', 11),
    (NULL, 'Chat', 'chat', '💬', 12),
    (NULL, 'Código QR de la tienda', 'qr', '📱', 13),
    (NULL, 'Login', 'login', '🔑', 14);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT m.nombre AS menu, s.nombre AS submenu, s.ruta FROM submenu s LEFT JOIN menu m ON m.id = s.menu_id ORDER BY m.orden, s.orden;
