# Dominio `entrega` — Pagado y Entregado son dos cosas

Decidido por el dueño el 2026-10-06 (`PLAN_PEDIDOS_VENTAS_ENTREGA.md` §11, skill `reglas-pedidos` 2.4).

La card de un pedido dice siempre **dos** cosas: el pago (**Pagado** / **Falta pagar $X**) y la
entrega (**Entregado** / **Falta entregar**). El pago sigue en `pedidos.estado_pedido` como siempre;
la entrega va en `pedidos.entregado` + `pedidos.fecha_entregado` (`migration_entrega_pedido.sql`).

| # | Regla | Dónde |
|---|---|---|
| E1–E4 | Al liquidar un Apartado, crear un Ir pagando o cobrar un contado, el front pregunta "¿Ya se lo llevó?" y, si es Sí, llama a `POST /v1/pedidos/{id}/entrega` | front |
| E2 | Si se olvidó, la card tiene **📦 Entregar** (mismo endpoint) | front |
| E5 | Entregar no cobra nada | `Entrega` |
| E6 | Regresar a "Falta entregar": `DELETE /v1/pedidos/{id}/entrega`, acción `regresar-entrega` (solo admin por default) | `Entrega.regresar()` |
| E7 | Pedido unido: entregar / regresar actúa sobre todo el grupo activo (los cancelados se ignoran) | `PedidosParaEntregarJdbcAdapter` |
| E8 | Apartado y contado se entregan solo pagados; Ir pagando sí se entrega debiendo | `Entrega.entregar()` |
| — | Cancelar un Ir pagando que **no** se lo llevó regresa el stock (2.3) | `AbonoServiceImpl.cancelar…`, `PedidoServiceImpl` |

Dónde está cada cosa: `dominio/modelo` (`PedidoParaEntregar`, `Entrega`), `dominio/excepcion`
(`EntregaNoPermitidaException` → 400), `aplicacion/servicio/EntregaService`,
`infraestructura/entrada/rest/EntregaController`, `infraestructura/salida/persistencia/PedidosParaEntregarJdbcAdapter`.
