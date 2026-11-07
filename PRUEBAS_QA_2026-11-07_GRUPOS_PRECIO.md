# Pruebas de QA — Grupos de precio por cantidad (Fase 1)
Fecha: 2026-11-07 · Diseño: `PLAN_CAMPANAS_Y_PRECIOS.md` (reglas G1 a G8) · Contrato: `CAMBIOS_FRONT.md` («Grupos de precio por cantidad»)

## Antes de empezar
1. Correr `migration_grupo_precio_cantidad.sql` en QA (se puede correr dos veces) y **volver a entrar** con el administrador.
2. Tener 4 productos con stock y precio normal conocido. En esta guía: **A $250, B $300, C $180, D $100** (los precios son de ejemplo; usa los tuyos).
3. Menú: **Catálogo → Grupos de precio por cantidad**.

## Mapa de impacto
| # | Se movió | Le pega a | Dónde se ve | Antes | Después |
|---|---|---|---|---|---|
| 1 | Cálculo de precio al guardar | Venta directa | Tienda → Venta directa | Cobraba el precio normal o la rebaja | **Igual** si no hay grupo; con grupo que junta el mínimo, el precio de grupo |
| 2 | Cálculo de precio al guardar | Carrito (cliente y admin) → Generar pedido | Tienda → Carrito | Igual | **Igual** si no hay grupo |
| 3 | Pantalla nueva | Menú | Catálogo | No existía | Aparece solo para administrador |
| 4 | Permisos | `/v1/grupos-precio-cantidad` | — | No existía | Solo administrador (cliente 403) |

## Sin grupo: debe dar exactamente lo mismo que antes
**P0 — Venta directa de 2 piezas de A, sin ningún grupo creado.** Total $500. **⛔ No puede pasar:** que cambie el total, o que falle.

## Grupos (G1 a G8)
**P1 — Crear grupo.** Nuevo grupo «Bolsas prueba», mínimo 3, sin fechas, activo, productos A ($200), B ($270), C ($150). **✅ Debe pasar:** aparece en la lista como «Vigente». **⛔** que deje guardar sin nombre, con mínimo 1, o sin productos.

**P2 — Un producto, un grupo.** Crear otro grupo con A. **✅** error «“A” ya está en el grupo “Bolsas prueba”…». **⛔** que lo permita.

**P3 — Junta el mínimo con productos distintos.** Venta directa: 1 de A, 1 de B, 1 de C. **✅** A $200, B $270, C $150 · ahorro por cantidad $110 · total **$620** (normal $730). El ticket y el pedido guardan $620. **⛔** total $730.

**P4 — No junta el mínimo.** Venta directa: 1 de A y 1 de B. **✅** precio normal ($550), sin ahorro. Agregar 1 de C → baja solo al precio de grupo. Quitar C → **vuelve** a normal.

**P5 — Piezas de otro producto no cuentan.** 1 de A, 1 de B y 1 de **D** (D no es del grupo). **✅** precio normal; D siempre normal.

**P6 — Dos piezas del mismo producto.** Crear grupo «Solo A» mínimo 2 con A $200 (antes quitar A del primer grupo). Venta de 2 de A → $400. **⛔** que sume piezas de otro grupo.

**P7 — Fechas.** Grupo con «Hasta» = ayer → «Fuera de fechas» y se cobra normal. «Desde» = hoy y «Hasta» = hoy → vigente (los extremos entran).

**P8 — Apagar y borrar.** «Apagar» → «Apagado» y precio normal. «Activar» → vuelve. «Eliminar» → sus productos a normal.

**P9 — Apartado.** Venta directa como **Apartado** con 3 piezas del grupo (G8). **✅** el Apartado se guarda con el precio de grupo. Revisar el total en Mis pedidos.

**P10 — Carrito.** Con un cliente: agregar 3 piezas del grupo → el carrito muestra el precio de grupo tachado y el total correcto → «Generar pedido». **✅** el pedido sale con el mismo total.

**P11 — El back no se deja engañar.** (Solo si se puede con la consola del navegador.) Mandar una venta con precio de grupo cuando **solo** lleva 1 pieza → error «precio no es valido». 

**P12 — Permisos.** Con un usuario **cliente**: `Catálogo → Grupos de precio…` no aparece y la dirección directa lo manda al inicio; el carrito **sí** calcula el precio de grupo.

**P13 — Total y reportes.** El **Corte de caja**, la ganancia y el ticket de una venta con precio de grupo cuadran con lo cobrado.

## Lo que todavía NO funciona (no es error)
- Editar un pedido **ya guardado** (agregar/quitar artículos) no recalcula el precio de grupo (Fase 1b).
- El ticket impreso no muestra el ahorro por cantidad.
