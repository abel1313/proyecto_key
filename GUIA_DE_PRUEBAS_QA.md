# Guía de pruebas en QA — paso a paso

Esta guía reemplaza, para probar, a `PRUEBAS_QA_2026-10-01.md` (ese se queda como historial).
Aquí está **todo lo que falta probar**, en el orden en que conviene hacerlo, y cada prueba dice:

- **Para qué es** — en una línea.
- **Antes de empezar** — qué pedidos o datos crear y qué anotar.
- **Pasos** — una tabla: a la izquierda lo que **haces**, a la derecha lo que **debes ver**.
- **❌ Está mal si…** — lo que sería un error.

### ¿Qué ya está en QA y qué falta subir? (2026-10-06)

| Parte | Estado |
|---|---|
| Pruebas **1, 2, 4** y de la **3** los casos **3.1 a 3.7** | ✅ Ya están en QA |
| **3.6**, **3.7** y la **Prueba 5** (filtros de pedidos) | ✅ **Validadas por ti el 2026-10-06** |
| **3.8** (⇄ en un artículo con 2 o más piezas pregunta cuántas cambiar) | ⏳ Hecha y probada en mi lado, **falta que digas "sube"** |
| **Prueba 6** (datos de prueba) | ✅ En QA. Va **al final** |

**Cómo anotar:** si algo no sale como dice la columna "Debes ver", escribe debajo de esa tabla
`💬` y lo que pasó (qué hiciste, qué esperabas, qué salió, número de pedido). Cuando termines una
prueba, marca su casilla `[x]`. Yo leo tus 💬 y contesto debajo con `↳`.

---

## Antes de todo (una sola vez, 5 minutos)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre QA: `qa.shop.novedades-jade.com.mx` | La tienda |
| 2 | Entra con tu usuario **administrador** | Tu nombre abajo en el menú lateral |
| 3 | Presiona `Ctrl + Shift + R` (recarga forzada) | La página se recarga. Hazlo **cada vez** que yo suba algo |
| 4 | En **Tienda** busca y anota **4 artículos con stock de 10 o más**, de **modelos distintos** (no la misma prenda en otra talla). Ver la tabla de abajo | — |

**Tus artículos de prueba** (los vas a usar en varias pruebas):

| Nombre en esta guía | Precio que sugiero | Nombre real del artículo | Su precio real |
|---|---|---|---|
| **Art-A** | ~$100 | | |
| **Art-B** | ~$100 | | |
| **Art-200** | ~$200 | | |
| **Art-300** | ~$300 | | |

Si tus precios no son esos, no pasa nada: en cada paso viene la cuenta para que la hagas con los tuyos.

### Receta: cómo crear un pedido (la vas a usar muchas veces)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Menú **Ventas → 💰 Venta directa** | La pantalla de venta |
| 2 | En **🔍 Buscar producto** escribe 3 letras del artículo y elígelo | El artículo en la lista de la venta |
| 3 | Elige la forma de cobro que pida la prueba: **📦 Apartado** o **💳 Ir pagando** | El botón queda marcado |
| 4 | En **💰 Pago inicial (enganche)** escribe lo que pida la prueba (o déjalo vacío si dice "nada") | — |
| 5 | Toca **💰 Cobrar** | Aviso **"✅ Apartado registrado"** o **"✅ Ir pagando registrado"** con *"Pedido #**N** creado"* |
| 6 | **Anota el número N** y toca **Cerrar** | — |

El cliente es opcional: si no eliges uno, el pedido queda a tu nombre.

**Para abrir un pedido:** menú **Pedidos → Mis pedidos** → escribe su número en el buscador →
en su tarjeta toca **👁 Detalle**.

---

## Orden de las pruebas

| Orden | Prueba | Qué revisa | Tiempo aprox. | ¿En QA? | ✔ |
|---|---|---|---|---|---|
| 1 | **Botón Volver** | Que el botón ← Volver quede alineado con la tarjeta | 10 min | ✅ | [ ] |
| 2 | **Apartado es sin dinero** | Que un Apartado no acepte abonos a medias y se pueda pasar a Ir pagando | 25 min | ✅ | [ ] |
| 3 | **Detalle del pedido: quitar, cambiar o agregar artículos** | Que el total, lo que debe y el estado cambien bien; que el buscador solo ofrezca lo que se puede vender | 35 min | ✅ 3.1–3.7 · ⏳ 3.8 | [ ] |
| 4 | **Cancelar pedidos con abonos** | Qué pasa con el stock y qué mensaje sale al cancelar | 20 min | ✅ | [ ] |
| 5 | **Filtros de pedidos** | Buscar por nombre y todos los filtros nuevos de Mis pedidos | 30 min | ✅ | [x] |
| 6 | **Datos de prueba con un botón** | Crear 20 mil modelos y mil pedidos de prueba | 15 min | ✅ | [ ] |

**La 5 va después de la 2, 3 y 4 a propósito:** usa los pedidos que creaste en esas (A–F, H4–H7,
K1–K6), que ya sabemos en qué estado quedaron. **La 6 va al final:** mete miles de pedidos de prueba
y con eso es más difícil encontrar los tuyos.

---

## Prueba 1 — Botón Volver alineado con la tarjeta

**Para qué es:** el botón **← Volver** (o como se llame en cada pantalla) ahora queda en el mismo
borde izquierdo que la tarjeta de abajo, no pegado a la esquina. Lo que hace no cambió.

**Antes de empezar:** nada.

**En cada pantalla de la lista haz lo mismo:**

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra a la pantalla | El botón de regresar arriba de la tarjeta |
| 2 | Compara el borde izquierdo del botón con el borde izquierdo de la tarjeta | **Están en la misma línea vertical** |
| 3 | Toca el botón | Regresas a la pantalla de antes |
| 4 | Haz la ventana angosta (o ábrela en el celular) | Sigue alineado |

❌ **Está mal si:** el botón queda pegado a la orilla de la pantalla, o más a la izquierda o derecha que la tarjeta.

| Pantalla | Cómo llegar | ✔ |
|---|---|---|
| Agregar Modelo | **Catálogo → ➕ Agregar modelo** | [ ] |
| Actualizar Modelo | **Catálogo → 🔍 Modelos** → en un modelo **✏️ Actualizar** | [ ] |
| Nuevo Producto | **Catálogo → 🧩 Agregar producto** | [ ] |
| Actualizar artículo | **Tienda** → en un artículo **✏️ Editar** | [ ] |
| Cargar catálogo Excel | **Catálogo → 📂 Cargar Excel** | [ ] |
| Carrito | Agrega algo al carrito → botón **Carrito** del menú | [ ] |
| Entregas por zona | **Envíos → 📦 Entregas por zona** | [ ] |
| Ver un cliente | **Clientes** → en un cliente **👁️ Ver/Editar** | [ ] |
| Nuevo cliente | Pega en el navegador `qa.shop.novedades-jade.com.mx/clientes/agregar` | [ ] |
| Mis datos | Abajo en el menú, debajo de tu nombre: **Mis datos** | [ ] |
| Cambiar contraseña | Mismo lugar: **Cambiar contraseña** | [ ] |
| Mi perfil | Mismo lugar: **Mi perfil** | [ ] |
| Agregar mi compra | Mismo lugar: **Agregar mi compra** | [ ] |

💬 Notas:

- [ ] **Prueba 1 terminada**

---

## Prueba 2 — Apartado es sin dinero

**Para qué es:** un **Apartado** es el pedido que llega por Facebook o un live y **no ha dado
dinero**: se paga **completo** al recogerlo. Si el cliente deja un adelanto, el pedido se cambia a
**Ir pagando**. Esta prueba revisa que el sistema lo respete en todos lados.

**Antes de empezar:** con la **Receta**, crea 6 pedidos con **Art-A** y anota sus números:

| Pedido | Forma de cobro | 💰 Pago inicial | Número |
|---|---|---|---|
| **A** | 📦 Apartado | nada | |
| **B** | 📦 Apartado | nada | |
| **C** | 📦 Apartado | nada | |
| **D** | 📦 Apartado | nada | |
| **E** | 💳 Ir pagando | **$50** | |
| **F** | 💳 Ir pagando | nada | |

### 2.1 Un Apartado no acepta un adelanto → se cambia a Ir pagando (pedido A)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre el **👁 Detalle** de **A** y toca **💳 Registrar abono** | El campo **Monto** ya trae el total del pedido, y la nota *"Es un Apartado: se paga completo ($…)"* |
| 2 | Cambia el monto a **50** y toca **💾 Guardar abono** | Aviso **"Un Apartado se paga completo"**. **No** se registró nada en **📋 Pagos registrados** |
| 3 | En el aviso toca **🔁 Cambiar a Ir pagando** | Se abre **🔁 Cambiar la forma de cobro** con **Ir pagando** marcado y **$50** en *"¿Cobra algo ahora?"* |
| 4 | En *"¿Por qué cambia?"* escribe *"dejó $50 de adelanto"* y toca **Guardar cambio** | El pedido queda **💳 Ir pagando**, pagado **$50**, y debe el resto. En **📋 Pagos registrados** aparece el abono de $50 |

❌ **Está mal si:** se registra el abono de $50 mientras sigue siendo Apartado, o si al cambiar a Ir pagando se pierden los $50.

### 2.2 Un Apartado pagado completo, con cambio (pedido B)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **B** → **💳 Registrar abono** | El monto ya trae el total |
| 2 | Deja el monto, forma de pago **Efectivo**, y en **💵 Monto recibido** escribe **500** | *"Cambio a devolver: $…"* = 500 menos el total |
| 3 | Toca **💾 Guardar abono** | Se registra sin aviso, el pedido queda **Pagado**, ya no sale **Registrar abono** y dice *"Este pedido ya está pagado por completo"* |

❌ **Está mal si:** sale el aviso "Un Apartado se paga completo" pagando el total, o el cambio sale mal.

### 2.3 Lo mismo desde Créditos / Abonos (pedido C)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💳 Créditos / Abonos** → busca **C** → **+ Abono** | El monto ya trae el total y abajo *"Es un Apartado: se paga completo…"* |
| 2 | Cambia el monto a **50** y regístralo | Aviso **"Un Apartado se paga completo"** con el botón **Ir al pedido**. No se registra nada |
| 3 | Toca **Ir al pedido** | Se abre el pedido **C** |

❌ **Está mal si:** se registra el abono de $50.

### 2.4 Cambiar la forma de cobro (pedidos E y F)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **E** (Ir pagando con $50) → **🔁 Cambiar forma de cobro** | El botón **Apartado** está gris. Al pasar el mouse: *"Ya dio dinero: un Apartado es sin dinero, queda como Ir pagando"*. Toca **Cancelar** |
| 2 | Abre **F** (Ir pagando sin dinero) → **🔁 Cambiar forma de cobro** → toca **Apartado** | **No** aparece *"¿Cobra algo ahora?"*; sale la nota *"Un Apartado es sin dinero: el cliente lo paga completo cuando lo recoge…"* |
| 3 | Toca **Guardar cambio** | **F** queda **📦 Apartado** |

❌ **Está mal si:** en **E** se puede elegir Apartado.

### 2.5 Apartados unidos (pedidos C y D)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **C** → **🔗 Unir con otros pedidos** → busca **D** → elígelo → **Unir 2 pedidos** | El bloque **"🔗 Unido en el grupo #…"** con el botón **💵 Pagar el grupo completo** |
| 2 | Toca **💵 Pagar el grupo completo** | El monto ya trae lo que deben **C + D** y el texto *"Son Apartados: se pagan completos…"* |
| 3 | Cambia el monto a **20** → **Registrar abono** | Aviso **"Los Apartados se pagan completos"**. No se registra nada |
| 4 | Regresa el monto al total → **Registrar abono** | **C** y **D** quedan **Pagado** y el grupo ya no debe nada |

❌ **Está mal si:** el grupo acepta un pago menor al total.

### 2.6 En la venta: Apartado con dinero se vuelve Ir pagando

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💰 Venta directa** → agrega **Art-A** → toca **📦 Apartado** | — |
| 2 | En **💰 Pago inicial (enganche)** escribe **100** | El botón marcado cambia solo a **💳 Ir pagando** y sale *"Un Apartado es sin dinero: como te dio $100.00, queda como Ir pagando."* |
| 3 | Toca **💰 Cobrar** | **"✅ Ir pagando registrado"** con *"Enganche de $100.00 registrado"* |
| 4 | Haz otra venta: **Art-A** → **📦 Apartado** → sin enganche → **💰 Cobrar** | **"✅ Apartado registrado"** |

❌ **Está mal si:** se guarda un Apartado con enganche.

💬 Notas:

- [ ] 2.1 · [ ] 2.2 · [ ] 2.3 · [ ] 2.4 · [ ] 2.5 · [ ] 2.6 — **Prueba 2 terminada**

---

## Prueba 3 — Detalle del pedido: quitar, cambiar o agregar artículos

**Para qué es:** cuando le quitas, cambias o agregas un artículo a un pedido de **Ir pagando**, el
sistema vuelve a sumar el total y lo compara con lo que el cliente ya pagó:
- Si lo pagado **alcanza** → queda **Pagado**.
- Si **no alcanza** → queda (o regresa a) **Ir pagando**, debiendo la diferencia.

Es la que falló la vez pasada (al **agregar** un artículo el total no subía); ya está corregido.

**Antes de empezar:** con la **Receta**, crea 4 pedidos de **Ir pagando** (no uses los de pruebas anteriores):

| Pedido | Artículos | Total | 💰 Pago inicial | Debe | Número |
|---|---|---|---|---|---|
| **H4** | Art-A + Art-B | $200 | **$150** | $50 | |
| **H5** | Art-A + Art-B | $200 | **$150** | $50 | |
| **H6** | Art-A + Art-B | $200 | **$150** | $50 | |
| **H7** | Art-A | $100 | nada | $100 | |

En el **👁 Detalle**, cada tarjeta de artículo tiene **−** (quita una pieza) y **⇄** (cambiarlo por
otro). **➕ Agregar artículo** está arriba, junto a **🔁 Cambiar forma de cobro**.

### 3.1 Quitar un artículo y que lo pagado alcance (H4)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **H4** | **Ir pagando**, *"Debe: $50"* (Total $200 · pagado $150) |
| 2 | Toca **−** en **Art-B** | Se recarga solo: **Total $100** y **Pagado**. Cuenta: quedó $100 y pagó $150 → ya no debe |
| 3 | Mira **📋 Pagos registrados** | Sigue en **$150** |
| 4 | En **Tienda** mira el stock de Art-B | Subió 1 |

❌ **Está mal si:** sigue diciendo "Debe", o tienes que salir y volver a entrar para verlo Pagado.

### 3.2 Pedido Pagado: cambiar por uno más caro (H4)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En **H4** (Pagado, solo Art-A) toca **⇄** en Art-A, escribe 3 letras de **Art-200** y elígelo | *"Artículo cambiado"*. **Total $200**, **Ir pagando**, *"Debe: $50"*. Cuenta: $200 − $150 = $50 |
| 2 | Mira abajo | Vuelve a salir **💳 Registrar abono** y ya **no** dice *"Este pedido ya está pagado por completo"* |

❌ **Está mal si:** sigue Pagado o Total $100.

### 3.3 Pedido Pagado: AGREGAR un artículo (H5) — lo que fallaba

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **H5** y toca **−** en **Art-B** | Total $100, **Pagado** |
| 2 | Toca **➕ Agregar artículo**, busca **Art-300** y elígelo | *"Artículo agregado"*. Arriba **Total $400**, **Ir pagando**, *"Debe: $250"*. Cuenta: $100 + $300 = $400; $400 − $150 = $250 |
| 3 | Mira abajo | **📋 Pagos registrados** sigue en $150 y vuelve a salir **💳 Registrar abono** |
| 4 | Toca **💳 Registrar abono**, escribe **250** y guárdalo | Queda **Pagado**; los pagos suman **$400** |

❌ **Está mal si:** arriba dice Total $100 o sigue Pagado después del paso 2.

### 3.4 Quitar y después cambiar (H6)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **H6** y toca **−** en **Art-B** | Total $100, **Pagado** |
| 2 | Toca **⇄** en Art-A y cámbialo por **Art-300** | **Total $300**, **Ir pagando**, *"Debe: $150"*. Cuenta: $300 − $150 |

### 3.5 No se puede quitar el único artículo (H7)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **H7** y toca **−** en Art-A | **No lo deja.** Aviso: *"… es el ultimo articulo del pedido #… Para regresar todo, cancela el pedido"* |

❌ **Está mal si:** queda un pedido sin artículos.

### 3.6 El buscador de ⇄ y ➕ solo ofrece lo que se puede vender — ✅ validada 2026-10-06

**Qué se arregló:** antes, al tocar **⇄** o **➕ Agregar artículo**, salían también artículos **sin
stock** y **dados de baja** (es el buscador de Tienda, donde sí se administran). Si elegías uno,
el sistema lo rechazaba al guardar. Ahora el pedido tiene su propio buscador.

**Antes de empezar:** en **Tienda** elige un modelo con **3 tallas** (o crea uno con **Catálogo → 🧩
Agregar producto**) y déjalo así: una talla con stock **5**, otra con stock **0**, y la tercera **dada
de baja** (en su tarjeta, el botón para darla de baja). Anota el nombre del modelo: ______.

| # | Haz esto | Antes del arreglo | Debes ver ahora |
|---|---|---|---|
| 1 | Abre **H6** (o cualquier pedido abierto) → **⇄** en un artículo → escribe 3 letras del modelo | Salían las **3** tallas | Sale **solo** la talla con stock 5 |
| 2 | Cierra y toca **➕ Agregar artículo** → mismas 3 letras | Igual, las 3 | **Solo** la de stock 5 |
| 3 | Escribe algo que no exista, ej. **zzzz** | *"No se encontró ningún artículo con eso."* | *"No hay ningún artículo con existencias que coincida. Los que no tienen stock o están dados de baja no salen aquí."* |
| 4 | Escribe solo **2 letras** | No buscaba | **No busca** (igual que antes) |
| 5 | **Tienda** → busca el mismo modelo | Salen las 3 tallas | **Lo mismo que antes:** salen las 3 (Tienda no cambió) |
| 6 | Escribe **blusa** letra por letra, normal | Buscaba con cada letra desde la 3.ª (la lista brincaba) | Busca **una sola vez**, medio segundo después de que dejas de escribir |
| 7 | Borra todo el texto | — | La lista se limpia de inmediato |

❌ **Está mal si:** en el pedido sale la talla sin stock o la dada de baja, en Tienda dejan de salir, o la lista cambia con cada letra.

### 3.7 Pedidos unidos: cambiar un artículo de $500 por uno de $100 (tu caso del Saldo) — ✅ validada 2026-10-06

**Por qué está aquí:** me dijiste que al unir 2 pedidos, dar un pago de $100 y cambiar un artículo de
$500 por uno de $100, el **Saldo** no se recalculó. En un pedido **solo** lo probé y sí se recalcula;
no he podido repetir tu caso con pedidos unidos. Esta prueba lo repite paso a paso para ver **en
qué paso** se queda el número viejo.

**Antes de empezar:** necesitas un artículo de ~$500 (**Art-500**: ______) y Art-A (~$100).
Con la **Receta** crea:

| Pedido | Artículos | Forma de cobro | 💰 Pago inicial | Número |
|---|---|---|---|---|
| **U1** | Art-500 | 💳 Ir pagando | nada | |
| **U2** | Art-A | 💳 Ir pagando | nada | |

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **U1** → **🔗 Unir con otros pedidos** → busca **U2** → **Unir 2 pedidos** | Grupo con **Total $600** |
| 2 | En el bloque del grupo toca **💵 Abonar al grupo** → **100** → registrar | Grupo: **Pagado $100 · Saldo $500**. Cuenta: 600 − 100 |
| 3 | En **U1**, en la tarjeta de **Art-500** toca **⇄** → elige **Art-A** | *"Artículo cambiado"*. Grupo: **Total $200 · Pagado $100 · Saldo $100**. Cuenta: 100 + 100 = 200; 200 − 100 = 100 |
| 4 | Sal a **Mis pedidos** y vuelve a abrir **U1** | Los mismos números del paso 3 |
| 5 | **Ventas → 💳 Créditos / Abonos** → busca **U1** y **U2** | Lo que deben suma **$100** |

**Si falla, anota con 💬:** en qué paso, qué número viste (Total, Pagado, Saldo) y **dónde** lo viste
(el bloque gris del grupo que dice "Saldo", el encabezado que dice "Debe", o Créditos / Abonos).
Con eso lo ubico.

💬 *"Buscador en pedidos detalle ya lo veo bien, agregar artículo a un pedido listo… prueba 3.7 ya se recalcula, confirmo"* (2026-10-06)
↳ Anotadas 3.6 y 3.7 como OK. Lo que encontraste del ⇄ con varias piezas quedó como la **3.8**.

💬 *"Si en un pedido hay un artículo con 2 o 3 stock y le doy cambiar, lo que hace es quitar los 2 stock y agregar el nuevo; hace falta que pregunte cuántos quiere quitar, si es uno dejar el resto"* (2026-10-06)
↳ Correcto, era un error: el sistema regresaba **todas** las piezas y dejaba **1** del artículo nuevo, así que las demás desaparecían del pedido. Arreglado (back y front, en `dev`): ahora pregunta cuántas cambias y el resto se queda. Prueba **3.8**.

### 3.8 ⇄ en un artículo con varias piezas: pregunta cuántas cambiar — ⏳ después de "sube"

**Qué se arregló:** antes, con 3 piezas de un artículo, **⇄** regresaba las 3 al stock y dejaba 1 del
artículo nuevo (las otras 2 se perdían del pedido y el total bajaba). Ahora, si la línea tiene **2 o
más** piezas, pregunta *"¿Cuántas piezas cambias?"*; las que elijas se cambian **1 a 1** por el
artículo nuevo y las demás se quedan como estaban. Con **1** pieza cambia directo, como siempre.

**Mapa de impacto — qué más usa este botón:**

| # | Le pega a | Antes | Después |
|---|---|---|---|
| 3.8.1 | **⇄** en un artículo con **1** pieza (3.2, 3.4, 3.7) | Cambia directo | **Lo mismo que antes** (no pregunta) |
| 3.8.2 | **⇄** en un artículo con **3** piezas | Quitaba las 3, quedaba 1 nueva | Pregunta; cambiar 1 deja 2 viejas + 1 nueva |
| 3.8.3 | **⇄** en un artículo de **otro pedido del grupo** (bloque de abajo) | Igual que 3.8.2 | Igual que 3.8.2, en ese pedido |
| 3.8.4 | **⇄** en un artículo de una **promoción** con 2 piezas | Quitaba las 2, quedaba 1 | **No pregunta**: cambia la línea completa (2 por 2), porque partir el combo no se puede |
| 3.8.5 | **➕ Agregar artículo** y **−** | — | **Lo mismo que antes** (no se tocaron) |

**Antes de empezar:** con la **Receta** crea un pedido **Ir pagando** **H8** con **3 piezas de Art-A**
($100 c/u → **Total $300**) y pago inicial **$100** → debe $200. Anota el stock de **Art-A** y de **Art-B** en Tienda: Art-A ____ · Art-B ____.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **H8** → **⇄** en Art-A → busca **Art-B** ($100) y elígelo | Pregunta **"¿Cuántas piezas cambias?"**, dice que hay **3** y arranca en **1** |
| 2 | Escribe **4** y toca **Cambiar** | No deja: *"Solo hay 3 pieza(s) en el pedido"* |
| 3 | Escribe **1** y toca **Cambiar** | *"Artículo cambiado"* · *"Se cambiaron 1 pieza(s); quedan 2 del artículo de antes."* El pedido queda con **Art-A × 2** y **Art-B × 1**. **Total $300**, debe **$200** (no cambió: mismo precio) |
| 4 | En **Tienda** mira el stock | Art-A **subió 1** (no 3) · Art-B **bajó 1** |
| 5 | **⇄** en Art-A (ahora 2 piezas) → **Art-B** → escribe **2** → **Cambiar** | Queda **una sola línea Art-B × 3** (se suma a la que ya había). Total $300 |
| 6 | **⇄** en Art-B (3 piezas) → **Art-300** → **Cancelar** en la pregunta | No cambia nada |
| 7 | **⇄** en Art-B → **Art-300** → **1** → **Cambiar** | Art-B × 2 + Art-300 × 1 → **Total $500**, debe **$400**. Cuenta: 200 + 300 = 500; 500 − 100 = 400 |
| 8 | Abre un pedido con **1** pieza de un artículo (ej. **H6**) → **⇄** | **No pregunta**, cambia directo como antes |

❌ **Está mal si:** al cambiar 1 de 3 desaparecen las otras 2, el stock del artículo viejo sube 3, se deja escribir más piezas de las que hay, o con 1 pieza sale la pregunta.

💬 Notas:

- [ ] 3.1 · [ ] 3.2 · [ ] 3.3 · [ ] 3.4 · [ ] 3.5 · [x] 3.6 · [x] 3.7 · [ ] 3.8 — **Prueba 3 terminada**

---

## Prueba 4 — Cancelar pedidos con abonos

**Para qué es:** al cancelar, el stock regresa **o no** según si el cliente ya se llevó la mercancía:

| Forma de cobro | ¿Se llevó la mercancía? | Al cancelar, el stock… |
|---|---|---|
| **Ir pagando** que todavía debe | Sí | **no** regresa (queda como deuda que no se cobró) |
| **Apartado** | No | **sí** regresa |
| Pedido ya **Pagado** | Sí | **sí** regresa: es una devolución. **Solo un administrador** |

**Antes de empezar:**
1. Anota el **stock de Art-A** en Tienda: ______. Vas a revisarlo después de cada cancelación.
2. Con la **Receta**, crea estos pedidos con **Art-A**:

| Pedido | Forma de cobro | 💰 Pago inicial | Debe | Número |
|---|---|---|---|---|
| **K1** | 💳 Ir pagando | **$40** | $60 | |
| **K2** | 💳 Ir pagando | **$40** | $60 | |
| **K3** | 📦 Apartado | nada | $100 | |
| **K4** | 💳 Ir pagando | nada | $100 | |
| **K5** | 💳 Ir pagando | nada | $100 | |
| **K6** | 💳 Ir pagando | **$40** | $60 | |

3. **Une K4 y K5:** abre **K4** → **🔗 Unir con otros pedidos** → busca **K5** → **Unir 2 pedidos**.
4. **Termina de pagar K6:** **Ventas → 💳 Créditos / Abonos** → pestaña **📋 Cuentas por cobrar** → tarjeta de **K6** → **+ Abono** → **60** → guárdalo. K6 pasa a la pestaña **✅ Liquidados**.

### 4.1 Cancelar un Ir pagando desde Créditos / Abonos (K1)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **💳 Créditos / Abonos** → **📋 Cuentas por cobrar** → tarjeta **💳 Ir pagando #K1** → **✖ Cancelar** | *"¿Cancelar el crédito (ir pagando) de …?"* y *"El producto ya fue entregado. La deuda de $60.00 quedará registrada."* |
| 2 | Elige el motivo **El cliente avisó** → **Sí, registrar como incobrable** | **Cancelado**: *"FIADO cancelado. Stock NO devuelto (producto entregado). Deuda incobrable: $60.00"* |
| 3 | Mira el stock de Art-A | **No cambió** |

(*FIADO* es el nombre interno de Ir pagando. Si prefieres que diga "Ir pagando", anótalo con 💬.)

### 4.2 Cancelar un Ir pagando desde la tarjeta (K2)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → busca **K2** → en su tarjeta **✖ Cancelar** | *"¿Por qué cancelas este pedido?"* con los 3 motivos |
| 2 | **El cliente avisó** → **Cancelar pedido** | *"Pedido cancelado correctamente"* y la tarjeta desaparece |
| 3 | Mira el stock de Art-A | **No cambió** |

(Aquí todavía **no** dice que quedó a deber $60. Ya está decidido agregarlo; no es error de esta prueba.)

### 4.3 Cancelar un Apartado (K3)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **💳 Créditos / Abonos** → **📋 Cuentas por cobrar** → tarjeta **📦 Apartado #K3** → **✖ Cancelar** | *"¿Cancelar el apartado de …?"* y *"Pagó $0.00 de $100.00. Se devolverá el stock."* |
| 2 | **No se presentó** → **Sí, cancelar y devolver stock** | *"APARTADO cancelado. Stock devuelto…"* |
| 3 | Mira el stock de Art-A | **Subió 1** |

### 4.4 Cancelar un pedido unido que no es el titular (K5)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **K4** | El grupo suma **$200** |
| 2 | **Mis pedidos** → escribe el número **exacto** de **K5** → **✖ Cancelar** → **El cliente avisó** → **Cancelar pedido** | *"Pedido cancelado correctamente"* |
| 3 | Vuelve a abrir **K4** | K4 sigue **Ir pagando**; el grupo ya solo suma **$100** |

❌ **Está mal si:** también se cancela K4, o el grupo sigue sumando $200.

### 4.5 Alguien que no es administrador intenta cancelar un Pagado (K6)

Si no tienes un usuario que no sea administrador, sáltala y anótalo.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra con el usuario **que no es administrador** → **Mis pedidos** → busca **K6** | O no ve **✖ Cancelar**, o al tocarlo sale *"Este pedido no lo puedes cancelar tú"*. Las dos están bien |
| 2 | Vuelve a entrar como **administrador** | — |

❌ **Está mal si:** K6 queda cancelado.

### 4.6 El administrador cancela un Pagado: devolución (K6)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **💳 Créditos / Abonos** → pestaña **✅ Liquidados** → tarjeta de **K6** → **✖ Cancelar** | *"¿Cancelar (devolución) el pedido de …?"* y *"Ya se pagó por completo ($100.00). Se devolverá el stock."*. Entre los motivos **no** sale "No se presentó" |
| 2 | **El cliente avisó** → **Sí, cancelar y devolver stock** | *"Pedido pagado cancelado (devolución). Stock devuelto. Monto a reembolsar: $100.00"* |
| 3 | Mira el stock de Art-A | **Subió 1** |

💬 Notas:

- [ ] 4.1 · [ ] 4.2 · [ ] 4.3 · [ ] 4.4 · [ ] 4.5 · [ ] 4.6 — **Prueba 4 terminada**

---

## Prueba 5 — Filtros de pedidos — ✅ validada 2026-10-06

**Para qué es:** en **Pedidos → Mis pedidos** el buscador ahora encuentra por **nombre** (del
cliente, de quien recibe), **teléfono**, **correo** o **artículo**, además del número. Y hay un panel
con **todos los filtros**: forma de cobro, estado, dinero, fecha de entrega, dónde se entrega,
unidos, ramos, promociones, fechas de registro, total, y el **orden** de la lista. Lo que dejas
puesto se guarda para la próxima vez que entres (en cualquier computadora o celular).

**Cómo es la pantalla nueva:**
- El buscador dice *"Número, nombre, teléfono, correo o artículo…"*.
- A su lado, el botón **⚙️ Filtros** (con cuántos tienes puestos, ej. *"3 activos"*) abre y cierra el panel.
- **Ordenar**: *Más recientes primero*, *Más antiguos primero*, *Entrega más próxima*, *Los que más deben*.
- Abajo del panel: **✕ Quitar filtros**. Arriba de las tarjetas: *"Buscando: … · N pedidos"*.
- Dentro de un mismo bloque las opciones **se suman** (Apartado **o** Ir pagando). Entre bloques **se
  combinan** (Ir pagando **y** Debe dinero).
- **"🛒 Normal" ahora dice "🛒 Contado"** (es el mismo filtro).

### Mapa de impacto — qué toca este cambio

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 5.1 | La lista de Mis pedidos ahora pide a `GET /v1/pedidos/buscar` en vez de `/v1/pedidos/buscarClientePedido` | Buscar un pedido por su número (lo usan las pruebas 2, 3 y 4) | Sale ese pedido | **Lo mismo** |
| 5.2 | (mismo) | **Ver el pedido** desde Créditos / Abonos | Abre su detalle | **Lo mismo**, aunque tengas filtros puestos |
| 5.3 | (mismo) | Abrir otro pedido del grupo desde el detalle | Lo abre | **Lo mismo**, aunque tengas filtros puestos |
| 5.4 | (mismo) | Regresar del detalle a la lista | La tarjeta se actualiza y te quedas en la misma página | **Lo mismo** |
| 5.5 | (mismo) | Un cliente (no administrador) en Mis pedidos | Ve sus pedidos y busca por número | **Lo mismo** (no ve filtros) |
| 5.6 | Filtros guardados ahora también para Mis pedidos | Los filtros guardados de **Tienda** y **Modelos** | Se guardan | **Lo mismo** |
| 5.7–5.12 | **El cambio** | — | Solo número, lugar, Normal/Apartado/Ir pagando, Pagados/Cancelados | Todo lo de abajo |

**Antes de empezar:**
1. `Ctrl + Shift + R`.
2. Ten a la mano los números de los pedidos de las pruebas 2, 3 y 4. Si las hiciste **hoy**, en todos
   los pasos de 5.7 a 5.11 pon primero **Registrado → Desde: hoy, Hasta: hoy**: así solo salen los
   pedidos de hoy y es fácil revisar. Si las hiciste en otros días, pon el rango de esos días.

Cómo quedaron (con Art-A $100, Art-B $100, Art-200 $200, Art-300 $300):

| Pedido | Forma | Cómo quedó | Total | Pagado |
|---|---|---|---|---|
| A, E | Ir pagando | Por cobrar, debe $50 | $100 | $50 |
| B | Apartado | Pagado | $100 | $100 |
| C + D | Apartado, **unidos** | Pagado (en la lista solo sale **C**, el titular) | $100 c/u | $100 c/u |
| F | Apartado | Por cobrar, **sin abonos** | $100 | $0 |
| H4 | Ir pagando | Por cobrar, debe $50 | $200 | $150 |
| H5 | Ir pagando | Pagado | $400 | $400 |
| H6 | Ir pagando | Por cobrar, debe $150 | $300 | $150 |
| H7 | Ir pagando | Por cobrar, **sin abonos** | $100 | $0 |
| K1, K2 | Ir pagando | Cancelado (debía: deuda incobrable) | $100 | $40 |
| K3 | Apartado | Cancelado | $100 | $0 |
| K4 + K5 | Ir pagando, **unidos** | K4 Por cobrar sin abonos; K5 Cancelado | $100 c/u | $0 |
| K6 | Ir pagando | Cancelado como **devolución** (hay que regresarle $100) | $100 | $100 |

### 5.1 a 5.6 — Lo que no debe cambiar

| # | Haz esto | Debes ver (igual que antes) |
|---|---|---|
| 5.1 | **Mis pedidos** → escribe el número de **H5** | Sale **solo** H5 |
| 5.2 | Marca el filtro **❌ Cancelado** → **💳 Créditos / Abonos** → en **A** toca **Ver el pedido** | Se abre el detalle de **A** aunque A no está cancelado (abrir por número ignora los filtros). Después toca **✕ Quitar filtros** |
| 5.3 | Abre **C** → en la sección de pedidos unidos abre **D** | Se abre **D** |
| 5.4 | Ve a la página 2 de la lista → abre un pedido → regresa | Sigues en la página 2 |
| 5.5 | Entra con un usuario **cliente** → **Mis pedidos** | Sus pedidos, buscador *"Buscar por número de pedido…"*, **sin** botón ⚙️ Filtros. (Si no tienes usuario cliente, sáltala y anótalo) |
| 5.6 | **Tienda** → pon un filtro → recarga la página | El filtro sigue puesto, como antes |

❌ **Está mal si:** algo de esta tabla cambió.

### 5.7 Buscar por nombre, teléfono, correo y artículo

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Escribe **2 letras** de un nombre | **No busca**. Abajo dice *"Escribe al menos 3 letras para buscar por nombre, teléfono, correo o artículo."* |
| 2 | Escribe la 3.ª letra | Busca una sola vez, medio segundo después de dejar de escribir |
| 3 | Escribe el nombre **sin acentos** de un cliente que lo tenga (ej. *maria* para *María*) | Salen sus pedidos |
| 4 | Busca el **nombre de quien recibe** de un pedido (el que capturaste en **Entrega**) | Sale ese pedido. **Antes no salía** |
| 5 | Busca los últimos **5 dígitos de un teléfono** de cliente | Salen sus pedidos |
| 6 | Busca **3 letras de Art-300** | Salen **H5** y **H6** (los que tienen Art-300) |
| 7 | Busca el **código de barras** de Art-A | Salen los pedidos con Art-A |
| 8 | Busca **#** + el número de **H7** (ej. *#245*) | Sale **H7** |
| 9 | Borra todo el texto | Regresa la lista completa |

### 5.8 Forma de cobro y estado (usa Registrado = hoy)

Pueden salir además otros pedidos que hayas hecho hoy (por ejemplo los de 2.6). Lo importante es que
estén los de *Deben salir* y **no** esté ninguno de *No deben salir*.


| # | Marca | Deben salir | No deben salir |
|---|---|---|---|
| 1 | **💳 Ir pagando** | A, E, H4, H5, H6, H7, K1, K2, K4, K6 | B, C, F, K3 |
| 2 | Además **🕒 Por cobrar** | A, E, H4, H6, H7, K4 | H5 (pagado), K1, K2, K6 (cancelados) |
| 3 | Quita Ir pagando y Por cobrar; marca **✅ Pagado** | B, C, H5 | A, F |
| 4 | Agrega **❌ Cancelado** (Pagado **o** Cancelado) | B, C, H5, K1, K2, K3, K6 (K5 no: es parte del grupo de K4 y solo sale buscando su número) | A, F, H7 |
| 5 | Quita todo; marca **⏳ Pendiente** | Solo ventas de contado sin cobrar (de las pruebas, ninguna) | Ninguno de A–K |

### 5.9 Dinero (usa Registrado = hoy)

| # | Marca | Deben salir | No deben salir |
|---|---|---|---|
| 1 | **💰 Debe dinero** | A, E, H4, H6, H7, F, K4 | B, H5, K1, K6 |
| 2 | Cambia a **🚫 Sin abonos** | F, H7, K4 | A, E, H4 (tienen abonos) |
| 3 | Cambia a **↩️ Saldo a favor** | **K6** (devolución de $100) | **K1, K2** (Ir pagando que debían: es deuda incobrable, no se les devuelve), K3 (no pagó nada) |

### 5.10 Entrega, lugar, unidos y otros

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | A **F** ponle fecha de entrega **ayer** (en su tarjeta, **Entrega**) y a **H7** **hoy** | — |
| 2 | Marca **⚠ Atrasados** | Sale **F** con *"⚠ Atrasado 1 día"* en la tarjeta. No sale H7 |
| 3 | Cambia a **📅 Hoy** | Sale **H7**. No sale F |
| 4 | Cambia a **Esta semana** | Sale **H7** (hoy entra en la semana). No sale F (ya pasó) |
| 5 | Toca otra vez **Esta semana** | Se quita (estos filtros son de una sola opción) |
| 6 | En **Lugar de entrega** elige uno que tenga algún pedido | Solo los de ese lugar. La ✕ lo quita |
| 7 | **🚚 Envío** | Solo los que van a un lugar de entrega que **no** es la tienda |
| 8 | **🔗 Solo unidos** | **C** y **K4** (los titulares; D y K5 no salen) |
| 9 | **Sin unir** | Todos menos C, D, K4, K5 |
| 10 | **💐 Ramos de flores** (sin Registrado = hoy) | Solo pedidos de ramo |
| 11 | **🏷️ Con promoción** (sin Registrado = hoy) | Solo pedidos que llevan un combo de promoción |

### 5.11 Total, fechas y orden

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Total del pedido** Desde **250** | **H5** ($400) y **H6** ($300) |
| 2 | Hasta **350** (Desde 250, Hasta 350) | Solo **H6** |
| 3 | Pon Desde **400** y Hasta **100** | Aviso *"Revisa el total"*. No busca |
| 4 | **Registrado** Desde mañana y Hasta hoy | Aviso *"Revisa las fechas"* |
| 5 | Quita los totales. **Ordenar → Los que más deben** | Arriba **H6** (debe $150), luego los de $100 (F, H7, K4), luego los de $50 |
| 6 | **Ordenar → Más antiguos primero** | El primero que creaste hoy hasta arriba |
| 7 | **Ordenar → Entrega más próxima** | **F** (ayer) primero, después **H7** y las ventas de contado de hoy (la venta de contado guarda hoy como fecha), y al final los que no tienen fecha |
| 8 | Quita el filtro Registrado y deja **Más recientes primero**. Pasa a la página 2, 3… | **Ningún pedido se repite** entre páginas. Antes, con varios pedidos el mismo día, uno se podía repetir y otro no salir |

### 5.12 Esconder, guardar, celular y noche

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Pon 3 filtros y cierra el panel con **⚙️ Filtros** | El botón dice *"3 activos"* y arriba de las tarjetas *"Buscando: … · N pedidos"* |
| 2 | Recarga la página (`F5`) | Los 3 filtros y el orden siguen puestos. **El texto del buscador no** (eso no se guarda) |
| 3 | Entra desde **otro navegador o el celular** con tu usuario | Los mismos filtros |
| 4 | **✕ Quitar filtros** | Se quitan todos y la lista sale completa. Al recargar ya no regresan |
| 5 | En el celular abre el panel | Los bloques uno debajo del otro, sin salirse de la pantalla |
| 6 | Cambia a modo noche (☀️/🌙) | Todo se lee: títulos, opciones marcadas, cajas de fecha y total |
| 7 | Con un usuario que **no tenga** el permiso *filtro-pagados* (Gestión de roles) | No ve **✅ Pagado**; sí ve las opciones nuevas. (Si no tienes ese usuario, sáltala) |

❌ **Está mal si:** un filtro trae pedidos que no cumplen, el contador no coincide, se pierde lo
guardado al recargar, o algo no se lee de noche.

💬 Notas:

💬 *"Ya validé los filtros en pedidos y me gustan, ya los validé todos para ponerlos en ok"* (2026-10-06)
↳ Anotada toda la Prueba 5 como OK.

- [x] 5.1–5.6 · [x] 5.7 · [x] 5.8 · [x] 5.9 · [x] 5.10 · [x] 5.11 · [x] 5.12 — **Prueba 5 terminada**

---

## Prueba 6 — Datos de prueba con un botón (la última)

**Para qué es:** crear de un jalón miles de modelos, artículos y pedidos de prueba para no darlos de
alta a mano. Todo queda marcado como **"Prueba QA"** para distinguirlo de lo real.

**Antes de empezar:** termina las pruebas 1 a 5. Mientras esté generando, **no me pidas subir nada a
QA** (subir reinicia el servidor y corta la corrida).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → 🗑️ Limpiar caché** | Arriba la tarjeta de siempre (**Gestión de Caché**) y abajo una nueva: **🧪 Datos de prueba** |
| 2 | Toca **🗑️ Limpiar toda la caché → Sí, limpiar** | *"¡N cachés limpiadas!"*, igual que siempre |
| 3 | En **🧪 Datos de prueba** deja los valores (Modelos **20000**, Artículos **1** a **4**, Pedidos **1000**) y toca **🧪 Generar datos de prueba → Sí, generar** | El botón dice **Generando…** y aparece una barra: *"Creando modelos y artículos · N%"* |
| 4 | Sal de la pantalla y vuelve a entrar | La barra sigue avanzando |
| 5 | Espera a que diga **Terminado · 100%** (unos minutos) | *Modelos: 20000 de 20000* y *Pedidos: 1000 de 1000*. Primero suben los modelos y **después** los pedidos |
| 6 | Toca otra vez **🗑️ Limpiar toda la caché → Sí, limpiar** | Para que la tienda muestre lo nuevo de inmediato |
| 7 | **Tienda** → busca **QA-1** | Artículos de prueba con foto (la foto es de otro producto real: es normal) |
| 8 | **Catálogo → 🔍 Modelos** → busca **QA-** | Modelos con marca **Prueba QA**, de 1 a 4 artículos cada uno |
| 9 | **Mis pedidos** → busca **Cliente Prueba QA** (con la Prueba 5 ya puedes también filtrar por estado) | Pedidos **Entregado**, **Apartado**, **Ir pagando** y **Pagado** |
| 10 | **💳 Créditos / Abonos** → **📋 Cuentas por cobrar** | Apartados e Ir pagando de "Cliente Prueba QA" |
| 11 | **Reportes** | Ventas de contado y de los pedidos que se terminaron de pagar |
| 12 | Regresa a **🗑️ Limpiar caché** → **Dar de baja los datos de prueba → Sí, dar de baja** | *"N artículos de prueba dados de baja (y sus modelos)"* |
| 13 | **Tienda** → busca **QA-1** otra vez (limpia la caché si todavía salen) | Ya no salen. Tus productos reales siguen igual. Los pedidos de prueba siguen en Mis pedidos como historial |

❌ **Está mal si:** la barra dice *"Se detuvo: …"* o *"con error: N"*. Copia el **Último error** en tus notas.

💬 Notas:

- [ ] **Prueba 6 terminada**

---

## Lo que todavía no se prueba

- **Diseño Jade y el cambio de "variante" a "artículo"**: están en una rama aparte y no han subido a QA.
  Su guía es `PRUEBAS_RAMA_TEMA_JADE_ARTICULO.md`; te aviso cuando se suba.
- **"Falta entregarlo" en la venta** y **"Saldo a favor"**: todavía no están programados.
