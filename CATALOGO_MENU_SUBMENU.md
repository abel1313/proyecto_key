# Catálogo completo de Menú/Submenú (referencia)

Esta es la semilla original completa de `menu`/`submenu` (de `migration_menu_submenu.sql`, más
`personalizacion` agregada después en `migration_tema_variable.sql`). Úsala para comparar contra
lo que ves hoy en **Sistema → Menús y submenús** y volver a dar de alta lo que falte, con el mismo
ícono/orden si quieres que se vea igual.

No es la fuente de verdad en vivo — la fuente de verdad es siempre lo que hay en la base de datos
(tabla `submenu`). Este documento es solo la foto del punto de partida.

| Grupo | Ícono | Pantalla | Ruta | Orden |
|---|---|---|---|---|
| Catálogo | 📦 | Modelos | `productos/buscar` | 1 |
| Catálogo | 📦 | Agregar modelo | `productos/agregar` | 2 |
| Catálogo | 📦 | Agregar producto | `tienda/venta` | 3 |
| Catálogo | 📦 | Carga rápida de imágenes | `carga-imagenes` | 4 |
| Catálogo | 📦 | Cargar Excel | `tienda/cargar-excel` | 5 |
| Catálogo | 📦 | Categorías | `palabras-clave` | 6 |
| Envíos | 🚚 | Zonas de entrega | `lugares-entrega` | 1 |
| Pedidos | 📋 | Mis pedidos | `pedidos/mis-pedidos` | 1 |
| Pedidos | 📋 | Historial de pagos (MP) | `pedidos/historial-mp` | 2 |
| Ventas | 💰 | Venta directa | `tienda/venta-directa` | 1 |
| Ventas | 💰 | Créditos / Abonos | `abonos` | 2 |
| Ventas | 💰 | Gastos | `gastos/buscar` | 3 |
| Reportes | 📊 | Dashboard | `dashboard` | 1 |
| Reportes | 📊 | Reportes de ventas | `reportes` | 2 |
| Rifas | 🎰 | Rifa de productos | `rifas/agregar` | 1 |
| Rifas | 🎰 | Rifa mensual | `rifas/mes` | 2 |
| Rifas | 🎰 | Ver rifas activas | `rifas/buscar` | 3 |
| Flores eternas | 🌹 | Ramos de flores | `flores/ramos` | 1 |
| Flores eternas | 🌹 | Arma tu ramo | `flores/configurar` | 2 |
| Flores eternas | 🌹 | Catálogos | `flores/catalogos` | 3 |
| Flores eternas | 🌹 | Entregas | `flores/entregas` | 4 |
| Flores eternas | 🌹 | Frases por aprobar | `flores/frases` | 5 |
| Flores eternas | 🌹 | Administrar ramos armados | `flores/ramos-admin` | 6 |
| Marketing | 📣 | Promociones activas | `promociones` | 1 |
| Marketing | 📣 | Gestionar promociones | `admin/promociones` | 2 |
| Marketing | 📣 | Cinta de anuncios | `admin/cinta` | 3 |
| Marketing | 📣 | Publicar en redes | `admin/facebook` | 4 |
| Marketing | 📣 | Hashtags de redes | `admin/hashtags` | 5 |
| Sistema | 🛠️ | Usuarios | `usuarios/buscar` | 1 |
| Sistema | 🛠️ | Negocio & Contactos | `admin/negocio` | 2 |
| Sistema | 🛠️ | Chat en vivo | `admin/chat` | 3 |
| Sistema | 🛠️ | Imágenes de presentación | `admin/presentacion` | 4 |
| Sistema | 🛠️ | Diagnóstico de imágenes | `admin/diagnostico-imagenes` | 5 |
| Sistema | 🛠️ | Reconciliación de imágenes | `admin/reconciliacion-imagenes` | 6 |
| Sistema | 🛠️ | Limpiar caché | `admin/cache` | 7 |
| Sistema | 🗂️ | Menús y submenús | `gestion-menu` | 8 |
| Sistema | 🛡️ | Gestión de roles | `gestion-menu/roles` | 9 |
| Sistema | 🎨 | Personalización | `personalizacion` | 10 |
| *(sin grupo)* | 👥 | Clientes | `clientes/buscar` | 10 |
| *(sin grupo)* | 🏠 | Home | `home` | 1 |
| *(sin grupo)* | 🛍️ | Tienda | `tienda/buscar` | 2 |
| *(sin grupo)* | ❤️ | Favoritos | `favoritos` | 11 |
| *(sin grupo)* | 💬 | Chat | `chat` | 12 |
| *(sin grupo)* | 📱 | Código QR de la tienda | `qr` | 13 |
| *(sin grupo)* | 🔑 | Login | `login` | 14 |

**Total: 45 pantallas.** Si `ROLE_ADMIN` tiene menos de 45 en Gestión de roles, falta dar de alta
alguna de esta lista.
