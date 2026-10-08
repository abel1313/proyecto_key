# Pruebas que faltan — paso a paso (al 2026-10-07, actualizado 2026-10-08)

Aquí están **solo las pruebas que todavía no haces**, en el orden en que conviene hacerlas. Las que ya
validaste no vienen: **1** (Volver), **5** (filtros de pedidos), **8** (cobrar desde la card),
**9** (Gestión de roles), **14** (hotfix de prod) y los casos **3.6** y **3.7**. La **10.1** quedó cubierta
por la 14 y la **10.3** la reemplazó la **11.4**. El historial completo, con tus 💬 de antes, sigue en
`GUIA_DE_PRUEBAS_QA.md`.

Cada prueba trae:
- **Para qué es** — en una línea.
- **Antes de empezar** — qué pedidos o datos crear y qué anotar.
- **Mapa de impacto** (cuando aplica) — qué se movió, a qué le pega, qué daba **antes** y qué debe dar **después**.
- **Pasos** — a la izquierda lo que **haces**, a la derecha lo que **debes ver**.
- **❌ Está mal si…** — lo que sería un error.

**Cómo anotar:** si algo no sale como dice "Debes ver", escribe debajo de esa tabla `💬` y lo que
pasó (qué hiciste, qué esperabas, qué salió, número de pedido). Al terminar una prueba marca su
casilla `[x]`. Yo leo tus 💬 y contesto debajo con `↳`.

## Resumen: qué falta y cuándo se puede

| Orden | Prueba | Qué revisa | ¿Ya se puede? | Tiempo | ✔ |
|---|---|---|---|---|---|
| 1 | **2 — Apartado es sin dinero** | Un Apartado no acepta abonos a medias; se pasa a Ir pagando | 2.2 y 2.6 ✅ ya; 2.1, 2.4 y 2.5 cambiaron el 2026-10-08 → cuando pase a `qa` | 25 min | [ ] |
| 2 | **3 — Quitar, cambiar o agregar artículos** | Total, lo que debe y el estado al editar un pedido; ⇄ con varias piezas | ✅ Ya, en QA | 30 min | [ ] |
| 3 | **4 — Cancelar pedidos con abonos** | Stock y mensajes al cancelar | ✅ Ya, en QA | 20 min | [ ] |
| 4 | **7 — Filtros y detalle con pedidos unidos** | Grupos en los filtros, card del grupo, botón −, orden, celular | ✅ Ya, en QA | 40 min | [ ] |
| 5 | **11 — Entregado aparte del pago** | Pagado / Entregado, 📦 Entregar, "¿Ya se lo llevó?", stock del modelo, pantallas homologadas, Gastos | ✅ Ya, en QA | 60 min | [ ] |
| 6 | **12 — Lo legal** | Datos del negocio, pie de página, Términos, Aviso de privacidad, registro, ticket, Google | ✅ Ya, en QA | 40 min | [ ] |
| 7 | **13 — Fondo de los filtros y seguridad** | Recuadro de Tienda y Modelos, colores en Personalización, encabezados de seguridad | ✅ Ya, en QA (subido el 2026-10-07) | 15 min | [ ] |
| 8 | **15 — Agregar producto, habilitar, Zonas de entrega** | Código y categoría del modelo, stock al habilitar, Zonas de entrega y Entregas por zona | ✅ Ya, en QA (subido el 2026-10-07) | 30 min | [ ] |
| 9 | **16 — 🕓 Pendiente, íconos ⓘ y datos legales** | Pendiente en filtros y card, pasarlo a Apartado, Entregas por zona, ⓘ, de dónde sale cada dato legal | ✅ Ya, en QA (subido el 2026-10-07) | 30 min | [ ] |
| 10 | **10.2 — Mis datos sin spinner** | Que Mis datos cargue | ✅ Confirmada por ti el 2026-10-08 | 5 min | [x] |
| 11 | **14-QA — El hotfix de prod en QA** | Que 🧩 Productos haga en QA lo mismo que en prod | ✅ Ya, en QA (subido el 2026-10-07) | 10 min | [ ] |
| 12 | **17 — Filtros de Tienda, Venta directa, buscadores y Agregar artículos** (2026-10-08) | Filtros que se combinan, precio mín/máx, stock después de vender, nombres de 3 letras, la ventana 🧩 Agregar artículos | ⏳ Cuando pase de `dev` a `qa` (el 2026-10-08 se subió **solo a `dev`**) | 45 min | [ ] |
| 13 | **18 — Agregar artículo: todos los modelos, habilitar y agregar stock** (2026-10-08) | Buscar modelos sin stock o deshabilitados, ✅ Habilitar, Guardar en el modelo, "¿agregar los artículos de una vez?" | ⏳ Cuando pase de `dev` a `qa` (el 2026-10-08 se subió **solo a `dev`**) (el script `migration_accion_tienda_venta_ver_todos.sql` ✅ ya se corrió en QA el 2026-10-08) | 30 min | [ ] |
| 14 | **6 — Datos de prueba con un botón** | Miles de modelos y pedidos de prueba | ✅ Ya, pero **al final** | 15 min | [ ] |

**Por qué este orden:** las 2, 3 y 4 crean pedidos (A–F, H4–H8, K1–K6) que se reusan después. Las
17 y 18 (y lo que cambió de la 2) esperan a que lo del 2026-10-08 pase a `qa`. La **6** va al final: mete miles de pedidos de
prueba y con eso es más difícil encontrar los tuyos.

**Estado de las subidas (2026-10-08):** lo del 2026-10-07 (pruebas 13, 15, 16, 10.2 y 14-QA) ya está en
QA. Lo del 2026-10-08 (Prueba 17, Prueba 18 y los cambios de 2.1, 2.3, 2.4 y 2.5) se subió **solo a
`dev`**: llega a QA cuando digas "sube a qa". Scripts nuevos para QA: `migration_tema_modal.sql`
y `migration_accion_tienda_venta_ver_todos.sql`, ✅ los dos corridos en QA el 2026-10-08 (falta prod). Lista completa de pendientes:
`PENDIENTES_2026-10-08.md`.

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
| 3 | Elige la forma de cobro que pida la prueba: **💵 Contado**, **📦 Apartado** o **💳 Ir pagando** | El botón queda marcado |
| 4 | En **💰 Pago inicial (enganche)** escribe lo que pida la prueba (o déjalo vacío si dice "nada") | — |
| 5 | Toca **💰 Cobrar** | En **Contado** e **Ir pagando** pregunta *"¿Ya se lo llevó?"*: contesta lo que diga la prueba (**✅ Sí, ya se lo llevó** o **📦 Todavía no**). Si la prueba no dice nada, **✅ Sí**. En **Apartado** no pregunta |
| 5b | — | Aviso **"✅ Apartado registrado"** o **"✅ Ir pagando registrado"** con *"Pedido #**N** creado"* |
| 6 | **Anota el número N** y toca **Cerrar** | — |

El cliente es opcional: si no eliges uno, el pedido queda a tu nombre.

**Para abrir un pedido:** menú **Pedidos → Mis pedidos** → escribe su número en el buscador →
en su tarjeta toca **👁 Detalle**.

---

---

## Prueba 2 — Apartado es sin dinero

**Para qué es:** un **Apartado** es el pedido que llega por Facebook o un live y **no ha dado
dinero**: se paga **completo** al recogerlo. Si el cliente deja un adelanto, el pedido se cambia a
**Ir pagando**. Esta prueba revisa que el sistema lo respete en todos lados.

**Antes de empezar:** con la **Receta**, crea 6 pedidos con **Art-A** y anota sus números. En los de **Ir pagando**, a *"¿Ya se lo llevó?"* contesta **✅ Sí, ya se lo llevó**:

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
> 🔄 **Cambió el 2026-10-08** (tu comentario de abajo): el monto de un Apartado ya **no se puede
> mover**, y un botón explica por qué. Los pasos de abajo ya son los nuevos.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre el **👁 Detalle** de **A** y toca **💳 Registrar abono** | El campo **Monto** trae el total del pedido, con fondo de color y **no deja escribir**. Junto a él, el botón **🔒 ¿Por qué no puedo cambiar el monto?**. Arriba, la nota *"Es un Apartado: se paga completo ($…)"* |
| 2 | Intenta escribir otro número en **Monto** | No cambia: se queda el total |
| 3 | Toca **🔒 ¿Por qué no puedo cambiar el monto?** | Se abre un recuadro: *"Un Apartado es un pedido sin dinero: el cliente lo paga completo, en un solo pago, cuando lo recoge…"* y el botón **🔁 Cambiar a Ir pagando**. Vuelve a tocar 🔒 y se cierra |
| 4 | Ábrelo otra vez y toca **🔁 Cambiar a Ir pagando** | Se cierra el abono y se abre **🔁 Cambiar la forma de cobro** con **Ir pagando** marcado |
| 5 | En *"¿Cobra algo ahora?"* escribe **50**; en *"¿Por qué cambia?"* escribe *"dejó $50 de adelanto"* y toca **Guardar cambio** | Mensaje **"Quedó como Ir pagando"** con *"Se registró el cobro de $50.00"* y qué sigue; se queda hasta que tocas **Entendido**. El pedido queda **💳 Ir pagando**, pagado **$50**, y en **📋 Pagos registrados** aparece el abono de $50 |

❌ **Está mal si:** se puede cambiar el monto de un Apartado, se registra un abono menor al total mientras sigue siendo Apartado, o al cambiar a Ir pagando se pierden los $50.

💬 *"para el apartado en detalle, si pongo otro valor si me da el error o el mensaje que eso solo se hace en un pago … bloquear el monto para no dejar mover porque solo es un pago y un botón que aparezca flotando en algún lugar con el texto porque no se puede cambiar el monto … y ya que aparezca el porqué si es que no se ve el texto … bloquear el monto cuando sea apartado"* (2026-10-08)
↳ Hecho: el monto de un Apartado queda fijo en el total en los **tres** lugares donde se cobra (Detalle → 💳 Registrar abono, **Créditos / Abonos → + Abono** y **💵 Pagar el grupo completo**). Junto al monto está el botón **🔒 ¿Por qué no puedo cambiar el monto?**, que abre la explicación. Va en 2.1, 2.3 y 2.5.

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
| 1 | **Ventas → 💳 Créditos / Abonos** → busca **C** → **+ Abono** | El monto trae el total, **no deja escribir**, y junto a él está **🔒 ¿Por qué no puedo cambiar el monto?**. Abajo *"Es un Apartado: se paga completo…"* |
| 2 | Toca **🔒 ¿Por qué no puedo cambiar el monto?** | La explicación y el botón **Ir al pedido** |
| 3 | Toca **Ir al pedido** | Se abre el pedido **C** |

❌ **Está mal si:** se puede escribir un monto menor al total.

💬 *"2.3 también quedó listo"* (2026-10-08)
↳ ✅ Gracias. Ojo: por tu comentario de 2.1 aquí también se bloqueó el monto, así que los pasos 1 y 2 cambiaron (antes se escribía 50 y salía el aviso). Vale la pena repasarla rápido.

### 2.4 Cambiar la forma de cobro (pedidos E y F)

| # | Haz esto | Debes ver |
|---|---|---|
En **🔁 Cambiar la forma de cobro** salen **4 recuadros** que se tocan como botones. Cada uno
tiene el nombre en negritas y una línea abajo:

| Recuadro | Línea de abajo |
|---|---|
| **🕓 Pendiente** | *Lo pidió el cliente desde su cuenta y nadie lo ha cobrado ni apartado* |
| **Normal (contado)** | *Se paga completo ahora* |
| **Apartado** | *Sin dinero: lo paga completo al recogerlo* |
| **Ir pagando** | *Ya dio algo y va abonando* |

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **E** (Ir pagando con $50) → **🔁 Cambiar forma de cobro** | Los 4 recuadros. **Ir pagando** dice **"Así está ahora"**. El recuadro **Apartado** está en **gris** y debajo dice, escrito (sin pasar el mouse): *"🔒 No se puede: ya dio $50.00. Un Apartado es sin dinero; por eso queda como Ir pagando."* Tocarlo no hace nada |
| 2 | Mira el recuadro **🕓 Pendiente** | También gris, con *"🔒 Solo los pedidos que hace el cliente desde su cuenta quedan así; no se puede regresar a Pendiente"*. Toca **Cancelar** |
| 3 | Abre **F** (Ir pagando sin dinero) → **🔁 Cambiar forma de cobro** → toca el recuadro **Apartado** | Se pinta con el color de marca. **No** aparece *"¿Cobra algo ahora?"*; en su lugar, un recuadro amarillo: *"Un Apartado es sin dinero: el cliente lo paga completo cuando lo recoge. Si te deja algo ahora, elige Ir pagando."* |
| 4 | Toca **Guardar cambio** | Mensaje **"Quedó como Apartado"** con *"Cuando el cliente venga a recogerlo, cobra el total con 💳 Registrar abono: se paga completo, en un solo pago."* Se queda hasta que tocas **Entendido**. **F** queda **📦 Apartado** |
| 5 | Toca **💳 Registrar abono** en **F** | El monto viene fijo en el total con **🔒 ¿Por qué no puedo cambiar el monto?** (igual que 2.1) |

❌ **Está mal si:** en **E** se puede elegir Apartado, si el motivo solo se ve pasando el mouse, o si al guardar no sale ningún mensaje.

💬 *"2.4 abre E ir pagando 50 y dice el botón apartado está en gris pero no veo ningún botón que diga apartado y habíamos quedado que eso ya lo tenías que decir tal cual"* (2026-10-08)
↳ Tenías razón en las dos cosas. (1) El documento decía "botón" a lo que en pantalla es un **recuadro** con el nombre arriba y una explicación abajo, y el motivo solo salía **pasando el mouse** (en el celular no se ve nunca). Ahora el recuadro bloqueado se pone gris y el motivo va **escrito debajo**. (2) Los pasos de arriba ya describen la pantalla tal cual se ve.

💬 *"en detalle no veo la opción de la nueva opción, como Pendiente como lo habíamos visto?"* (2026-10-08)
↳ Antes Pendiente solo aparecía como texto ("Ahora está como 🕓 Pendiente") y únicamente en pedidos Pendientes. Ahora **🕓 Pendiente** sale siempre como el primer recuadro. En un pedido Pendiente dice "Así está ahora"; en los demás sale gris, porque Pendiente es el estado del pedido que hace el cliente desde su cuenta y no se regresa a él.
💬 *"hay que dejarlo así"* (2026-10-08) ↳ ✅ Decidido: a Pendiente **no** se regresa.

💬 *"F le cambié forma de pago no salió nada de mensaje y sí se cambió la forma de pago y no sale ningún mensaje de lo que dices, solo está dar abono, si doy dar abono ahí sí sale el mensaje"* (2026-10-08)
↳ El mensaje de "Forma de cobro actualizada" duraba **3 segundos** y se cerraba solo, y no decía qué seguía. Ahora dice **"Quedó como Apartado"** con lo que sigue (cobrar el total con 💳 Registrar abono) y **se queda hasta que tocas Entendido**. La nota amarilla del paso 3 sale **antes** de guardar, al tocar el recuadro Apartado; si tampoco la ves, avísame con captura.

### 2.5 Apartados unidos (pedidos C y D)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **C** → **🔗 Unir con otros pedidos** → busca **D** → elígelo → **Unir 2 pedidos** | El bloque **"🔗 Unido en el grupo #…"** con el botón **💵 Pagar el grupo completo** |
| 2 | Toca **💵 Pagar el grupo completo** | El monto trae lo que deben **C + D**, **no deja escribir**, y junto a él **🔒 ¿Por qué no puedo cambiar el monto?**. Arriba el texto *"Son Apartados: se pagan completos…"* |
| 3 | Toca **🔒 ¿Por qué no puedo cambiar el monto?** | La explicación: son Apartados, se pagan completos en un solo pago; para dejar una parte hay que separarlos y pasarlos a Ir pagando |
| 4 | Con el monto como viene → **💵 Pagar el grupo completo** | **C** y **D** quedan **Pagado** y el grupo ya no debe nada |

❌ **Está mal si:** el grupo acepta un pago menor al total.

### 2.6 En la venta: Apartado con dinero se vuelve Ir pagando

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💰 Venta directa** → agrega **Art-A** → toca **📦 Apartado** | — |
| 2 | En **💰 Pago inicial (enganche)** escribe **100** | El botón marcado cambia solo a **💳 Ir pagando** y sale *"Un Apartado es sin dinero: como te dio $100.00, queda como Ir pagando."* |
| 3 | Toca **💰 Cobrar** → a *"¿Ya se lo llevó?"* **✅ Sí, ya se lo llevó** | **"✅ Ir pagando registrado"** con *"Enganche de $100.00 registrado"* |
| 4 | Haz otra venta: **Art-A** → **📦 Apartado** → sin enganche → **💰 Cobrar** | **"✅ Apartado registrado"** |

❌ **Está mal si:** se guarda un Apartado con enganche.

💬 Notas:

- [ ] 2.1 (repetir: cambió) · [ ] 2.2 · [x] 2.3 ✅ 2026-10-08 (repaso rápido: cambió el monto) · [ ] 2.4 (repetir: cambió) · [ ] 2.5 (cambió) · [ ] 2.6 — **Prueba 2 terminada**

---

## Prueba 3 — Detalle del pedido: quitar, cambiar o agregar artículos

**Para qué es:** cuando le quitas, cambias o agregas un artículo a un pedido de **Ir pagando**, el
sistema vuelve a sumar el total y lo compara con lo que el cliente ya pagó:
- Si lo pagado **alcanza** → queda **Pagado**.
- Si **no alcanza** → queda (o regresa a) **Ir pagando**, debiendo la diferencia.

Es la que falló la vez pasada (al **agregar** un artículo el total no subía); ya está corregido.

**Antes de empezar:** con la **Receta**, crea 4 pedidos de **Ir pagando** (no uses los de pruebas anteriores). A la pregunta *"¿Ya se lo llevó?"* contesta **✅ Sí, ya se lo llevó**:

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

### 3.8 ⇄ en un artículo con varias piezas: pregunta cuántas cambiar — ✅ en QA

El **paso 5** (que se sume a la línea que ya había) ya está en QA: es el arreglo de la **Prueba 7.4**.

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

- [ ] 3.1 · [ ] 3.2 · [ ] 3.3 · [ ] 3.4 · [ ] 3.5 · [ ] 3.8 — **Prueba 3 terminada** (3.6 y 3.7 ya las validaste el 2026-10-06)

---

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
2. Con la **Receta**, crea estos pedidos con **Art-A**. En los de **Ir pagando**, a *"¿Ya se lo llevó?"* contesta **✅ Sí, ya se lo llevó** (si contestas "Todavía no", al cancelar el stock **sí** regresa: eso es la 11.5):

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

## Prueba 7 — Revisión de filtros y detalle del pedido (2026-10-06)

**Para qué es:** al revisar todo lo de filtros y detalle encontré estas fallas y ya las corregí.
Cada una tiene su prueba: **antes** (lo que pasa hoy en QA) y **después** (lo que debe pasar).

### Mapa de impacto

| # | Qué se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 7.1 | Filtros de **Mis pedidos** con pedidos **unidos** (`GET /v1/pedidos/buscar`) | Pago, Dinero, Total, Fecha de entrega, "Los que más deben" | Miraban solo al titular: un grupo que todavía debe salía como **Pagado** y no salía en **Falta pagar** | Miran al grupo, igual que la card |
| 7.2 | (mismo) | Buscar por **nombre / teléfono / artículo** del otro cliente del grupo | No encontraba nada | Sale la card del grupo |
| 7.3 | Card del titular de un grupo | Etiqueta de estado y "Entrega" | Decía **Pagado** con "Falta $100" debajo; no mostraba la fecha de entrega | Dice **Falta pagar $100.00** y muestra la fecha |
| 7.4 | **⇄** cambiando **todas** las piezas por un artículo que ya está | Detalle del pedido | Quedaban **dos líneas** del mismo artículo | Una sola línea con la suma |
| 7.5 | Botón **−** (`DELETE /v1/pedidos/{id}/detalle/{productoId}`, ahora con `detalleId`) | Pedido con **dos tallas del mismo modelo** | Podía quitar la **otra** talla | Quita la que tocaste |
| 7.6 | Orden **"Entrega más próxima"** | Mis pedidos → Ordenar | Salían primero entregados de hace meses | Primero lo que falta entregar, atrasados arriba |
| 7.7 | Aviso **📍 Entrega** de la card | Nombre o notas con comillas | Se cortaba en la comilla | Sale completo |
| 7.8 | Buscador de Mis pedidos | Flechas o Tab dentro del buscador | Regresaba a la página 1 | Te quedas donde estabas |
| 7.9 | **Lo mismo que antes** (no debe cambiar) | Pedidos **sin unir** en todos los filtros, buscar por número, ⇄ con 1 pieza, ➕ Agregar | — | **Lo mismo que antes** |

**Antes de empezar:** `Ctrl + Shift + R`. Con la **Receta** crea (a *"¿Ya se lo llevó?"* → **✅ Sí, ya se lo llevó**):

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
| 1 | Pago **💰 Falta pagar** (antes se llamaba "🕒 Por cobrar") | GA **no** salía | Sale **GA** (la card del grupo) |
| 2 | Quita ese y marca Pago **✅ Pagado** | Salía GA | GA **no** sale (al grupo le falta $100) |
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
| 1 | Mira la card de **GA** | Arriba decía **✔ Pagado** y abajo "Falta $100.00" | Arriba **Falta pagar $100.00** (rojo); abajo "Total de los 2 pedidos $200.00 · Falta $100.00" |
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

## Prueba 11 — Entregado aparte del pago y pantallas homologadas

**Para qué es:** desde ahora la card de un pedido dice **dos cosas**: si ya **pagó** y si ya se lo
**llevó**. Verde lo que ya está, rojo lo que falta. Más el stock del modelo desde Agregar artículo y
todas las pantallas con el mismo ancho y diseño.

### 11.0 Antes de empezar

Los 3 scripts ya están corridos en QA (2026-10-07). **Tú:** cierra sesión y vuelve a entrar (los permisos nuevos viajan al entrar) y `Ctrl + Shift + R`.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 11.1 | Columna nueva `pedidos.entregado` (la llena el script con lo que ya había) | Todas las cards de **Mis pedidos** | Una etiqueta: Pendiente / Por cobrar / Pagado / Entregado / Cancelado | Dos: **Pagado / Falta pagar $X** y **Entregado / Falta entregar** (cancelado: solo "Cancelado") |
| 11.2 | Botones **📦 Entregar** y **↺** en la card y en el detalle | **Mis pedidos**, **👁 Detalle** | No había forma de marcar entregado | 📦 marca entregado (o todo el grupo); ↺ lo regresa (solo administrador) |
| 11.3 | Pregunta **"¿Ya se lo llevó?"** | Venta directa, Carrito (Ir pagando), cobrar desde la card, liquidar en el detalle, Créditos / Abonos, pagar un grupo | Contado quedaba "Entregado" siempre; un Apartado liquidado quedaba "Pagado" sin saber si se lo llevó | Se pregunta. "Todavía no" deja **Falta entregar** con 📦 en la card |
| 11.4 | Filtro **Estado** partido en **Pago** y **Entrega** | **Mis pedidos → ⚙️ Filtros** | Pendiente / Por cobrar / Pagado / Entregado / Cancelado | Pago: 💰 Falta pagar · ✅ Pagado · ❌ Cancelado. Entrega: 📦 Falta entregar · 🤝 Entregado |
| 11.5 | Cancelar un **Ir pagando** | Cancelar desde Mis pedidos o Créditos / Abonos | El stock nunca regresaba | Si **no** se lo había llevado, el stock **sí** regresa. Si ya se lo llevó, igual que antes |
| 11.6 | **Agregar artículo**: stock del modelo | **Catálogo → Agregar producto** | Si al modelo no le quedaba stock, había que salir a subírselo | Se ve el stock total (bloqueado) y un campo para agregar o quitar |
| 11.7 | Pantallas homologadas | Ver la lista en 11.7 | Anchos distintos, encabezados blancos o verdes, "Regresar" y "Volver" | Mismo ancho que Agregar modelo, encabezado con el color de Personalización, todos "Volver" |
| 11.8 | Gastos: permisos del administrador | **Gastos** | El admin no veía el botón para agregar | Lo ve (y editar / eliminar) |
| — | **Lo que NO debe cambiar** | Total, abonado, resta, cobrar, abonar, unir y separar | — | **Lo mismo que antes**: solo cambian las etiquetas y la entrega |

**Tus pedidos para esta prueba** (créalos con la receta del principio, Art-200 = $200):

| Pedido | Cómo | Anota su número |
|---|---|---|
| **E1** | Venta directa, **💵 Contado**, Art-200, a la pregunta "¿Ya se lo llevó?" → **✅ Sí, ya se lo llevó** | |
| **E2** | Venta directa, **💵 Contado**, Art-200, a la pregunta → **📦 Todavía no** | |
| **E3** | Venta directa, **📦 Apartado**, Art-200, sin enganche (no pregunta) | |
| **E4** | Venta directa, **💳 Ir pagando**, Art-200, enganche $50, a la pregunta → **📦 Todavía no** | |
| **E5** | Venta directa, **💳 Ir pagando**, Art-200, enganche $50, a la pregunta → **✅ Sí, ya se lo llevó** | |

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
| 4 | Si tienes un **🕓 Pendiente** (pedido que el cliente hizo desde su cuenta; su card tiene el botón **Cobrar**), cóbralo desde la card | Al cobrar pregunta "¿Ya se lo llevó?". La card **se queda** en la lista y se actualiza (antes desaparecía) |
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
| 4 | Solo **💰 Falta pagar** | 🕓 Pendientes, Apartados e Ir pagando abiertos, juntos |
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
| 2 | **Catálogo → Agregar producto** → elige ese modelo | **Stock total del modelo: 2** (gris, no se puede escribir) y al lado **Agregar (+) o quitar (−) al modelo**. Abajo *"Repartido: 2 · Libre: 0"* |
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
| 2 | Abre una por una: Nuevo producto, Carga rápida, Cargar catálogo Excel, Zonas de entrega, Entregas por zona, Cinta de anuncios, Hashtags de redes, Publicar en redes, Negocio & Contactos, Diagnóstico, Reconciliación, Limpiar caché, Agregar mi compra, Mi perfil, Mis datos, Cambiar contraseña | Mismo ancho, centradas. El encabezado de la tarjeta con el color de **Personalización** (no blanco en unas y verde en otras), sin un cuadro dentro de otro |
| 3 | **Clientes** y **Palabras clave (Categorías)** | Tabla: de día encabezado claro con letras en mayúsculas; de noche colores Jade oscuros |
| 4 | Cualquier select (Ordenar en Mis pedidos, lugar en Zonas de entrega) | Todos iguales: mismo alto, borde y flecha |
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

## Prueba 12 — Lo legal que ya se pudo resolver

**Para qué es:** de `LEGAL_PLAN_DE_ACCION.md`, lo que **no necesitaba tus datos**: un lugar para
capturarlos (y que salgan solos donde la ley pide), Términos y Aviso de privacidad nuevos, la casilla
de Términos al registrarse, el aviso de "sin intereses" en Ir pagando, el ticket y lo de Google.

### 12.0 Antes de empezar

`migration_datos_legales.sql` ya está corrida en QA (2026-10-07). **Tú:** `Ctrl + Shift + R`.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 12.1 | Tabla y pantalla nuevas de **datos legales** | **Sistema → Negocio & Contactos** | No había dónde poner domicilio ni teléfono | Sección **⚖️ Datos legales del negocio** |
| 12.2 | El pie de página lee esos datos | **Todas** las pantallas (abajo) | Solo "© Novedades Jade" y los 3 enlaces | Además: responsable, domicilio, teléfono, correo y horario (lo que esté capturado) |
| 12.3 | Texto de **Términos y condiciones** | `/termConditions` (pie de página) | Decía "dentro de los siguientes días" (sin número); sin garantía ni derecho a cancelar | Garantía de 90 días, cancelar en 5 días hábiles, formas de pago, qué pasa con el dinero al cancelar, PROFECO |
| 12.4 | Texto del **Aviso de privacidad** | `/privacidad` | Sin responsable ni domicilio; una sola lista de para qué | Responsable, para qué (necesario / lo que tú decides), con quién y en qué país, cómo pedir tus datos y en cuántos días |
| 12.5 | **Registro**: aviso corto y casilla de Términos | Login → **Regístrate aquí** | Una casilla (privacidad) | Un párrafo corto + **dos** casillas (privacidad y Términos) |
| 12.6 | Aviso de **Ir pagando / Apartado** | **Ventas → 💰 Venta directa** y **Carrito** | Nada | "Ir pagando, sin intereses. Precio de contado $X · Total a pagar $X (CAT 0%)" |
| 12.7 | **Ticket** | Cualquier ticket (venta, abono, liquidado, cancelación) | Solo "NOVEDADES JADE" | Debajo, tus datos legales; en abonos "Abonos sin intereses (CAT 0%)"; abajo "Garantía de 90 días" |
| 12.8 | **Bot**: no prometer lo que no dice el catálogo | Chat de la tienda, chat en vivo, Instagram y Facebook | Podía decir "original" o "garantizado" | Solo lo del catálogo |
| 12.9 | **Google** | Detalle de un artículo, página que no existe, sitemap | Título igual en todo; sitemap a páginas rotas; fotos con "Imagen variante" | Título con el nombre del artículo; sitemap a la tienda; fotos con su nombre |
| — | **Lo que NO debe cambiar** | Venta, cobro, abonos, totales, entrar al sistema | — | **Lo mismo que antes** |

### 12.1 Datos legales en Negocio & Contactos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Negocio & Contactos** → baja hasta **⚖️ Datos legales del negocio** | El correo `contacto@novedades-jade.com.mx` ya lleno y **"⚠️ Falta: Nombre del responsable, Domicilio, Teléfono"** |
| 2 | En RFC escribe `ABC123` y toca **💾 Guardar datos legales** | Mensaje del RFC (13 o 12 caracteres); no se guarda |
| 3 | En Teléfono escribe `12345` → Guardar | *"El teléfono tiene que tener 10 dígitos"* |
| 4 | Llena **datos de prueba** (no los reales todavía): nombre "Prueba QA", domicilio "Calle 1, Luvianos, Edo. Méx.", teléfono `(55) 1234-5678`, horario "Lunes a sábado 10 a 19" → Guardar | *"¡Datos legales guardados!"* y "Ya se ven en el pie de página…". El aviso de "Falta" desaparece |
| 5 | Recarga la página | Los datos siguen; el teléfono aparece como `5512345678` |
| 6 | Entra con un usuario **sin** permiso de escribir en Negocio & Contactos | No puede guardar (o no ve la pantalla) |

### 12.2 Pie de página

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Recarga (`Ctrl + Shift + R`) cualquier pantalla y baja hasta el final | "Prueba QA · Calle 1, Luvianos… · Tel. 55 1234 5678 · contacto@… · Atención: Lunes a sábado 10 a 19" |
| 2 | Sin sesión (ventana privada), en la tienda | Lo mismo |
| 3 | En el celular | Se acomoda en varias líneas, sin salirse de la pantalla |
| 4 | Toca el teléfono en el celular | Abre la llamada |
| 5 | **Al terminar la prueba:** borra los datos de prueba en Negocio & Contactos (deja solo el correo) o pon los reales | — |

### 12.3 y 12.4 Términos y Aviso de privacidad

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Pie de página → **Términos y condiciones** | Secciones nuevas: **Quién vende** (con tus datos), **Formas de pago**, **Cancelar tu compra (5 días hábiles)**, **Garantía… 90 días**, **Si cancelas un pedido en el que ya pagaste algo**, **Quejas y PROFECO**. Fecha: 7 de octubre de 2026. **Ya no** dice "dentro de los siguientes días" |
| 2 | Pie de página → **Aviso de privacidad** | Título "Aviso de privacidad"; **Quién es responsable de tus datos**; **Para qué los usamos** en dos partes; OVHcloud, Google, OpenAI y Mercado Pago con su país; **Tus derechos (ARCO)** con 5 y 15 días hábiles |
| 3 | Las dos, de noche y en celular | Se leen bien |

### 12.5 Registro

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Cierra sesión → **Regístrate aquí** | Un párrafo corto ("Novedades Jade usa tus datos para…") y **dos casillas**: aviso de privacidad y **Términos y condiciones** |
| 2 | Llena todo, marca solo la de privacidad | **Registrarse** sigue gris |
| 3 | Marca también Términos → Registrarse | Se crea la cuenta como siempre (código al correo) |
| 4 | **Leer los Términos y condiciones** | Abre los Términos en otra pestaña |
| 5 | (Opcional, en la base) `SELECT username, acepto_terminos, fecha_acepto_terminos FROM usuario_modificacion ORDER BY id DESC LIMIT 1;` | `1` y la fecha de hoy |
| 6 | Entra con tu usuario de siempre | Entra normal (las cuentas de antes no se ven afectadas) |

### 12.6 Ir pagando y Apartado

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Venta directa** con Art-200 → **💳 Ir pagando** | *"💳 Ir pagando, sin intereses. Precio de contado $200.00 · Total a pagar $200.00 (CAT 0%). Puede liquidar antes cuando quiera, sin cargo."* |
| 2 | Cambia a **📦 Apartado** | *"📦 Apartado, sin dinero ahora. Se paga completo ($200.00) al recogerlo."* |
| 3 | **💵 Contado** | Ninguno de los dos avisos |
| 4 | **Carrito** con artículos → Ir pagando / Apartado | Los mismos avisos con el total del carrito |
| 5 | Haz la venta Ir pagando con $50 de enganche | Se crea igual que antes (el aviso no cambia nada del cobro) |

### 12.7 Ticket

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Imprime el ticket de una venta de contado (con los datos de prueba de 12.1 capturados) | Debajo de "NOVEDADES JADE": nombre, domicilio, teléfono, correo. Abajo: "Garantía de 90 días desde que lo recibes" |
| 2 | Ticket de un **abono** de Ir pagando | Además: "Abonos sin intereses (CAT 0%)" |
| 3 | Totales, abonos y saldo | **Igual que antes** |

### 12.8 Bot

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En el chat de la tienda pregunta "¿esta bolsa es original?" de una bolsa cuya descripción no lo diga | No contesta "sí, es original": dice lo que trae el catálogo y ofrece que alguien del negocio confirme |
| 2 | "¿Este perfume quita las manchas?" | No promete efectos en la piel |

### 12.9 Google

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre el detalle de un artículo | La pestaña del navegador dice "<nombre del artículo> — Novedades Jade" |
| 2 | Regresa a la tienda | La pestaña vuelve a "Novedades Jade — Bolsas, Pantalones…" |
| 3 | Abre `qa.shop.novedades-jade.com.mx/sitemap.xml` | Solo `/tienda/buscar`, Términos y Privacidad (ya no `/variantes/buscar` ni `/login`) |
| 4 | (Opcional) Herramientas del navegador → Elementos, en el detalle, busca `ld+json` | Un bloque con el nombre, precio en MXN y disponibilidad |

💬 Notas:

- [ ] **Prueba 12 terminada**

---

## Prueba 13 — Fondo de los filtros desde Personalización y seguridad de la tienda

**Para qué es:** dos cosas del 2026-10-07.
1. En **Tienda** y en **Catálogo → 🔍 Modelos**, el recuadro de la búsqueda y los filtros se había
   quedado **sin fondo** (todo suelto sobre la página). Vuelve el recuadro, y su color y el de cada
   filtro se cambian en **Sistema → Personalización → Formularios**.
2. La tienda ahora manda **encabezados de seguridad** (`SEGURIDAD_DATOS.md` §6, S1).

**Por qué se había perdido el fondo:** la regla del 2026-10-06 que dejó los encabezados de pantalla
en una sola franja ("sin un div dentro de otro div") volvía transparente todo `*-header__content`.
En Tienda y en Modelos ese elemento **es** el recuadro (no hay franja afuera), así que se quedó sin
nada. Esas dos pantallas salen de esa regla; las demás siguen igual.

### 13.0 Antes de empezar

1. ✅ `migration_tema_filtros.sql` corrida en `inventario_key_qa` (2026-10-07). Falta subir el
   **front** a QA (lo hago cuando digas "sube"; el back no cambia).
2. **Tú:** `Ctrl + Shift + R`.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 13.1 | La regla que vuelve transparente `*-header__content` ya no aplica a `vb-header__content` ni `pl-header__content` | **Tienda** | Título, búsqueda y filtros sueltos sobre la página | **Recuadro** blanco translúcido de día y gris oscuro de noche, con borde y sombra |
| 13.2 | (la misma) | **Catálogo → 🔍 Modelos** | Igual que Tienda: sin recuadro | Igual que Tienda: con recuadro |
| 13.3 | (la misma) | Las demás pantallas con encabezado: Créditos / Abonos, Clientes, Dashboard, Reportes, Gastos, Favoritos, Carga de imágenes, Buscar usuarios | Una sola franja con el color de Personalización | **Lo mismo que antes** |
| 13.4 | El subtítulo ya no lleva la clase `text-white-50` (blanco forzado) | "Todos nuestros productos" (Tienda) y "Catálogo de productos" (Modelos) | Gris | **Gris, igual** (sin el cambio, de día quedaba blanco sobre blanco) |
| 13.5 | 2 colores nuevos: `filtros-panel-bg` y `filtro-bg` | **Sistema → Personalización → Formularios** | No existían | "Fondo del recuadro de búsqueda y filtros (Tienda y Productos)" y "Fondo de cada filtro (casillas, fechas y precio)" |
| 13.6 | Las casillas, las fechas, **$ mín / $ máx** y **✕ Limpiar filtros** usan `filtro-bg` | Tienda y Modelos | Verde muy claro (el color de "seleccionado") | **El mismo verde** (mismo valor), pero ahora se cambia aparte |
| 13.7 | **Talla / Color / Marca** son selects | Tienda | Diseño de todos los selects (fondo de los campos) | **Lo mismo que antes**: siguen la regla de todos los selects del 2026-10-06 |
| 13.8 | Los 5 diseños predefinidos traen los 2 colores | **Personalización → Diseños predefinidos** | — | Al aplicar uno, los 2 colores cambian con él |
| 13.9 | Encabezados de seguridad en el nginx de la tienda (`default.conf`) | Todas las pantallas | No había ninguno | 6 encabezados (ver 13.4 de abajo) |
| 13.10 | La cámara y la ubicación quedan permitidas solo para la tienda | 📷 Escanear código de barras, 📡 Usar mi ubicación | Funcionaban | **Lo mismo que antes** |
| — | **Lo que NO debe cambiar** | Buscar, filtrar, guardar filtros, carrito, login | — | **Lo mismo que antes** |

### 13.1 El recuadro de Tienda y Modelos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Abre **Tienda** de día | El título, la búsqueda, **⚙️ Filtros** y los filtros dentro de un **recuadro blanco** con borde y sombra |
| 2 | Abre **⚙️ Filtros** | Las casillas, las fechas y **$ mín / $ máx** con fondo verde claro, **dentro** del recuadro |
| 3 | Debajo de **Tienda** | "Todos nuestros productos" en gris, se lee |
| 4 | Cambia a noche (🌙) | Recuadro gris oscuro con borde; todo se lee |
| 5 | En el celular | El recuadro cabe sin mover la página de lado; los filtros, uno debajo del otro |
| 6 | **Catálogo → 🔍 Modelos**, de día y de noche | Lo mismo: recuadro, filtros dentro, "Catálogo de productos" se lee |
| 7 | **Créditos / Abonos**, **Clientes** y **Reportes** | Su encabezado **igual que antes** (una franja, sin recuadro adentro) |

❌ **Está mal si:** el recuadro no se ve de día; el subtítulo desaparece; Créditos / Abonos o Clientes
ahora tienen un recuadro dentro de la franja.

### 13.2 Cambiarlo desde Personalización

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Personalización** → abre **Formularios** | Dos colores nuevos: **Fondo del recuadro de búsqueda y filtros (Tienda y Productos)** y **Fondo de cada filtro (casillas, fechas y precio)** |
| 2 | Pon el del recuadro en un color fuerte (por ejemplo amarillo) para día → guarda | Abre **Tienda**: el recuadro cambió a ese color |
| 3 | Pon el de cada filtro en otro color → guarda | Las casillas, fechas y $ mín / $ máx cambian. **Talla / Color / Marca no** (siguen el fondo de los campos) |
| 4 | **Diseños predefinidos → Jade → Usar para día y noche** | Los dos regresan: recuadro blanco translúcido y filtros verde claro |

❌ **Está mal si:** los colores no aparecen en Formularios (falta el script 13.0); cambiar el color no
cambia nada en Tienda.

### 13.3 Los filtros siguen funcionando igual

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En **Tienda** marca **Con stock** y **Habilitadas**, elige una talla y pon $ mín 100 | La lista se filtra **igual que antes** y el contador dice "N activos" |
| 2 | Sal de la pantalla y vuelve | Los filtros se quedan guardados, como antes |
| 3 | **✕ Limpiar filtros** | Se quitan y vuelve la lista completa |

### 13.4 Encabezados de seguridad

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En la computadora abre `qa.shop.novedades-jade.com.mx`, `F12` → **Red** → recarga → toca la primera fila (la página) → **Encabezados de respuesta** | `x-frame-options: SAMEORIGIN`, `content-security-policy: frame-ancestors 'self'`, `x-content-type-options: nosniff`, `referrer-policy: strict-origin-when-cross-origin`, `permissions-policy: camera=(self), geolocation=(self), microphone=()`, `strict-transport-security: max-age=31536000` |
| 2 | (Opcional, en la VPS) `curl -sI https://qa.shop.novedades-jade.com.mx/ \| grep -iE "x-frame\|strict\|nosniff\|referrer\|permissions\|content-security\|server"` | Los mismos 6 y `Server: nginx` sin número de versión |
| 3 | En el celular: **Tienda** → **📷 Escanear código de barras** | Pide permiso de cámara y lee el código **igual que antes** |
| 4 | **Sistema → Negocio & Contactos** → en el mapa, **📡 Usar mi ubicación** | Pide permiso y pone el pin **igual que antes** |
| 5 | En el celular, en la card de un artículo de **Tienda**, toca **Compartir imagen** | Abre el menú de compartir del celular (o copia la imagen), como antes |
| 6 | Abre el link de la tienda desde Facebook o Instagram | Abre normal |
| 7 | **(Va con S2)** En la VPS: `curl -sI http://qa.shop.novedades-jade.com.mx/` | `301` con `Location: https://qa.shop…`. Si sale `200`, avísame (falta el redireccionamiento) |

❌ **Está mal si:** la cámara o la ubicación ya no piden permiso o marcan error; alguna imagen,
letra o pantalla deja de cargar.

💬 Notas:

- [ ] **Prueba 13 terminada**

---

## Prueba 15 — Agregar producto con código y categoría, stock al habilitar, Zonas de entrega y Entregas por zona

**Para qué es:** lo que pediste el 2026-10-07 al revisar QA.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 15.1 | Resultados de "Buscar modelo" muestran el código de barras | **Catálogo → Agregar producto** | Solo el nombre: 3 modelos "BOLSA" no se distinguían | "BOLSA · 7501234567890" en la lista y en el modelo elegido |
| 15.2 | Habilitar un artículo viejo deshabilitado que guarda stock | **Tienda**: 🔓 Habilitar de la card y 🔓 Habilitar seleccionadas | Volvía con su stock viejo aunque el modelo ya no tuviera libre (descuadre) | Se queda con lo libre del modelo y el mensaje dice el ajuste |
| 15.3 | "Recoger en tienda" es un interruptor | **Envíos → Zonas de entrega** | Checkbox del navegador | Interruptor (prendido / apagado) |
| 15.4 | Recuadro con fondo | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona** | Formulario suelto sobre la página | Dentro de una card con el mismo fondo que **Agregar Modelo** |
| 15.8 | "Recoger en tienda" solo en una fila | **Envíos → Zonas de entrega** | Se podía prender en cualquier zona, en varias, con envío y horas | Al prenderlo se esconden Envío, Horas extra, Día y anillos; una segunda fila marcada se rechaza con el nombre de la que ya lo es |
| 15.9 | (mismo) | **Carrito** → "📍 Lugar de entrega" y **Envíos → Entregas por zona** | — | **Lo mismo que antes**: el local pide fecha de recogida; el local no sale como zona en Entregas por zona |
| 15.10 | Se quitó el "día de la semana" de la zona | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona** | Select "Sin día fijo / 🚚 Lunes…" que solo prellenaba la fecha del viaje (y podía no coincidir con la fecha escogida) | Ya no existe; la fecha del viaje se escoge en Entregas por zona y esa fecha ya dice el día |
| 15.7 | Explicación en la pantalla | **Envíos → Zonas de entrega** | Título "Catálogo de lugares de entrega", sin explicación (solo el "?") | Título **"📍 Zonas de entrega"** y recuadro "ℹ️ Para qué sirve esta pantalla…" abierto: dónde se usa la lista, qué hace cada dato y qué no hace |
| 15.5 | La búsqueda de modelos trae la categoría (solo admin) | **Catálogo → Agregar producto** → Categoría | Vacía aunque el modelo la tuviera (el back sí se la ponía al guardar) | Sale llena con la del modelo; se puede cambiar |
| 15.6 | (mismo) | **Catálogo → Modelos**, Tienda y todo lo que lista modelos | — | **Lo mismo que antes** (es un campo más en la respuesta, nadie más lo lee) |
| — | **Lo que NO cambia** | Guardar artículo, deshabilitar (sigue dejando en 0), guardar lugar, programar entregas | — | **Lo mismo que antes** |

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar producto** → busca "bolsa" | Cada resultado con su código: "BOLSA · 750…" |
| 2 | Elige uno | Arriba sale "BOLSA · 750…" y los datos del modelo ya llenos (color, marca, descripción…) y "Repartido / Libre" |
| 2b | Elige un modelo que **tenga categoría** (en **Catálogo → Modelos** se ve) | La casilla **Categoría** ya dice la del modelo. Cámbiala por otra y guarda: el artículo queda con la que escogiste |
| 2c | Elige un modelo con categoría, quítalo con ✕ y elige otro con otra categoría | La Categoría cambia a la del segundo. Si la habías cambiado a mano, se queda la tuya |
| 2d | Modelo **sin** categoría | La casilla queda vacía, como antes |
| 3 | Modelo con stock 5: un artículo habilitado con 4 y uno **deshabilitado viejo** con 3 (ver la consulta de abajo para encontrar uno o prepararlo) → en Tienda, filtro **No habilitadas**, márcalo → **🔓 Habilitar seleccionadas** | Mensaje *"…Se ajustó el stock… de 3 a 1"*; el artículo queda con 1. Con el **🔓 Habilitar** de la card también queda en 1 (ese botón no muestra el detalle) |
| 4 | Mismo caso con el modelo en 10 | Se habilita con sus 3, sin ajuste |
| 5 | Deshabilita un artículo con stock 2 y vuelve a habilitarlo | Queda en 0 (igual que antes: al deshabilitar su stock vuelve al libre) |
| 6 | **Envíos → Zonas de entrega** → nuevo lugar | "🏬 Recoger en tienda" es un interruptor; prenderlo y guardar marca la fila con "🏬 recoger en tienda" |
| 6b | **Envíos → Zonas de entrega** | Arriba el recuadro "ℹ️ Para qué sirve esta pantalla…", abierto; se cierra y abre con clic. Se lee bien de día y de noche. El "?" dice lo mismo en corto |
| 6c | **Envíos → Zonas de entrega** → nuevo lugar "El estanco" → prende **🏬 Recoger en tienda** | Se esconden Envío, Horas extra y Día de entrega; sale la nota "Esta fila es tu local…". Al **➕ Agregar**: si ya tienes la fila del local, error *"\"(tu local)\" ya es la fila de recoger en tienda. Solo puede haber una…"* y no se guarda |
| 6d | ✏️ en la fila del local que ya existe → **💾 Actualizar** | Se guarda. Si tenía envío u horas extra, ya no los muestra en la lista |
| 6e | ✏️ en una zona normal (Tejupilco) | Se ven Envío, Horas extra y, si tiene centro, los anillos, igual que antes |
| 6f | ✏️ en Tejupilco | **Ya no sale** el select "Sin día fijo / Lunes…": se quitó. Al **💾 Actualizar** se borra el día viejo que tuviera |
| 6g | **Envíos → Entregas por zona** → elige Tejupilco | Salen **todos** los de Tejupilco que faltan por entregar (ver Prueba 16). La **Fecha** del viaje sale **vacía**: la escoges tú (ya no se prellena con el día fijo) |
| 7 | **Envíos → Zonas de entrega** y **Envíos → Entregas por zona**, de día, de noche y en el celular | Todo dentro de un recuadro con borde y sombra; en el celular sin moverse de lado |

**Para el paso 3:** hoy, al deshabilitar un artículo su stock queda en 0, así que solo los viejos
tienen stock estando deshabilitados. Para encontrarlos en `inventario_key_qa`:
```sql
SELECT v.id, p.nombre AS modelo, p.stock AS stock_modelo, v.stock AS stock_articulo
FROM variantes v JOIN producto p ON p.id = v.producto_id
WHERE v.habilitado = '0' AND v.stock > 0
ORDER BY v.id DESC LIMIT 10;
```
Si no sale ninguno, prepara uno de prueba (solo en QA, con un artículo de un modelo de prueba):
`UPDATE variantes SET habilitado = '0', stock = 3 WHERE id = <id del artículo>;`

💬 Notas:

- [ ] **Prueba 15 terminada**

---

## Prueba 16 — 🕓 Pendiente, cambiarlo a Apartado, Entregas por zona, íconos ⓘ y datos legales

**Para qué es:** lo que pediste el 2026-10-07: que el pedido que el cliente hace desde su cuenta
(**Pendiente**) se vea en los filtros, que se pueda pasar a Apartado (o Ir pagando) y que después
se vea como tal en todos lados, y el ícono ⓘ que explica cada opción.

### Mapa de impacto

| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 16.1 | Filtro **Forma de cobro** tiene **🕓 Pendiente** | **Pedidos → Mis pedidos → ⚙️ Filtros** | No existía: el pedido del cliente salía en "🛒 Contado" + "Falta pagar" | "🕓 Pendiente" muestra solo esos. **"🛒 Contado" ya no los incluye** (solo los que se cobraron completos) |
| 16.2 | (mismo) | Filtros guardados con "Contado" + "Falta pagar" | Daban los Pendientes | Dan 0: hay que cambiar a "🕓 Pendiente" |
| 16.3 | Etiqueta **🕓 Pendiente** en la card | **Mis pedidos** (card) | La card solo decía "Pendiente" abajo, sin forma de cobro arriba | Arriba, junto al número, "🕓 Pendiente" con contorno punteado |
| 16.4 | 🔁 **Cambiar forma de cobro** de un Pendiente → **Apartado** | Detalle del pedido, Mis pedidos, Créditos / Abonos, cancelación automática | Quedaba Apartado pero **por dentro seguía "Pendiente"**: el cancelador automático lo cancelaba igual a los 2 días de su fecha | Queda Apartado por dentro también: **ya no se cancela solo**; sale en Créditos / Abonos y en el filtro 📦 Apartado |
| 16.5 | 🔁 Pendiente → **Ir pagando** con adelanto | Detalle del pedido | Error *"es de tipo NORMAL y no tiene saldo que cobrar"* | Se registra el adelanto y queda Ir pagando |
| 16.6 | **Entregas por zona** trae todo lo que falta entregar | **Envíos → Entregas por zona** | Solo Pendientes y Apartados | También los **Ir pagando que no se ha llevado** y los **Pagados que faltan por entregar**. Siguen fuera: cancelados, entregados y ramos |
| 16.8 | Debajo de cada dato legal dice **de dónde sale** y **dónde se ve** | **Sistema → Negocio & Contactos → ⚖️ Datos legales del negocio** | Solo el nombre del campo y "Usa los mismos datos de tu constancia" (que para el domicilio era incorrecto) | Recuadro "📄 Antes de llenar…" y una línea por campo |
| 16.7 | Íconos **ⓘ** | Filtros de Mis pedidos (cada bloque) y título de Entregas por zona | No había | Al tocarlo sale qué significa cada opción. Solo lo ven el admin y los roles con **Ayuda contextual** |
| — | **Lo que NO cambia** | Cobrar desde la card, abonos, Venta directa, el carrito del cliente | — | **Lo mismo que antes** |

### Antes de empezar

1. Con una **cuenta de cliente** (no admin), haz un pedido desde la tienda con zona **Tejupilco**. Anota su número (pedido **A**).
2. Haz otro igual (pedido **B**).
3. Como admin, en **Ventas → Venta directa**: un **💳 Ir pagando** con enganche $50, lugar de entrega Tejupilco, y a *"¿Ya se lo llevó?"* → **📦 Todavía no** (pedido **C**). Otro de **💵 Contado** con Tejupilco y también **📦 Todavía no** (pedido **D**).

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** → busca A | La card dice arriba **🕓 Pendiente** (contorno punteado) |
| 2 | **⚙️ Filtros → Forma de cobro → 🕓 Pendiente** | Salen A y B. No salen C ni D |
| 3 | Quita ese filtro y marca **🛒 Contado** | Sale D (cobrado de contado). **No** salen A ni B |
| 4 | Toca la **ⓘ** junto a "Forma de cobro" | Se abre un recuadro con qué significa Pendiente, Contado, Apartado e Ir pagando. Se cierra con ✕, con Esc o tocando afuera. Prueba las ⓘ de Pago, Entrega, Dinero, Fecha de entrega, Dónde se entrega y Unidos y otros |
| 5 | Abre A → **🔁 Cambiar forma de cobro** | Dice "Ahora está como **🕓 Pendiente (lo pidió el cliente desde su cuenta, sin cobrar)**" |
| 6 | Elige **Apartado** → guarda | "Quedó como Apartado". La card ya dice **📦 Apartado**; sale en el filtro 📦 Apartado y en **Créditos / Abonos**; **ya no** sale en 🕓 Pendiente |
| 7 | Abre B → 🔁 → **Ir pagando**, en "¿Cobra algo ahora?" pon **$100** → guarda | Queda **💳 Ir pagando** con $100 abonados y "Falta $(total − 100)" |
| 8 | **Envíos → Entregas por zona** → Tejupilco, rango que incluya los 4 pedidos | Salen **A, B, C y D** |
| 9 | Toca la **ⓘ** del título "📦 Entregas por zona" | Explica qué pedidos salen, el rango de fechas, qué hace "Programar" y el aviso de los 🕓 Pendientes |
| 9b | **Sistema → Negocio & Contactos** → baja a **⚖️ Datos legales del negocio** | Arriba el recuadro "📄 Antes de llenar, ten a la mano tu Constancia…" con el aviso de no copiar el domicilio de la constancia de hoy; debajo de cada campo "De dónde" y "Se ve en". Se lee bien de día y de noche |
| 10 | Quita **Ayuda contextual** a un rol (Sistema → Gestión de roles), entra con un usuario de ese rol | No ve ninguna ⓘ (ni el "?" de la pantalla) |

**❌ Está mal si…** al día siguiente de la fecha + 2 días el pedido A (ya Apartado) aparece cancelado;
"🛒 Contado" trae pedidos que nadie ha cobrado; la ⓘ tapa los filtros en el celular sin poder cerrarla.

💬 Notas:

- [ ] **Prueba 16 terminada**

---

## Prueba 10.2 — Mis datos sin spinner

**Para qué es:** **Mis datos** se quedaba con el spinner encima cuando el usuario ya tenía su fecha de
nacimiento guardada. Va a QA y a prod con tu "sube".

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra a **Mis datos** con un usuario que ya tenga su fecha de nacimiento guardada | Se ven tus datos, la fecha dice p. ej. *"12 de mayo de 1990"* y el spinner **se quita** |
| 2 | Cambia el teléfono y **Guardar** | Se guarda. La fecha de nacimiento **no cambia** (ni un día antes) |
| 3 | **Mis pedidos → ⚙️ Filtros → Registrado** desde / hasta | Los calendarios funcionan igual que antes |

❌ **Está mal si:** el spinner sigue girando, o la fecha se guarda un día antes.

💬 Notas:

- [ ] **Prueba 10.2 terminada**

---

## Prueba 14-QA — El hotfix de prod, ya en QA

**Para qué es:** el arreglo de **🧩 Productos** (cuánto stock queda y dónde está) ya lo validaste en
**prod**. Cuando lo baje a `qa` y `dev` (va junto con tu "sube"), hay que confirmar que en QA hace lo
mismo y que no chocó con lo de QA (artículo nace con los datos del modelo, Agregar producto).

**Antes de empezar:** en QA, un modelo de prueba con stock **3** y un artículo con stock **1** con foto.

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Modelos** → en el modelo **🧩 Productos** | *"Stock del modelo: 3 · En sus artículos: 1 · Puedes crear: 2"* |
| 2 | Pon **3** en cantidad → Crear | No deja: *"Puedes crear hasta 2"* |
| 3 | Pon **2**, elige una foto **sin** marcar "Misma imagen" → Crear | No deja: *"Para usar las fotos marca 'Misma imagen para todas las variantes'"* |
| 4 | Marca la casilla → Crear | "2 variante(s) creada(s)"; en **Tienda** salen con la foto y con el color, marca, descripción y categoría del modelo |
| 5 | Otra vez **🧩 Productos** del mismo modelo | No abre la ventana: *"No queda stock para artículos nuevos…"* |
| 6 | **Catálogo → Agregar producto** con ese modelo | *"Repartido: 3 · Libre: 0"*, igual que antes |

❌ **Está mal si:** sale *"Error al crear variantes — Intenta de nuevo"* (era el 400 de prod), o la
ventana deja pedir más de lo que dice "Puedes crear".

💬 Notas:

- [ ] **Prueba 14-QA terminada**

---

## Prueba 17 — Filtros de Tienda, Venta directa, buscadores y Agregar artículos (2026-10-08)

**Para qué es:** lo que reportaste el 2026-10-08 al validar en QA: los filtros de Tienda que no se
combinaban, el precio mín/máx que no encontraba los de $100, el buscador de Venta directa que no se
actualizaba después de vender, nombres de una letra guardados en Venta directa, y la ventana para dar
de alta los artículos de un modelo al guardarlo.

**Antes de empezar:** ⏳ espera mi aviso de que subió a QA y haz `Ctrl + Shift + R`. Ten a la mano:
- Un modelo con **3 o más artículos con stock**, todos **habilitados**, y uno **deshabilitado** (borde rojo).
- **Art-A** (de ~$100) y, si tienes, un artículo con **descuento activo** que en la tarjeta diga $100.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 17.1 | Búsqueda de Tienda: filtros de admin y del catálogo en **una sola** llamada | **Tienda → Buscar** (escritorio, con sesión de admin) | Las tarjetas | "Con stock" + "Habilitadas" dejaba 1; quitar un filtro no siempre volvía a buscar | Salen todos los que cumplen los dos; al quitar uno se busca con los que quedan |
| 17.2 | Precio mín/máx compara el **precio que se cobra** | Tienda → Buscar (admin y catálogo de clientes) | Las tarjetas | Mín 100 / máx 100 no encontraba los de $100 | Encuentra los que la tarjeta dice $100, también con descuento |
| 17.3 | (mismo) | Tienda sin sesión (cliente) | Las tarjetas | Comparaba el precio normal | **Lo mismo que 17.2**; lo demás del catálogo, igual que antes |
| 17.4 | Venta directa vuelve a buscar después de cobrar | **Ventas → 💰 Venta directa** | Lista de **🔍 Buscar producto** | Seguía diciendo el stock viejo | El stock ya baja solo |
| 17.5 | Validación del cliente sin registro y de quien recibe | Venta directa → registrar cliente / Datos de entrega | Aviso rojo debajo del campo | Guardaba "a" | Pide 3 letras; correo y teléfono válidos si se escriben |
| 17.6 | Buscador de **Mis pedidos** desde la cuenta del cliente | Tienda → Mis pedidos (cliente) | Debajo del buscador | Letras = ventana emergente en cada tecla | Aviso debajo, sin ventana; el número de pedido con cualquier cantidad de dígitos |
| 17.7 | Buscadores que buscaban con 2 letras | Promociones (buscar artículo del combo), Publicar en Facebook (buscar producto), Reportes (buscar cliente) | La lista de resultados | Buscaba con 2 letras | Busca desde 3 (vacío limpia) |
| 17.8 | Ventana **🧩 Agregar artículos** | **Catálogo → Agregar modelo** (al guardar uno nuevo) | Ventana nueva | Solo "¡Producto guardado!" | Pregunta si quieres agregar sus artículos |
| 17.9 | (misma ventana) | **Catálogo → 🔍 Modelos → 🧩 Productos** | Ventana | "Inicializar variantes": N artículos iguales con stock 1 y sin talla | Un formulario por artículo, con talla, stock, foto… |
| 17.10 | `guardarConImagenes` acepta foto propia por artículo | **Catálogo → 🧩 Agregar producto** con varias tallas y una foto | Las fotos de los artículos | La foto la llevan todas las tallas | **Lo mismo que antes** |

### 17.1 Filtros que se combinan (Tienda → Buscar, escritorio, como admin)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Tienda → Buscar** → abre los filtros → marca **Con stock** | Todos los artículos con stock (habilitados y deshabilitados) |
| 2 | Marca también **Habilitadas** | Todos los que tienen stock **y** están habilitados. El del borde rojo ya no sale; los demás **sí** (antes quedaba 1) |
| 3 | Agrega un tercer filtro: **Talla** (elige una que tenga tu modelo) | Solo los de esa talla, con stock y habilitados |
| 4 | Quita **Talla** | Vuelve el resultado del paso 2 sin tocar nada más |
| 5 | Quita **Habilitadas** | Vuelve el resultado del paso 1 |
| 6 | Quita **Con stock** | Todo el catálogo, como al entrar |

❌ **Está mal si:** al quitar un filtro la lista no cambia, o con dos filtros sale menos de lo que cumple los dos.

### 17.2 Precio mín / máx

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En los filtros escribe **Precio mín 100** y **máx 100** (espera un segundo sin escribir) | Los artículos que **en la tarjeta** dicen $100, incluidos los que llegan a $100 con descuento |
| 2 | Cambia el máx a **99** | No sale ninguno de $100 |
| 3 | Borra los dos precios | Vuelve a todo |
| 4 | Cierra sesión (o abre la tienda en una ventana privada) y repite el paso 1 | Los mismos de $100 que tengan foto y stock (el catálogo de clientes solo muestra esos) |

❌ **Está mal si:** con 100 / 100 no sale un artículo cuya tarjeta dice $100.

### 17.3 Venta directa: el buscador se actualiza al vender

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Ventas → 💰 Venta directa** → en **🔍 Buscar producto** escribe 3 letras de un artículo con **stock 1** | El artículo con **1** de stock |
| 2 | Agrégalo, cobra de **💵 Contado** y cierra el aviso | La venta se registra |
| 3 | Mira la lista del buscador (sin escribir nada) | Ese artículo ya dice **0** o ya no sale. **No** sigue diciendo 1 |

### 17.4 Venta directa: nombres de 3 letras, correo y teléfono

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Agrega un artículo → **💰 Cobrar** → *"¿Agregar cliente para la rifa?"* → **Sí** | El formulario del cliente |
| 2 | En **Nombre** escribe **a** | Debajo, en rojo: *"Escribe al menos 3 letras"*. **Guardar cliente** apagado |
| 3 | Cambia a **Ana**; en **Apellido paterno** escribe **Lo** | Debajo de apellido: *"Si lo escribes, al menos 3 letras"*. Bórralo y el aviso se va (es opcional) |
| 4 | En **Correo** escribe **ana@gmail** | *"El correo no es válido (ejemplo: nombre@gmail.com)"*. Corrígelo a **ana@gmail.com** y se va |
| 5 | En **Número telefónico** escribe **55123** | *"El teléfono debe tener 10 dígitos"*. Con **55 1234 5678** se va |
| 6 | Cancela. En **📍 Datos de entrega → Nombre de quien recibe** escribe **Jo** | Debajo: *"Si lo escribes, al menos 3 letras"*, y junto a **💰 Cobrar**: *"Para cobrar, corrige el nombre de quien recibe…"*. **Cobrar** apagado |
| 7 | Cámbialo a **José** (o bórralo) | **Cobrar** se vuelve a prender |

❌ **Está mal si:** se guarda un cliente o un "quien recibe" de 1 o 2 letras.

### 17.5 Mis pedidos desde la cuenta del cliente

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Entra con una cuenta de **cliente** que tenga pedidos → **Mis pedidos** | Sus pedidos |
| 2 | En el buscador escribe el número de uno de sus pedidos, aunque sea de **1 dígito** | Ese pedido |
| 3 | Escribe letras, por ejemplo **abc** | Debajo del buscador: *"Escribe solo el número de tu pedido (por ejemplo 15)."* **Sin** ventana emergente |
| 4 | Borra todo | Vuelven todos sus pedidos |

### 17.6 Buscadores que ahora piden 3 letras

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Promociones** (pantalla de administrar promociones) → en el buscador de artículos del combo escribe **2 letras** | No busca. Con la 3.ª letra, sí |
| 2 | **Publicar en Facebook** → buscador del producto → 2 letras / 3 letras | Igual que el paso 1 |
| 3 | **Reportes** → buscador de cliente | El texto de ayuda dice *"Escribe al menos 3 letras…"* y busca desde la 3.ª |

### 17.7 Agregar modelo → "¿Quieres agregar sus artículos ahora?"

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → Agregar modelo** → llena nombre, precio, **stock 10**, color **Negro**, marca, descripción, categoría y una foto → **Guardar** | Ventana **"✅ Modelo guardado"** con *"¿Quieres agregar sus artículos ahora?"* y los botones **Después** / **Sí, agregar artículos** |
| 2 | Toca **Sí, agregar artículos** | Ventana **🧩 Agregar artículos**: *"Stock del modelo: 10 · Libre: 10"*; la sección **Usar del modelo en todos los artículos** con una casilla **marcada** por cada dato que llenaste (Color: Negro, Marca…, Descripción…, Categoría…, **Foto del modelo**). Lo que no llenaste **no** aparece |
| 3 | En **¿Cuántos artículos?** escribe **2** | Dos formularios: **Artículo 1** y **Artículo 2**, con color, marca y descripción ya llenos. Talla y stock vacíos. En **Foto**: *"📷 La foto del modelo"* |
| 4 | En **Mismo stock para todos** escribe **4** → **Aplicar** | Los dos con stock 4. Abajo: *"Stock del modelo: 10 · Repartido: 8 · Te quedan 2"* |
| 5 | En el **Artículo 2** cambia el stock a **8** | Abajo en rojo: *"Te pasaste por 2"*. **Guardar 2 artículos** apagado |
| 6 | Regrésalo a **4**. Tallas: **CH** y **M**. En el Artículo 2 cambia el color a **Blanco** | El Artículo 1 sigue en Negro |
| 7 | Desmarca **Color** arriba | El color **Negro** se borra del Artículo 1; **Blanco** del Artículo 2 se queda (lo cambiaste tú) |
| 8 | Vuelve a marcar **Color**. En el Artículo 2 toca **Usar otra foto** y elige una foto | Se ve la miniatura en el Artículo 2 y los botones **Cambiar foto** / **Quitar** |
| 9 | Toca **Guardar 2 artículos** | *"2 artículos guardados"* → **Entendido** y se cierra |
| 10 | **Tienda** → busca el modelo | Dos artículos: **CH Negro** con la foto del modelo y **M Blanco** con su foto propia. Stock 4 cada uno |

❌ **Está mal si:** se guarda uno solo de los dos, el Artículo 1 sale con la foto del 2, o se puede guardar con *"Te pasaste"*.

### 17.8 "Después" y la tarjeta 🧩 Productos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Da de alta otro modelo (stock 5) → en la pregunta toca **Después** | Se cierra. El modelo **sí** quedó guardado (búscalo en **Catálogo → 🔍 Modelos**) y no tiene artículos |
| 2 | En su tarjeta toca **🧩 Productos** | La misma ventana **🧩 Agregar artículos**, **sin** la pregunta, con *"Libre: 5"* |
| 3 | Pide **1** artículo con stock **5**, talla **U** → **Guardar 1 artículo** | *"1 artículo guardado"*; la lista de modelos se recarga |
| 4 | Vuelve a tocar **🧩 Productos** en ese modelo | El aviso de siempre: *"No queda stock para artículos nuevos"* (ya repartiste los 5) |
| 5 | En el modelo del 17.7 toca **🧩 Productos** | *"En sus artículos: 8 · Libre: 2"* |

### 17.9 Lo que no debe cambiar: Agregar producto con varias tallas

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Catálogo → 🧩 Agregar producto** → elige un modelo con stock libre → llena el formulario de arriba, sube **una** foto y agrega **2 tallas** abajo → guarda | **Lo mismo que antes:** se guardan los artículos y **todos** llevan la foto que subiste |

### 17.10 Color nuevo en Personalización (solo si corriste `migration_tema_modal.sql`)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Sistema → Personalización → Página** | Fila nueva **"Velo detrás de una ventana abierta"** |
| 2 | Cámbiale el color de día, guarda y abre la ventana de 🧩 Productos | El fondo detrás de la ventana usa el color nuevo |

💬 Notas:

- [ ] 17.1 · [ ] 17.2 · [ ] 17.3 · [ ] 17.4 · [ ] 17.5 · [ ] 17.6 · [ ] 17.7 · [ ] 17.8 · [ ] 17.9 · [ ] 17.10 — **Prueba 17 terminada**

---

## Prueba 18 — Agregar artículo: todos los modelos, habilitar y agregar stock (2026-10-08)

**Para qué es:** lo que pediste el 2026-10-08: en **Catálogo → 🧩 Agregar producto** (la pantalla de
agregar artículo) buscar **todos** los modelos, también los sin stock, deshabilitados o dados de baja; al
elegir uno, que diga en qué estado está, dejar **habilitarlo**, decir **cuántos artículos más se pueden
hacer**, y que al **agregarle stock** se vaya al modelo y pregunte *"¿Deseas agregar los artículos de una vez?"*.

**Antes de empezar:** ⏳ espera mi aviso de que subió a QA y haz `Ctrl + Shift + R`. Ten a la mano:
- **M-OFF**: un modelo **deshabilitado** (en Catálogo → 🔍 Modelos, con el interruptor apagado).
- **M-0**: un modelo **habilitado** cuyo stock ya está todo repartido en sus artículos (Libre 0).
- Para el paso con otro rol: `migration_accion_tienda_venta_ver_todos.sql` ✅ ya está corrida en QA (2026-10-08); en
  **Gestión de roles**, márcale a ese rol **"Ver todos los modelos (sin stock, deshabilitados o dados de baja)"** en 🧩 Agregar producto.

### Mapa de impacto

| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 18.1 | Buscador de modelos con `todos=true` | 🧩 Agregar producto (admin o con el permiso) | Lista del buscador | Un rol que no es admin no veía los sin stock ni deshabilitados | Salen todos, con etiqueta ⛔ Deshabilitado / Sin stock / Sin foto |
| 18.2 | (mismo endpoint, sin `todos`) | **Tienda**, Modelos y cualquier otro buscador de modelos | Sus listas | Lo de siempre | **Lo mismo que antes** |
| 18.3 | La disponibilidad se lee bien | 🧩 Agregar producto → recuadro de stock | Recuadro verde | **No aparecía nunca** (el front leía mal la respuesta) | Aparece: total, repartido, libre y "Puedes hacer hasta N artículos más" |
| 18.4 | **Guardar en el modelo** (al momento) | 🧩 Agregar producto | Botón junto a "Agregar (+) o quitar (−)" | El ajuste solo se guardaba junto con el artículo | Se guarda en el modelo al tocarlo; luego pregunta por los artículos |
| 18.5 | ✅ Habilitar modelo | 🧩 Agregar producto | Aviso rojo | No había forma desde aquí | Habilita el modelo (sus artículos quedan como estaban) |
| 18.6 | Ajuste dentro del guardado del artículo | 🧩 Agregar producto → 💾 Guardar | Stock del modelo | Funcionaba | **Lo mismo que antes** |

### 18.1 Ver todos los modelos

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Como **admin**: **Catálogo → 🧩 Agregar producto** → en el buscador escribe 3 letras de **M-OFF** | Sale **M-OFF** con la etiqueta **⛔ Deshabilitado** debajo del nombre (y **Sin stock** / **Sin foto** si aplica) |
| 2 | Busca **M-0** | Sale sin etiqueta de deshabilitado |
| 3 | Entra con el **rol con el permiso** y busca **M-OFF** | También sale, con sus etiquetas |
| 4 | Quítale el permiso al rol en Gestión de roles, vuelve a entrar y busca **M-OFF** | **Ya no sale** (como antes) |

### 18.2 Modelo deshabilitado → Habilitar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Elige **M-OFF** | Aviso rojo **"⛔ Este modelo está deshabilitado"** con el botón **✅ Habilitar modelo**. El botón **💾 Guardar** está apagado y **Guardar en el modelo** también |
| 2 | Toca **✅ Habilitar modelo** | Pregunta *"¿Habilitar "M-OFF"?"* explicando que sus artículos dados de baja siguen de baja → **Sí, habilitar** |
| 3 | — | **"Modelo habilitado"** con *"Stock del modelo: N. Tiene X artículos con Y piezas. Puedes hacer hasta Z artículos más"* (o *"No le queda stock libre…"*). Se queda hasta **Entendido** |
| 4 | Mira la pantalla | El aviso rojo ya no está; se ve el recuadro de stock y **💾 Guardar** se prende |
| 5 | **Catálogo → 🔍 Modelos** → busca **M-OFF** | Ya está habilitado |
| 6 | Con un rol **sin** el permiso Habilitar de Modelos, elige un modelo deshabilitado | En vez del botón: *"Pídele a alguien con permiso de Habilitar…"* |

### 18.3 Sin stock libre → Guardar en el modelo → artículos de una vez

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | Elige **M-0** | Recuadro de stock: *"Stock total del modelo"*, *"Repartido · Libre 0"* y *"No le queda stock libre: agrégale stock al modelo para hacer artículos nuevos."* |
| 2 | En **Agregar (+) o quitar (−) al modelo** escribe **5** | Abajo: *"El modelo quedaría en N+5"*. Se prende **Guardar en el modelo** |
| 3 | Toca **Guardar en el modelo** | **"Se agregaron 5 al modelo"** con el resumen (*"Puedes hacer hasta 5 artículos más"*) y la pregunta **"¿Deseas agregar los artículos de una vez?"** → **Sí, agregar artículos** / **Después** |
| 4 | Toca **Sí, agregar artículos** | Se abre la ventana **🧩 Agregar artículos** con *"Libre: 5"* (la misma de la Prueba 17.7) |
| 5 | Pide **2** artículos con stock **2** cada uno → **Guardar 2 artículos** | *"2 artículos guardados"*. Al cerrar, el recuadro dice *"Puedes hacer hasta 1 artículo más"* |
| 6 | Escribe **−1** → **Guardar en el modelo** | *"Se quitaron 1 al modelo"*; Libre 0 |
| 7 | Escribe **−1** otra vez | Aviso rojo *"No se puede dejar el modelo en …: ya tiene … repartidos"* y el botón apagado |
| 8 | Con libre > 0, toca **🧩 Agregar varios artículos de una vez** | La misma ventana, sin la pregunta |

### 18.4 Lo que no debe cambiar

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | En un modelo con libre 0, escribe **+3** en el campo **sin** tocar *Guardar en el modelo*, llena un artículo con stock **3** y toca **💾 Guardar** | **Lo mismo que antes:** se guarda el artículo y el modelo sube 3 (en una sola operación) |
| 2 | **Tienda → Buscar** y **Catálogo → 🔍 Modelos** | Igual que antes: lo que veías antes es lo que ves ahora |
| 3 | Sin sesión, busca en la tienda un modelo deshabilitado | No sale (el permiso nuevo no aplica sin sesión) |

❌ **Está mal si:** un rol sin el permiso ve modelos deshabilitados, se puede guardar un artículo de un modelo
deshabilitado, el stock agregado se reparte solo a algún artículo, o el recuadro de stock no aparece.

💬 Notas:

- [ ] 18.1 · [ ] 18.2 · [ ] 18.3 · [ ] 18.4 — **Prueba 18 terminada**

---

## Prueba 6 — Datos de prueba con un botón (la última)

**Para qué es:** crear de un jalón miles de modelos, artículos y pedidos de prueba para no darlos de
alta a mano. Todo queda marcado como **"Prueba QA"** para distinguirlo de lo real.

**Antes de empezar:** termina **todas** las demás de este documento. Mientras esté generando, **no me pidas subir nada a
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
- **"¿Viene bien o dañado?"** al cancelar o devolver y **registrar que el dinero ya se devolvió**: acordados, sin programar.
