# Guía de pruebas en QA — paso a paso

Esta guía reemplaza, para probar, a `PRUEBAS_QA_2026-10-01.md` (ese se queda como historial).
Aquí está **todo lo que falta probar**, en el orden en que conviene hacerlo, y cada prueba dice:

- **Para qué es** — en una línea.
- **Antes de empezar** — qué pedidos o datos crear y qué anotar.
- **Pasos** — una tabla: a la izquierda lo que **haces**, a la derecha lo que **debes ver**.
- **❌ Está mal si…** — lo que sería un error.

### ¿Qué ya está en QA y qué falta subir? (2026-10-07)

| Parte | Estado |
|---|---|
| Pruebas **1, 2, 4** y de la **3** los casos **3.1 a 3.7** | ✅ Ya están en QA. **La 1 (Volver) la validaste el 2026-10-06**: falta que todos digan lo mismo ("Volver" o "Regresar"), va con la homologación de pantallas |
| **3.6**, **3.7** y la **Prueba 5** (filtros de pedidos) | ✅ **Validadas por ti el 2026-10-06** |
| **3.8** (⇄ en un artículo con 2 o más piezas pregunta cuántas cambiar) | ✅ En QA desde el 2026-10-06 (el **paso 5** necesita también la Prueba 7) |
| **Prueba 7** (fallas que encontré al revisar filtros y detalle, 2026-10-06) | ✅ En QA (la 7.10, celular, sube junto con la 8) |
| **Prueba 8** (Liquidar / Dar abono / Abonar al grupo desde la card) | ✅ **Validada por ti el 2026-10-06** ("ya se puede cobrar desde la card"). Tus dudas de los filtros se contestan en la Prueba 10 |
| **Prueba 9** (Gestión de roles al día: permisos de los filtros nuevos y del cobro desde la card) | ✅ **Validada por ti el 2026-10-06** ("ya veo todos los roles") |
| **Prueba 10** (arreglos del 2026-10-06: Crear artículos en prod, Mis datos, filtro Pendiente) | ⏳ 10.1 **ya está en prod**; 10.2 y 10.3 **todavía no se suben** (esperan tu "sube") |
| **Prueba 11** (Entregado aparte del pago, filtros Pago / Entrega, stock del modelo en Agregar artículo, pantallas homologadas, Jade) | ⏳ **Todavía no se sube** (espera tu "sube"; antes hay que correr 3 scripts en QA, ver 11.0) |
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
| 1 | **Botón Volver** | Que el botón ← Volver quede alineado con la tarjeta | 10 min | ✅ | [x] |
| 2 | **Apartado es sin dinero** | Que un Apartado no acepte abonos a medias y se pueda pasar a Ir pagando | 25 min | ✅ | [ ] |
| 3 | **Detalle del pedido: quitar, cambiar o agregar artículos** | Que el total, lo que debe y el estado cambien bien; que el buscador solo ofrezca lo que se puede vender | 35 min | ✅ | [ ] |
| 4 | **Cancelar pedidos con abonos** | Qué pasa con el stock y qué mensaje sale al cancelar | 20 min | ✅ | [ ] |
| 5 | **Filtros de pedidos** | Buscar por nombre y todos los filtros nuevos de Mis pedidos | 30 min | ✅ | [x] |
| 7 | **Revisión de filtros y detalle** | Pedidos unidos en los filtros, la card de un grupo, el botón − con dos tallas, ⇄ que suma en la misma línea | 40 min | ⏳ en QA | [ ] |
| 8 | **Cobrar a crédito desde la card** | Liquidar / Dar abono / Abonar al grupo sin ir a Créditos / Abonos | 40 min | ✅ | [x] |
| 9 | **Gestión de roles al día** | Que los permisos de Mis pedidos digan lo que hay hoy y que los filtros nuevos se puedan quitar por rol | 20 min | ✅ | [x] |
| 10 | **Arreglos del 2026-10-06** | Crear artículos en prod, Mis datos sin spinner, filtro Pendiente vs Por cobrar | 20 min | ⏳ 10.1 en prod; 10.2–10.3 sin subir | [ ] |
| 11 | **Entregado y pantallas homologadas** | Etiquetas Pagado / Entregado, 📦 Entregar, "¿Ya se lo llevó?", filtros Pago y Entrega, stock del modelo en Agregar artículo, anchos, tablas, selects, Volver, Gastos | 60 min | ⏳ sin subir | [ ] |
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

### 3.8 ⇄ en un artículo con varias piezas: pregunta cuántas cambiar — ✅ en QA

⚠️ **El paso 5 todavía falla en QA** (salen dos líneas "Art-B" en vez de una "Art-B × 3"): al
revisar encontré que cambiar **todas** las piezas no se sumaba a la línea que ya había. Ya está
corregido en mi lado; pruébalo después de "sube" junto con la **Prueba 7.4**.

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

## Prueba 7 — Revisión de filtros y detalle del pedido (2026-10-06) — ⏳ en QA, por probar

**Para qué es:** al revisar todo lo de filtros y detalle encontré estas fallas y ya las corregí.
Cada una tiene su prueba: **antes** (lo que pasa hoy en QA) y **después** (lo que debe pasar).

### Mapa de impacto

| # | Qué se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 7.1 | Filtros de **Mis pedidos** con pedidos **unidos** (`GET /v1/pedidos/buscar`) | Estado, Dinero, Total, Fecha de entrega, "Los que más deben" | Miraban solo al titular: un grupo que todavía debe salía como **Pagado** y no salía en **Por cobrar** | Miran al grupo, igual que la card |
| 7.2 | (mismo) | Buscar por **nombre / teléfono / artículo** del otro cliente del grupo | No encontraba nada | Sale la card del grupo |
| 7.3 | Card del titular de un grupo | Etiqueta de estado y "Entrega" | Decía **Pagado** con "Falta $100" debajo; no mostraba la fecha de entrega | Dice **Por cobrar** y muestra la fecha |
| 7.4 | **⇄** cambiando **todas** las piezas por un artículo que ya está | Detalle del pedido | Quedaban **dos líneas** del mismo artículo | Una sola línea con la suma |
| 7.5 | Botón **−** (`DELETE /v1/pedidos/{id}/detalle/{productoId}`, ahora con `detalleId`) | Pedido con **dos tallas del mismo modelo** | Podía quitar la **otra** talla | Quita la que tocaste |
| 7.6 | Orden **"Entrega más próxima"** | Mis pedidos → Ordenar | Salían primero entregados de hace meses | Primero lo que falta entregar, atrasados arriba |
| 7.7 | Aviso **📍 Entrega** de la card | Nombre o notas con comillas | Se cortaba en la comilla | Sale completo |
| 7.8 | Buscador de Mis pedidos | Flechas o Tab dentro del buscador | Regresaba a la página 1 | Te quedas donde estabas |
| 7.9 | **Lo mismo que antes** (no debe cambiar) | Pedidos **sin unir** en todos los filtros, buscar por número, ⇄ con 1 pieza, ➕ Agregar | — | **Lo mismo que antes** |

**Antes de empezar:** `Ctrl + Shift + R`. Con la **Receta** crea:

| Pedido | Artículos | Forma de cobro | 💰 Pago inicial | Cliente | Número |
|---|---|---|---|---|---|
| **GA** (créalo **primero**) | Art-A ($100) | 💳 Ir pagando | nada | uno cualquiera | |
| **GB** (después) | Art-B ($100) | 💳 Ir pagando | nada | **otro** cliente (anota su nombre: ______) | |
| **T2** | **2 tallas del mismo modelo** (ej. Blusa M y Blusa L), 1 de cada una | 💳 Ir pagando | nada | — | |

Luego **une GA con GB**: abre **GA** → **🔗 Unir con otros pedidos** → busca **GB** → **Unir 2 pedidos**
(GA queda como titular: su card dice "Unido con #GB"). En el bloque del grupo toca **💵 Abonar al
grupo** → **100** → registrar. El dinero va primero al más viejo: **GA queda Pagado** y **GB debe $100**.

### 7.1 Filtros con un grupo que todavía debe (GA + GB)

En **Pedidos → Mis pedidos** pon **⚙️ Filtros → Registrado: Desde hoy, Hasta hoy** y luego:

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Estado **🕒 Por cobrar** | GA **no** salía | Sale **GA** (la card del grupo) |
| 2 | Quita ese y marca **✅ Pagado** | Salía GA | GA **no** sale (al grupo le falta $100) |
| 3 | Quita ese y marca Dinero **💰 Debe dinero** | GA no salía | Sale **GA** |
| 4 | Cambia a **🚫 Sin abonos** | — | GA **no** sale (el grupo ya tiene un abono) |
| 5 | Quita Dinero; en **Total del pedido** pon Desde **150** | GA no salía (su pedido es de $100) | Sale **GA** (la card dice $200 entre los dos) |
| 6 | Quita filtros; **Ordenar → Los que más deben** | GA estaba al fondo (su pedido debe $0) | GA aparece según los **$100** que debe el grupo |

### 7.2 Buscar al otro cliente del grupo

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | En el buscador escribe el **nombre del cliente de GB** | No salía nada | Sale la card de **GA** ("Unido con #GB") |
| 2 | Escribe las 3 primeras letras de **Art-B** | Solo salían otros pedidos | También sale **GA** |
| 3 | Escribe el **número exacto de GB** | Salía GB | **Lo mismo:** sale **GB** solo |

### 7.3 La card del grupo

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Mira la card de **GA** | Arriba decía **✔ Pagado** y abajo "Falta $100.00" | Arriba **🕒 Por cobrar**; abajo "Total de los 2 pedidos $200.00 · Falta $100.00" |
| 2 | En la card de GA toca **Entrega** → **📅 Fecha de entrega**: mañana → **💾 Guardar**; recarga | No salía la fecha (GA está pagado) | Sale la fila **"Recoge en el local"** (o **"Entrega"** si tiene lugar) con la fecha de mañana |
| 3 | Filtro **Fecha de entrega → Mañana** | GA no salía | Sale **GA** |

### 7.4 ⇄ todas las piezas por un artículo que ya está en el pedido

Usa el pedido **H8** de la 3.8 (o crea uno Ir pagando con **Art-A × 2** y **Art-B × 1**).

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | **⇄** en Art-A (2 piezas) → **Art-B** → escribe **2** → **Cambiar** | Dos líneas: "Art-B × 2" y "Art-B × 1" | **Una sola línea "Art-B × 3"**. Total igual ($300) |
| 2 | Stock en **Tienda** | — | Art-A **+2** · Art-B **−2** |

❌ **Está mal si:** quedan dos líneas de Art-B o cambia el total.

### 7.5 Botón − con dos tallas del mismo modelo (T2)

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Abre **T2** y toca **−** en la **segunda** talla (la de abajo) | Podía desaparecer la **de arriba** | Desaparece **la que tocaste**; la otra sigue |
| 2 | Stock en **Tienda** | Subía la talla equivocada | Sube **la talla que quitaste** |

### 7.6 Ordenar "Entrega más próxima"

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Quita filtros → **Ordenar → Entrega más próxima** | Primero pedidos **Entregados** de hace meses | Primero los que **faltan por entregar**: los **⚠ Atrasados** arriba, luego hoy, mañana… Los entregados, pagados o sin fecha van al final |

### 7.7 Comillas en Entrega

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | En cualquier pedido **📍 Entrega** → "Nombre de quien recibe": `Ana "La Güera" López` → Guardar | — | — |
| 2 | Vuelve a abrir **📍 Entrega** | El campo decía solo `Ana ` | Dice `Ana "La Güera" López` completo |

### 7.8 Flechas en el buscador

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Sin texto en el buscador, ve a la **página 2** (Siguiente →) | — | Página 2 de N |
| 2 | Da clic en el buscador y presiona **←** o **→** | Regresaba a la página 1 | Te quedas en la **página 2** |

### 7.10 Detalle del pedido en el celular

Abre en el **celular** el pedido **GA** (está unido, así se ve el bloque del grupo).

| # | Haz esto | Antes | Debes ver ahora |
|---|---|---|---|
| 1 | Mira el encabezado | "← Regresar" al lado del título lo apretaba; la hora se partía en dos renglones | "← Regresar" arriba, el título y la hora completos abajo |
| 2 | Baja al bloque **🔗 Unido en el grupo** | La tabla se cortaba en el nombre del cliente; el estado y el total solo se veían deslizando de lado | Cada pedido en dos renglones: número y total; cliente y estado. Sin deslizar de lado |
| 3 | Toca **➕ Agregar artículo** (elige el pedido si pregunta) | No se veía nada: el buscador se abría una pantalla más abajo | La pantalla baja sola al buscador y el teclado se abre |
| 4 | Escribe **3 letras** de un artículo | Cada resultado decía "B.", "Talla:" y "M" en renglones sueltos y el precio se encimaba | Nombre completo, "Talla · Color" en un renglón, y abajo el precio con **Otro precio** y **Elegir** |
| 5 | Toca **⇄** en un artículo | Igual que el 3 | El buscador queda en pantalla con el cursor puesto |
| 6 | En ningún paso | — | La pantalla **no** se mueve de lado a lado |
| 7 | Ábrelo en la **computadora** | — | **Lo mismo que antes** (la tabla del grupo con sus títulos) |

### 7.9 Lo que no debe cambiar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Repite **5.7 a 5.11** con pedidos que **no** estén unidos | **Lo mismo que antes** |
| 2 | Busca un pedido por su **número** | **Lo mismo que antes** |
| 3 | **⇄** en un artículo de **1** pieza, y **➕ Agregar artículo** | **Lo mismo que antes** (3.2, 3.3) |
| 4 | **−** en un pedido con modelos distintos (3.1) | **Lo mismo que antes** |

💬 Notas:

- [ ] 7.1 · [ ] 7.2 · [ ] 7.3 · [ ] 7.4 · [ ] 7.5 · [ ] 7.6 · [ ] 7.7 · [ ] 7.8 · [ ] 7.9 · [ ] 7.10 — **Prueba 7 terminada**

---

## Prueba 8 — Cobrar a crédito desde la card de Mis pedidos — ⏳ en QA desde el 2026-10-06

**Para qué es:** antes, **Cobrar** en un Apartado o un Ir pagando te mandaba a **Créditos / Abonos**
y había que regresar a Mis pedidos para el siguiente. Ahora cada card cobra ahí mismo, con su propio
formulario.

### Mapa de impacto

| # | Le pega a | Antes | Después |
|---|---|---|---|
| 8.1 | Card de **Apartado** (suelto) | "Cobrar" → aviso → Créditos / Abonos | **Liquidar** → formulario con lo que debe, fijo |
| 8.2 | Card de **Ir pagando** (suelto) | Igual que 8.1 | **Dar abono** → formulario con monto libre |
| 8.3 | Card de **Apartados unidos** | "Cobrar" → "entra al detalle" | **Liquidar** → saldo de todo el grupo, fijo |
| 8.4 | Card de **Ir pagando unidos** | Igual que 8.3 | **Abonar al grupo** → monto libre |
| 8.5 | Card de **contado** (suelto o unido) | Cobrar → diálogo de cobro | **Lo mismo que antes** |
| 8.6 | **Créditos / Abonos** y **💳 Registrar abono** / **💵 Abonar al grupo** del detalle | — | **Lo mismo que antes** (no se tocaron) |

**Antes de empezar:** con la **Receta** crea: **L1** Apartado sin dinero (Art-A); **L2** Ir pagando
con $50 de enganche (Art-A + Art-B, total $200); **L3** y **L4** Apartados sin dinero (Art-A cada uno)
y únelos; **L5** y **L6** Ir pagando sin dinero (Art-A cada uno) y únelos; **L7** contado sin cobrar.

### 8.1 Liquidar un Apartado (L1)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → busca **L1** | El botón de la card dice **💲 Liquidar** (antes "Cobrar") |
| 2 | Toca **Liquidar** | Se abre **💵 Liquidar Apartado** ahí mismo: "Se cobra completo **$100.00**", sin poder escribir otro monto |
| 3 | Efectivo, **Monto recibido 500** | "Cambio a devolver: **$400.00**" |
| 4 | Toca **💵 Liquidar** | **¡Pedido liquidado!** con el cambio y **🖨️ Imprimir ticket**. Al cerrar, sigues en Mis pedidos y la card de L1 dice **Pagado** |
| 5 | Repite con otro Apartado y toca **¿Dejó solo una parte? Cámbialo a Ir pagando** | Se abre su detalle, donde está **🔁 Cambiar forma de cobro** |

❌ **Está mal si:** te manda a Créditos / Abonos, deja escribir un monto menor, o hay que recargar para ver Pagado.

### 8.2 Dar abono a un Ir pagando (L2)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la card de **L2** toca **Dar abono** | **💳 Dar abono**: Total $200, Pagado $50, Saldo **$150** |
| 2 | Escribe **200** | "Es más de lo que se debe: el saldo es de $150.00" y el botón apagado |
| 3 | Escribe **50**, efectivo, recibido **100** → **💳 Registrar abono** | **Abono registrado**, "Saldo restante: **$100.00**. Cambio al cliente: **$50.00**" |
| 4 | Vuelve a **Dar abono** → toca **Liquidar todo ($100.00)** → Transferencia → guardar | **¡Pedido liquidado!**; la card queda **Pagado** |
| 5 | **Créditos / Abonos** → pestaña **✅ Liquidados** | L2 aparece con sus 3 pagos ($50, $50, $100) |

### 8.3 Liquidar Apartados unidos (L3 + L4)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la card del grupo toca **Liquidar** | **💵 Liquidar pedidos unidos** con #L3 y #L4 y "Se cobra completo **$200.00**" (fijo) |
| 2 | Toca **💵 Liquidar** | **Grupo pagado**: "Pagado hoy $200.00 · Falta $0.00". La card queda pagada |

### 8.4 Abonar a Ir pagando unidos (L5 + L6)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la card del grupo toca **Abonar al grupo** | Total $200, Pagado $0, Saldo **$200** |
| 2 | Escribe **150** → guardar | **Abono registrado**: "Pagado del grupo $150.00 · Falta $50.00". En la card: "Falta $50.00" |
| 3 | Abre el detalle de L5 | El bloque del grupo dice Pagado $150 · Saldo $50 (L5 pagado, L6 debe $50: primero el más viejo) |

### 8.5 a 8.12 — Que no se haya roto nada

Estas pruebas no usan el botón nuevo: revisan las pantallas que cobran **por otro camino**, que
deben seguir **igual que antes**. Usa pedidos nuevos (con la **Receta**) para no mezclar números.

| # | Pantalla | Haz esto | Debes ver (igual que antes) |
|---|---|---|---|
| 8.5 | Card de **contado** sin cobrar | **Cobrar** → efectivo → **Confirmar cobro** | El diálogo de cobro de siempre; la card queda **Entregado** |
| 8.6 | Card de **contado** | **Cobrar** → tarjeta | Sale la opción de terminal y meses como siempre (no cobres de verdad: **Cancelar**) |
| 8.7 | Card de **contado unidos** | **Cobrar** | "Se cobran juntos los que falten: $…" y cobra el grupo completo |
| 8.8 | **Ventas → 💳 Créditos / Abonos** | **+ Abono** a un Ir pagando: 50, efectivo, recibido 100 | Cambio $50, ticket, "Saldo restante" correcto. El formulario de esa pantalla **no** cambió |
| 8.9 | **Créditos / Abonos** | **+ Abono** a un Apartado con menos del total | Sigue saliendo "Un Apartado se paga completo" con **Ir al pedido** |
| 8.10 | **Detalle del pedido** (Ir pagando suelto) | **💳 Registrar abono** | Igual que antes: monto, monto recibido, cambio, "Abono registrado" |
| 8.11 | **Detalle del pedido** (unidos Ir pagando) | **💵 Abonar al grupo** en el bloque del grupo | Igual que antes: "Pagado del grupo / Falta" |
| 8.12 | **Mis pedidos** con filtros puestos (ej. **🕒 Por cobrar**) y en la **página 2** | Cobra una card con **Dar abono** | Te quedas en la página 2 con los mismos filtros; la card se actualiza (si quedó pagada, ya no sale en "Por cobrar") |

### 8.13 a 8.16 — Permisos, cliente y celular

| # | Haz esto | Debes ver |
|---|---|---|
| 8.13 | Entra con un usuario **cliente** (no administrador) → **Mis pedidos** | Ningún botón de **Liquidar**, **Dar abono** ni **Cobrar** |
| 8.14 | *(Opcional, si tienes un usuario administrador de prueba)* En **Gestión de roles** quítale a su rol la acción **Abonar y liquidar (tarjeta y detalle)** de Mis pedidos (antes se llamaba "Registrar abono") y vuelve a iniciar sesión con él | En cards de Apartado / Ir pagando **no** sale el botón; en contado sigue **Cobrar** si tiene **cobrar** |
| 8.15 | En el **celular** abre los 4 formularios (8.1 a 8.4) | Caben en la pantalla, se puede bajar dentro del formulario y el botón del chat **no** tapa **Liquidar** / **Registrar abono** |
| 8.16 | En un formulario escribe un monto mayor al saldo y toca afuera / **Cancelar** | No se registra nada; la card queda igual |

❌ **Está mal si:** Créditos / Abonos o el detalle cambiaron en algo, el contado ya no abre su diálogo,
el cliente ve botones de cobro, o al cobrar se pierde la página o los filtros.

💬 Notas:

- [ ] 8.1 · [ ] 8.2 · [ ] 8.3 · [ ] 8.4 · [ ] 8.5–8.12 · [ ] 8.13–8.16 — **Prueba 8 terminada**

---

## Prueba 9 — Gestión de roles al día con Mis pedidos — ⏳ en QA desde el 2026-10-06

**Qué cambió.** En **Sistema → Gestión de roles**, los permisos de **Mis pedidos** describían la
pantalla de antes: "Filtro: Normal", "junto al buscador por lugar"... Los bloques nuevos de
**⚙️ Filtros** (Pendiente, Por cobrar, Entregado, Dinero, Fecha de entrega, Dónde se entrega,
Unidos y otros, Registrado, Total) no tenían permiso, así que no se podían quitar por rol. Y
"Registrar abono" ahora también son los botones **Liquidar / Dar abono / Abonar al grupo** de la card.

**Antes de empezar (una sola vez):**
1. ✅ Ya hecho el 2026-10-06: `migration_accion_pedidos_filtros_y_cobro.sql` se corrió en
   `inventario_key_qa` (y en prod) y la verificación dio **0**. No hay que volver a correrla.
2. **Cierra sesión y vuelve a entrar** (los permisos van dentro del token). Si no lo haces, los
   bloques nuevos de ⚙️ Filtros **no salen** aunque seas administrador.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 9.1 | Etiquetas, categorías y orden de las acciones de Mis pedidos | Gestión de roles | **Sistema → 🛡️ Gestión de roles** → un rol → **Mis pedidos** | 3 grupos de filtros con nombres viejos; "Unir pedidos" y "Quitar promoción" con el mismo orden | Grupos **Filtros — forma de cobro**, **Filtros — estado**, **Filtros — más filtros**, **Tarjeta de pedido**, **Detalle del pedido**, en ese orden y sin grupos repetidos |
| 9.2 | 9 acciones nuevas, dadas solo a ROLE_ADMIN | Panel ⚙️ Filtros (administrador) | **Pedidos → Mis pedidos → ⚙️ Filtros** | Todos los bloques | **Lo mismo que antes**: todos los bloques y todas las opciones |
| 9.3 | Filtros guardados | Lo que se carga al entrar | Mis pedidos | Tus filtros guardados | **Lo mismo que antes** |
| 9.4 | Roles que ya tenían acciones | Lo que cada rol puede hacer | Gestión de roles | Sus casillas marcadas | **Las mismas casillas marcadas** (no se quita ni se agrega nada a otros roles) |
| 9.5 | Ayuda **?** de Mis pedidos y de Créditos / Abonos | El texto de ayuda | El **?** arriba de cada pantalla | Abonos decía "lo que un cliente abonó a cuenta de un apartado" | Dice Ir pagando, y que un Apartado se liquida completo |
| 9.6 | **El cambio:** quitar un bloque de filtros a un rol | Panel ⚙️ Filtros de ese rol | Mis pedidos | No se podía | El bloque desaparece |

### 9.1 a 9.5 — Que todo siga igual y los textos estén al día

| # | Haz esto | Debes ver |
|---|---|---|
| 9.1 | **Sistema → 🛡️ Gestión de roles** → **ROLE_ADMIN** → abre **Pedidos → Mis pedidos** | En este orden: **Filtros — forma de cobro** (Contado, Apartado, Ir pagando) · **Filtros — estado** (Pendiente, Por cobrar, Pagado, Entregado, Cancelado) · **Filtros — más filtros** (Dinero, Fecha de entrega, Dónde se entrega, Unidos y otros, Registrado, Total del pedido) · **Tarjeta de pedido** (Entrega, Cobrar de contado, Imprimir ticket, Enviar comprobante, Cancelar pedido, **Abonar y liquidar**) · **Detalle del pedido** (Editar ramo, Quitar piezas, Cambiar forma de cobro, Agregar artículo, Cambiar un artículo, Quitar promoción, **Unir y separar pedidos**). **Todas marcadas** |
| 9.1b | Toca el **ℹ️** de **Abonar y liquidar** | "Tarjeta: Liquidar, Dar abono, Liquidar el grupo y Abonar al grupo. Detalle: 💳 Registrar abono y 💵 Abonar al grupo…" |
| 9.2 | **Pedidos → Mis pedidos → ⚙️ Filtros** (como administrador, después de volver a entrar) | Los 8 bloques de siempre con **todas** sus opciones, igual que en la Prueba 5 |
| 9.3 | Si tenías filtros guardados, sal de Mis pedidos y vuelve a entrar | Se cargan los mismos filtros que tenías |
| 9.4 | En Gestión de roles abre **otro rol** que ya tenga algo marcado en Mis pedidos | Siguen marcadas las mismas casillas que antes; las nuevas salen **sin** marcar |
| 9.5 | Toca el **?** en **Mis pedidos** y en **Ventas → 💳 Créditos / Abonos** | Mis pedidos menciona ⚙️ Filtros y el cobro desde la tarjeta. Abonos dice que un Apartado se liquida completo y que los abonos son de Ir pagando |
| 9.5b | **Sistema → 🗂️ Menús y submenús** → submenú **Créditos / Abonos** | Descripción: "Pedidos Apartado e Ir pagando: registrar abonos y liquidarlos…" |

### 9.6 — El cambio: quitar filtros por rol *(opcional, necesitas un usuario administrador de prueba)*

| # | Haz esto | Debes ver |
|---|---|---|
| 9.6.1 | En Gestión de roles, al rol de tu usuario de prueba **desmarca** "Filtro: Dinero" y "Filtro: Pendiente" y guarda | Se guarda sin error |
| 9.6.2 | Entra con ese usuario → **Mis pedidos → ⚙️ Filtros** | **No** sale el bloque **Dinero** y en **Estado** **no** sale **⏳ Pendiente**; lo demás sí |
| 9.6.3 | Desmarca también Registrado y Total | Desaparece el bloque de fechas y montos |
| 9.6.4 | Vuelve a marcar todo y vuelve a entrar | Regresa todo |

❌ **Está mal si:** al administrador le falta un bloque o una opción de ⚙️ Filtros después de volver
a entrar, otro rol perdió o ganó una casilla, en Gestión de roles un grupo sale dos veces o
partido, o la migración da error en Workbench.

💬 Notas:

- [ ] 9.1 · [ ] 9.2 · [ ] 9.3 · [ ] 9.4 · [ ] 9.5 · [ ] 9.6 — **Prueba 9 terminada**

---

## Prueba 10 — Arreglos del 2026-10-06 — ⏳ 10.1 en prod, 10.2 y 10.3 sin subir

**Para qué es:** comprobar los tres arreglos de hoy. La **10.1** ya está en **prod** (se prueba en
prod). La **10.2** va a prod y a QA, y la **10.3** solo a QA: las dos esperan tu "sube".

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 10.1 | "Crear artículos" (🧩 de la tarjeta del modelo) copia del modelo color, marca, descripción, contenido neto y categoría | **Catálogo → 🔍 Modelos → 🧩 Productos** | En prod el artículo nacía vacío (y daba 400) | Nace con esos 5 datos, igual que en QA |
| 10.1 | Mensaje cuando la base rechaza un guardado | Cualquier alta o cambio que use el guardado genérico (clientes, artículos, direcciones…) | Decía "El codigo postal ya existe" | Dice el motivo real. Un duplicado sigue diciendo lo mismo de antes |
| 10.2 | El campo de fecha acepta también una fecha "de calendario" | **Mis datos** (fecha de nacimiento) y todos los campos de fecha (filtros de pedidos, gastos, reportes) | Mis datos se quedaba con el spinner encima y no dejaba hacer nada | Carga normal. Los demás campos de fecha, **igual que antes** |
| 10.3 | Filtro **Estado** de Mis pedidos | **Pedidos → Mis pedidos → ⚙️ Filtros → Estado** | Un Apartado podía salir en "⏳ Pendiente" y su card decía "Por cobrar" | Un Apartado / Ir pagando solo sale en "🕒 Por cobrar", "✅ Pagado" o "❌ Cancelado", igual que su card |

### 10.1 Crear artículos desde la tarjeta del modelo (en **prod**)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar modelo**: modelo de prueba con stock **1**, color, marca, descripción, contenido neto y una categoría → Guardar | Se guarda |
| 2 | **Catálogo → 🔍 Modelos** → búscalo → **🧩 Productos** → cantidad **1**, sin imagen → **Crear variantes** | *"1 variante(s) creada(s)"* (ya no el 400) |
| 3 | Abre el artículo nuevo (✏️ en su tarjeta de Tienda) | Trae el color, la marca, la descripción, el contenido neto y la categoría del modelo. Stock 1 |
| 4 | Vuelve a **🧩 Productos** del mismo modelo, cantidad 1 | *"Stock insuficiente… Stock disponible: 0"* (es correcto: ya no hay piezas libres) |

❌ **Está mal si:** vuelve a salir 400. Si pasa: herramientas del navegador (F12) → **Red** → la
línea `inicializarDesdeProducto` → pestaña **Respuesta**, y me pegas lo que diga.

### 10.2 Mis datos sin spinner (prod y QA, cuando se suba)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra a **Mis datos** con un usuario que ya tenga su fecha de nacimiento guardada | Se ven tus datos, la fecha dice p. ej. *"12 de mayo de 1990"* y el spinner **se quita** |
| 2 | Cambia el teléfono y **Guardar** | Se guarda. La fecha de nacimiento **no cambia** (ni un día antes) |
| 3 | **Mis pedidos → ⚙️ Filtros → Registrado** desde / hasta | Los calendarios funcionan igual que antes |

❌ **Está mal si:** el spinner sigue girando, o la fecha se guarda un día antes.

### 10.3 Filtro "Pendiente" vs "Por cobrar" (QA, cuando se suba)

> ⚠️ **Ya no se prueba así:** con la Prueba 11 el filtro Estado se partió en **Pago** y **Entrega** y
> "Pendiente" / "Por cobrar" se juntaron en **💰 Falta pagar**. Prueba la **11.4** en lugar de esta.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos → ⚙️ Filtros**: Forma de cobro **📦 Apartado** + Estado **⏳ Pendiente** | **Ningún** pedido. "Pendiente" es solo para contado sin cobrar |
| 2 | Cambia Estado a **🕒 Por cobrar** | Los Apartados abiertos; su card dice **Por cobrar** |
| 3 | Forma de cobro **🛒 Contado** + **⏳ Pendiente** | Contados sin cobrar; su card dice **Pendiente** (igual que antes) |

### Respuestas a tus dudas de los filtros (2026-10-06)

💬 *"Elegí Apartado y en estado sale Pendiente… ¿por qué un Apartado quedaría en pendiente? En la card dice Por cobrar."*
↳ Tenías razón, era un error. "Pendiente" es solo para **contado sin cobrar**. Algunos Apartados
tienen guardado por dentro el estado "Pendiente" (el cobro de la frase de listón nacía así), y el
filtro le hacía caso a eso mientras la card decía "Por cobrar". Ya quedó: el filtro dice lo mismo
que la card (10.3), y el cobro de la frase ya nace como Apartado. Las opciones de Estado se siguen
viendo todas aunque elijas Apartado; con Apartado, "Pendiente" y "Entregado" simplemente no traen nada.

💬 *"Liquidar con el link 'dejar algo y cambiarlo a Ir pagando' está bien y me gusta."*
↳ ✅ Se queda así.

💬 *"Filtro Pagados: sale uno que dice Entregado, pero ¿dónde marco que se entregó?"*
↳ Hoy **no hay** botón para marcar "entregado" a un Apartado o Ir pagando liquidado: queda en
**Pagado**. "Entregado" es como queda una venta de **contado** al cobrarla (el cliente se la lleva en
ese momento). El botón **Entregar** para lo pagado que falta entregar está acordado
(skill `reglas-pedidos` 2.3, "Pagado · falta entregar") pero **todavía no se programa**. Si en
"Pagados" viste una card que dice "Entregado", pásame su número: puede ser un grupo de pedidos y lo
reviso.

💬 *"Filtro Saldo a favor: salen muchos y no sé por qué."*
↳ "Saldo a favor" = pedidos donde **hay que devolverle dinero al cliente**: (a) pagó de más, (b) se
canceló un **Apartado** que ya tenía dinero, o (c) se canceló un pedido **ya pagado**. En QA salen
muchos porque las pruebas 2, 3 y 4 cancelaron y cambiaron pedidos con abonos a propósito. Para ver
cuáles son y por qué:
```sql
SELECT id, tipo_pedido, estado_pedido, total_pedido, total_pagado,
       total_pagado - total_pedido AS a_favor
FROM pedidos
WHERE tipo_pedido IN ('APARTADO','FIADO') AND total_pagado > 0
  AND ((estado_pedido <> 'cancelado' AND total_pagado > total_pedido)
       OR (estado_pedido = 'cancelado' AND (tipo_pedido = 'APARTADO' OR total_pagado >= total_pedido)))
ORDER BY id DESC LIMIT 50;
```

💬 *"Ir pagando tiene lo mismo… si en Apartados no puede haber abonos, ¿por qué?"*
↳ Un Apartado nuevo no acepta abonos a medias (desde el 2026-10-01), pero sí puede tener dinero
cuando se **liquida completo**, y si después se cancela, ese dinero queda **a favor**. Los Apartados
viejos (antes del 2026-10-01) pueden tener abonos de antes. En Ir pagando, "Saldo a favor" sale
cuando pagó de más o cuando se canceló ya pagado; un Ir pagando cancelado que **todavía debía** no
sale (eso es deuda, no saldo a favor).

💬 *"Ordenar: el select se ve muy básico."*
↳ Anotado: va en la homologación de pantallas (todos los selects con el mismo diseño).

💬 Notas:

- [ ] **Prueba 10 terminada**

---

## Prueba 11 — Entregado aparte del pago y pantallas homologadas — ⏳ sin subir

**Para qué es:** desde ahora la card de un pedido dice **dos cosas**: si ya **pagó** y si ya se lo
**llevó**. Verde lo que ya está, rojo lo que falta. Más el stock del modelo desde Agregar artículo y
todas las pantallas con el mismo ancho y diseño.

### 11.0 Antes de empezar (lo hago yo cuando digas "sube")

1. En `inventario_key_qa`, en este orden: `migration_entrega_pedido.sql`, `migration_accion_gastos_admin.sql`
   y `migration_tema_jade.sql`. Después se sube el back y el front a QA.
2. **Tú:** cierra sesión y vuelve a entrar (los permisos nuevos viajan al entrar) y `Ctrl + Shift + R`.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 11.1 | Columna nueva `pedidos.entregado` (la llena el script con lo que ya había) | Todas las cards de **Mis pedidos** | Una etiqueta: Pendiente / Por cobrar / Pagado / Entregado / Cancelado | Dos: **Pagado / Falta pagar $X** y **Entregado / Falta entregar** (cancelado: solo "Cancelado") |
| 11.2 | Botones **📦 Entregar** y **↺** en la card y en el detalle | **Mis pedidos**, **👁 Detalle** | No había forma de marcar entregado | 📦 marca entregado (o todo el grupo); ↺ lo regresa (solo administrador) |
| 11.3 | Pregunta **"¿Ya se lo llevó?"** | Venta directa, Carrito (Ir pagando), cobrar desde la card, liquidar en el detalle, Créditos / Abonos, pagar un grupo | Contado quedaba "Entregado" siempre; un Apartado liquidado quedaba "Pagado" sin saber si se lo llevó | Se pregunta. "Todavía no" deja **Falta entregar** con 📦 en la card |
| 11.4 | Filtro **Estado** partido en **Pago** y **Entrega** | **Mis pedidos → ⚙️ Filtros** | Pendiente / Por cobrar / Pagado / Entregado / Cancelado | Pago: 💰 Falta pagar · ✅ Pagado · ❌ Cancelado. Entrega: 📦 Falta entregar · 🤝 Entregado |
| 11.5 | Cancelar un **Ir pagando** | Cancelar desde Mis pedidos o Créditos / Abonos | El stock nunca regresaba | Si **no** se lo había llevado, el stock **sí** regresa. Si ya se lo llevó, igual que antes |
| 11.6 | **Agregar artículo**: stock del modelo | **Catálogo → 🧩 Agregar producto** | Si al modelo no le quedaba stock, había que salir a subírselo | Se ve el stock total (bloqueado) y un campo para agregar o quitar |
| 11.7 | Pantallas homologadas | Ver la lista en 11.7 | Anchos distintos, encabezados blancos o verdes, "Regresar" y "Volver" | Mismo ancho que Agregar modelo, encabezado con el color de Personalización, todos "Volver" |
| 11.8 | Gastos: permisos del administrador | **Gastos** | El admin no veía el botón para agregar | Lo ve (y editar / eliminar) |
| — | **Lo que NO debe cambiar** | Total, abonado, resta, cobrar, abonar, unir y separar | — | **Lo mismo que antes**: solo cambian las etiquetas y la entrega |

**Tus pedidos para esta prueba** (créalos con la receta del principio, Art-200 = $200):

| Pedido | Cómo | Anota su número |
|---|---|---|
| **E1** | Venta directa, **Contado**, Art-200, a la pregunta "¿Ya se lo llevó?" → **✅ Sí** | |
| **E2** | Venta directa, **Contado**, Art-200, a la pregunta → **📦 Todavía no** | |
| **E3** | Venta directa, **📦 Apartado**, Art-200, sin enganche (no pregunta) | |
| **E4** | Venta directa, **💳 Ir pagando**, Art-200, enganche $50, a la pregunta → **📦 Todavía no** | |
| **E5** | Venta directa, **💳 Ir pagando**, Art-200, enganche $50, a la pregunta → **✅ Sí** | |

### 11.1 Las dos etiquetas de la card

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → busca **E1** | **Pagado** (verde) y **Entregado** (verde). Sin botón 📦 |
| 2 | Busca **E2** | **Pagado** (verde) y **Falta entregar** (rojo). Botón **📦 Entregar** |
| 3 | Busca **E3** | **Falta pagar $200.00** (rojo) y **Falta entregar** (rojo). **Sin** botón 📦 (un Apartado se entrega pagado) |
| 4 | Busca **E4** | **Falta pagar $150.00** (rojo) y **Falta entregar** (rojo). **Con** botón 📦 (un Ir pagando sí se puede llevar debiendo) |
| 5 | Busca **E5** | **Falta pagar $150.00** (rojo) y **Entregado** (verde) |
| 6 | Busca un pedido **viejo** de contado cobrado, uno de Ir pagando y un Apartado abierto | Contado: Pagado + Entregado. Ir pagando: Entregado. Apartado abierto: Falta entregar |
| 7 | Busca un pedido **cancelado** | Solo **Cancelado**, sin etiqueta de entrega ni 📦 |
| 8 | Haz lo mismo **de noche** (botón 🌙) | Verde y rojo se leen bien |

❌ **Está mal si:** el total, lo abonado o lo que falta cambió respecto a antes, o un Apartado sin pagar tiene 📦.

### 11.2 📦 Entregar y ↺ regresar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En **E2** toca **📦 Entregar** | La card pasa a **Entregado** (verde) y el 📦 desaparece. Sale el botón ↺ |
| 2 | Toca **↺** → confirma | Regresa a **Falta entregar** |
| 3 | Vuelve a tocar 📦 en E2 | Entregado otra vez |
| 4 | Abre **👁 Detalle** de **E4** | Arriba: **Falta pagar** y **Falta entregar**, y el botón 📦 Entregar |
| 5 | Toca 📦 en el detalle | Queda **Entregado**; sigue **Falta pagar $150.00** (entregar no cobra) |
| 6 | En un grupo que falte entregar (el del paso 5 de 11.3 si ahí contestas **Todavía no**), toca 📦 en la card del grupo | Se entregan **los dos** pedidos del grupo |
| 7 | Entra con un usuario **que no sea administrador** | No ve el ↺. El 📦 solo si en **Gestión de roles** su rol tiene "Entregar" |

❌ **Está mal si:** 📦 cobra algo, o el ↺ le sale a alguien que no es administrador sin dárselo en Gestión de roles.

### 11.3 "¿Ya se lo llevó?" al terminar de pagar

La pregunta solo sale a quien tiene **Entregar** en Gestión de roles (de arranque, el administrador).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **E3** (Apartado $200) → **💳 Registrar abono** $200 (liquida) | Sale **"¿Ya se lo llevó?"**. Toca **✅ Sí** → Pagado + Entregado |
| 2 | Crea otro Apartado de $200 y liquídalo desde **💳 Créditos / Abonos**; a la pregunta → **📦 Todavía no** | **Pagado** + **Falta entregar**, con 📦 en la card |
| 3 | En **E4** (ya entregado en 11.2) registra $150 | Queda Pagado. **No** pregunta (ya se lo llevó) |
| 4 | Si tienes un pedido de **contado sin cobrar** (su card tiene el botón **Cobrar**), cóbralo desde la card | Al cobrar pregunta "¿Ya se lo llevó?". La card **se queda** en la lista y se actualiza (antes desaparecía) |
| 5 | Grupo de 2 Apartados de $100 → **💵 Pagar el grupo completo** ($200) | Pregunta una vez; con **Sí**, los dos quedan Entregado |
| 6 | **Carrito** (Tienda → 🛒) → Ir pagando con enganche | Después de crear el pedido pregunta "¿Ya se lo llevó?" |
| 7 | Venta directa con **Apartado** | **No** pregunta |
| 8 | En la pregunta presiona **Esc** | Cuenta como "Todavía no": queda Falta entregar con 📦 |

### 11.4 Filtros Pago y Entrega

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **⚙️ Filtros** | Dos bloques: **Pago** (💰 Falta pagar · ✅ Pagado · ❌ Cancelado) y **Entrega** (📦 Falta entregar · 🤝 Entregado) |
| 2 | Solo **✅ Pagado** | E1, E2, E3 (ya liquidado)… todos con etiqueta verde de Pagado |
| 3 | **✅ Pagado** + **📦 Falta entregar** | Solo los pagados que no se han llevado (el Apartado del paso 11.3.2). **No** sale E1 |
| 4 | Solo **💰 Falta pagar** | Contados sin cobrar, Apartados e Ir pagando abiertos, juntos |
| 5 | **💰 Falta pagar** + **🤝 Entregado** | Ir pagando que ya se llevaron y deben (E5) |
| 6 | Forma de cobro **📦 Apartado** + **💰 Falta pagar** | Los Apartados abiertos. Ya no existe "Pendiente" para un Apartado |
| 7 | Si tenías un filtro **guardado** con "Pendiente" o "Por cobrar" | Se carga como **💰 Falta pagar** |
| 8 | **Gestión de roles → Mis pedidos** | Los filtros salen en "Filtros — pago" y "Filtros — entrega"; "Entregar" y "Regresar a Falta entregar" en "Tarjeta de pedido" |

### 11.5 Cancelar un Ir pagando

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Anota el stock de Art-200. Crea un Ir pagando de Art-200 con enganche $50 y **📦 Todavía no** | El stock bajó 1 |
| 2 | Cancélalo | *"Ir pagando cancelado. Stock devuelto (no se lo había llevado). Saldo a favor del cliente: $50.00"*. El stock **regresó** |
| 3 | Cancela **E5** (Ir pagando que **sí** se llevó, debe $150) desde **💳 Créditos / Abonos** | *"FIADO cancelado. Stock NO devuelto (producto entregado). Deuda incobrable: $150.00"*. El stock **no** regresa (igual que antes) |

### 11.6 Agregar artículo con stock del modelo

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar modelo** con stock **2** → Guardar. Crea 2 artículos de 1 (que no le quede libre) | — |
| 2 | **Catálogo → 🧩 Agregar producto** → elige ese modelo | **Stock total del modelo: 2** (gris, no se puede escribir) y al lado **Agregar (+) o quitar (−) al modelo**. Abajo *"Repartido: 2 · Libre: 0"* |
| 3 | Llena el artículo con stock **3** sin tocar el ajuste | Aviso: *"Estás repartiendo 3 y solo quedan 0. Súbele stock al modelo en el campo de arriba."* |
| 4 | Escribe **3** en Agregar (+) | *"Libre: 3 · El modelo quedaría en 5"*. Guardar → se guarda |
| 5 | Vuelve a elegir el modelo | Stock total **5**, Repartido **5** |
| 6 | Escribe **−1** | *"No se puede dejar el modelo en 4: ya tiene 5 repartidos."* y no deja guardar |
| 7 | Con un usuario sin permiso de editar modelos | Solo ve el stock total, sin el campo de agregar |

❌ **Está mal si:** el modelo sube de stock pero el artículo no se guardó (o al revés).

### 11.7 Pantallas homologadas (de día y de noche)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **Agregar modelo** y fíjate en el ancho de la tarjeta | Esa es la medida de todas |
| 2 | Abre una por una: Nuevo producto, Carga rápida, Cargar catálogo Excel, Lugares de entrega, Entregas por zona, Cinta, Hashtags, Publicar en redes, Configuración del negocio, Diagnóstico, Reconciliación, Limpiar caché, Agregar mi compra, Mi perfil, Mis datos, Cambiar contraseña | Mismo ancho, centradas. El encabezado de la tarjeta con el color de **Personalización** (no blanco en unas y verde en otras), sin un cuadro dentro de otro |
| 3 | **Clientes** y **Palabras clave (Categorías)** | Tabla: de día encabezado claro con letras en mayúsculas; de noche colores Jade oscuros |
| 4 | Cualquier select (Ordenar en Mis pedidos, lugar en Lugares de entrega) | Todos iguales: mismo alto, borde y flecha |
| 5 | Pantallas con botón para regresar | Todas dicen **"Volver"** (ninguna "Regresar") |
| 6 | **Cambiar contraseña** | Sin botón Volver y el formulario arriba (ya no hasta abajo) |
| 7 | **Sistema → Personalización** → cambia el color del encabezado de las tarjetas | Cambia en todas las pantallas de la lista |

### 11.8 Gastos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Gastos** con administrador | Botón para agregar gasto; editar y eliminar en cada uno |
| 2 | Con un usuario sin ese permiso y sin gastos | No dice "agrega uno" |

💬 Notas:

- [ ] **Prueba 11 terminada**

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

- **El cambio de "variante" a "artículo"** (rutas `/v2/articulos`): rama aparte `rename/variante-a-articulo`, no ha subido a QA.
- **Alta de modelo con sus artículos en un paso** (flujo A de `PLAN_ALTA_MODELO_Y_ARTICULOS.md`):
  esperan tus respuestas A10, A11 y si también reemplaza la ventana de 🧩 de la tarjeta.
- **"¿Viene bien o dañado?"** al cancelar o devolver y **registrar que el dinero ya se devolvió**: acordados, sin programar.
