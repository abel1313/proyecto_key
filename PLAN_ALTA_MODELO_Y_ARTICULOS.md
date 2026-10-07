# Plan — Dar de alta un modelo y sus artículos en un solo paso

Pedido del dueño (2026-10-06). **Todavía no se programa nada:** primero se acuerdan las reglas
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
| A10 | Atajo **"Mismo stock para todos"**: escribes 2 y los 5 quedan en 2 | ❓ ¿lo quieres? |
| A11 | Tallas: ¿cada formulario es **un** artículo (una talla), o cada formulario trae también la sección de **varias tallas** de Agregar artículo (que crea un artículo por talla)? | ❓ |
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
