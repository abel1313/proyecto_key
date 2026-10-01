---
name: reglas-pedidos
description: Reglas del negocio de Novedades Jade para ventas, pedidos, formas de cobro (contado, Apartado, Ir pagando), abonos, tarjeta y meses sin intereses, pedidos unidos (unir, abonar al grupo, separar) y cómo trabajar y contestar al dueño. Usar SIEMPRE antes de tocar código, contestar dudas, escribir planes o pruebas de QA de pedidos, ventas, carrito, cobro, abonos o grupos de pedidos.
---

# Reglas de pedidos y cobro — Novedades Jade

Estas reglas las dio el dueño y ya no se vuelven a preguntar. Si algo de aquí choca con el código,
**gana la regla**, pero no se cambia el código sin avisar: se anota la diferencia y se dice.

Leyenda: ✅ así funciona hoy · 🔮 para después (no está programado) · ❓ falta que el dueño confirme

---

## 1. Cómo trabajar con el dueño

1. **Nombres reales, nunca adivinados.** Antes de escribir el nombre de un menú, botón, pantalla o
   mensaje, seguir la skill **`nombres-reales`** (dónde vive cada nombre: el menú está en la base,
   los botones en el HTML del front). Si no se encontró, decirlo; no inventar uno parecido.
2. **Hablar con sus palabras.** Decir **Apartado** e **Ir pagando**, no "a crédito" (en el código
   `APARTADO` y `FIADO` se llaman "crédito", pero para el dueño esa palabra no significa nada).
   `FIADO` en el código = **Ir pagando** en pantalla. Decir **artículo**, no "variante".
3. **No volver a preguntar lo que ya está aquí** ni en los documentos de la sección 6. Leerlos antes.
4. **Regla nueva → se anota aquí en el mismo cambio**, con la fecha. Si contradice una anterior, no
   se pisa: se marca ❓ con las dos versiones y se le pregunta al dueño cuál gana.
5. **Comentarios de pruebas:** el dueño escribe debajo de cada paso. Su comentario se copia tal cual
   (`💬`) y la respuesta va justo abajo (`↳`). No se repite lo que ya está en otro documento: se
   pone la referencia.
6. **Antes de decir "ya quedó" en QA**, confirmar que el deploy de QA terminó con ese commit
   (GitHub Actions). Dos push seguidos a `qa` del front lanzan dos deploys y el último en terminar
   gana, aunque sea el viejo (pasó el 2026-10-01).

---

## 2. Formas de cobro

| Forma | Cómo se paga | Cuándo se entrega | Estado |
|---|---|---|---|
| **Contado, efectivo** (en el local) | Completo en ese momento | Ahí mismo: el cliente está en el local y se lo lleva. El pedido queda terminado | ✅ |
| **Contado, tarjeta** (en el local, terminal) | Tarjeta de débito o crédito, de una exhibición o a meses sin intereses | Ahí mismo, igual que efectivo | ✅ tarjeta · MSI ver 3 |
| **Pago en línea** (el cliente paga con tarjeta desde su cuenta en la tienda) | Completo, con tarjeta, al hacer el pedido | **No** se entrega en ese momento: el cliente pone **a dónde** y **cuándo** se lo llevan, y la tienda se lo lleva | 🔮 |
| **Apartado** | **Sin dinero.** Es el pedido que el cliente hace por Facebook, un live o un mensaje: solo lo pide, no ha dado nada. Se paga **completo** al recogerlo | Cuando va al local a recogerlo | ✅ desde 2026-10-01 (ver 2.1) |
| **Ir pagando** | Abonos en efectivo o transferencia. **Nunca tarjeta ni MSI**. Los abonos **no caducan**. **Cualquier pedido en el que el cliente ya dio algo de dinero es Ir pagando** (enganche, transferencia, o un familiar que trajo $100) | El cliente se lleva la mercancía y va pagando | ✅ |
| **Apartado, al recogerlo con tarjeta** | Al ir al local a recogerlo, poder pagar con tarjeta y, si cumple 3, a MSI | Al recogerlo | 🔮 |

- Hoy el cliente **no puede pagar con tarjeta en la tienda en línea**: lo que pide por su cuenta queda
  como pedido sin pagar (Apartado). El pago en línea es 🔮 (fila de arriba).
- El botón **Cobrar** de la card no cobra un Apartado ni un Ir pagando: manda a Créditos / Abonos. ✅
- **Un abono es uno solo, se dé donde se dé:** "💳 Registrar abono" en el detalle del pedido, la
  pantalla Créditos / Abonos y el "Pago inicial (enganche)" de la venta usan el mismo registro
  (`POST /v1/abonos/{pedidoId}`). No hay abonos "de primera" y "de segunda". ✅
- Cuando lo pagado cubre el total, el pedido pasa solo a **PAGADO** y se crea su venta. ✅
- **Ir pagando nunca tiene MSI**, aunque el pedido cumpla las reglas de 3.

### 2.1 Apartado = sin dinero (decidido 2026-10-01)

Palabras del dueño: *"El apartado es porque lo piden por Face, live o mensaje, y ahí no me ha dado
nada, solo lo pide. Si al hacer el pedido me hace una transferencia o manda a la prima, la tía, a
dejarme $100, lo tendría que poner en Ir pagando porque ya me dio un enganche, y en Apartado no
podría entrar."*

| Situación | Qué hace el sistema |
|---|---|
| Venta: se elige **Apartado** y se escribe un **Pago inicial (enganche)** | Pasa solo a **Ir pagando** y una nota explica por qué: un Apartado es sin dinero (ver 2.3; reemplaza el aviso que no dejaba seguir) |
| Apartado ya hecho y el cliente da **menos** que el total (él, o un familiar en su nombre) | No se registra el abono en el Apartado: el sistema dice *"Para dar un abono, cambia el pedido a Ir pagando"*. Se cambia con 🔁 Cambiar forma de cobro y ya se registra el abono |
| Apartado y el cliente paga **el total** (se escribe en "Registrar abono", en el detalle o en Créditos / Abonos) | Se acepta: es el pago completo, queda pagado. Si paga en efectivo con un billete mayor, el cambio sale de "Monto recibido" (ver 2.2) |
| Cambiar un pedido **a Apartado** escribiendo algo en "¿Cobra algo ahora?", o un Ir pagando que ya tiene abonos | No se permite: tiene dinero, es Ir pagando |
| Apartados unidos | El botón dice **💵 Pagar el grupo completo** y solo acepta el saldo completo del grupo |
| El cliente recoge el Apartado | Paga **todo** en ese momento, en el formulario de abono de siempre (detalle o Créditos / Abonos). El botón **Cobrar** de la card sigue mandando a Créditos / Abonos. Decidido 2026-10-01: "dejarlo como está" (reemplaza la idea de que Cobrar cobrara todo) |
| Apartados unidos y vienen a recogerlos | Se liquida el total del grupo y queda pagado y entregado. Si solo deja un adelanto, el **grupo entero** pasa a Ir pagando (todos tienen la misma forma de cobro) — hoy eso obliga a separar, cambiar cada uno y volver a unir (🆕 `PLAN` §10.6 A10) |

Estado: ✅ **programado en `dev` y `qa` el 2026-10-01** (A1–A6 de `PLAN` §10.6; falta A7, el script
de QA). El back rechaza el abono parcial a un Apartado (pedido y grupo) y pasar a Apartado con
dinero; el front avisa y ofrece cambiar a Ir pagando. Si el dueño pide "deja dar un abono en un
Apartado", **recordarle esta regla**: para dar un abono, el pedido se cambia a Ir pagando.

### 2.2 Abono, billete y cambio (decidido 2026-10-01)

- **El abono es lo que cuenta para la deuda; el "Monto recibido" es el billete que te dio.** El cambio
  sale de la resta. Ejemplos: debe $100, quiere dar $20 y paga con uno de $500 → abono $20, monto
  recibido $500 → **cambio $480**. Debe $100, lo liquida con uno de $500 → abono $100, recibido $500
  → **cambio $400**. ✅ Así funciona hoy en el detalle y en Créditos / Abonos (solo en efectivo).
- Un abono **nunca** puede ser mayor a lo que se debe (se rechaza: *"El monto excede el saldo
  pendiente"*). ✅ Se queda así.
- **Transferencia = monto exacto.** No hay cambio en transferencia: si transfirió de más, fue un
  error del cliente y no se registra así. ✅ (el "Monto recibido" solo sale en efectivo)

### 2.3 Venta: lo que paga decide, y "¿ya se lo llevó?" (decidido 2026-10-01, 🆕 sin programar)

Ejemplo con un pedido de $200. Casos de prueba completos: `PRUEBAS_QA_2026-10-01.md`, Prueba 6.

| Lo que te dio | Queda como | Botón(es) para terminar la venta |
|---|---|---|
| $0 | **Apartado** | Uno solo (guardar), como hoy. **Nunca** salen los dos botones |
| Menos de $200 | **Ir pagando** | Dos: **✅ Ya se lo llevó** · **📦 Todavía no se lo lleva** |
| $200 (efectivo, transferencia o tarjeta) | **Pagado** | Dos: **✅ Ya se lo llevó** (Entregado) · **📦 Falta entregarlo** (Pagado · falta entregar) |

- El tipo se decide solo mientras se escribe el monto, y la pantalla dice **"Quedará como: …"**. Si
  se había elegido Apartado y se escribe dinero, cambia a Ir pagando con una nota que explica por qué
  (esto reemplaza el aviso que bloqueaba, de 2.1).
- Ninguno de los dos botones viene marcado: siempre hay que elegir.
- Antes de guardar, resumen: *"Cobrado: $200 por transferencia · Falta entregarlo"*.
- Efectivo de más: el monto es el total y el billete va en "Monto recibido" (cambio). Transferencia
  de más: no se deja.
- **Ir pagando "todavía no se lo lleva":** la mercancía sigue en la tienda; si se cancela, el stock
  **sí** regresa. "Ya se lo llevó": no regresa (como hoy). Los Ir pagando que ya existen cuentan
  como "ya se lo llevó".
- **Pagado · falta entregar:** etiqueta en la card, botón **Entregar** (no cobra), filtro
  **"Por entregar"**, "⚠ Atrasado N días" si tiene fecha y ya pasó, y **nunca se cancela solo**.

**Si se equivocan al guardar:**
| Error | Cómo se corrige |
|---|---|
| "Falta entregarlo" pero no había pagado nada | 🔁 Cambiar forma de cobro → Apartado (se anula el cobro y se borra la venta) |
| "Falta entregarlo" pero pagó solo una parte | 🔁 Cambiar forma de cobro → Ir pagando, y en "¿Cobra algo ahora?" lo que dio de verdad |
| "Falta entregarlo" pero sí se lo llevó | **Entregar** en la card |
| "Ya se lo llevó" pero no se lo llevó | Botón para regresarlo a "Falta entregarlo", **solo administrador** |

## 3. Meses sin intereses (MSI)

| Regla | Estado |
|---|---|
| Solo en el local, pagando con tarjeta en la terminal; se elige el plazo al cobrar | ✅ (hoy aplica a toda la venta de contado; no hay casilla por artículo ni mínimo) |
| Mínimo de compra **$300** para ofrecer MSI (configurable) | 🔮 decidido, sin programar |
| Casilla **"Acepta meses sin intereses"** por producto (aplica a todos sus artículos) y por artículo, **apagada** por defecto | 🔮 decidido, sin programar |
| Solo se ofrece MSI si **todos** los artículos de la venta lo aceptan; si no, aviso "Este artículo no acepta meses sin intereses" | 🔮 decidido, sin programar |
| Pantalla para activar / desactivar planes (3, 6, 9, 12) con su comisión | 🔮 decidido, sin programar |
| Apartado al recogerlo: MSI si cumple lo anterior | 🔮 |
| Ir pagando: **nunca** MSI | regla |

Detalle: `PLAN_PEDIDOS_VENTAS_ENTREGA.md` 6.8, 7.2, 7.3 (Bloque 6).

---

## 4. Pedidos unidos (grupo)

Reglas técnicas completas (R1–R18): `src/main/java/com/ventas/key/hexagonal/grupopedido/README.md`.

| Regla | Estado |
|---|---|
| Solo se unen pedidos con la **misma forma de cobro**. Un Apartado y un Ir pagando **no** se unen | ✅ |
| Para unirlos, primero se cambia uno con **🔁 Cambiar forma de cobro** en su detalle (y luego se unen). Dentro de un grupo ya no se puede cambiar la forma de cobro | ✅ |
| Elegir en la pantalla de unir "Pasar todos a … y unir" | 🔮 (`PLAN` 6.7) |
| **Ir pagando unido:** los abonos son **del grupo**, de cualquier monto, y no caducan | ✅ (ver nota) |
| **Al separar** se decide cuánto se queda cada pedido; tiene que sumar exacto lo que dio el cliente | ✅ |
| **➕ Agregar artículo** pregunta "¿A qué pedido lo agregas?" | ✅ |
| **⇄** (cambiar artículo) se queda en el pedido de ese artículo | ✅ |
| Un solo botón de abonar en un pedido unido (**💵 Abonar al grupo**); esconder **💳 Registrar abono** | 🔮 pedido por el dueño, sin hacer |
| Tabla del grupo: cada pedido muestra **solo su total**; abajo, **Pagado** y **Saldo del grupo**. Se quitan "Pagado" y "Saldo" por pedido (el dinero es del grupo; lo de cada uno se ve al separar) | 🆕 decidido 2026-10-01 |
| Botón **"Detalle de los pagos"** en grupos de **Ir pagando**: lista cada pago del grupo **una sola vez y completo** (monto, fecha, forma de pago, nota), aunque por dentro se haya anotado partido entre pedidos | 🆕 decidido 2026-10-01 |

**Ejemplo acordado (Ir pagando):** 2 pedidos de $200 (total $400). Da $100 para los dos → deben $300.
Al separar: 50 y 50 → cada uno debe $150. Los 100 a uno solo → uno debe $100 y el otro $200.

**Nota:** por dentro, un abono al grupo se anota llenando primero el pedido más viejo (R6). Para el
dueño eso no importa: el dinero es del grupo y al separar se reparte como él diga (R14). En pantalla
no hay que presentarlo como si el abono fuera de un pedido.

---

## 5. Quitar, cambiar y cancelar

Ver `PENDIENTES_2026-09-29.md` B y `PLAN` D1–D5. En corto:
- La cuenta es **solo de ese pedido**; si lo pagado cubre el nuevo total queda Pagado; no se puede
  quitar el último artículo (se cancela el pedido). ✅
- **Al cancelar o regresar productos, por cada producto: "¿Viene bien o dañado?"** Bien → regresa al
  stock. Dañado → **no** regresa y queda la nota "regresó dañado". Aplica también a Ir pagando si la
  mercancía regresa. 🆕 (`PLAN` D1–D3)
- **Al cancelar, siempre decir cuánto hay que devolverle al cliente** ("Saldo a favor: $X"), se
  cancele desde la card de Mis pedidos o desde Créditos / Abonos. 🆕 (hoy solo lo dice Créditos /
  Abonos)
- Registrar que el dinero ya se devolvió (para el corte): después (`PLAN` D5).

---

## 6. Dónde está cada cosa

- Skills relacionadas: `nombres-reales` (nombres de pantallas), `diseno-componentes` (pantallas nuevas), `arquitectura-hexagonal-limpia` (código nuevo del back), `renombrar` (cambiar un término en todo el proyecto).

- `PLAN_PEDIDOS_VENTAS_ENTREGA.md` — plan de ventas, pedidos, entrega y MSI (decisiones del dueño).
- `PENDIENTES_2026-09-29.md` — pendientes por tema (A–E).
- `PRUEBAS_QA_<fecha>.md` — pruebas en QA con los comentarios del dueño y las respuestas.
- `hexagonal/grupopedido/README.md` — reglas R1–R18 de pedidos unidos.
