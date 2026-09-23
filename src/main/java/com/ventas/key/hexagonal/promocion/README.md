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

---

## Causa exacta encontrada (2026-09-22)

No es que la promoción "pase directo a efectivo". **El back la rechaza explícitamente.**

`PromocionServiceImpl.validarLineasPromocion()` línea 140, lo primero que hace:

```java
if (tipoPedido != null && !"NORMAL".equalsIgnoreCase(tipoPedido)) {
    throw new RuntimeException(
        "Las promociones solo se pueden comprar de contado, no se pueden apartar ni dar a credito");
}
```

Cualquier `tipoPedido` que no sea `NORMAL` (o sea: APARTADO, CREDITO) revienta ahí mismo,
antes de mirar nada más. Por eso la única salida que queda es contado.

**Lo que hay que hacer** es quitar esa restricción, pero no sola: al habilitar el apartado
hay que definir qué pasa con el stock y con la vigencia (las Dudas 1 y 2 de arriba). El
usuario ya decidió lo del stock: **se descuenta al apartar**, y vuelve si se cancela.

Nota: el código ya descuenta stock al crear el pedido, así que esa parte ya se comporta
como se quiere. Lo que falta es el permiso, la vigencia congelada (R2) y la devolución al
cancelar.

---

## Hallazgo relacionado: el subtotal no se validaba (corregido)

Al revisar esta validación salió un agujero de dinero: `validarLineasPromocion` revisa
vigencia, precio unitario y cantidad, pero **el subtotal nunca viajaba**:

```java
new LineaPromocionCheck(dp.getVariante().getId(), dp.getCantidad(), dp.getPrecioUnitario())
//                                                  el subTotal no esta aca
```

Y como el total del pedido se arma sumando subtotales, un request con el precio unitario
correcto y `subTotal: 1` dejaba el pedido entero en $1.

**Corregido el 2026-09-22:** el subtotal ya no se lee del request, se calcula como
`precioUnitario × cantidad` en `VentaServiceImpl` y `PedidoServiceImpl`. El precio unitario
sigue validándose como antes.

---

## Reglas acordadas 2026-09-22 — editar un pedido ya creado

### R5 — Una promocion es un combo: o entra completa, o no entra

Al cambiar un producto que pertenece a una promocion, se busca primero **dentro de esa
promocion**. Si hay un reemplazo ahi, se cambia y la promocion sigue intacta.

Si no lo hay, se le avisa y se le ofrecen dos salidas, **las dos validas**:

| Opcion | Que pasa |
|---|---|
| **(a) Quitar la promocion** | salen **todas** las lineas de esa promocion (su stock vuelve), entra el producto nuevo a precio normal. Las lineas ajenas a la promocion no se tocan |
| **(b) Conservarla y agregar** | no se quita nada, se suma una linea nueva a precio normal |

**Por que (a) saca el combo entero y no solo la linea que se cambiaba:** el precio
promocional existe porque se llevan esas piezas juntas. Dejar dos de tres al precio del
combo seria cobrar un descuento por una condicion que ya no se cumple. `validarLineasPromocion`
ya exige el combo completo al crear el pedido; esto mantiene la misma regla al editarlo.

**Ejemplo del negocio (2026-09-22):** pedido con pantalon de dama (en promocion), perfume y
cartera. El cliente quiere el pantalon de hombre. Si la promocion no tiene pantalon de
hombre, el admin elige: cambiar la promocion completa por el de hombre (y quedarse con
perfume y cartera), o dejar la promocion y llevar tambien el de hombre.

### R6 — El tercer precio se usa, y el cliente ve lo que pago

`producto` tiene tres precios: `precio_costo` (nunca se vende a eso), `precio_venta` (el
normal) y `precio_rebaja` (el descuento del admin). Hasta el 2026-09-22 el tercero se
guardaba y se mostraba en el admin, pero **nunca se cobraba**: para bajar un precio habia que
armar una promocion, que es mas trabajo y deja registrado algo que no es.

La venta puede cobrar `precio_venta` **o** `precio_rebaja`. Nada mas: siguen siendo los dos
precios del catalogo, asi que el front no puede inventar un monto y el blindaje del subtotal
(ver VentaServiceImpl) queda intacto.

**Que ve el cliente:**

| Donde | Precio |
|---|---|
| Catalogo / tienda | `precio_venta` — la rebaja nunca se publica |
| **Su pedido** | lo que realmente pago, aunque sea el rebajado |

La rebaja es secreta como precio de lista, no como precio cobrado: una vez aplicada en una
venta, es el precio de ese cliente y tiene derecho a verlo en su comprobante.

### R7 — Todo boton nuevo necesita su permiso

Cambiar el tipo de un pedido, cambiar un producto ya vendido y cobrar a un precio distinto
son acciones que no puede hacer cualquiera. Cada una necesita **cuatro piezas**, no solo el
endpoint:

1. su accion en `accion_submenu`
2. una migracion SQL que la da de alta (ver `migration_accion_tienda_eliminar.sql`)
3. el matcher en `SecurityConfig` con `accion("<pantalla>", "<accion>")`
4. el endpoint

Sin las tres primeras el boton queda visible para todos o para nadie. Mismo patron que
`eliminar` y `habilitar` en `productos/buscar`.
