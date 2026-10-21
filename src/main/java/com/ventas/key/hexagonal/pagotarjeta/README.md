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

## Contratos que TODO adaptador nuevo debe cumplir
- `PasarelaDePagoPort.crearCobro`: manda el id del intento como `external_reference` e idempotency key. Rechazo claro → `CobroNoCreado`; cualquier otro fallo → RuntimeException.
- `consultar` (Checkout Pro): si algún pago está aprobado, devuelve APROBADO aunque otro se haya rechazado.
- `ActualizarPedidoPort.marcarPagado` y `registrarDevolucion`: **idempotentes**. No reutilizar `PedidoServiceImpl.updatePedido` tal cual (pone "Entregado", rechaza Apartado y clientes sin registro, no es idempotente).
- Servicios con `@Service` + `@Bean Clock` + dos `@Scheduled` (`expirarVencidos`, `reconciliar`), en una sola instancia.
- Capa web: comprobar que quien consulta/cancela es el dueño (`clienteId` del intento) o admin; devolver solo admin; `CobroNoCreado`→502, `ConflictoDeConcurrencia`→409, resto→400; el webhook valida la firma (T5).
- Límite honesto: la API vigente de la terminal (Point) quizá no permita buscar por `external_reference`; confirmarlo antes de escribir ese adaptador.

## Falta
Adaptador del pedido (marcar pagado + crear venta), pasarela real de la terminal y de Checkout Pro, controllers + permisos, `CAMBIOS_FRONT.md`, front.

## ⚠️ Fuera de esta rama
`MercadoPagoController.simularPago` (`@Profile("dev")` en un método se ignora) existe también en producción (solo ADMIN). Hotfix pendiente de decisión del dueño.
