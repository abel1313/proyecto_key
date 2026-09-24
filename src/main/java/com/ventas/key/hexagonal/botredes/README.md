# Dominio: botredes (el bot que contesta en Facebook e Instagram)

Contesta **comentarios** y **mensajes directos** en **Facebook e Instagram**. Reemplaza a
`FacebookCommentBotService`, `InstagramCommentBotService` e `InstagramDirectMessageBotService`
(migrados el 2026-09-24, cuando hubo que cambiarles las reglas y sumar Messenger).

Las reglas las definió el dueño el 2026-09-24. El detalle de la conversación está en
`ALTA_NEGOCIO_META_TIKTOK.md`, sección 9.

## Reglas

**R1 — Nunca deja a nadie sin respuesta, y siempre con cortesía.** Si el bot no puede contestar
él, saluda, avisa al admin por correo y se aparta (R5).

**R2 — Comentarios: el bot solo habla de la publicación.**
- **Publicación subida desde el panel admin** (ligada a un producto en `publicacion_social`):
  contesta sobre **ese producto** (precio, talla, color, stock…). Si le preguntan algo que no está
  en los datos del producto, o por otra cosa, escala.
- **Publicación subida directo** en Facebook o Instagram: el bot **no sabe de qué producto es**.
  A saludos, halagos y avisos ("bonito", "ya te sigo") contesta con un agradecimiento. A cualquier
  pregunta contesta **solo un saludo** y escala.
- Si no entiende el comentario: contesta un saludo cordial.

**R3 — Mensajes directos: el bot usa todo el catálogo.** Si no sabe algo o no entiende: saluda y
escala. Foto, audio o sticker sin texto: saluda y escala.

**R4 — La primera vez que le escribe a una persona se presenta** como "el asistente automático
de Novedades Jade" (política de Meta). Las siguientes veces, no.

**R5 — Pausa de 30 minutos.** El bot se aparta de una conversación cuando la atiende una persona:
- cuando **el bot escala** (saluda una vez y manda el correo), o
- cuando **el admin contesta a mano** desde la app de Facebook o Instagram.

La pausa dura 30 minutos **desde la última** de esas dos cosas; cada respuesta del admin la
reinicia. Mientras dura, el bot no contesta ni manda correos. Pasado ese tiempo, si la persona
vuelve a escribir, el bot la retoma con estas mismas reglas. Configurable con
`redes.bot.pausa-minutos`.
- En comentarios la conversación es **persona + publicación**. En mensajes directos es **la persona**.

**R6 — Nunca se contesta a sí mismo**, ni procesa dos veces el mismo evento (Meta a veces los reenvía).

**R7 — Control de abuso:** bloqueos, cooldown y 20 mensajes por hora por persona
(`ChatbotBlockService`), con la clave por red + persona.

## Casos que ya se sabe que pasan

- **El eco del bot puede llegar antes de guardar su respuesta.** En mensajes directos el servicio
  lleva en memoria a quién le está contestando en ese momento, para no confundir su propio eco con
  una respuesta del admin. Alcanza con un solo pod.
- **Si el chatbot falla** (OpenAI caído, timeout), se escala: el cliente recibe el saludo y el admin
  el correo.
- **Messenger** necesita el permiso `pages_messaging` en el token de la página y la página suscrita
  al campo `messages`. Sin eso, Meta no manda los mensajes y el bot no los puede contestar.
