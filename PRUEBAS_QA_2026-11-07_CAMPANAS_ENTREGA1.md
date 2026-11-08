# Pruebas QA — Campañas, entrega 1 (configuración) — 2026-11-07

Esta entrega **solo configura** campañas. **No cambia ningún precio ni ninguna venta todavía** (eso es la entrega 2). Por eso lo más importante es comprobar que todo lo demás sigue igual.

**Antes de empezar:** correr `migration_campana.sql` en la base de QA y **volver a entrar** (las pantallas viajan en el JWT). Entrar como administrador.

## Mapa de impacto
| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 1 | Menú Catálogo: nueva entrada «🎁 Campañas» | Menú lateral (administrador / otros roles) | no existía | solo el administrador la ve; los demás roles **lo mismo que antes** |
| 2 | `SecurityConfig`: regla `/v1/campanas/**` solo ADMIN | Todas las demás rutas | — | cliente/empleado → 403 en campañas; el resto **igual** (`AutorizacionPorEndpointTest` en verde) |
| 3 | Tablas `campana`, `campana_producto` | Borrar un producto del catálogo | se borra | se borra igual y sus filas en campañas se borran solas |
| 4 | Venta directa, carrito, Mis pedidos | — | — | **lo mismo que antes** (no leen campañas) |

## 🔴 PRUEBA PENDIENTE — 1 Menú
**Pasos:** 1. Entrar como administrador → menú **Catálogo**. 2. Entrar con otro rol sin permiso.
**✅ Debe pasar:** el administrador ve «🎁 Campañas» justo debajo de «🏷️ Grupos de precio por cantidad»; el otro rol no la ve.
**⛔ No puede pasar:** que aparezca a un rol sin permiso; que desaparezca otra entrada del menú.

## 🔴 PRUEBA PENDIENTE — 2 Crear y guardar una campaña
**Pasos:** 1. Catálogo → Campañas → **＋ Nueva campaña**. 2. Nombre «Día de Reyes 2027», Inicia 20/12/2026, Termina 06/01/2027, Meta $1,500. 3. En el catálogo escribe «juguete» (o el filtro que uses), **＋ Agregar** dos juguetes. 4. Cambia el precio de campaña de cada uno (ej. $250 → $200, $180 → $150). 5. **Guardar campaña**.
**✅ Debe pasar:** aparece la tarjeta con las fechas, «Meta $1,500.00», los dos juguetes con su precio de campaña y estado **Apagada**.
**⛔ No puede pasar:** que nazca encendida; que cambie el precio normal del producto (revísalo en Catálogo → Productos: sigue en $250 / $180).

## 🔴 PRUEBA PENDIENTE — 3 Validaciones
**Pasos:** intenta guardar (a) sin nombre, (b) con «Inicia» después de «Termina», (c) sin ningún juguete, (d) con un precio en 0.
**✅ Debe pasar:** el botón **Guardar campaña** queda desactivado y, en (b), sale «La fecha de inicio no puede ser después de la de fin».

## 🔴 PRUEBA PENDIENTE — 4 Activar, solo una a la vez, duplicar
**Pasos:** 1. **Activar** «Día de Reyes 2027». 2. Crea otra campaña «Rosca» con fechas distintas y **Activar**. 3. **Duplicar** «Día de Reyes 2027».
**✅ Debe pasar:** al activar la segunda sale el aviso «Solo puede haber una campaña encendida… se apaga» y la primera queda **Apagada**. La copia se llama «Copia de Día de Reyes 2027», **Apagada**, con los mismos juguetes y precios. El estado dice **Vigente** solo si hoy cae dentro de las fechas y está encendida; si está encendida pero fuera de fechas, dice «Encendida, fuera de fechas».
**⛔ No puede pasar:** dos campañas encendidas a la vez.

## 🔴 PRUEBA PENDIENTE — 5 Editar y eliminar
**Pasos:** 1. **Editar** una campaña, quita un juguete, cambia un precio, **Guardar**. 2. **Eliminar** la copia.
**✅ Debe pasar:** los cambios se ven al volver a la lista; la campaña editada **conserva** si estaba encendida. La eliminada desaparece y los productos siguen en el catálogo.

## 🔴 PRUEBA PENDIENTE — 6 Un producto en dos campañas
**Pasos:** pon el mismo juguete en «Día de Reyes 2027» y en «Rosca».
**✅ Debe pasar:** se permite (a diferencia de los grupos de precio por cantidad).

## 🔴 PRUEBA PENDIENTE — 7 Todo lo demás sigue igual
**Pasos:** con una campaña **encendida y vigente** que incluya un juguete, hacer una **Venta directa** de ese juguete en Contado, una en Apartado, y una compra desde el carrito.
**✅ Debe pasar:** el juguete se cobra a su **precio normal** (o a su grupo/rebaja de siempre); no aparece nada de campaña en la venta.
**⛔ No puede pasar:** que el precio de campaña se aplique (todavía no se construye).

## Prueba del cambio mismo (técnica)
- Herramientas del navegador → Red: `GET …/mis-productos/v1/campanas` responde 200 con `lista[]`; `GET …/v1/campanas/activa` responde `data: null` si no hay vigente. Con un usuario no administrador, ambos → 403.
