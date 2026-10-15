# Inventario de componentes del front — qué es cada pantalla

Pedido del dueño (2026-10-14): *"revisa todos los componentes del front y hazme listas… en el menú está Tienda y tiene una tabla, y mencionar que es una tabla, y así con todos… quiero saber qué es cada componente: si son card, table o qué son"*.

**Cómo se hizo:** un script leyó las rutas de Angular (`*-routing.module.ts`) y el HTML de los 106 componentes, y clasificó cada uno por lo que **realmente contiene** (`<table>`, elementos con `*ngFor` y clase `card`, `<form>`, etc.). Los nombres del menú son los de la skill `nombres-reales` (salen de las migraciones: si en tu pantalla se llaman distinto, gana tu pantalla). Es una **clasificación automática** (cuando una card contiene otra lista de cards o filas adentro, a veces la marca como «Card con lista de filas»: p. ej. *Imágenes de presentación* es una card que contiene una cuadrícula de cards de imagen): revísala y dime las que no coincidan con lo que ves.

## Los tipos

| Tipo | Qué significa |
|---|---|
| **Tabla** | Filas y columnas reales (`<table>`): Zonas de entrega, Entregas por zona, Gastos… |
| **Lista de cards** | Una tarjeta por cada elemento (un modelo, un pedido, una rifa…), en cuadrícula: Tienda, Mis pedidos, Rifas… |
| **Formulario dentro de una card** | Una sola tarjeta con campos para llenar: Agregar modelo, Negocio & Contactos… |
| **Card con lista de filas** | Una tarjeta con una lista simple de renglones adentro (no es tabla ni varias cards) |
| **Card** | Una tarjeta con información, sin tabla ni lista repetida |
| **Contenedor** | No dibuja nada propio: solo coloca otro componente adentro |

### Resumen de las pantallas con ruta (72)

| Tipo | Pantallas |
|---|---|
| Lista de cards | 19 |
| Formulario dentro de una card | 12 |
| Card con lista de filas | 11 |
| Contenedor (sin diseño propio: usa otro componente) | 10 |
| Tabla dentro de una card | 6 |
| Tabla | 6 |
| Card | 4 |
| Formulario | 4 |

## Pantallas del menú

> **Ejemplo que pediste:** *Tienda* **no es una tabla**: es una **lista de cards** (una card por artículo) con buscador y filtros arriba.

### Tienda

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Tienda** | `tienda/buscar` | `BuscarComponent` (`variante/buscar`) | Lista de cards | lista de cards, campos sueltos, buscador / filtros, ventanas y avisos, imágenes |

### Catálogo

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Modelos** | `productos/buscar` | `BuscaComponent` (`productos/producto/busca`) | Contenedor (sin diseño propio: usa otro componente) | ventanas y avisos — **Es un contenedor**: solo pone dentro el componente `AllComponent` (`app-all`), que es la lista de cards de modelos con su buscador y filtros. |
| **Agregar modelo** | `productos/agregar` | `AddComponent` (`productos/producto/add`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, gráfica o QR, ventanas y avisos, imágenes |
| **Agregar producto** | `tienda/venta` | `AgregarComponent` (`variante/agregar`) | Lista de cards | lista de cards, formulario, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |
| **Carga rápida de imágenes** | `carga-imagenes` | `CargaImagenesComponent` (`carga-imagenes`) | Lista de cards | lista de cards, campos sueltos, ventanas y avisos, imágenes |
| **Cargar Excel** | `tienda/cargar-excel` | `CargaArchivoComponent` (`documentos/carga-archivo`) | Card con lista de filas | listas de filas, card contenedora, ventanas y avisos |
| **Categorías** | `palabras-clave` | `GestionPalabrasClave` (`palabras-clave/gestion`) | Tabla dentro de una card | tabla, listas de filas, card contenedora, formulario, ventanas y avisos |

### Envíos

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Zonas de entrega** | `lugares-entrega` | `GestionLugaresComponent` (`lugares-entrega/gestion`) | Tabla | tabla, listas de filas, formulario, ventanas y avisos |
| **Entregas por zona** | `entregas-zona` | `EntregasZonaComponent` (`entregas-zona`) | Tabla | tabla, listas de filas, formulario, ventanas y avisos |

### Pedidos

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Mis pedidos** | `pedidos/mis-pedidos` | `MisPedidosComponent` (`pedidos/mis-pedidos`) | Lista de cards | lista de cards, campos sueltos, buscador / filtros, ventanas y avisos |
| **Historial de pagos (MP)** | `pedidos/historial-mp` | `HistorialMpComponent` (`pedidos/historial-mp`) | Card con lista de filas | listas de filas, card contenedora, buscador / filtros, ventanas y avisos, pestañas |

### Ventas

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Venta directa** | `tienda/venta-directa` | `VentaDirectaComponent` (`variante/venta-directa`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, buscador / filtros, ventanas y avisos, imágenes — Pantalla de caja: buscador de artículos, carrito y cobro dentro de una card; no es lista de cards. |
| **Créditos / Abonos** | `abonos` | `AbonosComponent` (`abonos`) | Lista de cards | lista de cards, campos sueltos, buscador / filtros, ventanas y avisos, pestañas |
| **Gastos** | `gastos/buscar` | `AllComponent` (`gastos/all`) | Tabla | tabla, listas de filas, ventanas y avisos, pestañas |

### Reportes

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Dashboard** | `dashboard` | `DashboardComponent` (`dashboard`) | Card | card contenedora, ventanas y avisos |
| **Reportes de ventas** | `reportes` | `ReportesComponent` (`reportes`) | Tabla dentro de una card | tabla, listas de filas, card contenedora, buscador / filtros, gráfica o QR, ventanas y avisos, pestañas |

### Rifas

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Rifa de productos** | `rifas/agregar` | `AgregarRifaComponent` (`rifas/agregar-rifa`) | Lista de cards | lista de cards, formulario, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |
| **Rifa mensual** | `rifas/mes` | `RifaMesComponent` (`rifas/rifa-mes`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |
| **Ver rifas activas** | `rifas/buscar` | `BuscarRifaComponent` (`rifas/buscar-rifa`) | Lista de cards | lista de cards, buscador / filtros, ventanas y avisos, pestañas |
| **Boletos de rifa** | `rifas/boletos` | `BoletosRifaComponent` (`rifas/boletos-rifa`) | Card con lista de filas | listas de filas, card contenedora, campos sueltos, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |

### Flores eternas

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Ramos de flores** | `flores/ramos` | `VitrinaFloresComponent` (`flores/vitrina`) | Lista de cards | lista de cards, ventanas y avisos, imágenes |
| **Arma tu ramo** | `flores/configurar` | `ConfigurarRamoComponent` (`flores/configurar`) | Formulario | listas de filas, campos sueltos, ventanas y avisos, imágenes |
| **Catálogos** | `flores/catalogos` | `CatalogosFloresComponent` (`flores/catalogos`) | Card con lista de filas | listas de filas, card contenedora, campos sueltos, ventanas y avisos, pestañas, imágenes |
| **Entregas** | `flores/entregas` | `ConfigEntregasComponent` (`flores/entregas`) | Lista de cards | lista de cards, campos sueltos, ventanas y avisos |
| **Frases por aprobar** | `flores/frases` | `BandejaFrasesComponent` (`flores/frases`) | Lista de cards | lista de cards, ventanas y avisos |
| **Administrar ramos armados** | `flores/ramos-admin` | `GestionRamosFloresComponent` (`flores/ramos-admin`) | Lista de cards | lista de cards, campos sueltos, ventanas y avisos, imágenes |

### Marketing

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Promociones activas** | `promociones` | `PromocionesComponent` (`promociones`) | Lista de cards | lista de cards, ventanas y avisos, imágenes |
| **Gestionar promociones** | `admin/promociones` | `GestionPromocionesComponent` (`admin/promociones`) | Lista de cards | lista de cards, campos sueltos, buscador / filtros, ventanas y avisos, imágenes |
| **Cinta de anuncios** | `admin/cinta` | `GestionCintaComponent` (`admin/cinta`) | Card con lista de filas | listas de filas, card contenedora, ventanas y avisos |
| **Publicar en redes** | `admin/facebook` | `PublicarFacebookComponent` (`admin/redes-sociales`) | Tabla dentro de una card | tabla, listas de filas, card contenedora, campos sueltos, buscador / filtros, ventanas y avisos, imágenes |
| **Hashtags de redes** | `admin/hashtags` | `GestionHashtagsComponent` (`admin/hashtags`) | Lista de cards | lista de cards, ventanas y avisos |

### Sistema

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Usuarios** | `usuarios/buscar` | `AllUsuariosComponent` (`usuarios/usuarios/all-usuarios`) | Lista de cards | lista de cards, ventanas y avisos, imágenes |
| **Negocio & Contactos** | `admin/negocio` | `ConfigNegocioComponent` (`admin/config-negocio`) | Formulario dentro de una card | card contenedora, formulario, ventanas y avisos |
| **Chat en vivo** | `admin/chat` | `ChatAdminComponent` (`admin/chat-admin`) | Contenedor (sin diseño propio: usa otro componente) | listas de filas — Bandeja del chat en vivo: lista de conversaciones a la izquierda y mensajes a la derecha (no es card ni tabla). |
| **Imágenes de presentación** | `admin/presentacion` | `PresentacionImagenesComponent` (`admin/presentacion-imagenes`) | Card con lista de filas | listas de filas, card contenedora, ventanas y avisos, imágenes — Cuadrícula de cards de imagen (login y registro), cada una con su descripción y botón Guardar. |
| **Diagnóstico de imágenes** | `admin/diagnostico-imagenes` | `DiagnosticoImagenesComponent` (`admin/diagnostico-imagenes`) | Card con lista de filas | listas de filas, card contenedora, buscador / filtros, pestañas — Herramienta de una sola card: se busca un modelo o producto y se ve el resultado. No es una lista. |
| **Reconciliación de imágenes** | `admin/reconciliacion-imagenes` | `ReconciliacionImagenesComponent` (`admin/reconciliacion-imagenes`) | Card con lista de filas | listas de filas, card contenedora — Herramienta de una sola card con botones de revisar/limpiar. |
| **Limpiar caché** | `admin/cache` | `CacheComponent` (`admin/cache`) | Card con lista de filas | listas de filas, card contenedora, campos sueltos, ventanas y avisos |
| **Menús y submenús** | `gestion-menu` | `GestionMenuComponent` (`menu-admin/gestion`) | Tabla | tabla, listas de filas, formulario, buscador / filtros, ventanas y avisos |
| **Gestión de roles** | `gestion-menu/roles` | `GestionRolesComponent` (`menu-admin/gestion-roles`) | Formulario | listas de filas, formulario, ventanas y avisos |
| **Personalización** | `personalizacion` | `GestionPersonalizacionComponent` (`tema-admin/gestion`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, ventanas y avisos |

### (sin grupo)

| Opción del menú | Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|---|
| **Home** | `home` | `HomeComponent` (`home`) | Contenedor (sin diseño propio: usa otro componente) | imágenes — Página de inicio; usa imágenes y bloques propios, sin card ni tabla. |
| **Clientes** | `clientes/buscar` | `ClientesBuscarComponent` (`clietes/clientes-buscar`) | Tabla dentro de una card | tabla, listas de filas, card contenedora, buscador / filtros, ventanas y avisos |
| **Favoritos** | `favoritos` | `FavoritosComponent` (`favoritos`) | Lista de cards | lista de cards, ventanas y avisos, imágenes |
| **Chat** | `chat` | `ChatUsuarioComponent` (`chat/chat-usuario`) | Card con lista de filas | listas de filas, card contenedora |
| **Código QR de la tienda** | `qr` | `QrVentasJadeComponent` (`qr-ventas-jade`) | Card con lista de filas | listas de filas, card contenedora, campos sueltos, gráfica o QR, ventanas y avisos, pestañas |
| **Login** | `login` | `LoginFormComponent` (`login/login-form`) | Formulario | formulario, gráfica o QR, ventanas y avisos, imágenes |

## Pantallas con ruta que NO son una opción del menú

Se abren desde un botón de otra pantalla, por un enlace o son públicas.

| Ruta | Componente | Tipo | Qué contiene |
|---|---|---|---|
| `**` | `PaginaNoDisponibleComponent` (`pagina-no-disponible`) | Contenedor (sin diseño propio: usa otro componente) | imágenes |
| `clientes/agregar` | `ClientesAddComponent` (`clietes/clientes-add`) | Formulario dentro de una card | card contenedora, formulario, ventanas y avisos |
| `clientes/agregar-compra` | `AgregarCompraComponent` (`clietes/agregar-compra`) | Card | card contenedora |
| `clientes/cambiar-password` | `CambiarPasswordComponent` (`clietes/cambiar-password`) | Formulario dentro de una card | card contenedora, formulario, ventanas y avisos |
| `clientes/mi-perfil` | `MiPerfilComponent` (`clietes/mi-perfil`) | Formulario dentro de una card | card contenedora, campos sueltos, ventanas y avisos |
| `clientes/mis-datos` | `MisDatosComponent` (`clietes/mis-datos`) | Lista de cards | lista de cards, formulario, ventanas y avisos |
| `clientes/mostrar/:id` | `ClientesMostrarComponent` (`clietes/clientes-mostrar`) | Lista de cards | lista de cards, formulario, ventanas y avisos |
| `demo-tablas` | `DemoTablasComponent` (`demo-tablas`) | Tabla dentro de una card | tabla, card contenedora |
| `eliminar-datos` | `EliminarDatosComponent` (`legal/eliminar-datos`) | Contenedor (sin diseño propio: usa otro componente) | — |
| `gastos/agregar` | `AddComponent` (`gastos/add`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, ventanas y avisos |
| `login/olvide-password` | `OlvidePasswordComponent` (`login/olvide-password`) | Formulario dentro de una card | card contenedora, formulario, ventanas y avisos |
| `login/verificar-correo` | `VerificarCorreoComponent` (`login/verificar-correo`) | Card | card contenedora, ventanas y avisos |
| `no-encontrada` | `PaginaNoDisponibleComponent` (`pagina-no-disponible`) | Contenedor (sin diseño propio: usa otro componente) | imágenes |
| `personalizacion/logos` | `GestionLogosComponent` (`tema-admin/gestion/gestion-logos`) | Lista de cards | lista de cards, ventanas y avisos, imágenes |
| `privacidad` | `PrivacidadComponent` (`legal/privacidad`) | Contenedor (sin diseño propio: usa otro componente) | — |
| `productos/detalle-producto/:id` | `DetalleProductoComponent` (`productos/producto/detalle-producto`) | Contenedor (sin diseño propio: usa otro componente) | ventanas y avisos, imágenes |
| `productos/update` | `UpdateComponent` (`productos/producto/update`) | Contenedor (sin diseño propio: usa otro componente) | ventanas y avisos, imágenes |
| `termConditions` | `TerminosComponent` (`legal/terminos`) | Contenedor (sin diseño propio: usa otro componente) | — |
| `tienda/carrito` | `VentaVarianteComponent` (`variante/venta-variante`) | Tabla | tabla, listas de filas, campos sueltos, buscador / filtros, ventanas y avisos, imágenes |
| `tienda/detalle/:id` | `DetalleVarianteComponent` (`variante/detalle-variante`) | Formulario | listas de filas, campos sueltos, ventanas y avisos, imágenes |
| `tienda/detalle/producto/:productoId` | `DetalleVarianteComponent` (`variante/detalle-variante`) | Formulario | listas de filas, campos sueltos, ventanas y avisos, imágenes |
| `tienda/update` | `UpdateVarianteComponent` (`variante/update-variante`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |
| `tiktok/callback` | `TiktokCallbackComponent` (`tiktok-callback`) | Card | card contenedora, imágenes |
| `usuarios/registrar` | `AddUsuariosComponent` (`usuarios/usuarios/add-usuarios`) | Formulario dentro de una card | listas de filas, card contenedora, formulario, buscador / filtros, gráfica o QR, ventanas y avisos, imágenes |
| `usuarios/update` | `ActualizarUsuariosComponent` (`usuarios/usuarios/actualizar-usuarios`) | Contenedor (sin diseño propio: usa otro componente) | — |
| `ventas/buscar` | `BuscarVentaComponent` (`ventas/venta-producto/buscar-venta`) | Tabla | tabla |
| `ventas/venta` | `AddVentaComponent` (`ventas/venta-producto/add-venta`) | Tabla dentro de una card | tabla, card contenedora, buscador / filtros, ventanas y avisos, imágenes |

## Piezas internas (no son una pantalla por sí solas)

| Componente | Carpeta | Tipo | Qué es |
|---|---|---|---|
| `AppComponent` | `.` | Contenedor (sin diseño propio: usa otro componente) | Raíz de la aplicación: arma la barra lateral, el contenido y el chat flotante. |
| `BuscarGenericoComponent` | `buscador/buscar-generico` | Contenedor (sin diseño propio: usa otro componente) | Caja de búsqueda reutilizable (solo el input). |
| `ChatbotComponent` | `chatbot` | Lista de cards | Burbuja 💬 del chat de la tienda (ventana flotante con mensajes y tarjetas de producto). |
| `CintaComponent` | `cinta` | Contenedor (sin diseño propio: usa otro componente) | Cinta de anuncios que corre arriba de la pantalla. |
| `BuscarComponent` | `gastos/buscar` | Contenedor (sin diseño propio: usa otro componente) | **Sin contenido propio** (1 línea) en `gastos/buscar`. |
| `EditarComponent` | `gastos/editar` | Contenedor (sin diseño propio: usa otro componente) | **Sin contenido propio** (1 línea): posible resto de Gastos. |
| `RegresarLegalComponent` | `legal/regresar-legal` | Contenedor (sin diseño propio: usa otro componente) | Enlace de regreso en las pantallas legales. |
| `LoadingComponent` | `loading` | Contenedor (sin diseño propio: usa otro componente) | Capa de «cargando» global. |
| `AnillosEditorComponent` | `lugares-entrega/anillos-editor` | Formulario | Editor de anillos de envío por distancia (filas con radio y costo) dentro de Zonas de entrega. |
| `NavbarComponent` | `navbar` | Card | Barra lateral y pie: menú, botón de tema día/noche, carrito de artículos, perfil. |
| `PalabraClaveAutocompleteComponent` | `palabras-clave/autocomplete` | Contenedor (sin diseño propio: usa otro componente) | Autocompletado de categorías (input con lista desplegable). |
| `AbonarGrupoComponent` | `pedidos/cobro/abonar-grupo` | Contenedor (sin diseño propio: usa otro componente) | Formulario (ventana) para abonar a un grupo de pedidos. |
| `CampoPagoComponent` | `pedidos/cobro/campo-pago` | Contenedor (sin diseño propio: usa otro componente) | Campo reutilizable de forma de pago (efectivo/transferencia/tarjeta) con su monto. |
| `DarAbonoComponent` | `pedidos/cobro/dar-abono` | Contenedor (sin diseño propio: usa otro componente) | Formulario (ventana) para registrar un abono. |
| `LiquidarApartadoComponent` | `pedidos/cobro/liquidar-apartado` | Contenedor (sin diseño propio: usa otro componente) | Formulario (ventana) para pagar un Apartado completo. |
| `LiquidarGrupoComponent` | `pedidos/cobro/liquidar-grupo` | Contenedor (sin diseño propio: usa otro componente) | Formulario (ventana) para pagar un grupo completo. |
| `DetallePedidoComponent` | `pedidos/detalle-pedido` | Lista de cards | Detalle de un pedido: encabezado, artículos, abonos, botones de acciones. Se abre desde Mis pedidos. |
| `GrupoPedidoComponent` | `pedidos/grupo-pedido` | Tabla | Pedidos unidos en grupo: tabla del grupo, abonos y separar. |
| `AllComponent` | `productos/producto/all` | Lista de cards | Lista de cards de **modelos** (la de Catálogo → Modelos y Productos → Buscar). Se mete dentro de `BuscaComponent`. |
| `MostrarRifasComponent` | `rifas/mostrar-rifas` | Contenedor (sin diseño propio: usa otro componente) | **Placeholder** («mostrar-rifas works!»): sin diseño. |
| `RuletaPublicaComponent` | `rifas/ruleta-publica` | Card con lista de filas | Ruleta pública de una rifa (card con la ruleta y la lista de participantes). |
| `AltaArticulosComponent` | `shared/alta-articulos` | Formulario | Ventana «🧩 Agregar artículos»: un formulario por artículo con sus checks. |
| `AyudaOpcionesComponent` | `shared/ayuda-opciones` | Contenedor (sin diseño propio: usa otro componente) | Ícono ⓘ que explica una opción. |
| `AyudaPantallaComponent` | `shared/ayuda-pantalla` | Contenedor (sin diseño propio: usa otro componente) | Ícono «?» que explica la pantalla. |
| `BotonVolverComponent` | `shared/boton-volver` | Contenedor (sin diseño propio: usa otro componente) | Botón «Volver» homologado. |
| `FiltroBuscableComponent` | `shared/filtro-buscable` | Contenedor (sin diseño propio: usa otro componente) | Filtro con buscador (talla/color/marca) en MAYÚSCULAS. |
| `SelectorFechaComponent` | `shared/selector-fecha` | Contenedor (sin diseño propio: usa otro componente) | Selector de fecha propio (calendario con año y mes rápidos). |
| `SelectorUbicacionComponent` | `shared/selector-ubicacion` | Contenedor (sin diseño propio: usa otro componente) | Mapa para elegir ubicación (Leaflet). |
| `UbicacionLocalComponent` | `shared/ubicacion-local` | Contenedor (sin diseño propio: usa otro componente) | Mapa/datos del local (cómo llegar). |
| `TableGenericoComponent` | `tablas/table-generico` | Tabla | Tabla genérica reutilizable (ag-grid). |
| `BuscarUsuariosComponent` | `usuarios/usuarios/buscar-usuarios` | Contenedor (sin diseño propio: usa otro componente) | Barra de búsqueda de usuarios (se usa dentro de Usuarios). |
| `ListarVariantesComponent` | `variante/listar-variantes` | Contenedor (sin diseño propio: usa otro componente) | **Sin contenido propio** (casi vacío): posible resto del diseño anterior. |
| `MaskCellRendererComponent` | `ventas/venta-producto/add-venta` | Contenedor (sin diseño propio: usa otro componente) | Pieza de ag-grid que enmascara una celda. |
| `AllVentaComponent` | `ventas/venta-producto/all-venta` | Contenedor (sin diseño propio: usa otro componente) | **Sin contenido propio** (casi vacío): posible resto del diseño anterior. |

## Para revisar (hallazgos)

- **Posibles restos sin uso:** `ListarVariantesComponent`, `AllVentaComponent`, `MostrarRifasComponent`, `EditarComponent` y `gastos/buscar/BuscarComponent` tienen 0–1 línea de HTML. Si nadie los usa, son candidatos a `@deprecated` / eliminar.
- **Contenedores:** `productos/buscar` y `home` no dibujan nada propio; conviene que la skill `cards` diga cuál componente es el «de verdad».
- Esta clasificación es automática; las pantallas mixtas (Reportes, Publicar en redes, Menús y submenús) tienen tabla **y** cards.
