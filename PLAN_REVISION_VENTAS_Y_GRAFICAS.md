# Plan — Revisión de ventas y más gráficas (anotado 2026-11-01)

Pedido del dueño: *"herramienta para hacer gráficas o validar las ventas"*. Decisión: **empezar por la Revisión de
ventas** (alertas de cosas que no deberían pasar) y anotar todo lo demás. Nada se sube a `dev`/`qa` sin su orden.

## Lo que ya existe (no se rehace)
- **Dashboard** (`/dashboard`, solo ADMIN): 9 tarjetas con números (ventas hoy/mes, ganancia, gastos, ganancia neta,
  por entregar, créditos activos, monto por cobrar, stock bajo) + historial de accesos. Sin gráficas.
- **Reportes de ventas** (`/reportes`): pestañas Diario (tabla), Mensual (barras ventas vs ganancia por día),
  Por cliente (línea), Más vendidos (barras) y Promociones (tabla). Librería **Chart.js 4.4.9** ya instalada; los
  colores se leen de los tokens del diseño (`tema()` en `reportes.component.ts`).
- Back: `/v1/dashboard/resumen`, `/v1/reportes/ventas/{diario,mensual,cliente,productos-mas-vendidos,...}`.

## ETAPA 1 — Revisión de ventas (en curso)
Pestaña nueva **"🔎 Revisión"** dentro de Reportes, **solo administrador**, **solo lectura** (no corrige nada: dice qué
revisar). Dominio nuevo `hexagonal/revisionventas` (reglas puras + puerto de consultas + adaptador JDBC), endpoint
`GET /v1/reportes/revision-ventas?desde=&hasta=` (fechas opcionales; sin ellas, últimos 90 días para lo que depende de
fecha; stock y cobros se revisan siempre completos). Cada tipo devuelve el total y hasta 50 ejemplos.

| Código | Qué avisa (palabras del dueño) | Regla exacta | Gravedad |
|---|---|---|---|
| `PAGADO_SIN_VENTA` | Pedido pagado que no tiene venta | `estado_pedido` en (Entregado, Pagado) y no cancelado, sin fila en `ventas` con ese `pedido_id` | Alta |
| `VENTA_SIN_PEDIDO` | Venta que no está ligada a ningún pedido | `ventas.pedido_id` es NULL | Media |
| `TOTAL_PEDIDO_NO_CUADRA` | El total del pedido no es la suma de sus artículos | `|total_pedido − Σ detalle_pedidos.sub_total| > 0.01`, no cancelado | Alta |
| `TOTAL_VENTA_NO_CUADRA` | El total de la venta no es la suma de sus artículos | `|total_venta − Σ detalle_venta_variantes.sub_total| > 0.01` (solo ventas que tienen renglones ahí) | Alta |
| `ABONOS_NO_CUADRAN` | Lo pagado de un Apartado / Ir pagando no coincide con sus abonos | Pedido FIADO o APARTADO no cancelado: `|total_pagado − Σ abono_pedido.monto| > 0.01` | Alta |
| `STOCK_NEGATIVO` | Artículo o modelo con stock negativo | `variantes.stock < 0` o `producto.stock < 0` | Alta |
| `ARTICULO_CON_STOCK_SIN_MODELO` | Se ve con stock en la tienda pero el pedido lo rechaza ("Disponible: 0") | Artículo habilitado con `stock > 0` cuyo modelo (habilitado) tiene `stock < 1` — el caso de *Great Jeans talla 14* del 2026-10-31 | Alta |
| `COBRO_TARJETA_SIN_APLICAR` | Se cobró con tarjeta y no quedó aplicado al pedido | `pago_tarjeta_intento.estado = COBRADO_SIN_APLICAR`, o APROBADO con pedido no pagado. Si la tabla no existe (producción sin la migración) se omite y se avisa | Alta |

**Agregadas el 2026-11-02 (gravedad Media):** `VENDIDO_BAJO_COSTO` (precio de venta < costo en el renglón), `ARTICULO_SIN_PRECIO` (habilitado, con stock y sin precio) y `PEDIDO_PAGADO_SIN_ENTREGAR` (PAGADO, sin entregar, más de 14 días). **Agregada el 2026-11-02 (el dueño decidió 30 días):** `DEUDA_VENCIDA` (Ir pagando con saldo y sin abonar hace más de 30 días). **Pendiente de definir:** artículo sin foto.

**Fuera de la v1 a propósito (falsas alarmas probables, se decide con el dueño):**
- *Total de venta ≠ total del pedido:* si se edita un pedido ya pagado, "lo vendido no se recalcula" (R4), así que puede
  diferir legítimamente.
- *`total_pagado` de pedidos de contado:* se guarda distinto según el camino (venta directa = total; cobro con tarjeta
  del módulo nuevo = 0). Hasta unificarlo no se revisa.
- *Pedido cancelado que sigue teniendo venta:* falta confirmar qué hace hoy cancelar un pedido ya pagado con su venta.

**Pruebas:** dominio (puertos simulados) + adaptador con H2 (una prueba por regla, con datos que SÍ y datos que NO
deben salir) + `mvn test` completo. Front: `ng build`; pruebas manuales en `TESTS_PENDIENTES.md` del front.
**Permisos:** acción `reporte-revision` en la pantalla `reportes` (migración `migration_accion_reporte_revision.sql`,
**solo ROLE_ADMIN**, no se da a los demás roles con "Ver reportes"); el endpoint además exige ADMIN en `SecurityConfig`.

## ETAPA 2 — Gráficas nuevas (por orden de valor; cada una con su plan breve antes de programar)
**Hechas el 2026-11-01:** (subidas a dev y qa)  la 1 (ventas por forma de pago, con comisión) y la 2 (ventas, gastos y ganancia neta por mes), en el **Dashboard**. Dominio `hexagonal/graficasventas`. **Segunda tanda (subida):** 3 (por cobrar), 5 (pedidos por estado) y 6 (capital parado). **Tercera tanda (rama `feature/graficas-3`, sin subir):** 7 (ventas por día y hora), 8 (clientes nuevos/recurrentes y top) y 9 (entregas por zona). **Quedan:** 4 (cuadre del día, necesita ventas nuevas) y 10 (salud del cobro con tarjeta, espera Mercado Pago).

1. ✅ **Ventas por forma de pago** (dona): efectivo, tarjeta, transferencia; contado contra meses; **comisión de tarjeta**
   (lo que se queda la terminal) usando `detalle_pago` / `ventas.ganancia_total`.
2. ✅ **Ventas, gastos y ganancia neta por mes** (barras + línea, 12 meses): `ventas`, `gastos_surtir`.
3. ✅ **Dinero por cobrar por antigüedad** (barras 0–7, 8–30, 31+ días) de Ir pagando y Apartados.
4. **Cuadre del día**: cobrado hoy en efectivo / tarjeta / transferencia + abonos, contra lo vendido.
5. ✅ **Pedidos por estado** (embudo): pendientes, pagados por entregar, entregados, cancelados, atrasados.
6. ✅ **Capital parado**: artículos con stock y sin ventas en 60 días.
7. ✅ **Ventas por día de la semana y por hora** (mapa de calor): cuándo vender o hacer lives.
8. ✅ **Clientes nuevos contra recurrentes** y top clientes.
9. ✅ **Entregas por zona** (`lugares_entrega`).
10. **Salud del cobro con tarjeta**: aprobados, rechazados, vencidos, sin aplicar (cuando Mercado Pago esté activo).
Dónde van: tarjetas/gráficas ligeras en **Dashboard** (hoy solo tiene números) y las de análisis en pestañas nuevas de
**Reportes**, con una acción por pestaña (patrón `migration_accion_reportes.sql`).

## ETAPA 3 — Herramienta externa (opcional)
**Metabase** (gratis, autoalojado) conectado a la base de **QA** con un usuario de **solo lectura**, para que el dueño
explore ventas sin pedir una pantalla por pregunta. No sustituye la Revisión de ventas (que sí conoce las reglas del
negocio). Se evalúa después de la Etapa 1.

## Reglas del dueño que se mantienen
Todo en español; sin commit/push hasta que lo ordene; nunca a `main`/`master` sin orden; sin tokens en chat ni docs;
verificar el despliegue de QA antes de decir "ya está"; el front no lleva tests nuevos (se anotan en
`TESTS_PENDIENTES.md`) y cada cambio se documenta en su `CLAUDE.md`.
