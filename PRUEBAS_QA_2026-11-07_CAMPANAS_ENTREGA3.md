# Pruebas QA — Campañas, entrega 3 (Ir pagando, reporte, borrar datos, reenviar código) — 2026-11-07

**Antes de empezar:** las migraciones de las entregas 1 y 2 ya corridas, una campaña vigente con meta $1,500 y un juguete (normal $250, campaña $200), y **pedidos NUEVOS** hechos después de subir esto (los de antes no guardaron su "precio de antes"). Entrar como administrador.

## Mapa de impacto
| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 1 | 🔁 Cambiar forma de cobro → Ir pagando | Mis pedidos → detalle, grupos de pedidos unidos, Créditos / Abonos | el pedido conservaba el total | con precio de campaña: el total sube; **sin** precio de campaña: **igual que antes** |
| 2 | Editar un pedido (agregar/quitar artículo, cambiar cantidad) | Precio por cantidad (grupos) | recalculaba el grupo | **igual**; no toca los juguetes con precio de campaña |
| 3 | Detalle del pedido: botón «🎁 Enviar código de campaña» | Mis pedidos → detalle | no existía | solo administrador y solo si el pedido tiene código |
| 4 | Catálogo → Campañas → Reporte | Campañas | no existía | tabla de personas |

## 🔴 PRUEBA PENDIENTE — 1 Apartado conserva, Ir pagando regresa
**Pasos:** 1. Venta directa: cliente recurrente (check), 2 juguetes ($200 c/u) + 1 bolsa $500, forma **Apartado**. Total $900. 2. Mis pedidos → ese pedido → detalle → **🔁 Cambiar forma de cobro** → elige **Ir pagando**.
**✅ Debe pasar:** antes de guardar sale «🎁 Con Ir pagando no entra el descuento de campaña…». Al guardar: juguetes a $250 c/u, total **$1,000**, y en observaciones la nota «Con Ir pagando no entra el descuento de campaña…». El botón de abonos ya calcula el saldo con $1,000.
**⛔ No puede pasar:** que el total se quede en $900; que la bolsa cambie.

## 🔴 PRUEBA PENDIENTE — 2 Cambiar entre Apartado y Contado no quita el descuento
Pedido Apartado con juguetes a $200 → **Contado** cobrando el total ($900). **✅** Sigue en $900 y no sale el aviso de Ir pagando.

## 🔴 PRUEBA PENDIENTE — 3 Pedido sin campaña: igual que antes
Un pedido Apartado normal (sin juguetes de campaña) → Ir pagando. **✅** El total no cambia, no sale el aviso, no hay nota nueva.

## 🔴 PRUEBA PENDIENTE — 4 Pedidos unidos
Dos Apartados con juguetes a $200 unidos en un grupo → cambia el **grupo** a Ir pagando. **✅** Los dos pedidos regresan a $250 y suben sus totales.

## 🔴 PRUEBA PENDIENTE — 5 Editar no deshace el precio de campaña
Pedido Apartado con juguetes a $200 que además forma parte de un grupo de precio por cantidad: agrégale otro artículo del grupo. **✅** Los juguetes siguen a $200 (si el grupo da un precio más alto) y el artículo nuevo entra a su precio normal o de grupo.

## 🔴 PRUEBA PENDIENTE — 6 Reporte
Catálogo → Campañas → tarjeta de la campaña → **Reporte**.
**✅** Encabezado «N personas con código · M llegaron a la meta de $1,500 · suman $X». Cada persona con su código, contacto, lo que lleva, # de pedidos y «Llegó/Falta»; la que más lleva, primero. Solo cuentan las ventas de **Contado**.

## 🔴 PRUEBA PENDIENTE — 7 Reenviar código
Mis pedidos → detalle de un pedido que creó un código (con correo): **🎁 Enviar código de campaña** → «Código enviado» y llega el correo. En un pedido sin código el botón **no sale**. Con una persona sin correo: aviso «no dejó correo».

## 🔴 PRUEBA PENDIENTE — 8 Eliminar datos
Reporte → **Eliminar datos** de una persona → confirmar. **✅** Desaparece del reporte; sus pedidos siguen en Mis pedidos; su código ya no verifica («Ese código no existe»); el botón «Enviar código» desaparece de sus pedidos.

## Prueba técnica
Red del navegador: `GET …/v1/campanas/{id}/reporte` y `POST …/v1/campanas/pedidos/{id}/enviar-codigo` responden 200; con un usuario que no es administrador, 403. El detalle del pedido trae `tieneJuguetesConPrecioDeCampana` / `tieneCodigoDeCampana` solo cuando es true.
