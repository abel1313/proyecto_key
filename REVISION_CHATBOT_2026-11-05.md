# Revisión del chatbot — 2026-11-05 (noche)

Lectura del código del chatbot del sitio (`ChatbotController`, `ChatbotRequest`, `ChatbotSitioWebService`, `ChatbotBase`, `ChatbotBlockService`). **No se probó contra la IA real** (no hay llave ni red desde aquí); para eso quedó `propuestas/pruebas/chatbot-ataques.sh` con 30 frases para correr contra QA.

## Bien (verificado en el código)
- El mensaje se limita a 1,000 caracteres y el historial a 20 mensajes de 1,000 cada uno (`@Size`), así que no se puede inflar el gasto de tokens con un solo envío.
- Del historial que manda el navegador solo se aceptan los roles `user` y `assistant`: **no se puede colar un mensaje de rol `system`**.
- La respuesta de la IA tiene tope de 500 tokens (`max_tokens`).
- Control de abuso por IP: bloqueo tras despedidas/errores repetidos, enfriamiento entre mensajes y máximo 200 mensajes por hora.
- La IP se toma del extremo **derecho** de `X-Forwarded-For` (el que agrega el proxy), no del izquierdo que el cliente puede falsificar.
- El prompt de sistema prohíbe repetir o cambiar las reglas, revelar el modelo y dar datos internos; el catálogo que recibe la IA ya viene recortado (solo precio normal, sin existencias exactas ni artículos internos) según la auditoría del 2026-11-04.

## Riesgos que quedan
| # | Gravedad | Riesgo | Propuesta |
|---|---|---|---|
| C1 | Media | **No hay tope global de gasto**: el límite es por IP (200/h). Con muchas IPs se puede gastar mucho en tokens. | Poner un límite mensual de gasto en el panel del proveedor de IA y un tope global diario en el back (contador en Redis). |
| C2 | Baja | El cliente puede **inventar mensajes de rol `assistant`** en el historial (la IA «recuerda» que ya dijo algo) para empujarla a seguir un patrón. Los datos sensibles no están en el prompt, así que el daño es de comportamiento, no de fuga. | Aceptar solo el historial que el propio servidor guardó por `sesionId`, o recortar el historial a los últimos 6 mensajes. |
| C3 | Baja | El límite por IP depende de que el proxy agregue `X-Forwarded-For`; si el back se expone directo, una persona podría falsificar el header y saltarse el límite. | Confirmar que el back solo recibe tráfico del proxy (NetworkPolicy) o usar la IP del proxy de confianza configurada en Spring (`server.forward-headers-strategy`). |
| C4 | Info | La respuesta del bot se pinta en el front: confirmar que se escapa como texto (no `innerHTML`) para que una respuesta con HTML no ejecute nada. | Revisar el componente del chat en el front (pendiente). |

## Cómo probarlo
`BASE=https://qa.backend.novedades-jade.com.mx/mis-productos propuestas/pruebas/chatbot-ataques.sh` — 30 frases (pedir costos, existencias, códigos de barras, instrucciones del sistema, datos de clientas, formato JSON, suplantación de administrador, ruegos emocionales, mensajes enormes). Marca con ⚠ las respuestas con palabras prohibidas; hay que leer las demás a ojo. Se espera 5 s entre frases por el enfriamiento.
