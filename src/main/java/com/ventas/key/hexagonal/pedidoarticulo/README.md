# Dominio: pedidoarticulo

Editar los **artículos de un pedido ya creado**: agregar uno, cambiar uno por otro, y quitar una
promoción completa.

Reglas acordadas antes de escribir el código (ver `hexagonal/_plantilla/README.md`, PASO 0).
Continúa las reglas R5, R6 y R7 del dominio `promocion`.

---

## Por qué es un dominio aparte y no más código en `PedidoServiceImpl`

`PedidoServiceImpl` ya pasa las 1000 líneas y mezcla creación de pedido, notificaciones por
correo, stock, flores, abonos y caché. Meter aquí la edición de artículos significaba que
cualquier cambio futuro en cualquiera de esos temas tocara el mismo archivo — que es exactamente
como se llega a que un cambio de correo rompa el stock.

Este dominio **no toca** `PedidoServiceImpl`. Se comunica con la base por sus propios puertos y
se puede leer entero sin abrir nada más.

Lo único que se modifica afuera es `eliminarDetallePedido`, y solo para **cerrarle un agujero**
(ver R4 abajo).

---

## El problema (reportado 2026-09-22)

> *"Necesito poder cambiar o agregar un producto en un pedido que ya está hecho. Y si es una
> promoción no debe dejar cambiar el producto a uno que no tenga promoción."*

Hoy solo se puede **quitar** (`DELETE /v1/pedidos/{id}/detalle/{productoId}`, el botón `−`). Para
agregar algo o cambiar una talla hay que cancelar el pedido entero y rehacerlo — que devuelve y
vuelve a descontar el stock, y deja registrado algo distinto de lo que realmente pasó.

---

## Reglas

### R1 — Un pedido entregado o cancelado no se edita

Ya se cerró. Es la misma regla que ya aplica `eliminarDetallePedido` y que aplica el cambio de
forma de cobro; se repite aquí porque es del dominio, no de cada endpoint.

### R2 — Solo se cobra a precio de catálogo

Un artículo que se agrega se cobra a `precio_venta` **o** a `precio_rebaja` (R6 del dominio
`promocion`). Ningún otro monto. El front no puede inventar un precio ni al crear el pedido ni al
editarlo — si pudiera al editarlo, el blindaje de la creación no serviría de nada.

El subtotal **no se recibe**: se calcula `precioUnitario × cantidad`. Mismo criterio con el que se
tapó el agujero del subtotal el 2026-09-22.

### R3 — El stock se mueve en el mismo paso

- Agregar un artículo **descuenta** stock del producto y de la variante.
- Quitar un artículo lo **devuelve**.
- Cambiar es quitar + agregar: devuelve el viejo y descuenta el nuevo, en una sola transacción.

Si no hay stock del nuevo, **no se toca nada** — no se puede quedar con el viejo devuelto y el
nuevo sin agregar.

### R4 — Una promoción es un combo: o entra completa, o no entra

(R5 del dominio `promocion`, aplicada a la edición.)

Al **cambiar** un artículo que pertenece a una promoción, se busca primero un reemplazo **dentro
de esa promoción**. Si lo hay, se cambia y la promoción sigue intacta.

Si no lo hay, el back **no decide solo**: responde con las dos salidas y deja elegir.

| Opción | Qué pasa |
|---|---|
| **(a) Quitar la promoción** | salen **todas** las líneas de esa promoción (su stock vuelve) y entra el artículo nuevo a precio normal. Las líneas ajenas no se tocan |
| **(b) Conservarla y agregar** | no se quita nada; se suma una línea nueva a precio normal |

**Por qué (a) saca el combo entero:** el precio promocional existe porque se llevan esas piezas
juntas. Dejar dos de tres al precio del combo sería cobrar un descuento por una condición que ya
no se cumple.

**Y por eso `eliminarDetallePedido` tenía un agujero:** dejaba quitar una línea suelta de una
promoción con el botón `−`, rompiendo el combo en silencio y dejando el resto al precio
promocional. Ahora lo rechaza y apunta a quitar la promoción completa.

**Ejemplo del negocio (2026-09-22):** pedido con pantalón de dama (en promoción), perfume y
cartera. El cliente quiere el pantalón de hombre. Si la promoción no lo tiene, el admin elige:
cambiar la promoción completa por el de hombre (y quedarse con perfume y cartera), o dejar la
promoción y llevar también el de hombre.

### R5 — Agregar nunca entra a una promoción

Se agrega **a precio de catálogo**, siempre. Una promoción se arma al crear el pedido, con su
combo completo validado; dejar que se le "agreguen piezas" después obligaría a revalidar el combo
entero en cada agregado y abriría la puerta a armar medio combo al precio del combo.

Quien quiera una promoción en un pedido que no la tiene, hace un pedido nuevo.

### R6 — Misma variante, misma línea

Agregar algo que ya está en el pedido **suma cantidad a la línea existente**, no crea una
segunda. Dos líneas de lo mismo con distinto precio son imposibles de explicar en un ticket.

La excepción es una línea de promoción: esa se deja quieta y se crea una línea nueva a precio
normal (por R5 una y otra no son lo mismo aunque sean la misma variante).

### R7 — El total lo recalcula el back, siempre

Después de cualquier edición, `totalPedido` se recalcula sumando los subtotales que quedaron. No
se ajusta con sumas y restas sobre el valor anterior: un pedido que ya venía descuadrado se
arrastraría para siempre.

**Lo que NO se toca:** `totalPagado`. Si el pedido ya tenía abonos y ahora vale menos, puede
quedar pagado de más — eso es una devolución, una decisión de negocio, y este dominio no la
inventa solo. Se informa en el response (`saldo` puede dar negativo) para que la pantalla lo
muestre.

### R8 — Cada botón, su permiso

Tres botones nuevos, tres acciones (R7 del dominio `promocion`):

| Botón | Acción | Endpoint |
|---|---|---|
| "+ Agregar artículo" | `agregar-articulo` | `POST /v1/pedidos/{id}/articulos` |
| "Cambiar" en la línea | `cambiar-articulo` | `PUT /v1/pedidos/{id}/articulos/{detalleId}` |
| "Quitar promoción" | `quitar-promocion` | `DELETE /v1/pedidos/{id}/promociones/{promocionId}` |

Migración: `migration_accion_pedido_articulos.sql`. Matchers en `SecurityConfig`, **antes** del
`hasRole("ADMIN")` genérico de `/v1/pedidos/**`.

---

## Preguntas que se hicieron antes de modelar

Del checklist de `_plantilla/README.md` — las que cambiaron el diseño:

**¿Se puede vaciar un pedido?** No. Un pedido sin artículos no es un pedido; si el cliente ya no
quiere nada, se cancela (que además registra el motivo). Quitar la última línea se rechaza.

**¿Se puede editar un pedido ya pagado?** Sí, y es el caso común: se apartó, se pagó, y al
entregarlo el cliente cambia una talla. Por eso R7 no toca `totalPagado`.

**¿Cambiar a la misma variante?** Se rechaza en vez de hacer un no-op: pedirlo significa que
quien lo mandó cree que está cambiando algo, y un 200 silencioso lo deja creyendo eso.

**¿Cantidad 0 o negativa?** Rechazada. Quitar es el endpoint de quitar.

---

## Estructura

```
pedidoarticulo/
├── dominio/            ← reglas puras: sin Spring, sin JPA
│   ├── modelo/         [Hexagonal: dentro del hexágono] [Clean: Entities]
│   ├── excepcion/
│   └── puerto/
│       ├── entrada/    [Hexagonal: Driving Port]  [Clean: Use Case]
│       └── salida/     [Hexagonal: Driven Port]   [Clean: Interface Adapter]
├── aplicacion/servicio/ [Hexagonal: dentro]       [Clean: Use Case interactor]
└── infraestructura/
    ├── entrada/rest/    [Hexagonal: Driving Adapter]
    └── salida/persistencia/ [Hexagonal: Driven Adapter]
```
