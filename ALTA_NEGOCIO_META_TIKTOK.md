# Alta del negocio en Meta y TikTok para contestar mensajes automáticamente

Investigado el **2026-09-24**. Es la guía para cuando se haga el trámite: qué pide cada plataforma, qué
documentos, en qué orden, cuánto tarda, por qué rechazan y qué significa cada error. Complementa a
`REDES_SOCIALES_PENDIENTE_PROD.md` (qué falta del lado del código/infra) y a `FACEBOOK_SETUP.md` /
`TIKTOK_SETUP.md` (cómo se sacaron las credenciales de QA).

**Cómo se investigó y qué tan confiable es cada dato.** El entorno donde se investigó bloquea
`facebook.com` y `tiktok.com`, así que las páginas oficiales no se abrieron completas: cada dato sale
del resumen del buscador de la página citada. Los endpoints de TikTok se confirmaron leyendo el código
de un SDK público. Cada dato lleva su marca:

| Marca | Significa |
|---|---|
| **[Oficial]** | Sale de documentación de Meta o TikTok (developers.facebook.com, facebook.com/business/help, business-api.tiktok.com, ads.tiktok.com, developers.tiktok.com) |
| **[Proveedor]** | Sale de la documentación de un proveedor que ya pasó el trámite (Chatwoot, Wati, SleekFlow, 360dialog…) o de blogs especializados |
| **[Código]** | Confirmado leyendo código (nuestro repo o un SDK) |
| **[Experiencia]** | Lo vivimos en este proyecto |
| **[Sin confirmar]** | Una sola fuente o fuente débil — verificarlo en la pantalla real antes de confiar |

Si al hacer el trámite la pantalla dice algo distinto a este documento, **gana la pantalla**: anotarlo
aquí para la próxima.

---

## 0. Resumen en una pantalla

**Meta (Instagram y Facebook).** Hoy el bot solo contesta a cuentas que tienen rol en la app (admin,
desarrollador, tester) **[Experiencia]**. Para que conteste a cualquier cliente faltan tres cosas, en
este orden:
1. **Verificación del negocio** en el portafolio comercial. Sin esto Meta no da acceso avanzado **[Oficial]**.
2. **Revisión de la app (App Review) con acceso avanzado** para los permisos de comentarios y mensajes
   (`instagram_manage_messages`, `instagram_manage_comments`, `pages_manage_engagement` y sus
   dependencias) **[Oficial]**.
3. **App en modo Live (publicada)**, más la configuración de la cuenta de Instagram (sección 3.8).

**TikTok.** **Sí se puede** contestar mensajes directos y comentarios por API, pero con un producto
**distinto** al que usamos hoy:
- Hoy usamos **TikTok for Developers** (`developers.tiktok.com`) para subir videos. Ese portal **no tiene** mensajes ni comentarios.
- Lo que sirve es **TikTok API for Business** (`business-api.tiktok.com`):
  - **Business Messaging API** para mensajes directos, en beta abierta en Latinoamérica **[Oficial]**.
  - **Accounts API** para comentarios de videos propios **[Código]**.
- **Requisito duro:** registrarse como desarrollador exige **correo con el dominio propio del negocio
  y una web de empresa en ese mismo dominio**. Gmail o Hotmail = rechazo **[Oficial]**.
- **Sin programar nada, desde hoy:** TikTok tiene respuestas automáticas propias (mensaje de
  bienvenida y respuestas por palabra clave).

> ⚠️ **Corrección a `REDES_SOCIALES_PENDIENTE_PROD.md`:** ahí dice que los comentarios de TikTok se
> descartaron porque "no hay endpoint de comentarios". Eso aplica solo a `developers.tiktok.com`. En
> `business-api.tiktok.com` sí existen `business/comment/list/`, `business/comment/reply/create/` y un
> webhook de comentarios (sección 4.5).

---

## 1. ¿En qué paso vas? — para ubicarte rápido

| # | Paso | Cómo saber si ya está |
|---|---|---|
| 1 | Correo con dominio propio (`algo@novedades-jade.com.mx`) | Puedes mandar y recibir correo en esa dirección |
| 2 | Páginas públicas en la tienda: aviso de privacidad, eliminación de datos, términos, contacto | Se abren sin iniciar sesión, en el dominio propio |
| 3 | Portafolio comercial de Meta con control total | business.facebook.com → Configuración → Personas: tu usuario dice "Control total" |
| 4 | Página de Facebook e Instagram de empresa dentro del portafolio | Configuración del negocio → Cuentas → Páginas / Cuentas de Instagram |
| 5 | Verificación del negocio en Meta | Centro de seguridad → Verificación del negocio dice **Verificado** |
| 6 | App de Meta con icono, privacidad, eliminación de datos y categoría | developers.facebook.com → tu app → Configuración → Básica: sin avisos en rojo |
| 7 | Al menos 1 llamada exitosa por cada permiso en los últimos 30 días | El botón "Solicitar acceso avanzado" ya no está gris |
| 8 | App Review enviada | Revisión de la app → Solicitudes: "En revisión" |
| 9 | Acceso avanzado aprobado | Permisos y funciones: dice "Acceso avanzado" en cada permiso pedido |
| 10 | App en modo Live | Interruptor arriba del panel en "Live / Publicada" |
| 11 | Instagram: "Permitir acceso a los mensajes" encendido | En la app de Instagram, sección 3.8 |
| 12 | Respuestas automáticas de Business Suite apagadas | Business Suite → Bandeja → Automatizaciones: todo apagado |
| 13 | Webhook de prod decidido (hoy apunta a QA) | Panel de la app → Webhooks → URL de devolución de llamada |
| 14 | TikTok: cuenta de empresa + "Recibir mensajes de Todos" | TikTok → Configuración y privacidad → Privacidad → Mensajes directos |
| 15 | TikTok: cuenta ligada a TikTok for Business / Business Center (acceso avanzado) | Business Center → Cuentas de TikTok: aparece la cuenta |
| 16 | TikTok: desarrollador aprobado en `business-api.tiktok.com` | El portal te deja "Create an app" |
| 17 | TikTok: acceso a Business Messaging API y al permiso "TikTok Accounts" | La app muestra esos permisos aprobados |

---

## 2. Lo que ya tenemos (al 2026-09-24)

Del lado de Meta **[Experiencia]**, ver `REDES_SOCIALES_PENDIENTE_PROD.md`:
- App de Meta tipo **Business**, en **modo desarrollo**, ligada al portafolio. Página **NovedadesJade**
  y su Instagram conectado. Los IDs están en `REDES_SOCIALES_PENDIENTE_PROD.md` §3.1.
- `instagram_manage_comments` figura como "aprobado" desde el 2026-08-20. Como el bot sigue sin
  contestarle a clientes sin rol, lo más probable es que sea **acceso estándar**, no avanzado.
- La revisión (App Review / verificación del negocio) se envió y **no se aprobó**. Al retomar,
  revisar en el panel el motivo exacto del rechazo o si sigue pendiente.
- Webhook suscrito a `comments` y `messages` de Instagram. **Apunta a QA, no a prod.**
- El bot de mensajes directos nunca contestó en vivo a un cliente real.
- El código usa **Graph API v21.0** (`application-qa.yml` / `application-docker.yml`).

Del lado de TikTok **[Experiencia]**:
- App en `developers.tiktok.com` con Login Kit y Content Posting (modo Upload), **sin auditar**.
- Cuenta `novedadesjade8` autorizada. Su token se guarda en la tabla `tiktok_token` de QA.
- **No hay nada** en `business-api.tiktok.com`: ni desarrollador registrado ni app.

---

## 3. META

### 3.1 Cuentas y roles que necesitas antes de empezar

- **Tu cuenta personal de Facebook** con **autenticación en dos pasos** activa. Meta la pide para
  administrar portafolios y para la cuenta de desarrollador **[Proveedor]**.
- **Portafolio comercial** (antes Business Manager) donde tú tengas **control total**. Con acceso
  parcial no aparece el botón de verificar **[Proveedor]**.
- **Página de Facebook** del negocio **dentro** del portafolio.
- **Instagram profesional de tipo Empresa**, conectado a la página. Las cuentas personales no tienen
  API de mensajes **[Proveedor]**.
- **Cuenta de desarrollador de Meta**, que se verifica con teléfono (SMS) o tarjeta **[Proveedor]**.
- **App tipo Business** ligada al mismo portafolio. Ya existe.

### 3.2 Verificación del negocio

**Dónde:** business.facebook.com → Configuración del negocio → **Centro de seguridad** → Verificación
del negocio → **Iniciar verificación** **[Proveedor]**.

**Qué te pide:**
1. Nombre legal, domicilio, teléfono y sitio web.
2. Meta intenta encontrar el negocio sola. Si no lo encuentra, pide documentos **[Oficial]**.
3. Un método para confirmar que eres tú: correo en el dominio del negocio, SMS o llamada al teléfono
   del negocio, o dominio verificado **[Oficial]**.

**Documentos que sirven en México:**

| Caso | Documento principal | Nombre legal que debes capturar |
|---|---|---|
| **Persona física con actividad empresarial** | **Constancia de Situación Fiscal (CSF)** reciente, con la actividad empresarial y en estado activo **[Proveedor]**. Meta lista para México el "RFC ID Card" (Cédula de Identificación Fiscal) **[Proveedor]** | **Tu nombre completo, idéntico a como viene en la CSF.** "Novedades Jade" no es nombre legal: va como nombre visible del negocio |
| **Persona moral** | **Acta constitutiva** + CSF de la empresa **[Proveedor]** | La razón social exacta, con "S.A. de C.V." o lo que corresponda |

Documentos de apoyo para domicilio o teléfono: recibo de luz (CFE), agua, internet o teléfono (Telmex,
Izzi, Totalplay…), o **estado de cuenta bancario a nombre del negocio** **[Proveedor]**. Un estado de
cuenta **personal** con el nombre del negocio agregado **no sirve** **[Proveedor]**.

**Reglas de los documentos:**
- Vigentes, sin editar, completos y legibles. Nada de fotos borrosas, recortes ni archivos dañados **[Oficial]** **[Proveedor]**.
- Que muestren **nombre legal + domicilio**, o **nombre legal + teléfono** **[Proveedor]**.
- Se pueden subir **2 documentos**, y **los dos** deben traer el nombre legal **[Oficial]**.
- Recientes: las fuentes dicen de 3–6 meses hasta 1 año. **Usa la CSF del mes** y un recibo de los
  últimos 3 meses para no fallar por eso **[Proveedor]**.
- El español es un idioma aceptado **[Proveedor]**.

**Cuánto tarda:** si se confirma sola, minutos. Con documentos, 2–5 días hábiles típicamente y hasta
14 **[Proveedor]**. El resultado llega a Business Suite y por correo.

**Por qué rechazan y cómo se arregla** — Meta casi siempre solo dice "no pudimos verificar" **[Proveedor]**:

| Causa | Arreglo |
|---|---|
| Nombre legal distinto letra por letra (acentos, abreviaturas, falta "S.A. de C.V.") | Copiar el nombre exacto del documento y pegarlo en Configuración del negocio → Información del negocio |
| Pusiste "Novedades Jade" como nombre legal y la CSF trae tu nombre | Nombre legal = el de la CSF |
| Domicilio distinto entre CSF, recibo y lo capturado (CP, colonia, número interior) | Capturar el domicilio fiscal exacto de la CSF y usar un recibo con esa misma dirección |
| El teléfono capturado no aparece en ningún documento | Usar el teléfono del recibo telefónico, o confirmar por correo o dominio |
| La web no muestra el negocio (sin nombre, dirección o teléfono visibles) | Poner nombre, contacto y aviso de privacidad visibles en la tienda |
| Documento vencido, borroso, recortado o sin sellos | Descargar de nuevo el PDF original del SAT o de la compañía |
| Estado de cuenta personal | Usar CSF + recibo de servicio |

**El botón "Iniciar verificación" no aparece o dice "No apto":**
- No tienes control total del portafolio. Pídeselo a quien lo administre.
- O el portafolio no tiene un activo que la requiera. Crear la app de desarrollador con el mismo
  correo y ligarla al portafolio suele habilitar el botón **[Proveedor]**. La nuestra ya existe, así
  que esto no debería pasar.

> **No confundir con Meta Verified** (la palomita azul de pago). Es otra cosa y **no** hace falta para la API.

### 3.3 Dominio — opcional para Meta, obligatorio para TikTok

- **Verificar el dominio** en Business Manager → Seguridad de la marca → Dominios **[Oficial]**. Hay
  tres métodos:
  - Etiqueta `<meta name="facebook-domain-verification">` en el `<head>` de la página de inicio.
  - Archivo HTML en la raíz del sitio.
  - Registro **TXT** en el DNS.
- Solo se verifica el **dominio principal** (`novedades-jade.com.mx`), **no** los subdominios
  (`shop.`, `backend.`) **[Oficial]**.
- Ya verificado, se puede quitar la etiqueta o el registro y no se pierde **[Oficial]**.
- **Correo con el dominio** (ej. `ventas@novedades-jade.com.mx`): Meta lo acepta como forma de
  confirmar el negocio, y **TikTok lo exige** (sección 4.2).

### 3.4 La app de Meta — lo que debe estar lleno ANTES de pedir revisión

En developers.facebook.com → tu app → **Configuración → Básica** **[Oficial]**:
- **Icono:** entre 512×512 y 1024×1024 px, JPG, GIF o PNG, menos de 5 MB.
- **URL de política de privacidad:** pública, sin iniciar sesión, en el dominio propio. Debe decir:
  - qué datos se guardan de comentarios y mensajes (texto, ID del usuario, fecha);
  - para qué (contestar dudas de productos);
  - con quién se comparten: **OpenAI procesa el texto para generar la respuesta**, hay que decirlo;
  - cuánto tiempo se guardan;
  - cómo pedir que se borren, y un correo de contacto.
- **URL de instrucciones de eliminación de datos**, o un callback. Obligatorio para apps de tipo
  Consumer y Gaming **[Oficial]**. La nuestra es Business, pero conviene ponerla igual: es una página
  de texto y quita un motivo de rechazo.
- **Categoría**, **correo de contacto** y **dominio de la app**.
- **Términos del servicio**: recomendado.

**Preguntas de manejo de datos (Data Handling)**: se contestan antes o durante la revisión **[Oficial]**.
- Preguntan si hay **procesadores de datos**. **Sí: OpenAI**, porque el bot le manda el texto del
  cliente. También el hosting (la VPS).
- Preguntan quién es la entidad responsable (tú o tu negocio) y el país.
- Preguntan si has recibido solicitudes de autoridades y qué políticas tienes para eso.
- Después hay un **Data Use Checkup** anual: si no se contesta, se pierden los permisos **[Oficial]**.

### 3.5 Qué permisos pedir — cruzado con nuestro código

**Regla de oro: pedir solo lo que se usa.** Pedir permisos de más es de los motivos de rechazo más
comunes, y **cada permiso lleva su propio video y su propia descripción** **[Oficial]**.

| Permiso | Qué hace con él nuestro código | ¿Acceso avanzado? |
|---|---|---|
| `instagram_manage_messages` | `InstagramDirectMessageBotService` → `POST /{ig-user-id}/messages` **[Código]** | **Sí** — sin él solo llegan mensajes de cuentas con rol |
| `instagram_manage_comments` | `InstagramCommentBotService` → `POST /{comment-id}/replies` **[Código]** | **Sí** |
| `instagram_basic` | Base de todo lo de Instagram | Sí (dependencia) |
| `pages_manage_metadata` | Suscribir la página a webhooks (`/{page-id}/subscribed_apps`). Meta la lista como requisito para mensajes de Instagram **[Oficial]** | Sí (dependencia) **[Sin confirmar]** |
| `pages_show_list` | Listar páginas, dependencia de webhooks **[Oficial]** | Sí (dependencia) |
| `pages_manage_engagement` | `FacebookCommentBotService` → `POST /{comment-id}/comments` **[Código]** | **Sí**, si se quiere el bot en comentarios de Facebook |
| `pages_read_engagement` | Leer publicaciones y comentarios de la página | Sí (dependencia) |
| `pages_manage_posts`, `instagram_content_publish` | Publicar en la propia página | **No hace falta**: es tu propia página y tu usuario tiene rol, el acceso estándar alcanza. No meterlos a la revisión |
| `pages_messaging` | Messenger. **Hoy no hay bot de Messenger**: el webhook recibe `object=page` y lo ignora **[Código]** | Solo si algún día se agrega |
| **Human Agent** (función) | Contestar hasta 7 días después, **escrito por una persona**. Solo sirve si se responde desde un panel propio. Desde la app de Instagram no hace falta | Opcional |
| `business_management` | A veces Meta la exige como dependencia (ej. de `pages_messaging`) **[Oficial]** | Solo si la pantalla la pide |

**Qué significa cada nivel de acceso** **[Oficial]**:
- **Estándar:** datos de cuentas que son tuyas o de gente con rol en la app.
- **Avanzado:** datos de otras personas o negocios. Requiere revisión de la app **y** verificación del negocio.

Un cliente que escribe o comenta no tiene rol, por eso los permisos de mensajes y comentarios
necesitan acceso avanzado aunque la cuenta de Instagram sea tuya. Lo confirman proveedores y foros de
Meta **[Proveedor]**, y es lo que vivimos en QA **[Experiencia]**.

### 3.6 App Review paso a paso

1. **Uso previo obligatorio.** Hacer **al menos 1 llamada exitosa con cada permiso** en los 30 días
   anteriores al envío. Se puede con la app o con el Explorador de la API Graph. El botón "Solicitar
   acceso avanzado" **sigue gris hasta que Meta registra la llamada**, y eso puede tardar hasta 2 días
   **[Oficial]**. Si el botón está gris, casi siempre es esto.
2. **Casos de uso.** En el panel, cada caso de uso se marca "Listo para probar" y se prueba **[Oficial]**.
3. **Permisos y funciones.** En cada permiso: "Solicitar acceso avanzado" → "Continuar a la revisión".
4. **Descripción por permiso:** qué hace la app, quién la usa (solo el negocio, para sus propios
   clientes) y los pasos exactos para que el revisor lo reproduzca **[Oficial]**.
5. **Un video por permiso**, sin mezclar permisos de casos de uso distintos en el mismo video **[Oficial]**:
   - En 1080p o más, con cursor visible y, de preferencia, notas en pantalla **[Oficial]**.
   - Debe mostrar el flujo completo: inicio de sesión con Facebook, la pantalla de consentimiento con
     el permiso, la acción y el resultado **[Proveedor]**.
   - **Guion para `instagram_manage_messages`:**
     1. Desde una cuenta de Instagram de prueba (sin rol en la app), escribe al Instagram de Novedades
        Jade "¿tienen bolsas negras?".
     2. Muestra en el panel admin, o en el log, que llegó el mensaje.
     3. Muestra en la app de Instagram de esa cuenta la respuesta del bot.
   - **Guion para `instagram_manage_comments`:** lo mismo, con un comentario en una publicación.
6. **Instrucciones para el revisor:** URL del panel admin, usuario y contraseña de prueba, y qué
   cuenta de Instagram usar. **Si el revisor no puede entrar ni reproducirlo, rechazan todo el envío**
   **[Oficial]**. Nuestro bot no tiene pantalla propia (vive en el servidor), así que el video y las
   instrucciones tienen que dejar clarísimo dónde se ve cada paso.
7. **Enviar.** Tarda de 1 a 5 días hábiles típicamente; los mensajes suelen tardar más que otros
   permisos. En 2026 hay reportes de hasta ~20 días **[Proveedor]**.

**Motivos de rechazo conocidos:**

| Motivo | Cómo evitarlo |
|---|---|
| Permisos de más | Solo los de la tabla 3.5 marcados "Sí" |
| Video que no muestra el permiso, o un video para varios permisos | Un video por permiso, siguiendo el guion |
| Falta la descripción aunque haya video (o al revés) | Llenar ambos en cada permiso **[Oficial]** |
| El revisor no pudo entrar o reproducirlo | Usuario de prueba que funcione y pasos numerados |
| No se ve bien quién mandó el mensaje | Que en el video se vea el nombre o usuario del remitente **[Proveedor]** |
| Mal manejo de fotos que manda el cliente (las URLs del CDN de Meta **caducan**) | Hoy el bot solo procesa texto. Si algún día se guardan fotos del cliente, hay que **descargarlas**, no guardar la URL. A Chatwoot lo rechazaron por esto **[Proveedor]** |
| Pedir permisos de Instagram en un caso de uso de solo Messenger | No mezclar productos en el mismo caso de uso **[Proveedor]** |
| Política de privacidad inaccesible, en PDF o sin mencionar datos o eliminación | Página pública con lo de la sección 3.4 |

### 3.7 Pasar la app a Live

- Ya aprobado, cambiar la app a **Live / Publicada**. En Live, **solo** funcionan con usuarios sin rol
  los permisos que tengan acceso avanzado aprobado **[Oficial]**.
- En modo desarrollo solo llegan las notificaciones de prueba del panel y las de cuentas con rol
  **[Oficial]**. Por eso hoy solo contesta a los testers.

### 3.8 Configuración de Instagram y de la página — lo que más falla

1. **Instagram profesional de tipo Empresa**, conectado a la página NovedadesJade.
2. **"Permitir acceso a los mensajes" encendido.** En la app de Instagram: Configuración → Mensajes
   y respuestas a historias → Controles de mensajes → **Herramientas conectadas** → Permitir acceso a
   los mensajes. **Si está apagado, la conexión "funciona" pero nunca llega ningún mensaje**
   **[Oficial]** **[Proveedor]**.
3. **Enrutamiento de conversaciones.** En Meta Business Suite → Configuración → **Integraciones →
   Enrutamiento de conversaciones**, nuestra app debe ser la que recibe los mensajes **[Oficial]**.
   - Si otra herramienta es "dueña" del hilo (ManyChat, el agente de IA de Meta, otro inbox), a
     nuestra app los mensajes le llegan por el canal `standby`, no por `messaging` **[Oficial]**.
   - Nuestro webhook hoy **solo lee `messaging`** **[Código]**: en ese caso no contestaría nada.
4. **Apagar las respuestas automáticas de Business Suite**: respuesta instantánea, mensaje de
   ausencia, preguntas frecuentes, respuestas por palabra clave y el agente de IA de Meta, si se
   activa (sección 3.12). Si quedan prendidas:
   - el cliente recibe **dos** respuestas;
   - nuestro bot ve una respuesta que no mandó él, cree que contestó un humano y **pausa al cliente
     para siempre** (`InstagramDirectMessageBotService.detectarRespuestaManualYPausar`) **[Código]** **[Experiencia]**.
5. **Webhooks** (panel de la app → Webhooks):
   - Objeto **Instagram**: campos `comments` y `messages`. Opcionales: `messaging_postbacks`,
     `message_reactions`.
   - Objeto **Page**: campo `feed`, para comentarios de Facebook.
   - La página debe tener la app instalada: `POST /{page-id}/subscribed_apps` con el token de la
     página **[Oficial]**.
6. **URL del webhook: prod o QA.** Hoy apunta a QA. Una app tiene **una sola URL de callback por
   objeto**. Si prod y QA usan la misma app, solo uno de los dos recibe. Decidirlo antes de lanzar;
   ver `REDES_SOCIALES_PENDIENTE_PROD.md` §3.1–3.2.

### 3.9 Tokens

- **Hoy:** Page Access Token "de larga duración", sacado de un token de usuario extendido
  (`FACEBOOK_SETUP.md` paso 5). **Se invalida** si cambias la contraseña de Facebook, te quitan de
  admin de la página o se revoca el acceso.
- **Recomendado para prod:** token de **usuario del sistema** del portafolio (Configuración del
  negocio → Usuarios del sistema). **No expira**. Se le asignan la app y la página con los permisos,
  y conviene rotarlo de vez en cuando **[Oficial]**.

### 3.10 Reglas de la mensajería que el bot debe respetar

| Regla | Detalle |
|---|---|
| **Ventana de 24 h** | Se puede contestar hasta 24 h después del último mensaje del cliente, y ahí sí puede ir contenido promocional **[Oficial]**. Después falla con el error 10 / 2534022 |
| **Etiqueta HUMAN_AGENT** | Extiende a **7 días**, pero **escrito por una persona**, nunca automático ni promocional. Automatizarlo lo detecta Meta y restringe la cuenta **[Oficial]** **[Proveedor]** |
| **Respuesta privada a un comentario** | **1 solo mensaje** por comentario, dentro de **7 días**, máximo 750 por hora **[Oficial]** |
| **Límites de llamadas** | 100/s por cuenta para texto, 10/s para audio o video, 2/s para leer conversaciones **[Oficial]** |
| **Responder en menos de 30 s** | Los bots deben contestar en 30 s **[Oficial]**. El nuestro contesta comentarios en menos de 20 s **[Experiencia]** |
| **Decir que es un bot** | Obligatorio "cuando lo exija la ley" y recomendado siempre, ej. "Soy el asistente automático de Novedades Jade" **[Oficial]**. **Nuestro bot hoy no lo dice** **[Código]**; agregarlo en el primer mensaje también ayuda en la revisión. No encontré ley mexicana que lo exija **[Sin confirmar]** |
| **Prohibido** | Mensajes en frío a quien no te escribió, promociones fuera de 24 h, bots que no usan la API oficial **[Proveedor]** |

### 3.11 Errores y qué significan

| Síntoma o error | Causa | Qué hacer |
|---|---|---|
| El bot solo le contesta al admin o a los testers | Sin acceso avanzado o app en desarrollo **[Oficial]** **[Experiencia]** | Pasos 5–10 de la sección 1 |
| No llega nada al webhook | "Permitir acceso a los mensajes" apagado, campo `messages` sin suscribir, página sin `subscribed_apps`, enrutamiento a otra app, o app en desarrollo | Revisar 3.8 en ese orden |
| Llegan mensajes pero el bot no contesta a un cliente | Está en `mensaje_directo_pausa` (falsa respuesta manual), en límite por hora, o escaló por correo | Consultas en `inventario_key_qa`, ver el historial del bot |
| `(#10)` subcódigo `2534022` "outside of allowed window" | Pasaron más de 24 h **[Proveedor]** | Solo contesta una persona con HUMAN_AGENT (7 días) |
| `(#10)` "Application does not have permission for this action" | El permiso no tiene acceso avanzado o el token no lo trae **[Proveedor]** | Revisar el nivel de acceso y regenerar el token con el permiso |
| `(#200)` | Permiso faltante o token de alguien sin rol ni permisos **[Proveedor]** | Igual que el anterior |
| `(#190)` token inválido | Token expirado o revocado (cambio de contraseña, quitaron admin) | Regenerar; en prod usar usuario del sistema |
| `(#551)` "Esta persona no está disponible" | Cuenta del cliente restringida o que limita quién le escribe **[Proveedor]** | No reintentar |
| `(#4)`, `(#613)` / `2534040` | Límite de llamadas **[Proveedor]** | Esperar |
| `(#100)` "The parameter recipient is required" | El `recipient` va en el query y no en el body **[Proveedor]** | Nuestro código ya lo manda en el body **[Código]** |
| Verificación: "No pudimos verificar tu negocio" | Ver la tabla de 3.2 | — |
| "Solicitar acceso avanzado" gris | Falta la llamada exitosa reciente con ese permiso **[Oficial]** | Paso 1 de 3.6 |

**Versión de la API:** el código usa **v21.0**, que Meta mantiene hasta el **21-ene-2027**
**[Proveedor]**. Al vencer, las llamadas **no fallan**: Meta las corre en la siguiente versión
disponible, y los cambios de comportamiento pueden aparecer sin aviso **[Proveedor]**. La actual es
v26.0 (29-jul-2026) **[Proveedor]**; v25.0 salió el 18-feb-2026 **[Oficial]**. Subir `api-version` en
los yml y probar antes de enero de 2027.

### 3.12 Alternativas de Meta sin código

- **Respuestas automáticas de Business Suite** **[Oficial]**:
  - respuesta instantánea al primer mensaje;
  - mensaje de ausencia;
  - hasta 5 preguntas frecuentes;
  - hasta 5 respuestas por palabra clave (solo en la versión de escritorio).

  Sirven para Instagram, Facebook y Messenger. **No combinarlas con nuestro bot** (sección 3.8, punto 4).
- **Meta Business Agent**, la IA de Meta que contesta por el negocio:
  - Disponible en todo el mundo desde el **4-jun-2026** para WhatsApp, Messenger y Business Suite;
    se está extendiendo a Instagram **[Oficial]**.
  - Se entrena con tu catálogo, tu sitio y tus preguntas frecuentes **[Proveedor]**.
  - **Cobra desde el 1-ago-2026:** USD 2 por millón de tokens, unos 4–5 centavos de dólar por
    conversación sencilla **[Proveedor]**.
  - Es competencia directa de nuestro bot: usar uno u otro, nunca los dos.

### 3.13 WhatsApp — por si algún día se agrega

No es parte de este trámite, pero es el mismo portafolio:
- Se usa la **WhatsApp Business Platform (Cloud API)**, con un número que **no** esté activo en la app
  de WhatsApp o que se migre.
- La verificación del negocio de la sección 3.2 sirve también aquí y sube los límites de envío.
- **Política 2026:** desde el **15-ene-2026** están prohibidos los chatbots de IA **de propósito
  general** (tipo ChatGPT). Los bots de atención, ventas y pedidos de un negocio **sí** están
  permitidos **[Proveedor]** (varias fuentes y prensa). Nuestro bot entra en lo permitido.
- Se cobra por mensaje según tipo y país. Un proveedor dice que Meta vuelve a cobrar los mensajes de
  servicio dentro de las 24 h desde el 1-oct-2026 **[Sin confirmar]**.

---

## 4. TIKTOK

### 4.1 Hay dos portales distintos — la clave de todo

| Portal | Qué tiene | ¿Lo usamos? |
|---|---|---|
| **TikTok for Developers** — `developers.tiktok.com` | Login Kit, Content Posting API (subir y publicar videos), Display API | **Sí**: `TikTokGraphClient`, `open.tiktokapis.com`, `/v2/oauth/token/` **[Código]** |
| **TikTok API for Business** — `business-api.tiktok.com` | Anuncios, **Accounts API** (contenido orgánico: comentarios, publicaciones, métricas) y **Business Messaging API** (mensajes directos) | **No.** Aquí está lo que se quiere |

Son registros de desarrollador **separados**, apps **separadas** y OAuth **distinto**. El de Business
usa `/tt_user/oauth2/token/` y el `open_id` de la cuenta va como `business_id` **[Código]**. El token
de `novedadesjade8` que ya tenemos **no sirve** para mensajes ni comentarios.

### 4.2 Registrarse como desarrollador en TikTok API for Business

**[Oficial]** (página "Register as a developer"):
- **Correo de comunicación con el dominio de la empresa.** Correo personal (Gmail, Hotmail) o
  temporal = **rechazo**. Recomiendan un alias tipo `developers@novedades-jade.com.mx`.
- **Sitio web de la empresa:**
  - que funcione, esté completo y se vea profesional;
  - con información del negocio y los productos **sin iniciar sesión**;
  - **en el mismo dominio que el correo** y en un dominio propio del negocio.
- **No aceptan desarrolladores individuales** ni sitios personales.

Después: crear la app, pedir los permisos y pasar las revisiones de seguridad y privacidad de datos.
Tarda de 3 a 7 días en lo básico y de 1 a 3 semanas si piden más documentación **[Proveedor]**.
Si se pide más de lo que se demuestra, rechazan **[Proveedor]**.

### 4.3 La cuenta de TikTok

- **Cuenta de empresa (Business Account).** Las personales no sirven para la API de mensajes **[Proveedor]**.
- **Recibir mensajes de "Todos"** encendido. Si no, cada mensaje hay que aceptarlo a mano y no
  funcionan las respuestas automáticas **[Oficial]** **[Proveedor]**.
- **Acceso avanzado:** se obtiene **ligando la cuenta de TikTok a TikTok for Business o a un Business
  Center**, aunque el Business Center no esté verificado **[Oficial]**.
- La otra vía es ser **cuenta de empresa verificada** (Verified Business Account), con verificación
  del negocio. La API de mensajes pide una de las dos **[Proveedor]**.
- **Verificación del negocio en TikTok (disponible en México, mayores de 18):**
  - Acepta **acta constitutiva o registro** y **Cédula de Identificación Fiscal (RFC)** **[Oficial]**.
  - El nombre legal y el número del documento se capturan **exactos**.
  - El número del documento tiene que verse en la imagen.
- La verificación de negocio **del portal de desarrolladores** (`developers.tiktok.com`) tarda de 1 a
  3 días hábiles **[Oficial]**. Sus resultados posibles:
  - Verificado;
  - No se pudo verificar (se corrige y se reenvía);
  - Falta información;
  - Rechazado.

### 4.4 Mensajes directos — Business Messaging API

- **Disponibilidad:** beta abierta en Asia-Pacífico, **Latinoamérica**, Medio Oriente y Norteamérica.
  No disponible en el Espacio Económico Europeo, Suiza ni Reino Unido **[Oficial]**.
- **Reglas** **[Proveedor]**:
  - **El cliente escribe primero**; el negocio no puede iniciar.
  - Ventana de **48 horas** desde el último mensaje del cliente.
  - Máximo **10 mensajes seguidos** sin que el cliente conteste. Cuando contesta, se reinicia todo.
- **Qué se puede mandar** **[Código]**:
  - texto;
  - imagen, solo si los dos países lo soportan;
  - compartir una publicación de TikTok;
  - plantilla;
  - acción de "escribiendo…".

  Texto e imagen no pueden ir en el mismo mensaje.
- **Endpoints** confirmados en el SDK **[Código]**:
  - `POST /open_api/v1.3/business/message/send/`
  - `GET /open_api/v1.3/business/message/conversation/list/`
  - `GET /open_api/v1.3/business/message/capabilities/get/` (si la cuenta puede escribir en esa conversación)
  - `/business/message/auto_message/*` (bienvenida y palabras clave vía API)
  - subida y descarga de archivos
- **Webhooks:** se configuran **por API** ("Create a TikTok Business Messaging Webhook
  configuration"), no en una pantalla del panel **[Oficial]**.
- **Hay que escribir código nuevo:** un cliente para `business-api.tiktok.com` con su propio OAuth,
  más un controlador de webhook. El chatbot actual (`ChatbotBase.responderComentario`) sí se puede
  reusar, igual que en Instagram.

### 4.5 Comentarios — Accounts API (orgánico)

**Endpoints** confirmados en el SDK **[Código]**:
- `GET /open_api/v1.3/business/comment/list/`: comentarios públicos y ocultos de un video propio.
- `POST /open_api/v1.3/business/comment/reply/create/`: responder a un comentario.
- También: ocultar, dar like, borrar y listar respuestas.

**Webhook de comentarios:** tipo de evento `COMMENT` (`comment.update`). Se configura con
`/business/webhook/update/`, para todos los videos o solo para algunos **[Código]**.

Algunos blogs dicen que "TikTok no tiene API pública de comentarios": hablan de `developers.tiktok.com`,
no de `business-api.tiktok.com`.

Requiere el permiso **"TikTok Accounts"** en la app **[Proveedor]**.

### 4.6 Sin programar nada: respuestas automáticas nativas de TikTok

Dónde: en la app, **Business Suite → Centro de mensajes**, o en el Business Center, versión web
**[Oficial]**. Hay cuatro tipos:
- **Mensaje de bienvenida:** hasta 250 caracteres **[Proveedor]**.
- **Respuesta por palabra clave:** coincidencia **exacta**; palabra de hasta 40 caracteres y
  respuesta de hasta 500 **[Proveedor]**.
- **Preguntas sugeridas:** botones con preguntas frecuentes.
- **Temas de chat:** botones para iniciar la conversación.

Condiciones:
- Requiere **"Recibir mensajes de Todos"** **[Oficial]**.
- **Cada mensaje nuevo o editado pasa una revisión de 1 a 5 días** **[Proveedor]**.

**Recomendación:** activarlo **ya**, mientras se tramita la API. Cuando esté el bot, hay que apagar
estas respuestas (mismo problema de doble respuesta que en Meta).

### 4.7 Socios de mensajería de TikTok — si el trámite propio no pasa

TikTok certifica "Messaging Partners" que ya tienen acceso a la API: respond.io, SleekFlow, MessageGate,
Infobip, Sprinklr y otros **[Oficial]** **[Proveedor]**. ManyChat lanzó TikTok en EE. UU. en enero de
2026; no se confirmó para México **[Sin confirmar]**. Son de pago: contestan por ti o se conectan a tu
sistema.

### 4.8 La auditoría de publicar videos (lo que ya usamos)

Esto no es de mensajes, pero es el mismo pendiente de TikTok:
- Mientras la app no pase la auditoría, todo lo publicado por API sale **privado** **[Proveedor]**.
- La auditoría tarda **2–4 semanas** con varias rondas **[Proveedor]**.
- Piden:
  - **un video demo que cubra cada scope**;
  - **verificación de las URL o el dominio**;
  - que el formulario de publicación respete las opciones de privacidad del creador.

  Pedir scopes de más = rechazo **[Proveedor]**.

### 4.9 Errores y rechazos de TikTok

| Síntoma | Causa | Qué hacer |
|---|---|---|
| Rechazan el registro de desarrollador | Correo personal, web personal o inactiva, o web en otro dominio que el correo **[Oficial]** | Sección 4.2 |
| Rechazan la app o la auditoría | Scopes de más, demo incompleto, datos de privacidad incompletos **[Proveedor]** | Pedir solo lo que muestra el demo |
| No llegan los mensajes directos | Cuenta no de empresa, "Todos" apagado, sin acceso avanzado, webhook sin configurar | Secciones 4.3 y 4.4 |
| No se puede contestar | Pasaron más de 48 h, o ya van 10 mensajes sin respuesta del cliente **[Proveedor]** | Esperar a que el cliente escriba |
| La respuesta automática nativa no se activa | Sigue en revisión (1–5 días) o "Todos" apagado | Esperar o revisar la privacidad |
| "No se pudo verificar" el negocio | Nombre legal o número de documento distinto, o documento ilegible | Copiar exacto de la CSF o el acta |

---

## 5. Lo que tienes que juntar

**Documentos (PDF originales, no fotos):**
- [ ] **Constancia de Situación Fiscal** del mes, descargada del SAT.
- [ ] **Acta constitutiva** (solo persona moral).
- [ ] **Recibo de luz, teléfono o internet** de los últimos 3 meses, con el **mismo domicilio** de la CSF.
- [ ] **Estado de cuenta bancario** a nombre del negocio, si lo hay.
- [ ] **INE**, por si TikTok o Meta piden verificar identidad.

**Datos, escritos exactamente como en la CSF:**
- [ ] Nombre legal.
- [ ] RFC.
- [ ] Domicilio fiscal: calle, número, colonia, CP, municipio y estado.
- [ ] Teléfono que aparezca en algún documento.
- [ ] URL del sitio.

**Web y correo, en el dominio propio:**
- [ ] Correo `@novedades-jade.com.mx` funcionando.
- [ ] Página de inicio pública con nombre del negocio y contacto.
- [ ] Aviso de privacidad, que mencione OpenAI como procesador.
- [ ] Página de eliminación de datos.
- [ ] Términos del servicio.
- [ ] Dominio verificado en Meta (opcional, ayuda).

**Cuentas:**
- [ ] Facebook personal con verificación en dos pasos.
- [ ] Portafolio comercial con control total.
- [ ] Página e Instagram de empresa conectados.
- [ ] Desarrollador de Meta.
- [ ] TikTok de empresa.
- [ ] TikTok for Business o Business Center con la cuenta ligada.
- [ ] Desarrollador en `business-api.tiktok.com`.

**Material para la revisión de Meta:**
- [ ] Icono de 1024×1024.
- [ ] Una cuenta de Instagram de prueba **sin rol** en la app, para grabar como "cliente".
- [ ] Usuario de prueba del panel admin para el revisor.
- [ ] Un video 1080p por permiso, siguiendo los guiones de 3.6.
- [ ] Texto de descripción por permiso.

---

## 6. Orden recomendado

1. **Correo con dominio y páginas públicas** (privacidad, eliminación, términos). Bloquea a Meta y a
   TikTok, así que va primero.
2. **TikTok, respuestas automáticas nativas** (4.6). Cinco minutos y ya contesta algo mientras sale
   lo demás.
3. **Meta, verificación del negocio** (3.2).
4. **Meta, completar la app** (3.4) y hacer 1 llamada con cada permiso (3.6, paso 1). Esperar 2 días.
5. **Meta, App Review** solo de comentarios y mensajes (3.5 y 3.6). Mientras esperas:
6. **TikTok, ligar la cuenta a Business Center** (acceso avanzado) y **registrarse como desarrollador**
   en `business-api.tiktok.com` (4.2 y 4.3).
7. **Antes de lanzar Meta a prod:**
   - decidir la URL del webhook (3.8, punto 6);
   - usar un token de usuario del sistema (3.9);
   - apagar las respuestas automáticas de Business Suite (3.8, punto 4);
   - revisar el enrutamiento de conversaciones (3.8, punto 3);
   - traer redes sociales a `main` (`REDES_SOCIALES_PENDIENTE_PROD.md` §4).

---

## 7. Pendientes de código que salieron de esta investigación

- [x] 2026-09-24 — El bot se presenta como **asistente automático** en el primer mensaje a cada
      persona (comentarios de Facebook e Instagram, y mensajes directos de Instagram).
      `ChatbotBase.instruccionesRedSocial`.
- [x] 2026-09-24 — **Graph API v25.0** en `application-dev.yml` y `application-qa.yml`.
      `application-docker.yml` (prod) sigue en v21.0: cambiarlo cuando se valide en QA.
- [x] 2026-09-24 — El webhook **registra en el log los eventos `standby`** ("otra app es dueña de la
      conversación"). No los contesta, solo avisa por qué el bot calla.
- [x] 2026-09-24 — **Pausa falsa por carrera** corregida: el eco de la respuesta del bot llegaba
      antes de guardar `respuestaMid` y el cliente quedaba pausado. Prueba:
      `InstagramDirectMessagePausaTest`. Sigue pendiente la pausa por las respuestas automáticas de
      Business Suite: esa se evita apagándolas (3.8, punto 4).
- [x] 2026-09-24 — **Front:** aviso de privacidad actualizado (asistente automático, OpenAI, Meta y
      TikTok, mensajes y comentarios de redes, rifas) y página pública nueva **`/eliminar-datos`**.
- [ ] **TikTok, cliente nuevo** para `business-api.tiktok.com`: OAuth `/tt_user/oauth2/token/`,
      webhook de mensajes y de `COMMENT`, y envío con `business/message/send/` (4.4 y 4.5).
- [ ] Corregir en `REDES_SOCIALES_PENDIENTE_PROD.md` la nota "TikTok comentarios: descartado" (sección 0).

---

## 8. Bitácora del trámite

Anotar aquí cada paso que se haga, con fecha, lo que pidió la pantalla y cómo quedó.

**Datos de la app de Meta** (no son secretos):
- App ID: `1017171384561253` (nombre en el panel: novedadesJade)
- Portafolio comercial (business_id): `636203476989310`
- Envío a revisión en curso: `submission_id=1017197064558685`

### 2026-09-24 — Envío a revisión (Revisar → Enviar a revisión de apps)

| Sección | Estado | Nota |
|---|---|---|
| Verificación | ⏳ Falta | Botón "Ir a Verificación". Es la verificación del negocio (3.2) |
| Configuración de apps | ✅ Lista | Ícono ✅ · Política de privacidad `https://shop.novedades-jade.com.mx/privacidad` · Categoría "Negocios y páginas" · Contacto principal: correo personal de Hotmail (para Meta sirve; TikTok no lo acepta). **Falta:** subir a prod la versión nueva del aviso (menciona OpenAI) y poner `https://shop.novedades-jade.com.mx/eliminar-datos` en Configuración → Básica |
| Uso permitido | ⏳ Falta | Botón "Ir a Uso permitido". Certificar que cada permiso se usa solo para lo permitido |
| Tratamiento de datos | ✅ Corregida (2026-09-24) | Encargados: Hosting Mexico, OVHcloud, OpenAI. Responsable: el nombre del dueño, sin domicilio. País México. Autoridades: "No" y "Ninguna de las anteriores" |
| Instrucciones para revisores | ⚠️ Corregir | Tenía la URL de login de QA y sin pasos para probar el bot. Texto propuesto abajo |
| **Enviar para revisión** | 🔒 Deshabilitado | Se activa cuando las 5 secciones estén en verde |

**Tratamiento de datos — respuestas acordadas (2026-09-24):**

| Pregunta | Estaba | Debe decir |
|---|---|---|
| processor-0: ¿Tienes encargados del tratamiento? | Sí | **Sí** |
| processor-2: Lista de encargados | ✅ Corregido: quedaron los 3 de abajo, sin "Abel Tiburcio" | **OpenAI** (genera las respuestas automáticas con el texto de comentarios y mensajes) · **OVHcloud** (servidor VPS donde viven el sistema y la base de datos) · **Hosting-Mexico** (correo del dominio: los mensajes que el bot no sabe contestar se reenvían por correo al admin) |
| responsible-1: Responsable de los datos | ✅ Corregido: solo el nombre, sin domicilio | El **nombre completo idéntico a la Constancia de Situación Fiscal**, sin domicilio. Tiene que coincidir con el nombre legal de la verificación del negocio |
| responsible-2: País | México | **México** |
| requests-3: ¿Entregaste datos a autoridades por seguridad nacional? | — | **No** |
| requests-4: Procesos ante solicitudes de autoridades | "Ninguna de las anteriores" | Marcar solo los que de verdad se van a seguir. Si no hay proceso, "Ninguna" es la respuesta honesta y se puede dejar. Si el dueño se compromete a seguirlos: revisión de legalidad, minimización y documentación |

processor-2 se llena **un encargado por entrada** (botón "Agregar encargado…"), cada uno con nombre,
categoría (processor-2a) y países (processor-2b):

| Nombre | Categoría | Países |
|---|---|---|
| `OpenAI, L.L.C.` | Soluciones y servicios de TI, incluido el almacenamiento en la nube y el procesamiento | Estados Unidos |
| `OVHcloud` | Soluciones y servicios de TI, incluido el almacenamiento en la nube y el procesamiento | Francia (IP `51.178.29.99`; confirmar "Localización" en el panel de OVH) |
| `Hosting Mexico` | Soluciones y servicios de TI, incluido el almacenamiento en la nube y el procesamiento | México y Estados Unidos (servidor de correo `63.143.40.210`, sin confirmar) |

**Instrucciones para revisores — propuesta (2026-09-24):**

| Campo | Qué poner |
|---|---|
| URL del sitio | `https://shop.novedades-jade.com.mx` (la tienda pública). Estaba `https://qa.shop.novedades-jade.com.mx/login`: el revisor no ve nada del bot ahí |
| instructions-web-2 | El texto en inglés de abajo |
| fblogin-web-1 (¿Facebook Login integrado?) | **No**. El bot no tiene inicio de sesión de usuarios: los tokens se sacan del Explorador de la API Graph |
| accesscode-web-1 / -2 | Vacío: no hay pagos ni membresías. Solo si se piden permisos de publicar, dar un usuario de prueba del panel admin |
| geo-web-5 | Vacío: no hay bloqueo geográfico |
| documents-web-1 | **Subir los videos** de 3.6 (uno por permiso). Mientras la app esté en modo desarrollo, el revisor no puede ver al bot contestarle, así que el video es la prueba |

Texto para `instructions-web-2` (en inglés, que es lo que leen los revisores). Ajustar la lista de
permisos a los que de verdad se piden:

```
This app is an internal tool used only by our own small business, Novedades Jade (a retail
store in Mexico). It connects only to our own Facebook Page (facebook.com/NovedadesJade) and our
own Instagram professional account (@novedades_bolsas_jade). There is no end-user login and the
bot has no user interface: it runs on our server and receives Meta webhooks.

What the app does with each permission:
1. instagram_manage_messages: when someone sends a Direct Message to @novedades_bolsas_jade,
   our server receives the "messages" webhook, writes an answer about our product catalog and
   replies with POST /{ig-user-id}/messages. The first reply always says it is an automated
   assistant. If the bot cannot answer, it does not reply and forwards the message by email to
   a person on our team, who answers from the Instagram app.
2. instagram_manage_comments: when someone comments on one of our posts, our server receives
   the "comments" webhook and replies in the same thread with POST /{ig-comment-id}/replies.

How to test:
1. From any Instagram account, send a Direct Message to @novedades_bolsas_jade, for example
   "¿Tienen bolsas negras?" ("Do you have black bags?"). An automated reply in Spanish arrives
   within about 20 seconds.
2. Comment on any recent post of @novedades_bolsas_jade, for example "¿Qué precio tiene?"
   ("What is the price?"). The reply appears under the comment within about 20 seconds.

While the app is in development mode, Meta only delivers these webhooks for accounts with a
role on the app, so replies will not reach an account without a role. The attached screen
recordings show the complete flow for each permission with a test account.

Privacy policy: https://shop.novedades-jade.com.mx/privacidad
Data deletion instructions: https://shop.novedades-jade.com.mx/eliminar-datos
```

Proveedores confirmados en el repo: VPS en OVHcloud (`VPS_AUDITORIA.md`), correo en Hosting-Mexico
(`application-docker.yml`, `spring.mail`), IA en OpenAI (`openai.api-key`). El DNS está en Hostinger,
pero no toca datos de la plataforma. Cloudflare ya no está en el camino.

**Correo con dominio:** el negocio **ya tiene** buzones `@novedades-jade.com.mx` en Hosting-Mexico
(el back manda correos desde `admin@`). Sirve para confirmar la verificación de Meta y es requisito
de TikTok (4.2).

### 2026-09-24 — Desplegado para grabar los videos

| Repo | Rama | Qué |
|---|---|---|
| Back | `main` (prod) | Bot se presenta como asistente automático, eco sin pausa falsa, log de `standby` (cherry-pick, sin pedidos) |
| Back | `dev` y `qa` | Lo mismo + Graph API v25.0 + esta guía |
| Front | `master` (prod), `qa`, `dev` | Aviso de privacidad nuevo + página `/eliminar-datos` |

El webhook de Meta apunta a **QA**, así que los videos se graban contra QA (el deploy es automático
al subir a `qa`, 1–2 minutos).

**Pendiente en el panel de Meta:** poner `https://shop.novedades-jade.com.mx/eliminar-datos` en
Configuración de la app → Básica → "URL de instrucciones de eliminación de datos".

### Cuentas de prueba para grabar (modo desarrollo)

Mientras la app esté en desarrollo, el bot solo le contesta a cuentas **con rol en la app**.
- **Sirven tus propias cuentas** personales de Instagram. **No sirve** la del negocio
  (`novedades_bolsas_jade`): es la que recibe, y el bot ignora lo que manda ella misma.
- **Caso fácil:** una cuenta de Instagram tuya conectada (Centro de cuentas) al mismo Facebook con el
  que administras la app. Ese Facebook ya es administrador, así que la cuenta ya tiene rol. Así se
  probó en QA el bot de comentarios.
- **Otra persona u otra cuenta:** developers.facebook.com → la app → **Roles de la app → Roles →
  Agregar personas → Evaluador (Tester)**. Se invita a su cuenta de **Facebook**, la persona acepta
  en developers.facebook.com/requests, y su Instagram debe estar conectado a ese Facebook en el
  Centro de cuentas. Si en Roles aparece la sección **"Evaluadores de Instagram"**, se invita por
  usuario de Instagram y se acepta en Instagram → Configuración → Permisos de sitios web → Apps y
  sitios web → Invitaciones de evaluador **[Sin confirmar cuál de las dos pide esta app]**.
- La cuenta de prueba debe ser **distinta** en cada video "de cliente" que se quiera mostrar como
  primera vez, porque el saludo de asistente automático solo sale en el **primer** mensaje de cada
  persona. Si se reutiliza una cuenta que ya escribió antes, no saldrá el aviso.

---

## 9. Fuentes

**Meta — oficiales**
- [Verificar tu negocio en Meta Business Suite](https://www.facebook.com/business/help/2058515294227817)
- [Subir documentos oficiales para verificar tu negocio](https://www.facebook.com/business/help/159334372093366)
- [Solucionar problemas con la verificación del negocio](https://es-la.facebook.com/business/help/2342133782492969)
- [Verificación de dominio](https://developers.facebook.com/docs/sharing/domain-verification/) · [Dominio en Business Manager](https://www.facebook.com/business/help/286768115176155)
- [Access Levels (estándar vs avanzado)](https://developers.facebook.com/docs/graph-api/overview/access-levels/)
- [Instagram Platform — Overview](https://developers.facebook.com/docs/instagram-platform/overview/) · [App Review de Instagram](https://developers.facebook.com/docs/instagram-platform/app-review/)
- [Permiso instagram_manage_messages](https://developers.facebook.com/docs/permissions/reference/instagram_manage_messages/) · [Permiso pages_messaging](https://developers.facebook.com/docs/permissions/reference/pages_messaging/)
- [Instagram API with Instagram Login](https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/) · [Enviar mensajes](https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/messaging-api/)
- [Webhooks de Instagram](https://developers.facebook.com/docs/instagram-platform/webhooks) · [Webhooks de Page](https://developers.facebook.com/docs/graph-api/webhooks/reference/page/) · [App Modes](https://developers.facebook.com/docs/development/build-and-test/app-modes/)
- [App Review — guía de envío](https://developers.facebook.com/docs/resp-plat-initiatives/individual-processes/app-review/submission-guide) · [Grabaciones de pantalla](https://developers.facebook.com/docs/app-review/submission-guide/screen-recordings/) · [Guía de rechazos de Instagram](https://developers.facebook.com/docs/app-review/support/rejection-guides/instagram/)
- [Preguntas de manejo de datos](https://developers.facebook.com/docs/resp-plat-initiatives/individual-processes/data-handling-questions/questions-preview/) · [Política de privacidad — expectativas](https://developers.facebook.com/docs/development/terms-and-policies/privacy-policy/) · [Callback de eliminación de datos](https://developers.facebook.com/docs/development/create-an-app/app-dashboard/data-deletion-callback/)
- [Política de Messenger e IG Messaging API](https://developers.facebook.com/documentation/business-messaging/messenger-platform/policy) · [Human Agent](https://developers.facebook.com/docs/messenger-platform/instagram/features/human-agent-escalation/) · [Respuestas privadas](https://developers.facebook.com/docs/instagram-platform/private-replies/)
- [Límites de Messenger/IG](https://developers.facebook.com/documentation/business-messaging/messenger-platform/overview/rate-limiting) · [Límites Graph API](https://developers.facebook.com/docs/graph-api/overview/rate-limiting/)
- [Enrutamiento de conversaciones para Instagram](https://developers.facebook.com/docs/messenger-platform/instagram/features/conversation-routing/) · [Configurarlo en Business Suite](https://www.facebook.com/business/help/415420934987251) · [Handover / standby](https://developers.facebook.com/docs/messenger-platform/reference/webhook-events/standby/)
- [Usuarios del sistema y tokens](https://developers.facebook.com/docs/business-management-apis/system-users/install-apps-and-generate-tokens/)
- [Respuestas automáticas de Business Suite](https://www.facebook.com/business/help/395965998733706)
- [Permitir acceso a mensajes en otras apps (Instagram)](https://help.instagram.com/791161338412168/)
- [Meta Business Agent (anuncio)](https://about.fb.com/news/2026/06/meta-business-agent/) · [Graph API v25](https://developers.facebook.com/blog/post/2026/02/18/introducing-graph-api-v25-and-marketing-api-v25/)

**Meta — proveedores y otros**
- [Wati: documentos por país](https://support.wati.io/en/articles/11463208-meta-business-verification-required-documents-by-country) · [Wati: habilitar "Iniciar verificación"](https://support.wati.io/en/articles/11463214-how-to-enable-the-start-verification-button-in-meta-business-manager)
- [Leadsales: verificar negocio (México, CSF)](https://leadsales.io/blog/verificar-negocio-meta-business-para-usar-api/) · [AsistChat: errores que te rechazan](https://asistchat.com/blog/como-verificar-negocio-meta-business-paso-a-paso)
- [Chatwoot: Instagram App Review](https://developers.chatwoot.com/self-hosted/instagram-app-review) · [Chatwoot #8583 (CDN)](https://github.com/chatwoot/chatwoot/issues/8583) · [Chatwoot #13860 (scopes)](https://github.com/chatwoot/chatwoot/issues/13860)
- [n8n: webhooks de DM solo en modo prueba](https://community.n8n.io/t/instagram-dms-webhooks-work-only-in-test-mode/176851)
- [Códigos de error de IG para DMs](https://instantdm.com/instagram-api-error-codes-for-dm-automation) · [Límites de IG 2026](https://www.conferbot.com/limits/instagram)
- [Tiempos de App Review en 2026](https://bundle.social/blog/meta-app-review-20-days) · [Deprecación de versiones de Graph API](https://singhamandeep.com/meta-graph-api-version-deprecation/) · [v20 expira y corre en v21](https://dev.to/flarecanary/metas-graph-api-v20-expires-september-24-your-calls-wont-fail-theyll-quietly-start-running-v21-2m2a)
- [Costos de Meta Business Agent](https://marketing4ecommerce.mx/el-meta-business-agent-deja-de-ser-gratis/) · [Guía de Business Agent](https://www.memacon.com/meta-business-agent/)
- [WhatsApp: política de IA 2026 (respond.io)](https://respond.io/blog/whatsapp-general-purpose-chatbots-ban)

**TikTok — oficiales**
- [Business Messaging API](https://business-api.tiktok.com/portal/docs/business-messaging-api/v1.3) · [Webhooks de Business Messaging](https://business-api.tiktok.com/portal/docs/subscribe-to-business-messaging-webhook-events-via-webhooks-api/v1.3) · [Autorización](https://business-api.tiktok.com/portal/docs/authorization/v1.3)
- [Registrarse como desarrollador](https://business-api.tiktok.com/portal/docs/register-as-a-developer/v1.3) · [Crear una app](https://business-api.tiktok.com/portal/docs/create-a-developer-app/v1.3)
- [Responder un comentario](https://business-api.tiktok.com/portal/docs/reply-to-a-comment/v1.3) · [Webhooks de Accounts](https://business-api.tiktok.com/portal/docs/accounts-api-webhooks-guide/v1.3) · [Organic API](https://business-api.tiktok.com/portal/docs/organic-api/v1.3)
- [Acceso avanzado y bandeja profesional](https://ads.tiktok.com/resources/help/article/access-messaging-settings-from-professional-inbox-of-verified-business-account-or-tiktok-account-with-advanced-access) · [Integrar la cuenta con Business Center](https://ads.tiktok.com/help/article/business-account-integration-with-business-center?lang=en)
- [Verificación de negocio para cuentas de TikTok](https://ads.tiktok.com/help/article/about-business-registration) · [Documentos aceptados](https://ads.tiktok.com/help/article/acceptable-documents-for-business-verification) · [Verificar negocio en el portal de desarrolladores](https://developers.tiktok.com/docs/en/verify-your-business)
- [Mensajes automáticos](https://ads.tiktok.com/help/article/navigate-auto-message-business-accounts?lang=en) · [Configurarlos en Business Center](https://ads.tiktok.com/help/article/how-to-configure-business-account-automatic-messages-in-business-center) · [Messaging Partners](https://ads.tiktok.com/help/article/about-message-management-tools)
- [Webhooks de TikTok for Developers](https://developers.tiktok.com/doc/webhooks-overview/)

**TikTok — código, proveedores y otros**
- [SDK Go `bububa/tiktok-business`](https://github.com/bububa/tiktok-business) (endpoints de comentarios, mensajes y webhooks, leídos del código al 2026-09-08) · [SDK oficial de TikTok (anuncios)](https://github.com/tiktok/tiktok-business-api-sdk)
- [Chatwoot: canal TikTok](https://developers.chatwoot.com/self-hosted/configuration/features/integrations/tiktok) · [SleekFlow: TikTok Business Messaging](https://sleekflow.io/channels-integrations/tiktok-business-messaging) · [respond.io: TikTok](https://respond.io/help/tiktok/tiktok-overview)
- [Aprobación y auditoría de TikTok API](https://bundle.social/blog/tiktok-api-approval)
