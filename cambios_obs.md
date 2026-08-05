Quiero desarrollar un proyecto profesional desde cero llamado AI OBS Assistant.

No quiero generar código sin entenderlo. Quiero diseñar primero toda la arquitectura del sistema y después implementar cada módulo paso a paso.

==================================================
OBJETIVO DEL PROYECTO
==================================================

Construir un asistente inteligente que pueda conectarse a OBS Studio mediante WebSocket para monitorear transmisiones en vivo.

El sistema deberá obtener información de OBS, capturar imágenes del programa en vivo, enviarlas a una IA para analizarlas y posteriormente responder con observaciones, recomendaciones o automatizaciones.

El objetivo final es crear un asistente que funcione como un copiloto durante transmisiones y grabaciones.

==================================================
BACKEND
==================================================

Lenguaje

Java 21

Framework

Spring Boot

Arquitectura

Arquitectura Hexagonal (Ports & Adapters)

Patrones

SOLID

Clean Code

DTO

Builder

Repository

Service

Facade

Factory (cuando sea necesario)

Strategy (cuando sea necesario)

Dependency Injection

==================================================
DEPENDENCIAS
==================================================

Spring Web

Spring Validation

Spring Data JPA

Spring Security (más adelante)

Lombok

MapStruct

MySQL Driver

OBS WebSocket Java Client

Jackson

OpenAI SDK (más adelante)

JUnit

Mockito

Docker

==================================================
BASE DE DATOS
==================================================

Inicialmente utilizar MySQL.

Diseñar las tablas necesarias para:

Configuración

Conexiones

Sesiones

Capturas

Análisis

Prompts

Respuestas

Historial

Logs

Eventos

No crear tablas innecesarias.

Cada tabla deberá justificarse antes de implementarse.

==================================================
MÓDULOS DEL SISTEMA
==================================================

Configuración

Conexión con OBS

Escenas

Fuentes

Capturas

IA

Historial

Usuarios

Configuraciones

Automatizaciones

Logs

==================================================
API REST
==================================================

Diseñar una API REST profesional.

Cada módulo deberá tener:

Controller

Service

Repository

DTO Request

DTO Response

Mapper

Entidad

Validaciones

Excepciones

Documentación

==================================================
CONEXIÓN CON OBS
==================================================

Aprender paso por paso:

1. Conectarse al WebSocket.

2. Verificar autenticación.

3. Obtener la versión.

4. Obtener escenas.

5. Obtener escena activa.

6. Obtener fuentes.

7. Saber si está grabando.

8. Saber si está transmitiendo.

9. Cambiar escenas.

10. Capturar imágenes.

==================================================
INTELIGENCIA ARTIFICIAL
==================================================

Más adelante integrar GPT para:

Analizar imágenes.

Describir objetos.

Reconocer productos.

Dar recomendaciones.

Responder preguntas.

Generar títulos.

Generar descripciones.

Ayudar durante transmisiones.

==================================================
FRONTEND
==================================================

Utilizar React.

Diseñar una interfaz moderna tipo dashboard.

Pantallas iniciales:

Login

Dashboard

Estado de OBS

Escenas

Fuentes

Vista previa

Capturas

Resultados IA

Configuración

Logs

Historial

==================================================
DISEÑO DEL DASHBOARD
==================================================

Barra lateral izquierda.

Panel central con la vista previa.

Panel derecho con respuestas de IA.

Indicadores superiores:

Conectado a OBS

Grabando

Transmitiendo

FPS

Escena actual

Botón para capturar imagen.

Botón para enviar a IA.

==================================================
FASES DEL PROYECTO
==================================================

FASE 1

Arquitectura

FASE 2

Conexión con OBS

FASE 3

API REST

FASE 4

Capturas

FASE 5

Integración IA

FASE 6

Dashboard React

FASE 7

Automatizaciones

FASE 8

Docker

FASE 9

Deploy

==================================================
FORMA DE ENSEÑANZA
==================================================

No quiero copiar código.

Quiero aprender.

Antes de escribir código explicar:

Qué problema resuelve.

Por qué se necesita.

Cómo funciona.

Qué patrones utiliza.

Qué ventajas tiene.

Después implementar paso por paso.

No avanzar al siguiente tema hasta comprender completamente el anterior.

Actúa como un arquitecto de software senior y un mentor, guiándome para construir un proyecto profesional, escalable y mantenible.




respuesta
# SOFTWARE DESIGN DOCUMENT
# AI STREAM ASSISTANT

==================================================
VISIÓN DEL PROYECTO
==================================================

El objetivo del proyecto es desarrollar un asistente inteligente para transmisiones en vivo, creación de contenido y atención automática a clientes.

El sistema deberá ejecutarse inicialmente de forma local utilizando Java y Spring Boot, integrándose con OBS Studio para monitorear transmisiones, analizar imágenes mediante Inteligencia Artificial y ayudar al creador durante un live.

El proyecto deberá ser escalable para que en el futuro pueda funcionar como una plataforma profesional.

==================================================
OBJETIVO PRINCIPAL
==================================================

Construir un asistente que pueda:

• Conectarse a OBS Studio.
• Conocer el estado de una transmisión.
• Analizar imágenes del programa en vivo mediante IA.
• Crear marcadores durante el live.
• Ayudar en la edición posterior del video.
• Detectar productos.
• Registrar eventos importantes.
• Ayudar a responder automáticamente mensajes de clientes.
• Integrarse con redes sociales.

==================================================
ALCANCE DEL PROYECTO
==================================================

FASE 1

Conexión con OBS.

FASE 2

Captura de imágenes.

FASE 3

Análisis mediante IA.

FASE 4

Historial de transmisiones.

FASE 5

Marcadores.

FASE 6

Dashboard.

FASE 7

Automatizaciones.

FASE 8

Integración con redes sociales.

==================================================
CASOS DE USO
==================================================

CASO DE USO 1

Conectar OBS.

El usuario abre el sistema.

El sistema detecta OBS.

Obtiene escenas.

Obtiene fuentes.

Obtiene el estado del streaming.

Obtiene el estado de grabación.

==================================================

CASO DE USO 2

Capturar imagen.

El usuario pulsa Capturar.

El sistema obtiene una imagen del programa.

La almacena.

La envía a IA.

La IA devuelve un análisis.

==================================================

CASO DE USO 3

Marcadores.

Durante la transmisión el usuario puede crear un marcador manual.

También podrán generarse marcadores automáticamente mediante IA.

Cada marcador deberá almacenar:

• Sesión.
• Tiempo del video.
• Hora real.
• Producto.
• Tipo de evento.
• Observaciones.

Los marcadores servirán posteriormente para:

• Localizar rápidamente un momento del video.
• Editar Reels.
• Buscar ventas.
• Encontrar clientes.
• Localizar comentarios importantes.

==================================================

CASO DE USO 4

Edición.

Cuando termina una transmisión.

El usuario podrá abrir una sesión.

Visualizar todos los marcadores.

Ir directamente al minuto correspondiente.

Crear clips.

Exportar contenido.

==================================================

CASO DE USO 5

Análisis IA.

La IA podrá detectar:

Productos.

Colores.

Marcas.

Errores.

Encuadres.

Productos agotados.

Texto.

Objetos.

También podrá generar:

Descripción.

Título.

Hashtags.

Ideas para publicaciones.

==================================================

CASO DE USO 6

Asistente para ventas.

El sistema podrá responder automáticamente preguntas frecuentes.

Ejemplos:

Precio.

Disponibilidad.

Envíos.

Horarios.

Ubicación.

Formas de pago.

Cuando la IA no pueda responder deberá transferir la conversación al usuario.

==================================================

CASO DE USO 7

Integración con Redes Sociales.

El sistema deberá ser modular para soportar futuras integraciones.

Facebook.

Instagram.

TikTok.

WhatsApp.

YouTube.

Cada integración deberá implementarse mediante un módulo independiente.

==================================================
ARQUITECTURA
==================================================

Backend

Java 21

Spring Boot

Arquitectura Hexagonal

REST API

WebSocket

DTO

Repository

Service

Mapper

Builder

Validation

SOLID

Docker

==================================================

Frontend

React

Dashboard

Tiempo real mediante WebSocket

==================================================

Base de Datos

MySQL

Flyway

==================================================

ENTIDADES PRINCIPALES
==================================================

LiveSession

Marker

Product

Capture

AIAnalysis

Prompt

Response

Configuration

SocialAccount

Conversation

Customer

Sale

Log

==================================================

TABLA MARKER
==================================================

Cada marcador deberá almacenar:

Id

LiveSession

Producto

Tiempo del video

Hora real

Tipo

Observaciones

Miniatura

Estado

Los marcadores permitirán localizar rápidamente cualquier evento ocurrido durante una transmisión.

==================================================

RESPUESTAS AUTOMÁTICAS

En el futuro el sistema deberá poder responder automáticamente mensajes provenientes de:

Facebook.

Instagram.

TikTok.

WhatsApp.

La IA responderá utilizando información del negocio.

Antes de responder deberá consultar:

Catálogo.

Precios.

Stock.

Promociones.

Preguntas frecuentes.

Todas las respuestas deberán quedar registradas para auditoría.

Cuando la confianza de la respuesta sea baja, la conversación deberá enviarse al usuario para revisión.

==================================================

OBJETIVO FINAL

Construir un asistente inteligente capaz de ayudar durante transmisiones en vivo, editar contenido posteriormente y atender automáticamente clientes de diferentes redes sociales utilizando Inteligencia Artificial.



                  Cliente

                     │

                     ▼

        Facebook / Instagram / TikTok

                     │

                     ▼

                 Webhook

                     │

                     ▼

        AI Commerce Assistant

     ┌────────────┼────────────┐

     ▼            ▼            ▼

Conversation   Product      Knowledge

Service         Service        Base

     │            │            │

     └────────────┼────────────┘

                  ▼

             GPT / IA

                  ▼

            Respuesta

==================================================
ANÁLISIS Y DECISIONES — sesión de arquitectura (2026-08-05)
==================================================

Este bloque es continuación del documento de arriba, para retomar el análisis
sin repetir preguntas ya resueltas.

--------------------------------------------------
DECISIONES YA CONFIRMADAS
--------------------------------------------------

1. Relación con proyecto_key: el nuevo sistema CONSUME la API de proyecto_key
   para catálogo/stock/clientes/ventas. No se duplican tablas Product/Customer/
   Sale propias — se evita desincronización de stock real vs. lo que muestra
   el asistente durante el live.

2. Mono-usuario para el MVP. Sin tabla Usuario por ahora. Multi-usuario se
   agrega después si hace falta (no se descarta, se posterga).

3. El foco real del MVP NO es el análisis de imágenes por IA — es el sistema
   de MARCADORES. La IA sobre imágenes/Capture/AIAnalysis queda para una fase
   posterior, no es lo que se necesita usar primero.

4. Flujo real de trabajo (el motivo de los marcadores):
   - Equipo: Mac (corre OBS) + iPhone (para marcar rápido).
   - OBS transmite EN SIMULTÁNEO a TikTok, Facebook e Instagram — es UNA sola
     sesión de OBS aunque salga a 3 plataformas (no se necesita una sesión por
     plataforma para este feature).
   - La app se conecta a OBS por WebSocket para saber que la sesión está
     activa y desde cuándo empezó (para poder calcular offsets de tiempo).
   - Durante el live: ve algo en el chat (en cualquiera de las 3 plataformas)
     → entra a la app desde el iPhone → toca "Marcar". Debe ser instantáneo,
     un tap, sin formulario — no hay tiempo de escribir mientras está en vivo.
   - El marcador se crea "en blanco" (solo sesión + tiempo_video + hora_real).
     Se enriquece DESPUÉS (tipo, observación, producto) cuando revisa con
     calma ya terminado el live.
   - Al terminar: revisa los marcadores de esa sesión, completa detalle, y
     los usa para ir directo al minuto en la grabación / cortar clips.

5. Diseño de tablas mínimo para este MVP (no las 13 entidades del documento
   completo — esas son de fases muy posteriores):

   sesion    -> id, inicio, fin, ruta_grabacion, estado (en_vivo / finalizada)
   marcador  -> id, sesion_id, tiempo_video, hora_real,
                tipo_evento (nullable), observaciones (nullable),
                producto_id (nullable, referencia a proyecto_key), 
                miniatura (nullable), estado (pendiente/revisado)

--------------------------------------------------
PREGUNTAS ABIERTAS — pendientes de respuesta del usuario
--------------------------------------------------

P1. ¿OBS va a grabar en LOCAL además de transmitir a las 3 plataformas?
    Necesario para que "tiempo_video" tenga sentido (ir directo al minuto
    requiere un archivo local al cual saltar). OBS soporta stream + record
    simultáneo de forma nativa.

P2. El botón "Marcar" en el iPhone — ¿página web simple (abrir navegador y
    tocar un botón) o PWA/app instalable? No cambia las tablas, sí cambia el
    alcance de la primera fase de frontend.

--------------------------------------------------
TEMA EN DISCUSIÓN — conectar Facebook / Instagram / TikTok para que el
chatbot responda automáticamente en el chat
--------------------------------------------------

(Ver respuesta del arquitecto en la conversación — pendiente de anotar
conclusión final una vez decidido el enfoque.)

--------------------------------------------------
ORDEN DE TRABAJO SUGERIDO (revisar antes de empezar a codear)
--------------------------------------------------

1. Resolver P1 y P2 de arriba.
2. Esqueleto Spring Boot + hexagonal, sin lógica.
3. Cliente OBS WebSocket: conectar, autenticar, saber si está grabando/
   transmitiendo, obtener hora de inicio de sesión.
4. Endpoint mínimo de marcar (crear marcador "en blanco") + endpoint de
   listar/enriquecer marcadores de una sesión.
5. Página simple de "Marcar" accesible desde el iPhone.
6. Recién después: Capturas + IA + redes sociales + dashboard completo.

==================================================
REDES SOCIALES — FACEBOOK: DECISIONES DE LA SESIÓN
==================================================

--------------------------------------------------
QUÉ DEVUELVE EL WEBHOOK DE COMENTARIOS (Graph API, campo "feed")
--------------------------------------------------

Al suscribirse al campo "feed" de la Página, cada comentario nuevo llega así:

  field: "feed"
  value.item: "comment"
  value.verb: "add" | "edit" | "remove"
  value.comment_id: id del comentario
  value.post_id: id de la publicación comentada
  value.parent_id: id del padre (post o comentario padre)
  value.message: TEXTO del comentario (lo que escribió el cliente)
  value.from: { id, name } de quien comentó
  value.created_time / edited_time

El webhook NO trae la descripción/caption original de la publicación — solo
el comentario. Para leer la caption hay que hacer una llamada aparte:
GET /{post_id}?fields=message  (requiere pages_read_engagement, mismo
permiso que ya se pide para leer comentarios, no requiere permisos extra).

--------------------------------------------------
PUBLICAR DESDE LA APP (Opción B — CONFIRMADA, es la que se va a construir)
--------------------------------------------------

- Fotos: POST /{page-id}/photos con la imagen (url o archivo) + message
  (caption). Responde { "id": "<photo_id>", "post_id": "<post_id>" }.
- Videos: endpoint distinto (Video API), subida por partes para archivos
  grandes — flujo diferente al de fotos, no es "lo mismo pero con video".
- Permisos: pages_manage_posts + pages_manage_engagement (+ publish_video
  para video). Estos permisos requieren App Review de Meta — no es
  instantáneo, hay que iniciarlo con tiempo.
- PROGRAMAR PUBLICACIÓN: agregar published=false + scheduled_publish_time
  (timestamp Unix). Ventana permitida: mínimo 10 minutos, máximo 6 meses
  en el futuro. Confirmado para /feed; para fotos/video se asume igual mismo
  patrón pero falta confirmar con fuente directa antes de construirlo.
  Historias probablemente NO se puedan programar (contenido efímero) —
  falta confirmar.

--------------------------------------------------
ADVERTENCIA TÉCNICA — historias y reels no son "iguales" a foto/video normal
--------------------------------------------------

- Reels: sí tienen comentarios públicos como un video normal, sin problema.
- Historias: NO tienen comentarios públicos — las reacciones llegan como
  respuesta directa (Messenger), API distinta (permiso pages_messaging,
  fuera del alcance actual). Si se publican historias, el bot de
  comentarios NO les va a aplicar igual que a foto/video/reel.

--------------------------------------------------
DECISIÓN — el chatbot de redes sociales NO es el mismo que el del sitio
--------------------------------------------------

Se revisó el código real de ChatbotService.java / ChatbotController.java
(el que ya existe para el sitio web). Diagnóstico:

| Punto | Chatbot actual (web) | Nuevo (Facebook) |
|---|---|---|
| Precio | Sí lo da | Nunca lo da |
| Contexto al IA | Todo el catálogo en cada call | Solo el producto vinculado a esa publicación |
| Conversación | Multi-turno con historial | Un comentario -> una respuesta, sin hilo |
| Salida | Texto + ##BUSCAR## (tarjetas con imagen en el sitio) | Texto plano (comentario de respuesta) |
| Control abuso | Por IP (cooldown + bloqueo 30h, ChatbotBlockService) | Por facebook_usuario_id + publicacion_id (tabla respuesta_bot_comentario) |
| Fallback | ##FAREWELL## + bloqueo de IP | "En un momento nos comunicamos contigo" |

CONCLUSIÓN: se reutiliza solo la infraestructura de bajo nivel (WebClient
hacia OpenAI, config de api-key/modelo, manejo de timeout). El prompt, el
contexto y el flujo son un servicio NUEVO y SEPARADO (ej.
RedesSocialesChatbotService) — el ChatbotService actual del sitio NO se
toca, se queda exactamente como está.

El bot de Facebook responde con base en la DESCRIPCIÓN del producto
(analiza la pregunta del cliente contra esa descripción, ej. "¿le caben
los celulares?" -> si la descripción lo menciona, contesta que sí). Nunca
menciona precio. Si no tiene información suficiente para responder, cae al
mensaje fijo de "en un momento nos comunicamos contigo" — no inventa.

--------------------------------------------------
TABLAS NUEVAS PARA ESTA PARTE (redes sociales)
--------------------------------------------------

publicacion_social
  id
  variante_id            -> referencia a la variante de proyecto_key
  plataforma              -> facebook (por ahora)
  tipo_publicacion         -> foto / historia / reel / video
  descripcion_publicada     -> texto final editado por el usuario antes de
                               publicar (pre-llenado por default concatenando
                               variante.descripcion + código de barras; NO
                               sobreescribe variante.descripcion, se guarda
                               aparte)
  ruta_media               -> imagen o video real subido (puede ser el que
                               ya tiene la variante, o uno nuevo cargado
                               desde PC / cámara / video, sin comprimir,
                               resolución completa que acepte Facebook)
  post_id_facebook           -> el que devuelve la Graph API al publicar
  scheduled_publish_time (nullable) -> si se programó para después
  fecha_publicacion

respuesta_bot_comentario
  id
  publicacion_id          -> a qué publicación pertenece
  facebook_usuario_id      -> "from.id" de quien comentó (del webhook)
  tipo_respuesta            -> saludo_generico | descripcion_producto
  fecha

Regla de uso: antes de contestar un comentario, se revisa si ya existe fila
con esa combinación publicacion_id + facebook_usuario_id. Si existe, no se
vuelve a contestar (evita que el bot repita saludo/respuesta a la misma
persona en la misma publicación). Si no existe, contesta y se guarda.

--------------------------------------------------
PANTALLA NUEVA — publicar variante a redes sociales
--------------------------------------------------

1. Buscar variante (ej. "pantalón") -> resultados con nombre, marca, talla,
   color, descripción, código de barras (reutiliza la misma consulta que
   ya usa ChatbotService.buscarProductos).
2. Elegir una -> se pre-llena "descripción a publicar" concatenando
   descripción de la variante + código de barras. Editable libremente.
3. Media: por default la imagen ya guardada de la variante, con opción de
   subir imagen nueva desde PC, tomar foto, o subir video -- sin comprimir.
4. Publicar (o programar) -> se guarda post_id_facebook devuelto por Graph
   API, vínculo con el producto garantizado desde el momento de publicar.

--------------------------------------------------
PENDIENTES POR VERIFICAR ANTES DE CONSTRUIR
--------------------------------------------------

- Límites reales de tamaño/resolución de imagen y video que acepta la
  Graph API de Facebook.
- Si /{page-id}/photos y el endpoint de video soportan published=false +
  scheduled_publish_time igual que /feed.
- Si las historias se pueden programar o publicar vía API en absoluto.
- Confirmar con documentación si hay alguna forma de comentarios/engagement
  en historias (aunque la primera revisión dice que no, va por Messenger).