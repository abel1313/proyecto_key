-- ============================================================
-- Migración: descripción ("¿qué es esta pantalla? ¿dónde vive en el menú?") para CADA pantalla
-- del catálogo submenu -- no solo las acciones puntuales (ver migration_descripcion_acciones_
-- modelos.sql). Pedido del usuario 2026-08-28: el botón ℹ️ que ya existe para las acciones
-- también hacía falta para el checkbox "Ver" de cada pantalla, para que Gestión de roles explique
-- las ~38 pantallas, no solo las 14 acciones de Modelos/Tienda.
-- ============================================================

ALTER TABLE submenu ADD COLUMN descripcion VARCHAR(255) NULL;

UPDATE submenu SET descripcion = CASE ruta
    -- Catálogo
    WHEN 'productos/buscar' THEN 'Buscar y administrar los Modelos (el "molde" de un producto, ej. "Blusa Zara" antes de elegir talla/color). Vive en el menú: Catálogo → Modelos.'
    WHEN 'productos/agregar' THEN 'Formulario para dar de alta un Modelo nuevo. Vive en el menú: Catálogo → Agregar modelo.'
    WHEN 'tienda/venta' THEN 'Formulario para agregar un Producto (una variante concreta: talla/color) a partir de un Modelo ya creado. Vive en el menú: Catálogo → Agregar producto.'
    WHEN 'carga-imagenes' THEN 'Pantalla para subir varias imágenes de golpe y asignarlas a sus variantes por código de barras. Vive en el menú: Catálogo → Carga rápida de imágenes.'
    WHEN 'tienda/cargar-excel' THEN 'Carga masiva de variantes/stock desde un archivo Excel. Vive en el menú: Catálogo → Cargar Excel.'
    WHEN 'palabras-clave' THEN 'Catálogo de categorías/palabras clave usadas para clasificar y buscar productos. Vive en el menú: Catálogo → Categorías.'
    -- Envíos
    WHEN 'lugares-entrega' THEN 'Catálogo de zonas de entrega y sus costos de envío (usado también por Flores eternas). Vive en el menú: Envíos → Zonas de entrega.'
    -- Pedidos
    WHEN 'pedidos/mis-pedidos' THEN 'Listado de pedidos del negocio (no confundir con "Mis pedidos" del cliente, mismo componente, distinta audiencia). Vive en el menú: Pedidos → Mis pedidos.'
    WHEN 'pedidos/historial-mp' THEN 'Historial de pagos procesados con Mercado Pago. Vive en el menú: Pedidos → Historial de pagos (MP).'
    -- Ventas
    WHEN 'tienda/venta-directa' THEN 'Punto de venta para vender en mostrador (efectivo/tarjeta), sin pasar por el checkout de la Tienda online. Vive en el menú: Ventas → Venta directa.'
    WHEN 'abonos' THEN 'Ventas a crédito (fiado): registrar abonos parciales de un cliente hasta liquidar su compra. Vive en el menú: Ventas → Créditos / Abonos.'
    WHEN 'gastos/buscar' THEN 'Registro de gastos del negocio (para que los reportes de ganancia los descuenten). Vive en el menú: Ventas → Gastos.'
    -- Reportes
    WHEN 'dashboard' THEN 'Panel con métricas generales del negocio (ventas, stock, etc.) de un vistazo. Vive en el menú: Reportes → Dashboard.'
    WHEN 'reportes' THEN 'Reportes detallados de ventas, por fecha/producto/vendedor. Vive en el menú: Reportes → Reportes de ventas.'
    -- Rifas
    WHEN 'rifas/agregar' THEN 'Crear una rifa nueva y elegir qué productos participan. Vive en el menú: Rifas → Rifa de productos.'
    WHEN 'rifas/mes' THEN 'Configurar la rifa mensual automática del negocio. Vive en el menú: Rifas → Rifa mensual.'
    WHEN 'rifas/buscar' THEN 'Ver las rifas activas y sus participantes/ganador. Vive en el menú: Rifas → Ver rifas activas.'
    -- Flores eternas
    WHEN 'flores/ramos' THEN 'Vitrina pública de ramos ya armados por el admin, para que el cliente los pida directo. Vive en el menú: Flores eternas → Ramos de flores (pública, no necesita este permiso para que el cliente la vea).'
    WHEN 'flores/configurar' THEN 'Configurador donde el cliente arma su propio ramo desde cero (flor, cantidad, colores, accesorios). Vive en el menú: Flores eternas → Arma tu ramo (pública, no necesita este permiso para que el cliente la vea).'
    WHEN 'flores/catalogos' THEN 'Catálogos de flores eternas: especies, colores, cantidades válidas y accesorios (coronas, luces, etc.). Vive en el menú: Flores eternas → Catálogos.'
    WHEN 'flores/entregas' THEN 'Configuración de zonas/costos de entrega específicos de Flores eternas. Vive en el menú: Flores eternas → Entregas.'
    WHEN 'flores/frases' THEN 'Bandeja para aprobar o rechazar las frases de listón que los clientes piden en sus ramos. Vive en el menú: Flores eternas → Frases por aprobar.'
    WHEN 'flores/ramos-admin' THEN 'Armar y publicar los ramos ya armados que se muestran en la vitrina pública. Vive en el menú: Flores eternas → Administrar ramos armados.'
    -- Marketing
    WHEN 'promociones' THEN 'Listado público de las promociones activas (lo que ve el cliente). Vive en el menú: Marketing → Promociones activas.'
    WHEN 'admin/promociones' THEN 'Crear y administrar promociones/descuentos. Vive en el menú: Marketing → Gestionar promociones.'
    WHEN 'admin/cinta' THEN 'Editar la cinta de anuncios que se muestra arriba en la tienda pública. Vive en el menú: Marketing → Cinta de anuncios.'
    WHEN 'admin/facebook' THEN 'Publicar productos/promociones directo en Facebook/Instagram desde el sistema. Vive en el menú: Marketing → Publicar en redes.'
    WHEN 'admin/hashtags' THEN 'Catálogo de hashtags reutilizables para las publicaciones en redes. Vive en el menú: Marketing → Hashtags de redes.'
    -- Sistema
    WHEN 'usuarios/buscar' THEN 'Buscar, crear y administrar las cuentas de usuario del sistema (empleados, admins). Vive en el menú: Sistema → Usuarios.'
    WHEN 'admin/negocio' THEN 'Datos del negocio (horario, contacto, redes sociales) que se muestran al público. Vive en el menú: Sistema → Negocio & Contactos.'
    WHEN 'admin/chat' THEN 'Panel de chat en vivo para responder a los clientes en tiempo real. Vive en el menú: Sistema → Chat en vivo.'
    WHEN 'admin/presentacion' THEN 'Imágenes destacadas/banner de portada que se muestran en la tienda pública. Vive en el menú: Sistema → Imágenes de presentación.'
    WHEN 'admin/diagnostico-imagenes' THEN 'Herramienta para revisar por qué no aparece la imagen de un producto/variante. Vive en el menú: Sistema → Diagnóstico de imágenes.'
    WHEN 'admin/reconciliacion-imagenes' THEN 'Herramienta para detectar y arreglar imágenes huérfanas o faltantes entre la base de datos y el microservicio de imágenes. Vive en el menú: Sistema → Reconciliación de imágenes.'
    WHEN 'admin/cache' THEN 'Botón para limpiar la caché de Redis del sistema. Vive en el menú: Sistema → Limpiar caché.'
    WHEN 'gestion-menu' THEN 'Catálogo de menús y submenús (la estructura del sidebar) -- editable sin tocar código. Vive en el menú: Sistema → Menús y submenús.'
    WHEN 'gestion-menu/roles' THEN 'Esta misma pantalla: crear roles y marcar, pantalla por pantalla, qué puede ver/editar cada uno. Vive en el menú: Sistema → Gestión de roles.'
    WHEN 'personalizacion' THEN 'Colores, tipografías y textos personalizables de la tienda (lo que edita este catálogo de variables). Vive en el menú: Sistema → Personalización.'
    -- Clientes (sin grupo)
    WHEN 'clientes/buscar' THEN 'Buscar y administrar las cuentas de clientes registrados. Vive en el menú: Clientes (link directo, sin acordeón).'
    -- Standalone -- públicas, no necesitan este permiso para que cualquiera las vea, se listan
    -- solo para que quede claro por qué no tienen acciones que restringir.
    WHEN 'home' THEN 'Pantalla de inicio del sistema. Pública -- cualquier usuario logueado la ve, no depende de este permiso.'
    WHEN 'tienda/buscar' THEN 'La tienda pública -- el catálogo que ve el cliente para comprar. Pública -- no depende de este permiso para los clientes; el modo admin (editar/eliminar variantes) sí usa las acciones puntuales de esta pantalla.'
    WHEN 'favoritos' THEN 'Lista de productos favoritos del cliente logueado. Pública para cualquier cuenta, no depende de este permiso.'
    WHEN 'chat' THEN 'Chat del cliente con el negocio (lado cliente, distinto del panel admin "Chat en vivo"). Pública para cualquier cuenta, no depende de este permiso.'
    WHEN 'qr' THEN 'Código QR que lleva directo a la tienda -- para imprimir/compartir. Pública, no depende de este permiso.'
    WHEN 'login' THEN 'Pantalla de inicio de sesión. Pública por definición.'
    ELSE descripcion
END;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
-- SELECT ruta, nombre, descripcion FROM submenu ORDER BY ruta;
-- SELECT COUNT(*) FROM submenu WHERE descripcion IS NULL; -- deberían ser 0 (o solo pantallas
-- nuevas que se hayan dado de alta después de correr este script)
