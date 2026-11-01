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

## Endpoints (solo administrador: todo `/v1/dashboard/**` ya lo exige en `SecurityConfig`)
- `GET /v1/dashboard/graficas/ventas-por-forma-pago?desde=&hasta=`
- `GET /v1/dashboard/graficas/ventas-gastos-mensual?meses=12`

## Dónde está cada cosa
`dominio/modelo` (resultados), `dominio/puerto/{entrada,salida}`, `aplicacion/servicio/ConsultarGraficasService` (Java puro: fechas por
defecto, porcentajes, meses seguidos), `infraestructura/salida/persistencia/GraficasJdbcAdapter` (SQL de solo lectura, probado con H2),
`infraestructura/entrada/rest/GraficasVentasController`.
