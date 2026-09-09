-- ============================================================
-- Migración: catálogo dinámico de variables de personalización visual
-- (reemplaza el diseño anterior de columnas fijas, migration_tema_negocio*.sql -- NO correr
-- esas si ya se corrieron, y si ya se corrieron sus tablas pueden borrarse: DROP TABLE
-- tema_negocio;  -- este catálogo nuevo no las usa).
--
-- Cada fila es una variable CSS: `clave` es el nombre del custom property SIN el prefijo "--"
-- (ej. "app-bg" -> se aplica como --app-bg). El admin puede agregar/editar/eliminar filas desde
-- la pantalla de Personalización sin tocar código -- ver TemaVariable.java.
--
-- Semilla: los valores *_claro y *_oscuro de abajo son EXACTAMENTE los que ya estaban a mano en
-- los bloques body.theme-light / body.theme-dark de styles.scss, así que la primera vez que se
-- carga el catálogo no cambia nada visualmente. valor_oscuro NULL en las dos filas estructurales
-- (card-radius, card-shadow) -- no son color, un solo valor sirve para los dos modos.
-- ============================================================

CREATE TABLE tema_variable (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    clave        VARCHAR(60)  NOT NULL UNIQUE,
    etiqueta     VARCHAR(80)  NOT NULL,
    grupo        VARCHAR(40)  NULL,
    tipo         VARCHAR(20)  NOT NULL DEFAULT 'color',
    valor_claro  VARCHAR(200) NULL,
    valor_oscuro VARCHAR(200) NULL,
    orden        INT NULL
);

INSERT INTO tema_variable (clave, etiqueta, grupo, tipo, valor_claro, valor_oscuro, orden) VALUES
    -- Marca
    ('brand-1',          'Color de marca (principal)', 'Marca', 'color', '#00875A', '#FFFFFF', 1),
    ('brand-2',          'Color de marca (secundario)', 'Marca', 'color', '#005C3D', '#C7C7CC', 2),
    ('brand-3',          'Color de marca (terciario)', 'Marca', 'color', '#00301F', '#8E8E93', 3),
    ('app-accent-ink',   'Texto sobre el color de marca (ej. dentro de un botón)', 'Marca', 'color', '#FFFFFF', '#000000', 4),
    -- Página
    ('app-bg',           'Fondo de la página', 'Página', 'color', '#F3FAF6', '#000000', 1),
    ('app-text',         'Texto principal', 'Página', 'color', '#152420', '#E9E9EC', 2),
    ('app-text-muted',   'Texto secundario', 'Página', 'color', '#55736A', '#9A9AA0', 3),
    ('app-border',       'Bordes generales', 'Página', 'color', '#D5E8DD', '#2A2A2E', 4),
    -- Card
    ('card-header-bg',   'Fondo del encabezado de las tarjetas', 'Card', 'color', '#FFFFFF', '#0C0C0C', 1),
    ('card-body-bg',     'Fondo del cuerpo de las tarjetas', 'Card', 'color', '#FFFFFF', '#0C0C0C', 2),
    ('card-footer-bg',   'Fondo del pie de las tarjetas', 'Card', 'color', '#FFFFFF', '#0C0C0C', 3),
    ('card-border',      'Borde de las tarjetas', 'Card', 'color', '#D5E8DD', '#2A2A2E', 4),
    ('card-radius',      'Redondeo de las tarjetas (px)', 'Card', 'numero', '14', NULL, 5),
    ('card-shadow',      'Intensidad de la sombra', 'Card', 'seleccion', 'media', NULL, 6),
    -- Tablas
    ('table-header-bg',  'Fondo del encabezado de tablas', 'Tablas', 'color', '#F9FAFB', '#151517', 1),
    ('table-header-text','Texto del encabezado de tablas', 'Tablas', 'color', '#9CA3AF', '#9A9AA0', 2),
    ('table-row-hover',  'Fondo de fila al pasar el mouse', 'Tablas', 'color', '#F9FAFB', '#151517', 3),
    ('table-border',     'Borde de tablas', 'Tablas', 'color', '#E5E7EB', '#2A2A2E', 4),
    -- Menú lateral
    ('sb-header-bg',     'Fondo del encabezado del menú lateral', 'Menú lateral', 'color', 'rgba(255,255,255,0.97)', 'rgba(0,0,0,0.92)', 1),
    ('sb-body-bg',       'Fondo del cuerpo del menú lateral', 'Menú lateral', 'color', 'rgba(255,255,255,0.97)', 'rgba(0,0,0,0.92)', 2),
    ('sb-footer-bg',     'Fondo del pie del menú lateral', 'Menú lateral', 'color', 'rgba(255,255,255,0.97)', 'rgba(0,0,0,0.92)', 3),
    ('sb-text',          'Texto del menú lateral', 'Menú lateral', 'color', '#12241D', '#E9E9EC', 4),
    ('sb-border',        'Borde del menú lateral', 'Menú lateral', 'color', '#D5E8DD', 'rgba(255,255,255,0.08)', 5),
    -- Formularios
    ('form-section-bg',  'Fondo de secciones de formulario', 'Formularios', 'color', '#E3F2EA', '#151517', 1),
    ('input-bg',         'Fondo de los campos de formulario', 'Formularios', 'color', '#FFFFFF', 'rgba(255,255,255,0.05)', 2);

-- ============================================================
-- Habilitar la pantalla "Personalización" en el catálogo de permisos (Menu/Submenu) --
-- sin esto NINGÚN rol, ni ROLE_ADMIN, puede acceder a /personalizacion (mismo bug que pasó con
-- gestion-menu/gestion-menu-roles: ver migration_fix_submenu_gestion_menu.sql).
-- ============================================================

INSERT INTO submenu (menu_id, nombre, ruta, icono, orden)
SELECT id, 'Personalización', 'personalizacion', '🎨', 10
FROM menu WHERE nombre = 'Sistema'
  AND NOT EXISTS (SELECT 1 FROM submenu WHERE ruta = 'personalizacion');

INSERT INTO rol_submenu (rol_id, submenu_id)
SELECT r.id, s.id FROM roles r CROSS JOIN submenu s
WHERE r.nombre_rol = 'ROLE_ADMIN' AND s.ruta = 'personalizacion'
  AND NOT EXISTS (SELECT 1 FROM rol_submenu rs WHERE rs.rol_id = r.id AND rs.submenu_id = s.id);

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT grupo, clave, etiqueta, valor_claro, valor_oscuro FROM tema_variable ORDER BY grupo, orden;
-- SELECT s.nombre, s.ruta, r.nombre_rol FROM submenu s JOIN rol_submenu rs ON rs.submenu_id = s.id JOIN roles r ON r.id = rs.rol_id WHERE s.ruta = 'personalizacion';
