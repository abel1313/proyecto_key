# Renombre variante → artículo — qué se movió y cómo probarlo

**Rama:** `rename/variante-a-articulo` en el back (`proyecto_key`) y en el front
(`producto_venta_online`). Las dos salen de `dev` (2026-10-06) y el 2026-10-08 se les volvió a traer
todo lo de `dev` (sección 1.1). **No está en `dev`, `qa` ni `main`**:
se junta a `dev` cuando la apruebes, y de ahí sigue el flujo normal.

Reglas que se siguieron: skill `renombrar` y `CLAUDE.md` → "Renombrado en curso".

---

## 1. Qué se movió y qué no

| Nivel | Qué | ¿Se movió? |
|---|---|---|
| 1. Lo que ve el usuario | Textos de pantalla del front (títulos, botones, avisos) y mensajes de error del back | ✅ Dicen "artículo" |
| 2. Código interno | Comentarios y logs que se tocaron | ✅ Solo donde ya se editaba |
| 3. Contrato con el front | **Rutas**: se agregó el nombre nuevo **sin quitar el viejo** | ✅ `/v2/articulos` convive con `/v1/variantes` |
| 3. Contrato con el front | **Campos del JSON** (`varianteId`, `configurarRifaVariante`…) | ❌ No se tocan |
| 4. Base de datos | Tablas y columnas (`variantes`, `variante_imagen`…) | ❌ No se tocan |
| — | Rutas de pantallas del front (`/tienda/buscar`, `/tienda/detalle/5`…) | ❌ No se tocan |
| — | Nombres de clases (`Variantes`, `VarianteServiceImpl`) | ❌ No se tocan |

### Las rutas que cambian

El back contesta con **los dos nombres**, y llega al **mismo método**, así que no pueden contestar
distinto. El front de esta rama usa el nuevo; el front de prod sigue con el viejo y no se rompe.

| Ruta vieja (sigue viva) | Ruta nueva (la usa el front de la rama) |
|---|---|
| `/v1/variantes/...` (las 30 rutas del recurso) | `/v2/articulos/...` |
| `/v1/variantes/variante/{id}/producto-id` | `/v2/articulos/{id}/producto-id` |
| `/v1/configurarRifaVariante/...` | `/v2/configurarRifaArticulo/...` |
| `/v1/ganadorRifa/continuarVariante/{id}` | `/v1/ganadorRifa/continuarArticulo/{id}` |
| `/v1/productos/admin/sin-variantes/reporte` | `/v1/productos/admin/sin-articulos/reporte` |
| `/v1/productos/compartir-imagenes-variantes` | `/v1/productos/compartir-imagenes-articulos` |
| `/v1/resenas/variante/{id}` (y `/resumen`) | `/v1/resenas/articulo/{id}` (y `/resumen`) |

### Lo que se encontró al revisar (2026-10-06)

| # | Hallazgo | Qué se hizo |
|---|---|---|
| H1 | El renombrado **nunca se había subido**: el back estaba en un `stash` y el front en archivos sin commit, los dos mezclados con la rama del diseño Jade | Se pasó a esta rama propia, sin nada de Jade |
| H2 | `dev` agregó `/v1/variantes/para-pedido` (buscador del detalle de pedido, solo con permiso) **después** del renombrado. Sin copiar la regla, **`/v2/articulos/para-pedido` habría quedado pública** | Regla en espejo en `SecurityConfig` |
| H3 | Dos textos chocaban con colores del diseño Jade | Se dejó solo el texto nuevo; el color sigue como en `dev` |
| H4 | En **Sistema → 🛡️ Gestión de roles** varios permisos dicen "variante" ("Tarjeta de variante", "Habilitar / deshabilitar variante", "Crear variantes"…). Esos textos están **en la base**, no en el código | ✅ 2026-10-08: `migration_renombre_articulo_etiquetas.sql` (sección 1.1) |
| H5 | El micro de imágenes, el chatbot y los interceptores del front **no** usan rutas de variantes | Nada que mover |
| H6 | (Anotado por el dueño, 2026-10-06) **Catálogo → 🧩 Agregar producto** en realidad da de alta un **artículo** de un modelo que ya existe: el menú debería decir **"Agregar artículo"**. El texto del menú está **en la base** (tabla `submenu`), igual que H4. En la tarjeta del modelo (**Catálogo → 🔍 Modelos**) el botón **🧩 Productos** debe decir **🧩 Artículos**, y su ventana "Inicializar variantes" / "Crear variantes" → **"Crear artículos"** (`all.component`). Lo mismo la ayuda de pantalla (`ayuda-pantallas.catalog.ts`: "Nuevo Producto") y el texto de Agregar Modelo que manda a «Agregar producto» (`add.component.html`) | ✅ 2026-10-08: textos del front en esta rama + el mismo script de H4 para el menú |
| H7 | (Anotado por el dueño, 2026-10-06) En esa misma pantalla, al **elegir un modelo que ya existe**, todo lo que el artículo comparte con el modelo tiene que llegar **lleno y a la vista**: hoy precarga color, marca y descripción, pero el dueño vio que **contenido neto** y **categoría** no se llenan. La categoría no viaja en la búsqueda de modelos (`ProductoDTO` no la trae) y por eso el formulario sale sin ella aunque el back sí la copia al guardar | ⏳ Pendiente en `dev`: agregar la categoría a la búsqueda de modelos y precargarla; revisar por qué contenido neto no llegó (ver la consulta en `TESTS_PENDIENTES.md`, entrada 2026-10-06 "Crear artículos hereda del modelo") — ✅ hecho en `dev` el 2026-10-07; llegó a esta rama con el merge del 2026-10-08 |

**Compila:** back `mvn -q compile` ✅ · front `ng build --configuration development` ✅.

### 1.1 Retomado el 2026-10-08

**Pedido del dueño:** *"retomarla y hacer una búsqueda extensa para ya dejar cambiado variante por
artículo en todo, y que las búsquedas digan artículo, menos en Tienda → Buscar"*.

**1. Se trajo `dev` completo** (back y front, con todo lo del 2026-10-08). Conflictos resueltos con la
lógica de `dev` y el texto en "artículo": `VarianteServiceImpl` (crear artículos y habilitar), la
ventana 🧩 Agregar artículos de Modelos, el carrito y Agregar artículo.

**2. Búsqueda completa.** Se revisó cada texto que ve una persona: lo que se pinta en las pantallas
(`.html`, incluidos `title`, `placeholder`, `alt` y `aria-label`), los textos de los avisos (`.ts`)
y los mensajes del back. Resultado:

| Dónde | Qué quedaba | Cómo quedó |
|---|---|---|
| Front, pantallas | "+ Agregar variante" (Rifas → Agregar rifa) y "varianteId" (Diagnóstico de imágenes) | "+ Agregar artículo", "Id del artículo" |
| Front, avisos (`.ts`) | Nada: ya decían "artículo" | — |
| Back, mensajes al usuario | Nada: ya decían "artículo" | — |
| Back, logs y Swagger | Dicen "variante" | **Se quedan así**: no los ve el dueño ni el cliente, y hablan de la tabla `variantes` |
| Base de datos (menú y Gestión de roles) | "Agregar producto", "Tarjeta de variante", "Habilitar / deshabilitar variante", "🧩 Productos", "Excel sin productos"… | Script `migration_renombre_articulo_etiquetas.sql` |

**3. Los buscadores dicen "artículo"** (regla del dueño), y los que buscan modelos dicen "modelo":

| Pantalla | Antes | Ahora |
|---|---|---|
| Ventas → 💰 Venta directa | 🔍 Buscar producto · "Nombre o código de barras…" | 🔍 Buscar artículo · "Buscar artículo por nombre o código de barras…" |
| Pedidos → Mis pedidos → 👁 Detalle → ➕ / ⇄ | "Buscar por nombre o código de barras…" | "Buscar artículo por nombre o código de barras…" |
| Ventas → 💳 Créditos / Abonos → ↪ Aplicar a otro producto | "Buscar producto" · "Nombre, código o descripción…" | **↪ Aplicar a otro artículo** · "Buscar artículo" · "Buscar artículo por nombre, código o descripción…" |
| Admin → 🎁 Gestión Promociones | "Agregar producto al combo" · "Buscar por nombre, talla o color…" | "Agregar artículo al combo" · "Buscar artículo por nombre, talla o color…" |
| Marketing → Publicar en redes | "¿Es de algún producto?" · "Código, nombre o categoría…" | "¿Es de algún artículo?" · "Buscar artículo por código, nombre o categoría…" |
| Rifas → Agregar rifa | "+ Agregar variante" · "Buscar por nombre, color o código…" | "+ Agregar artículo" · "Buscar artículo por nombre, color o código…" |
| Rifas → Rifa mensual | "Buscar por nombre, talla o color…" | "Buscar artículo por nombre, talla o color…" |
| Rifas → Boletos → Agregar premio | "Buscar producto por nombre o código…" · "Ningún producto coincide" | "Buscar artículo…" · "Ningún artículo coincide" |
| Sistema → Diagnóstico de imágenes | pestañas 📦 Modelo / 🏷️ Producto, las dos "Buscar producto o código…" | 📦 Modelo ("Buscar modelo por nombre o código…") / 🏷️ **Artículo** ("Buscar artículo por nombre o código…") |
| Catálogo → 🔍 Modelos | encabezado "Productos / Catálogo de productos" · "Buscar nombre o código…" · botón **🧩 Productos** | "Modelos / Catálogo de modelos (producto base)" · "Buscar modelo por nombre o código…" · botón **🧩 Artículos** |
| Catálogo → 🧩 Agregar artículo (`tienda/venta`) | título "Nuevo Producto", "Productos adicionales", "💾 Guardar producto(s)" | "Nuevo artículo", "Artículos adicionales", "💾 Guardar artículo(s)" |
| Editar artículo (`tienda/update`) | sección "Producto" · "Buscar nombre o código…" | sección "Modelo" · "Buscar modelo por nombre o código…" |
| Catálogo → ➕ Agregar modelo | "…agrégale productos con su talla y color desde «Agregar producto»" | "…agrégale artículos con su talla y color desde «Agregar artículo»" |
| Menú lateral | "🧩 Agregar producto" y sus explicaciones | "🧩 Agregar artículo"; el modelo se explica como "producto base" |
| ⓘ Ayuda de pantalla | "Nuevo Producto" | "Agregar artículo" |
| **Tienda → Buscar** | "Buscar nombre o código…" | **Sin cambio** (lo pidió el dueño: es lo que ve el cliente) |

Lo que ve el **cliente** (carrito, ficha del artículo, chat) sigue diciendo "producto" donde ya lo
decía: no se tocó. Las pruebas automáticas (`e2e/`) se ajustaron a los textos nuevos.

**4. Script de la base:** `src/main/resources/static/migration_renombre_articulo_etiquetas.sql`.
Cambia solo textos (`submenu.nombre/descripcion/descripcion_escritura` y
`accion_submenu.etiqueta/descripcion/categoria`); **no** toca claves, rutas ni permisos, así que
nadie gana ni pierde acceso y no hay que volver a entrar. Se probó en una base desechable con los
textos que dejaron las migraciones: dos corridas seguidas (la segunda no cambia nada) y con el modo
"safe updates" de MySQL Workbench prendido. Al final trae dos consultas que deben salir **vacías** y
una que debe decir **Agregar artículo**. Se corre cuando la rama llegue a ese ambiente (primero
`inventario_key_qa`, luego prod).

---

## 2. Orden para subirlo (importante)

1. **Back primero** (a `dev` → `qa`). Agrega las rutas nuevas sin quitar las viejas: el front de
   QA de ese momento sigue funcionando igual.
2. **Front después.** Si el front sube antes, llamaría `/v2/...` a un back que todavía no las tiene
   → todo lo de artículos daría 404.
3. A `main` sube igual: back y luego front. Las rutas viejas no se quitan hasta que el front de prod
   ya no las use.

---

## 3. Cómo ver a qué ruta llama una pantalla

En la computadora, con la pantalla abierta:
1. `F12` → pestaña **Red** (Network).
2. En el filtro escribe **`articulos`** (o **`variantes`** para ver las viejas).
3. Haz la acción de la prueba. Cada fila es una llamada: la columna **Estado** debe decir **200**
   (o 201), nunca 401, 403 ni 404.

---

## 4. Antes de subir: línea base en QA (hoy)

Corre las pruebas de la sección 6 **en QA tal como está hoy** y anota lo que salga en la columna
"Antes" (cuántos resultados, el total, el texto). Con el filtro `variantes` en la pestaña Red verás
las rutas viejas. Después de subir la rama, se corren **los mismos pasos con los mismos datos** y
tiene que salir lo mismo, salvo lo que la prueba dice que cambia (el texto "artículo" y la ruta).

**Datos que conviene anotar antes:** un modelo con 2 o más artículos y stock (código de barras:
____), un artículo con fotos (id: ____), un pedido Ir pagando con 2 artículos (#____), una rifa con
artículos (#____).

---

## 5. Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| R1 | `/v1/variantes/buscar`, `buscar-filtrado`, `filtros-disponibles`, `porProducto/.../resumen` | Catálogo de la tienda (sin sesión) | **Tienda → Buscar** | Lista de artículos con filtros | **Lo mismo**, por `/v2/articulos` |
| R2 | `.../producto-id`, `porProducto`, `imagenes/.../paginado`, `resenas/variante` | Ficha de un artículo por link directo (sin sesión) | `/tienda/detalle/{id}` | Ficha con fotos, artículos hermanos y reseñas | **Lo mismo** |
| R3 | `habilitar`, `admin/habilitar-lote`, `deleteBy`, `admin/filtrar` | Tarjeta de artículo (admin) | **Tienda → Buscar** con sesión | Habilitar, en lote, dar de baja | **Lo mismo**; mensajes dicen "artículo" |
| R4 | `getOne`, `guardarConImagenes`, `{id}/imagenes`, `imagenes/.../paginado` | Editar artículo | ✏️ en la tarjeta → `tienda/update` | Guarda cambios y fotos | **Lo mismo** |
| R5 | `guardarConImagenes` | Alta de artículo | **Catálogo → 🧩 Agregar artículo** (antes "Agregar producto") | "Nuevo Producto", "¡Variante creada!" | **"Nuevo artículo"**, **"¡Artículo creado!"**, mismo resultado |
| R6 | `guardarConImagenes` (ventana 🧩), `sin-articulos/reporte`, `compartir-imagenes-articulos` | Tarjeta del modelo | **Catálogo → 🔍 Modelos** | botón "🧩 Productos", Excel `productos_sin_variantes.xlsx` | botón **"🧩 Artículos"** (abre 🧩 Agregar artículos), Excel `productos_sin_articulos.xlsx` |
| R7 | `/v2/articulos/buscar` + venta | Venta en mostrador | **Ventas → 💰 Venta directa** | Busca, cobra, baja stock | **Lo mismo, mismos totales** |
| R8 | `/v2/articulos/buscar` y `para-pedido` | ➕ Agregar artículo y ⇄ del detalle | **Pedidos → Mis pedidos** → 👁 Detalle | Busca solo lo que se puede vender | **Lo mismo** |
| R9 | `/v2/articulos/buscar` | Buscador de Créditos / Abonos | **Ventas → 💳 Créditos / Abonos** | Encuentra el artículo | **Lo mismo** |
| R10 | `/v2/configurarRifaArticulo/...`, `continuarArticulo`, `imagenes` | Rifas | **Rifas → Agregar rifa** y **Rifas → Boletos → paso Ruleta** | Agrega/quita artículos de la rifa, sortea, continúa | **Lo mismo**; "Artículo eliminado y stock restaurado" |
| R11 | `admin/filtrar` | Armar combo | **Admin → 🎁 Gestión Promociones** | Busca artículos para el combo | **Lo mismo**; avisos dicen "artículo" |
| R12 | `/v2/articulos/buscar` | Elegir artículo a publicar | **Marketing → Publicar en redes** | Encuentra el artículo y su foto | **Lo mismo** |
| R13 | `getOne`, `guardarConImagenes`, `imagenes` | Fotos de ramos | **Flores eternas → Ramos admin** | Lee el artículo del ramo y sube foto | **Lo mismo** |
| R14 | `admin/diagnostico-imagenes`, `buscar` | Diagnóstico | `admin/diagnostico-imagenes` | Diagnóstico de fotos de un artículo | **Lo mismo** |
| R15 | Reglas de `SecurityConfig` | Permisos | Usuario con rol limitado | Lo que no tiene permitido da 403 | **Lo mismo** en `/v2` (y `para-pedido` **no** público) |
| R16 | Texto del chatbot ("CATÁLOGO ACTUAL…") | Chat de la tienda | Burbuja de chat en la tienda | Contesta con productos | **Lo mismo** |
| R18 | Textos de los buscadores (2026-10-08) | Venta directa, detalle de pedido, Créditos / Abonos, Promociones, Publicar en redes, Rifas, Diagnóstico, Editar artículo | Cada pantalla | "Buscar producto…", "Buscar por nombre…" | "Buscar artículo…" (o "Buscar modelo…" donde se busca un modelo); **lo mismo encontrado** |
| R19 | Textos de la base (`migration_renombre_articulo_etiquetas.sql`) | Menú Catálogo y Gestión de roles | Sistema → 🛡️ Gestión de roles | "Agregar producto", "Tarjeta de variante"… | "Agregar artículo", "Tarjeta de artículo"…; **mismos permisos** |
| R17 | **El cambio mismo** | — | Pestaña Red | Llamadas a `/v1/variantes/...` | Llamadas a `/v2/articulos/...`, y `/v1` sigue contestando |

---

## 6. Pruebas paso a paso

> Antes de cada prueba: `Ctrl + Shift + R` en QA. Ten la pestaña **Red** abierta con el filtro
> `articulos`. Si algo no sale como dice "Debes ver", escribe debajo `💬` y lo que pasó.

### 🔴 R1 — Tienda → Buscar, sin sesión

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre una ventana de incógnito → `qa.shop.novedades-jade.com.mx` → **Tienda → Buscar** | La lista de artículos, **igual que en tu línea base** (mismos primeros 5) |
| 2 | Escribe 3 letras de un modelo (ej. `bol`) | Los mismos resultados que antes. En Red: `v2/articulos/buscar…` con **200** |
| 3 | Aplica un filtro (talla o color) | Mismo número de resultados. En Red: `buscar-filtrado` y `filtros-disponibles` con **200** |
| 4 | Abre un modelo con varios artículos | Sus artículos, igual que antes |

⛔ No puede pasar: lista vacía, spinner que no se quita, 401/403/404 en Red.

### 🔴 R2 — Ficha de un artículo por link directo, sin sesión

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En incógnito abre `qa.shop.novedades-jade.com.mx/tienda/detalle/<id del artículo con fotos>` | La ficha con nombre, precio y fotos, igual que antes |
| 2 | Baja a los otros artículos del modelo y a las reseñas | Igual que antes. En Red: `v2/articulos/<id>/producto-id`, `porProducto`, `resenas/articulo/<id>` con **200** |

⛔ No puede pasar: "producto no encontrado", fotos que no cargan.

### 🔴 R3 — Tarjeta del artículo como administrador (Tienda → Buscar)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra con tu usuario admin → **Tienda → Buscar** | Igual que antes |
| 2 | Apaga el interruptor de habilitar de un artículo de prueba | Se deshabilita. En Red: `PUT …/v2/articulos/<id>/habilitar` con **200** |
| 3 | Vuélvelo a habilitar | Queda habilitado |
| 4 | Selecciona 2 artículos y usa habilitar/deshabilitar en lote | Se aplican a los 2. Si sale aviso del sistema, dice **"artículos"**, nunca "variantes" |
| 5 | **Dar de baja** un artículo de prueba (que no uses) | Desaparece de la tienda. En Red: `DELETE …/v2/articulos/deleteBy/<id>` con **200** |

⛔ No puede pasar: 403 siendo administrador; que diga "variante" en un aviso.

### 🔴 R4 — Editar un artículo (✏️ en la tarjeta)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la tarjeta del artículo con fotos toca **✏️ Editar** | Se abre con sus datos y fotos, igual que antes |
| 2 | Cambia la talla, guarda | Guarda. En Red: `POST …/v2/articulos/guardarConImagenes` con **200** |
| 3 | Quita una foto y agrega otra, guarda | La foto quitada ya no sale; la nueva sí |

### 🔴 R5 — Catálogo → 🧩 Agregar artículo (antes "Agregar producto")

| # | Haz esto | Debes ver |
|---|---|---|
| 0 | Abre la pantalla | Título **"Nuevo artículo"**; el buscador dice **"Buscar modelo por nombre o código…"** |
| 1 | Llena un artículo de prueba de un modelo con stock libre y guarda | El botón dice **"💾 Guardar artículo"**. Aviso **"¡Artículo creado!"** (antes decía "¡Variante creada!") |
| 2 | Búscalo en **Tienda → Buscar** | Aparece (si tiene foto y stock) |

### 🔴 R6 — Catálogo → 🔍 Modelos (tarjeta del modelo)

| # | Haz esto | Debes ver |
|---|---|---|
| 0 | Abre la pantalla | Encabezado **"Modelos"**; el buscador dice **"Buscar modelo por nombre o código…"** |
| 1 | En un modelo **con stock libre**, toca **🧩 Artículos** (antes "🧩 Productos") | Se abre la ventana **🧩 Agregar artículos** (la misma de Agregar modelo) |
| 2 | Crea 1 con stock 1 | Aviso **"1 artículo guardado"**. En Red: `v2/articulos/guardarConImagenes` con **200** |
| 3 | Repite en un modelo **sin stock libre** | Aviso **"No queda stock para artículos nuevos"** y no se abre la ventana |
| 4 | Descarga el Excel de modelos sin artículos | El archivo se llama **`productos_sin_articulos.xlsx`** y la hoja **"Productos Sin Artículos"** |
| 5 | En el detalle de un modelo, comparte sus imágenes a sus artículos | Igual que antes. En Red: `compartir-imagenes-articulos` con **200** |

### 🔴 R7 — Ventas → 💰 Venta directa

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Busca un artículo con stock (3 letras) | Sale, igual que antes |
| 2 | Agrega 1 pieza y cobra en efectivo | Total **igual al precio del artículo** (anota: $____). El stock del artículo baja 1 |

⛔ No puede pasar: total distinto al de la línea base con el mismo artículo.

### 🔴 R8 — Pedidos → Mis pedidos → 👁 Detalle

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre el pedido Ir pagando de tu línea base → **➕ Agregar artículo** → escribe 3 letras | Solo artículos que se pueden vender, igual que antes. En Red: `v2/articulos/para-pedido` con **200** |
| 2 | Toca **⇄** en un artículo y busca otro | Igual que antes |
| 3 | Cierra sin guardar | El pedido no cambia |

### 🔴 R9 — Ventas → 💳 Créditos / Abonos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Usa el buscador de artículo de la pantalla | Encuentra los mismos que antes |

### 🔴 R10 — Rifas

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Rifas → Agregar rifa** → abre la rifa de tu línea base | Sus artículos y palabras clave, igual que antes. En Red: `v2/configurarRifaArticulo/porRifa/<id>` con **200** |
| 2 | Agrega un artículo y cámbiale la palabra clave | Se guarda |
| 3 | Quítalo con ✕ | Se quita y el stock del artículo regresa. Si sale aviso, dice **"Artículo eliminado y stock restaurado"** |
| 4 | **Rifas → Boletos → paso Ruleta** → **Continuar** (con una rifa de prueba) | Pasa al siguiente artículo igual que antes. En Red: `ganadorRifa/continuarArticulo/<id>` con **200** |

### 🔴 R11 — Admin → 🎁 Gestión Promociones

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Arma un combo nuevo y busca artículos | Salen igual que antes |
| 2 | Agrega el mismo artículo dos veces | Aviso **"Este artículo ya está en el combo"** |
| 3 | Intenta guardar sin artículos | Aviso **"Agrega al menos un artículo al combo"** |

### 🔴 R12 — Marketing → Publicar en redes

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Busca un artículo para publicar (no publiques) | Sale con su foto, igual que antes |

### 🔴 R13 — Flores eternas → Ramos admin

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre un ramo y su foto | Igual que antes |
| 2 | Cambia la foto de un ramo de prueba | Se guarda |

### 🔴 R14 — Diagnóstico de imágenes (`admin/diagnostico-imagenes`)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Busca el artículo con fotos y diagnostícalo | Mismo resultado que antes |

### 🔴 R15 — Permisos (necesita un usuario con rol limitado)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Con un rol que tiene **Tienda → Buscar** pero **sin** la acción "Habilitar", intenta deshabilitar un artículo | No lo deja (403), **igual que antes** |
| 2 | Con un rol **sin** "Agregar artículo" en Mis pedidos, abre el detalle de un pedido | No sale el botón ➕, igual que antes |
| 3 | Sin sesión, abre en el navegador `https://qa.backend.novedades-jade.com.mx/mis-productos/v2/articulos/para-pedido?termino=bol` | **401** (no deja). Antes, con `/v1/variantes/para-pedido`, también 401 |
| 4 | Sin sesión, abre `…/mis-productos/v2/articulos/getAll?page=0&size=1` | **401** (igual que `/v1/variantes/getAll`) |

⛔ No puede pasar: que alguna de las rutas `/v2` deje entrar donde `/v1` no deja.

### 🔴 R16 — Chat de la tienda

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la tienda abre el chat y pregunta "¿tienen bolsas negras?" | Contesta con productos, igual que antes |

### 🔴 R17 — El cambio mismo (al final)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Con Red filtrando `variantes`, navega Tienda → Buscar, una ficha, Venta directa y Modelos | **Ninguna** llamada a `/v1/variantes/...` del front de la rama |
| 2 | Con Red filtrando `articulos` | Todas las llamadas de artículos van a `/v2/articulos/...` con 200 |
| 3 | Sin sesión, abre `https://qa.backend.novedades-jade.com.mx/mis-productos/v1/variantes/buscar?termino=bol` y luego `…/v2/articulos/buscar?termino=bol` | **El mismo JSON** en las dos (el `/v1` sigue vivo para el front de prod) |

### 🔴 R18 — Los buscadores dicen "artículo" (2026-10-08)

Solo cambia el texto: lo que encuentra cada buscador tiene que ser **lo mismo que antes** con las
mismas 3 letras.

| # | Dónde | Debes ver en el buscador |
|---|---|---|
| 1 | Ventas → 💰 Venta directa | **🔍 Buscar artículo** · "Buscar artículo por nombre o código de barras…" |
| 2 | Pedidos → Mis pedidos → 👁 Detalle → ➕ Agregar artículo | "Buscar artículo por nombre o código de barras (mínimo 3 letras)" |
| 3 | Ventas → 💳 Créditos / Abonos → un pedido cancelado → **↪ Aplicar a otro artículo** | "Buscar artículo" · "Buscar artículo por nombre, código o descripción…" |
| 4 | Admin → 🎁 Gestión Promociones → nuevo combo | "Agregar artículo al combo" · "Buscar artículo por nombre, talla o color…" |
| 5 | Marketing → Publicar en redes | "¿Es de algún artículo?" · "Buscar artículo por código, nombre o categoría…" |
| 6 | Rifas → Agregar rifa → **+ Agregar artículo** | "Buscar artículo por nombre, color o código…" |
| 7 | Rifas → Rifa mensual (paso del premio) y Rifas → Boletos → Agregar premio | "Buscar artículo…" |
| 8 | Sistema → Diagnóstico de imágenes | Pestañas **📦 Modelo** y **🏷️ Artículo**, cada una con su buscador |
| 9 | ✏️ en la tarjeta de un artículo (Editar) | Sección **Modelo** · "Buscar modelo por nombre o código…" |
| 10 | **Tienda → Buscar** | **"Buscar nombre o código…", igual que antes** (no cambia) |

⛔ No puede pasar: que algún buscador encuentre distinto que antes, o que siga diciendo "variante".

### 🔴 R19 — Menú y Gestión de roles (después de correr el script)

**Antes de empezar:** correr `migration_renombre_articulo_etiquetas.sql` en `inventario_key_qa` y
recargar la pantalla (no hace falta volver a entrar).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Las consultas del final del script | Las dos primeras **vacías**; la tercera dice **Agregar artículo** |
| 2 | Sistema → 🛡️ Gestión de roles → un rol → Catálogo | La pantalla **Agregar artículo** (antes "Agregar producto") |
| 3 | En la misma pantalla → Tienda → Buscar | Categoría **"Tarjeta de artículo"**; "Habilitar / deshabilitar artículo"; las ℹ️ dicen "artículo" |
| 4 | En la misma pantalla → Catálogo → Modelos | "Crear artículos desde el modelo (🧩 "Artículos" en la tarjeta)", "Descargar Excel sin artículos (📥 …)" |
| 5 | Con un rol que ya tenía esos permisos, entra a Modelos y a Tienda → Buscar | Los mismos botones que antes (el script no cambia permisos) |
| 6 | Corre el script otra vez | No cambia nada (0 filas) |

⛔ No puede pasar: que un rol pierda o gane un botón; que alguna etiqueta diga "el artículo" donde
debía decir "del artículo" (o "de el").

- [ ] R1 · [ ] R2 · [ ] R3 · [ ] R4 · [ ] R5 · [ ] R6 · [ ] R7 · [ ] R8 · [ ] R9 · [ ] R10 · [ ] R11 · [ ] R12 · [ ] R13 · [ ] R14 · [ ] R15 · [ ] R16 · [ ] R17 · [ ] R18 · [ ] R19
