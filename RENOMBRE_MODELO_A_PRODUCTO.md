# Renombrado «Modelo» → «Producto» (pantalla) — inventario, decisiones y plan

**Fecha:** 2026-10-20 · **Estado:** textos de pantalla y mensajes ya cambiados en la rama `rename/modelo-a-producto` (back y front) para validar en QA; el script SQL de la base se corre aparte (ver `migration_renombre_producto_etiquetas.sql`). Falta validar en QA antes de pensar en producción.
**Pedido del dueño:** ya terminó el cambio de *variante* a **artículo**; ahora lo que en pantalla se llama **«Modelo»** (el producto base, ej. «Blusa Zara») debe llamarse **«Producto»**. Documentar por menú qué pantallas, rutas del front, endpoints del back y servicios internos están involucrados.

---

## 1. De dónde partimos (verificado en GitHub el 2026-10-20)

| | Back (`proyecto_key`) | Front (`producto_venta_online`) |
|---|---|---|
| Producción | `main` = `32cbd8b` | `master` = `ba7056a9` |
| QA | `qa` = `e7c37fd` | `qa` = `13de984a` |
| ¿qa = producción? | **Sí, mismo contenido** | **Sí, mismo contenido** |
| `dev` | **Atrasado: no tiene el renombrado a artículo** (se subió de la rama `rename/variante-a-articulo` directo a `qa` y a producción) | **Atrasado** igual |

- «Artículo» (antes variante) **ya está en producción**: el menú dice «🧩 Agregar artículo», los endpoints `/v2/articulos` conviven con `/v1/variantes`.
- Por eso este inventario se hizo sobre copias limpias de **`qa`**, no de `dev`. (Una primera pasada sobre `dev` dio conclusiones equivocadas, como «el renombrado está a medias»; se descartó.)
- **Consecuencia para el trabajo:** la rama nueva debe salir de `qa`/`main`, nunca de `dev`.

## 2. Alcance (igual que variante → artículo)

| Sí se cambia | No se cambia |
|---|---|
| Textos de pantalla: etiquetas, títulos, botones, mensajes, avisos, tooltips, placeholders, ayudas ⓘ | Nombres de tablas y columnas (la tabla es `producto`) |
| Mensajes que el back manda al usuario | Rutas de Angular y de endpoints (`/v1/productos/...`) |
| Nombres de menú, submenú y acciones guardados en la base (con **script SQL nuevo**) | Clases, DTOs, variables, paquetes `dominio/modelo` |
| Documentación viva (`CLAUDE.md`, skills, READMEs, `CAMBIOS_FRONT.md`) | Campos JSON (`modelos`, `modeloId`, `tipo:"MODELO"`) y claves de permiso (`ver-todos-los-modelos`, `crear-variantes`), que viajan en el JWT |

## 3. Cifras (todo en `qa`)

| Dónde | Qué | Cantidad |
|---|---|---|
| Front | Líneas con «modelo» que **sí cambian** (texto visible) | ~146 en 38 archivos |
| Front | Líneas con «modelo» que son código y **no** cambian | ~246 |
| Front | Textos que ya dicen «Producto» para el producto base (hoy desentonan con «Modelos», quedarán bien) | ~40 |
| Back | Mensajes Java al admin | 18 líneas |
| Back | Panel QA «Datos de prueba» | 21 líneas |
| Base de datos | Filas de texto (menús, permisos, color) | 16 filas (5 `submenu`, 10 `accion_submenu`, 1 `tema_variable`) |
| Documentos | `.md` con «modelo» en sentido producto base | 77 en el back (~1300 apariciones) y 13 en el front (~100) |
| Pruebas e2e (Playwright) | Textos exactos que dependen de lo visible | 4 puntos (+ 4 ya rotos hoy) |

## 4. Decisiones que necesito del dueño

| # | Decisión | Mi recomendación |
|---|---|---|
| 1 | En la **tienda del cliente** hay 17 textos que dicen «producto» para lo que compra (que ya es un **artículo**): «Todos nuestros productos», reseñas, favoritos, chatbot, carrito. ¿Se quedan? | **Dejarlos.** El cliente nunca vio «modelo», y los textos legales (Términos y Privacidad) no se tocan. El cambio es solo para el admin. |
| 2 | Frase del prompt del chatbot: *«el nombre suele ser el del MODELO»* (`ChatbotBase.java:76`) | Cambiar a *«el nombre suele ser el del PRODUCTO base»*. El resto del prompt (≈140 líneas de «producto») se queda. |
| 3 | **Cargar Excel** (`tienda/cargar-excel`): dice «Todos los productos se insertaron». ¿Crea productos, artículos o los dos? | Definir qué crea y escribir el texto exacto; la columna `nombreProducto` del Excel es contrato y no cambia. |
| 4 | Pantalla vieja `ventas/venta` (sin menú, dice «Productos») | Dejarla, o confirmar que ya no se usa. |
| 5 | Permiso «**Ver todos los modelos**» (la clave `ver-todos-los-modelos` NO cambia, solo la etiqueta) | Etiqueta nueva: «Ver todos los productos…». |
| 6 | Datos de prueba que tecleas: «QA Prueba 17 **Modelo** nuevo», «QA Prueba 20 **Modelo** sin stock» | Cambiar a «…**Producto** nuevo» / «…**Producto** sin stock» **conservando el prefijo `QA Prueba N `** (el «quitar prueba» los busca por ese prefijo) y actualizar los 5 lugares del documento de pruebas. |
| 7 | Mensaje de «ya no está a la venta» que puede leer el **cliente** en el carrito | Dejarlo neutro («ya no está disponible»). |
| 8 | ¿Se sube esta vez a **producción** (`main`/`master`)? El `CLAUDE.md` dice que el renombrado se acumula en `dev`/`qa`; el de artículos sí se subió | Sí, pero como cambio propio, back → front → script SQL, igual que el de artículos. Confirmarlo. |

## 5. Lo que hay que confirmar antes de empezar (los agentes no pueden ver la base)

1. **¿Se corrió `migration_renombre_articulo_etiquetas.sql` en producción (`inventario_key`)?** El `CLAUDE.md` lo marca «⏳». Misma duda con `migration_accion_tienda_venta_ver_todos.sql` (acción `ver-todos-los-modelos`).
2. **Valores vivos** de `submenu`, `accion_submenu` y `tema_variable` en QA y producción (puede haber filas editadas a mano desde Sistema → Menús y submenús). Consultas de solo lectura en el anexo C §1.5.
3. Si las columnas `descripcion` y `descripcion_escritura` miden 255 o más caracteres (los textos nuevos son un poco más largos).

## 6. Riesgos principales

1. **Volver a correr `migration_renombre_articulo_etiquetas.sql` pisaría el renombrado nuevo** (sus REPLACE cambian «Agregar producto» por «Agregar artículo» en todas las filas). Hay que protegerlo con una guarda al inicio y anotar «no volver a correr» en `CLAUDE.md`.
2. **El menú lateral está escrito a mano en el front** (`navbar.component.html:67,69,71`) y **los nombres de Gestión de roles salen de la base**. Son dos cambios que deben salir juntos; entre el deploy del front y el SQL habrá unos minutos desfasados.
3. **No cambiar ninguna `ruta` ni `clave`** de pantallas o acciones: `SecurityConfig` usa `hasAuthority('ruta:clave')`; renombrarlas daría 403 a todos, incluido el admin.
4. **Textos del panel «Datos de prueba» ya desactualizados** desde el renombrado de artículos («🧩 Productos», «Agregar producto»): si no se corrigen primero a «Artículos»/«Agregar artículo», después de este cambio apuntarían a la pantalla equivocada.
5. **Pruebas e2e:** 4 puntos ya están rotos hoy en `qa` (botón «Guardar artículo», placeholder de `/productos/buscar`, `¡Variante creada!`) y este cambio rompe 3 más.
6. Dos pruebas del back **fallan hoy** (`ArticuloAVenderTest`, `EditarArticulosServiceTest` esperan «el producto está deshabilitado» y el código dice «el modelo»). Con este cambio vuelven a pasar.

## 7. Plan en orden (igual que el renombrado de artículos)

0. **Preparación (sin cambiar vocabulario):** traer `qa` a `dev` (merge) para que el flujo `dev → qa → main` vuelva a ser parejo; confirmar los scripts en producción (punto 5); corregir los textos viejos del panel de pruebas y los e2e ya rotos.
1. **Rama propia** `rename/modelo-a-producto` en back y front, **desde `qa`**.
2. **Back:** los 18 mensajes (anexo B §2.1) y el panel QA (anexo B §2.2); javadoc de `Producto.java` y `Variantes.java`.
3. **Front:** textos A por pantalla (anexo A §3), los 3 «modelo que es artículo» (anexo A §5), colisiones admin (anexo A §4.1), catálogo de ayudas.
4. **Script SQL nuevo** `migration_renombre_producto_etiquetas.sql`: respaldo de las 16 filas, diagnóstico previo (solo SELECT), `UPDATE` por `ruta`/`clave` con doble guarda (texto actual exacto), `REPLACE` limitado por frases completas, verificación final (`LIKE '%modelo%'` debe salir vacío salvo filas editadas a mano), segunda corrida sin efecto. Anotarlo en la tabla de migraciones del `CLAUDE.md`.
5. **Documentos vivos:** `CLAUDE.md` de los dos repos (el del front define el vocabulario inverso en las líneas 4623–4730), `TAXONOMIA_NOMBRES_BACK.md`, `RENOMBRE_VARIANTE_A_ARTICULO.md`, skills `nombres-reales`/`renombrar`/`preparar-prueba`, READMEs de `hexagonal/*`, ~20 citas literales en `CAMBIOS_FRONT.md` (más una sección nueva con el «antes / después»). Los documentos históricos con fecha no se tocan.
6. **e2e:** `modelo.ts:31,34`, `articulo.ts:21,28`, `login.spec.ts` y `modelo.spec.ts`.
7. **Pruebas en QA** con antes/después (skill `pruebas-de-impacto`): cada pantalla del anexo A §2, Gestión de roles, panel «Datos de prueba», mismos permisos sin volver a entrar.
8. **Subida:** `qa` → validar → `main` (back) → `master` (front) → script SQL en `inventario_key` (con respaldo). Las pruebas se hacen en QA con la base `inventario_key_qa`.

---

## 8. Menú → pantalla → endpoints → servicios internos del back

Resumen. El detalle completo (todos los endpoints, cadena de servicios y textos A de cada uno) está en los anexos A §2 y B §3.

| Menú (lo que ve el admin hoy) | Ruta Angular | Componente | Endpoints del back | Servicios que se llaman por dentro |
|---|---|---|---|---|
| Catálogo → **🔍 Modelos** *(pasará a «Productos»)* | `productos/buscar` | `productos/producto/all` + `busca` | `GET /v1/productos/obtenerProductos`, `buscarNombreOrCodigoBarra`, `admin/filtrar`, `PUT /v1/productos/{id}/habilitar`, `DELETE deleteBy/{id}`, Excel `admin/sin-articulos/reporte`, `PUT /v1/precios/producto/{id}` | `ProductosControllerImpl` → `ProductosServiceImpl` → repos de producto/artículo/imagen; precio: `CambiarPrecioService` |
| Catálogo → **➕ Agregar modelo** *(→ «Agregar producto»)* | `productos/agregar` | `productos/producto/add` + modal `shared/alta-articulos` | `POST /v1/productos/save`, `GET /v1/stock/producto/{id}`, `POST /v2/articulos/inicializarDesdeProducto` | `ProductosServiceImpl.saveProductoLote` → código de barras, palabras clave, imágenes (micro de imágenes); `VarianteServiceImpl` para crear artículos |
| (sin menú) **✏️ Actualizar modelo** *(→ «Actualizar producto»)* | `productos/update` | `productos/producto/update` | `POST /v1/productos/save`, `GET /v1/imagenes/{id}/detalle` | igual que Agregar |
| (sin menú) Ver detalle del modelo | `productos/detalle-producto/:id` | `detalle-producto` | `GET /v1/productos/findById/{id}`, `POST /v1/productos/compartir-imagenes-articulos` | `ProductosServiceImpl` |
| Catálogo → **🧩 Agregar artículo** | `tienda/venta` | `variante/agregar` + `shared/alta-articulos` | `GET /v1/productos/buscarNombreOrCodigoBarra?todos=true`, `PUT /v1/productos/{id}/habilitar`, `POST /v2/articulos/guardarConImagenes`, `PUT /v1/stock/producto/{id}/ajuste` | `VarianteServiceImpl.guardarConImagenes` → `aplicarAjusteStockModelo` → `AjustarStockModeloService` → puertos de stock, Redis y Rabbit |
| Tienda (admin sobre la card) | `tienda/buscar`, `tienda/update`, `tienda/detalle/:id` | `variante/buscar`, `update-variante`, `detalle-variante` | `GET/PUT/DELETE /v2/articulos/...`, `POST /v2/articulos/{id}/independizar` («Convertir en producto propio») | `VarianteServiceImpl` |
| Catálogo → **📸 Carga rápida de imágenes** | `carga-imagenes` | `carga-imagenes` | `/v1/carga-imagenes/...` | `CargaImagenesServiceImpl` (crea producto borrador + un artículo por foto) |
| Catálogo → **📂 Cargar Excel** | `tienda/cargar-excel` | `documentos/carga-archivo` | `POST /v1/documentos/productos` | `SubirDocumentosServiceImpl.procesarExcelProductos` |
| Sistema → **🔍 Diagnóstico de imágenes** | `admin/diagnostico-imagenes` | `diagnostico-imagenes` | `GET /v1/productos/admin/diagnostico-imagenes/{id}` + diagnóstico de artículo | `ProductosServiceImpl`, `VarianteServiceImpl` |
| Sistema → **🗑️ Limpiar caché** (incluye «Datos de prueba») | `admin/cache` | `admin/cache` | `/v1/admin/datos-prueba/...` | `hexagonal/datosprueba` (`PrepararPruebaService`) |
| Sistema → **🔧 Reconciliación de imágenes** | `admin/reconciliacion-imagenes` | `reconciliacion-imagenes` | — | `ReconciliacionImagenService` |
| Sistema → **🗂️ Menús y submenús** / **🛡️ Gestión de roles** | `gestion-menu`, `gestion-menu/roles` | `menu-admin/...` | los nombres vienen de la **base** | tablas `submenu`, `accion_submenu` |
| Pedidos / ventas / abonos | `pedidos/...`, `tienda/venta-directa` | varios | `POST /v1/pedidos/{id}/articulos`, ventas, abonos | `EditarArticulosService`, `PedidoServiceImpl` (:705), `AbonoServiceImpl`, `ArticuloAVender` (:38) |
| Marketing → **Promociones**, Rifas, Reportes, Categorías | `admin/promociones`, `palabras-clave`, ... | varios | `/v1/promociones`, `/v2/configurarRifaArticulo`, `/v1/reportes/...` | `PromocionServiceImpl`, `ConfigurarRifaVarianteService`, `ReporteVentasServiceImpl` |

> La tabla completa, con todos los endpoints, está en el anexo A §2. Todas las URLs llevan el prefijo `/mis-productos`.

---

# ANEXOS (informes de auditoría, tal como los entregaron los agentes)

Tres agentes de solo lectura revisaron copias limpias de `qa`. Lo que **no** pudieron verificar: los valores reales de la base de datos (todo «valor actual» de la base está reconstruido a partir de los scripts) y si ciertos scripts corrieron en producción.


# ANEXO A — Front: textos, colisiones, e2e y ayudas

## 1. Resumen y conteos

**El renombrado variante->artículo YA está hecho en qa.** Eso cambia el panorama: la colisión nº1 del informe previo (dos "Agregar producto") ya no existe.

| Clase | Cantidad (líneas) | Nota |
|---|---|---|
| A: "modelo" visible a cambiar | **~146** en 38 archivos (~77 en HTML, ~69 en TS/Swal/catálogo de ayudas) | Detalle en sección 3 |
| B: código/comentario/variable/ruta/campo JSON | ~246 de las 392 líneas con "modelo" (`modelo` @Input, `ModeloParaArticulos`, `ajustarStockModelo`, `ajusteStockModelo`, `delModelo`, `igualAlModelo`, `usarImagenDelModelo`, `modelosCreados`, `modelosPedidos`, `modelosDadosDeBaja`, `plan.modelos`, tipo `'MODELO'`, `modeloId`, `puedeVerTodosLosModelos`, clave `ver-todos-los-modelos`) | No se tocan |
| C: otro significado | ~12 | `ngModelOptions` (falso positivo), "modelo mental", "modelo de datos", `modelo` de IA no hay |
| "Producto" que YA dice Producto y significa Modelo (hoy inconsistente, quedará correcto) | ~40 textos | Sección 3.9: no requieren cambio |
| Colisiones reales hoy (admin) | 6 | Sección 4.1 |
| Cliente final | 17 textos con "producto" = artículo | Sección 4.2: decisión del dueño |
| "modelo" que en realidad es ARTÍCULO | 3 | Sección 5 |

**Diferencias importantes contra el informe previo (dev):**
1. Ya no hay que "terminar variante->artículo antes": en qa está hecho. Botón de la card = `🧩 Artículos` (all.component.html:260 ya NO dice "Productos"); navbar = `🧩 Agregar artículo`; Tienda abre con "Todos nuestros productos" (ver 4.2); `Guardar artículo`, `Nuevo artículo`, `Editar artículo`, `Rifa de artículos`, etc.
2. `VarianteService.url` ahora es `/v2/articulos` (variante.service.ts:37), ya no `/v1/variantes`. El front del informe previo ponía `/v1/variantes/...`.
3. `/admin/sin-variantes/reporte` ahora es `/v1/productos/admin/sin-articulos/reporte` (producto.service.ts:222) y el Excel dice `Excel sin artículos` (all.component.html:49).
4. Ya cambiado en qa: título de la ayuda `tienda/venta` = "Agregar artículo" (catalog:57), tab de diagnóstico `🏷️ Artículo`, `Artículo seleccionado:`, "Nuevo artículo", ítem de rifas = "Rifa de artículos".
5. all.component.html:9-10 ahora dice **"Modelos / Catálogo de modelos (producto base)"** (el previo decía "Productos"): hay que cambiarlo.
6. e2e: `articulo.ts:21` (`/Guardar producto/`) YA está roto hoy en qa (el botón dice "Guardar artículo"). Es una rotura previa, no del renombrado.
7. Sigue sin haber textos de ayuda que digan "Producto" para el artículo en el admin, salvo `qr-ventas-jade` y los de Cargar Excel (ver 4.1).
8. CLAUDE.md del front sigue con el vocabulario VIEJO (Variante=Producto) en la línea 4662; es incorrecto desde antes de este cambio.

## 2. Tabla menú -> pantalla

Nombres reales del menú (navbar/navbar.component.html, hardcodeados; `tienePantalla(ruta)` filtra por permisos de BD). Base de URL del back: `ProductoService.url = {api_Url}/v1/productos`; `VarianteService.url = {api_Url}/v2/articulos`; stock = `/v1/stock/producto/{id}`; precios = `/v1/precios/articulo/{id}`.

| Menú -> pantalla (texto en navbar) | Ruta Angular | Componente | Servicios del front | URLs de back |
|---|---|---|---|---|
| (sidebar) **🛍️ Tienda** (navbar:48-51) | `tienda/buscar` | `variante/buscar` | VarianteService | GET /v2/articulos/buscar, /buscar-filtrado, /filtros-disponibles, /admin/filtrar; PUT /v2/articulos/{id}/habilitar; DELETE /v2/articulos/deleteBy/{id}; PUT /v1/precios/articulo/{id} |
| Catálogo -> **🔍 Modelos** (navbar:66-67) | `productos/buscar` | `productos/producto/busca` + `all/all.component` | ProductoService, VarianteService | GET /v1/productos/obtenerProductos; GET /v1/productos/buscarNombreOrCodigoBarra; GET /v1/productos/admin/filtrar; PUT /v1/productos/{id}/habilitar; PUT /v1/productos/admin/habilitar-lote; DELETE /v1/productos/deleteBy/{id}; GET /v1/productos/admin/sin-articulos/reporte; GET /v2/articulos/porProducto/{id}; POST /v2/articulos/inicializarDesdeProducto |
| Catálogo -> **➕ Agregar modelo** (navbar:68-69) | `productos/agregar` | `add/add.component` (`nombreCard='Agregar Modelo'`) + modal `shared/alta-articulos` | ProductoService, VarianteService | POST /v1/productos/save; GET /v1/stock/producto/{id}; POST /v2/articulos/guardarConImagenes |
| (sin menú; llega con "✏️ Actualizar" de Modelos) | `productos/update` | `update/update.component` (+ `app-add`, `nombreCard="'Actualizar Modelo'"`) | ProductoService, VarianteService | POST /v1/productos/save; GET /v1/imagenes/{id}/detalle |
| (sin menú; llega con "Ver detalle" de Modelos) | `productos/detalle-producto/:id` | `detalle-producto` | ProductoService | GET /v1/productos/findById/{id}; POST /v1/productos/compartir-imagenes-articulos |
| Catálogo -> **🧩 Agregar artículo** (navbar:70-71) | `tienda/venta` | `variante/agregar/agregar.component` + `shared/alta-articulos` | VarianteService, ProductoService | GET /v1/productos/buscarNombreOrCodigoBarra[&todos=true]; PUT /v1/productos/{id}/habilitar; GET y PUT /v1/stock/producto/{id}[/ajuste]; POST /v2/articulos/guardarConImagenes |
| (llega desde la card de Tienda) | `tienda/update` | `variante/update-variante` | VarianteService, ProductoService | GET /v2/articulos/getOne/{id}; POST /v2/articulos/guardarConImagenes; stock del producto; DELETE /v2/articulos/{id}/imagenes |
| (detalle, también cliente) | `tienda/detalle/:id`, `tienda/detalle/producto/:productoId` | `variante/detalle-variante` | VarianteService | GET /v2/articulos/getOne/{id}; POST /v2/articulos/{id}/independizar (acción admin "Convertir en modelo propio") |
| Catálogo -> **📸 Carga rápida de imágenes** | `carga-imagenes` | `carga-imagenes` | servicio propio | crea un producto (padre) borrador + 1 artículo por foto |
| Catálogo -> **📂 Cargar Excel** | `tienda/cargar-excel` | `documentos/carga-archivo` | - | carga masiva (columna `nombreProducto`) |
| Catálogo -> **🏷️ Categorías** | `palabras-clave` | `palabras-clave/gestion` | - | - |
| Ventas -> **💰 Venta directa** | `tienda/venta-directa` | `variante/venta-directa` | VarianteService | busca artículos |
| Marketing -> **🎁 Gestionar promociones** | `admin/promociones` | `admin/promociones/gestion-promociones` | - | - |
| Sistema -> **🔍 Diagnóstico de imágenes** | `admin/diagnostico-imagenes` | `admin/diagnostico-imagenes` | ProductoService, VarianteService | GET /v1/productos/admin/diagnostico-imagenes/{productoId}; buscarNombreOrCodigoBarra; diagnóstico de artículo |
| Sistema -> **🔧 Reconciliación de imágenes** | `admin/reconciliacion-imagenes` | `admin/reconciliacion-imagenes` | - | - |
| Sistema -> **🗑️ Limpiar caché** (incluye "datos de prueba") | `admin/cache` | `admin/cache/cache.component` | AdminService | /v1/admin/datos-prueba/{generar,avance,escenarios,preparar/{n},preparar-todas,quitar/{n},quitar-todas} |
| Sistema -> **🗂️ Menús y submenús** / **🛡️ Gestión de roles** | `gestion-menu`, `gestion-menu/roles` | `menu-admin/...` | - | los NOMBRES de pantallas/acciones vienen de BD |
| Sistema -> **🎨 Personalización** | `personalizacion` | `tema-admin/gestion` | - | - |
| Ventas legacy (sin menú en navbar) | `ventas/venta` | `ventas/venta-producto/add-venta` | ProductoService | pantalla vieja con texto "Productos" (ver 4.1) |

Los textos de los menús de BD ("Modelos", "Agregar Modelo", "Tarjeta de modelo", "Ver todos los modelos") NO están en el front: vienen de `submenu`/`accion_submenu` del back. No pude confirmarlos desde el front y no los invento: coordinar UPDATE en BD con el back (la clave `ver-todos-los-modelos` es identificador y no cambia). Hasta que se actualicen, Gestión de roles dirá "Modelos" y el navbar "Productos". Nota: el navbar es el hardcodeado, no el de BD (el de BD solo filtra con `tienePantalla`).

## 3. Cambios de texto A (archivo:línea | actual | propuesto)

### 3.1 Navbar
| archivo:línea | actual | propuesto |
|---|---|---|
| navbar/navbar.component.html:66-67 | `🔍 Modelos` / title "Lista de modelos: el producto base, sin talla ni color (ej. Blusa Zara)" | `🔍 Productos` / "Lista de productos: el producto base, sin talla ni color (ej. Blusa Zara)" |
| navbar.component.html:68-69 | `➕ Agregar modelo` (title ya dice "producto base") | `➕ Agregar producto` |
| navbar.component.html:70-71 | title "...a partir de un modelo que ya existe..." | "...a partir de un producto que ya existe..." (el texto `🧩 Agregar artículo` queda) |
| (comentarios) navbar.component.html:45,54-57 | "Modelo agrupa / Artículo es lo que se vende" | solo comentario (B); actualizar por orden |

### 3.2 Productos (lista) `productos/buscar` -> all.component
| archivo:línea | actual | propuesto |
|---|---|---|
| productos/producto/all/all.component.html:9 | `Modelos` | `Productos` |
| all.component.html:10 | `Catálogo de modelos (producto base)` | `Catálogo de productos (producto base)` (o quitar "(producto base)") |
| all.component.html:26 | placeholder `Buscar modelo por nombre o código…` | `Buscar producto por nombre o código…` |
| all.component.html:48 | title `Descargar Excel de modelos sin artículos` | `Descargar Excel de productos sin artículos` |
| all.component.html:258 | title `Agregar artículos a este modelo (cada talla o color con su stock)` | `...a este producto...` |
| all.component.ts:707 | `El modelo tiene <b>X</b> y sus artículos ya tienen <b>Y</b>.` | `El producto tiene ...` |
| all.component.ts:708 | `Sube el stock del modelo (✏️ Actualizar) o quítale stock a un artículo.` | `...del producto...` |
| all.component.html:285 | comentario | B |

### 3.3 Agregar / Actualizar producto
| archivo:línea | actual | propuesto |
|---|---|---|
| productos/producto/add/add.component.ts:31 | `nombreCard = 'Agregar Modelo'` | `'Agregar Producto'` |
| productos/producto/update/update.component.html:5 | `'Actualizar Modelo'` | `'Actualizar Producto'` |
| add.component.html:226 | `💾 Actualizar modelo` / `💾 Guardar modelo` | `💾 Actualizar producto` / `💾 Guardar producto` (ya no choca con el botón de artículo, que dice "Guardar artículo") |
| add.component.ts:371 | `¡Modelo actualizado!` / `¡Modelo guardado!` | `¡Producto actualizado!` / `¡Producto guardado!` |

### 3.4 Modal "¿Cuántos artículos?" (shared/alta-articulos) — lo abren Agregar/Actualizar producto, 🧩 Artículos de la lista y Agregar artículo
| archivo:línea | actual | propuesto |
|---|---|---|
| alta-articulos.component.html:12, 51, 197 | `Stock del modelo:` | `Stock del producto:` |
| html:18 | `¿Cuántos artículos quieres hacer de este modelo?` | `...de este producto?` |
| html:24 | `Un modelo no se vende solo: se venden sus artículos (...)` | `Un producto no se vende solo: ...` |
| html:25 | `Si eliges Ahora no, el modelo ya quedó guardado...` | `...el producto ya quedó guardado...` |
| html:28 | `Al modelo no le queda stock libre. Súbele stock (✏️ Actualizar → «Actualizar stock»)...` | `Al producto no le queda stock libre...` |
| html:57 | `Al modelo no le queda stock libre. Súbele stock (✏️ Actualizar) o quítale stock a uno de sus artículos.` | `Al producto ...` |
| html:78-79 | `Cada artículo nace igual al modelo ... se queda como el modelo.` | `...igual al producto ... como el producto.` |
| html:181 | `Igual al modelo:` | `Igual al producto:` |
| html:184 | `El modelo no tiene foto: este artículo no sale en la tienda...` | `El producto no tiene foto: ...` |
| alta-articulos.component.ts:128 | `✅ Modelo actualizado` / `✅ Modelo guardado` (#aaTitulo; lo lee el e2e) | `✅ Producto actualizado` / `✅ Producto guardado` |
| ts:191 | `la del modelo` | `la del producto` |
| ts:226 | `(el modelo no tiene)` | `(el producto no tiene)` |
| ts:238 | `Igual al modelo` | `Igual al producto` |

### 3.5 Agregar artículo `tienda/venta` (variante/agregar)
| archivo:línea | actual | propuesto |
|---|---|---|
| agregar.component.html:11 | `Lo que se vende: un modelo con su talla y color. Solo el modelo es obligatorio` | `...un producto con su talla y color. Solo el producto es obligatorio` |
| html:19 | label `Modelo *` (+ "— el producto base al que le vas a dar talla y color" en :20) | `Producto *` (la aclaración ya dice "producto base": queda) |
| html:28 | placeholder `Buscar modelo por nombre o código…` (lo usa el e2e) | `Buscar producto por nombre o código…` |
| html:59 | title `Cambiar modelo` | `Cambiar producto` |
| html:68 | `⛔ Este modelo está deshabilitado` | `⛔ Este producto está deshabilitado` |
| html:73 | `✅ Habilitar modelo` | `✅ Habilitar producto` |
| html:76 | `...Habilitar en Catálogo → 🔍 Modelos que lo habilite.` | `...Catálogo → 🔍 Productos...` |
| html:80-82 | `📷 Este modelo no tiene foto ... o al modelo en Catálogo → 🔍 Modelos → ✏️ Actualizar.` | `📷 Este producto ... al producto en Catálogo → 🔍 Productos → ✏️ Actualizar.` |
| html:93 | `Stock total del modelo` | `Stock total del producto` |
| html:98 | `Agregar (+) o quitar (−) al modelo` | `... al producto` |
| html:106 | `Guardar en el modelo` | `Guardar en el producto` |
| html:117 | `...agrégale stock al modelo para hacer artículos nuevos.` | `...al producto...` |
| html:126 | `El modelo quedaría en N` | `El producto quedaría en N` |
| html:129 | `No se puede dejar el modelo en N: ya tiene M repartidos.` | `...el producto en N...` |
| html:133 | `⚠️ Modelo descuadrado` | `⚠️ Producto descuadrado` |
| html:142 | `Súbele stock al modelo en el campo de arriba.` | `...al producto...` |
| html:197 | `(aplica a todos; viene la del modelo y la puedes cambiar)` | `...la del producto...` |
| html:366 | `Falta elegir el <b>modelo</b> en el buscador de arriba.` | `...el <b>producto</b>...` |
| agregar.component.ts:249 | `Modelo habilitado` | `Producto habilitado` |
| ts:267 | `Stock del modelo: N. Tiene X con Y piezas.` | `Stock del producto: N. Tiene X artículos con Y piezas.` |
| ts:289 / 294 | `Se quitaron N al modelo` / `Se agregaron N al modelo` | `... al producto` |
| ts:726 | `Selecciona un modelo` | `Selecciona un producto` |
| ts:730 | `Primero habilita el modelo` | `Primero habilita el producto` |
| ts:735-736 | `Revisa el stock del modelo` / `No se puede dejar el modelo en N: ya tiene M repartidos en sus artículos.` | `...del producto` / `...dejar el producto en N...` |

### 3.6 Editar artículo `tienda/update` (variante/update-variante)
| archivo:línea | actual | propuesto |
|---|---|---|
| update-variante.component.html:12 | `Todos los campos son opcionales excepto el modelo` | `...excepto el producto` |
| html:20 | label `Modelo *` | `Producto *` |
| html:28 | placeholder `Buscar modelo por nombre o código…` | `Buscar producto por nombre o código…` |
| html:63 | `⚠️ Modelo descuadrado` | `⚠️ Producto descuadrado` |
| html:73 | `...primero agrégale stock al modelo.` | `...al producto.` |
| html:76-77 | `Con el modelo descuadrado ... agregarle stock al modelo.` | `Con el producto descuadrado ... al producto.` |
| html:82 | `➕ Agregar stock al modelo` | `➕ Agregar stock al producto` |
| update-variante.component.ts:236,237 | `El modelo «nombre» tiene N libre(s)...` | `El producto «nombre» tiene...` |
| ts:242 | title `Al modelo no le alcanza el stock` | `Al producto no le alcanza el stock` |
| ts:243 | `...que le sume stock al modelo, o baja lo que le pides a este artículo.` | `...al producto...` |
| ts:250 | `➕ Agregar stock al modelo` | `➕ Agregar stock al producto` |
| ts:252 | `¿Cuántas piezas le agregas al <b>modelo</b>?...` | `...al <b>producto</b>?...` |
| ts:253 | `Esto se guarda solo en el modelo. Este artículo no cambia:` | `...en el producto...` |
| ts:260 | botón `Agregar al modelo` | `Agregar al producto` |
| ts:284 | `Se agregaron N al modelo` | `... al producto` |
| ts:285 | `Ahora el modelo tiene X libres de Y.` | `Ahora el producto tiene...` |
| ts:290 | `...o agrega más al modelo.` | `...al producto.` |

### 3.7 Detalle de artículo (variante/detalle-variante) — acción admin
| archivo:línea | actual | propuesto |
|---|---|---|
| detalle-variante.component.html:243 | `Convertir en modelo propio` | `Convertir en producto propio` |
| html:254 | `⚡ Convertir en modelo propio #id` | `⚡ Convertir en producto propio #id` |
| html:259 | `Este artículo pasará a tener su propio modelo aparte...` | `...su propio producto aparte...` |
| html:277-278 | label y placeholder `Nombre del modelo nuevo` | `Nombre del producto nuevo` |
| detalle-variante.component.ts:440 | `El nombre del modelo nuevo es obligatorio.` | `El nombre del producto nuevo es obligatorio.` |
| ts:473 | `Modelo nuevo creado con ID #...` | `Producto nuevo creado con ID #...` |

### 3.8 Tienda (admin sobre la card, buscar.component) — precios y bajas
| archivo:línea | actual | significa | propuesto |
|---|---|---|---|
| variante/buscar/buscar.component.ts:898 | `...volvió al disponible del modelo` | el producto (padre) | `...del producto` |
| ts:899-900 | `(y si el modelo ya no tiene disponible, primero hay que subirle stock al modelo)` | padre | `...el producto...` / `...al producto` |
| ts:980, 1038 | `Pónselo al editar el modelo` | padre | `...al editar el producto` |
| ts:991 | `Los demás del modelo se quedan igual` | los demás artículos del mismo producto | `Los demás del producto se quedan igual` |
| ts:1009 | botón `Usar el del modelo` | precio del padre | `Usar el del producto` |
| ts:1081 | `Vuelve al precio del modelo` | padre | `Vuelve al precio del producto` |
| **buscar.component.html:218** | title `Dar de baja este modelo` | **es el ARTÍCULO** | `Dar de baja este artículo` (sección 5) |
| **buscar.component.ts:933** | `Modelo dado de baja` | **es el ARTÍCULO** | `Artículo dado de baja` (sección 5) |

### 3.9 Otras pantallas
| archivo:línea | actual | propuesto |
|---|---|---|
| shared/ayuda-pantalla/ayuda-pantallas.catalog.ts:38 | titulo `Agregar Modelo` | `Agregar Producto` |
| catalog:39 | `Da de alta el artículo base del catálogo: el modelo, sin talla ni color todavía.` | `Da de alta el producto base del catálogo, sin talla ni color todavía.` ("artículo base" también estorba: ahora artículo = lo que se vende) |
| catalog:41 | `Registrar un modelo (producto base)...` | `Registrar un producto (ej. «Blusa Zara») que aún no existe...` |
| catalog:43 | `el modelo por sí solo todavía no se puede vender` | `el producto por sí solo...` |
| catalog:47-48 | `lo que SE VENDE (el modelo ya con talla y color). Aquí se crea el modelo base... Si el modelo ya existe,...` | `lo que SE VENDE (el producto ya con talla y color)`: mejor `...(un producto con su talla y color, el artículo). Aquí se crea el producto base... Si el producto ya existe,...` |
| catalog:51 | `un mismo modelo suele tener muchas combinaciones` | `un mismo producto...` |
| catalog:58 | `Crea lo que realmente se vende: un modelo ya con su talla...` | `...un producto ya con su talla...` |
| catalog:60 | `Darle tallas y colores a un modelo que ya existe` | `...a un producto...` |
| catalog:65-67 | `Agregar Modelo (Catálogo)`, `Allá se crea el modelo`, `el modelo tiene que existir antes` | `Agregar Producto (Catálogo)`, `...el producto...` |
| catalog:70 | `El único campo obligatorio es el modelo` | `...el producto` |
| catalog:75-78 | titulo `Buscar Modelos`; `Listado de los modelos del catálogo...`; `Encontrar un modelo por nombre...` | `Buscar Productos`; `...productos...`; `...un producto...` |
| catalog:83 | `Aquí ves modelos (el producto base). Allá ves los artículos...` | `Aquí ves productos (el agrupador). Allá ves los artículos que se venden...` |
| catalog:107 | `Agregar Modelo / Agregar artículo` | `Agregar Producto / Agregar artículo` |
| catalog:28-32 (comentario de cabecera) | cita "Agregar Modelo", "Un modelo puede tener muchos productos colgando" | B, actualizar comentario |
| admin/reconciliacion-imagenes/...html:110 | `No borra productos ni modelos — un producto sin imagen no se elimina...` | `No borra productos ni artículos — ...` |
| reconciliacion html:174 | contador `Productos revisados` (el de `variantesRevisadas`) | `Artículos revisados` (ver colisión 4.1 nº2) |
| admin/diagnostico-imagenes/...html:19 | `la foto de un modelo o un artículo` | `...de un producto o un artículo` |
| diagnostico html:23 | `Busca el modelo o el artículo` | `Busca el producto o el artículo` |
| diagnostico html:30 | tab `📦 Modelo` | `📦 Producto` |
| diagnostico html:50 | placeholder `Buscar modelo por nombre o código…` | `Buscar producto por nombre o código…` |
| diagnostico html:79 | `No se encontraron modelos con "..."` | `No se encontraron productos con "..."` |
| diagnostico html:85 | `Modelo seleccionado:` | `Producto seleccionado:` |
| diagnostico-imagenes.component.ts:169 | `No se pudo consultar el diagnóstico del modelo` | `...del producto` |
| admin/cache/cache.component.html:22 | `(imágenes, modelos, artículos, clientes, tipos de pago…)` | `(imágenes, productos, artículos, ...)` |
| cache html:67 | `Crea modelos, artículos y pedidos de prueba...` | `Crea productos, artículos...` |
| cache html:74 | label `Modelos` | `Productos` (el modelo de datos `plan.modelos` NO se renombra) |
| cache html:79, 83 | `Artículos por modelo (mín.)` / `(máx.)` | `Artículos por producto (mín./máx.)` |
| cache html:109 | `Modelos: N de M` | `Productos: N de M` |
| cache html:125, 126, 161 | `(modelos, artículos y pedidos)`, `sus modelos se dan de baja`, `Los modelos y artículos...` | `productos` |
| admin/cache/cache.component.ts:83 | `<b>N</b> modelos con...` | `productos` |
| cache.ts:111-112 | `Los modelos y artículos con marca "Prueba QA" dejan de salir en la tienda y en Modelos... ningún modelo o artículo real` | `Los productos y artículos ... y en Productos... ningún producto o artículo real` |
| cache.ts:157, 184, 211 | `modelos, artículos y pedidos`, `sus modelos se dan de baja` | `productos` |
| cache.ts:237 | `case 'MODELO': return 'modelo'` | `return 'producto'` (el literal `'MODELO'` es contrato: NO cambia) |
| cache.ts:263 | `N pedido(s) cancelado(s) y M modelo(s) dado(s) de baja.` | `... producto(s) dado(s) de baja.` |
| carga-imagenes/carga-imagenes.component.html:186 | label `Nombre del modelo` | `Nombre del producto` (el resto de la pantalla ya dice "producto borrador": coincide) |
| admin/promociones/gestion-promociones.component.html:59 | `duplicados del mismo modelo con el stock repartido` | `...del mismo producto...` |
| palabras-clave/gestion/gestion-palabras-clave.component.html:5 | `búsqueda de modelos y artículos` | `búsqueda de productos y artículos` |
| palabras-clave/gestion/gestion-palabras-clave.component.ts:120 | `Los modelos y artículos que la usan quedarán sin categoría.` | `Los productos y artículos...` |
| menu-admin/gestion/gestion-menu.component.html:78 | placeholder `Nombre de la pantalla (ej. Modelos)…` | `(ej. Productos)…` |
| tema-admin/gestion/gestion-personalizacion.component.html:245 | `Cuerpo de la tarjeta — así se ve un artículo o modelo.` | `...un artículo o producto.` |

### 3.10 "Producto" que YA dice Producto y significa Modelo (sin cambio; hoy son inconsistentes con la pantalla "Modelos", quedarán correctos)
- productos/producto/all/all.component.html:168 title `Dar de baja este producto`; :149 `No se encontraron productos`.
- all.component.ts:246 `Error al recargar productos`; :249 `Producto dado de baja`; :254 `No se pudo dar de baja el producto`; :630 `Producto habilitado/deshabilitado`; :667 `Productos habilitados/deshabilitados`; :816 `No se encontraron productos`; :850, :863 `Error al cargar más productos` / `No se pudo cargar más productos.`
- add.component.html:13 `El producto base (ej. Blusa Zara)...`; :27 `Nombre del producto`; :92 `Descripción breve del producto…`; :176 `Imágenes del producto`. add.component.ts:260 `Este producto es un borrador de Carga rápida de imágenes`; :364 `No se pudo guardar el producto.`
- update.component.html:11 `🖼️ Imágenes del producto`; :20 `Este producto no tiene imágenes cargadas`.
- busca.component.ts:51 `No se pudo buscar el producto.`
- detalle-producto.component.ts:92 `¿Deseas compartir las imágenes de este producto con sus artículos?`; html:50 alt `Imagen producto`.
- reconciliacion html:170 `Productos revisados` (el de `productosRevisados`; correcto) y :28,34 `ID de producto`.
- carga-imagenes: ts:415-417 `Producto publicado`, ts:441 `Se borra el producto, su artículo y la imagen`, html:8, 35, 120, 160, 170 `Producto #`, `Completar producto #` (producto padre borrador).
- Navbar: tras el cambio, "Productos" (Catálogo) y "Tienda".

## 4. Colisiones

### 4.1 ADMIN (lo que ve el admin hoy y choca con "Producto" = Modelo)
| # | archivo:línea | qué dice hoy / qué significa | qué conviene |
|---|---|---|---|
| 1 | qr-ventas-jade/qr-ventas-jade.component.ts:22 | `👜 Producto o artículo` (ayuda: "Lleva al detalle de un artículo de la tienda"). Es el artículo. Hoy ya es ambiguo | Cambiar a `👜 Artículo`. Con el renombrado "Producto o artículo" pasaría a ser dos cosas distintas |
| 2 | admin/reconciliacion-imagenes/reconciliacion-imagenes.component.html:170 y 174 | Dos contadores con la MISMA etiqueta `Productos revisados`: el primero es `productosRevisados` (modelo), el segundo `variantesRevisadas` (artículo). Ya hoy confuso | Primero `Productos revisados`, segundo `Artículos revisados` (cambio obligatorio) |
| 3 | variante/buscar/buscar.component.html:218 y .ts:933 (Tienda, admin) | `Dar de baja este modelo` / `Modelo dado de baja` pero es un ARTÍCULO. Si solo se hace buscar/reemplazar quedaría "producto" para un artículo | Cambiar a "artículo" (sección 5) |
| 4 | documentos/carga-archivo/carga-archivo.component.html:22 y :113 (Cargar Excel) | Columna `nombreProducto` (nombre de la columna del Excel, contrato) y `✅ Todos los productos se insertaron sin errores.`. La ayuda (catalog:100) dice "Alta masiva de artículos". No sé qué crea el back (producto padre + artículo, ver `/v1/productos/save` de la carga) | Decisión del dueño: `Todos los artículos se insertaron...` si crea artículos; dejar `nombreProducto` (es columna, no se renombra) |
| 5 | ventas/venta-producto/add-venta/add-venta.component.html:6, .ts:135,155,184,259 y ventas/venta-producto/buscar-venta/buscar-venta.component.ts:46 | Pantalla legacy `ventas/venta`: `Productos`, `Sin productos`, `Agrega al menos un producto.`, `Este producto no tiene stock disponible.`, columna `Nombre Producto`. No tiene entrada en navbar. Vende lo que busca ProductoService (padre) | Dejar (con el renombrado "Producto" es correcto) y confirmar con el dueño si la pantalla sigue en uso |
| 6 | buscador/buscar-generico/buscar-generico.component.html:6, demo-tablas/*, mis-pedidos placeholder (ya dice "artículo") | `Buscar productos...` / `Producto` (demo). Sin uso claro / demo | Dejar. `demo-tablas` es una demo (`/demo-tablas`) |
| 7 | Menú de BD (Gestión de roles / Gestión de menú) | "Modelos", "Agregar Modelo", acción "Ver todos los modelos", "Tarjeta de modelo" | Back: UPDATE de `submenu`/`accion_submenu`. Mientras tanto no coinciden con el navbar |

NO hay colisiones del tipo "botón `🧩 Productos`" ni "Rifa de productos" ni "Todos nuestros productos" en el admin de qa: eso ya se limpió. Las demás pantallas admin (Rifas, Promociones, Abonos, Pedidos, Reportes, Gastos, Dashboard, Venta directa) ya dicen "artículo". Verificado por búsqueda de texto visible.

### 4.2 CLIENTE FINAL (tienda): "producto" = artículo — decisión del dueño
En la tienda el cliente nunca ve "modelo". Hoy dice "producto" para lo que compra (que en el vocabulario del dueño es el artículo). Si "Producto" pasa a ser el padre solo en el admin, el cliente seguiría leyendo "producto" para la prenda: eso es correcto y natural. Mi recomendación: **dejarlo** (el cliente compra "productos"; "artículo" es vocabulario interno), salvo que el dueño quiera unificar.

| archivo:línea | texto |
|---|---|
| variante/buscar/buscar.component.html:9 | `Tienda / Todos nuestros productos` |
| buscar.component.html:196-197 | `No se encontraron productos con "..."` / `No se encontraron productos` |
| variante/detalle-variante/detalle-variante.component.html:103 | fallback `?? 'Producto'` (pie de foto del cliente) |
| detalle-variante.component.html:228 | `Todavía no hay reseñas de este producto.` |
| detalle-variante.component.ts:150, 157 | `Este producto ya no está disponible.` |
| detalle-variante.component.ts:161-162 | `No pudimos abrir este producto desde un enlace directo...` / `No se pudo cargar el producto.` |
| variante/venta-variante/venta-variante.component.ts:433 | `El precio de uno o más productos cambió mientras tenías el carrito abierto...` |
| favoritos/favoritos.component.html:6, 26 | `Productos que guardaste para después` / `Toca el corazón de cualquier producto...` |
| chatbot/chatbot.component.ts:127, 231 | `Pregúntame sobre productos, precios y disponibilidad.` / `...agregar productos al carrito.` |
| legal/terminos/terminos.component.html:25, 51, 54-56, 104-105, 116-117, 132 | "Productos, precios y disponibilidad", etc. (10 apariciones) — **NO cambiar** (texto legal) |
| legal/privacidad/privacidad.component.html:35, 66, 98 | idem — **NO cambiar** |
Nota: el cliente ve "Tienda" en el título de la pantalla y "Artículo" solo en `alt` de imagen y en pantallas de pedidos ("artículo" en detalle de pedido ya cambiado). Mezcla actual en el cliente: "producto" (tienda) y "artículo" (pedidos, favoritos img alt, rifas). Si el dueño quiere uniformar con "artículo" son ~11 textos (no legales) de la tabla de arriba.

## 5. "Modelo" en pantalla que en realidad habla de un ARTÍCULO (deben decir "artículo", NO "producto")
1. variante/buscar/buscar.component.html:218 — title `Dar de baja este modelo` -> `Dar de baja este artículo`. (Botón ✕ de la card de Tienda; llama `confirmarDarDeBaja(v)`, DELETE /v2/articulos/deleteBy/{id}; el Swal anterior ya dice `¿Dar de baja <nombre> · talla · color?`.)
2. variante/buscar/buscar.component.ts:933 — Swal `Modelo dado de baja` -> `Artículo dado de baja`.
3. variante/service/variante.service.ts:184 (comentario) — "Baja lógica del modelo" -> es del artículo (B, solo comentario).
Casos dudosos (se quedan como "producto"): ts:991 `Los demás del modelo` (los demás artículos DEL producto, correcto "producto"); ts:1009/1081 (precio heredado del padre).
Otros "modelo" ambiguos revisados: tema-admin/...:245 (`un artículo o modelo` -> `un artículo o producto`), reconciliación :110 (`productos ni modelos` -> `productos ni artículos`; pero ojo: "producto" = padre ahora, "modelo" desaparece, así que `No borra productos ni artículos`).

## 6. e2e y specs
Carpeta `e2e/` (Playwright). Specs Karma (`*.spec.ts`) con "modelo"/"producto": solo `app.component.spec.ts` (título `producto-scaner`, no visible), `preferencia-filtro.service.spec.ts`, `producto.service.spec.ts`, `detalle-producto.component.spec.ts`: ninguno depende de texto visible de "modelo".

| archivo:línea | dependencia | se rompe cuando |
|---|---|---|
| e2e/support/modelo.ts:31 | `getByRole('button', { name: /Guardar modelo/ })` (productos/agregar) | cambia a `Guardar producto` |
| e2e/support/modelo.ts:34 | `#aaTitulo toHaveText('✅ Modelo guardado')` | cambia a `✅ Producto guardado` |
| e2e/support/articulo.ts:21 | `getByRole('button', { name: /Guardar producto/ })` en tienda/venta | **YA roto hoy en qa**: el botón dice `💾 Guardar artículo` (agregar.component.html:354-356). Cambiar a `/Guardar artículo/`. Ojo: tras el renombrado `/Guardar producto/` pegaría en OTRA pantalla (Agregar producto), coincidencia peligrosa |
| e2e/support/articulo.ts:22 | `toHaveText('¡Artículo creado!')` | OK con qa (agregar.component.ts:811) |
| e2e/support/articulo.ts:28 | `getByPlaceholder('Buscar modelo por nombre o código…')` (tienda/venta) | cambia a `Buscar producto por nombre o código…` |
| e2e/tests/login.spec.ts:7, 14-15 | nombre "lista de modelos"; `getByPlaceholder('Buscar nombre o código…')` | YA roto hoy: el placeholder de /productos/buscar es `Buscar modelo por nombre o código…` (ver nota); tras el cambio: `Buscar producto por nombre o código…` |
| e2e/tests/modelo.spec.ts:4, 10 | nombres de test; `getByPlaceholder('Buscar nombre o código…')` | idem |
| e2e/tests/descuento.spec.ts:18 | `getByTitle('Cambiar el precio de este artículo (solo este)')` | no depende de modelo |
| e2e/tests/articulo.spec.ts:5, 20; descuento.spec.ts:6; README.md; datos.ts:4; support/modelo.ts:11-15 | comentarios y nombres de test con "modelo" | solo texto; archivos y símbolos (`crearModelo`, `ModeloCreado`, `modelo.ts`) son código y no se renombran |
Nota (verificado): `Buscar nombre o código…` solo existe en Tienda (variante/buscar/buscar.component.html:34). En `/productos/buscar` el placeholder es `Buscar modelo por nombre o código…` (all.component.html:26), que NO contiene esa subcadena. Por tanto `login.spec.ts:15` y `modelo.spec.ts:10` (que van a /productos/buscar) ya fallan hoy en qa; con el renombrado deben usar `Buscar producto por nombre o código…`. `descuento.spec.ts:10` va a /tienda/buscar y sí coincide. No lo ejecuté (solo lectura).

## 7. Ayudas y CLAUDE.md
**Catálogo de ayudas** (shared/ayuda-pantalla/ayuda-pantallas.catalog.ts): se define por `ruta` de navbar. Tiene 3 entradas del Catálogo con vocabulario de Modelo (lín. 37-84 y 107). Líneas a reescribir: 38, 39, 41, 43, 47-48, 51, 58, 60, 65-67, 70, 75-78, 83, 107 y el comentario de cabecera 3-4, 28-32. En la entrada `tienda/venta` (57) el título ya es "Agregar artículo". La ayuda de `admin/diagnostico-imagenes` (413-427) dice solo "artículo": sin cambio. Cambios de tono: "el artículo base" (línea 39) hay que evitarlo.
Ruta consistente: sigue `ruta: 'productos/agregar'`, no se toca.

**CLAUDE.md del front** (12 442 líneas):
- Líneas 4623-4730 (sección "TAXONOMÍA DE NOMBRES", 2026-07-16) define el vocabulario INVERSO: tabla en 4662-4669 ("`Producto` (padre) = Modelo", "`Variante` = Producto"), regla de oro 4670 ("Modelo agrupa, Producto se vende"), "Traducción mental" 4681 (`variante` -> "producto"; `producto` -> "modelo"), y el menú 4685-4694 ("Inventario", "Modelos", "Agregar modelo", "Agregar producto", "Rifa de productos"). Ya está desactualizado hoy (artículo) y quedaría al revés tras el cambio. Hay que reescribir: Producto(padre)=Producto, Variante=Artículo, traducción mental `variante` -> "artículo", y la tabla del menú (Catálogo -> Productos / Agregar producto / Agregar artículo / Rifa de artículos).
- No encontré en CLAUDE.md una sección del renombrado variante->artículo (grep de "Artículo ... oficial|renombr" sin resultados). Línea 12140: "FIX MODELOS — SCROLL INFINITO ..." (título de sección histórica con "Modelos"; histórico, no cambiar).
- Otras menciones de "modelo" en CLAUDE.md son del sentido TS/dato (609, 1681, 2930, 3211...), no del vocabulario.
- Otros .md con vocabulario viejo (no tocar): TAXONOMIA_NOMBRES_BACK.md, PROPUESTA_REORGANIZACION_MENU.md, PLAN_PERMISOS_PANTALLAS.md, CAMBIOS_FRONT.md, e2e/README.md.
- `.claude/skills/nombres-reales`: no existe en esta copia (`.claude/` solo tiene `settings.json` y `worktrees`).

---

# ANEXO B — Back: mensajes, mapa de endpoints y contrato

## 1) Resumen y conteos

| Clase | Qué es | Conteo en `qa` (el previo, sobre dev, decía 35 + ~22) |
|---|---|---|
| **A-core** | Mensajes de back que lee el ADMIN y dicen "modelo" por el producto base | **18 líneas Java** (14 en el previo; +4 nuevas por el renombrado de hoy) |
| **A-QA** | Panel "Datos de prueba" (solo QA, lo ve el dueño/tester) | **21 líneas Java** (igual que el previo, mismos números de línea) |
| **A-BD** | Textos de menú / Gestión de roles que viven en la base y se siembran con SQL | **37 líneas SQL no comentadas** en 15 migraciones (15 de ellas son del script del renombrado de hoy) |
| B | Identificadores, logs, comentarios | ~330 líneas Java con "modelo" (344 total sin imports ni "modelo de lenguaje") |
| C | Otro significado (paquetes `dominio.modelo`, "modelo de lenguaje", "modelo de datos", "modelo viejo/nuevo" de entrega de flores) | ~310 imports + ~45 |

Diferencias importantes contra el informe previo (el renombrado variante->artículo ya cambió mensajes):
1. **El renombrado de hoy YA escribió "modelo" en 4 mensajes que antes decían "producto"** (commit `1cdabc4`): `ArticuloAVender.java:38`, `EditarArticulosService.java:276`, `PedidoServiceImpl.java:705`, y el `(articulo: %d, modelo: %d)` que ya estaba. Con Modelo->Producto vuelven a "producto", y **`ArticuloAVenderTest` y `EditarArticulosServiceTest` (que esperan "el producto está deshabilitado") vuelven a pasar** (hoy están rotos, anotado en TESTS_PENDIENTES.md:462-463).
2. Ya NO existen (quedaron corregidos por el renombrado): C-2 `PedidoServiceImpl:703` ("El artículo no existe en este pedido"), C-4 `AbonoServiceImpl:303` ("Ir pagando cancelado... el cliente ya se llevó los artículos"), C-12 `CargaImagenesServiceImpl:124` ("no tiene artículo asociado"), C-11 hoja Excel ahora "Productos Sin Artículos". `VarianteServiceImpl:1237-1241` ya dice "Artículos habilitados/deshabilitados".
3. **Discrepancia doc vs código**: `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.3 dice que `VarianteServiceImpl` ahora dice "El **modelo** N no tiene una imagen..." y "Stock insuficiente en el **modelo**". En `qa` el código dice `"El producto N no tiene una imagen para copiar a los artículos"` (VarianteServiceImpl:291) y el "Stock insuficiente en el modelo" ya no existe (reemplazado por el 422 de :837). **Nada que cambiar en :291** (ya dice "producto").
4. Cambios de menú en pantalla ya hechos que dejan **texto QA desactualizado hoy** (independiente de este rename): "🧩 Productos" -> "🧩 Artículos", "Agregar producto" -> "Agregar artículo" (ver 2.2).
5. La base de `qa` ya tiene textos con "modelo" escritos por `migration_renombre_articulo_etiquetas.sql` (corrida en QA 2026-10-20, CLAUDE.md:656), p. ej. `'Crear artículos desde el modelo (🧩 "Artículos" en la tarjeta)'` y `'Descargar Excel de modelos sin artículos'`. La migración nueva debe partir de ESOS textos, no de los de las migraciones viejas.

## 2) Textos A (archivo:línea | actual | propuesto)

### 2.1 Java, mensajes que lee el admin (A-core, 18 líneas)

| Archivo:línea | Actual | Propuesto | Estado vs previo |
|---|---|---|---|
| P/service/VarianteServiceImpl.java:266-267 | `"No alcanza el stock para crear %d artículo(s): el modelo tiene %d y sus artículos ya tienen %d%s. Puedes crear %d. Sube el stock del modelo o quítale stock a un artículo."` | "el producto tiene" / "Sube el stock del producto" | confirmado (404 `ExceptionDataNotFound`) |
| P/service/VarianteServiceImpl.java:751 | `"No tienes permiso para cambiar el stock del modelo. Pídele a alguien con permiso de editar modelos"` | "...del producto... editar productos" | confirmado |
| P/service/VarianteServiceImpl.java:762 | `"No se puede dejar el modelo en %d: ya tiene %d repartidos en sus artículos"` | "el producto" | confirmado |
| P/service/VarianteServiceImpl.java:837 | `"El modelo «%s» no tiene piezas libres para darle a este artículo: tiene %d libres y le pides %d más. "` | "El producto «%s»..." (422 `ExceptionStockInsuficiente`) | confirmado; NO tocar el final `(Disponible: %d, Solicitado: %d)` (lo asertan VarianteStockDevolucionTest:249 y VarianteStockValidacionTest:149) |
| P/service/VarianteServiceImpl.java:838 | `"Primero súmale stock al modelo en Catálogo → Modelos → ✏️ Actualizar → «Actualizar stock». "` | "...al producto en Catálogo → Productos → ✏️ Actualizar..." (el nombre del menú sale de la tabla `submenu`; debe coincidir con lo que se ponga en BD y en el front) | confirmado |
| P/service/VarianteServiceImpl.java:1241 | `"Artículos habilitados. Se ajustó el stock a lo que quedaba libre del modelo: "` | "...libre del producto: " | cambió: ya dice "Artículos habilitados" |
| H/stock/dominio/modelo/DisponibilidadStock.java:81 | `"Escribe cuánto stock agregar (+) o quitar (−) al modelo"` | "...al producto" | confirmado |
| H/stock/dominio/modelo/DisponibilidadStock.java:86 | `"No se puede quitar %d: el modelo solo tiene %d"` | "el producto solo tiene" | confirmado |
| H/stock/dominio/modelo/DisponibilidadStock.java:90 | `"No se puede dejar el modelo en %d: ya tiene %d repartidos en sus artículos"` | "el producto" | confirmado |
| H/stock/infraestructura/dto/DisponibilidadStockResponse.java:51 | `"Este modelo está descuadrado: tiene %d en total pero sus %d artículos suman %d."` (campo JSON `mensaje`) | "Este producto está descuadrado..." | confirmado |
| H/stock/aplicacion/servicio/AjustarStockModeloService.java:36 | `"El stock del modelo cambió mientras lo ajustabas (una venta o la edición de un pedido). ..."` | "El stock del producto cambió..." | confirmado |
| H/pedidoarticulo/dominio/modelo/ArticuloDisponible.java:48 | `"Solicitado: %d. Disponible: %d (articulo: %d, modelo: %d)"` | "(articulo: %d, producto: %d)" | confirmado; EditarArticulosServiceTest:230 solo busca "Solicitado: 5" |
| H/pedidoarticulo/infraestructura/salida/persistencia/MovimientoStockJpaAdapter.java:48 | `"Modelo no encontrado al mover stock: "` | "Producto no encontrado al mover stock: " | confirmado (prioridad baja) |
| H/pedidoarticulo/infraestructura/salida/persistencia/MovimientoStockJpaAdapter.java:50 | `noNegativo(..., "modelo " + producto.getId())` | `"producto " + ...` | confirmado (prioridad baja) |
| **H/articulo/dominio/modelo/ArticuloAVender.java:38** | `deshabilitado(nombre, "el modelo")` -> `'%s' ya no está a la venta: el modelo está deshabilitado o dado de baja. Quítalo del carrito para continuar.` | `"el producto"` | **NUEVO** (antes "el producto"); lo disparan `PedidoServiceImpl.articuloAVender` / `VentaServiceImpl` / `AbonoServiceImpl:366-369`. Texto "Quítalo del carrito" => también puede leerlo el CLIENTE (ver 4.2) |
| **H/pedidoarticulo/aplicacion/servicio/EditarArticulosService.java:276** | `"' ya no está a la venta: el modelo está deshabilitado o dado de baja"` (400 `EdicionPedidoException`) | "el producto está deshabilitado..." | **NUEVO** (antes "producto"); admin |
| **P/service/PedidoServiceImpl.java:705** | `"La linea N del pedido #M no es de ese modelo"` (RuntimeException al quitar línea) | "no es de ese producto" | **NUEVO** (antes "producto"); admin. OJO: el 703 vecino dice "El artículo no existe en este pedido" |

Ya dicen "producto" para el producto base y quedan bien tras el rename (no tocar): VarianteServiceImpl:251, 291, 383, 755, 792; ProductosServiceImpl:262, 297, 389, 392, 481, 593, 754, 756, 778, 801, 839; CargaImagenesServiceImpl:122, 124, 144, 302, 325, 330, 373, 377, 421; ProductosControllerImpl:123, 133-134 ("Producto(s) habilitado(s)/deshabilitado(s) correctamente"); ImageneController:169, 176; PrecioInvalidoException:39; ProductoSinStockConocidoException:11; PedidoServiceImpl:250, 258, 643, 730.

### 2.2 Panel de datos de prueba QA (A-QA, 21 líneas, `H/datosprueba/`)

Mismos archivos y líneas que el previo (verificados en `qa`):

| Archivo:línea | Actual | Propuesto |
|---|---|---|
| dominio/modelo/PlanDeDatos.java:23 | `"Los modelos van de 1 a "` | "Los productos van de 1 a " |
| dominio/modelo/PlanDeDatos.java:26 | `"Los artículos por modelo van de 1 a "` | "...por producto..." |
| dominio/modelo/PruebaQuitada.java:33 | `" modelo dado de baja"` / `" modelos dados de baja"` | "producto dado de baja" / "productos dados de baja" |
| dominio/modelo/Avance.java:44 | `"Creando modelos y artículos"` (campo `fase`) | "Creando productos y artículos" |
| dominio/modelo/CatalogoAleatorio.java:102 | `". Modelo de prueba generado para QA."` (se guarda en la descripción del producto) | ". Producto de prueba generado para QA." |
| dominio/modelo/EscenarioDePrueba.java:18 | `"🧩 Productos del modelo: stock 3 y un artículo con 1"` | "🧩 Artículos del producto: ..." (el botón ya se llama 🧩 Artículos) |
| dominio/modelo/EscenarioDePrueba.java:22 | `"Agregar artículo: modelo deshabilitado y sin stock libre"` | "...producto deshabilitado..." |
| dominio/modelo/EscenarioDePrueba.java:24 | `"...y un modelo agotado"` | "...un producto agotado" |
| aplicacion/servicio/PrepararPruebaService.java:212 | `"Modelo con stock 3 y un artículo con stock 1 con foto"` | "Producto con stock 3 ..." |
| ...:213 | `"Catálogo → 🔍 Modelos → escribe \"QA Prueba 14\" → 🧩 Productos. ..."` | "Catálogo → 🔍 Productos → ... → 🧩 Artículos." (hoy ya está desactualizado: el botón se llama 🧩 Artículos) |
| ...:231 | `"Modelo con stock 5: talla CH habilitada con 4"` | "Producto con stock 5: ..." |
| ...:233 | `"Modelo con stock 10 y otra categoría: ..."` | "Producto con stock 10..." |
| ...:235 | `"...Pasos 2b y 2c: Agregar producto con \"QA Prueba 15 M5\"..."` | "Agregar artículo ..." (ya desactualizado: el menú dice "Agregar artículo"; tras el rename "Agregar producto" sería OTRA pantalla) |
| ...:270 | `"Modelo deshabilitado (su artículo sigue habilitado)"` | "Producto deshabilitado..." |
| ...:271 | `"Modelo habilitado con stock 4, todo repartido en su artículo: libre 0"` | "Producto habilitado..." |
| ...:273 | `"Rol con 🧩 Agregar producto y \"Ver todos los modelos\", sin \"Habilitar\" de Modelos (18.1 paso 3 y 18.2 paso 6)"` | "Rol con 🧩 Agregar artículo y \"Ver todos los productos\", sin \"Habilitar\" de Productos (...)". La CLAVE `ver-todos-los-modelos` de la línea 272 NO se toca |
| ...:274 | `"Catálogo → 🧩 Agregar producto → escribe \"QA Prueba 18 M-OFF\"..."` | "Catálogo → 🧩 Agregar artículo → ..." |
| ...:347 | `"Modelo con stock 0 (agotado), para la 20.3"` | "Producto con stock 0 ..." |
| ...:348 | `"...20.3: Catálogo → 🔍 Modelos → \"QA Prueba 20 Agotado\"."` | "...🔍 Productos..." |
| infraestructura/entrada/rest/DatosPruebaController.java:62 | `" artículos de prueba dados de baja (y sus modelos)"` | "(y sus productos)" |
| infraestructura/entrada/rest/PrepararPruebaController.java:89 | `pedidos + " pedidos cancelados y " + modelos + " modelos dados de baja"` | "... productos dados de baja" |

También, con "modelo" que NO se renombra: `CatalogoPreparado.MODELO = "MODELO"` (valor del campo `tipo`), `GenerarDatosPruebaRequest.modelos`, `Avance.modelosPedidos/modelosCreados`, `PruebaQuitada.modelosDadosDeBaja`, `DatosDelModelo.modeloId`, `CatalogoPreparado.modeloId` (contrato JSON con el front).

### 2.3 Base de datos (A-BD): hace falta migración NUEVA, no editar las corridas

37 líneas no comentadas en SQL/ (15 archivos; 3 solo comentan). Estado a partir del cual se escribe la migración nueva = lo que dejó `migration_renombre_articulo_etiquetas.sql` (corrida QA 2026-10-20):
- `migration_renombre_articulo_etiquetas.sql` (líneas 58, 60, 73, 78, 100, 106, 108, 111, 113, 128, 153, 178, 203, 228, 253): deja en BD `'Formulario para agregar un artículo ... a partir de un modelo ya creado...'`, `'...imagen de un modelo o de un artículo'`, `'...buscar modelos y artículos'`, tema_variable `'Fondo del recuadro de búsqueda y filtros (Tienda y Modelos)'`, `'Crear artículos desde el modelo (🧩 "Artículos" en la tarjeta)'`, `'Deja crear artículos ... a partir de un modelo. Aparece en Modelos (productos/buscar): ... cada tarjeta de modelo.'`, `'Descargar Excel de modelos sin artículos'`.
- `migration_accion_tienda_venta_ver_todos.sql:20-21` (etiqueta `'Ver todos los modelos (sin stock, deshabilitados o dados de baja)'`, descripción "buscador de modelos... permiso Habilitar de Modelos"). Clave `ver-todos-los-modelos` NO cambia.
- `migration_accion_tienda_eliminar.sql:22-23` (`'Dar de baja modelo (✕ en la tarjeta)'`, "tarjeta de cada modelo... Da de baja el modelo").
- `migration_descripcion_acciones_modelos.sql:18,20,22,24,26` ("tarjeta de modelo, en Modelos").
- `migration_accion_modelos_etiquetas_y_escaner.sql:28,37` y `migration_accion_agregar_carga_imagenes_escaner.sql:20` ("Agregar modelo").
- `migration_submenu_descripcion_escritura.sql:17,19,20,25,30` ('COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar producto"' — el último ya es "Agregar artículo" tras el script de hoy).
- `migration_accion_pedido_articulos.sql:23` y `migration_accion_pedidos_filtros_y_cobro.sql:157` (`'...por otra talla o modelo'`), `migration_accion_submenu_categoria.sql:40` (`a.categoria = 'Tarjeta de modelo'`: es el título del grupo de casillas en Gestión de roles).
- **No están en el repo** (viven en la BD): `submenu.nombre` "Modelos" (`productos/buscar`), "Agregar modelo" (`productos/agregar`), "Nuevo Producto"... Consultar `SELECT id,nombre,ruta,descripcion FROM submenu` y las acciones antes de escribir la migración.
- ⚠️ Orden: el script de "variante->artículo" y sus REPLACE se re-corren si alguien vuelve a correr una migración vieja (RENOMBRE §1.4); la nueva migración debe sembrar sus REPLACE con el mismo patrón idempotente y anotarse en la tabla de migraciones del CLAUDE.md.

## 3) Mapa pantalla -> endpoint -> servicios

`[A]` = en la cadena hay texto de 2.1/2.2. Prefijos de clase verificados en `qa`.

**3.1 Agregar modelo / Actualizar modelo** — `ProductosControllerImpl` (`/v1/productos`)
- `POST /mis-productos/v1/productos/save`, `PUT /mis-productos/v1/productos/update`, `GET .../v1/productos/findById/{id}` -> `ProductosServiceImpl.saveProductoLote/guardarProducto/getResumen` -> `IProductosRepository`, `ICodigoBarrasService`, `IPalabraClaveRepository`, `ImagenPort`/`ImageneClienteDisco` (micro), `IProductoImagenRepository`, `IImagenRepository`, `IVarianteRepository`/`IVarianteImagenRepository` (reparte fotos), `ajustarVariantesSiExceden`, `CacheService` + Rabbit. Sin A (mensajes ya dicen "producto"). Después de guardar, el front llama `GET /v1/stock/producto/{id}` (3.3) [A].

**3.2 Catálogo -> Modelos (listado, filtros, baja, habilitar, Excel)** — mismo controller
- `GET /v1/productos/obtenerProductos`, `GET .../buscarNombreOrCodigoBarra?todos=` (usa `AuthenticationUtils.puedeVerTodosLosModelos`, permiso `tienda/venta:ver-todos-los-modelos`), `GET .../admin/filtrar|no-habilitados|sin-stock`, `PUT .../{id}/habilitar`, `PUT .../admin/habilitar-lote`, `DELETE .../deleteBy/{id}`, `GET .../admin/sin-variantes/reporte` y `.../admin/sin-articulos/reporte`, `POST .../compartir-imagenes-variantes|-articulos`, `GET .../admin/diagnostico-imagenes/{productoId}` -> `ProductosServiceImpl` -> `IProductosRepository` + `IProductoImagenRepository` (+ `ImagenPort`, `CacheService`, Rabbit en baja). Sin A.
- Precio: `PUT /v1/precios/producto/{productoId}` -> `CambiarPrecioService` -> `PreciosProductoPort`. Sin A.

**3.3 Crear artículos desde el producto base (🧩 Artículos) y stock del producto** — `VarianteController` (`/v1/variantes` y `/v2/articulos`), `StockController` (`/v1/stock`)
- `POST /mis-productos/v2/articulos/inicializarDesdeProducto` (y `/v1/variantes/...`) -> `VarianteServiceImpl` (:245-320) -> `IProductosRepository`, `IVarianteRepository`, `IVarianteImagenRepository`, `IProductoImagenRepository`, `ImagenPort`, `heredarDelModelo()`. **[A]** :266-267.
- `GET /mis-productos/v2/articulos/porProducto/{productoId}[/paginado[/resumen]]` -> `IVarianteRepository`. Sin A.
- `GET /mis-productos/v1/stock/producto/{productoId}` y `GET .../v1/stock/admin/descuadrados` -> `ConsultarDisponibilidadCasoUso` -> `ConsultarStockPort`/`ConsultarStockJpaAdapter` -> `DisponibilidadStock` -> `DisponibilidadStockResponse.mensajeDe`. **[A]** DisponibilidadStockResponse:51.
- `PUT /mis-productos/v1/stock/producto/{productoId}/ajuste` -> `AjustarStockModeloService` -> `ConsultarStockPort`, `DisponibilidadStock.conAjuste`, `GuardarStockModeloPort`, `AvisarCambioStockPort` (Redis + Rabbit). **[A]** DisponibilidadStock:81/86/90, AjustarStockModeloService:36.

**3.4 Tienda -> venta -> Agregar artículo (busca el producto base)**
- Buscar: `GET /v1/productos/buscarNombreOrCodigoBarra?todos=true` (3.2).
- Guardar: `POST /mis-productos/v2/articulos/guardarConImagenes` -> `VarianteServiceImpl.guardarConImagenes` -> `aplicarAjusteStockModelo` -> `puedeActualizarModelo` (rutas `productos/buscar|agregar|tienda/venta`) -> `IProductosRepository.findByIdWithLock`, validación de stock libre (:828-841), `ArticuloDeAlta`, repos de artículo/imagen, `ImagenPort`. **[A]** :751, :762, :837-838.
- Habilitar producto desde esa pantalla: `PUT /v1/productos/{id}/habilitar`. Habilitar/deshabilitar artículos: `PUT /v2/articulos/{id}/habilitar`, `/admin/habilitar-lote` -> `VarianteServiceImpl` (:1230-1280). **[A]** :1241.
- `POST /v2/articulos/{varianteId}/independizar` (crea producto nuevo desde un artículo; el front dice "Modelo nuevo creado") -> `VarianteServiceImpl:374-440`; solo mensaje "ya esta en uso por otro producto" (ya bien).

**3.5 Carga rápida y Carga de catálogo Excel**
- `/mis-productos/v1/carga-imagenes/{subir-imagen, {productoId}/reintentar-imagen, estado, borradores, fallidas, {productoId}/completar, DELETE {productoId}}` -> `CargaImagenesServiceImpl` -> repos de producto/artículo/código/palabra clave/imágenes, `ImagenPort`, `CacheService`, Rabbit. Sin A (dice "producto borrador": ya correcto).
- `POST /mis-productos/v1/documentos/productos` -> `SubirDocumentosServiceImpl.procesarExcelProductos` -> `ICodigoBarrasRepository`, `IProductosRepository`, `IVarianteRepository`. Mensaje de fila `"...existe pero sin producto asociado"`. Sin A. Cabeceras del Excel `nombreProducto` etc. (:30) son contrato con la plantilla.

**3.6 Pedidos / ventas / abonos (donde el producto base se valida)**
- `POST /v1/pedidos/{id}/articulos`, `PUT .../articulos/{detalleId}`, `DELETE .../promociones/{id}` -> `EditarArticulosService` -> `CatalogoArticuloPort`, `MovimientoStockPort`, `PedidoArticuloPort`. **[A]** EditarArticulosService:276, ArticuloDisponible:48, MovimientoStockJpaAdapter:48/50.
- Quitar línea: `PedidoServiceImpl` (:695-708). **[A]** :705.
- Crear venta/pedido/abono: `VentaServiceImpl`, `PedidoServiceImpl.articuloAVender`, `AbonoServiceImpl:366-369` -> `ArticuloAVender.exigirQueSePuedaVender`. **[A]** ArticuloAVender:38.

**3.7 Rifas, Promociones, Reportes, Tienda pública, Chatbot**
- Rifas `/v1/configurarRifaVariante` y `/v2/configurarRifaArticulo` -> `ConfigurarRifaVarianteService`; Promociones `/v1/promociones` -> `PromocionServiceImpl`; Reportes `/v1/reportes/ventas/productos-mas-vendidos` -> `ReporteVentasServiceImpl`. **Sin A** (no hay "modelo" visible; los correos de promoción no dicen "modelo").
- Tienda pública `/v2/articulos/buscar|buscar-filtrado|filtros-disponibles` y chatbot `POST /chatbot/mensaje`, "ver más" -> `IVarianteRepository`; el único "MODELO" del prompt es `ChatbotBase.java:76` (D-1, 4.2).

## 4) Colisiones reales hoy ("producto" ya significa otra cosa para el usuario)

### 4.1 Lo que lee el ADMIN
| # | archivo:línea | Qué dice hoy | Recomendación |
|---|---|---|---|
| X1 | H/datosprueba/.../PrepararPruebaService.java:213, :235, :273, :274 y EscenarioDePrueba.java:18 | "🧩 Productos", "Agregar producto" (nombres VIEJOS del botón/pantalla de artículo) | Cambiar a "🧩 Artículos" / "Agregar artículo" ANTES o junto con Modelo->Producto; si no, "Agregar producto" quedaría apuntando a la pantalla equivocada |
| X2 | BD `submenu` "Agregar modelo" (`productos/agregar`) vs "Agregar artículo" (`tienda/venta`) | Tras renombrar quedaría "Agregar producto" (base) y "Agregar artículo" | OK si ambos cambios van a la vez; el menú "Productos" (lista actual "Modelos") no choca ya que la pantalla de artículos se llama distinto. Verificar en BD los nombres reales (RENOMBRE §1.3 habla de "Catálogo → 🔍 Modelos") |
| X3 | P/entity/Producto.java:11-18 y P/entity/productoVariantes/Variantes.java:15-24 (Javadoc) y `TAXONOMIA_NOMBRES_BACK.md` | "Producto = lo que el usuario ve como MODELO; Variantes = lo que se muestra como PRODUCTO; el Modelo agrupa, el Producto se vende" | Reescribir: Producto agrupa, Artículo se vende. Es documentación, no pantalla; ya está desfasado desde el renombrado a artículo |
| X4 | P/controller/RedesSocialesController.java:81,104,216,240 (Swagger) | "el video no depende de un producto del catálogo; varianteId es OPCIONAL" — "producto" = artículo | Cambiar a "artículo del catálogo" (Swagger, lo ve quien lee la API; baja prioridad). También OpenApiConfig:19 "catálogo, variantes" (B) |
| X5 | P/controller/ChatbotController.java:189-190 (Swagger) | "Ver más productos del chatbot" | Dejar o "artículos"; solo Swagger |
| X6 | P/service/ReconciliacionImagenService.java:127,136 | `"PRODUCTO id=..."` en reconciliación de imágenes (admin) | Es el producto base: queda bien con el rename |
| X7 | BD `accion_submenu.etiqueta` 'Tarjeta de artículo' (script de hoy) vs 'Tarjeta de modelo' (`migration_accion_submenu_categoria.sql:40`) | Dos categorías (artículo / modelo) en Gestión de roles | La migración nueva debe renombrar la de "modelo" -> "producto" |
| X8 | H/articulo/dominio/modelo/ArticuloAVender.java:38 y EditarArticulosService.java:276 | "el modelo" junto a "el artículo" en la misma regla | Pasar a "el producto está deshabilitado" (coincide con los tests) |

### 4.2 Lo que lee el CLIENTE FINAL (tienda pública) — "producto" ahí significa lo que compra; recomendación: dejarlo
| # | archivo:línea | Texto | Recomendación |
|---|---|---|---|
| Y1 | P/service/ResenaServiceImpl.java:56, :59 | `"Ya dejaste una resena para este producto..."`, `"Solo puedes resenar productos que hayas comprado"` | Dejar (RENOMBRE §1.3: lo que ve el cliente sigue diciendo "producto") — **decisión del dueño** si algún día el cliente debe ver "artículo" |
| Y2 | P/service/RestockNotificacionService.java:36, VarianteServiceImpl:882, EmailService.java:284-298 | Correo "¡Ya volvió el stock!": `nombreProducto` (nombre del producto base + detalle del artículo) y fallback `"un producto"` | Dejar; es cliente. "nombreProducto" es el nombre del producto base, sin problema |
| Y3 | P/chatbot/ChatbotBase.java (≈140 líneas "producto": 73, 98, 127-160, 191-196, 199-234, 253-274, 390-414, 480, 648-649) y ChatbotSitioWebService:39,42; ChatbotChatVivoService:82 | Prompt: "producto" = lo que el cliente ve (artículo); cliente lee las respuestas del bot | **Dejar**. Una sola frase mezcla ambos sentidos: ChatbotBase.java:76 `"el nombre suele ser el del MODELO (ej. "Surprise SU8183")..."` -> decisión del dueño (la IA entiende "MODELO" como modelo comercial; cambiar a "producto" crea dos sentidos en el mismo prompt). El test ChatbotCatalogoContextoTest solo usa el ejemplo |
| Y4 | H/articulo/dominio/modelo/ArticuloAVender.java:38 vía ArticuloNoVendibleException ("Quítalo del carrito para continuar") | Al comprar con un producto base deshabilitado el cliente puede leer "el modelo está deshabilitado" (hoy) / "el producto está deshabilitado" (tras rename) | El cliente nunca ve el producto base: **decisión del dueño** — mensaje neutro tipo "'X' ya no está a la venta. Quítalo del carrito para continuar." (cambia `ArticuloNoVendibleException`, afecta ArticuloAVenderTest) |
| Y5 | ChatbotBase.java:648-649 | `"No hay productos disponibles en este momento"` | Dejar |

## 5) Contrato con el front que NO cambia

- Claves de permiso: `ver-todos-los-modelos` (acción de `tienda/venta`; `AuthenticationUtils.java:52`, `puedeVerTodosLosModelos` :51, SpEL en `ProductosServiceImpl:245`, `VarianteServiceImpl:776`, `ProductosControllerImpl:43`, SQL `migration_accion_tienda_venta_ver_todos.sql`, `PrepararPruebaService:272`); rutas de pantalla `productos/buscar`, `productos/agregar`, `tienda/venta`; `crear-variantes`; `Pantalla.PRODUCTOS_BUSCAR` ("productos-buscar").
- Campos JSON: `tipo:"MODELO"` (`CatalogoPreparado.MODELO`), `modelos`, `modelosPedidos`, `modelosCreados`, `modelosDadosDeBaja`, `modeloId`, `ajusteStockModelo`, `usarImagenDelModelo`, `modeloHabilitado`/`modeloConFoto` (DisponibilidadStockResponse:43-44), `productoId`, `varianteId`.
- Rutas `/v1/productos/...`, `/v1/stock/producto/{id}`, `/v1/precios/producto/{id}`, `/v1/carga-imagenes/...`, `/v1/admin/datos-prueba/...`; hoja Excel `Productos Sin Artículos`; cabeceras de plantilla Excel (`nombreProducto`).
- Textos de `CAMBIOS_FRONT.md` que citan literalmente mensajes del back y habría que actualizar (línea: texto):
  - 135 y 187: 422 *"El modelo «…» no tiene piezas libres… súmale stock al modelo en Catálogo → Modelos → ✏️ Actualizar…"*
  - 583: *"El stock del modelo cambió mientras lo ajustabas…"*
  - 593-594: etiqueta *"Ver todos los modelos"* (la clave queda)
  - 602, 604: "Stock total del modelo / Repartido / Libre" (texto del front)
  - 609-611: *"…agregar (+) o quitar (−) al modelo"*, *"No se puede quitar 4: el modelo solo tiene 3"*, *"No se puede dejar el modelo en 9…"*
  - 806 (y 802): *"No alcanza el stock para crear 2 artículo(s): el modelo tiene 3…"*
  - 821: color *"Fondo del recuadro de búsqueda y filtros (Tienda y Catálogo → Modelos)"*
  - 932, 934: *"No tienes permiso para cambiar el stock del modelo… editar modelos"*, *"No se puede dejar el modelo en 8…"*
  - 273, 274, 287: *"modelos dados de baja"*, *"5 pedidos cancelados y 2 modelos dados de baja"*, *"…1 modelo dado de baja."*
  - 21286: `(articulo: 1, modelo: 8)`
  - 21909-21922 y 22780, 22791: bloque `/v1/stock/producto/{id}` (el doc ya está desfasado: dice *"Este producto está descuadrado… sus 3 modelos suman 18"* y el back dice "Este modelo… sus 3 artículos"), `"fase": "Creando modelos y artículos"`, `"…dados de baja (y sus modelos)"`.
  - 21328 (sección "El modelo y el artículo — cómo se relacionan"): terminología general.
  - Falta nada que escribir si no cambia el JSON, pero la sección nueva del rename debe documentar el "antes/después" (regla del CLAUDE.md).
- Documentos de apoyo con "modelo" = producto base: `PRUEBAS_QA_STOCK.md` (20 líneas), `TAXONOMIA_NOMBRES_BACK.md` (9), READMEs `H/datosprueba` (21), `H/stock` (11), `H/articulo` (14), `RENOMBRE_VARIANTE_A_ARTICULO.md`.

## 6) Pruebas (`src/test`)

Pruebas que comparan texto (con "producto"/"modelo"):
- `H/articulo/dominio/modelo/ArticuloAVenderTest.java:25,60-62` (hasMessageContaining "el producto está deshabilitado") y `H/pedidoarticulo/aplicacion/servicio/EditarArticulosServiceTest.java:258`: **hoy fallan** (el código dice "el modelo"; TESTS_PENDIENTES.md:462-463). El rename a "producto" las deja pasando. Si en 4.2-Y4 se elige mensaje neutro, ArticuloAVenderTest hay que actualizarlo.
- `CambiarPrecioServiceTest.java:63` "No existe el producto 99": sin cambio.
- `ChatbotComentariosTest.java:18` "NO SABES de qué producto es esta publicación": sin cambio si no se toca el prompt.
- `mis/productos/controller/RolesControllerTest.java:84,89,115,120`: mock con texto hardcodeado `"…primero necesita poder VER \"Modelos\"…"`; no se rompe, actualizar por coherencia (y TESTS_PENDIENTES.md:~455 ya lo marca como viejo por otro motivo: 400 vs 409).
- Fixtures con nombre de submenú "Modelos"/"Agregar modelo" (no asertan el texto): `ISubmenuRepositoryTest.java:46,47,68,72`; `RolesServiceImplTest.java:31,83,95,119,134,174,187,203,217,232`.
- Asertan solo la parte numérica del mensaje 422 (no se rompen al cambiar "modelo"->"producto"): `VarianteStockDevolucionTest.java:249` ("Disponible: 0, Solicitado: 2"), `VarianteStockValidacionTest.java:149`, `EditarArticulosServiceTest.java:230`.
- Texto con "modelo" solo en `@DisplayName`/mensaje de assert/variable: `HeredarDelModeloTest.java:14`, `VarianteStockDevolucionTest.java:159,253`, `ArticuloDeAltaTest`, `DatosPruebaIntegracionTest.java:237` (println), `ArticuloAVenderTest.java:10-14` (DisplayName/Javadoc).
- **Ninguna prueba asierta** los mensajes de 2.1 (stock del modelo, descuadrado, "cambió mientras lo ajustabas", permisos) ni los de 2.2 (panel QA).

*No se copiaron llaves, tokens ni contraseñas.*

---

# ANEXO C — Base de datos, procedimiento anterior, datos de prueba y riesgos

## 1. Textos de la base de datos que dicen "modelo"

### 1.1 Cómo leer la tabla

- "Valor actual" = el último que dejó la cadena de scripts, **suponiendo** que nadie lo editó a mano y que el script de artículos se aplicó (en QA seguro; en prod ver 6.2).
- "Corrido": según `CLAUDE.md` (tabla "Migraciones ya corridas") y `PROMOCION_QA_A_PROD.md`. Donde ninguno de los dos habla, lo digo.
- Selector = cómo se localiza la fila para un UPDATE idempotente. **Todas las filas se pueden encontrar por `submenu.ruta` y, para acciones, por `accion_submenu.clave`**; ninguna debe buscarse solo por texto.
- La clave de las acciones y las rutas **no cambian** (sección 5).

### 1.2 `submenu` (5 filas)

| ID | Script origen : línea | tabla.columna | Selector | Valor actual (reconstruido) | Valor propuesto | Corrido |
|---|---|---|---|---|---|---|
| A1 | `migration_menu_submenu.sql:47` (solo en la raíz del repo, no en `static/`) | `submenu.nombre` | `ruta='productos/buscar'` | `Modelos` | `Productos` | Prod: sí existe (`PROMOCION_QA_A_PROD.md` §1.a #1). Sin fila en `CLAUDE.md` |
| A2 | `migration_menu_submenu.sql:48` | `submenu.nombre` | `ruta='productos/agregar'` | `Agregar modelo` | `Agregar producto` | igual que A1 |
| B1 | `migration_descripcion_submenu.sql:13` (raíz) | `submenu.descripcion` | `ruta='productos/buscar'` | `Buscar y administrar los Modelos (el "molde" de un producto, ej. "Blusa Zara" antes de elegir talla/color). Vive en el menú: Catálogo → Modelos.` | `Buscar y administrar los Productos (el producto base, ej. "Blusa Zara", antes de elegir talla/color). Vive en el menú: Catálogo → Productos.` | Prod: sí (`PROMOCION_QA_A_PROD.md` §1.a #7) |
| B2 | `migration_descripcion_submenu.sql:14` | `submenu.descripcion` | `ruta='productos/agregar'` | `Formulario para dar de alta un Modelo nuevo. Vive en el menú: Catálogo → Agregar modelo.` | `Formulario para dar de alta un Producto nuevo. Vive en el menú: Catálogo → Agregar producto.` | igual |
| B3 | origen `migration_descripcion_submenu.sql:15`; reescrito por `migration_renombre_articulo_etiquetas.sql:57-60` | `submenu.descripcion` | `ruta='tienda/venta'` | `Formulario para agregar un artículo (talla/color) a partir de un modelo ya creado. Vive en el menú: Catálogo → Agregar artículo.` | `... a partir de un producto ya creado. ...` | QA sí (2026-10-20); prod: ver 6.2 |
| B4 | origen `migration_descripcion_submenu.sql:53`; reescrito por el script de artículos `:72-75` | `submenu.descripcion` | `ruta='admin/diagnostico-imagenes'` | `Herramienta para revisar por qué no aparece la imagen de un modelo o de un artículo. Vive en el menú: Sistema → Diagnóstico de imágenes.` | `... de un producto o de un artículo ...` | igual que B3 |
| B5 | origen `migration_descripcion_submenu.sql:18`; reescrito por el script de artículos `:77-80` | `submenu.descripcion` | `ruta='palabras-clave'` | `Catálogo de categorías/palabras clave usadas para clasificar y buscar modelos y artículos. Vive en el menú: Catálogo → Categorías.` | `... buscar productos y artículos. ...` | igual que B3 |

Texto compartido de escritura (`submenu.descripcion_escritura`), 3 filas con el mismo texto base:

| ID | Script origen | Selector | Valor actual (reconstruido; se ve como el original con `Agregar producto`->`Agregar artículo` y `variantes`->`artículos`) | Propuesto |
|---|---|---|---|---|
| C1 | `static/migration_submenu_descripcion_escritura.sql:16-22` | `ruta='productos/buscar'` | `Permiso de escritura COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar artículo" -- basta con tenerlo marcado en CUALQUIERA de las 3 para poder crear/editar/borrar en las 3. Modelos en sí es solo buscar/listar (no tiene formulario propio), así que marcarlo AQUÍ no habilita nada visible en esta pantalla -- el efecto se nota al entrar a Agregar modelo o Agregar artículo. También habilita subir fotos a artículos desde Flores/Catálogos y Ramos armados.` | Cambiar `"Modelos"` -> `"Productos"`, `"Agregar modelo"` -> `"Agregar producto"`, `Modelos en sí` -> `Productos en sí`, `Agregar modelo o` -> `Agregar producto o` |
| C2 | mismo archivo `:24-27` | `ruta='productos/agregar'` | `Permiso de escritura COMPARTIDO entre "Modelos", "Agregar modelo" y "Agregar artículo" -- basta con tenerlo marcado en CUALQUIERA de las 3 para poder crear/editar/borrar en las 3.` | igual cambio |
| C3 | mismo archivo `:29-32` | `ruta='tienda/venta'` | igual que C2 | igual cambio |

Corrido de C1-C3: prod sí (`PROMOCION_QA_A_PROD.md` §1.c #5, "existen"); el script de artículos las reescribió en QA el 2026-10-20.

**Advertencia de largo (no verificable sin la base):** la columna `descripcion_escritura` se creó `VARCHAR(255)` (`static/migration_submenu_descripcion_escritura.sql:14`, entidad `Submenu.java:46` `@Column(length = 255)`) pero el texto de C1 mide **458 caracteres**. O el servidor lo truncó a 255 al correr (modo no estricto), o la columna se ensanchó a mano, o el UPDATE falló. Lo mismo pasa con `descripcion` de `habilitar` de la versión raíz (286 caracteres, `AccionSubmenu.java:34` también 255). **Antes de cualquier UPDATE con REPLACE hay que mirar `SHOW COLUMNS FROM submenu LIKE 'descripcion_escritura'` y `SELECT CHAR_LENGTH(...)`**: si está truncado, un REPLACE no encuentra la frase cortada y deja la fila a medias.

### 1.3 `accion_submenu` (10 filas) y `tema_variable` (1 fila)

| ID | Script origen : línea | tabla.columna | Selector (`ruta` + `clave`) | Valor actual (reconstruido) | Valor propuesto | Corrido |
|---|---|---|---|---|---|---|
| D1 | `static/migration_accion_modelos_etiquetas_y_escaner.sql:28`; reescrito por el script de artículos `:105-108` | `accion_submenu.etiqueta` | `productos/buscar` + `crear-variantes` | `Crear artículos desde el modelo (🧩 "Artículos" en la tarjeta)` | `Crear artículos desde el producto (🧩 "Artículos" en la tarjeta)` | prod sí (§1.c #3); artículos: QA sí, prod ver 6.2 |
| D2 | origen `migration_descripcion_acciones_modelos.sql:19` (raíz) reescrito por `:110-113` del script de artículos | `accion_submenu.descripcion` | `productos/buscar` + `crear-variantes` | `Deja crear artículos (tallas/colores) a partir de un modelo. Aparece en Modelos (productos/buscar): el botón 🧩 "Artículos" en cada tarjeta de modelo.` (si en su día corrió la versión de `static/`: `Botón 🧩 "Artículos" en el pie de la tarjeta de cada modelo, en Modelos.`) | `... a partir de un producto. Aparece en Productos (productos/buscar): el botón 🧩 "Artículos" en cada tarjeta de producto.` | prod sí (§1.a #8) |
| D3 | `migration_descripcion_acciones_modelos.sql:23` (raíz) + reglas de `:226-248` del script de artículos | `accion_submenu.descripcion` | `productos/buscar` + `descargar-excel` | `Deja descargar un Excel con los modelos que todavía no tienen artículos. Aparece en Modelos (productos/buscar): el botón "📥 Excel sin artículos" en la barra de filtros.` | `... con los productos que todavía no tienen artículos. Aparece en Productos (productos/buscar): ...` | prod sí; artículos igual que D1 |
| D4 | `migration_descripcion_acciones_modelos.sql:15` (raíz) | `accion_submenu.descripcion` | `productos/buscar` + `habilitar` | `Deja Habilitar/Deshabilitar un producto para que se vea o no en la Tienda. Aparece en Modelos (productos/buscar) de 3 formas: ...` (versión `static/`: `... en el pie de cada tarjeta de modelo, en Modelos.`) | `Aparece en Productos (productos/buscar)` (y "tarjeta de producto" en la versión `static/`) | prod sí. **Hay dos versiones del mismo script (raíz y `static/`, con texto distinto); no sé cuál corrió.** |
| D5 | `migration_descripcion_acciones_modelos.sql:17` | `accion_submenu.descripcion` | `productos/buscar` + `eliminar` | `Deja borrar un producto por completo (con sus artículos e imágenes). Aparece en Modelos (productos/buscar): el botón 🗑️ en cada tarjeta.` | `Aparece en Productos`. **Aparte:** el texto "borrar por completo" ya es falso (desde 2026-09-17 es baja lógica, `CLAUDE.md` "Eliminar = baja lógica"); no es parte del renombre pero conviene corregirlo en el mismo script | prod sí |
| D6 | `migration_descripcion_acciones_modelos.sql:21` | `accion_submenu.descripcion` | `productos/buscar` + `compartir-imagen` | `Deja compartir la imagen del producto (WhatsApp, etc.). Aparece en Modelos (productos/buscar): ...` | `Aparece en Productos` | prod sí |
| D7 | `static/migration_accion_modelos_etiquetas_y_escaner.sql:37` | `accion_submenu.descripcion` | `productos/buscar` + `escanear-codigo` | `... y botón 📷 "Escanear código de barras" que aparece arriba del buscador en vista móvil, en Modelos. Abre la cámara ...` | `..., en Productos. ...` | prod sí (§1.c #3) |
| D8 | `static/migration_accion_agregar_carga_imagenes_escaner.sql:20` | `accion_submenu.descripcion` | `productos/agregar` + `escanear-codigo` | `Botón 📷 junto al campo de código de barras, en Agregar modelo. Abre la cámara para leer el código y rellenar el campo automáticamente.` | `..., en Agregar producto. ...` | **sin constancia** en `CLAUDE.md` ni `PROMOCION_QA_A_PROD.md` (el script no aparece en ninguno de los dos). Verificar con `SELECT` |
| D9 | `static/migration_accion_tienda_venta_ver_todos.sql:20-21` | `accion_submenu.etiqueta` y `.descripcion` | `tienda/venta` + `ver-todos-los-modelos` | etiqueta `Ver todos los modelos (sin stock, deshabilitados o dados de baja)`; descripción `En el buscador de modelos salen también los que no tienen stock, los deshabilitados y los dados de baja, con una etiqueta de su estado. Al elegir uno se puede habilitar (si además tiene el permiso Habilitar de Modelos) o subirle stock.` | `Ver todos los productos (...)` y `En el buscador de productos ... permiso Habilitar de Productos ...`. **La clave `ver-todos-los-modelos` NO cambia.** Largo propuesto 239 (cabe en 255) | QA sí (2026-10-08); prod: `CLAUDE.md` dice ⏳; no hay constancia (6.2) |
| D10 | `static/migration_accion_submenu_categoria.sql:40` | `accion_submenu.categoria` | `productos/buscar` + `IN ('eliminar','habilitar','crear-variantes','compartir-imagen')` (4 filas) | `Tarjeta de modelo` | `Tarjeta de producto`. El front agrupa con `categoria.startsWith('Tarjeta')` (`gestion-roles.component.ts:190`), se conserva el prefijo | prod sí (§1.c #4) |
| D11 | `static/migration_accion_tienda_eliminar.sql:22-23` | `accion_submenu.etiqueta` y `.descripcion` | `tienda/buscar` + `eliminar` | `Dar de baja modelo (✕ en la tarjeta)`; `Botón ✕ de la tarjeta de cada modelo, en Tienda. Da de baja el modelo (deja de mostrarse y pierde sus fotos), no borra el historial de ventas ni de pedidos.` | **A "artículo", no a "producto"**: `Dar de baja artículo (✕ en la tarjeta)` / `... tarjeta de cada artículo ... Da de baja el artículo ...`. Es una corrección de un error de fondo (la tarjeta de Tienda es un artículo) | QA y prod 2026-09-22 (fila en `CLAUDE.md`) |
| D12 | `static/migration_accion_pedidos_filtros_y_cobro.sql:157` (pisa a `migration_accion_pedido_articulos.sql:23`) | `accion_submenu.etiqueta` | `pedidos/mis-pedidos` + `cambiar-articulo` | `Cambiar un artículo del pedido (⇄ por otra talla o modelo)` | `... (⇄ por otra talla o producto)`; **decisión de redacción**: aquí "modelo" significa "otro producto base", queda ambiguo con "producto" de cliente | QA y prod 2026-10-06 (`CLAUDE.md:645`) |
| T1 | `migration_tema_filtros.sql` (raíz) reescrito por el script de artículos `:99-102` | `tema_variable.etiqueta` | `clave='filtros-panel-bg'` | `Fondo del recuadro de búsqueda y filtros (Tienda y Modelos)` | `Fondo del recuadro de búsqueda y filtros (Tienda y Productos)` (es el texto original que el script de artículos cambió; hoy vuelve) | qa 2026-10-07 + artículos; prod ⏳ (Jade) |

Totales: 5 filas `submenu`, 10 filas `accion_submenu` (D1-D12 son 12 renglones por columnas, pero D10 son 4 filas y varias comparten fila: `crear-variantes`=D1+D2+D10, `habilitar`=D4+D10, `eliminar`=D5+D10, `compartir-imagen`=D6+D10, `descargar-excel`=D3, `escanear-codigo`(buscar)=D7, `escanear-codigo`(agregar)=D8, `ver-todos-los-modelos`=D9, `tienda/buscar eliminar`=D11, `cambiar-articulo`=D12), 1 fila `tema_variable`. Total **16 filas**.

### 1.4 Lo que **no** dice "modelo" pero conviene revisar al mismo tiempo

- `accion_submenu.etiqueta` de `descargar-excel`: hoy `Descargar Excel sin artículos (📥 en la barra de filtros)`. No dice modelo; mirar si el botón del front se llama igual cuando cambie "Productos" (para el dueño "producto sin artículos" es el concepto que reporta).
- `submenu.descripcion` de `favoritos` ("Lista de productos favoritos del cliente"), `reportes`... : "producto" de cara al cliente. Tras el renombre, "producto" significará **dos cosas** en la base (lo que ve el cliente = artículo, y el producto base del admin). Hay que decidir si se acepta (sección 6.6).
- Hay menús y descripciones que el dueño pudo haber creado o editado desde **Sistema -> Menús y submenús** (skill `nombres-reales`). No los puedo ver. Esas filas solo se pueden detectar con el SELECT de diagnóstico de abajo.

### 1.5 Cómo hacer el UPDATE nuevo idempotente y respetuoso con ediciones manuales

Se puede dejar así (esto es un diseño propuesto, no un script ejecutado):

1. **Localizar por `ruta` / `clave`**, nunca por texto. Pantallas: `productos/buscar`, `productos/agregar`, `tienda/venta`, `admin/diagnostico-imagenes`, `palabras-clave`. Acciones: las claves de D1-D12 con su `ruta`.
2. **Doble guarda**: `UPDATE ... SET col = <nuevo> WHERE ruta = ... AND col = <texto actual exacto>`. Si el dueño editó el texto a mano, el `WHERE` no encuentra la fila y **no la toca**. Para las filas con texto largo (C1, D4) usar `LIKE` por fragmento o `REPLACE` solo dentro de esa `ruta`.
3. **Para el segundo intento**, `REPLACE` limitado por `ruta` y por palabra completa de la frase (`Agregar modelo`->`Agregar producto`, `"Modelos"`->`"Productos"`), nunca `modelo`->`producto` suelto: hay filas donde "modelo" ya significa otra cosa (D11 -> artículo, D12 ambigua).
4. **Idempotente**: la segunda corrida no encuentra nada que reemplazar, igual que el script de artículos. Probarlo en una base desechable dos veces y con `--safe-updates` (así se probó el de artículos, `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.1 punto 4).
5. **Verificación al final**: `SELECT` por `ruta`/`clave` que muestre el valor nuevo, y otro que busque `LIKE '%modelo%'` en `submenu`/`accion_submenu`/`tema_variable` y deba salir **vacío**. Lo que salga ahí son las filas editadas a mano.
6. **Diagnóstico antes** (solo SELECT), útil para que el dueño vea qué hay realmente:
   `SELECT 'submenu', id, ruta, nombre, descripcion, descripcion_escritura FROM submenu WHERE CONCAT_WS(' ', nombre, descripcion, descripcion_escritura) LIKE '%modelo%';`
   `SELECT 'accion', a.id, s.ruta, a.clave, a.etiqueta, a.categoria, a.descripcion FROM accion_submenu a JOIN submenu s ON s.id = a.submenu_id WHERE CONCAT_WS(' ', a.etiqueta, a.descripcion, a.categoria) LIKE '%modelo%';`
   `SELECT clave, etiqueta FROM tema_variable WHERE etiqueta LIKE '%Modelo%';`
7. **No hay que volver a entrar**: no cambia ninguna clave ni ruta. Pero ver 5 para lo que no se debe tocar.
8. **Anotarla** en la tabla de migraciones de `CLAUDE.md` en el mismo cambio (regla del proyecto).

---

## 2. Cómo se hizo el renombre variante -> artículo (para repetirlo igual)

Fuente: `RENOMBRE_VARIANTE_A_ARTICULO.md`, skill `.claude/skills/renombrar/SKILL.md`, `git log` de ambos repos.

**Alcance acordado (niveles):** 1 = textos de pantalla y mensajes del back (sí). 2 = código interno, solo donde ya se editaba (sí). 3 = contrato: rutas, **versionado `/v2` conviviendo con `/v1`**, campos JSON no se tocan. 4 = tablas y columnas no se tocan; sí los **textos guardados** en la base, con un script. Esta decisión sigue en `CLAUDE.md`, sección "Renombrado en curso".

Pasos reales, en orden:

1. **Rama propia desde `dev`**, un repo cada uno: `rename/variante-a-articulo` (back y front), para no mezclar con Jade ni con hotfixes. Antes se había hecho en un `stash` del back y archivos sin commit en el front (hallazgo H1, 2026-10-06).
2. **Inventario escrito** en `RENOMBRE_VARIANTE_A_ARTICULO.md` (back): niveles, rutas que cambian, hallazgos H1-H7, mapa de impacto R1-R21, pruebas paso a paso. La skill `renombrar` obliga a esto antes de editar.
3. **Back primero (2026-10-06):** `32bc586` "rutas /v2/articulos en espejo de /v1/variantes y mensajes en 'artículo'" (27 archivos, 418+/87-). Regla de seguridad en espejo en `SecurityConfig` para todas las rutas nuevas.
4. **Front después (2026-10-06):** `1c721e7f` "el front llama /v2/articulos y los textos de pantalla dicen 'artículo'" (29 archivos).
5. **Retomado 2026-10-08:** `14a96f8` / `44ddb4c3` traen `dev` a la rama (back y front). Segundo paso de textos: front `66847472` (buscadores, 23 archivos) y `b0004d90` (pantallas que usan los endpoints, 35 archivos); back `dabb26a` (script de textos de la base + documentación, 6 archivos) y `1cdabc4` (endpoints, mensajes, 13 archivos).
6. **Script de la base:** `src/main/resources/static/migration_renombre_articulo_etiquetas.sql`. Textos fijos (sección 1, comparando el texto exacto) más barrido por `REPLACE` de frases largas a cortas, con género y número ("la variante" -> "el artículo"). Probado en una base MySQL desechable: dos corridas seguidas (la segunda no cambia nada) y con `--safe-updates`; "33 textos la primera vez, 0 la segunda" (`PENDIENTES_2026-10-08.md:136`).
7. **Merge a `qa` (2026-10-20):** `95871b1` (back, 02:32 UTC) y `9fd2eb43` (front, 02:35). No pasó por `dev`.
8. **Script en QA (2026-10-20):** el dueño lo corrió en `inventario_key_qa` (cubre dev y qa), con la consulta de verificación al final (3 consultas deben salir vacías; la última devuelve "Agregar artículo" y "Rifa de artículos"). Detectó 3 notas (N1-N3, `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.4): textos que no cambiaron porque el script se corrió sin base seleccionada (`Error 1046`), falta del ℹ️ en Menús y submenús, y "VARIANTE" en mayúsculas.
9. **Segundo barrido (mismo día):** back `e7c37fd` (4 archivos) y front `487ce4e7`; el script ganó la sección 0 (¿base correcta?) y la sección 3 (mayúsculas y concordancia "editarlas" -> "editarlos"). Se volvió a correr completo.
10. **Pruebas en QA:** R1-R21 del mismo documento (rutas `/v2` en la pestaña Red con filtro `articulos`, con la línea base anotada antes de subir; textos del menú y de Gestión de roles; mismos permisos). Quedó pendiente la escritura de tests automáticos (`TESTS_PENDIENTES.md`, regla de no escribir tests mientras el código cambia).
11. **Subida a producción (2026-10-20):** `32cbd8b` (back, merge qa -> main, 05:11 UTC) y `ba7056a9` (front, merge qa -> master, 05:14 UTC, incluye además el diseño Pizarra). Back antes que front, como pide el documento (§2). El push dispara el workflow de GitHub Actions y el reinicio del deployment (`CLAUDE.md`, "Deployment automático").
12. **Script en prod:** el documento dice "se corre en `inventario_key` después del front". **No hay constancia escrita de que se corrió** (`CLAUDE.md:656` dice `⏳ cuando la rama llegue a main`, sin actualizar). Confirmarlo.

Para repetirlo con Modelo -> Producto: seguir el mismo orden, con estas diferencias: la rama debe salir de `qa`/`main` (no de `dev`, que no tiene el renombre anterior), y el `/v2` no hace falta porque no hay rutas ni campos que cambiar.

Documentos del renombre anterior que se desactualizan en cuanto se haga éste: `CLAUDE.md` ("Dónde se hace — solo dev y qa por ahora" ya es falso: está en `main`), `RENOMBRE_VARIANTE_A_ARTICULO.md` (la última frase del §1.4: "basta volver a correr el script" pasa a ser peligrosa, ver 6.1).

---

## 4. Datos de prueba acoplados

### 4.1 Nombres que **teclea** el probador (no salen del código)

Son los dos que menciona el dueño. No están en ningún `.java`: son texto del documento que el probador escribe en el campo Nombre de **Catálogo -> Agregar modelo**.

| Texto | Dónde está |
|---|---|
| `QA Prueba 17 Modelo nuevo` | `PRUEBAS_PENDIENTES_2026-10-13.md:196` (tabla "Lo que escribes tú", 17.7) y `:580` (paso 1 de la 17.7); `.claude/skills/preparar-prueba/SKILL.md:147` (ejemplo de convención de nombres) |
| `QA Prueba 20 Modelo sin stock` | `PRUEBAS_PENDIENTES_2026-10-13.md:198` (20.3), `:1344` (paso de la 20.3 "Agregar modelo con stock 0"), `:1384` (el 💬 del dueño que lo cita literal) |

**Por qué importa el nombre:** `NombresDePrueba.prefijoModelos()` (`NombresDePrueba.java:52-54`) arma `"QA Prueba " + prueba + " "` y R21 ("quitar / reiniciar prueba") busca con `nombre LIKE 'QA Prueba 17 %'` **y** `marca = 'Prueba QA'` (`EscenariosPruebaAdapter.java:287,293-294`, `CatalogoPruebaJdbcAdapter.java:177-180`). Eso significa que los modelos que el probador crea a mano **también se limpian** con el botón de reiniciar. Cambiar la última palabra ("Modelo nuevo" -> "Producto nuevo") es seguro **mientras se conserve el prefijo `QA Prueba N ` y la marca `Prueba QA`**.

### 4.2 Nombres y textos que **genera** el código (`hexagonal/datosprueba`)

| Archivo:línea | Qué escribe | Dónde se ve | Cambio |
|---|---|---|---|
| `dominio/modelo/NombresDePrueba.java:44-46` | nombre `"QA Prueba N <etiqueta>"`; sin etiqueta, `"Artículo"` | Lista de Modelos y Tienda | Ninguno (no dice "Modelo"). El **nombre del método** `modelo()` es código interno |
| `dominio/modelo/CatalogoAleatorio.java:100-102` | `nombre = tipo + estilo + " QA-" + n`; descripción termina en `". Modelo de prueba generado para QA."` | **Se guarda en `producto.descripcion` de la base** y se ve en la pantalla | Ver 4.3 |
| `aplicacion/servicio/PrepararPruebaService.java:209` | `modeloNuevo(e, "Productos", ...)` => nombre real **`QA Prueba 14 Productos`** | Lista de Modelos, prueba 14-QA | Revisar: ya hoy es una palabra del vocabulario de la pantalla |
| `PrepararPruebaService.java:212,231,233,253,270,271,347` | textos `queEs` de la respuesta: "Modelo con stock 3 y un artículo con stock 1 con foto", "Modelo con stock 5...", "Modelo deshabilitado...", "Modelo habilitado con stock 4...", "Modelo con stock 0 (agotado)..." | Pantalla 🧪 Preparar prueba (lo que lee el dueño) | Cambiar "Modelo" por "Producto" |
| `PrepararPruebaService.java:213,348` | `"Catálogo → 🔍 Modelos → escribe \"QA Prueba 14\" → 🧩 Productos"` y `"Catálogo → 🔍 Modelos → \"QA Prueba 20 Agotado\""` | Instrucción en pantalla | Cambiar. **Además** `🧩 Productos` ya está viejo desde el renombre de artículos: el botón dice "🧩 Artículos" |
| `PrepararPruebaService.java:235,273,274` | `"Agregar producto con \"QA Prueba 15 M5\""`, `"Rol con 🧩 Agregar producto y \"Ver todos los modelos\", sin \"Habilitar\" de Modelos"`, `"Catálogo → 🧩 Agregar producto → ..."` | Pantalla 🧪 Preparar prueba | **Ya están viejos desde el renombre de artículos** (la pantalla dice "Agregar artículo"). Hay que corregirlos en cualquier caso; y con este renombre "Agregar producto" pasaría a ser otra pantalla, así que el texto engañaría de verdad |
| `dominio/modelo/EscenarioDePrueba.java:18` | título `"🧩 Productos del modelo: stock 3 y un artículo con 1"` | Lista de pruebas | Cambiar |
| `dominio/modelo/CatalogoPreparado.java:17` | `MODELO = "MODELO"` (valor que viaja en el JSON, campo `tipo`) | El front lo usa: `admin.service.ts:63` (`tipo: 'MODELO' \| ...`) y `cache.component.ts:237` (`case 'MODELO': return 'modelo'`) | **No renombrar el valor** (es contrato con el front); sí se cambia el texto visible `'modelo'` en `cache.component.ts:237` |
| `infraestructura/entrada/rest/PrepararPruebaController.java:89` y `DatosPruebaController.java:62` | mensaje `"X pedidos cancelados y Y modelos dados de baja"`, `"... artículos de prueba dados de baja (y sus modelos)"` | Respuesta del botón | Cambiar texto |
| `datosprueba/README.md` | tabla de pruebas con "modelo" (37 apariciones) | Documento | Actualizar (sección 3) |

### 4.3 Datos de prueba ya guardados en la base de QA

Todo modelo creado por `CatalogoAleatorio` tiene en `producto.descripcion` la frase `Modelo de prueba generado para QA.` y marca `Prueba QA`, código `2098…`. Cambiar la frase en el código solo afecta a los **nuevos**. Los viejos se quedan con "Modelo" (están dados de baja o quedan para QA; `limpiar_datos_prueba_qa_catalogo.sql` (códigos `2099000…`) y `limpiar_datos_e2e_qa.sql` (códigos `E2E…`) los identifican por código de barras, no por esa frase; el botón de baja del back usa código `2098…` y marca). No hay que actualizarlos.

### 4.4 Tests automáticos que citan nombres de menú

- `src/test/.../ISubmenuRepositoryTest.java:46-47,68,72` y `RolesServiceImplTest.java:83,95,119` usan `"Modelos"` / `"Agregar modelo"` como datos de prueba en memoria (son fixtures, no leen la base real). **No se rompen** al renombrar.
- `e2e/` del front: ver 6.4.

---

## 5. Lo que NO se renombra y viaja al front / JWT

Estas cadenas son **contrato**. Cambiarlas deja a un usuario sin permiso o rompe una pantalla sin que nadie lo note.

| Qué | Dónde está | Por qué no se toca |
|---|---|---|
| Clave de acción `ver-todos-los-modelos` | `accion_submenu.clave`; `AuthenticationUtils.java:52`; `ProductosServiceImpl.java:238,245` (entra en la clave de caché); `ProductosControllerImpl.java:43`; `PrepararPruebaService.java:272`; front `agregar.component.ts:193` | Viaja en el JWT como `tienda/venta:ver-todos-los-modelos`. Un token vivo con ese permiso deja de dar acceso si se cambia la clave |
| Clave de acción `crear-variantes` | `SecurityConfig.java:326`; `VarianteServiceImpl.java:776`; front `all.component.ts:155` | Igual: `productos/buscar:crear-variantes` |
| Las demás claves de `productos/buscar` (`habilitar`, `eliminar`, `compartir-imagen`, `descargar-excel`, `escanear-codigo`, `filtro-*`), de `productos/agregar` y de `tienda/*` | tabla `accion_submenu` | Mismo mecanismo (el JWT lleva `ruta:clave`, `JwtUtil.java:58`) |
| **Rutas** de pantalla: `productos/buscar`, `productos/agregar`, `tienda/venta` | `submenu.ruta`; `SecurityConfig` (21 referencias); `navbar.component.html` (`routerLink`, `tienePantalla`); e2e | Son el identificador de la pantalla y del permiso. Además son URLs del navegador |
| Rutas REST `/v1/productos/...`, `/v2/articulos/...` | controllers | Contrato; no se cambian (esa fue la decisión de artículos) |
| Tabla `producto`, entidad `Producto`, campos `productoId`, `producto_id` | JPA y JSON | No tocar |
| `CatalogoPreparado.MODELO = "MODELO"` | JSON del endpoint de datos de prueba | Lo consume `admin.service.ts:63` |
| Nombres de archivo `PLAN_ALTA_MODELO_Y_ARTICULOS.md`, `DUPLICADO_IMAGENES_AGREGAR_MODELO.md`, `migration_accion_modelos_etiquetas_y_escaner.sql`, `migration_descripcion_acciones_modelos.sql`, `migration_accion_tienda_venta_ver_todos.sql`, `hexagonal/stock/.../AjustarStockModeloService` | repo | Se citan entre sí y desde `CLAUDE.md` y `PROMOCION_QA_A_PROD.md`; renombrarlos rompe las referencias |
| Paquetes `dominio/modelo` en `hexagonal/**` | Java | Es "modelo de dominio", otra cosa |
| `localStorage` / claves como `carritoVariante` | front | Ya se decidió no tocar en el renombre de artículos |

También hay **texto que no es de pantalla y no hay que tocar**: comentarios de Javadoc que dicen `"MODELO"` (`Producto.java:11`, `Variantes.java:20`, `ChatbotBase.java:76`), solo si ya no se editan por otra razón. El prompt del chatbot (`ChatbotBase.java:76`) es texto que **sí lee el cliente**, ver 6.7.

---

## 6. Riesgos

### 6.1 Volver a correr `migration_renombre_articulo_etiquetas.sql` destruiría el renombre nuevo (riesgo alto, evitable)

El script tiene reglas de reemplazo genérico en todas las filas (secciones `:130,:155,:180,:205,:230,:255`): `REPLACE(..., 'Agregar producto', 'Agregar artículo')`, y en `:127,:152…` `REPLACE(..., '🧩 "Productos"', '🧩 "Artículos"')`, y `'Rifa de productos'`. Después de Modelo -> Producto, la pantalla `productos/agregar` se llamará **"Agregar producto"** y su descripción dirá "Catálogo → Agregar producto". Si alguien vuelve a correr ese script (el propio `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.4 lo recomienda: "basta volver a correr"), esas filas pasarían a **"Agregar artículo"**: el menú "Agregar producto" dejaría de existir en Gestión de roles y habría dos "Agregar artículo". Lo mismo con `Excel sin productos`. Mitigación: dejar una nota en la cabecera del script y en `CLAUDE.md`, o crear una versión nueva "ya corrido, no volver a correr", y borrar la advertencia del documento de renombre.

### 6.2 Estado del script de artículos en producción (desconocido)

`CLAUDE.md:656` (en `qa` y en `main`, son idénticos) dice: QA ✅ 2026-10-20 y prod `⏳ cuando la rama llegue a main`. La rama ya llegó (`32cbd8b`). Si el script **no** se corrió en `inventario_key`, prod tiene el front con "Agregar artículo" y la base con "Agregar producto", "Tarjeta de variante", etc. Y el UPDATE nuevo de Modelo -> Producto, si se escribe suponiendo que el script ya corrió, dejaría las filas de `tienda/venta` y `crear-variantes` con texto mezclado. **Hay que preguntar al dueño** y correr los `SELECT` de diagnóstico (1.5 punto 6) en `inventario_key` antes de escribir nada.

Idem `migration_accion_tienda_venta_ver_todos.sql` (la acción `ver-todos-los-modelos`): `CLAUDE.md:655` la da ⏳ en prod. No sé si el paso a prod del 2026-10-15 la incluyó (`PASOS_SUBIDA` no la nombra). Si falta, D9 simplemente no existe en prod.

### 6.3 Permisos que viajan en el JWT

- Cambiar `ruta` o `clave` = el usuario con sesión viva pierde el permiso hasta volver a entrar; peor, un `hasAuthority('productos/buscar:crear-variantes')` en `SecurityConfig.java:326` devolvería 403 a todos, **incluido el admin**. Los permisos se emiten con el token (access 15 min, refresh 7 días, `CLAUDE.md`).
- Solo cambiar **texto** (nombre, etiqueta, descripción, categoría) no toca el JWT: no hace falta volver a entrar, como confirmó el renombre de artículos ("basta recargar la pantalla").
- El encabezado de 64 KB (`server.max-http-request-header-size`) no se afecta: no se agregan permisos.

### 6.4 Pruebas e2e del front

`e2e/tests/` y `e2e/support/` buscan texto: `'Buscar nombre o código…'` (`login.spec.ts:15`, `modelo.spec.ts:10`, `descuento.spec.ts:10`), `/Guardar modelo/` (`support/modelo.ts:31`), `'✅ Modelo guardado'` (`support/modelo.ts:34`), `'Buscar modelo por nombre o código…'` (`support/articulo.ts:28`), `/Guardar producto/` (`support/articulo.ts:21`). **Ya hoy cuatro están rotas por el renombre de artículos** (`TESTS_PENDIENTES.md` front, sección "Tests existentes que quedaron viejos", líneas 368-372). El renombre Modelo -> Producto rompe además `Guardar modelo`, `Modelo guardado` y `Buscar modelo…`. El workflow de e2e corre solo después de cierto despliegue: avisar antes de tocar. Las e2e usan `crearModelo()` para crear datos con código `E2E…`; el limpiador `limpiar_datos_e2e_qa.sql` los busca por código, no por texto.

### 6.5 Menús editados a mano y scripts que buscan por nombre

- Cualquier fila editada desde Gestión de menús queda fuera de los `UPDATE ... = texto exacto`. Quedan con "Modelos". El diagnóstico lo muestra.
- Los scripts viejos que **vuelven a escribir** texto si se vuelven a correr (lista en `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.4): `migration_accion_tienda_habilitar_compartir.sql`, `migration_accion_tienda_carrito.sql`, `migration_accion_tienda_filtros_publicos.sql`, `migration_accion_submenu_categoria.sql` (reescribe `categoria = 'Tarjeta de modelo'` en D10), `migration_fix_orden_escanear_tienda.sql`, `migration_submenu_tienda_buscar_descripcion_escritura.sql`, `migration_descripcion_submenu.sql` (los `CASE` de B1-B5). Si se corre cualquiera, hay que correr después el UPDATE nuevo.
- No encontré código Java que busque un submenú o acción **por su nombre** (todo va por `ruta`/`clave`): un `grep` de `findByNombre` y `getNombre().equals/contains` sobre menú, submenú y acción no devolvió nada en `src/main` (búsqueda por patrón, no revisión completa). Sí hay búsquedas por nombre de **menú** padre en las migraciones (`WHERE nombre = 'Catálogo'`), que no cambia.
- El menú lateral es **estático en el front** (`navbar.component.html:67,69,71`). Cambiar la base no cambia el menú lateral y viceversa. Son dos cambios que deben salir juntos.

### 6.6 Ambigüedad de la palabra "producto"

Después del renombre, "producto" significará: (a) el producto base del admin (nuevo), (b) lo que el cliente ve en Tienda y carrito, que sigue diciendo "producto" a propósito (`RENOMBRE_VARIANTE_A_ARTICULO.md` §1.3, "Lo que ve el cliente siguen diciendo producto") y que es un **artículo**. Ejemplos en la base: `submenu.descripcion` de `favoritos` ("productos favoritos"), `reportes` ("por fecha/artículo/vendedor" ya cambiado), descripción 💲 de Tienda. El front tiene un glosario escrito en `ayuda-pantallas.catalog.ts:29-31` (`MODELO = el artículo base`, `PRODUCTO = lo que sí se vende`) que quedaría **al revés**. Decidir y escribir en un solo lugar (`TAXONOMIA`) el glosario.

### 6.7 Textos que lee el cliente, no solo el admin

El prompt del chatbot dice `"En el catálogo el nombre suele ser el del MODELO"` (`ChatbotBase.java:76`) y los mensajes del back de sección siguiente llegan al admin y al cliente de Tienda con "modelo": `VarianteServiceImpl.java:266-267,751,762,837-838,1241`, `PedidoServiceImpl.java:705`, `EditarArticulosService.java:276`, `MovimientoStockJpaAdapter.java:48,50`, `DisponibilidadStockResponse.java:51`, `AjustarStockModeloService.java:36`. Eran mensajes del renombre de artículos (R20) y quedaron "modelo" a propósito. Son ~12 literales; hay que incluirlos si el alcance es "todo texto visible". Los tests de back no los comparan (`grep` en `src/test` no encontró aserciones con esas frases).

### 6.8 Rama base y flujo Git

`dev` **no** contiene el renombre de artículos. Cualquier trabajo nuevo que salga de `dev` choca con 33 archivos del back y 29 del front (más el script SQL). El flujo del `CLAUDE.md` (`dev -> qa -> main`) quedó roto en esta ocasión; hay que (a) llevar `qa` a `dev` antes de empezar, o (b) abrir la rama del renombre nuevo desde `qa`. Regla del `CLAUDE.md`: "A `main` no sube renombrado suelto" y "Cuando el renombrado esté completo y probado en QA se promueve a `main` como un cambio propio" (esa regla ya se incumplió una vez con la sección "solo dev y qa").

### 6.9 Producción / despliegue

- El push a `main` (back) y a `master` (front) despliega solo en 1-2 minutos (`CLAUDE.md`, "Deployment automático"). Entre que sube el front y se corre el UPDATE habrá una ventana en la que el menú lateral dice una cosa y Gestión de roles otra. Aceptable, pero avisar al dueño del orden: back, front, **luego** UPDATE de la base en QA y en prod (el mismo orden de §2 del documento de renombre).
- `migration_menu_submenu.sql` y varios scripts viejos (`migration_descripcion_submenu.sql`, `migration_descripcion_acciones_modelos.sql`, `migration_filtros_granulares.sql`, `migration_accion_submenu.sql`) están **solo en la raíz** del back, y los demás en `src/main/resources/static/`. Los de `static/` se empaquetan en la imagen Docker; los de la raíz no. Es normal, pero hay dos versiones distintas de `migration_descripcion_acciones_modelos.sql` (raíz y `static/`) y no hay forma de saber cuál corrió.
- Rollback: el UPDATE nuevo es solo de texto; si algo sale mal basta con un UPDATE inverso. Conviene guardar un respaldo de las 16 filas antes (`CREATE TABLE ... AS SELECT` de `submenu` y `accion_submenu`), como se hizo con Jade (`migration_tema_jade.sql`, "respaldo con 39 filas").

---

## 7. Qué no pude verificar (límites de esta auditoría)

- El contenido real de `submenu`, `accion_submenu` y `tema_variable` en QA y en producción. Todo "valor actual" es reconstruido.
- Si `migration_renombre_articulo_etiquetas.sql` y `migration_accion_tienda_venta_ver_todos.sql` corrieron en `inventario_key`.
- Cuál de las dos versiones de `migration_descripcion_acciones_modelos.sql` se ejecutó (afecta D2, D4, D5, D6).
- Si las columnas `descripcion` y `descripcion_escritura` tienen realmente 255 caracteres o más.
- Los conteos de `.md` son por patrón, no leídos línea a línea (pueden fallar en ±5 % por usos como "modelo" en otro sentido).
- No se corrió ningún comando que modifique archivos de los repos; los `git` usados fueron `log`, `diff`, `show`, `branch`, `rev-parse`, `grep` y lecturas.
