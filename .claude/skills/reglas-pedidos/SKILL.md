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

1. **Nombres reales, nunca adivinados.** Antes de escribir el nombre de un botón, pantalla o mensaje
   en una respuesta o en un plan de pruebas, buscarlo en el código del front (`producto_venta_online`)
   y copiarlo tal cual. Si no se encontró, decirlo; no inventar uno parecido.
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
| **Apartado** | Solo efectivo o transferencia. **Nunca tarjeta ni MSI** (hoy) | Cuando va al local a recogerlo | ✅ (ver ❓ 4.1) |
| **Ir pagando** | Abonos en efectivo o transferencia. **Nunca tarjeta ni MSI**. Los abonos **no caducan** | El cliente se lleva la mercancía y va pagando | ✅ |
| **Apartado, al recogerlo con tarjeta** | Al ir al local a recogerlo, poder pagar con tarjeta y, si cumple 3, a MSI | Al recogerlo | 🔮 |

- El botón **Cobrar** de la card no cobra un Apartado ni un Ir pagando: manda a Créditos / Abonos. ✅
- Cuando lo pagado cubre el total, el pedido pasa solo a **PAGADO** y se crea su venta. ✅
- **Ir pagando nunca tiene MSI**, aunque el pedido cumpla las reglas de 3.

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

**Ejemplo acordado (Ir pagando):** 2 pedidos de $200 (total $400). Da $100 para los dos → deben $300.
Al separar: 50 y 50 → cada uno debe $150. Los 100 a uno solo → uno debe $100 y el otro $200.

**Nota:** por dentro, un abono al grupo se anota llenando primero el pedido más viejo (R6). Para el
dueño eso no importa: el dinero es del grupo y al separar se reparte como él diga (R14). En pantalla
no hay que presentarlo como si el abono fuera de un pedido.

### ❓ 4.1 Apartado: ¿acepta anticipo / abonos? — falta confirmar

- 2026-09-29 (`PLAN` §1): *"Apartado: lo deja encargado. Si deja anticipo, se anota (efectivo o
  transferencia)."*
- 2026-10-01 (dueño): *"Si es apartado, cuando lo va a recoger es porque lo va a pagar por completo,
  porque si va a estar dando abonos eso sería Ir pagando. Si unimos 2 pedidos [apartados], aquí no
  aplican abonos."*
- Código hoy: Apartado acepta **enganche** al crearlo y **abonos** después (y "Abonar al grupo").

No cambiar nada hasta que el dueño diga cuál de las dos queda.

---

## 5. Quitar, cambiar y cancelar

Ver `PENDIENTES_2026-09-29.md` B. En corto: la cuenta es **solo de ese pedido**; si lo pagado cubre
el nuevo total queda Pagado; no se puede quitar el último artículo (se cancela el pedido); cancelar
un Apartado regresa el stock y deja saldo a favor; cancelar un Ir pagando **no** regresa el stock.

---

## 6. Dónde está cada cosa

- `PLAN_PEDIDOS_VENTAS_ENTREGA.md` — plan de ventas, pedidos, entrega y MSI (decisiones del dueño).
- `PENDIENTES_2026-09-29.md` — pendientes por tema (A–E).
- `PRUEBAS_QA_<fecha>.md` — pruebas en QA con los comentarios del dueño y las respuestas.
- `hexagonal/grupopedido/README.md` — reglas R1–R18 de pedidos unidos.
