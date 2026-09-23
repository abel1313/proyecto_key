# Dominio: grupopedido

**Unir pedidos** para cobrarlos y entregarlos juntos, y poder **regresar a como estaba**.

Reglas acordadas con el negocio antes de escribir el código (2026-09-23, ver
`hexagonal/_plantilla/README.md`, PASO 0).

---

## El problema

> *"Puede pasar que haga 3 pedidos al mismo cliente para que la suma sea de los pedidos, o que
> alguien vaya a recoger el de otra persona: poder unir y además poder regresar a como estaba."*

## La decisión: agrupar, no fusionar

Unir **no** mueve artículos ni abonos de un pedido a otro. Se crea un **grupo** que apunta a los
pedidos:

- Los totales del grupo se calculan al momento desde los pedidos vivos. Si a uno se le agrega un
  artículo o se cancela, el grupo ya lo refleja sin sincronizar nada.
- Un abono al grupo se **reparte**: cada parte es un abono real del pedido que le tocó, registrado
  con el abono de siempre (`AbonoServiceImpl.registrarAbono`), así que la liquidación, el paso a
  PAGADO y la venta funcionan igual que con un pedido suelto.
- Deshacer es marcar el grupo como inactivo. Cada pedido ya tiene lo suyo; no hay nada que
  separar.

Se descartó fusionar en un solo pedido porque deshacer obligaría a recordar de qué pedido venía
cada artículo y cada abono, y los abonos hechos después de unir no tendrían dueño.

## Reglas

| # | Regla |
|---|---|
| R1 | Se unen **2 o más** pedidos distintos. |
| R2 | Solo pedidos **abiertos**: no cancelados, no entregados, no pagados. |
| R3 | Todos con la **misma forma de cobro**. Si no, error con el tipo de cada pedido; se corrige con "Cambiar forma de cobro" en el detalle de cada uno y se vuelve a unir. |
| R4 | Un pedido está en **un solo grupo activo** a la vez. |
| R5 | El grupo tiene un **titular**: uno de sus pedidos, cuyo cliente paga y recoge. Cada pedido conserva su propio cliente. Se pueden unir pedidos de clientes distintos. |
| R6 | El abono al grupo solo aplica a crédito (Apartado / Ir pagando). Se reparte **del pedido más viejo al más nuevo**, nunca excede el saldo del grupo, y cada parte lleva la nota `Abono del grupo #N`. Tarjeta no, igual que el abono normal. El cambio del efectivo se calcula contra el abono completo. |
| R7 | **Deshacer se puede siempre**, aunque ya haya abonos: cada pedido se queda con sus artículos y con los abonos que le tocaron. No se mueve dinero ni stock. |
| R8 | Un pedido cancelado dentro del grupo deja de sumar al saldo. |
| R9 | Mientras un pedido está en un grupo activo **no se le cambia la forma de cobro** (rompería R3). Guardia en `PedidoServiceImpl.cambiarTipoPedido`. |
| R10 | Unir y deshacer quedan escritos en las observaciones de cada pedido, con fecha. |

Un grupo **de contado** sirve para ver la suma y saber quién recoge; cada pedido se sigue
confirmando desde su detalle, como hoy.

## Dónde está cada cosa

- Tablas `grupo_pedido` y `grupo_pedido_miembro` → `migration_grupo_pedido.sql`.
- Las entidades `GrupoPedido` y `GrupoPedidoMiembro` viven en `mis/productos/entity` y no en
  `infraestructura/`, porque el escaneo de entidades JPA solo cubre `com.ventas.key.mis.productos`.
- Endpoints: `/v1/grupos-pedido` (`GrupoPedidoController`). Contrato en `CAMBIOS_FRONT.md`.
- Permisos: `unir-pedidos` (nuevo) para unir, ver y deshacer; `abonar` (ya existía) para abonar.
