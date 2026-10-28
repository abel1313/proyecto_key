# Dominio `pagotarjeta` — cobrar con tarjeta (terminal y en línea)

Rama `feature/pago-tarjeta`. Plan y decisiones: `PLAN_PAGO_TARJETA.md`. Tablas: `pago_tarjeta_intento`,
`pago_tarjeta_devolucion` (script `migration_pago_tarjeta.sql`).

## Cuándo se acepta tarjeta (decidido con el dueño, 2026-10-21)
- Cuando el cliente **llega al local** y se cobra en la terminal, o cuando el cliente **hace el pedido y paga él mismo desde la tienda**.
- **Apartado** (sin dinero) e **Ir pagando** (abonos) **nunca** aceptan tarjeta: se pagan en efectivo o transferencia al entregar.
- Un pedido normal sin pagar (Contado) admite los dos canales (`TERMINAL`, `EN_LINEA`).
- Pagado en la terminal → queda Pagado y Entregado. Pagado en línea → Pagado, falta entregar.

## Reglas (cada una con su prueba)
| # | Regla |
|---|---|
| T1 | Un solo cobro activo por pedido (índice único en la base sobre una columna generada) |
| T2 | El monto lo calcula el back (total del pedido), nunca el front |
| T3 | Solo Contado/Pago en línea aceptan tarjeta |
| T4 | El pedido se marca pagado solo cuando la pasarela confirma; `confirmar` es idempotente |
| T5 | Webhook con firma (pendiente: va en el adaptador real) |
| T6 | Nunca se guardan datos de tarjeta |
| T7 | Vigencia: terminal 10 min, en línea 30 min; al vencer se consulta a la pasarela antes de liberar |
| T8 | Devoluciones totales o parciales, con clave de idempotencia y reserva previa |
| M1–M6 | Meses sin intereses: casilla apagada por default (producto y artículo), todos los artículos deben aceptar, mínimo $300 (`pagos.tarjeta.msi-minimo`), planes 3/6/9/12, nunca en Apartado/Ir pagando, el back manda el tope de cuotas |

Freno: 3 rechazos del mismo pedido en 2 horas (cancelar no cuenta).

## Principios de seguridad del dinero (de dos revisiones independientes)
1. **Nunca se da por "no cobrado" algo dudoso.** Si se pierde la respuesta al crear el cobro, queda activo (CREADO) y se busca por referencia; no se cancela a ciegas.
2. **Antes de expirar, cancelar o rechazar se pregunta a la pasarela.** Si ya cobró, se aplica (aprobación tardía).
3. **Marcar el pedido pagado y guardar APROBADO van en una sola transacción** (`TransaccionPort`); nunca se llama a la pasarela dentro de una transacción.
4. **Candado de versión** (`@Version`): `confirmar` devuelve lo que dejó el otro proceso.
5. **Doble pago o cambio de total** → estado `COBRADO_SIN_APLICAR`: el dinero no se aplica al pedido y queda pendiente de devolver.
6. **Devoluciones**: se reservan en la base (clave única) antes de hablar con la pasarela; si el back se cae a medias, la siguiente solicitud la termina con la misma clave.
7. **En línea un rechazo no termina el cobro** (el cliente puede reintentar con otra tarjeta); sigue vivo hasta que venza.
8. `reconciliar()` vuelve a preguntar por cobros rechazados/cancelados/vencidos de las últimas 24 h (por si el webhook nunca llegó).

## Dónde está cada cosa
- `dominio/modelo`: `IntentoDePago`, `EstadoIntento`, `ReglasMsi`, `Devolucion`, `PedidoACobrar`, …
- `dominio/puerto/entrada` (casos de uso) y `dominio/puerto/salida` (pedido, intentos, devoluciones, planes, pasarela, transacción).
- `aplicacion/servicio`: `CobroConTarjetaService`, `DevolverCobroService`, `OfrecerPlanesService` (sin `@Service` todavía).
- `infraestructura/salida/persistencia`: adaptadores JPA/JDBC/transacción. Entidades y repositorios viven en `mis/productos/{entity,repository}`.
- `infraestructura/salida/pasarela/PasarelaSimuladaAdapter`: solo perfiles `dev`/`qa` **y** `pagos.tarjeta.pasarela=simulada`; **nunca mueve dinero**. Resultado por los centavos del total: `.01` rechazada · `.02` nunca contesta · `.03` cancelada · otro → aprobada a los ~4 s (`pagos.tarjeta.simulada.segundos`).

## Tercera revisión (2026-10-21, agente revisor): qué se corrigió y qué queda

El revisor reprodujo 12 fallas con pruebas contra el servicio y los adaptadores reales. Corregidas:

| Hallazgo | Corrección |
|---|---|
| **C1** un contado cobrado se guardaba `PAGADO`; el resto del sistema espera `Entregado` (el estado dice el PAGO; la entrega va aparte en `entregado`) | Se escribe `Entregado` (+ `entregado=true` solo en la terminal) y `total_pagado` se queda en 0, igual que la venta de mostrador |
| **C2** una aprobación sobre un pedido cancelado, cambiado a Ir pagando, con abonos o con otro total se aplicaba igual | El adaptador bloquea el pedido y lo revisa de nuevo; si no admite el pago devuelve `NO_APLICABLE` y el cobro queda `COBRADO_SIN_APLICAR` (por devolver) |
| **C3** dos pagos simultáneos quedaban los dos aprobados | `marcarPagado` bloquea la fila (`PESSIMISTIC_WRITE`) y refresca la entidad (OSIV); el servicio decide con su respuesta, no con una lectura previa |
| **C4** `reconciliar`/`expirarVencidos` nunca aplicaban: `LazyInitializationException` sin sesión | `buscar` es `@Transactional(readOnly)`; los `catch` de los trabajos ahora escriben en el log |
| **A1** una aprobación tardía dejaba vivo otro cobro activo del pedido | Al aplicar se cancela en la pasarela el otro cobro activo (o, si ya cobró, queda por devolver) |
| **A2** si faltaba un dato para la venta el cobro se quedaba "en proceso" para siempre | `NoSePudoAplicar` (usuario o catálogo de pago faltante) → `COBRADO_SIN_APLICAR` con el error en el log |
| **A4** simulada tras reiniciar el pod: cobros desconocidos bloqueaban el pedido | Un cobro desconocido se da por cancelado |
| **M1** `@Bean Clock` faltante | `PagoTarjetaConfig` |
| **M2** sin la columna `acepta_msi` (p. ej. prod sin migrar) tronaba y hacía N+1 | Una sola consulta; si la columna falta, nadie acepta MSI y se puede cobrar de contado |
| **M3** pedido en un grupo | `iniciar` lo rechaza (el grupo no se cobra con tarjeta todavía) |
| **M6** planes MSI de débito | Solo se ofrecen planes de tarjeta de crédito |
| Cancelar o cambiar de forma de cobro un pedido con cobro de tarjeta | `PedidoServiceImpl` lo rechaza mientras haya cobro activo, aprobado o sin devolver |

**Cuarta revisión (capa web, 2026-10-22):** corregido (C1) cancelar/cambiar tipo tronaba en un ambiente sin la tabla aunque el módulo estuviera apagado → ahora esas revisiones dependen de `pagos.tarjeta.habilitado`; (A1) consultar el estado ya no vacía todas las cachés (solo si el cobro cambió); (M1) un cliente no cancela el cobro de la terminal; (M2 parcial) el cobro en efectivo (`updatePedido`) se bloquea con un cobro de tarjeta en proceso. Pendiente de esa revisión: bloquear también abonos, agregar artículos y unir en grupo con cobro activo; los trabajos programados comparten un solo hilo con los otros 15 (subir `spring.task.scheduling.pool.size` o dar timeout a la pasarela real); mensaje claro si se enciende sin pasarela; freno para repetir iniciar→cancelar en línea.

**Pendiente (decisión o trabajo aparte):** (A3) la devolución solo registra el rastro: no marca la venta ni el pedido ni regresa mercancía/stock, y cancelar un pedido pagado con tarjeta no devuelve el cargo (por eso ahora se bloquea); (M4) el cálculo de comisión copia la fórmula actual de ventas (tarifa + IVA completos sobre el subtotal), revisar con el dueño; (M5) `Venta.usuario` es quien inició el cobro; (M7) H2 no prueba el índice único de T1 ni los CHECK (se probaron en MySQL real); editar artículos y unir en grupo un pedido con cobro activo todavía no se bloquean (el cambio de total se detecta y deja el cobro por devolver); alerta/listado de `COBRADO_SIN_APLICAR`; condicionar el módulo con una propiedad antes de llevarlo a `main`.

## Contratos que TODO adaptador nuevo debe cumplir
- `PasarelaDePagoPort.crearCobro`: manda el id del intento como `external_reference` e idempotency key. Rechazo claro → `CobroNoCreado`; cualquier otro fallo → RuntimeException.
- `consultar` (Checkout Pro): si algún pago está aprobado, devuelve APROBADO aunque otro se haya rechazado.
- `ActualizarPedidoPort.marcarPagado` y `registrarDevolucion`: **idempotentes**. No reutilizar `PedidoServiceImpl.updatePedido` tal cual (pone "Entregado", rechaza Apartado y clientes sin registro, no es idempotente).
- Servicios con `@Service` + `@Bean Clock` + dos `@Scheduled` (`expirarVencidos`, `reconciliar`), en una sola instancia.
- Capa web: comprobar que quien consulta/cancela es el dueño (`clienteId` del intento) o admin; devolver solo admin; `CobroNoCreado`→502, `ConflictoDeConcurrencia`→409, resto→400; el webhook valida la firma (T5).
- Límite honesto: la API vigente de la terminal (Point) quizá no permita buscar por `external_reference`; confirmarlo antes de escribir ese adaptador.

## Adaptador del pedido (hecho): `PedidoCobroAdapter`
- Lee: `tipo_pedido` NORMAL=Contado, APARTADO, FIADO=Ir pagando; pagado = estado PAGADO/Entregado; cancelado nunca se cobra; la casilla MSI sale de `COALESCE(variantes.acepta_msi, producto.acepta_msi)`.
- `marcarPagado` (idempotente): pedido PAGADO con su total pagado + venta con forma de pago tarjeta (comisión según los meses; si no hay fila de esos meses en `pagos_y_meses` usa tarjeta de contado y avisa en el log) + si fue en la **terminal** queda también entregado; si fue **en línea**, queda Pagado y falta entregar.
- `registrarDevolucion` solo deja bitácora: el registro vive en `pago_tarjeta_devolucion`; regresar mercancía/stock es decisión aparte.
- Datos de QA verificados (2026-10-21): fila 2 = Tarjeta Débito a 1 pago (la misma que usa `AbonoServiceImpl`), fila 3 = Tarjeta crédito a 1 pago (misma comisión 3.5), fila 4 = Tarjeta crédito a 3 meses (4.69). Hoy solo existe el plan de 3 meses; para 6, 9 y 12 faltan filas en `pagos_y_meses`. Los meses se buscan solo en tarjeta de crédito. `Venta.usuario` es el usuario que inició el cobro (`iniciadoPor` = username).

## Casilla de meses sin intereses (2026-10-28)
`ConfigurarAceptaMsiCasoUso` / `ConfigurarAceptaMsiService` / `AceptaMsiPort` / `AceptaMsiJdbcAdapter` / `AceptaMsiController`
(`/v1/acepta-msi`, solo ADMIN). Lee y guarda `producto.acepta_msi` y `variantes.acepta_msi` (NULL = hereda) con **SQL directo, no con las
entidades JPA**: `Producto` y `Variantes` no mapean esas columnas a propósito, porque un ambiente sin la migración (producción) se quedaría
sin catálogo (`Unknown column`). Sin la columna solo falla esta casilla, con 409 y un mensaje claro. `GET /pedido/{id}/meses` ahora responde
`{meses, motivo, articulosSinMeses}` (`OfertaDeMeses`, `ReglasMsi.ofertaDeMeses`) para poder explicar por qué no hay meses.

## Dos caminos para la terminal (2026-10-28)
Mientras no se retire `/v1/mp`, `PedidoACobrarPort.hayCobroEnProcesoPorElCaminoAnterior` (consulta `mp_payment_intent` en `OPEN`) hace que
`CobroConTarjetaService.iniciar` rechace el pedido, y `MercadoPagoService.iniciarPago` rechaza el pedido si ya tiene un cobro nuevo en
proceso o sin devolver. Los grupos y la venta directa siguen por el camino anterior hasta que el módulo nuevo los soporte.

## Falta
Pasarela real de la terminal y de Checkout Pro (al final, necesita datos de Mercado Pago), front del cobro y de la casilla.

## ⚠️ Fuera de esta rama
`MercadoPagoController.simularPago` (`@Profile("dev")` en un método se ignora) existe también en producción (solo ADMIN). Hotfix pendiente de decisión del dueño.
