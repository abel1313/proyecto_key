# Pruebas QA — Campañas, entrega 2 (código de cliente y precio de campaña) — 2026-11-07

**Antes de empezar:** correr en QA `migration_campana.sql` y `migration_campana_personas.sql`, **volver a entrar** como administrador y tener una campaña **encendida y vigente** (Catálogo → Campañas) con meta $1,500 y un juguete (ej. normal $250, campaña $200) más un producto "bolsa" que NO sea de la campaña (ej. $500). Para ver el correo, el cliente de prueba debe tener un correo tuyo.

## Mapa de impacto
| # | Se movió | Le pega a | Antes | Después |
|---|---|---|---|---|
| 1 | Venta directa: bloque «🎁 Campaña» | Pantalla de Venta directa | no existía | solo sale con campaña vigente y artículos en la venta |
| 2 | Precio de las líneas en Venta directa | Contado, Apartado, Ir pagando, grupos de precio, rebajas | normal / grupo / rebaja | **igual** si no hay campaña o el cliente no es recurrente; el más bajo gana |
| 3 | `POST /v1/ventas/save` (campo `campana`) | Carrito del cliente, Mis pedidos, Corte de caja, tickets | sin campo | sin el campo todo **igual que antes** |
| 4 | Tablas `campana_persona*` | Borrar pedido / producto | se borra | se borra igual |
| 5 | Correo del código | Tickets y demás correos | — | el resto de correos **igual** |

## 🔴 PRUEBA PENDIENTE — 1 Sin campaña vigente todo igual
Apaga la campaña (Catálogo → Campañas → Apagar) y haz una Venta directa del juguete en Contado.
**✅** No sale el bloque «🎁 Campaña»; el juguete cuesta $250. **⛔** Que aparezca algo de campaña.

## 🔴 PRUEBA PENDIENTE — 2 Cliente recurrente a mano
Enciende la campaña. Venta directa: elige un cliente, agrega 2 juguetes y 1 bolsa. Marca **Cliente recurrente**.
**✅** Los juguetes pasan a $200 (cada uno muestra el $250 tachado y «ahorra $100» en total), la bolsa sigue en $500, sale «🎁 Ahorro de campaña $100». Total = $900. Cobra en **Contado**: el pedido queda con ese total.
**⛔** Que la bolsa baje de precio; que el total cobrado sea distinto al mostrado.

## 🔴 PRUEBA PENDIENTE — 3 Apartado e Ir pagando
Con el mismo carrito y el check marcado: elige **Apartado** → juguetes a $200 (el precio queda fijo en el pedido). Cambia a **Ir pagando** → vuelven a $250 y sale «⚠️ Con Ir pagando no entra el descuento de campaña».
**✅** Apartado a $200, Ir pagando a $250 con el aviso. **⛔** Descuento en Ir pagando.

## 🔴 PRUEBA PENDIENTE — 4 Primera compra: se crea el código y llega el correo
Cliente nuevo (con tu correo). Agrega bolsas por $500 (sin check). Marca **«El cliente aceptó el Aviso de privacidad…»** y cobra en Contado.
**✅** Llega un correo «Tu código de cliente» con el código y una liga. La liga abre una página con el QR y el código, **sin sesión**. En la base: `SELECT * FROM campana_persona;` hay una fila con ese correo y `consentimiento = 1`, y `campana_persona_pedido` suma $500.
**⛔** Que se cree una persona si NO marcaste la casilla; que se mande más de un correo si compra otra vez.

## 🔴 PRUEBA PENDIENTE — 5 Llegar a la meta con el código
Con ese mismo cliente: nueva Venta directa con 2 juguetes y bolsas por $1,000. Escribe/pega el código (o 📷 escanea el QR con la cámara, o 🖼️ sube/pega la imagen del QR) y pulsa **Verificar**.
**✅** Sale «✓ Es de <nombre> · lleva $500 de $1,500 · ya es recurrente» (500 + 1,000 = 1,500) y los juguetes bajan a $200. Con bolsas por $900 en vez de $1,000 NO es recurrente: «con este ticket le faltan $100» y los juguetes siguen a $250.
**⛔** Que los juguetes cuenten para la meta.

## 🔴 PRUEBA PENDIENTE — 6 Código ajeno o inexistente
Pega el código de la persona A con el cliente B (otro correo y teléfono) y Verifica; después uno inventado.
**✅** «Ese código es de otra persona» y «Ese código no existe»; los juguetes siguen a $250. Si lo intentas más de 15 veces en 10 minutos: «Demasiados intentos…».
**⛔** Que se aplique el descuento con un código ajeno.

## 🔴 PRUEBA PENDIENTE — 7 Tarjeta y combos
Venta con tarjeta (si el módulo de tarjeta está encendido) con cliente recurrente: el pedido nace Pendiente con los juguetes a $200. Una promoción/combo en la misma venta se cobra como siempre.

## Prueba técnica
Herramientas del navegador → Red: `POST …/v1/campanas/personas/verificar` responde `estado: OK | NO_COINCIDE | NO_EXISTE`. Con un usuario que no es administrador → 403.
