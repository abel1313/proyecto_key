# Plan — el bot de WhatsApp (seguimiento)

> Documento vivo: aquí se anota lo que se decidió, lo que ya está y lo que falta. Reglas técnicas del dominio:
> `src/main/java/com/ventas/key/hexagonal/whatsapp/README.md` (R1–R15). Conexión con Meta: `WHATSAPP_CLOUD_API.md`.
> Última actualización: 2026-11-27. **Aquí nunca van tokens, IDs de la cuenta ni el número** (repositorio público).

Leyenda: ✅ hecho y probado · 🟡 hecho en local, sin subir · ⏳ pendiente · ❓ falta decidir

## 1. Objetivo
El cliente escribe por WhatsApp → contesta el bot (24/7) → si pide una persona y es horario de atención, pasa a una
persona; si el cliente quiere comprar, el bot toma los datos y **genera el pedido** (Apartado) para que el
administrador lo revise y se contacte. Lo demás lo hace una persona desde **Sistema → WhatsApp**.

## 2. Decisiones del dueño (ya no se vuelven a preguntar)
| Tema | Decisión |
|---|---|
| Bot | Contesta 24/7. Solo habla de lo acordado; todo lo demás está bloqueado. |
| Horario de atención con persona | El **horario del negocio** (Sistema → Negocio y contactos), el mismo todos los días. Si no hay horario configurado, el bot no da ninguno. |
| Pide un admin dentro del horario | Pasa a una persona: el bot se aparta, la conversación queda marcada y llega **correo**. |
| Pide un admin fuera del horario | El bot NO pasa la conversación: dice el horario con amabilidad y pregunta si quiere seguir con él. Queda marcada para verla al abrir, **sin correo**. |
| Si el administrador no contesta | El bot retoma tras 30 min (pausa de R10). |
| Fuera de tema / abuso | Los mismos bloqueos del chat de la tienda (20 mensajes por hora, cooldown). |
| Entregas | El bot **no** habla de fines de semana por su cuenta. Pregunta **en qué lugar** se lo llevamos; solo los lugares del catálogo de zonas. Si está: «una persona se pondrá en contacto para acordar día y hora». Si no: no se entrega ahí, puede recoger en el local. |
| Ubicación del local | El bot da la liga del login de la tienda (ahí está la ubicación). Sin mapa por WhatsApp. |
| Privacidad | El bot menciona el aviso de privacidad (`/privacidad`) cuando pida datos o pregunten por ellos. |
| Pagos | Solo efectivo y transferencia. |
| Productos que muestra | Solo con **stock e imagen** (como la tienda en línea). |
| Precio | **Nunca lo decide el bot**: sale del artículo en la base. |
| Pedido creado por el bot | Siempre **Apartado** (sin dinero). Si el cliente dice que quiere **Ir pagando**, igual se crea Apartado y en observaciones va la nota «cambiar a Ir pagando cuando llegue el enganche». Si no lo dice, sin esa nota. El bot **nunca anota dinero**: el administrador abona a mano cuando ve la transferencia. |
| Cantidad | Se puede pedir cualquier cantidad con los datos completos. No caduca solo: el administrador cancela a mano si el cliente ya no contesta. |
| Cuál artículo | El bot busca y muestra opciones. Si dicen «quiero este» y no dan código ni nombre, el bot pregunta cuál; propone **uno solo** y espera un sí. Antes de crear, repite el resumen y espera confirmación. |
| Datos del pedido | Nombre (obligatorio) · teléfono (el de WhatsApp si confirma, o el que den; sin teléfono no se cierra y se les explica) · correo (obligatorio, confirmado con «¿este es tu correo?»; le llega el ticket) · nombre de quien recibe · dirección · lugar de entrega (del selector) · link (liga directa a su WhatsApp) · observaciones («lo pidió por WhatsApp»). Si ya dio sus datos antes, se reutilizan. |
| Después de crear | Le llega el ticket al cliente por correo y **al administrador el correo de la venta**. |
| Si algo falla | No se hace la venta y llega un correo al administrador con lo ocurrido y los datos que ya se tenían. |
| Cancelar / buscar pedidos | El bot **no busca ni cancela pedidos**. Si el cliente quiere cancelar, pide el número del pedido que le llegó por correo, lo anota y avisa al administrador, que cancela. |
| Campañas | El bot explica la campaña vigente (nombre, fechas, meta, juguetes con precio de campaña). Si el cliente ya cumple la meta, se le da el precio de campaña. ❓ cómo se aplica automático (ver fase 3). |
| Otros bots | **No se modifican** (sitio web, chat en vivo, Facebook, Instagram). |

## 3. Fases
### Fase 0 — Conexión ✅
- [x] Back y front en `dev` y `qa`; migración corrida en QA; variables cargadas; webhook verificado con Meta.
- [x] Pantalla Sistema → WhatsApp (lista, chat, ventana de 24 h, plantillas, tomar/devolver al bot).
- [x] Prueba real: el mensaje entra y el bot contesta.

### Fase 1 — Horario, correo e imagen 🟡
- [x] Horario de atención (R12), aviso por correo (R13), solo artículos con imagen (R14), entregas/ubicación/privacidad (R15).
- [x] Pruebas temporales con datos simulados (35/35).
- [x] Revisión independiente (agente revisor, 2026-11-27): 12 hallazgos. Corregidos: 1 (aviso repetido fuera de horario), 2 y 3 (promesas y contradicciones del prompt de entregas), 4 (correo en hilo aparte + tiempos SMTP en dev), 8 (motivo correcto), 9 (horario con caché de 1 min), 10 (asunto limpio), 11 (ligas de la tienda por ambiente), 12 («¡Hola!» solo la primera vez). Sin corregir, ver abajo.
- [ ] Subir a `dev` y `qa` (solo cuando el dueño diga «sube»).
- ❓ **Botón «Abrir/Cerrar negocio» (hallazgo 5):** hoy el bot usa solo la hora de apertura y cierre. Si el dueño cierra el negocio a mano un martes a las 12:00, el bot sigue pasando conversaciones a una persona; y si lo abre fuera de horario, dice que no hay nadie. ¿El bot debe respetar también ese botón?
- ⚠️ **Catálogo recortado (hallazgo 6):** el bot lee las primeras 1000 variantes con stock y filtra por imagen después; con más de 1000 con stock, algunas con imagen podrían quedar fuera. Arreglo: consulta propia con `EXISTS` imagen. Pendiente, bajo riesgo hoy.
- ⚠️ Hallazgo 7: si no hubiera repositorio de imágenes (solo pasa en pruebas) se muestra todo; si la consulta falla, el bot recibe «Catálogo temporalmente no disponible».
- [ ] En QA con mensajes reales: dentro y fuera de horario, correo, artículo sin imagen.
- [x] **Ajuste tras la prueba de QA del dueño (2026-11-27, R22):** el bot confundía «¿dónde te ubicas?» con las zonas de entrega y decía que no había ubicación. Ahora lee dirección, mapa, horario y redes del negocio (solo lectura, caché 1 min), separa ubicación de entregas en las reglas, y la presentación vive en una sola constante (`PoliticaDeRespuestaWhatsapp.PRESENTACION`, hoy «Soy la ayuda virtual de Novedades Jade 💖», ya sin «asistente automático»).
- [x] **Presentación decidida (2026-11-27):** «Te atiende Jade, la asistente virtual de Novedades Jade 💖» (constante `PRESENTACION`). Ubicación («dónde te ubicas / tienes local / algo físico») y entregas («dónde entregas / zonas / envíos») se separan; los datos del negocio y las redes se dan siempre, esté abierto o cerrado.
- [ ] En QA: con la dirección capturada, «¿dónde te ubicas?» da dirección + mapa + horario sin listar zonas; «¿entregan en Tejupilco?» da las zonas; el token de WhatsApp vigente (si falla el envío, el bot se vuelve a presentar en cada mensaje).

### Fase 2 — El bot genera el Apartado 🟡 (construida el 2026-11-27, sin subir)
- [x] Flujo: identificar artículo → confirmar → pedir datos → confirmar correo → resumen → crear (R16 a R19). El modelo solo propone (`##PEDIDO{..}##`); el sistema valida, arma el resumen con precios de la base y espera el «sí» (regla fija, no IA).
- [x] Crear el pedido con el servicio real de ventas (`saveVentaDetalle`, nunca un INSERT ni un endpoint nuevo), tipo Apartado, con datos de entrega y observaciones (R18, R19).
- [x] Ticket al cliente y correo al administrador; error → no se crea + correo con lo ocurrido (R20).
- [x] El precio de catálogo siempre de la base (el cobro final puede bajar por campaña o precio de grupo y lo que se informa al cliente al final, el ticket y el correo al dueño es lo REAL que cobró el servicio); valida existencia, habilitado, no interno, **imagen** y stock; sin dinero.
- [x] Reutilizar los datos de un teléfono que ya los dio (se le proponen al cliente para confirmar; se reutiliza su registro de cliente sin cuenta).
- [x] Cancelar: pide el número, lo anota y avisa por correo (R21). *(Se adelantó de la fase 3 porque costó poco.)*
- [x] Probado con programas temporales: 178 comprobaciones del dominio y los servicios con puertos falsos (+5 de adaptadores), y de punta a punta con el back completo arrancado sobre H2 (crea el Apartado de verdad con `VentaServiceImpl`).
- [x] Revisión independiente de la fase 2 (2026-11-27): hallazgos **corregidos** el mismo día, cada uno con su prueba temporal:
  - C1 resumen sin leer / sin salir (borrador solo si el resumen salió ENVIADO, hora de envío y «sí» anterior ignorado) ✅
  - I1 total y líneas reales en ticket, «¡Listo!» y correo; precio que cambió → se vuelve a proponer sin correo de error ✅
  - I2 sin número de piezas ✅ · I3 «sí» con resumen perdido → texto fijo + regla «NUNCA digas que quedó hecho» ✅
  - I4 «?» nunca confirma, máximo 8 palabras, ambiguo conserva el borrador ✅ · I5 borrador descartado al pausarse el bot ✅
  - I6 tope `whatsapp.bot.max-apartados-abiertos` (0 = sin tope) ✅ (ver ❓ abajo) · I7 datos previos solo del mismo número de WhatsApp y enmascarados ✅
  - M1 máximo 10 líneas y resumen que cabe en 4096 ✅ · M2 enteros limpios ✅ · M3 correos asíncronos y sin nombres en el log ✅
  - M4 «ref» en todas sus formas ✅ · M5 gana la persona y el JSON debe ir pegado a `##PEDIDO` ✅ · M6 (fase 1) el tercer intento también recibe el aviso corto ✅
  - M7 número no mexicano sin bucle, vendedor determinista, `CerebroWhatsappAdapter` con `Clock` ✅ · M8 fallo fuera de horario, `descartado()` borrado, README/plan corregidos ✅
- [x] Verificación del revisor (13/16 bien) y cierre de lo que faltaba: I5 completo (también al contestar o «Tomar» desde la pantalla), N1 (un «sí» temprano recibe «responde solo SÍ», nunca silencio), N2 (correo con «*» rechazado), N4 (estado sin distinguir mayúsculas) y documentación coherente (M3, N3) ✅
- [ ] Segunda revisión independiente (si el dueño la quiere).
- [ ] Subir a `dev` y `qa` (solo cuando el dueño diga «sube»). **Interruptor apagado por defecto:** en QA poner `WHATSAPP_BOT_PEDIDOS_HABILITADO=true` (y, si se quiere, `WHATSAPP_BOT_USUARIO_ID` con la cuenta que figura como vendedor; con 0 se usa la primera ROLE_ADMIN activa).
- [ ] En QA con mensajes reales y la IA de verdad: flujo completo, un teléfono que ya pidió antes, lugar fuera de la lista, artículo sin imagen, cantidad mayor al stock, correo saliendo de verdad.

**Decisiones de diseño tomadas (reversibles) y preguntas para el dueño:**
- ❓ **Campaña / precio de grupo:** `saveVentaDetalle` puede bajar el precio por una campaña vigente o por un grupo de precio por cantidad. El resumen muestra el precio de catálogo; el «¡Listo!», el ticket y el correo al dueño muestran las líneas y el total REALES que cobró el servicio (y el mensaje dice que «te quedó mejor por una promoción»). El cliente puede ver dos totales; se pulirá en la fase 3 (campañas). ¿Está bien mientras tanto?
- ❓ **Número de WhatsApp reasignado (N3):** con `usarDatosPrevios` el nuevo dueño del número vería el correo/dirección de la persona anterior (al modelo van enmascarados; los reales solo salen en el resumen que confirma). Riesgo conocido, sin cambio. ¿Se acepta?
- ❓ **Tope de Apartados abiertos por número (hallazgo I6):** `whatsapp.bot.max-apartados-abiertos` (`WHATSAPP_BOT_MAX_APARTADOS_ABIERTOS`) está en **0 = SIN tope**: hoy un solo número podría apartar todo el stock, porque los Apartados no caducan y el ticket sale a un correo sin verificar. **Recomendación: 3.** ¿Qué valor pone el dueño?
- ❓ **Recoger en el local:** no se pide dirección. ¿Está bien, o se pide igual el nombre de quien recoge? (Hoy sí se pide «quién recibe»; el bot usa el nombre del cliente si es él.)
- ❓ **Correo sin verificar:** el cliente confirma su correo en la conversación, pero no con un código. El ticket sale a ese correo. ¿Se acepta o se quiere verificarlo (más fricción)?
- ❓ **Cancelar:** el bot no comprueba que el pedido sea de esa persona (no da información de pedidos). El administrador revisa antes de cancelar. ¿Está bien?
- ❓ **Cuánto dura el resumen:** 30 min (`whatsapp.bot.pedido-vigencia-minutos`). ¿Más o menos?
- Borrador del pedido en memoria (no en tabla): si el servidor se reinicia a media conversación, el cliente repite su «sí». Se evitó una migración. Si se quiere que sobreviva a reinicios, hace falta una tabla.
- El pedido lleva como «vendedor» la cuenta ROLE_ADMIN activa con el id más bajo (o `WHATSAPP_BOT_USUARIO_ID`). Un Apartado no genera venta, solo valida que exista. ¿Se quiere una cuenta propia del bot?
- Un correo de aviso al administrador por cada pedido (a `CHAT_ADMIN_EMAIL`, el mismo del aviso de «necesita una persona»). Los administradores con cuenta no reciben copia.
- El mensaje del pedido (resumen y confirmaciones) lo escribe el sistema con texto fijo, no la IA: se ve menos «conversacional» a cambio de que precio y confirmación nunca dependan del modelo.

### Fase 3 — Campañas y cancelación ⏳
- [ ] El bot explica la campaña vigente. ❓ Aplicar el precio de campaña solo si la persona ya cumple la meta (se busca por teléfono). Revisar `hexagonal/campana` antes.
- [ ] Cancelar: pedir el número de pedido, anotarlo y avisar al administrador (el bot no cancela).

### Fase 4 — Fotos ⏳
- [ ] El bot manda la foto del artículo con pie (nombre, precio) para confirmar «¿es este?». Solo dentro de las 24 h.
- [ ] Mostrar en la pantalla las fotos y audios que manda el cliente (cuidar el limpiador nocturno de imágenes).

### Fase 5 — Plantillas, pago y producción ⏳
- [ ] «BAJA»: que un cliente pueda dejar de recibir promociones (antes de aprobar la plantilla de promoción).
- [ ] Mandar a aprobación las plantillas (autorización del dueño). Método de pago en Meta.
- [ ] Producción: migración → variables → despliegue (el número tiene **una sola** URL de webhook).

## 4. Acciones del dueño
- [ ] Poner el horario en Sistema → Negocio y contactos: apertura 10:00, cierre 18:00. ⚠️ El mismo horario cierra solo el negocio a esa hora.
- [ ] Verificar que `CHAT_ADMIN_EMAIL` apunta a su correo (por defecto `admin@novedades-jade.com.mx`).
- [ ] Confirmar que el token cargado es el **permanente** (Usuarios del sistema), no el temporal de 24 h.
- [ ] Agregar el método de pago en Meta cuando quiera usar plantillas.

## 5. Cómo se trabaja (agentes)
1. **Agente desarrollador (Sonnet)** construye una fase en una copia aislada del repo y la documenta.
2. **Agente revisor (independiente)** revisa sin haber visto las pruebas del desarrollador; no edita, solo reporta.
3. Se corrigen los hallazgos, se pasan las pruebas y **solo entonces** se pide «sube».
4. La validación final es en QA con mensajes reales.
