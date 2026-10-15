# Plan — Dar de alta un modelo y sus artículos en un solo paso

Pedido del dueño (2026-10-06). **Flujo A programado el 2026-10-08** (ver sección 5); flujo B (B1) ya
estaba desde el 2026-10-06. Antes de programar se acordaron las reglas
(regla de `CLAUDE.md`: *primero las reglas, después el código*). Las marcadas ❓ las tiene que decidir
el dueño. Se programa en `dev` y sube a `qa` como siempre.

---

## 1. Cómo es hoy y por qué cansa

Un **modelo** (producto base, ej. "Blusa Zara", stock 10) no se vende solo: se venden sus
**artículos** (cada talla/color con su stock). Hoy, para tener algo vendible, hay que ir a dos
pantallas distintas:

| Camino | Pasos | Problema |
|---|---|---|
| A | **Catálogo → Agregar modelo** → guardar → **Catálogo → 🔍 Modelos** → buscar el modelo → **🧩 Productos** → "Inicializar variantes" | Hay que salir, buscar el modelo y volver a entrar. Los artículos nacen todos iguales, con stock 1 y sin talla |
| B | **Catálogo → Agregar modelo** → guardar → **Catálogo → 🧩 Agregar producto** → buscar el modelo → llenar el artículo | Hay que volver a buscar el modelo. Contenido neto y categoría no se precargan (ver H7 de `RENOMBRE_VARIANTE_A_ARTICULO.md`) |

Y al revés: si en **Agregar producto** ya se llenó medio artículo y resulta que al modelo no le
queda stock libre, hoy hay que dejar todo, ir a subirle stock al modelo y volver a empezar.

**Nombres que se cambian con el renombrado** (anotados en `RENOMBRE_VARIANTE_A_ARTICULO.md`, H6):
- Menú **🧩 Agregar producto** → **🧩 Agregar artículo** (da de alta un artículo, no un producto).
- Botón de la tarjeta del modelo **🧩 Productos** → **🧩 Artículos**; su ventana "Inicializar
  variantes" / "Crear variantes" → **"Crear artículos"**.

---

## 2. Flujo nuevo A — al guardar un modelo, ofrecer sus artículos

### Cómo se vería

1. **Agregar modelo** → llenas todo (nombre, precio, stock 10, color, marca, descripción, contenido
   neto, categoría, imagen) → **Guardar**.
2. Sale: **"Modelo guardado. ¿Quieres agregar sus artículos ahora?"** → **Sí, agregar artículos** /
   **Después**.
3. Con **Sí** se abre el paso de artículos, en la misma pantalla:
   - **"Usar del modelo en todos los artículos"**: una casilla por cada dato que el modelo **sí
     tiene** (color, marca, descripción, contenido neto, categoría, imagen). Todas marcadas.
     Si el modelo no tiene imagen, la casilla de imagen **no aparece**.
   - **"¿Cuántos artículos?"** → escribes 5 → aparecen **5 formularios**, iguales a los de
     **Agregar artículo** (talla, color, presentación, stock, imagen…).
   - Lo marcado ya viene lleno en los 5 (y se puede cambiar en uno solo). Lo desmarcado viene vacío
     para llenarlo en cada uno. El **stock** de cada uno viene en blanco.
   - Abajo: **"Stock del modelo: 10 · Repartido: 8 · Te quedan 2"**.
4. **Guardar artículos** → se guardan los 5 juntos (si uno falla, no se guarda ninguno).

### Reglas propuestas

| # | Regla | Estado |
|---|---|---|
| A1 | La pregunta sale **solo al dar de alta** un modelo, no al actualizarlo | propuesta |
| A2 | **Después** deja el modelo guardado tal cual; los artículos se agregan luego por los caminos de hoy | propuesta |
| A3 | Solo aparecen casillas de lo que el modelo tiene lleno; todas empiezan marcadas | lo pidió el dueño |
| A4 | **Nunca** se copian del modelo: talla, presentación, stock (son de cada artículo) | igual que hoy (R2 del dominio `articulo`) |
| A5 | Cuántos artículos: mínimo 1, máximo el stock libre del modelo (un artículo necesita al menos 1 pieza) | propuesta |
| A6 | Cada artículo con stock **≥ 1**; la suma no puede pasar del stock del modelo. Se avisa **mientras escribes** ("Te pasaste por 2"), no hasta guardar | mismas validaciones de hoy |
| A7 | Se puede dejar stock sin repartir (ej. 10 del modelo y solo 8 en artículos) | igual que hoy |
| A8 | La imagen del modelo se **reusa** (no se sube otra vez): el artículo apunta a la misma foto | propuesta, necesita un cambio chico en el back |
| A9 | Se guardan todos o ninguno | propuesta (el back ya guarda la lista en una sola transacción) |
| A10 | Atajo **"Mismo stock para todos"**: escribes 2 y los 5 quedan en 2 (y se puede cambiar en cada uno) | ✅ decidido 2026-10-08: sí |
| A11 | Tallas: cada formulario es **un** artículo (una talla) | ✅ decidido 2026-10-08 |
| A12 | Si sale de la pantalla sin guardar los artículos, el modelo **ya quedó guardado** (se guardó en el paso 1) | propuesta |

---

## 3. Flujo nuevo B — en Agregar artículo, cuando al modelo no le alcanza el stock

Ejemplo: el modelo tiene 10 y ya están repartidos los 10. Estás dando de alta un artículo más con
stock 3, ya llenaste talla, color e imagen, y el sistema dice que no hay stock libre.

Dos formas de resolverlo:

| Opción | Cómo funciona | A favor | En contra |
|---|---|---|---|
| **B1 — Subir el stock del modelo ahí mismo** (recomendada) | Sale: *"Al modelo le faltan 3 piezas. ¿Subir su stock de 10 a 13 y guardar el artículo?"* → **Sí** → sube el stock del modelo y guarda el artículo de una vez | No sales de la pantalla ni pierdes lo que llenaste. No hay estados nuevos | Necesita el permiso de **actualizar modelo**; quien no lo tenga ve el aviso sin el botón |
| **B2 — Guardar avance** | Guarda el artículo como **borrador** (sin stock, sin venderse) y vas a subir el stock al modelo; luego regresas y lo completas | Lo que pediste literalmente | Hay que crear un estado nuevo "borrador" y esconderlo de la tienda, ventas, pedidos, rifas y chatbot, más una lista de "artículos pendientes" para encontrarlos |

| # | Regla (si se elige B1) | Estado |
|---|---|---|
| B1.1 | El aviso dice exactamente cuántas piezas faltan y a cuánto quedaría el modelo | propuesta |
| B1.2 | Subir el stock del modelo y guardar el artículo es **una sola operación**: si algo falla, no se sube nada | propuesta |
| B1.3 | Solo con permiso de actualizar modelo (Gestión de roles); sin permiso, el aviso dice *"Pídele a alguien con permiso que le suba stock al modelo"* | propuesta |

✅ **Decidido por el dueño (2026-10-06): B1, así:** en **Agregar artículo**, al elegir el modelo se ven
dos datos del modelo:
- **Stock total del modelo** — bloqueado, solo se lee (ej. 10). Abajo: *"Repartido: 10 · Libre: 0"*.
- **Agregar o quitar stock al modelo** — un campo con + / − (ej. +3). Al guardar, el modelo queda en 13
  y el artículo se guarda con su stock, **en una sola operación** (B1.2). Quitar no puede dejar al modelo
  con menos de lo que ya está repartido.

---

## 4. Lo que hay que decidir antes de programar

1. A10 — ¿atajo "Mismo stock para todos"?
2. A11 — ¿un formulario = un artículo, o cada formulario con su sección de varias tallas?
3. Flujo B — ¿B1, B2 o las dos?
4. ¿El flujo A también desde la tarjeta del modelo (**🧩 Artículos**), reemplazando la ventana de
   "Inicializar variantes"? Así los dos caminos serían la misma pantalla.

✅ **Decidido por el dueño (2026-10-08):** A10 sí (atajo "Mismo stock para todos"), A11 un formulario = un
artículo, y el flujo A **también** desde la tarjeta del modelo (🧩 Productos), reemplazando la ventana de hoy.

---

## 5. Cómo quedó programado el flujo A (2026-10-08)

| Regla | Cómo quedó |
|---|---|
| A1 | La pregunta sale al **dar de alta** en Catálogo → Agregar modelo y, desde el 2026-10-14, también al **actualizar** (Catálogo → 🔍 Modelos → ✏️ Actualizar) si el modelo queda con stock libre. Sin stock libre, solo el aviso *"¡Modelo actualizado!"*. Ver sección 7 |
| A2 / A12 | **Ahora no** (antes "Después") cierra; el modelo ya estaba guardado. Al actualizar, regresa a Modelos |
| A3 | ~~Casillas arriba para todos los artículos~~ (2026-10-08). **Desde el 2026-10-14: checks por artículo** — ver sección 7 |
| A4 | Talla, presentación y stock nunca vienen llenos |
| A5 | ¿Cuántos? de 1 al stock libre. Desde el 2026-10-14 es **la pregunta misma** (*"¿Cuántos artículos quieres hacer de este modelo?"*), y se puede cambiar otra vez en los formularios |
| A6 | Stock ≥ 1 por artículo; abajo *"Repartido · Te quedan / Te pasaste por N"* mientras se escribe; el back vuelve a validar |
| A7 | Se puede guardar dejando stock sin repartir |
| A8 | Campo nuevo `usarImagenDelModelo` en `guardarConImagenes`: el artículo apunta a la foto principal del modelo, sin subirla. Cada formulario puede subir **su propia** foto (campo nuevo `imagenesPropias`), y entonces usa esa en lugar de la del modelo |
| A9 | Un solo `POST /v1/variantes/guardarConImagenes` (una transacción): todos o ninguno |
| A10 | **Mismo stock para todos** + **Aplicar** |
| A11 | Un formulario = un artículo |
| 🧩 de la tarjeta | La misma ventana, sin la pregunta. Reemplaza "Inicializar variantes" (el endpoint `inicializarDesdeProducto` sigue, pero el front ya no lo usa) |

Si se deja un dato vacío, el artículo toma el del modelo (regla R3 del dominio `articulo`, la de siempre). Desde
el 2026-10-14 la ventana se apoya justo en eso: lo que no se marca en un artículo viaja vacío.

Código: front `shared/alta-articulos/` (componente `<app-alta-articulos>`), back
`VarianteServiceImpl.repartirImagenes()` + `guardarConImagenes()`. Pruebas: `PRUEBAS_PENDIENTES_2026-10-07.md`,
Prueba 17 (17.7–17.9); las automáticas que faltan, en `TESTS_PENDIENTES.md` (2026-10-08).


---

## 6. El camino inverso: desde Agregar artículo (2026-10-08)

Pedido del dueño: *"me voy a agregar artículo, busco el producto base y si no hay stock tiene que
aparecer; si agrego 5 se tendría que ir al modelo y ya que se agregue sale el mensaje de ¿deseas agregar
las variantes de una vez?"* y *"si está deshabilitado hay que habilitarlo con el mensaje, ya después
validar el stock y que aparezca cuántas variantes puede hacer"*.

| Regla | Cómo quedó |
|---|---|
| Ver todos los modelos | Permiso nuevo **"Ver todos los modelos"** en 🧩 Agregar producto (`tienda/venta`); el admin siempre. Etiquetas ⛔ Deshabilitado / Sin stock / Sin foto |
| Deshabilitado | Aviso + **✅ Habilitar modelo** (permiso Habilitar de Modelos). Solo el modelo; sus artículos quedan como estaban. No deja guardar artículos hasta habilitarlo. Después dice su stock y cuántos artículos caben |
| Agregar stock | **Guardar en el modelo**: se guarda al momento en el modelo (queda libre) y pregunta *"¿Deseas agregar los artículos de una vez?"* → ventana 🧩 Agregar artículos (sección 5) |
| Con stock libre | "Puedes hacer hasta N artículos más" y botón **🧩 Agregar varios artículos de una vez** |

Back: `PUT /v1/stock/producto/{id}/ajuste` (dominio `stock`, reglas R-A1..R-A5 en su README).
Pruebas: `PRUEBAS_PENDIENTES_2026-10-07.md`, Prueba 18.

---

## 7. Checks por artículo, pregunta al actualizar y stock del modelo desde el artículo (2026-10-14)

Pedido del dueño en QA (Prueba 20.3): *"si estoy en artículo y ya no tiene stock… debería abrir un modal…
ahí puedo poner 10 o 100, pero eso no quiere decir que el artículo… se le vayan a poner esos 10 o 100"*;
*"para el producto base… al dar guardar me debe salir cuántos artículos quiero hacer"*; y *"el primer
artículo con los check de que quiero actualizar, ejemplo la descripción o la imagen… la variante se va a
hacer igual que lo que tiene el producto, todos sus campos, y solo diferente los check que cambie"*.

| Regla | Cómo quedó |
|---|---|
| C1 — igual al modelo | Cada artículo nace **igual al modelo**. En su formulario hay 6 checks, **todos sin marcar**: Foto, Descripción, Color, Marca, Contenido neto, Categoría. Lo no marcado viaja **vacío** y el back pone el del modelo (R2/R3 del dominio `articulo`); la foto no marcada es la del modelo (`usarImagenDelModelo`, A8) |
| C2 — marcar = cambiar | Al marcar, aparece el campo **ya escrito con el dato del modelo** para cambiar solo lo necesario. Abajo de cada artículo: *"Igual al modelo: …"* con lo que no se marcó |
| C3 — marcado y vacío | No deja guardar: *"Escribe el color o quita la marca de Color"* (igual con los demás; la foto: *"Sube la foto o quita la marca de Foto"*) |
| C4 — siempre por artículo | Talla, presentación y stock (como A4) |
| C6 — plegar | Cada artículo se pliega y despliega con su propia línea (**▸ Mostrar / ▾ Ocultar**); plegado enseña *"Talla S · Stock 1 · Cambia: Descripción"* y *"⚠️ Falta: stock"* si le falta algo. Hasta 3 salen abiertos; con más, los nuevos salen plegados (el primero siempre abierto). **Abrir todos / Cerrar todos** y, abajo, *"A los artículos 2, 5 y 7 les falta algo — Abrir solo esos"*. Pedido del dueño: *"¿si pongo 20 o 30 artículos se hará una listota hacia abajo?"* |
| C5 — modelo sin foto | Si el modelo no tiene foto y no se marca Foto: *"este artículo no sale en la tienda hasta que tenga una"* (se puede guardar igual) |
| P1 — pregunta al actualizar | Al **💾 Actualizar modelo**, si `GET /v1/stock/producto/{id}` dice que caben artículos, sale la misma pregunta (*"✅ Modelo actualizado"*). Sin stock libre o si esa consulta falla: el aviso de siempre y regresa a Modelos |
| S1 — stock del modelo desde el artículo | En **Tienda → Buscar → ✏️ Editar**, si lo que se le pide al artículo pasa de lo libre del modelo, **💾 Actualizar artículo no manda nada**: abre **➕ Agregar stock al modelo**. Lo escrito ahí se suma **solo al modelo** (`PUT /v1/stock/producto/{id}/ajuste`) y el artículo no se toca; al cerrar, el artículo se guarda con su propio botón |
| S2 — botón directo | El recuadro de stock del artículo trae **➕ Agregar stock al modelo** cuando no le queda libre o cuando se está pasando |
| S3 — si el back rechaza | Si el back responde 422 (lo libre cambió mientras tanto), se recarga lo libre y se abre la misma ventana en vez del error |
| S4 — sin permiso | Sin permiso de escritura en Modelos / Agregar modelo / Agregar producto (el mismo que pide el back): *"Pídele a alguien con permiso de Catálogo que le sume stock al modelo…"* |

No cambió nada en el back: se usan `guardarConImagenes` y `PUT /v1/stock/producto/{id}/ajuste` tal como estaban.
Código: front `shared/alta-articulos/`, `variante/update-variante/`, `productos/producto/add/`.
Pruebas: `PRUEBAS_PENDIENTES_2026-10-13.md`, 20.3 (y 17.7 / 17.8 reescritas).

