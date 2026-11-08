# Pruebas QA — Campañas: el flujo completo (QR → cliente, modal, meta por fechas, ticket) — 2026-11-08

**Antes de empezar (en este orden):**
1. En QA, correr `migration_campana_persona_cliente.sql` (agrega 2 columnas a `campana_persona`; solo agrega). **Hazlo ANTES de que termine el deploy del back**: sin ella la campaña se apaga sola y las ventas siguen, pero no hay códigos.
2. Esperar el deploy del back y del front de QA, **volver a entrar** y tener una campaña **Vigente** (Catálogo → Campañas) con meta $1,500, fecha de inicio **hoy o antes**, y 2 juguetes con precio de campaña.
3. Para ver el correo, usa un cliente con **tu correo**. Para el QR con el celular: el celular de siempre (el de los códigos de barras) con sesión de la misma cuenta.

## Mapa de impacto
| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 1 | Ventana de «Cobrar» sin cliente | Venta directa sin campaña | clic fuera = cobra a nombre del administrador | clic fuera/Esc **ya no cobra**; sin campaña la ventana se puede cerrar (y no pasa nada) |
| 2 | Receptor de códigos del celular | **Buscador de códigos de barras** de Venta directa, Buscar, Agregar y Actualizar producto | todo código va al buscador | los códigos de barras **siguen yendo al buscador igual**; solo un UUID va al campo de campaña |
| 3 | Página del celular (`qr_code`) | Escanear códigos de barras con el celular | solo barras | barras **igual**; además lee QR |
| 4 | Ticket (Venta directa, Mis pedidos, detalle, abonos) | Imprimir / reenviar tickets | sin precio por pieza | **sin descuento sale igual**; con precio de campaña, normal tachado + etiqueta |
| 5 | Meta | «Verificar» y reporte | solo Contado ligado, sin juguetes | todo lo pagado entre inicio y fin, juguetes incluidos |

## 🔴 PRUEBA PENDIENTE — 1 Nada se rompió (sin campaña)
Apaga la campaña y haz una Venta directa normal **sin cliente** → Cobrar.
**✅** Sale «¿Agregar cliente para la rifa?». Con **clic fuera** o **Esc** la ventana se cierra y **NO se cobra**. Con «No, cobrar sin cliente» sí cobra. **⛔** Que clic fuera cobre.

## 🔴 PRUEBA PENDIENTE — 2 Primera compra: la ventana no se cierra
Enciende la campaña. Venta directa, 1 producto (bolsa) **sin cliente** → Cobrar.
**✅** «¿Registrar los datos del cliente?». **Clic fuera y Esc no la cierran.** Elige **Sí, registrar cliente**: en el modal **clic en el fondo no lo cierra**; aparece la casilla del Aviso de privacidad y el botón **No registrar y cobrar**.
Llena nombre, tu correo y teléfono, **marca la casilla** y **Guardar cliente** → (si pide verificar el correo puedes omitirlo) → se cobra.
**✅** Llega el correo «Tu código de cliente» con la liga. La liga abre el QR **sin iniciar sesión**, con botones **Descargar imagen** y **Compartir**. En MySQL: `SELECT uuid, nombre, correo, cliente_id, cliente_sin_registro_id FROM campana_persona;` → la persona con **`cliente_sin_registro_id` lleno**.
**⛔** Que se cree persona sin marcar la casilla.

## 🔴 PRUEBA PENDIENTE — 3 «No registrar»
Otra venta sin cliente → Cobrar → **No registrar** (o Sí → «No registrar y cobrar»).
**✅** Se cobra a nombre del administrador, sin persona ni correo.

## 🔴 PRUEBA PENDIENTE — 4 Segunda visita: el QR llena al cliente
Venta directa nueva, **sin elegir cliente**, con 2 juguetes y bolsas. En el bloque 🎁 pega el UUID (o 🖼️ sube la **captura/imagen del QR** que te mandaste por WhatsApp o Facebook, o 📱 con el celular, o escribe/escanea con lector USB).
**✅** Se busca solo: aparece el **cliente de la primera compra** como seleccionado, y «✓ Es de … · lleva $X de $1,500». Si ya lleva lo de la meta (contado de hoy cuenta): «ya es recurrente» y los juguetes bajan a precio de campaña, **con su precio por pieza («c/u»)**.
**⛔** Que no se llene el cliente; que no baje el precio estando recurrente.

## 🔴 PRUEBA PENDIENTE — 5 La meta (por fechas y lo pagado)
Con ese cliente: (a) una compra de **Contado** de $900 hoy; (b) un **Apartado** con un abono de $300; (c) un pedido **cancelado** de $500; (d) con la fecha de inicio de la campaña **mañana**, una compra de hoy.
**✅** Pulsa «Buscar código» y revisa «lleva $X»: suma el Contado + lo **abonado** del Apartado; **no** suma el cancelado ni lo anterior a la fecha de inicio. En una venta nueva en **Contado**, el ticket de hoy se suma para decidir; en **Apartado** no.
**⛔** Que sume lo cancelado o lo de antes del inicio.

## 🔴 PRUEBA PENDIENTE — 6 Casilla «Cliente recurrente» sin QR
Cliente nuevo (sin QR), marca **Cliente recurrente**, 2 juguetes, Contado, y registra sus datos con la casilla de privacidad.
**✅** Los juguetes bajan a precio de campaña; **no se crea persona ni sale correo**.

## 🔴 PRUEBA PENDIENTE — 7 Código de otro cliente o inexistente
Con un cliente ya elegido pega el código de **otra persona** → pregunta «¿Cambiarlo?»; «No» limpia el código. Un UUID inventado → «Ese código no existe» y precio normal. Más de 15 búsquedas en 10 minutos → «Demasiados intentos…».

## 🔴 PRUEBA PENDIENTE — 8 El celular con códigos de barras NO se rompe
En **Venta directa** con la campaña vigente: escanea un **código de barras** de producto con el celular (📱) → entra al buscador de artículos **como siempre**. Escanea el **QR** del cliente con el mismo celular → entra al campo de campaña. Repite en **Buscar producto** y **Agregar artículo** (sin campo de campaña): los códigos de barras entran como siempre.
**⛔** Que un código de barras caiga en el campo de campaña o que el QR caiga en el buscador estando el campo de campaña en pantalla. (Si el celular es Android y no lee QR con la cámara, anótalo: la imagen del QR y el lector USB sí sirven.)

## 🔴 PRUEBA PENDIENTE — 9 Ticket
Haz una venta de Contado con precio de campaña (2 juguetes + 1 bolsa) e **imprime el ticket**.
**✅** Los juguetes con «🎁 Precio de campaña» y el total normal **tachado** debajo del renglón; la bolsa sin marca. Luego, en **Mis pedidos** abre ese pedido → **Imprimir ticket**: igual marcado. Un pedido sin campaña: ticket **idéntico al de antes**.

## 🔴 PRUEBA PENDIENTE — 10 Ir pagando / reenviar código (ya existían)
Apartado con juguetes a precio de campaña → 🔁 Cambiar forma de cobro → Ir pagando: aviso y los juguetes vuelven al precio normal. En el detalle del pedido de la persona: **🎁 Enviar código de campaña**.

## Prueba técnica
Red del navegador: `POST …/v1/campanas/personas/buscar-por-codigo` → 200 con `valido`; con un usuario no administrador → 403. `POST …/v1/ventas/save` trae `renglones[]` con `precioNormal` / `precioCobrado`.
