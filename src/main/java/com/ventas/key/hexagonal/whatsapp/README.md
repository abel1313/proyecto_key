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
demás bots (`REGLAS_DE_RESPETO`). Se presenta **solo en el primer mensaje**, con el texto único `PoliticaDeRespuestaWhatsapp.PRESENTACION` (R22). Si el
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

**R12 — Horario de atención (2026-11-27).** Hay una persona disponible solo dentro del horario del negocio
(Sistema → Negocio y contactos: hora de apertura y de cierre, la de apertura cuenta y la de cierre no; mismo horario
todos los días; hora de México). Cuando el cliente pide una persona, manda un archivo que el bot no lee o el bot no
puede contestar: **dentro del horario** el bot avisa que lo atienden, se aparta (R10), la conversación queda marcada y
se avisa por correo (R13). **Fuera del horario** el bot NO se aparta ni promete que "en un momento" lo atienden: le dice
el horario y le ofrece seguir con el asistente o esperar; la conversación queda marcada para verla al abrir, sin correo.
**Horario con días (2026-11-27):** lunes a viernes tiene su horario y sábado/domingo puede tener otro (`hora_apertura_fin_semana` /
`hora_cierre_fin_semana` en `configuracion_negocio`, migración `migration_horario_fin_semana.sql`; vacíos = igual que entre semana).
El bot dice los días tal cual; «¿abierto ahora?» (`HorarioDeAtencion.abiertoEn(Instant)`) usa el horario del día.
**Ubicación (2026-11-27):** el bot dice SOLO dónde es (la dirección), nunca manda liga de mapa; si quiere ver la ubicación o
cómo llegar, lo manda a `{tienda}/login` (la aplicación la muestra).
**No inventa (2026-11-27):** si no sabe un dato (precio, existencia, medida, fecha, política, promoción) no lo supone:
dice que una persona se lo confirma y escribe `##HUMANO##`, dentro o fuera del horario.
Si el negocio no tiene horario, se trata como abierto y el bot no da ninguna hora. El bot solo puede decir ese horario.
Fuera del horario, el modelo recibe «ahorita no hay nadie» y no debe escribir `##HUMANO##`; si aun así lo escribe y el cliente
insiste, el aviso del horario **no se repite**: se le confirma que lo atienden desde la apertura. Si el asistente falló (no
fue el cliente quien pidió una persona), el texto no pregunta «¿qué prefieres?». El horario se lee del negocio con caché de 1 min.
El «¡Hola!» y la presentación salen solo en el primer mensaje.

**R13 — Aviso por correo.** Cuando una conversación necesita a una persona dentro del horario, se manda un correo a
`chat.admin-email` (variable `CHAT_ADMIN_EMAIL`, el mismo del chat en vivo) con el cliente, el motivo y su último
mensaje. Si el correo falla, no pasa nada más: la conversación ya quedó marcada en la pantalla. Se manda en un hilo aparte (una cola de 50): un SMTP lento nunca retiene el procesamiento del mensaje.

**R14 — Solo se muestran artículos con imagen.** El catálogo que ve el bot de WhatsApp deja fuera los artículos sin
imagen (igual que la tienda en línea). Vive en `ChatbotWhatsappService.filtrarCatalogoDelCanal`; el gancho de
`ChatbotBase` devuelve la lista tal cual, así que los demás bots no cambian.

**R15 — Entregas, ubicación y privacidad en este canal.** El bot no habla por su cuenta de fines de semana ni da
fechas: pregunta en qué lugar se entrega; si es una zona de la lista, dice que una persona se pondrá en contacto para
acordar día y hora; si no, que ahí no se entrega y que puede recoger. La ubicación del local la da con los datos del negocio (R22; solo si no hay dirección ni mapa remite a la liga del
login de la tienda); el aviso de privacidad con la liga de `/privacidad`.

**R16 — El bot genera el Apartado: la IA propone, el sistema decide (fase 2, 2026-11-27).** Interruptor
`whatsapp.bot.pedidos-habilitado` (`WHATSAPP_BOT_PEDIDOS_HABILITADO`, **apagado por defecto**): apagado, el bot ni menciona los
pedidos y cualquier marca que escriba el modelo se borra antes de llegar al cliente. Encendido, el modelo, cuando ya tiene los
datos, termina su respuesta con `##PEDIDO{json}##` (artículos por su «ref», nombre, teléfono, correo, quién recibe, dirección,
lugar, observaciones, «ir pagando»). **El modelo nunca decide un precio, un total, el resumen ni la confirmación:** el sistema
lee la marca (`InterpreteDeMarcas`, Java puro, sin Jackson), valida los datos (`ValidadorDeSolicitud`), busca cada artículo en
la base y arma él mismo el **resumen** (`TextosDePedido`) con precios de la base. Ese resumen se guarda como borrador
(`BorradoresDePedidoPort`, en memoria, un borrador por conversación, caduca a los 30 min) **solo cuando el resumen de verdad
salió hacia el cliente** (el mensaje devuelto por `EnvioConVentanaService` quedó `ENVIADO`; si Meta lo rechazó o la ventana se
cerró no queda ningún borrador vivo y la conversación se marca) y con la **hora de envío**. El «sí» lo decide `Confirmacion` con
reglas fijas, no la IA: solo vale un sí limpio escrito **después** de que salió el resumen (el `enviadoEn` del mensaje del
cliente debe ser al menos 3 s posterior al envío; un «sí» o «ok» escrito antes, por ejemplo la respuesta a «¿este es tu
correo?», no cuenta: se le contesta «responde solo SÍ para confirmar» y el resumen sigue vivo; un «sí» nunca se pierde en silencio). Un texto con «?» (`ok?`, «¿ya está listo?») nunca confirma: se contesta
«responde solo SÍ» y el resumen sigue vivo. «sí pero en talla M» tampoco. El borrador se usa **una sola vez** al confirmarse o
al contestar otra cosa; se descarta también si el bot se pausa, si una persona toma la conversación («Tomar») o si contesta desde la pantalla (aunque el bot reanude después). Un «sí» que llega cuando ya no hay
resumen (venció o el servidor se reinició) y el último mensaje del bot era la pregunta «¿Confirmas tu pedido?» se contesta con un
texto fijo («tu resumen venció»): nunca se deja al modelo, que podría decir «quedó apartado». Si entre el resumen y el «sí» cambió
el precio de algo, no se crea nada ni se manda correo de error: se arma un resumen nuevo con los precios de hoy; si algo se agotó,
se dice sin números. Si el modelo escribe `##PEDIDO` y `##HUMANO##` juntos, gana la persona y el pedido se descarta; el JSON
debe empezar inmediatamente después de `##PEDIDO`. Si el servidor se reinicia a media conversación el borrador se pierde y el
cliente solo repite su «sí» (el bot vuelve a proponer); es un costo aceptado para no crear tablas ni migración.

**R17 — Qué se valida antes de proponer.** Todo en `PedidoPorWhatsappService.proponer`: artículo que existe en la base, **no**
de catálogo interno (flores), habilitado (él y su producto), **con imagen** y con stock para la cantidad pedida (si el mismo
artículo viene dos veces se suman); precio = el que cobra la tienda hoy (`Variantes.precioACobrar`, nunca el que diga la IA);
nombre ≥ 3 letras, teléfono de 10 dígitos (el de WhatsApp si el cliente confirmó usarlo, o el que dio; **sin teléfono no se
cierra el pedido** y se le explica por qué), correo con formato, nombre de quien recibe, dirección y **lugar de entrega: solo uno
del catálogo de zonas** (`lugares_entrega`, comparado sin acentos ni mayúsculas); si es otro: «ahí no llevamos» y se ofrece
recoger en el local con la lista real de zonas. Recoger en el local no pide dirección. Cualquier cantidad es válida mientras
haya stock (tope técnico 999 por línea solo contra cifras absurdas). Lo que falta se le pide con un texto fijo; el pedido no se
crea. Máximo **10 artículos distintos** por pedido (el resumen tiene que caber en un mensaje de WhatsApp con su pregunta de
confirmación y el aviso de privacidad; los nombres largos se recortan a 80 caracteres). Cantidades y ids de la marca: solo enteros
limpios (`"1 o 2"` o `"-7"` se rechazan). Si el número de WhatsApp no es de México (no se puede sacar un teléfono de 10 dígitos),
el modelo no debe proponerlo: se pide el teléfono una sola vez y, si falta otra vez, se pasa a una persona (sin bucle).
Datos previos: **solo** los de pedidos hechos desde ESE mismo número de WhatsApp (se reconocen por las observaciones
«Pedido hecho por WhatsApp +número»); el teléfono que alguien escribió en su propio pedido no cuenta, así el correo de A nunca se
le propone a V. El modelo los ve **enmascarados** (`a***@correo.com`, dirección solo el inicio) y, si el cliente confirma, escribe
`"usarDatosPrevios":true`: el sistema pone los reales. Si no hay datos de ese número, se piden desde cero. Lo que escribió el cliente
se limpia (sin saltos de línea ni caracteres de control, con tope de largo) antes de guardarse.

**R18 — El pedido se crea con el servicio real de ventas, nunca con un INSERT ni con un endpoint nuevo.**
`PedidosApartadoVentaAdapter` llama a `VentaServiceImpl.saveVentaDetalle` (el mismo de «Venta directa») con tipo `APARTADO`,
un cliente sin registro y el precio de la base; así el stock, la validación de precio, las reglas del Apartado, el caché y la
versión de las listas de pedidos los pone el código de siempre. No hay ninguna ruta HTTP nueva: nadie de afuera llega a ese
camino. El precio que se muestra en el resumen es el de la base (`precioACobrar`); **el cobro final puede ser menor** si el servicio aplica
una campaña vigente o un precio de grupo por cantidad («gana el más bajo»). Por eso el mensaje «¡Listo!», el ticket al cliente y el
correo al dueño se arman con las líneas y el total **REALES** que devuelve el servicio (`resp.getRenglones()`), nunca con las del
resumen. Todo (cliente + pedido) va en una sola transacción. Si el mismo teléfono ya tiene un cliente sin registro con el
mismo nombre y correo, se reutiliza; si no, se crea uno nuevo (con el correo **sin verificar**: se confirmó en la conversación,
no con un código). El «vendedor» que pide el servicio es la cuenta `whatsapp.bot.usuario-id` (`WHATSAPP_BOT_USUARIO_ID`); con 0
(por defecto) se usa la primera cuenta ROLE_ADMIN activa. Un Apartado no genera venta; solo se valida que la cuenta exista.

**R19 — Siempre Apartado, siempre sin dinero.** El bot nunca anota dinero. Si el cliente dice que quiere ir pagando o dar un
enganche, igual nace Apartado, y las observaciones dicen «El cliente quiere Ir pagando: cambiar a Ir pagando cuando llegue el
enganche»; si no lo dijo, esa nota no existe. Observaciones del pedido: «Pedido hecho por WhatsApp +<número>» (+ la nota del
cliente, + la de Ir pagando). El campo «link» del pedido (`url_facebook`) lleva la liga directa a su WhatsApp
(`https://wa.me/<número>`). El administrador abona a mano cuando ve la transferencia (reglas de pedidos, 2.1). Si se pasó el
tiempo y el artículo se agotó entre el resumen y el «sí», no se crea nada y se le dice al cliente («ya no está disponible en esa
cantidad»); eso no es un error del sistema y no manda correo.

**R20 — Correos y fallos.** Pedido creado: ticket al correo del cliente y correo al administrador (`chat.admin-email`) con la
venta; ninguno tira el pedido si falla. El ticket sale en otro hilo (R20c): el bot dice «te mandamos el ticket» cuando el correo quedó en la cola; si el SMTP falla después, solo queda en el log y la conversación se marca para que una persona lo revise. Si falla **algo del sistema** al
crear: no se crea nada, el bot le dice que una persona lo contactará, la conversación queda marcada y al administrador le llega
un correo con lo ocurrido y los datos que ya se tenían (sin tokens ni claves). Un fallo de la IA o una marca mal escrita no crea
nada: se le pide al cliente que repita sus datos.

**R20b — Tope de Apartados por número.** `whatsapp.bot.max-apartados-abiertos` (`WHATSAPP_BOT_MAX_APARTADOS_ABIERTOS`, **0 = sin
tope**, que es el valor por defecto porque el dueño aún no decide; se recomienda 3). Con un valor mayor a 0 se cuentan los Apartados
abiertos (no cancelados, no entregados) hechos por el bot desde ese número y, si ya llegó al tope, no se crea otro, se pasa a una
persona y se avisa por correo; se revisa al proponer y otra vez al confirmar.

**R20c — Los correos del pedido salen en otro hilo (el ticket «sale» cuando se encola)** (uno solo, cola corta, se descarta si se llena), igual que el aviso de R13: un SMTP
lento no retiene el candado del número. El ticket dice «te mandamos» cuando quedó en la cola. En el log no se escriben nombres.
El texto de fallo dice «te va a contactar muy pronto» solo si hay una persona en horario; fuera de horario da el horario.

**Límites conocidos de R17/R20b (decisiones del dueño ❓):** (1) el tope y los datos previos reconocen los pedidos por las
observaciones «Pedido hecho por WhatsApp +número»; **editar esas observaciones saca el pedido de esos conteos**. El estado se
compara sin distinguir mayúsculas. (2) Con un **número de WhatsApp reasignado** a otra persona, el nuevo dueño vería el correo y
la dirección de la anterior si usa `usarDatosPrevios`: al modelo solo llegan enmascarados y los datos reales aparecen únicamente
en el resumen que el cliente confirma, pero sigue siendo un riesgo de privacidad aceptado por ahora. (3) El tope de Apartados está
apagado (0) hasta que el dueño decida su valor (recomendado 3). Un correo con «*» (el enmascarado) nunca se acepta como real:
se pide de nuevo.

**R21 — Cancelar.** El bot **no** busca ni cancela pedidos y no da ninguna información de ellos (ni si existen, ni estado, ni
total). Si el cliente quiere cancelar, pide el número del pedido (el que le llegó en el correo del ticket), escribe
`##CANCELAR[número]##` y el sistema solo anota el número, avisa al administrador por correo y marca la conversación. No se
comprueba que el pedido sea de esa persona: lo revisa el administrador antes de cancelar.

**R22 — Datos del negocio, ubicación ≠ entregas y presentación (2026-11-27).** (a) El bot **lee** de Sistema → Negocio y
contactos la dirección, el punto del mapa (latitud/longitud), el horario y los links de WhatsApp, Facebook, Instagram y TikTok, y
los pone en el prompt en la sección «DATOS DEL NEGOCIO» (`ChatbotWhatsappService.seccionDatosDelNegocio`) con sus valores
exactos; la liga del mapa es `https://www.google.com/maps/search/?api=1&query=<lat>,<lng>` (solo con coordenadas válidas). Un dato
que no existe **no se menciona ni se inventa**; solo se listan las redes configuradas. Con dirección o mapa, el bot **nunca** dice
que no hay ubicación; sin ninguna de las dos no afirma ni niega un local: remite a `{{TIENDA}}/login`. La lectura es **solo de
lectura** (`InformacionDelNegocioPort` → `InformacionDelNegocioJpaAdapter`, un `findAll` ordenado, sin pasar por
`NegocioService.obtenerConfig()` que puede crear o borrar filas) y con caché de 1 min; a diferencia de `getEstado()`, las redes se
leen siempre, no solo con el negocio cerrado. Si la lectura falla, el bot sigue sin esos datos (remite al login). (b) **Ubicación
y entregas son dos preguntas distintas**: «¿dónde te ubicas / dónde están / cómo llego?» → ubicación del local (no se listan las
zonas de entrega); «¿entregan en X? / ¿hacen envíos?» → zonas de entrega. Las reglas y los ejemplos están en `REGLAS_WHATSAPP` y
en la sección de datos. (c) **La presentación vive en un solo lugar:** `PoliticaDeRespuestaWhatsapp.PRESENTACION`
(**decidido por el dueño el 2026-11-27: «Te atiende Jade, la asistente virtual de Novedades Jade 💖»**; debe seguir diciendo que es virtual, no
una persona). La usan `saludo(...)` (todas las respuestas del dominio y los textos de fuera de horario) y `PRIMER_MENSAJE`
(instrucción al modelo). Para cambiarla se edita **solo esa constante**. Opciones que propuso el dueño: «Soy la ayuda virtual de
Novedades Jade», «Soy Jade, la ayuda virtual de Novedades Jade», «Te atiende Jade, la asistente virtual de Novedades Jade».
(d) **Si los envíos fallan** (p. ej. error 190 de token vencido), `esPrimeraVez` se calcula con mensajes **ENVIADOS**, así que el
bot cree que es la primera vez y se vuelve a presentar en cada mensaje; se arregla el token, no esta lógica.

## Capas

```
dominio/          Java puro (sin Spring): VentanaDe24Horas, PoliticaDeEnvio, PoliticaDeRespuestaWhatsapp,
                  modelos, puertos · fase 2: InterpreteDeMarcas, ValidadorDeSolicitud, Confirmacion, TextosDePedido
aplicacion/       EnvioConVentanaService (único camino de salida) · RecibirMensajeWhatsappService (webhook) ·
                  AdministrarConversacionesService (pantalla) · PedidoPorWhatsappService (fase 2: el Apartado)
infraestructura/
  entrada/rest    WhatsappWebhookController (/v1/whatsapp/webhook, público con firma)
                  WhatsappConversacionesController (/v1/whatsapp/..., pantalla admin/whatsapp)
  salida/         persistencia (JPA) · meta (WhatsappCloudClient) · chatbot · abuso · correo · negocio ·
                  pedido (fase 2: catálogo, lugares, borradores en memoria, crear el Apartado con VentaServiceImpl, correos)
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
