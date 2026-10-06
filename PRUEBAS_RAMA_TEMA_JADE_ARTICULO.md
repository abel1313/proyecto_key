# Pruebas de la rama `feature/tema-jade-articulo` — diseño Jade y "variante" → "artículo"

Rama propia en los dos repos (back `proyecto_key` y front `producto_venta_online`). **No está en QA
todavía**: se junta a `dev` → `qa` cuando digas. Este documento junta todo lo que hay que probar de
la rama, con el formato de siempre (🔴 pendiente, ✅ debe pasar, ⛔ no puede pasar, 💬 tus notas).

**Cómo leerlo:**
- **Parte A** — el diseño Jade (cómo se ve).
- **Parte B** — el cambio de nombre "variante" → "artículo": rutas nuevas del back, textos de pantalla y
  todo lo que depende de eso (skill `pruebas-de-impacto`).
- Cada prueba de la parte B tiene **antes** y **después**. El "antes" se mide **hoy en QA**, que
  todavía tiene la versión vieja; el "después", con los mismos datos, cuando la rama ya esté en QA.

---

## ▶ Orden para probar

| Orden | Prueba | Cuándo | Estado |
|---|---|---|---|
| 0 | **B.0** — Anotar los datos de prueba y medir el "antes" en QA | **Hoy**, antes de juntar la rama | 🔴 pendiente |
| 1 | **A** — Diseño Jade (5 casos) | Cuando la rama esté en QA y se corra `migration_tema_jade.sql` | 🔴 pendiente |
| 2 | **B.1 a B.10** — Todo lo que usa artículos debe dar lo mismo que antes | Cuando la rama esté en QA | 🔴 pendiente |
| 3 | **B.11** — Textos que cambiaron de "variante" a "artículo" | Cuando la rama esté en QA | 🔴 pendiente |
| 4 | **B.12** — El cambio mismo: las llamadas ya van a las rutas nuevas | Al final | 🔴 pendiente |

**Siempre, antes de probar:** recarga forzada (`Ctrl + Shift + R`).

---

## Parte A — Diseño Jade

### Qué cambió
- El diseño **Jade** (verde jade y dorado, letra Inter, títulos sin negrita, botones delineados) queda
  **por default**. Lo deja así `migration_tema_jade.sql` (respalda antes la tabla de colores, se puede
  regresar).
- Los diseños que ya había siguen ahí y se pueden elegir en **Personalización**, ahora completos:
  colores **y** letra, tamaños y botones.
- **Login, Olvidé mi contraseña y Registro no cambian** con ningún diseño.

### 🔴 PRUEBA PENDIENTE — A

**Antes de empezar:** que la rama ya esté en QA y que se haya corrido `migration_tema_jade.sql` en
`inventario_key_qa` (te aviso yo cuando quede).

**A.1 — Jade por default**
1. Entra a QA con tu usuario y recorre **🛍️ Tienda**, **🔍 Modelos**, **Mis pedidos** y **Reportes**.
   - ✅ Verde jade con detalles dorados, letra **Inter**, títulos sin negrita.
   - ⛔ Texto blanco sobre fondo blanco, botones sin contorno que no se distinguen, o pantallas que se
     quedaron con el verde de antes.

**A.2 — Los diseños se pueden elegir**
1. Menú **Sistema → 🎨 Personalización** → sección **✨ Diseños predefinidos**.
   - ✅ Salen 5: **Jade** (con la etiqueta **Predeterminado** y **en uso** de día y de noche),
     **Clásico**, **Jade profundo elevado**, **Neutros cálidos de boutique** y **Teal transformador**.
2. En **Clásico** toca **Usar para día y noche**.
   - ✅ Toda la app regresa al verde de marca de antes, con letra **Poppins**.
3. En **Jade** toca **Usar para día y noche**.
   - ✅ Regresa a Jade.
   - ⛔ Que algún diseño cambie solo los colores y deje la letra o los botones del anterior.

**A.3 — Día y noche**
1. Con Jade, cambia a modo noche (el botón de modo del menú) y recorre las mismas 4 pantallas.
   - ✅ Fondo oscuro, textos claros y legibles, dorado en los detalles.
   - ⛔ Textos oscuros sobre fondo oscuro.

**A.4 — Login y registro sin cambios**
1. Cierra sesión. Mira **Iniciar sesión**, **¿Olvidaste tu contraseña?** y **Registrarse**.
   - ✅ Se ven exactamente como hoy (mismos colores, misma letra), con Jade o con cualquier otro diseño.
   - ⛔ Que cambien de color o de letra.

**A.5 — Celular**
1. Repite A.1 en el celular (o con la ventana angosta).
   - ✅ Nada se sale de la pantalla ni se encima.

- [ ] A.1 Jade por default
- [ ] A.2 Elegir Clásico y volver a Jade
- [ ] A.3 Noche legible
- [ ] A.4 Login y registro iguales
- [ ] A.5 Celular

---

## Parte B — "Variante" → "artículo"

### Qué se movió

**1. Rutas del back (contrato con el front).** Las viejas **siguen funcionando igual**: el front de
producción las usa y no se puede romper. Las nuevas llegan **al mismo código** que las viejas.

| Antes (sigue viva) | Ahora (la usa el front de la rama) |
|---|---|
| `/v1/variantes/**` (todas) | `/v2/articulos/**` (las mismas) |
| `GET /v1/variantes/variante/{id}/producto-id` | `GET /v2/articulos/{id}/producto-id` |
| `/v1/configurarRifaVariante/**` | `/v2/configurarRifaArticulo/**` |
| `POST /v1/ganadorRifa/continuarVariante/{id}` | `POST /v1/ganadorRifa/continuarArticulo/{id}` |
| `GET /v1/resenas/variante/{id}` y `.../resumen` | `GET /v1/resenas/articulo/{id}` y `.../resumen` |
| `GET /v1/productos/admin/sin-variantes/reporte` | `GET /v1/productos/admin/sin-articulos/reporte` |
| `POST /v1/productos/compartir-imagenes-variantes` | `POST /v1/productos/compartir-imagenes-articulos` |

Cuando cambia el nombre del **recurso completo** se usa `/v2/`; cuando el recurso se llama igual y
solo cambia una palabra de una subruta, la ruta nueva convive en la misma versión.

**2. Seguridad.** Cada regla de `SecurityConfig` de las rutas viejas tiene su par en la nueva, en el
mismo lugar. Una prueba automática (`RenombreArticuloSecurityTest`) pega a las 28 rutas viejas y a sus
28 nuevas con 14 tipos de usuario (sin sesión, admin, y cada permiso que decide) y exige **la misma
respuesta**: antes del cambio daban distinto 183 de 392 combinaciones; ahora 0.

**3. Textos.** Lo que ve el usuario dice "artículo": avisos, botones, títulos y mensajes de error del
back (lista completa en B.11). Lo que **no** cambia: nombres de tablas y columnas, nombres de campos del
JSON (`varianteId`), las claves de permisos (`crear-variantes`) y los nombres de menús y acciones que
vienen de la base (Gestión de roles).

### Mapa de impacto — quién usa lo que se movió

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| B.1 | `/buscar-filtrado`, `/filtros-disponibles`, `/admin/filtrar` | Buscador y filtros de la tienda | 🛍️ Tienda | lista de artículos con filtros | **lo mismo** |
| B.2 | `/buscar` | 6 buscadores de artículos | 💰 Venta directa · 👁 Detalle de pedido (➕/⇄) · 💳 Créditos / Abonos · 🔍 Diagnóstico de imágenes · Publicar en redes · Palabras clave | resultados del buscador | **lo mismo** |
| B.3 | `/porProducto`, `/imagenes`, `/producto-id`, `/resenas/articulo` | Ficha del artículo y sus reseñas | Tienda → abrir un artículo · link directo | fotos, tallas, reseñas | **lo mismo** |
| B.4 | `/guardarConImagenes`, `/imagenes` (borrar, principal) | Alta y edición de artículos; fotos de flores | Agregar artículo · ✏️ Editar · Catálogos de flores | se guarda | **lo mismo**, aviso con "artículo" |
| B.5 | `/{id}/habilitar`, `/admin/habilitar-lote`, `/deleteBy` | Botones de la tarjeta en Tienda | 🛍️ Tienda | habilita / da de baja | **lo mismo** |
| B.6 | `/inicializarDesdeProducto`, `/compartir-imagenes-articulos`, `/sin-articulos/reporte` | Botones de Modelos | 🔍 Modelos | crea artículos, comparte fotos, Excel | **lo mismo**, textos con "artículo" |
| B.7 | `/{id}/independizar` | Convertir en modelo propio | Ficha del artículo | crea modelo nuevo | **lo mismo** |
| B.8 | `/v2/configurarRifaArticulo`, `continuarArticulo`, `/buscar-filtrado`, `/imagenes` | Armar y sortear una rifa | 🎡 Rifa de productos · 🎟️ Boletos de rifa | premios, sorteo | **lo mismo** |
| B.9 | `/admin/diagnostico-imagenes` · `/v2/articulos/imagenes` | Diagnóstico y fotos del chatbot | 🔍 Diagnóstico de imágenes · chat de la tienda | diagnóstico / foto | **lo mismo** |
| B.10 | Reglas de seguridad | Quién puede qué | sin sesión, admin, rol limitado | — | **lo mismo** |
| B.11 | Textos | Avisos y botones | varias | "variante" | "artículo" |
| B.12 | **El cambio** | — | Herramientas del navegador → Red | llamadas a `/v1/variantes` | llamadas a `/v2/articulos` |

### 🔴 PRUEBA PENDIENTE — B.0 Datos de prueba y "antes" (hoy, en QA)

Anota estos datos para usar **los mismos** antes y después:

| Dato | Cómo elegirlo | Anota |
|---|---|---|
| **Artículo X** | Uno con stock, foto y al menos una reseña. Código de barras y nombre | |
| **Modelo M** | Uno de **🔍 Modelos** con stock libre para crear 1 artículo | |
| **Pedido P** | Un pedido Ir pagando sin cobrar completo (para ➕ Agregar artículo) | |
| **Término** | Una palabra de 3+ letras que dé varios resultados (ej. "blusa") | |

Hoy, en QA, haz B.1 a B.9 **una vez** y anota en cada uno lo que sale en **Antes (hoy)**: cuántos
resultados, el primero de la lista, el texto del aviso. Con eso se compara el "después".

### 🔴 PRUEBA PENDIENTE — B.1 Tienda: buscar y filtrar

1. **🛍️ Tienda** → escribe el **Término**.
   - Antes (hoy): ____ resultados, el primero es ____.
   - ✅ Después: **los mismos resultados en el mismo orden**.
2. Abre los filtros y elige una **talla** y un **color**.
   - ✅ Las opciones de talla/color/marca son las mismas que antes, y el resultado igual.
3. Marca **No habilitados** (antes decía "No habilitadas").
   - ✅ Salen los artículos deshabilitados, igual que antes.
- ⛔ Lista vacía, error, o un spinner que no se quita.

### 🔴 PRUEBA PENDIENTE — B.2 Los 6 buscadores de artículos

Escribe el **Término** en cada uno y compara contra el "antes":

| Dónde | Cómo llegar | Antes (hoy) | ✔ |
|---|---|---|---|
| Venta directa | **Ventas → 💰 Venta directa** → buscador de artículos | | [ ] |
| Detalle de pedido | **Mis pedidos** → **Pedido P** → **👁 Detalle** → **➕ Agregar artículo** | | [ ] |
| Créditos / Abonos | **Ventas → 💳 Créditos / Abonos** → buscador del modal de transferir abono | | [ ] |
| Diagnóstico de imágenes | **Sistema → 🔍 Diagnóstico de imágenes** → pestaña del buscador | | [ ] |
| Publicar en redes | La pantalla de **Publicar en Facebook** → buscador de producto | | [ ] |
| Palabras clave | El autocompletar de palabras clave (en Agregar artículo) | | [ ] |

- ✅ Los mismos resultados que antes en cada uno.
- ⛔ "No se encontraron artículos" donde antes sí salían. *(El texto del aviso sí cambió: antes decía
  "No se encontraron variantes con la búsqueda…")*

### 🔴 PRUEBA PENDIENTE — B.3 Ficha del artículo y reseñas

1. **🛍️ Tienda** → abre el **Artículo X**.
   - ✅ Mismas fotos, tallas, precio y la sección de **reseñas** con el mismo número que antes.
   - ✅ El título dice **"Artículo #…"** (antes "Variante #…").
2. Copia la dirección de esa ficha, cierra sesión y pégala en otra pestaña.
   - ✅ Abre la misma ficha sin sesión (usa `/v2/articulos/{id}/producto-id`, que es pública como la vieja).
- ⛔ Ficha en blanco, fotos que no cargan o reseñas en 0.

### 🔴 PRUEBA PENDIENTE — B.4 Agregar y editar un artículo

1. Agrega un artículo al **Modelo M** (Agregar artículo) con una foto.
   - ✅ Aviso **"¡Artículo creado!"** (antes "¡Variante creada!"). Aparece en Tienda con su foto.
2. En Tienda, en ese artículo, **✏️ Editar** → cambia el color → guarda.
   - ✅ Aviso **"¡Artículo actualizado!"**. El cambio se ve en la tarjeta.
3. En la edición, marca otra foto como principal y borra una.
   - ✅ Igual que antes.
- ⛔ 403 "No tiene permisos" con un usuario que antes sí podía.

### 🔴 PRUEBA PENDIENTE — B.5 Botones de la tarjeta en Tienda

1. En el artículo de B.4: **🔒 Deshab.** → **🔓 Habilitar**.
   - ✅ Avisos **"Artículo deshabilitado correctamente"** / **"Artículo habilitado correctamente"**.
2. Marca 2 tarjetas → barra de abajo: **"2 seleccionado(s)"** → **🔒 Deshabilitar seleccionados** →
   **🔓 Habilitar seleccionados**.
   - ✅ Funciona igual que antes (antes decía "seleccionadas").
3. **Dar de baja** el artículo de B.4.
   - ✅ Igual que antes: deja de salir en la tienda.

### 🔴 PRUEBA PENDIENTE — B.6 Botones de Modelos

1. **Catálogo → 🔍 Modelos** → en el **Modelo M** toca **🧩** (al pasar el mouse dice *"Crear artículos
   a partir de este modelo"*).
   - ✅ Ventana **"Crear artículos"** con *"Cantidad de artículos"* y *"Misma imagen para todos los
     artículos"*; botón **Crear artículos**. Al crear: **"1 artículo(s) creado(s)"**.
2. En **🔍 Modelos**, toca **📥 Excel sin artículos** (antes "Excel sin productos").
   - ✅ Baja `productos_sin_articulos.xlsx` (antes `productos_sin_variantes.xlsx`) con la hoja
     **"Productos Sin Artículos"** y **las mismas filas** que el de antes.
3. Abre el detalle de un modelo → **Compartir imagenes**.
   - ✅ Pregunta *"¿Deseas compartir las imágenes de este producto con sus artículos?"* y funciona igual.

### 🔴 PRUEBA PENDIENTE — B.7 Convertir en modelo propio

1. Ficha de un artículo de prueba → **Convertir en modelo propio**.
   - ✅ Aviso **"✅ Artículo independizado"** con el botón **🔍 Ver artículos**. El modelo nuevo existe.

### 🔴 PRUEBA PENDIENTE — B.8 Rifas

1. **Rifas → 🎡 Rifa de productos** → crea una rifa → en el buscador de premios escribe el **Término**.
   - ✅ Mismos resultados que antes (usa `/v2/articulos/buscar-filtrado`).
2. Agrega 2 artículos como premio.
   - ✅ Dice **"2 artículo(s) agregado(s)"** (antes "variante(s) agregada(s)").
3. Edita la palabra clave de un premio y quita el otro.
   - ✅ Al quitar: **"Artículo eliminado y stock restaurado"**. El stock del artículo regresa.
4. Agrega 2 participantes y sortea el primer premio. Toca **▶ Continuar con el siguiente artículo**.
   - ✅ Pasa al segundo premio igual que antes; el resumen dice **"Artículo 1 — …"**, **"Artículo 2 — …"**.
5. **Rifas → 🎟️ Boletos de rifa** → abre las fotos de un premio.
   - ✅ Cargan las fotos.
- ⛔ 403 en cualquier paso con un usuario que antes sí podía.

### 🔴 PRUEBA PENDIENTE — B.9 Diagnóstico de imágenes y chatbot

1. **Sistema → 🔍 Diagnóstico de imágenes** → busca el **Artículo X** → diagnóstico.
   - ✅ Los mismos números que antes (imágenes en BD, consistente sí/no).
2. En la tienda, abre el chat y pide ver un producto que tenga foto.
   - ✅ La foto sale igual que antes.
   - ✅ El chat en vivo sigue ofreciendo pasar con una persona si lo pides ("quiero hablar con alguien").
     *(Esto se revisa porque el chat en vivo depende del encabezado "CATÁLOGO ACTUAL…" del bot, que
     cambió de "variantes" a "artículos" en los dos lugares a la vez; hay prueba automática.)*

### 🔴 PRUEBA PENDIENTE — B.10 Seguridad: los mismos permisos

1. **Sin sesión:** abre **🛍️ Tienda** y la ficha del **Artículo X**.
   - ✅ Se ven igual que antes (las rutas públicas siguen públicas).
2. **Con un rol sin permiso de Tienda/Modelos** (si tienes uno de prueba): intenta **🔍 Modelos**.
   - ✅ Lo mismo que antes (no entra / no ve los botones).
3. **Admin:** B.4 a B.8 sin ningún 403.

La comparación completa la hace la prueba automática (392 combinaciones). Esto es la revisión en pantalla.

### 🔴 PRUEBA PENDIENTE — B.11 Textos que cambiaron

Solo cambia la palabra; lo que hace cada pantalla es igual. Marca los que veas al hacer B.1–B.9:

| Dónde | Antes | Después | ✔ |
|---|---|---|---|
| Ficha del artículo | Variante #12 | Artículo #12 | [ ] |
| Agregar artículo | ¡Variante creada! / ¡3 variantes creadas! | ¡Artículo creado! / ¡3 artículos creados! | [ ] |
| Editar artículo | ¡Variante actualizada! | ¡Artículo actualizado! | [ ] |
| Tienda, filtro | No habilitadas | No habilitados | [ ] |
| Tienda, selección | seleccionada(s) · Habilitar/Deshabilitar seleccionadas | seleccionado(s) · Habilitar/Deshabilitar seleccionados | [ ] |
| Tienda, botones | Variante habilitada/deshabilitada correctamente | Artículo habilitado/deshabilitado correctamente | [ ] |
| Modelos 🧩 | Inicializar variantes · Cantidad de variantes · Crear variantes | Crear artículos · Cantidad de artículos · Crear artículos | [ ] |
| Modelos, Excel | 📥 Excel sin productos · productos_sin_variantes.xlsx | 📥 Excel sin artículos · productos_sin_articulos.xlsx | [ ] |
| Carrito | agrega variantes desde buscar · ¿Limpiar carrito de variantes? · Confirmar pedido de variantes | …artículos… | [ ] |
| Rifas | variante(s) agregada(s) · Continuar con la siguiente variante · Variante 1 — … | artículo(s) agregado(s) · Continuar con el siguiente artículo · Artículo 1 — … | [ ] |
| Boletos de rifa | Solo se pueden rifar variantes con stock, habilitadas y con foto | …artículos con stock, habilitados… | [ ] |
| Promociones | Agrega al menos una variante al combo · Esta variante ya está en el combo | …un artículo… · Este artículo ya está… | [ ] |
| Errores del back | No existe la variante… · Variante no encontrada · La variante no tiene stock… | No existe el artículo… · Artículo no encontrado · El artículo no tiene stock… | [ ] |

- ⛔ Frases con el género mal ("la artículo", "una artículo", "seleccionadas" para artículos).

### 🔴 PRUEBA PENDIENTE — B.12 El cambio mismo (al final)

1. En la computadora, abre **Herramientas del navegador** (`F12`) → pestaña **Red** (Network).
2. Haz B.1 y B.3 otra vez.
   - ✅ Filtrando por `articulos`, las llamadas van a **`/v2/articulos/...`** y responden **200**.
   - ✅ Filtrando por `v1/variantes` no sale **ninguna** llamada.
   - ✅ En rifas, filtrando por `configurarRifa`, van a **`/v2/configurarRifaArticulo/...`**.
- ⛔ Alguna llamada en rojo (401/403/404/500).

**Y las viejas siguen vivas:** el front de producción usa `/v1/variantes`. Eso lo comprueba
`RenombreArticuloRutasTest` (cada ruta vieja y su nueva llegan al mismo método, y ninguna ruta de
`/v1/variantes` se queda sin su par en `/v2/articulos`).

- [ ] B.0 Datos y "antes" anotados
- [ ] B.1 Tienda igual
- [ ] B.2 Los 6 buscadores iguales
- [ ] B.3 Ficha y reseñas iguales, link directo sin sesión
- [ ] B.4 Alta y edición
- [ ] B.5 Botones de Tienda
- [ ] B.6 Botones de Modelos y Excel
- [ ] B.7 Convertir en modelo propio
- [ ] B.8 Rifas
- [ ] B.9 Diagnóstico y chatbot
- [ ] B.10 Permisos iguales
- [ ] B.11 Textos
- [ ] B.12 Llamadas a las rutas nuevas

---

## Pruebas automáticas de la rama

| Prueba | Qué revisa | Resultado |
|---|---|---|
| `RenombreArticuloSecurityTest` (back) | 28 rutas viejas contra sus 28 nuevas × 14 usuarios: misma respuesta de seguridad | ✅ 392/392 (antes del cambio: 183 distintas) |
| `RenombreArticuloRutasTest` (back) | Cada ruta nueva llega al mismo método que la vieja; ninguna ruta de `/v1/variantes` sin su par | ✅ 3/3 |
| `ChatbotPromptFotosTest` (back, ya existía) | El chat en vivo sigue encontrando el encabezado del catálogo para meter "hablar con una persona" | ✅ (se actualizó el texto esperado) |
| `mvn test` completo (back) | Todo lo demás | ✅ 799 pruebas, 0 fallas |
| `ng build` (front) | Que compile | ✅ sin errores |
| `e2e/support/articulo.ts` (front) | La prueba E2E de alta de artículo esperaba "¡Variante creada!" | Se actualizó a "¡Artículo creado!" |

## Hallazgos de paso (no se tocaron)

- El front tiene un método `getPaginado()` que llama a `/v1/variantes/paginado`, ruta que **no existe**
  en el back. Ninguna pantalla lo usa, así que no falla nada; se puede borrar en otro cambio.
- Los nombres de menús y acciones que salen en **Gestión de roles** (por ejemplo "Crear variantes")
  vienen de la base (`submenu`, `accion_submenu`). Cambiarlos es una migración de datos: queda para
  otro paso, si lo quieres.
