# Dominio: whatsapp (el bot y la bandeja de WhatsApp con la API de Meta)

Recibe los mensajes que los clientes escriben al WhatsApp del negocio, los guarda, deja que conteste el
bot (el mismo cerebro del chatbot de la tienda, con reglas propias de WhatsApp) y permite que una persona
los conteste a mano desde la pantalla **Sistema → WhatsApp** (`admin/whatsapp`).

No reemplaza a `botredes` (Facebook e Instagram): `botredes` está hecho para comentarios y mensajes
directos con una pausa de 30 min; WhatsApp necesita historial, la ventana de 24 h, plantillas y respuesta
manual, que ahí no encajan. Sí **reusa** de `botredes` el filtro de "algo interno" (`PoliticaDeRespuesta`),
el cerebro (`ChatbotBase`) y el control de abuso (`ChatbotBlockService`).

Cómo se configura y se conecta con Meta: `WHATSAPP_CLOUD_API.md` (raíz del repo). Plantillas: `WHATSAPP_PLANTILLAS.md`.

## Reglas

**R1 — La ventana de 24 h se abre con cada mensaje ENTRANTE y se cuenta desde el ÚLTIMO.** Se guarda por
cliente (`whatsapp_cliente.ultimo_entrante_at`) con la hora que manda Meta en el webhook, no la hora en que
lo procesamos ni la de nuestra respuesta. Se actualiza con cada mensaje entrante y **nunca hacia atrás**
(si Meta entrega dos mensajes en desorden, gana el más reciente). Si el cliente escribe de nuevo, aunque sea
a la hora 25 o después, se abre una ventana nueva. Vive en `VentanaDe24Horas`.

**R2 — Mensaje libre (texto, imagen, enlace, botones) solo con la ventana abierta**, o sea con **menos de
24 h** desde el último mensaje del cliente. Con 24 h o más **no** se envía mensaje libre. Se valida justo
antes de enviar, con la hora de ese momento y la marca más reciente del cliente (se vuelve a leer), no la de
cuando empezó a procesarse. Todo envío (bot y personas) pasa por `EnvioConVentanaService`; no hay otra puerta.

**R3 — Fuera de la ventana solo se puede enviar una plantilla aprobada por Meta, y solo con método de pago
válido en la cuenta.** Si no hay plantilla o no hay método de pago, no se envía nada. El método de pago no se
puede consultar por API: se declara con `WHATSAPP_METODO_PAGO_ACTIVO` (por defecto `false`) y se pone en
`true` solo cuando Meta ya lo tenga y haya plantillas aprobadas.

**R4 — Todo queda en el historial, enviado o no, con su motivo** (`whatsapp_mensaje.estado` y `motivo`):
`ENVIADO`, `ENTREGADO`, `LEIDO`, `FALLIDO` (con `codigo_error`), `BLOQUEADO_VENTANA`, `BLOQUEADO_SIN_PAGO` y
`PENDIENTE_PLANTILLA`. Un mensaje que no salió marca la conversación como "requiere atención".

**R5 — Si el bot tarda y, cuando su respuesta está lista, ya pasaron 24 h** del mensaje del cliente, no se
manda mensaje libre: la respuesta queda `PENDIENTE_PLANTILLA` (con el borrador a la vista) y la conversación
queda marcada para una persona. Si el mensaje del cliente ya llega con más de 24 h (Meta lo entregó tarde),
ni siquiera se consulta a la IA.

**R6 — Error 131047 ("Re-engagement message") y cualquier otro rechazo de Meta: se registra y NO se
reintenta, nunca en bucle.** Vale igual si el error llega al enviar (HTTP 400) o después, en el aviso
`statuses: failed` del webhook. Reenviar lo decide una persona. Tampoco se reintenta ante un tiempo agotado
(el mensaje pudo haber salido y se duplicaría).

**R7 — Un mismo mensaje no se procesa dos veces.** Meta reenvía. `whatsapp_mensaje.wa_message_id` es único en
la base: el primero gana y los reenvíos se ignoran, incluso si llegan al mismo tiempo.

**R8 — El webhook valida la firma, responde 200 y procesa aparte.** `X-Hub-Signature-256` (HMAC-SHA256 del
cuerpo exacto con el App Secret); sin App Secret configurado todo se rechaza. Se responde 200 enseguida y el
mensaje se procesa en un pool aparte. Los mensajes de un mismo número se procesan de uno en uno. Si la cola se
llena responde 503 y Meta reintenta (R7 evita duplicados). Alcanza con un solo pod, igual que `botredes`.

**R9 — El bot de WhatsApp (`ChatbotWhatsappService`).** Mismas reglas de respeto y de "nada interno" que los
demás bots (`REGLAS_DE_RESPETO`). Se presenta como asistente automático **solo en el primer mensaje**. Si el
cliente pide una persona, o falta un dato, el bot lo dice con amabilidad y marca `##HUMANO##`: se manda el
saludo (sin la marca) y la conversación pasa a una persona. Foto, audio, sticker, ubicación o contacto sin
texto: no van a la IA, se saluda y pasa a una persona. Si la IA falla o menciona algo interno, también.
**Fase 1: solo texto** (nada de prometer fotos; manda a la tienda en línea). Las reglas propias de este canal
se editan en `REGLAS_WHATSAPP` y `seccionMostrarProductos()` de esa clase.

**R10 — Quién atiende.** Cuando una persona contesta desde la pantalla, el bot se aparta
`whatsapp.bot.pausa-minutos` (30 por defecto), y cada respuesta suya reinicia la cuenta (igual que R5 de
`botredes`). «Tomar» apaga el bot hasta que alguien pulse «Devolver al bot». Mientras el bot está apartado,
los mensajes del cliente solo se guardan y marcan la conversación.

**R11 — Control de abuso:** los mismos bloqueos, cooldown y 20 mensajes por hora del chat del sitio
(`ChatbotBlockService`), con la clave `whatsapp:<número>`. Sin respuesta del bot, el mensaje igual se guarda.

## Capas

```
dominio/          Java puro (sin Spring): VentanaDe24Horas, PoliticaDeEnvio, PoliticaDeRespuestaWhatsapp,
                  modelos, puertos
aplicacion/       EnvioConVentanaService (único camino de salida) · RecibirMensajeWhatsappService (webhook) ·
                  AdministrarConversacionesService (pantalla)
infraestructura/
  entrada/rest    WhatsappWebhookController (/v1/whatsapp/webhook, público con firma)
                  WhatsappConversacionesController (/v1/whatsapp/..., pantalla admin/whatsapp)
  salida/         persistencia (JPA) · meta (WhatsappCloudClient) · chatbot · abuso
  config          WhatsappBeans
```

Las entidades JPA (`WhatsappClienteEntidad`, `WhatsappMensajeEntidad`) y sus repositorios están en
`mis/productos/entity` y `mis/productos/repository` porque el escaneo de JPA solo cubre ese paquete.
Todas las fechas se guardan **en UTC**; la API las entrega en ISO-8601 con «Z».

## Casos que ya se sabe que pasan

- **El número del cliente se contesta tal como llegó.** En México, Meta ha mandado el número con y sin el «1»
  después del 52; se responde al mismo valor que trajo el webhook (`from`), sin normalizarlo.
- **Un aviso `statuses` de un mensaje que no es nuestro** (o de otro número) se ignora.
- **Reacciones y avisos del sistema** no se contestan.
- **Respuesta a un botón de plantilla o a una lista** se lee como si el cliente la hubiera escrito.

## Pendiente (fases siguientes)

- Fotos del catálogo con pie y botón «Ver en la tienda» (hoy el bot solo manda texto y el enlace de la tienda).
- Mostrar en la pantalla las fotos y audios que manda el cliente.
- Aviso por correo al administrador cuando una conversación necesita a una persona (hoy solo la marca).
- «BAJA»: que un cliente pida no recibir más plantillas de promoción (requisito de las plantillas de marketing).
