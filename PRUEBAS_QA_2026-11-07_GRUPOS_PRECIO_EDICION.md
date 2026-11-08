# Pruebas de QA — Grupos de precio por cantidad al EDITAR un pedido (Fase 1b)
Fecha: 2026-11-07 · Reglas: G6 y G8 de `PLAN_CAMPANAS_Y_PRECIOS.md` · Contrato: `CAMBIOS_FRONT.md` («Fase 1b»)

## Antes de empezar
1. Correr `migration_precio_base_linea_grupo.sql` en QA (se puede correr dos veces). Sin ella todo funciona como en la Fase 1 y **al editar no se recalcula**.
2. Tener el grupo de la Fase 1 (**mínimo 3**, productos A, B y C con precio de promoción) y los mismos productos de ejemplo: A $250 → $200, B $300 → $270, C $180 → $150.
3. Para ver el antes y el después, usar **Pedidos 📋 → Mis pedidos** con un pedido **Apartado** o **Ir pagando** (los ya pagados y entregados no se editan).

## Mapa de impacto
| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 1 | Recalculo tras editar | Agregar artículo a un pedido | Mis pedidos → detalle → ➕ Agregar artículo | El pedido conservaba los precios de cuando se hizo | Se recalcula con lo que lleva el pedido ahora |
| 2 | Recalculo tras editar | Cambiar artículo ⇄ | Mis pedidos → detalle | Igual | Se recalcula |
| 3 | Recalculo tras editar | Quitar piezas / quitar artículo | Mis pedidos → detalle | Igual | Se recalcula |
| 4 | Recalculo tras editar | Quitar una promoción (combo) | Mis pedidos → detalle | Igual | Se recalcula |
| 5 | Estado de pago | Abonos | Mis pedidos → Pagado / Saldo | Según el total | Según el **total nuevo** |
| 6 | Sin grupo | Cualquier pedido sin grupos | — | Igual | **Debe dar exactamente lo mismo que antes** |

## Pruebas
**E0 — Sin grupo: lo mismo que antes.** Edita un pedido de productos que **no** están en ningún grupo (agregar, quitar, cambiar). **✅** los precios y el total son los de siempre. **⛔** que cambie un precio.

**E1 — Agregar la tercera pieza.** Apartado con 1 de A ($250) y 1 de B ($300): total $550. Agrega 1 de C ($180). **✅** A pasa a $200, B a $270, C a $150 y el total queda **$620**. **⛔** que las dos primeras se queden en $250 y $300.

**E2 — Quitar una pieza: regresa al precio de antes.** Pedido con A, B y C a precio de grupo (total $620). Quita la pieza de C. **✅** A regresa a **$250** y B a **$300**, total **$550**. **⛔** que A y B se queden en $200 y $270.

**E3 — Quitar solo una de dos piezas del mismo producto.** Pedido con 3 piezas de A ($200 c/u, total $600). Quita 1 pieza. **✅** quedan 2, que ya no juntan el mínimo de 3: A **$250 c/u**, total **$500**.

**E4 — Agregar otra pieza del mismo artículo.** Pedido con A, B y C a precio de grupo. Agrega 1 pieza de A (la misma que ya está). **✅** queda **una sola línea** de A con cantidad 2 a $200, no dos líneas. Total $200×2 + $270 + $150 = **$820**.

**E5 — Cambiar un artículo.** Pedido con A, B y C a precio de grupo. Cambia C por D (que **no** es del grupo, $100) con ⇄. **✅** quedan 2 piezas del grupo: A vuelve a $250 y B a $300; D a su precio normal $100. **⛔** que D salga con el precio de C.

**E6 — Abonos.** Pedido Ir pagando con A, B y C a precio de grupo (total $620) y un abono de $400. Quita C. **✅** el total pasa a **$550** y el saldo a **$150**; **⛔** que el abono cambie. Si el abono cubre el total nuevo, el pedido queda **Pagado** y lo que sobre es saldo a favor.

**E7 — Un combo no se toca.** Pedido con una promoción (combo) y piezas de un grupo. **✅** las líneas del combo conservan su precio promocional al editar.

**E8 — Una rebaja menor al precio de grupo se respeta.** Si una línea tiene un precio con descuento más bajo que el de grupo, no sube. **⛔** que se cobre el de grupo si es más caro.

**E9 — Cancelar la tarjeta.** Un pedido con cobro con tarjeta en proceso no se puede editar (como antes).

## Lo que NO funciona todavía (no es error)
- Un pedido hecho **antes** de correr la migración, con precio de grupo, no recuerda su precio anterior: al editarlo se queda con el que tiene.
- «Transferir abono» crea un pedido nuevo de una sola línea y no calcula precio de grupo.
