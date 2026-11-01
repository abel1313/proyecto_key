# Dominio: graficasventas (gráficas del Dashboard)

Los números de las gráficas del Dashboard. **Solo lee.** Plan y siguientes gráficas: `PLAN_REVISION_VENTAS_Y_GRAFICAS.md`.

## Reglas
- Las ventas con estado **«Devuelta» no cuentan** (mismo criterio que las tarjetas del Dashboard: `IVentaRepository.sumVentas`).
- **Ganancia neta = ganancia − gastos** (misma cuenta que la tarjeta «Ganancia neta»); gastos = `gastos_surtir.precio_gasto`.
- **Comisión** de una forma de pago = `total_venta × (tarifa + IVA de la terminal) / 100`, la misma cuenta con la que se calcula la
  ganancia al vender (`VentaServiceImpl`, `PedidoCobroAdapter`). En efectivo es 0.
- Una venta sin forma de pago registrada sale como «Sin forma de pago registrada» (no se pierde del total).
- **Por mes:** siempre vienen todos los meses seguidos (del más viejo al actual); un mes sin movimiento sale en cero. De 1 a 24 meses.
- **Por forma de pago:** sin fechas, del día 1 del mes a hoy (hora de México); el porcentaje es sobre el total del periodo; de la que
  más vendió a la que menos.

- **Dinero por cobrar:** pedidos con estado `APARTADO` o `FIADO` (la misma cuenta que la tarjeta «Por cobrar») con saldo (`total_pedido − total_pagado`)
  mayor a un centavo. Antigüedad = días desde `fecha_pedido`: 0–7, 8–30 y más de 30. Lista los 8 que más tiempo llevan, con el nombre del cliente
  (o del cliente sin registro, o «Cliente sin nombre»).
- **Capital parado:** artículo habilitado, de modelo habilitado y no interno, con `stock > 0`, dado de alta hace más de N días (o sin fecha) y sin ventas
  en los últimos N días (o nunca vendido). Capital = `stock × producto.precio_costo`. Muestra los 10 con más capital. N de 7 a 365 (default 60).
- **Pedidos por estado:** Pendiente de pago, Apartado, Ir pagando, Pagado-falta entregar (estado Entregado/Pagado con `entregado = false`), Entregado y
  Cancelado. Los **abiertos se cuentan todos**; los terminados (Entregado, Cancelado) solo de los últimos N días (default 90). **Atrasados** = sin entregar,
  sin cancelar y con `fecha_recogida` ya pasada. Siempre salen las seis categorías (con 0).
- ⚠️ Pedidos viejos con `estado_pedido` Entregado y `entregado = false` (anteriores a la columna `entregado`) saldrían como «falta entregar»/«atrasados».

## Endpoints (solo administrador: todo `/v1/dashboard/**` ya lo exige en `SecurityConfig`)
- `GET /v1/dashboard/graficas/ventas-por-forma-pago?desde=&hasta=`
- `GET /v1/dashboard/graficas/ventas-gastos-mensual?meses=12`
- `GET /v1/dashboard/graficas/por-cobrar`
- `GET /v1/dashboard/graficas/capital-parado?dias=60`
- `GET /v1/dashboard/graficas/pedidos-por-estado?dias=90`

## Dónde está cada cosa
`dominio/modelo` (resultados), `dominio/puerto/{entrada,salida}`, `aplicacion/servicio/ConsultarGraficasService` (Java puro: fechas por
defecto, porcentajes, meses seguidos), `infraestructura/salida/persistencia/GraficasJdbcAdapter` (SQL de solo lectura, probado con H2),
`infraestructura/entrada/rest/GraficasVentasController`.
