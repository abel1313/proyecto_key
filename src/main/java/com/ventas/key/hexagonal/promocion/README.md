# Dominio: promocion

Reglas del dominio. **Se acuerdan antes de escribir código** (ver
`hexagonal/_plantilla/README.md`, PASO 0).

Estado: reglas a confirmar — ver las dudas al final.

---

## El problema (reportado 2026-09-22)

> *"Cuando genero una promoción pasa directo a pago en efectivo."*

Confirmado en el código: las promociones se resuelven dentro de
`VentaServiceImpl` (línea 192 en adelante), y una **venta** es cobro inmediato. El flujo
de **pedido** (`PedidoServiceImpl`), que es el que permite apartado y abonos, no toca
promociones.

Por eso una promoción no puede apartarse: nunca pasa por el flujo que lo permite.

---

## Lo que se quiere

Una promoción debe poder cerrarse **igual que cualquier pedido normal**:

| | Hoy | Debe poder |
|---|---|---|
| Venta de contado | ✅ | ✅ |
| **Apartado** | ❌ | ✅ |
| Forma de pago efectivo | ✅ (forzado) | ✅ (elegido) |
| Forma de pago tarjeta | ❌ | ✅ |
| Abonos parciales | ❌ | ✅ |

---

## Reglas propuestas

### R1 — La promoción no decide la forma de cobro
Una promoción define **qué se lleva y a qué precio**. Cómo se paga (contado o apartado) y
con qué (efectivo o tarjeta) son decisiones del pedido, no de la promoción.

### R2 — El precio promocional se congela al crear el pedido
Si el cliente aparta hoy con una promoción vigente, paga el precio de hoy aunque liquide
la semana que viene. Si no, un apartado largo podría encarecerse solo.

### R3 — El total lo calcula el back
El front manda qué promoción y qué artículos; el back busca la promoción, verifica que
esté vigente, aplica el descuento y calcula el total. Nunca se confía en el monto que
manda el front. (Misma regla R5 del dominio `stock`.)

### R4 — Una promoción vencida no se puede usar para un pedido nuevo
Vigencia validada contra la fecha del servidor, no la del cliente.

---

## ⚠️ Dudas antes de modelar

**Duda 1 — ¿Cuándo se descuenta el stock en un apartado con promoción?**
- (a) Al apartar → el artículo queda reservado, nadie más lo compra
- (b) Al liquidar → el artículo sigue a la venta hasta que se pague

Cambia qué pasa si el apartado se cancela, y si se puede apartar algo sin stock.

**Duda 2 — Apartado con promoción que vence antes de liquidarse**
Por R2 el precio queda congelado. Pero si el cliente nunca liquida y se cancela,
¿el artículo vuelve al catálogo al precio normal o sigue en promoción?

**Duda 3 — ¿Un pedido puede mezclar artículos con y sin promoción?**
Hoy `VentaServiceImpl` :195 exige que las líneas sin `promocionId` vayan a precio de
catálogo, así que la mezcla ya está contemplada en venta. Confirmar que en pedido sea
igual.
