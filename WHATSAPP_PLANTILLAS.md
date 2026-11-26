# Plantillas de WhatsApp propuestas (para mandar a aprobación en Meta)

> **Todavía NO se han enviado a Meta.** Enviarlas es una acción hacia afuera (Meta las revisa y las ve su
> equipo): se hace cuando el dueño lo autorice. Hasta que Meta las apruebe **y** la cuenta tenga método de
> pago, enviarlas falla y queda registrado (R3, R4 y R6 del README del dominio).

Fuera de la ventana de 24 h (o sin que el cliente haya escrito antes) **solo** se puede mandar una plantilla
aprobada. Las plantillas de abajo salen de `PlantillasPropuestas` (código); el **nombre** tiene que coincidir
exacto con el de Meta. Idioma: español de México (`es_MX`).

## 1. `seguimiento_producto` — retomar una conversación sobre un producto

- **Categoría que se pide:** `UTILITY` (seguimiento de algo que el cliente preguntó). Meta decide la categoría
  final y puede reclasificarla como `MARKETING`, que cuesta más.
- **Variables:** `{{1}}` nombre del cliente · `{{2}}` producto por el que preguntó

**Cuerpo:**
```
Hola {{1}} 👋 Te escribimos de Novedades Jade. Hace poco nos preguntaste por {{2}}. ¿Sigues interesada(o)? Con gusto resolvemos tus dudas. Responde a este mensaje para continuar 💖
```
**Pie:** `Novedades Jade`

**Ejemplos que pide Meta al crearla:** `{{1}}` = `Ana` · `{{2}}` = `la bolsa roja de mano`

## 2. `promocion_novedades_jade` — avisar de una promoción

- **Categoría:** `MARKETING`
- **Variables:** `{{1}}` nombre del cliente · `{{2}}` texto corto de la promoción

**Cuerpo:**
```
¡Hola {{1}}! 💖 En Novedades Jade tenemos una promoción para ti: {{2}}. Conócela en https://shop.novedades-jade.com.mx/ o responde a este mensaje y te ayudamos con tu pedido.
```
**Pie:** `Si ya no quieres recibir mensajes, responde BAJA`

**Ejemplos que pide Meta al crearla:** `{{1}}` = `Ana` · `{{2}}` = `2x1 en bolsas de mano esta semana`

## Cosas a tener en cuenta

- **Solo a quien dio su consentimiento.** Meta exige que el cliente haya aceptado recibir mensajes del negocio; no
  se manda una promoción a quien nunca escribió.
- **«BAJA» todavía no hace nada.** El pie lo promete, pero el sistema aún no lo atiende (queda pendiente en el README
  del dominio). Hasta entonces, una persona debe registrar a quien lo pida y no volver a escribirle.
- Las variables no pueden llevar saltos de línea, tabuladores ni más de unos pocos espacios seguidos; el código
  los limpia (`PlantillaWhatsapp`).
- Una plantilla **rechazada o pausada** por Meta (calidad baja) deja de enviarse: el rechazo queda en el historial.
- Para mandarlas: pantalla **Sistema → WhatsApp** (plantillas), o `POST /mis-productos/v1/whatsapp/conversaciones/{id}/plantilla`.
  `GET /mis-productos/v1/whatsapp/plantillas` lista estas propuestas y dice si ya se pueden enviar.
