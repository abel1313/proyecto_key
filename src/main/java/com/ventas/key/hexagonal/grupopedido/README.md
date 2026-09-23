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
| R11 | Un grupo **de contado** se cobra de una vez desde la card del titular: se confirman todos los pedidos abiertos, del más viejo al más nuevo, con la misma forma de pago, todo o nada. Los ya entregados se saltan. Un grupo a crédito no se cobra así: se abona (R6). |
| R12 | **Unidos se ven como uno.** En la lista del admin solo sale el titular, con el total de todos; los demás se abren buscando su número exacto. En el detalle se ven los artículos de todos. |
| R13 | Un pedido de contado `Entregado` cuenta como cobrado (`pagado = total`, `saldo = 0`), aunque `totalPagado` siga en 0. |
| R14 | **Separar reparte lo que dio el cliente.** Al separar un grupo a crédito se escribe cuánto de lo abonado se queda cada pedido que sale. La suma tiene que ser exacta (ni más ni menos de lo que ha dado) y ningún pedido puede quedarse con más de lo que cuesta. Si no cuadra, no se separa. El dinero se mueve moviendo los abonos que ya existen (con su fecha y forma de pago), no registrando abonos nuevos: así el corte de ese día no cuenta el dinero dos veces. |
| R15 | **Se puede separar solo uno.** Lo que no se lleva sigue siendo del grupo y se acomoda en los que quedan, del más viejo al más nuevo. Si quedan 2 o más, siguen unidos en un **grupo nuevo** ("Sigue del grupo #N"); si queda 1, también se separa. Si el que recogía se separa, hay que elegir quién recoge a los que quedan. |
| R16 | **Quién recoge se cambia** cuando se quiera, entre los pedidos del grupo. |
| R17 | Después de repartir, cada pedido queda según su dinero: si cubre su total queda **PAGADO** y se crea su venta; si ya no lo cubre (se le pasó dinero a otro) vuelve a Apartado / Ir pagando y se **borra** la venta que tenía (mismo criterio que reabrir un contado). |

**Cambios a reglas anteriores (2026-09-23, v2):**
- **R2:** un pedido a crédito **ya pagado sí se une** (su dinero pasa a ser del grupo). Uno cobrado de
  contado (`Entregado`) no: primero se pasa a Apartado / Ir pagando con "Cambiar forma de cobro".
- **R7:** "deshacer" ya no se puede si el cliente ya dio dinero en un grupo a crédito: se usa
  **separar** para decir cuánto se queda cada pedido. En la pantalla ya no hay "Deshacer", solo "Separar".
- Los artículos de los otros pedidos se cambian o quitan **desde el detalle del anfitrión**, y al
  agregar se pregunta a qué pedido va. El cambio se guarda en el pedido dueño del artículo.

El historial queda en las observaciones de cada pedido (unir, separar, con cuánto se quedó, cambio
de quién recoge) y en la nota de cada abono movido (`Reparto al separar el grupo #N: del pedido #A al #B`).

R11–R13 se agregaron después de la primera prueba en QA (2026-09-23): con los pedidos separados en
la lista, la unión no se notaba.

## Dónde está cada cosa

- Tablas `grupo_pedido` y `grupo_pedido_miembro` → `migration_grupo_pedido.sql`.
- Las entidades `GrupoPedido` y `GrupoPedidoMiembro` viven en `mis/productos/entity` y no en
  `infraestructura/`, porque el escaneo de entidades JPA solo cubre `com.ventas.key.mis.productos`.
- Endpoints: `/v1/grupos-pedido` (`GrupoPedidoController`). Contrato en `CAMBIOS_FRONT.md`.
- La lista del admin: el filtro que esconde a los no titulares está en los dos queries nativos
  de `IPedidoRepository` (`buscarPedidosPorCliente`, `buscarTodosLosPedidos`); el campo
  `pedido.grupo` lo pone `PedidoServiceImpl.marcarGrupos` con `ConsultarGruposCasoUso`.
- `ConsultarGruposCasoUso` va separado de `UnirPedidosCasoUso` a propósito: la lista
  (`PedidoServiceImpl`) lo usa, y `UnirPedidosService` depende de `PedidoServiceImpl` para cobrar
  (`ConfirmarPedidoAdapter`). Juntos cerrarían un ciclo de dependencias y Spring no arranca.
- Permisos: `unir-pedidos` (nuevo) para unir, ver y deshacer; `abonar` (ya existía) para abonar.
