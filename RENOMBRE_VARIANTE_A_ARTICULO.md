# Renombre variante → artículo — qué se movió y cómo probarlo

**Rama:** `rename/variante-a-articulo` en el back (`proyecto_key`) y en el front
(`producto_venta_online`). Las dos salen de `dev` (2026-10-06). **No está en `dev`, `qa` ni `main`**:
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
| H4 | En **Sistema → 🛡️ Gestión de roles** varios permisos dicen "variante" ("Tarjeta de variante", "Habilitar / deshabilitar variante", "Crear variantes"…). Esos textos están **en la base**, no en el código | ⏳ Pendiente: necesita un script `.sql` propio, probado en una base desechable. No entra en esta rama |
| H5 | El micro de imágenes, el chatbot y los interceptores del front **no** usan rutas de variantes | Nada que mover |

**Compila:** back `mvn -q compile` ✅ · front `ng build --configuration development` ✅.

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
| R5 | `guardarConImagenes` | Alta de artículo | **Catálogo → 🧩 Agregar producto** | "¡Variante creada!" | **"¡Artículo creado!"**, mismo resultado |
| R6 | `inicializarDesdeProducto`, `sin-articulos/reporte`, `compartir-imagenes-articulos` | Tarjeta del modelo | **Catálogo → 🔍 Modelos** | "Inicializar variantes", Excel `productos_sin_variantes.xlsx` | **"Crear artículos"**, Excel `productos_sin_articulos.xlsx` |
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

### 🔴 R5 — Catálogo → 🧩 Agregar producto (alta de artículo)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Llena un artículo de prueba de un modelo con stock libre y guarda | Aviso **"¡Artículo creado!"** (antes decía "¡Variante creada!") |
| 2 | Búscalo en **Tienda → Buscar** | Aparece (si tiene foto y stock) |

### 🔴 R6 — Catálogo → 🔍 Modelos (tarjeta del modelo)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En un modelo **con stock libre**, toca 🧩 | La ventana dice **"Crear artículos"**, **"Cantidad de artículos"**, botón **"Crear artículos"** |
| 2 | Crea 1 | Aviso **"1 artículo(s) creado(s)"**. En Red: `inicializarDesdeProducto` con **201** |
| 3 | Repite en un modelo **sin stock libre** | Error **"Stock insuficiente para crear 1 artículos del producto N. Stock disponible: 0"** (antes decía "variantes") |
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

- [ ] R1 · [ ] R2 · [ ] R3 · [ ] R4 · [ ] R5 · [ ] R6 · [ ] R7 · [ ] R8 · [ ] R9 · [ ] R10 · [ ] R11 · [ ] R12 · [ ] R13 · [ ] R14 · [ ] R15 · [ ] R16 · [ ] R17
