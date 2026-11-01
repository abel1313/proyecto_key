# Dominio: revisionventas (Revisión de ventas)

Avisa de **cosas que no deberían pasar** en pedidos, ventas, abonos, stock y cobros con tarjeta. **Solo lee**: nunca
corrige nada; dice qué revisar. Pedido del dueño (2026-11-01): *"validar las ventas"*. Plan completo y siguientes pasos:
`PLAN_REVISION_VENTAS_Y_GRAFICAS.md` (raíz del proyecto).

## Reglas (una por tipo; palabras del dueño en `TipoHallazgo`)
| Código | Regla |
|---|---|
| `PAGADO_SIN_VENTA` | Pedido en Entregado/Pagado, con fecha en el rango, sin fila en `ventas` |
| `TOTAL_PEDIDO_NO_CUADRA` | `\|total_pedido − Σ detalle_pedidos.sub_total\| > 0.01`, no cancelado, en el rango |
| `TOTAL_VENTA_NO_CUADRA` | `\|total_venta − Σ detalle_venta_variantes.sub_total\| > 0.01` (solo ventas con renglones ahí), en el rango |
| `ABONOS_NO_CUADRAN` | FIADO/APARTADO no cancelado: `\|total_pagado − Σ abono_pedido.monto\| > 0.01`, en el rango |
| `COBRO_TARJETA_SIN_APLICAR` | `pago_tarjeta_intento` en COBRADO_SIN_APLICAR, o APROBADO con pedido no pagado (sin rango) |
| `STOCK_NEGATIVO` | `variantes.stock < 0` o `producto.stock < 0` (sin rango) |
| `ARTICULO_CON_STOCK_SIN_MODELO` | Artículo y modelo habilitados, modelo no interno, `variantes.stock > 0` y `producto.stock < 1` (sin rango) |
| `VENTA_SIN_PEDIDO` | `ventas.pedido_id IS NULL`, en el rango |

Sin fechas: últimos 90 días hasta hoy (hora de México). Por tipo se devuelve el **total real** y hasta **50 ejemplos**
(los más recientes primero). Un tipo que no se puede revisar en un ambiente (p. ej. producción sin la tabla del cobro con
tarjeta) sale en `noRevisados` y no tumba a los demás.

## Dónde está cada cosa
- `dominio/modelo`: `TipoHallazgo` (reglas y textos), `Hallazgo`, `HallazgosDeUnTipo`, `ResultadoRevision`.
- `dominio/puerto/entrada/RevisarVentasCasoUso`, `dominio/puerto/salida/HallazgosPort`.
- `aplicacion/servicio/RevisarVentasService` (Java puro: fechas por defecto, recorre los tipos).
- `infraestructura/salida/persistencia/HallazgosJdbcAdapter` (SQL de solo lectura; se prueba con H2).
- `infraestructura/entrada/rest/RevisionVentasController`: `GET /v1/reportes/revision-ventas?desde=&hasta=` (solo ADMIN).
- Permiso de la pestaña: acción `reporte-revision` (`migration_accion_reporte_revision.sql`, solo ROLE_ADMIN).

## Falsas alarmas que se evitaron a propósito (no están en la v1)
Total de venta ≠ total del pedido (editar un pedido pagado no recalcula lo vendido, R4); `total_pagado` de pedidos de
contado (cada camino lo guarda distinto); pedido cancelado que conserva venta (falta confirmar la regla).
