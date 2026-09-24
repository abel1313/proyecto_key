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
| **(Nos pasó, 2026-09-24)** "No podemos verificar que el número de teléfono … está asociado con el negocio" | Confirmar con el **correo del dominio** (`@novedades-jade.com.mx`), o subir recibo de teléfono / estado de cuenta con nombre legal + ese número **[Experiencia]** |
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
| Al subir el video del App Review: "Se produjo un error. Actualiza la página o cierra y vuelve a abrir la ventana del navegador", y recargar no lo arregla | El navegador **Brave** bloquea la subida **[Experiencia]** | Subirlo desde **Safari** o Chrome |

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

### 3.14 Reglas de uso permitido — lo que confirmas en la casilla de cada permiso

Cada permiso del App Review termina con la casilla *"Si se aprueba, confirmo que cualquier
información que reciba a través de `<permiso>` se usará de acuerdo con el uso permitido"*. Esto es lo
que se está prometiendo y dónde validarlo.

**Dónde leer las reglas oficiales** (abrir en Safari o Chrome, no en Brave):

| Documento | URL | Para qué |
|---|---|---|
| Referencia de permisos | https://developers.facebook.com/docs/permissions/ | Cada permiso con su **"Allowed usage"** y sus dependencias. Buscar el nombre con Cmd + F |
| Política de Messenger y de mensajes de Instagram | https://developers.facebook.com/documentation/business-messaging/messenger-platform/policy | Ventana de 24 h, aviso de bot, qué se puede mandar. Aplica a `pages_messaging` **y** a `instagram_manage_messages` |
| Moderación de comentarios de Instagram | https://developers.facebook.com/docs/instagram-platform/comment-moderation/ | Qué se puede hacer con `instagram_manage_comments` |
| Condiciones de la plataforma | https://developers.facebook.com/terms/ | Reglas generales de datos: aplican a **todos** los permisos |
| Políticas para desarrolladores | https://developers.facebook.com/devpolicy/ | Igual, generales |

⚠️ Desde el servidor no se pudo abrir developers.facebook.com (bloqueado): los resúmenes de abajo salen
del texto que muestra el formulario de Meta (copiado por el dueño, marcado **[Formulario]**) y de
búsquedas **[Proveedor]**. Antes de marcar cada casilla, leer el permiso en la referencia oficial.

**Por permiso:**

| Permiso | Qué permite (según Meta) | Qué hace nuestro bot con él | Reglas que prometemos cumplir |
|---|---|---|---|
| `pages_messaging` | "Administrar y acceder a conversaciones de la página en Messenger… crear experiencias interactivas iniciadas por el usuario, enviar mensajes de servicio de atención al cliente o confirmar reservas, compras y pedidos" **[Formulario]**. Depende de `pages_manage_metadata` y `pages_show_list` **[Proveedor]** | Contesta los mensajes que la gente le manda a la página NovedadesJade | Solo contestar a quien escribió primero, **dentro de 24 h** de su último mensaje (el código manda `messaging_type: RESPONSE` y contesta al momento) · decir al inicio que es un asistente automático (lo hace en el primer mensaje) · nada de publicidad fuera de las 24 h · pasar a una persona cuando el bot no puede (escala y se pausa 30 min) |
| `instagram_manage_messages` | "Permite que los usuarios comerciales lean y respondan mensajes de Instagram Direct. El uso autorizado es que los negocios recuperen conversaciones y mensajes de su bandeja de entrada de Instagram Direct, administren los mensajes que intercambian con sus clientes o utilicen herramientas CRM externas" **[Formulario]**. Requiere `instagram_basic` en la solicitud **[Formulario]** | Contesta los mensajes directos a @novedades_bolsas_jade | Las mismas de Messenger: misma política. La etiqueta `human_agent` (7 días) solo la puede usar una persona, **nunca el bot** |
| `instagram_manage_comments` | "Crear, eliminar y ocultar comentarios en nombre de la cuenta de Instagram vinculada a una página… leer, actualizar y eliminar comentarios de cuentas de empresa de Instagram" **[Formulario]** | Contesta comentarios en las publicaciones propias | Solo comentarios de **nuestras** publicaciones · no ocultar ni borrar comentarios de clientes con el bot (hoy no lo hace) |
| `pages_manage_engagement` | "Crear, editar y eliminar comentarios publicados en la página… crear y eliminar los 'Me gusta' del contenido de tu propia página… con el objetivo de ayudar a administrar y moderar el contenido en la página" **[Formulario]**. Requiere `pages_show_list` y `pages_read_user_content` **[Formulario]** | Contesta comentarios en las publicaciones de la página de Facebook | Igual que el anterior, en Facebook |
| `pages_read_engagement` | "Leer contenido (publicaciones, fotos, videos y eventos) publicado por la página, leer datos de seguidores (nombre y PSID) y ver la foto del perfil, además de leer metadatos y otras estadísticas… con el objetivo de ayudar al administrador de una página a administrarla" **[Formulario]**. Requiere `pages_show_list` **[Formulario]** | Leer el comentario y su publicación | Leer solo lo de nuestra página |
| `pages_read_user_content` | "Leer el contenido de la página generado por los usuarios, como las publicaciones, los comentarios o las calificaciones… y eliminar los comentarios de los usuarios en las publicaciones de la página… El uso autorizado es leer el contenido de los usuarios y de otras páginas que se haya publicado en la página, siempre que sea necesario para administrarla" **[Formulario]**. Requiere `pages_show_list` **[Formulario]** | Recibe el comentario del cliente (webhook `feed`) y le contesta con `POST /{comment-id}/comments`. La llamada de prueba ya sale Completado, o sea que Meta registró que la usamos | Leer solo comentarios de clientes en **nuestra** página, para contestarlos · no borrar comentarios de clientes con el bot (hoy no lo hace) |
| `pages_manage_metadata` | "Suscribirse y recibir webhooks sobre actividades en la página, y actualizar los ajustes de esta… con el objetivo de ayudar al administrador de una página a administrarla" **[Formulario]**. Requiere `pages_show_list` en la misma solicitud **[Formulario]** | Suscribir la página a `feed`, `messages`, `message_echoes` | Usarlo solo para esa suscripción |
| `pages_show_list` | "Acceder a la lista de páginas que administra una persona… mostrarle la lista de páginas que administra y verificar que una persona administra una página" **[Formulario]** | Sacar el token de la página | — |
| `instagram_basic` | "Leer la información y el contenido multimedia del perfil de una cuenta de Instagram. El uso permitido es obtener metadatos básicos del perfil de una cuenta de empresa de Instagram, por ejemplo, un nombre de usuario o un identificador" **[Formulario]** | Ligar la publicación con el producto | — |

**Reglas que aplican a todos** (texto del formulario y Condiciones de la plataforma): usar los datos
**solo** para lo que se describió (atención a clientes); estadísticas solo "con información agrupada y
no identificada o anónima (siempre que esos datos no se puedan volver a identificar)"
**[Formulario]**; política de privacidad pública y forma de borrar datos
(`/privacidad`, `/eliminar-datos`); no vender ni pasar los datos a terceros fuera de los encargados
declarados en "Tratamiento de datos" (OpenAI, OVHcloud, Hosting Mexico).

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
| ¿Dónde podemos encontrar la app? (Configuración → Básica → instrucciones de prueba) | `https://www.instagram.com/novedades_bolsas_jade/` |
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

Facebook Login: not used. No end users log in to this app. The only access token is the Page
access token of our own Page, generated once by our Page administrator. The Meta APIs we use are
the Instagram messaging and comments endpoints listed below, plus the Page and Instagram
webhooks ("messages" and "comments" fields).

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

Until Advanced Access is approved, Meta only delivers these webhooks for accounts with a role
on the app, so replies will not reach an account without a role. The attached screen
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

### 2026-09-24 — Configuración de la app → Básica (revisada)

| Campo | Estaba | Debe quedar |
|---|---|---|
| Dominios de la app | `shop.novedades-jade.com.mx` | ✅ |
| Correo de contacto | Hotmail personal | ✅ (a Meta le sirve) |
| URL de la política de privacidad | `https://shop.novedades-jade.com.mx/privacidad` | ✅ |
| URL de Condiciones del servicio | `https://www.facebook.com/` → ✅ corregido | `https://shop.novedades-jade.com.mx/termConditions` |
| Eliminación de datos de usuario | `https://www.facebook.com/` → ✅ corregido | Elegir "URL de instrucciones" → `https://shop.novedades-jade.com.mx/eliminar-datos` |
| Ícono | `icono-app-1024.png` | ✅ |
| Categoría | Negocios y páginas | ✅ |
| Delegado de protección de datos (RGPD) | Nombre, Gmail personal y un domicilio incompleto → ✅ vaciado | **Vaciarlo.** Es opcional, solo aplica a negocios con actividad en la Unión Europea, y Meta lo publica en Facebook. El domicilio no coincide con el fiscal |
| Sitio web → URL del sitio | `…/login` de QA → ✅ corregido | `https://shop.novedades-jade.com.mx` |

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

**Modo de la app: PUBLICADA (Live)** — el menú izquierdo dice "Publicar · Publicada". O sea, ya no
está en modo desarrollo: lo que limita al bot a cuentas con rol es que los permisos siguen en acceso
**estándar**, no el modo. Por eso en las instrucciones para revisores va el párrafo "Until Advanced
Access is approved…".

### 2026-09-24 — Centro de cuentas del dueño (para elegir cuentas de prueba)

En el mismo Centro de cuentas están: el Facebook personal **Jade Castañeda**, los Instagram
`jade.castaneda.71868`, `trece0594`, `dulcek.garcia.3` y `novedades_bolsas_jade` (el negocio), un
WhatsApp y la página de Facebook "Rosas Eternas Caterine".

- Cuentas "cliente" para los videos: `trece0594` y `dulcek.garcia.3`. Nunca `novedades_bolsas_jade`.
- Tienen rol en la app **solo si el Facebook "Jade Castañeda" tiene rol**: revisar en Roles de la
  app → Roles. Si la app se creó con otro Facebook (el de Abel), agregar a "Jade Castañeda" como
  Evaluador y aceptar en developers.facebook.com/requests.

**Roles de la app (2026-09-24):** "Trece Trece" = Administrador (el Facebook con el que se creó la
app). "Jade Castañeda" = Evaluador, **Pendiente**: la invitación existe pero no se ha aceptado.
Hasta aceptarla, `trece0594` y `dulcek.garcia.3` no tienen rol y el bot no les contesta.
Para aceptar: entrar con el Facebook de Jade Castañeda a `developers.facebook.com/requests`.

### 2026-09-24 — Automatizaciones de Business Suite (Mensajes → Automatizaciones)

Estaban activas y hay que apagarlas (causaban doble respuesta y la pausa falsa del bot):
- **Respuesta automática** (Saludar a las personas). Aparecía también arriba como "Respuesta
  instantánea — usando un mensaje predeterminado".
- **Preguntas frecuentes** (Compartir información).
- **Mensaje automático** (Saludar a las personas).

Las tarjetas de "Sugerencias para ti" no están activas; no se tocan.

✅ 2026-09-24: las tres apagadas por el dueño (no se borraron, se pueden volver a prender).

### 2026-09-24 — "Permitir acceso a los mensajes" en novedades_bolsas_jade

✅ Encendido. En esta versión de Instagram **no está** en "Controles de mensajes": está en
Configuración → Mensajes y respuestas a historias → **Solicitudes de mensajes** → Herramientas
conectadas. "Quién puede enviarte solicitudes de mensajes" = Todos.

### 2026-09-24 — Decisión: el webhook se queda en QA hasta que aprueben la app

El dueño decidió no mover el webhook a prod todavía: las credenciales de prod se cargan cuando Meta
apruebe el acceso avanzado. Los videos y las pruebas se hacen contra QA (`inventario_key_qa`).

**Incidente 2026-09-24:** el merge `main → dev` duplicó el bloque de `standby` en
`FacebookWebhookController` y `dev`/`qa` no compilaban, así que el deploy de QA falló (QA siguió con
la imagen anterior). Corregido en `e9bd64d` y subido a `dev`/`qa`. Lección: compilar después de cada
merge, no solo antes.

**Diagnóstico 2026-09-24:** en `inventario_key_qa`, `mensaje_directo_pausa` y
`mensaje_directo_social` están **vacías**: nunca ha llegado un mensaje directo a QA, aunque los
comentarios sí llegan. El problema es de Meta, no del código. Sospechas, en orden:
1. El objeto Instagram del webhook no está suscrito al campo `messages` (probar con el botón "Probar").
2. El Page Access Token de QA se generó sin `instagram_manage_messages` (revisar en el Depurador de tokens).

**Revisión por API desde la VPS (2026-09-24)** — comandos en el chat de esa fecha: `/{app}/subscriptions`,
`/debug_token` y `/{page}/subscribed_apps`, usando las llaves del pod de QA sin mostrarlas.
- Suscripción `instagram` → `https://qa.backend.novedades-jade.com.mx/mis-productos/v1/redes-sociales/facebook/webhook`,
  campos `comments` y `messages` ✅.
- Suscripción `page` → **`https://hook.eu1.make.com/...` (Make.com)**, campo `feed`. ⚠️ Los comentarios
  de **Facebook** van a Make.com, no a QA, así que el bot de comentarios de Facebook de QA no los recibe.
  Hay que confirmar con el dueño si Make.com se sigue usando.
- Token de página de QA: válido, no expira, **sí tiene** `instagram_manage_messages` ✅ (descarta la sospecha 2).
- La página tiene instalada la app novedadesJade, pero **solo con `subscribed_fields: feed`**. Sospecha
  nueva: falta `messages` en la suscripción de la página para que Meta entregue los mensajes directos
  de Instagram.
- Intento de agregar `messages` a la página: Meta responde `(#200) … needs pages_messaging`. Ese campo
  es el de **Messenger** (mensajes a la página de Facebook), no el de Instagram, así que probablemente
  **no** es la causa. No se pidió `pages_messaging`.
- Siguiente prueba para separar causas: comentar desde `trece0594` y ver si llega el comentario
  (si llega → la cuenta sí tiene rol y el problema es solo de mensajes directos).
- Resultado: ni el comentario ni el mensaje directo de `trece0594` llegaron. En nginx, el último
  `POST` de Meta al webhook de QA fue a las 01:33 UTC (con `200`) y después nada. **La conexión funciona;
  Meta descarta lo de `trece0594` porque todavía no tiene rol** (Jade Castañeda seguía "Pendiente").
  Comando útil: `sudo grep -h "facebook/webhook" /var/log/nginx/*access*.log | tail`.
- **Corrección:** en `comentario_social` (QA) sí quedó el comentario "Que precio tiene" del
  2026-09-23 21:09 (hora de México = 03:09 UTC del 24), contestado con **"¡Hola! Soy el asistente
  automático de No…"**. Los comentarios de Instagram **funcionan** con la versión nueva y la cuenta de
  prueba **sí tiene rol**. Lo que no llega son solo los **mensajes directos**. Nota: `fecha` en la base
  está en hora de México (UTC−6), nginx en UTC.
- Sospechas para los mensajes directos: (a) enrutamiento de conversaciones: otra app (la bandeja de
  Business Suite) es la predeterminada y a la nuestra solo le llegaría `standby`, que no está suscrito;
  (b) el mensaje cayó en "Solicitudes de mensajes" y Meta no lo entrega hasta aceptarlo.
- ✅ El dueño confirmó en Instagram que el bot **sí contestó el comentario** (la respuesta sale
  colapsada, en "Ver respuestas"). **El video de comentarios ya se puede grabar.** Falta resolver los
  mensajes directos.
- Chat de `trece0594` en novedades_bolsas_jade: está en **Solicitudes de mensajes** ("¿Aceptar la
  solicitud de mensaje…?"). Adentro se ve la respuesta morada de Meta ("¡Hola! Gracias por ponerte en
  contacto…"), que era la **Respuesta automática de Business Suite** ya apagada, y después dos "Hola"
  sin respuesta. Siguiente prueba: aceptar la solicitud y mandar un mensaje nuevo.
- Resultado: aceptada la solicitud y mandado un mensaje nuevo, **Meta no manda nada** (en nginx el
  último `POST` es el del comentario de las 03:09 UTC). Descartadas: URL, suscripción a `messages`,
  permisos del token, rol de la cuenta y solicitud pendiente. **Queda: enrutamiento de conversaciones**
  en Business Suite (la app predeterminada para los mensajes directos de Instagram no es novedadesJade).

### 2026-09-24 — ⚠️ Información del negocio del portafolio (bloquea la verificación)

Business Suite → Configuración → Información del negocio tiene hoy:
- **Nombre legal:** "Tortilleria la Salida". Debe ser **idéntico al de la Constancia de Situación
  Fiscal** (persona física: el nombre completo del dueño) y al que se puso como responsable en
  Tratamiento de datos. Si no coincide, Meta rechaza la verificación.
- **Dirección:** "51440 / 51440 / Mexico, Mexico 51440": incompleta. Debe ser el domicilio fiscal de
  la CSF completo (calle, número, colonia, CP, municipio, estado).
- Teléfono: +52 722 111 1793. Sitio web: `https://shop.novedades-jade.com.mx/` ✅.

Corregir con "Editar" en "Información del negocio" **antes** de iniciar la verificación.

**Constancia de Situación Fiscal revisada (2026-09-24)** — sin anotar RFC, CURP ni domicilio aquí:
- Emitida en **abril de 2022**: demasiado vieja, hay que sacar una del mes.
- Régimen: **solo "Sueldos y Salarios"**. No demuestra actividad de negocio; Meta lo rechaza.
- Domicilio registrado: el **del patrón**, en CDMX. No es el del negocio ni coincide con el portafolio.
- Nombre: coincide con el responsable de Tratamiento de datos ✅.
- **Qué falta:** constancia nueva. Si sigue en Sueldos y Salarios, alta en RESICO o Actividades
  Empresariales y cambio de domicilio fiscal, **con un contador** (genera obligaciones fiscales).
  Después: corregir nombre legal y domicilio del portafolio y recién ahí iniciar la verificación.

Enrutamiento de conversaciones (según la ayuda de Meta): Business Suite → Configuración →
**Integraciones → Enrutamiento de conversaciones** → cuenta de Instagram → pestaña Enrutamiento →
"Enrutamiento predeterminado" → ⋯ → Editar.

### 2026-09-24 — Messenger: permiso, token nuevo y suscripciones

- **Permiso:** developers.facebook.com → Casos de uso → Agregar casos de uso → "Interactuar con los
  clientes en Messenger from Meta". `pages_messaging` queda "Listo para la prueba" (obligatorio del
  caso de uso). Tiene **0 llamadas**: para mandarlo a App Review, Meta pide al menos una llamada real,
  así que primero el bot tiene que contestar un Messenger.
- **Token de página nuevo** (el viejo no traía `pages_messaging`): Graph API Explorer → "Token del
  usuario" con los 12 permisos → Generate → ⓘ → Extender → `me/accounts` → `access_token` de la
  página **NovedadesJade, id `645820348605806`** (no "Novedades Jade" `1275448475648441`).
- En QA el token **no está en un secret**: está escrito directo en el deployment. Se cambió con
  `kubectl -n qa set env deployment/proyecto-key-deployment FACEBOOK_PAGE_ACCESS_TOKEN=...` (con
  `read -s`, sin dejarlo en el historial). `set env` reinicia el pod solo.
- **Página** (`POST /{page}/subscribed_apps`): `feed`, `messages`, `message_echoes` ✅.
  `message_echoes` es el aviso de que el admin contestó a mano desde Messenger (pausa de 30 min); en
  Instagram ese aviso ya viene dentro de `messages`.
- **Webhook de la app** (`POST /{app}/subscriptions`, `object=page`): `feed`, `messages`,
  `message_echoes` ✅. Instagram: `comments`, `messages` ✅.
- ⚠️ El primer intento dio `(#2200) Callback verification failed ... 502`: el backend de QA tarda
  **~270 s en arrancar** y `rollout status` termina antes. Antes de suscribir, confirmar que responde:
  `curl -s -o /dev/null -w "%{http_code}" ".../facebook/webhook?hub.mode=subscribe&hub.verify_token=x&hub.challenge=1"`
  → debe dar **403** (token falso rechazado = backend vivo); 502 = sigue arrancando.
- ✅ **Messenger funcionando en QA:** mensaje desde Trece Trece a NovedadesJade, el bot contestó.
  Estado al cierre del día: comentarios de Facebook ✅, comentarios de Instagram ✅, Messenger ✅,
  **mensajes directos de Instagram ❌** (siguen sin llegar: enrutamiento de conversaciones).
- Comentario "Hola" en Facebook contestado solo con "¡Hola! 😊": seco, sin agradecimiento. Se cambió
  para que saludos y halagos se contesten con texto fijo ("¡Hola! 😊 Gracias por tu comentario 💖")
  y el chatbot solo clasifique (`##GRACIAS##` / `##ESCALAR##`). Ver `botredes/README.md`, R2.
- **Enrutamiento de conversaciones** (Business Suite → Configuración → Integraciones → Enrutamiento
  de conversaciones): solo aparece la página de Facebook, **Instagram no aparece**. En "Apps de socios"
  de la página hay dos apps: **novedadesJade** (la nuestra) y **Manychat** (id `532160876956612`), que
  nadie usa. El dueño le quitó los permisos "Acceder a todas las conversaciones" y "Tomar el control de
  las conversaciones"; desde ahí no deja eliminarla. Para quitarla del todo: Facebook como la página →
  Configuración → **Integraciones comerciales** → Manychat → Eliminar. Sospecha: Manychat se quedaba
  con los mensajes directos de Instagram y a nuestra app solo le llegaba `standby`, que no escuchamos.

### 2026-09-24 — App Review: subiendo los videos (capturas del dueño)

- **Captura 37** (Revisar → Revisión de la app): el primer permiso (su nombre no sale en la captura) y
  `pages_manage_metadata` solo tienen ✅ en "llamadas de prueba a la API" (metadata además ✅ "debe
  contener pages_show_list"). Les falta descripción, video, uso permitido e instrucciones para
  reproducir. `instagram_manage_comments` ya tiene ✅ la descripción; le falta el video.
- **Captura 38**: al subir el video en `instagram_manage_comments` sale "Se produjo un error.
  Actualiza la página…", y recargar no lo arregla. El navegador es **Brave con Shields activo** (5
  bloqueos en developers.facebook.com).
- ✅ **Confirmado: era el navegador.** En Brave no subió ni recargando; en **Safari** el mismo video
  subió al primer intento ("Tu video se está procesando. Te enviaremos una notificación cuando esté
  listo"). No hizo falta convertir a MP4 ni reducir el tamaño: Meta acepta el `.mov` de Cmd+Shift+5.
  **Regla: todo lo de developers.facebook.com hacerlo en Safari o Chrome, nunca en Brave.**
- Video de comentarios de Instagram: **`videoRedesInstaComentariosv2`** → subido en
  `instagram_manage_comments`.

**Qué video va en qué permiso** (un video por permiso; nunca el de Facebook en uno de Instagram):

| Video | Permiso |
|---|---|
| Comentarios de Instagram | `instagram_manage_comments` |
| Comentarios de Facebook | `pages_manage_engagement` |
| Mensajes directos de Instagram | `instagram_manage_messages` |
| Messenger (mensajes a la página) | `pages_messaging` |
| Dependencias sin efecto visible (`pages_manage_metadata`, `pages_show_list`, `pages_read_engagement`, `instagram_basic`) | El video de la función que hacen posible, diciéndolo en la descripción. **[Sin confirmar]** que Meta lo acepte siempre |

Si en la solicitud aparece un permiso que el bot no usa (`pages_manage_posts`,
`instagram_content_publish`), quitarlo: pedir de más es de los rechazos más comunes (3.5).

**Descripción de `instagram_manage_comments` — corregida.** La primera versión decía que si el bot no
sabe la respuesta "no publica nada y avisa por correo". Ya no es así: contesta en público "En un
momento te compartimos la información 💖", avisa al admin y deja de contestarle a esa persona en ese
post por 30 min. Meta compara el texto con el video, así que el segundo párrafo quedó:

```
When a customer comments on one of our posts (for example, asking about price, availability or product details), our system receives the event via webhook, looks up the product linked to that post in our catalog, and replies to the comment with that information. If the bot does not have the information, it replies with a short message saying we will follow up, notifies the business administrator so a person can answer, and stops replying to that customer on that post for 30 minutes. The bot also thanks customers who comment that they shared or followed our page. If the administrator replies to a comment manually, the bot pauses for that customer. Replies are in Spanish because our customers are in Mexico.
```

⚠️ El texto para revisores (`instructions-web-2`, arriba) tiene el mismo error en el punto 1 de
mensajes directos ("it does not reply and forwards the message by email"). Corregirlo igual antes de
enviar: el bot contesta "En un momento te atendemos 💖" y avisa al admin.

### 2026-09-24 — `pages_messaging`: cómo llenar cada campo

Pantalla: Revisar → Revisión de la app → "¿Cómo usará la app pages_messaging?" → Empezar.
"Llamadas de prueba a la API" ya sale **Completado**.

1. **Indícanos por qué solicitas pages_messaging** (en inglés; tiene que decir lo mismo que el video):

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico that sells bags and accessories. We use pages_messaging so an automated assistant can answer customers who send a message to our own Facebook Page (NovedadesJade) on Messenger.

When a customer writes to the Page, our server receives the Messenger webhook and replies with the Send API (messaging_type RESPONSE), only in reply to a conversation the customer started and always within the 24-hour standard messaging window. The first reply says it is the automated assistant of Novedades Jade. The assistant answers questions about our products, prices and availability using our own catalog. If it cannot answer, it replies that a person will attend them shortly and notifies our team, and a person continues the conversation from the Page inbox. When someone from our team replies manually, the assistant stops replying to that customer for 30 minutes.

We never send promotional or unsolicited messages, and we use these conversations only to provide customer service. Without this permission the assistant cannot reply on Messenger, and customers who write outside business hours would wait hours for an answer.
```

2. **Prueba y reproduce → Selecciona una página:** **NovedadesJade** (id `645820348605806`), no
   "Novedades Jade" (`1275448475648441`), que es otra.
   Meta pide una **cuenta real de Facebook con rol de Evaluador** (Roles de la app → Evaluadores). Los
   "usuarios de prueba" creados en Roles de la app **no sirven**: no reciben mensajes de bots. Hay que
   crear una cuenta de Facebook dedicada (no la personal), invitarla como evaluador y aceptar la
   invitación desde esa cuenta. Sus datos de acceso van en **Instrucciones para revisores**. Riesgo
   **[Sin confirmar]**: Facebook puede bloquear el inicio de sesión del revisor desde otro país si la
   cuenta es nueva o tiene verificación en dos pasos.
3. **Video:** el de **Messenger** (no el de mensajes directos de Instagram). Debe verse: la cuenta del
   cliente escribiendo a NovedadesJade en Messenger, la primera respuesta "Soy el asistente automático
   de Novedades Jade", y de preferencia una pregunta que el bot no sabe → "En un momento te atendemos"
   → la respuesta de una persona desde la bandeja de la página. Subirlo en **Safari**.
4. **Casilla de uso permitido:** marcarla después de leer las reglas de 3.14.

**Captura 39** (formulario de `pages_messaging`): la descripción ya pegada; debajo, "Prueba y
reproduce la funcionalidad de tu integración" tiene un desplegable **"Selecciona una página"** y un
cuadro de instrucciones paso a paso (el ejemplo de Meta empieza con un enlace `m.me`). Ahí va el texto
del punto 5, con el enlace `https://m.me/645820348605806` (id de NovedadesJade) — probarlo antes en
Safari: debe abrir el chat de Messenger con NovedadesJade.
✅ Probado por el dueño (2026-09-24): el enlace abre directo la conversación de Messenger con NovedadesJade.
5. **Instrucciones para reproducir:**

```
1. Log in to Facebook with the tester account given in the reviewer instructions.
2. Open https://m.me/645820348605806 (our Page "NovedadesJade" in Messenger) and send a message, for example: "¿Tienen bolsas negras?" ("Do you have black bags?").
3. Within about 20 seconds the automated assistant replies in Spanish. The first reply says it is the automated assistant of Novedades Jade.
4. Send a question the assistant cannot answer, for example "¿Me pueden llamar?" ("Can you call me?"). The assistant replies that a person will attend you shortly, and a member of our team answers from the Page inbox.
```

### 2026-09-24 — `pages_manage_metadata`: cómo llenar cada campo

Pantalla: Revisar → Revisión de la app → "¿Cómo usará la app pages_manage_metadata?" → Empezar.
"Llamadas de prueba a la API" ya sale **Completado**, y el requisito "La solicitud debe contener
pages_show_list" ya salía ✅ (captura 37). Este formulario **no** tiene "Selecciona una página" ni
instrucciones para reproducir: solo descripción, video y casilla.

1. **Descripción** (cuadro debajo de "Proporciona una descripción detallada…"):

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico. We use pages_manage_metadata to subscribe our own Facebook Page (NovedadesJade) to our app's webhooks for the fields "feed", "messages" and "message_echoes". We do not change any other Page settings.

These webhooks are what make our automated customer service assistant work: "feed" tells our server when a customer comments on one of our Page's posts, so the assistant can reply to that comment; "messages" tells it when a customer writes to our Page on Messenger, so the assistant can reply; and "message_echoes" tells it when someone from our team has replied manually, so the assistant pauses and lets that person continue the conversation.

Without this permission our server would not receive these events and the assistant could not answer our customers. The screen recording shows a customer writing to our Page and the assistant replying, which only happens because of this webhook subscription.
```

2. **Video:** este permiso no tiene nada visible propio (es la suscripción a los webhooks). Se sube el
   video de **Messenger** o el de **comentarios de Facebook**: los dos solo funcionan gracias a esta
   suscripción, y la descripción lo explica. **[Sin confirmar]** que Meta lo acepte; si lo rechaza,
   grabar un video que además muestre la llamada `POST /{page-id}/subscribed_apps` en el Explorador
   de la API Graph (sin enseñar el token).
3. **Casilla de uso permitido:** marcarla después de leer la fila de `pages_manage_metadata` en 3.14.

Decisión del dueño (2026-09-24): en `pages_manage_metadata` se sube el **video de Messenger de Facebook** (nombre del archivo: pendiente de anotar). El mismo video va también en `pages_messaging`.

### 2026-09-24 — `pages_read_user_content`

Todo en ✅ (descripción, llamadas de prueba, `pages_show_list`) menos el video. **Video: el de
comentarios de Facebook**, porque este permiso es el que deja leer el comentario que escribe un
cliente en la página, que es lo que se ve cuando el bot lo contesta. El código no hace ningún `GET`
de comentarios: el texto llega en el webhook `feed` y se contesta con `POST /{comment-id}/comments`.
Si Meta pidiera más, el video tendría que enseñar el comentario del cliente y la respuesta del bot en
el mismo hilo.

✅ 2026-09-24: el dueño subió el **video de comentarios de Facebook** en `pages_read_user_content` (nombre del archivo: pendiente de anotar).

### 2026-09-24 — `pages_manage_posts`: no va en esta revisión

Apareció en la solicitud con todo ✅ menos el video. **No lleva video: hay que quitarlo de la
solicitud.** El bot no lo usa; lo usa `PublicacionSocialService`, que publica fotos, videos y reels en
la página desde el panel admin (`/{page-id}/photos`, `/videos`, `/video_reels`). Eso lo hace el propio
dueño, que tiene rol en la app y en la página, así que el **acceso estándar alcanza** (3.5): el
acceso avanzado solo hace falta para datos de gente sin rol. Pedirlo de más es de los rechazos más
comunes y puede tumbar toda la solicitud.

- Cómo quitarlo: en la lista de permisos de la solicitud, el botón de quitar/eliminar junto a
  `pages_manage_posts` **[Sin confirmar dónde está exactamente]**.
- Si Meta no deja quitarlo (por venir amarrado a un caso de uso), plan B: grabar el panel admin
  publicando una foto en la página y que se vea la publicación en Facebook, con una descripción que
  diga que solo el dueño publica en su propia página.
- Mismo caso para `instagram_content_publish` si aparece.
- **Pregunta del dueño: ¿quitarlo rompe publicar desde la app web?** No. Quitarlo de la revisión no
  le quita el permiso a la app: se queda con **acceso estándar**, que funciona para cuentas con rol
  (el dueño). Condiciones para que siga funcionando: (1) el token de página lo genera siempre alguien
  con rol en la app y en la página; (2) al regenerar el token en el Explorador de la API Graph, seguir
  marcando `pages_manage_posts` (y `instagram_content_publish` para Instagram). Prueba: si hoy
  publicar desde la app web funciona, después de la revisión sigue igual, porque la revisión no toca
  el acceso estándar.
- **Resultado (2026-09-24):** en la solicitud **no hay ningún botón** para quitar
  `pages_manage_posts` (ni "Quitar", ni X, ni "…"). Se va con el **plan B**: se deja y se justifica,
  porque la app **sí** lo usa (publicar desde el panel admin). El código solo crea publicaciones
  (`/photos`, `/videos`, `/video_reels`); no edita ni borra ninguna.
  - Descripción:

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico. The business owner uses our internal admin panel to publish product photos, videos and reels to our own Facebook Page (NovedadesJade) with pages_manage_posts, so our online store catalog and our Page stay in sync without uploading each product twice.

Only the Page owner, who is an admin of the Page, publishes through our app; customers never publish anything. We only create posts on our own Page. We do not edit or delete posts, and we never publish on pages we do not own.

The screen recording shows the owner creating a post from our admin panel and the new post appearing on our Facebook Page.
```

  - Video nuevo: entrar al panel admin → pantalla de publicar en redes → elegir una foto, escribir
    el texto → publicar en Facebook → abrir `facebook.com` en la página NovedadesJade y mostrar la
    publicación nueva. En Safari, 1080p, sin mostrar tokens. La publicación de prueba es real: se
    puede borrar a mano desde Facebook después de grabar.

### 2026-09-24 — `pages_show_list`

No pide llamadas de prueba ni página. Solo descripción, video y casilla. Uso real: sacar el token de
la página con `me/accounts` en el Explorador de la API Graph (3.9 y "Token de página nuevo").

1. **Descripción:**

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico. We use pages_show_list when the business owner sets up our automated customer service assistant: using Meta's Graph API Explorer, the owner lists the Facebook Pages they manage (/me/accounts) to confirm they administer our Page (NovedadesJade) and to obtain that Page's access token.

That Page token is what our server uses for the webhooks and replies described in pages_messaging, pages_manage_metadata and pages_read_user_content. We do not show, store or use the list of Pages for any other purpose, and we only work with our own Page.

The screen recording shows the owner listing the Pages they manage and finding NovedadesJade.
```

2. **Video propio (1 minuto):** developers.facebook.com/tools/explorer → Meta App **novedadesJade**
   → Usuario o página: **Token del usuario** → en Permisos agregar `pages_show_list` → **Generate
   Access Token** (sale el cuadro de consentimiento de Facebook: dejarlo en el video) → en la consulta
   escribir `me/accounts?fields=id,name` → **Enviar** → en la respuesta se ve NovedadesJade.
   ⚠️ **El token se ve en la columna derecha del Explorador.** Grabar con Cmd + Shift + 5 → **"Grabar
   parte seleccionada de la pantalla"** y dejar fuera esa columna. Pedir solo `fields=id,name` para
   que la respuesta no traiga el `access_token` de la página. Generar un token de usuario nuevo no
   invalida el token de página que usa el bot.
3. **Casilla** de uso permitido.

Nota 2026-09-24: en el Explorador, la lista de **Permisos** ya trae las 12 opciones seleccionadas
(pages_show_list, business_management, pages_messaging, instagram_basic, instagram_manage_comments,
instagram_content_publish, instagram_manage_messages, pages_read_engagement, pages_manage_metadata,
pages_read_user_content, pages_manage_posts, pages_manage_engagement). **No tocarla**: dejarla igual
para que el token nuevo traiga los mismos permisos. Antes de subir el video, revisarlo completo: si
en algún cuadro se ve el token (texto largo que empieza con `EAA…`), no subirlo y grabar de nuevo.

### 2026-09-24 — `pages_manage_engagement`

Es el permiso con el que el bot **contesta** los comentarios de Facebook (`POST /{comment-id}/comments`).
Llamadas de prueba ya en **Completado**. Video: el mismo de **comentarios de Facebook** que se subió en
`pages_read_user_content` (leer el comentario y contestarlo se ven en la misma grabación).

Descripción:

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico. We use pages_manage_engagement so our automated customer service assistant can reply to comments that customers leave on the posts of our own Facebook Page (NovedadesJade).

When a customer comments on one of our posts (for example, asking about price or availability), our server receives the comment through the Page webhook and replies in the same thread with information from our product catalog. If the assistant does not have the answer, it replies that we will share the information shortly and notifies our team so a person can answer. It also thanks customers who comment that they shared or followed our Page. If someone from our team replies to a comment manually, the assistant stops replying to that customer on that post.

We only create replies on our own Page. We do not edit or delete customers' comments and we do not like or unlike content. Without this permission the assistant could not reply to our customers' comments.

The screen recording shows a customer commenting on one of our posts and the assistant's reply appearing under the comment.
```

### 2026-09-24 — `business_management`: quitarlo de la revisión

Meta lo describe así: "leer y escribir con la API del administrador comercial. El uso permitido es
administrar activos comerciales, como cuentas publicitarias, y reclamar este tipo de cuentas"
**[Formulario]**. **El código no lo usa**: no hay ninguna llamada a `/businesses`, `owned_pages` ni
`client_pages` (revisado con grep). Se quita de la solicitud, igual que `pages_manage_posts`.

Quitarlo de la revisión no le quita el acceso estándar: si al sacar el token de página en el
Explorador hiciera falta (páginas de un portafolio comercial), sigue funcionando porque el dueño tiene
rol. Al regenerar el token, dejarlo marcado igual que hoy.

### 2026-09-24 — Lista completa de la solicitud y qué falta

Pantalla: Revisar → Revisión de la app → "Solicitudes de revisión de apps" (estado **No enviada**).
Al pie dice: *"Para eliminar permisos y funciones de tu app, no solo de la revisión de la app,
personaliza los casos de uso"* → quitar de la revisión y quitar de la app son cosas distintas.
`pages_manage_posts` ya no aparece: el dueño lo quitó de la revisión.

| Permiso en la solicitud | ¿Se queda? | Video |
|---|---|---|
| `pages_messaging` | ✅ Sí — bot de Messenger | Messenger |
| `pages_manage_metadata` | ✅ Sí — suscripción a webhooks | Messenger |
| `instagram_manage_comments` | ✅ Sí — bot de comentarios de Instagram | `videoRedesInstaComentariosv2` |
| `pages_read_user_content` | ✅ Sí — leer comentarios de clientes | Comentarios de Facebook |
| `pages_show_list` | ✅ Sí — sacar el token de la página | Explorador `me/accounts` |
| `pages_manage_engagement` | ✅ Sí — contestar comentarios de Facebook | Comentarios de Facebook |
| `pages_read_engagement` | ✅ Sí — dependencia de los de página | Pendiente de llenar |
| `instagram_basic` | ✅ Sí — base de todo lo de Instagram | Pendiente de llenar |
| `public_profile` | ✅ Se deja — Meta lo agrega a todas las apps | Pendiente de ver qué pide |
| `business_management` | ❌ Quitar — el código no lo usa | — |
| **`instagram_manage_messages`** | ⚠️ **FALTA agregarlo** — sin él, el bot de mensajes directos de Instagram solo contesta a cuentas con rol | Mensajes directos de Instagram |

**Dónde se agrega (confirmado con capturas 8, 12 y 16):**
- **Capturas 8 y 12** — menú izquierdo → **Casos de uso**: la app tiene "Administrar todos los aspectos
  de tu página" y **"Administrar mensajes y contenido en Instagram"**, cada uno con botón
  **Personalizar** (después se agregó también "Interactuar con los clientes en Messenger from Meta").
  Capturas 13–15: el cuadro "Agrega más casos de uso" (botón "Agregar casos de uso").
- **Captura 16** — adentro de Personalizar, la lista de permisos. Cada fila tiene: nombre, número de
  llamadas, estado ("Listo para la prueba") y a la derecha **"+ Agregar a revisión de la app"** (si no
  está en la solicitud) o **"Acciones ▾"** (si ya tiene algo). En "Acciones" salen **"Ir a revisión de
  la app"** y **"Obligatorio para el caso de uso"** (gris = no se puede quitar). Ahí
  `instagram_manage_messages` salía con **6 llamadas**, "Listo para la prueba", "Se encontró en 2 casos
  de uso".
- Pasos: Casos de uso → **Administrar mensajes y contenido en Instagram** → **Personalizar** → Cmd + F
  `instagram_manage_messages` → **"+ Agregar a revisión de la app"**, o **Acciones ▾ → Agregar a
  revisión**. Hacerlo desde el caso de uso de Instagram, no desde el de Messenger (mezclar productos es
  motivo de rechazo, 3.6).

**Pantalla "Casos de uso"** — URL de esta app:
`https://developers.facebook.com/apps/1017171384561253/use_cases/?business_id=636203476989310`
(app `1017171384561253`, portafolio comercial `636203476989310`). Ahí están los casos de uso que ya
tiene la app, cada uno con **Personalizar** (adentro: la lista de permisos, captura 16), y arriba a la
derecha el botón **"Agregar casos de uso"**.

**Capturas 40, 41 y 42 — ventana "Agrega más casos de uso"** (se abre con "Agregar casos de uso"). Sirve
solo para agregar casos de uso **nuevos**; **no es donde se agregan permisos a la revisión**. Si se abre
por error: cerrar con la X sin marcar nada. Aviso de Meta: *"no todos los casos de uso se pueden
agregar a la misma app. Crea una app nueva si los casos de uso que quieres agregar no están
disponibles"*. Filtros: Destacados (4) · **Todo (13)** · Anuncios y monetización (7) · Administración
de contenido (4) · Mensajes comerciales (1) · Otros (1). Los 13 que ofrece (ninguno hace falta para el
bot):

1. Crear y administrar anuncios con la API de marketing
2. Medir datos de rendimiento de los anuncios con la API de marketing
3. Captar y administrar clientes potenciales de anuncios con la API de marketing
4. Crear y administrar anuncios sobre apps con el administrador de anuncios de Meta
5. Acceder a la API de Threads
6. Crear y administrar anuncios con el servidor MCP para anuncios
7. Anúnciate en tu app con Meta Audience Network
8. Administrar productos con la API de catálogos
9. Comparte o crea recaudaciones de fondos en Facebook e Instagram
10. Accede a la API de video en vivo
11. Insertar contenido de Facebook, Instagram y Threads en otros sitios web (oEmbed)
12. Conectarte con los clientes a través de WhatsApp (requiere portafolio comercial; ver 3.13)
13. Haz un seguimiento de la interacción con la herramienta de eventos de la app de Meta

En la captura 14 (madrugada) eran 14 e incluía **"Interactuar con los clientes en Messenger from
Meta"**; ya no sale porque se agregó a la app ese mismo día (ver "Messenger: permiso, token nuevo").
Para una cuenta nueva, los casos de uso que necesita el bot son: **Administrar todos los aspectos de
tu página**, **Administrar mensajes y contenido en Instagram** e **Interactuar con los clientes en
Messenger from Meta**.

**Adentro de Personalizar del caso de uso de Instagram (2026-09-24):** el selector de caso de uso dice
**"API de Instagram"** (antes la tarjeta se llamaba "Administrar mensajes y contenido en Instagram").
Abre en la pestaña de configuración (`selected_tab=API-Setup`): app de Instagram **novedadesJade-IG**
(id `1355584923399178`), y pasos 1–5 (permisos, tokens, webhooks, inicio de sesión de empresa de
Instagram, revisión). Esa pestaña lista permisos **`instagram_business_*`** (`instagram_business_basic`,
`instagram_business_manage_comments`, `instagram_business_manage_messages`): son de la variante **"API
con inicio de sesión de Instagram"**.

⚠️ **Trampa: hay dos juegos de permisos de Instagram con nombres casi iguales.**
- `instagram_business_*` → variante con inicio de sesión de **Instagram** (token de Instagram).
- `instagram_manage_messages`, `instagram_manage_comments`, `instagram_basic` → variante con inicio de
  sesión de **Facebook** (token de la **página**).

Nuestro código usa el **token de la página** (`/{page-id}/messages`, `/{comment-id}/replies`), o sea la
variante de **Facebook**. Por eso la solicitud ya tiene `instagram_manage_comments` e `instagram_basic`,
y el que falta es **`instagram_manage_messages`** (sin "business"), que ya tenía 6 llamadas.
**No agregar ni pedir los `instagram_business_*`**, y no tocar el enlace "cambia a API setup with
Facebook login". El botón "+ Agregar a revisión de la app" está en la pestaña/página **"Permisos y
funciones"** del mismo Personalizar (captura 16), no en la de configuración.

**Captura 43 — así se agregó `instagram_manage_messages` (2026-09-24):** en Personalizar de "API de
Instagram", bajando en la lista de permisos (Cmd + F `instagram_manage_messages`), la fila del permiso
tiene **Acciones ▾** (las filas que no están en la solicitud tienen **"+ Agregar a revisió…"**). Al
pedirlo sale el aviso *"Requesting advanced access for this privilege will affect other use cases on
this app — Este privilegio se encuentra en uno o varios casos de uso más en esta app. Al solicitarlo,
también se solicitará en los casos de uso que figuran a continuación: Interactuar con los clientes en
Messenger from Meta"*. Es normal (el mismo permiso vive en los dos casos de uso): botón **Request**.

✅ **Resultado:** la solicitud ahora tiene 11: `business_management` (❌ falta quitarlo),
**`instagram_manage_messages`** (✅ agregado), `pages_messaging`, `pages_manage_metadata`,
`instagram_manage_comments`, `pages_read_user_content`, `pages_show_list`, `pages_manage_engagement`,
`pages_read_engagement`, `public_profile`, `instagram_basic`. No falta ninguno; solo sobra
`business_management`.

✅ **Lista final (2026-09-24):** `business_management` quitado. Quedan **10**: `instagram_manage_messages`,
`pages_messaging`, `pages_manage_metadata`, `instagram_manage_comments`, `pages_read_user_content`,
`pages_show_list`, `pages_manage_engagement`, `pages_read_engagement`, `public_profile`,
`instagram_basic`. Esta es la lista correcta para el bot; para otra cuenta, pedir exactamente estos.

### 2026-09-24 — `instagram_manage_messages`: cómo llenar cada campo

No pide llamadas de prueba ni página. Requisito: `instagram_basic` en la solicitud (ya está). Arriba
trae dos avisos con enlace **"View requirements"**: uno si se usa para cuentas de **otros** negocios y
otro para la cuenta **propia**. El nuestro es el de la cuenta **propia**: abrir su "View requirements"
y anotar aquí qué pide.

1. **Descripción:**

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico, and only with our own Instagram professional account (@novedades_bolsas_jade), which is linked to our Facebook Page. We use instagram_manage_messages so an automated customer service assistant can answer customers who send a Direct Message to our account.

When a customer writes to us, our server receives the Instagram "messages" webhook and replies through our Page with the messaging API, only in reply to a conversation the customer started and within the 24-hour messaging window. The first reply says it is the automated assistant of Novedades Jade. The assistant answers questions about our products, prices and availability using our own catalog. If it cannot answer, or the customer sends a photo, audio or sticker, it replies that a person will attend them shortly and notifies our team, and a person continues the conversation from the Instagram inbox. When someone from our team replies manually, the assistant stops replying to that customer for 30 minutes.

We never send promotional or unsolicited messages, we do not use the human_agent tag for automated replies, and we use these conversations only to provide customer service. Without this permission the assistant cannot reply to Instagram Direct messages, and customers who write outside business hours would wait hours for an answer.

The screen recording shows a customer sending a Direct Message to @novedades_bolsas_jade and the assistant's reply.
```

2. **Video:** el de **mensajes directos de Instagram** (no el de Messenger). Debe verse la cuenta del
   cliente escribiéndole a @novedades_bolsas_jade y la respuesta "Soy el asistente automático de
   Novedades Jade". Subirlo en Safari.
3. **Casilla** de uso permitido → **Guardar**.

⚠️ **Posible requisito: mensajes anulados ("unsend").** Un proveedor dice que en 2026 Meta pide, para
los permisos de mensajes de Instagram, un video que demuestre qué hace la app cuando el cliente
**anula el envío** de un mensaje **[Proveedor]** (Chatwoot / BotSailor, ver fuentes de 11.9). Instagram
avisa por el webhook con `message.is_deleted: true`. **Nuestro código no lo atiende** (grep sin
resultados en `FacebookWebhookController` ni en `botredes`): el bot no contesta ese evento (no trae
texto), pero el texto original **se queda guardado** en `mensaje_directo_social`. Si "View
requirements" lo pide, hay que programar: al llegar `is_deleted`, borrar o vaciar el mensaje con ese
`mid` en `mensaje_directo_social`. Pendiente de confirmar con lo que diga "View requirements".

### 2026-09-24 — `pages_read_engagement`

Llamadas de prueba en **Completado** (32 en la captura 16). El código no hace ningún `GET` a la página:
se usa como dependencia de contestar en contenido de la página, y el bot liga el `post_id` del
comentario con el producto que se publicó desde la app (`publicacion_social`, vía
`ProductoDePublicacionJpaAdapter`). En Messenger, el PSID del cliente solo sirve para contestarle.
**Video: el de comentarios de Facebook.**

Descripción:

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico. We use pages_read_engagement together with pages_manage_engagement and pages_read_user_content so our automated customer service assistant can work with the posts published by our own Facebook Page (NovedadesJade).

When a customer comments on one of our posts, the webhook tells us which of our Page's posts the comment belongs to. Our app matches that post with the product we published from our catalog, so the assistant can answer with that product's price and availability, and then replies in the same thread. When a customer writes to our Page on Messenger, we use the customer's page-scoped ID (PSID) only to reply to that conversation.

We only read content from our own Page, we do not collect followers' data for any other purpose, and we do not use it for advertising. The screen recording shows a customer commenting on one of our posts and the assistant replying with information about that product.
```

### 2026-09-24 — `instagram_basic`

Llamadas de prueba en **Completado**. Es la base de los otros permisos de Instagram: el código trabaja
con el **identificador de la cuenta de Instagram** (`igUserId`) de @novedades_bolsas_jade, ligada a la
página, y con el id de la publicación (`media.id`) que trae el webhook de comentarios para saber de qué
producto se habla. **Video: `videoRedesInstaComentariosv2`** (comentarios de Instagram).

Descripción:

```
Our app is used only by our own small business, Novedades Jade, a store in Mexico, and only with our own Instagram professional account (@novedades_bolsas_jade), which is linked to our Facebook Page. We use instagram_basic to read the basic profile metadata of that account, such as its Instagram user ID and username, and the IDs of its own posts.

Our automated customer service assistant needs this to work: the Instagram user ID identifies our account when we reply to comments and Direct Messages, and when a customer comments on one of our posts, the post ID tells us which product from our catalog the customer is asking about, so the assistant can answer with that product's price and availability.

We only read our own account's basic information and content. We do not read other people's profiles and we do not use this data for any other purpose. The screen recording shows a customer commenting on one of our Instagram posts and the assistant replying with information about that product.
```

### 2026-09-24 — Captura 44: la solicitud solo espera la Verificación

Pantalla "Solicitud de revisión de app" (Revisar → Revisión de la app → continuar). Barra de 5 pasos:
**Verificación** (gris, falta) · Configuración de la app ✅ · Uso permitido ✅ · Tratamiento de datos ✅
· Instrucciones para revisores ✅ ("Guardado automáticamente"). "Uso permitido" en verde = los 10
permisos quedaron llenos.

Verificación → "Conecta un portfolio comercial verificado": app **novedadesJade** conectada al
portafolio **Novedades Jade**, con estado 🟠 **"Se requiere más información"** y botones **Ver
detalles** / **Eliminar** (no usar Eliminar: desconecta el portafolio). Es la verificación del negocio
(3.2 y 📌 Pendientes 1–3): Constancia de Situación Fiscal + información del negocio que coincida.
Siguiente paso: **Ver detalles** para leer qué información pide Meta.

Antes de enviar: el texto de "Instrucciones para revisores" (`instructions-web-2`) todavía podría decir
que el bot de mensajes directos "does not reply and forwards the message by email"; corregirlo aunque
el paso salga en verde.

### 2026-09-24 — 📍 Estado al cierre del App Review (para retomar)

**Hecho:**
- Solicitud con los **10 permisos correctos** (lista final arriba). Quitados de la revisión:
  `pages_manage_posts` y `business_management` (se quedan con acceso estándar; publicar desde la app web
  sigue funcionando). Agregado: `instagram_manage_messages`.
- Los 10 formularios llenos: el paso **"Uso permitido" salió en verde** (captura 44). En
  `instagram_basic` las palomitas del formulario no se marcaban justo después de guardar; no importó,
  porque el paso general quedó en verde. Si pasa otra vez: revisar que se haya dado **Guardar**, esperar
  a que Meta termine de procesar el video y recargar.
- Configuración de la app, Tratamiento de datos e Instrucciones para revisores en verde.

**Videos por permiso (lo que se subió o se decidió):**

| Permiso | Video |
|---|---|
| `instagram_manage_comments`, `instagram_basic` | `videoRedesInstaComentariosv2` |
| `pages_read_user_content`, `pages_manage_engagement`, `pages_read_engagement` | Comentarios de Facebook (nombre del archivo sin anotar) |
| `pages_messaging`, `pages_manage_metadata` | Messenger de Facebook (nombre del archivo sin anotar) |
| `pages_show_list` | Explorador de la API Graph, `me/accounts?fields=id,name` |
| `instagram_manage_messages` | Mensajes directos de Instagram |
| `public_profile` | No se llenó por separado; el paso "Uso permitido" quedó en verde igual |

**Falta, en este orden:**
1. **Verificación del negocio** — portafolio "Novedades Jade" en 🟠 "Se requiere más información". Abrir
   **Ver detalles** y anotar qué pide. Es la Constancia de Situación Fiscal + información del negocio
   (📌 Pendientes 1–2).
2. **Instrucciones para revisores:** corregir la frase del punto de mensajes directos ("does not reply and
   forwards the message by email") antes de enviar.
3. **`pages_messaging`:** crear una cuenta real de Facebook, darle rol de **Evaluador** y poner sus datos en
   Instrucciones para revisores.
4. **`instagram_manage_messages`:** abrir **"View requirements"** (cuenta propia) y anotar qué pide. Si pide
   lo de mensajes anulados, programar el borrado con `is_deleted` (ver arriba).
5. Anotar los nombres de los archivos de los videos de Facebook y de Messenger.
6. Recién entonces: **Enviar para revisión**.

### 2026-09-24 — Captura 45: Centro de seguridad del portafolio

Business Suite → Configuración (portafolio Novedades Jade) → **Centro de seguridad**. Sección "Action
needed" con dos recomendaciones, cada una con botón **Realizar acción**:
1. *"1 usuario without passkey enabled"* → recomienda *"Remove users without passkeys from your
   business portfolio"*.
2. *"1 usuario con un dominio de correo electrónico público"* → recomienda *"Elimina de tu portfolio
   comercial a los usuarios con dominios de correo electrónico públicos"*.

"Acción completada": dominio de confianza agregado a la aprobación de pares, y aprobación de pares
activada en todas las cuentas publicitarias.

⚠️ **No usar "Realizar acción" ahí:** las dos proponen **sacar usuarios** del portafolio, y el único
usuario persona con correo público (`@hotmail.com`) es el propio dueño (captura 18/20: "Tortilleria la
Salidad (tú)", acceso total). Sacarlo lo deja fuera de su propio negocio. Son recomendaciones de
seguridad, **no** son el requisito de la verificación. Lo seguro, si se quiere quitar la alerta:
(1) crear una **llave de acceso (passkey)** en la cuenta de Facebook del dueño (Centro de cuentas →
Contraseña y seguridad → Llaves de acceso) **[Sin confirmar la ruta exacta]**; (2) cambiar el correo
del usuario a uno del dominio (`admin@novedades-jade.com.mx`, que ya existe). Ninguna de las dos
bloquea el App Review.

### 2026-09-24 — Captura 46: por qué no pasó la verificación del negocio

**Dónde está:** Business Suite → Configuración (portafolio Novedades Jade) → **Centro de seguridad** →
bajar hasta el cuadro **"Verificación del negocio"**. Caso de uso de verificación: *"La app requiere
acceso a los permisos en Meta for Developers"*. Dice: **"Verificación para Abel Tiburcio Felipe — No
pudimos verificar tu organización con la información proporcionada"** · ⚠️ "Se necesita más
información" · botón **Más información**. (El "Ver detalles" de la solicitud de revisión lleva aquí.)

**Motivo exacto (Más información → "Detalles y próximos pasos"):**
> No podemos verificar que el número de teléfono +52 722 *** **14 está asociado con el negocio Abel
> Tiburcio Felipe. Para solucionar esto, vuelve a la verificación del negocio y realiza una de las
> siguientes acciones: **Sube otro documento acreditativo** que muestre que el número de teléfono está
> asociado al negocio (certificados/estatutos de la sociedad; licencias/permisos comerciales; cartas,
> extractos o resúmenes bancarios; o facturas de agua, gas, electricidad y teléfono en la que figure el
> número), con **el nombre legal del negocio y el número de teléfono**. **O bien confirma tu correo
> electrónico de empleado del negocio.**

O sea: el nombre legal ya no es el problema; lo que falla es **ligar el teléfono** al nombre legal.

**Camino elegido: confirmar con el correo del dominio** (no hace falta ningún documento):
- El negocio ya tiene buzones `@novedades-jade.com.mx` en Hosting-Mexico (`admin@…` existe). Meta manda
  un código a ese correo y se captura.
- Requisito **[Proveedor]**: el sitio web capturado en la información del negocio tiene que ser del mismo
  dominio (`https://shop.novedades-jade.com.mx` → dominio `novedades-jade.com.mx`).
- Si no deja elegir correo: plan B = **recibo de teléfono** (Telcel/Telmex/…) a nombre de Abel Tiburcio
  Felipe donde salga ese número, o **estado de cuenta bancario** con nombre y teléfono. Si el recibo sale
  a nombre de otra persona, no sirve: cambiar el teléfono capturado por uno que sí salga a su nombre.

**Captura 47 — el asistente de verificación (se abre al reanudar):** ventana **"Verificar Abel Tiburcio
Felipe"**: *"Para la verificación, deberás demostrar que tu negocio es real y que tienes una conexión
legítima con él."* "Acciones que deberás realizar", en este orden:
1. **Verifica la información del negocio** — nombre, dirección, número de teléfono, correo electrónico
   y sitio web del negocio.
2. **Confirma tu conexión** — elegir el método de contacto para confirmar la conexión con el negocio.
   **Aquí se elige el correo `@novedades-jade.com.mx`.**
3. **Subir documentos** — solo si Meta no encuentra el negocio ("documentos aceptados").

Botón **Empezar**. En el paso 1 revisar antes de seguir: sitio web = `https://shop.novedades-jade.com.mx`
y correo = uno del dominio, porque el correo del paso 2 tiene que coincidir con el sitio. Si en el paso
1 se cambia el teléfono, tiene que ser uno que salga en un recibo a nombre de Abel Tiburcio Felipe.

**Captura 48 — primera pantalla después de "Empezar":** "Seleccionar un país — Indica la ubicación de
la organización que quieres verificar." Campo **País: México** (ya viene elegido) → **Siguiente**.

### 📌 PENDIENTES PARA EL FINAL (acordado 2026-09-24)

1. **Constancia de Situación Fiscal del negocio.** Sacar una nueva; si sigue en "Sueldos y Salarios",
   darse de alta en RESICO o Actividades Empresariales con domicilio fiscal en Luvianos, **con un contador**.
2. **Información del negocio en el portafolio** (Business Suite → Configuración → Información del
   negocio): nombre legal `Abel Tiburcio Felipe` (idéntico a la constancia), calle y número, colonia,
   ciudad Luvianos, Estado de México, CP 51440, RFC en "Identificación fiscal". Debe coincidir con la
   constancia nueva y con un recibo del mismo domicilio.
3. Recién con eso: **Verificación** → **Uso permitido** → **Enviar para revisión**.
4. ~~Decidir qué hacer con el webhook de Página que apunta a **Make.com**.~~ ✅ 2026-09-24: el dueño no usa Make. El webhook de Página ahora apunta a QA (`POST /{app}/subscriptions`, `object=page`, `fields=feed` → `{"success":true}`).

(Nota 2026-09-24: con el webhook de Página ya en QA, el primer comentario de Facebook llegó y el bot
lo procesó: `Comentario … escalado por correo al admin (bot no tenía el dato)`. El camino de
Facebook → bot funciona; ese comentario no se contestó en público porque el bot no tenía el precio y
lo mandó por correo, que es la regla.)
✅ 2026-09-24 21:53 (hora de México): comentario en Facebook **respondido por el bot**. Comentarios de
Facebook e Instagram funcionando en QA. Para grabar el video con el saludo de "asistente
automático" hay que borrar antes los registros de esa persona en `comentario_social`, porque el saludo
solo sale la primera vez.

5. **Cambiar `FACEBOOK_WEBHOOK_VERIFY_TOKEN` de QA**: hoy es el texto de ejemplo de la plantilla (se
   vio en el log de nginx al verificar el webhook de Página). Poner un valor aleatorio propio en el
   deployment de QA (`kubectl set env`, igual que el token de página) y repetir la suscripción de
   `object=page` y `object=instagram` con ese valor.
6. **Token de usuario expuesto (2026-09-24):** el token de usuario extendido se pegó en el chat al
   armar `me/accounts`. Caduca solo en 60 días, pero el token de página sale de él. La única forma de
   invalidarlo es **cambiar la contraseña de Facebook**, y eso invalida también el token de página
   del bot. Al final: cambiar contraseña → repetir "Token de página nuevo" → `kubectl set env`.
   **También quedó expuesto el token de página nuevo** (2026-09-24): una consulta a
   `/{page}/conversations` lo imprimió dentro de la URL `paging.next`. La misma rotación de arriba
   lo reemplaza. De aquí en adelante, toda consulta a la Graph API desde el VPS se filtra con
   `| sed 's/access_token=[^&"]*/access_token=XXX/g'`, y las que llevan `{}` en `fields` van con
   `curl -g` (si no, curl interpreta las llaves como un patrón y parte la URL en dos).

**Orden acordado:** primero los videos (comentarios ya funciona; mensajes directos falta el
enrutamiento de conversaciones), al final la constancia y la verificación.

---

## 9. Plan — bots de mensajes y comentarios en Facebook e Instagram (2026-09-24, SIN programar)

Pedido del dueño: que los avisos de la página de Facebook lleguen a nuestro bot y no a Make.com, y
que el bot conteste **mensajes y comentarios** en **Facebook e Instagram**. Primero el plan, después
el código.

### Estado actual

| | Instagram | Facebook |
|---|---|---|
| Comentarios | ✅ Funciona en QA | Código listo (`FacebookCommentBotService`), pero el webhook de Página va a **Make.com** |
| Mensajes directos | Código listo (`InstagramDirectMessageBotService`); Meta no entrega los mensajes: falta el **enrutamiento de conversaciones** | **No existe.** El webhook recibe `object=page` con `messaging` y lo ignora |

### Fase 0 — Configuración en Meta (sin código)

1. **Webhook de Página → QA** en lugar de Make.com, con `POST /{app}/subscriptions`, `object=page`,
   `fields=feed` y el mismo verify token de QA. Meta verifica con un `GET` al controlador.
2. **Quitar Make.com:** apagar o borrar el escenario en Make.com. Revisar Facebook → Configuración →
   Integraciones comerciales y Business Suite → Integraciones, y quitar "Make" si aparece. La página
   no tiene a Make instalado como app (`subscribed_apps` solo muestra novedadesJade); recibía por el
   webhook de nuestra app.
3. **Permiso `pages_messaging`:** agregar el caso de uso de Messenger a la app. Regenerar el token de
   página incluyendo `pages_messaging` y las mismas que tiene hoy, de larga duración
   (`FACEBOOK_SETUP.md` paso 5), y cargarlo en el secreto de QA.
4. Con el token nuevo: página suscrita a `feed,messages` (`/{page}/subscribed_apps`) y campo
   `messages` en el webhook de Página.
5. **Enrutamiento de conversaciones:** novedadesJade como app predeterminada para Instagram y para
   Messenger (Business Suite → Configuración → Integraciones → Enrutamiento de conversaciones).
6. Respuestas automáticas de Business Suite apagadas ✅ (ya hecho).

### Fase 1 — Código (cuando se aprueben las reglas)

**Reglas del dominio propuestas** (hay que acordarlas antes de programar):
1. Solo se contesta a **mensajes de texto entrantes de personas**. Nunca a ecos, a la propia
   cuenta, a reacciones ni a avisos de "visto".
2. **Idempotencia** por `mid`: si Meta reenvía el mismo evento, no se contesta dos veces.
3. **Primer mensaje por persona y por red:** saludo con aviso de "asistente automático".
4. **Pausa:** si una persona del negocio contesta a mano, el bot deja de contestarle a esa persona
   en esa red. ❓ ¿Para siempre (como hoy) o por un tiempo (24 h, 7 días)?
5. Si el bot no tiene el dato, **no contesta y avisa por correo** (como hoy).
6. **Límites:** 20 mensajes por hora por persona, cooldown y bloqueo (`ChatbotBlockService`, con la
   clave por red + persona).
7. **Adjuntos** (fotos, audios, stickers): ❓ ¿ignorarlos o contestar una vez "por ahora solo puedo
   leer texto"?
8. Contestar en menos de 30 s y solo dentro de las 24 h (política de Meta).
9. Los IDs de persona de Facebook (PSID) e Instagram (IGSID) son distintos. La pausa y la "primera
   vez" se llevan por **(red, persona)**.

**Arquitectura** (regla del proyecto: lo nuevo en hexagonal; lo viejo se migra cuando se toca):
- Dominio nuevo `hexagonal/mensajedirecto/`. Dominio: el mensaje entrante y las reglas 1–9.
  Puertos: enviar mensaje por canal, registro de mensajes, pausas, cerebro del chatbot, aviso por correo.
- Adaptadores: Instagram (`POST /{ig-user-id}/messages`, ya existe) y **Messenger nuevo**
  (`POST /{page-id}/messages` con `messaging_type=RESPONSE`), JPA sobre las tablas existentes, y
  `ChatbotInstagramService` / `ChatbotFacebookService` como cerebro.
- `InstagramDirectMessageBotService` **se migra** a este dominio (se toca de todos modos).
- Webhook: `object=page` + `messaging` → Messenger (hoy se ignora), con el manejo de eco igual que
  en Instagram.
- ~~**Migración de BD**~~: no hizo falta. Los IDs de persona de Facebook e Instagram no se cruzan y
  la pausa usa la columna `fecha` que ya existía.
- Pruebas unitarias del dominio y del adaptador de Messenger.

**Comentarios de Facebook:** no requieren código nuevo. Se prueban cuando el webhook de Página
apunte a QA (Fase 0.1).

### Fase 1b — Comentarios: cómo debería funcionar (pedido del dueño, 2026-09-24)

**Cómo funciona hoy [Código]:**
- El bot sabe de qué producto es una publicación **solo si se publicó desde el panel admin**
  (`publicacion_social` guarda el id del post y la variante). Si se subió directo desde la app de
  Facebook o Instagram, no sabe de qué producto es. **No lee el texto de la publicación.**
- **Saluda solo la primera vez** que una persona comenta (en cualquier publicación). Si escala una
  pregunta, no contesta nada en público.
- Contesta sobre **todo el catálogo**; al producto de la publicación solo le da prioridad.

**Cómo lo espera el dueño (reglas a confirmar antes de programar):**
1. **Identificar el producto por la publicación:** si no se publicó desde el panel, leer el texto de
   la publicación con la API (Facebook `GET /{post-id}?fields=message`, Instagram
   `GET /{media-id}?fields=caption`), sacar el **código de barras o número** y buscar el producto.
   ❓ ¿Con qué formato va el código en el texto? (pedir un ejemplo real)
2. **Saludar siempre** y después contestar si pregunta algo del producto. ❓ ¿En cada comentario o
   una vez por persona en cada publicación?
3. **Contestar solo sobre el producto de esa publicación.** ❓ Si preguntan por otra cosa: ¿invitar
   a escribir por mensaje o a la tienda, o contestar igual?
4. ❓ Si el bot no tiene el dato, ¿saludar en público ("¡Hola! En un momento te damos el precio") y
   además mandar el correo, en lugar de quedarse callado?

**✅ Reglas decididas por el dueño (2026-09-24):**
- **Siempre se contesta, y siempre con cortesía.** El bot nunca se queda callado ante un comentario.
- **Publicación subida desde el panel admin** (ligada a un producto en `publicacion_social`): saluda y
  contesta sobre **ese producto**.
- **Publicación subida directo en Facebook o Instagram** (sin producto ligado, que es el caso de hoy):
  el bot **no sabe de qué producto es**, así que **no contesta preguntas del producto**:
  - Si el comentario es un saludo, un halago o un aviso ("bonito", "ya te sigo", "ya compartí"):
    contesta un **saludo o agradecimiento cordial** y listo.
  - Si pregunta algo (precio, tallas, colores, disponibilidad, etc.): contesta **solo un saludo
    cordial** ("¡Hola! En un momento te compartimos la información 💖") y **escala por correo** al
    admin, que le contesta directamente.
  (Corregido 2026-09-24: antes decía "contesta normal con todo el catálogo"; el dueño lo aclaró.)
  ✅ **Confirmado por el dueño (2026-09-24): "así mero".** Aplica **igual en Instagram** que en Facebook (confirmado).
- Aplica igual en **Facebook y en Instagram**, cada una con su propio canal.
- **Leer el código del texto de la publicación: por ahora NO.**
- Consecuencias para el código:
  - Cuando escala (no tiene el dato), **además** del correo contesta en público algo cordial, tipo
    "¡Hola! En un momento te compartimos esa información 💖".
  - Cuando "no entiende" (`##FAREWELL##`), ya no se calla: contesta un saludo cordial.
  - Pendiente de confirmar: si el saludo va en **cada** respuesta o solo la primera vez por persona.
    Por lo que dijo el dueño ("siempre tiene que contestar cordialmente"), se propone: **cada**
    respuesta empieza cordial y el aviso de "asistente automático" va solo la primera vez.

**✅ Regla de pausa y escalado (decidida 2026-09-24, comentarios y mensajes, Facebook e Instagram):**
- **Cuando el bot escala** (manda el correo porque no tiene el dato): contesta **una sola vez** con un
  saludo cordial y **se pausa para esa persona** en esa conversación (en comentarios: esa persona en
  esa publicación; en mensajes directos: esa persona en esa red). Desde ahí **lo retoma el admin**.
- **Publicación ligada a un producto (subida desde la app):** el bot **sigue la conversación**
  contestando lo del producto (tallas, colores, precio…) mientras tenga el dato.
- ~~**Cuando el admin contesta a mano:** el bot deja de contestarle a esa persona. **Para siempre**.~~
  **Corregido por el dueño:** la pausa **no es para siempre**. Dura **30 minutos** (cambiado de 1 hora el 2026-09-24) desde la última vez
  que el admin contestó a mano (o desde que el bot escaló). Si la persona vuelve a escribir después de
  esos 30 minutos sin que el admin haya contestado, **el bot la retoma** con las mismas reglas: si la
  publicación es de la app, contesta lo que sepa del producto; si no, saluda y manda el correo.
  (Aceptado: los 30 minutos se reinician cada vez que el admin contesta; durante la pausa el bot no
  contesta ni manda correos; la duración queda configurable. Fotos, audios y stickers = igual que
  cuando no sabe algo.)
- **Mensajes directos (Instagram y Messenger):** el bot contesta con **todo el catálogo** (como hoy,
  puede buscar productos). Si no sabe algo, saluda, escala por correo y se pausa 30 minutos.
- **Comentarios:** solo sobre **esa publicación** (ver arriba). Confirmado 2026-09-24.
- Cambio de código nuevo: hoy el escalado **no** pausa (el bot volvería a contestar el siguiente
  mensaje). Hay que guardar la pausa al escalar.

### ✅ Fase 1 programada (2026-09-24, en `dev` sin commit)

- Dominio nuevo **`hexagonal/botredes/`** con las reglas R1–R7 en su `README.md`. Reemplaza a
  `FacebookCommentBotService`, `InstagramCommentBotService` e `InstagramDirectMessageBotService`
  (borrados).
- **Messenger:** el webhook ya no ignora `object=page` + `messaging`. Se contesta con
  `POST /{page-id}/messages` (`FacebookGraphClient.enviarMensajeDirecto`). Falta la parte de Meta:
  `pages_messaging` en el token y la página suscrita a `messages` (Fase 0, pasos 3 y 4).
- **Comentarios:** sin producto → agradece o saluda y escala; con producto → contesta solo sobre él,
  con precio (el de descuento si lo hay), talla, color y existencias.
- **Pausa de 30 min** (`redes.bot.pausa-minutos`) al escalar o cuando el admin contesta a mano; se
  reinicia con cada respuesta del admin. **No hizo falta migración:** la columna `fecha` de
  `comentario_pausa` y `mensaje_directo_pausa` ahora es "desde cuándo".
- Si el chatbot falla, se escala en vez de callar.
- Pruebas: `AtenderInteraccionServiceTest` (17), `PoliticaDeRespuestaTest` (4) y
  `ChatbotComentariosTest` (3). La suite completa pasa (326).

**Textos nuevos del log** (para `kubectl logs … | grep`):
- `COMENTARIO <id> de FACEBOOK respondido por el bot (primeraVez=…, escalado=…)`
- `MENSAJE_DIRECTO <id> de INSTAGRAM escalado al admin (…)`
- `Mensaje directo de INSTAGRAM recibido: mid=… de=… para=… eco=… texto=… adjunto=…`
- `… ignorado -- una persona está atendiendo esa conversación` (pausa vigente)
- `El admin contestó a mano …` (pausa creada o reiniciada)

Comando para verlo todo: `kubectl logs deployment/proyecto-key-deployment -n qa --since=10m | grep -E "COMENTARIO|MENSAJE_DIRECTO|Mensaje directo|admin contestó"`

### Fase 2 — Revisión de Meta

Agregar al envío `pages_messaging` y `pages_manage_engagement` (y `pages_read_user_content` si Meta
lo pide como dependencia). **Cada permiso lleva su propio video y su descripción.** Actualizar el
texto para los revisores.

### Fase 3 — Producción

Cuando Meta apruebe: credenciales de prod, webhook a prod, migraciones en `inventario_key`
(ver pendientes para el final).

---

## 10. Fuentes

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

---

## 11. Segunda investigación (2026-09-24) — lo que falta, con base en lo que ya avanzamos

Esta sección no repite lo de arriba: parte de lo que ya se configuró hoy (Messenger funcionando,
comentarios funcionando, Instagram DM sin llegar) y de revisar otra vez **todas** las capturas.

### 11.1 Lo que se ve en las capturas y hay que corregir

| Dónde | Qué se ve | Qué hacer |
|---|---|---|
| Business Suite → Información del negocio | Nombre legal "Tortilleria la Salida"; dirección "51440 / 51440 / Mexico, Mexico 51440" | Ya anotado: nombre y domicilio **idénticos a la constancia nueva**. Es la causa #1 de rechazo de verificación en 2026. |
| Business Suite → Personas | Tu usuario del portafolio se llama **"Tortilleria la Salidad (tú)"** | Cambiar tu nombre en el portafolio a tu nombre real (Detalles → editar). No es el nombre legal, pero el revisor lo ve. |
| Business Suite → Personas | **"La llave de acceso no está activada"** en los dos usuarios | Activar la **llave de acceso (passkey)**: Meta la exige a cuentas ligadas a portafolios; si la pide y no la creas, **bloquea la cuenta** hasta crearla. Celular → Configuración de la cuenta de Meta → Inicio de sesión y seguridad → Llave de acceso → Crear. |
| developers.facebook.com → Configuración → Básica (bloque de contacto) | Dirección incompleta: calle genérica, ciudad "mexico", estado "Mexico" | Poner **la misma dirección** que tendrá el portafolio y la constancia. Tres lugares distintos con tres direcciones distintas es motivo de rechazo. |
| developers → Roles de la app | **Jade Castañeda — Evaluador — Pendiente** | La invitación **no se ha aceptado**. Mientras siga pendiente, sus cuentas no cuentan como "con rol en la app" (ver 11.2). Aceptarla desde su Facebook: developers.facebook.com/requests o la notificación. |
| Selector de portafolio | 3 activos: NovedadesJade (+ Instagram), **Pagina2**, **"Novedades Jade"** (otra página) | Dos páginas casi con el mismo nombre confunden al revisor y ya confundieron el page id una vez. Si Pagina2 y la de "Novedades Jade" con espacio no se usan, **quitarlas del portafolio** (no hace falta borrarlas de Facebook). |
| Enrutamiento de conversaciones → Apps de socios | **Manychat** conectada a la página | Ya se le quitaron los permisos. Quitarla del todo: Facebook como la página → Configuración → Integraciones comerciales → Eliminar. En Manychat **no** dar "Connect" ni "Refresh Permissions". |
| Centro de cuentas (celular) | `novedades_bolsas_jade`, `trece0594` y `jade.castaneda.71868` están en **el mismo Centro de cuentas que el Facebook de Jade Castañeda** | Importante para 11.2: las pruebas de Instagram desde `trece0594` cuentan como de **Jade** (evaluador pendiente), no de Trece Trece. |
| Instagram → Mensajes y respuestas a historias | "Herramientas conectadas" **no** está en esa pantalla | Está dentro de **Solicitudes de mensajes** → "Herramientas conectadas" → **Permitir acceso a los mensajes**. En la captura está **activado** ✅. |
| Instagram → Solicitudes | Los mensajes de prueba cayeron en **Solicitudes** | La API **no devuelve** conversaciones de Solicitudes que lleven 30 días sin actividad; y todo el que no te sigue entra ahí. Para probar, que la cuenta de prueba **siga** a la tienda o aceptar la solicitud. |

### 11.2 Por qué no llegan los mensajes directos de Instagram (y cómo probarlo)

Ya descartado: webhook de la app con `messages` ✅, página suscrita con `messages` ✅, token con
`instagram_manage_messages` ✅, página ligada a `novedades_bolsas_jade` (`17841444237033427`) ✅,
cuenta dentro del portafolio ✅, "Permitir acceso a los mensajes" activado ✅, Manychat sin permisos ✅.
Aun así `GET /{page}/conversations?platform=instagram` devuelve `{"data":[]}` y no llega ningún webhook.

**Causa más probable — acceso Standard:** con acceso Standard, Meta solo entrega y deja leer mensajes
de **cuentas con rol en la app** (admin, desarrollador, evaluador); el resto necesita acceso avanzado
por App Review. La documentación también pide que el evaluador "tenga un rol en la cuenta profesional
de Instagram". Messenger funcionó porque lo mandó **Trece Trece**, que es admin. Las pruebas de
Instagram salieron de cuentas del Centro de cuentas de **Jade Castañeda**, cuyo rol sigue
**pendiente**. Por eso la lista de conversaciones sale vacía y no llega ningún webhook: para Meta,
nadie con rol ha escrito.

**Cómo probarlo, en este orden:**
1. Que **Jade acepte la invitación de evaluador** (developers.facebook.com/requests, con su Facebook).
2. **Aclarado el 2026-09-24:** la cuenta de Instagram de pruebas del dueño es **`trece0594`** ("trece1305"
   fue una confusión con los números) y vive en el Centro de cuentas de **Jade Castañeda**. Tiene que
   estar ligada a un Facebook con rol **aceptado**: o Jade acepta su invitación de evaluadora, o se
   mueve `trece0594` al Centro de cuentas de Trece Trece (admin).
   ✅ **Hecho el 2026-09-24:** se quitó `trece0594` del Centro de cuentas de Jade ("Administrar
   cuentas" → Quitar; en "Perfiles" no aparece esa opción) y se agregó el Facebook **Trece Trece**. Su
   Centro de cuentas quedó con solo `trece0594` + Trece Trece (5 páginas administradas).
   ✅ **Resultado:** el mensaje directo de `trece0594` **ya llega al bot**
   (`Mensaje directo de INSTAGRAM recibido ... para=17841444237033427`). Se confirma la causa:
   con acceso Standard, Meta solo entrega mensajes de cuentas ligadas a un Facebook con rol en la app.
   ❌ Pero el bot no pudo contestar: el token de página quedó inválido
   (`code 190, subcode 467: The session is invalid because the user logged out`). Al entrar con
   Trece Trece en el celular para ligar la cuenta se cerró la sesión de la que salió el token. Eso
   tumba **todo** el bot (comentarios y Messenger incluidos) hasta poner un token nuevo.
   **Solución de fondo:** token de un **usuario del sistema** del portafolio (no depende de la sesión
   ni de la contraseña personal; no caduca). Ver pasos en la conversación del 2026-09-24.
   ✅ Usuario del sistema **bot-novedades** creado (Admin), con la página NovedadesJade (acceso total,
   incluye la cuenta de Instagram) y la app novedadesJade (acceso total). Si la app no está asignada,
   "Generar token" dice "No hay permisos disponibles". Token de página nuevo cargado en QA; la
   prueba `me?fields=id,name` responde NovedadesJade.
   ❌ Siguiente prueba, dos errores de código (corregidos, falta subir):
   - Enviar el DM de Instagram por `POST /{ig-user-id}/messages` con token de página da
     `(#3) Application does not have the capability to make this API call`. Con Facebook Login, los
     DMs de Instagram van por la **página**: `POST /{page-id}/messages` (igual que Messenger).
     Corregido en `InstagramGraphClient.enviarMensajeDirecto`.
   - Los `mid` de Instagram miden ~180 caracteres y la columna era `VARCHAR(100)`:
     `Data too long for column 'mid'`. Entidad a 512 y migración
     `migration_mensaje_directo_mid.sql` (**correrla en QA antes de probar**).
   ✅ **2026-09-24 11:53 — Mensajes directos de Instagram funcionando en QA:** migración corrida en qa
   y prod, código subido a dev/qa, y el bot contestó el mensaje de `trece0594`. Con esto funcionan
   los **cuatro canales** en QA: comentarios de Facebook, comentarios de Instagram, Messenger y
   mensajes directos de Instagram. Lo que sigue: **grabar los videos de App Review**.
3. Que la cuenta de prueba **siga** a `novedades_bolsas_jade`, para que el mensaje no caiga en Solicitudes.
4. Mandar un mensaje **nuevo** y revisar:
   `GET /{page}/conversations?platform=instagram` (filtrando el token con `sed`) y el log
   `Mensaje directo de INSTAGRAM recibido`.

Si con eso llega, el bot ya funciona para Instagram en pruebas. Para **clientes reales** hace falta
el acceso avanzado de `instagram_manage_messages` (App Review + verificación del negocio).

### 11.3 ¿Por qué el bot le contestó un comentario a una clienta real si la app no está verificada?

Interpretación de la documentación (no hay una frase de Meta que lo diga así de directo): el acceso
Standard limita a **qué usuarios de la app** pueden conectar sus datos, no a quién comenta. El token
que usa el bot es de una página que administra **Trece Trece**, que tiene rol de admin; por eso la app
puede leer y contestar **cualquier** comentario de esa página. En los mensajes directos Meta sí
aplica el filtro a quien **escribe** (11.2); por eso Messenger solo contesta a cuentas con rol.

**Consecuencia importante:** el bot que contesta hoy los comentarios **es el de QA**, y lo hace con
clientes reales en la página real. Cualquier cambio que se suba a `qa` se ve de inmediato en tu
página. Probar primero en `dev` y subir a `qa` solo lo revisado.

### 11.4 Presentarse como asistente: comentarios vs mensajes directos

- La regla de Meta sobre avisar que es un bot está en la **política de Messenger/Instagram Messaging**,
  o sea, **mensajes directos**. Pide avisar **al inicio de la conversación**, **después de un silencio
  largo** y **cuando una persona le regresa la conversación al bot**. Es obligatorio donde lo exige la
  ley; Meta menciona **California y Alemania**, y en el resto lo recomienda como buena práctica.
- En **comentarios públicos** no hay esa regla. Por eso, desde el 2026-09-24 el bot **ya no se
  presenta en comentarios**, solo en el primer mensaje directo (`botredes/README.md`, R4).
- **Mejora pendiente (no programada):** cuando el bot retoma un mensaje directo después de la pausa de
  30 minutos (la persona lo atendió y se lo "regresó"), Meta recomienda volver a decir que es el
  asistente automático. Hoy no lo hace.

### 11.5 App Review: lo que Meta pide para los mensajes (nuevo)

- **Acceso avanzado = verificación del negocio + App Review.** Para contestar mensajes de clientes
  reales, `instagram_manage_messages` y `pages_messaging` necesitan acceso avanzado. Eso solo se da con
  App Review aprobado **sobre un portafolio verificado**. Por eso la constancia va primero.
- **Screencast por permiso:** debe verse el **inicio de sesión y el consentimiento** (la pantalla de
  Facebook que pide los permisos), un **intercambio real de mensajes** y el bot contestando. La
  justificación escrita tiene que decir lo mismo que el video.
- **El uso tiene que ser atención a clientes.** Meta rechaza `instagram_manage_messages` si el caso no
  es claramente "negocio contesta a su cliente". Justificación sugerida: "Tienda en línea que contesta
  dudas de sus clientes sobre productos, precios y pedidos; si el asistente no sabe, lo atiende una persona".
- **Webhook funcionando es obligatorio.** Sin URL de callback que responda, rechazan (ya lo tenemos).
- **Permiso `pages_messaging`:** tiene **0 llamadas**. Meta pide al menos una llamada real reciente
  antes de mandarlo a revisión. Ya se hizo al contestar Messenger hoy; revisar que el contador suba.
- **`human_agent` (7 días):** permite que **una persona** conteste hasta 7 días después del último
  mensaje del cliente, fuera de la ventana de 24 h. Tiene su propio App Review y **está prohibido
  usarlo para el bot**. Solo tiene sentido si el admin tarda más de 24 h en contestar. Se deja para después.
- **Respuesta privada a un comentario (idea futura):** Instagram deja mandarle **un** mensaje directo a
  quien comentó, hasta **7 días** después del comentario. Si esa persona contesta, se abre la ventana
  normal de 24 h. Sirve para "¿precio?" → "Te mandamos la info por mensaje". Requiere
  `instagram_manage_messages` avanzado.

### 11.6 Verificación del negocio — qué te falta y cómo resolverlo

| Falta | Cómo se resuelve |
|---|---|
| Constancia con actividad de negocio (hoy solo "Sueldos y Salarios", de 2022, domicilio del patrón) | 11.8: agregar la actividad (RESICO) y cambiar el domicilio fiscal, y sacar la constancia nueva. |
| Nombre legal del portafolio ≠ constancia | Poner el nombre **exacto** de la constancia, letra por letra (acentos y espacios incluidos). |
| Dirección incompleta en portafolio y en la app | La misma dirección completa de la constancia en los dos lugares. |
| Documento que pruebe domicilio **a nombre del negocio o tuyo** | Recibo de luz, teléfono o estado de cuenta **con tu nombre y esa dirección** en la misma hoja. Un recibo con dirección pero sin tu nombre **no sirve**. |
| Correo del negocio | Opcional, pero ayuda: un correo con el dominio `@novedades-jade.com.mx` en lugar de Gmail/Hotmail **acelera** la revisión, y la verificación por dominio es la más rápida. |
| Llave de acceso (passkey) sin activar | Activarla (11.1). |
| Tiempo | La revisión con documentos tarda de **3 a 7 días hábiles**, hasta 10. Si cambias nombre, dirección o dominio a medio trámite, la revisan con más lupa. **Corregir todo antes de enviar.** |

### 11.7 TikTok (lo nuevo)

- **Mensajes directos por API (Business Messaging API):** en **beta abierta en LATAM**, así que México
  entra. Solo para **cuentas Business registradas fuera** de EE. UU., la UE, Suiza y el Reino Unido.
- **Comentarios:** hay endpoints de "responder comentario" en la API for Business, pero una fuente
  dice que solo cubren **comentarios de anuncios**, no de videos orgánicos. La colección oficial de
  Postman sí tiene "Business comment list / reply" (Accounts API). **Hay que confirmarlo en el portal**
  antes de planear el bot de comentarios de TikTok. No se pudo abrir el portal desde aquí (bloqueado).
- **Orden sugerido:** TikTok después de que Meta quede aprobado. Mismo patrón de verificación de
  negocio: nombre y documentos que coincidan con la constancia.

### 11.8 SAT — seguir en tu trabajo y además tener el negocio

**Sí se puede, y es muy común:** en el **mismo RFC** tienes "Sueldos y Salarios" (régimen 605) y agregas
**RESICO** (régimen 626) para el negocio. Tu patrón te sigue reteniendo el ISR del sueldo como siempre;
el negocio lo declaras tú aparte. El tope de RESICO (**$3.5 millones al año**) cuenta **solo** lo del
negocio, no tu sueldo.

**Pasos, en orden:**
1. **Contraseña del SAT** (si no la tienes o no la recuerdas): se recupera en línea con la e.firma, o
   con cita en el SAT.
2. **e.firma** (muy recomendable, la vas a necesitar para el domicilio y otros trámites):
   - Cita en **citas.sat.gob.mx** → "e.firma personas físicas".
   - Llevar: **INE vigente**, **CURP**, **comprobante de domicilio** de no más de 3 meses, **correo**
     y una **USB**.
   - Te dan los archivos `.cer` y `.key` y un acuse. **Guarda la USB y la contraseña de la llave privada.**
3. **Cambio de domicilio fiscal** (hoy está el del patrón, en CDMX):
   - En línea con e.firma: "Realiza tu cambio de domicilio en el RFC", adjuntando el comprobante
     digitalizado. Si no, en cita.
   - Plazo legal: **10 días hábiles** después de cambiarte. La multa por no tenerlo al día va de
     **$2,080 a $6,660**.
4. **Aviso de actualización de actividades económicas y obligaciones** (persona física):
   - En línea con **RFC y contraseña** (no pide e.firma), o en "mi @spacio" del SAT.
   - Ahí agregas la actividad del negocio (comercio al por menor de ropa, bolsas y accesorios por
     internet) y eliges **RESICO**.
   - Descargas el **acuse de movimientos de actualización**.
5. **Constancia de Situación Fiscal nueva:**
   - Con **RFC y contraseña**, desde el portal del SAT o desde la app **SAT Móvil** → Documentos →
     Constancia.
   - Sin contraseña: app **SAT ID** (INE + video). Llega al correo en hasta 5 días hábiles.
   - Revisar que diga: tu nombre, el **domicilio nuevo**, la actividad del negocio y el régimen
     **RESICO**, además de Sueldos y Salarios.
6. **Con esa constancia:** corregir nombre legal y dirección del portafolio y de la app, y ahí sí
   iniciar la verificación en Meta.

**Lo que implica tener el negocio en RESICO** (confirmar todo con un contador):
- **ISR:** del **1% al 2.5%** sobre lo **cobrado** cada mes, sin IVA y sin deducciones. Se paga a más
  tardar el **día 17 del mes siguiente**.
- **IVA:** se declara aparte, normalmente también cada mes. RESICO solo simplifica el ISR.
- **Facturas:** si le vendes a una empresa (persona moral), te retiene el **1.25%** de ISR.
- **Declaración anual:** con sueldo **más** RESICO, en abril. Si tu sueldo pasa de $400,000 al año o
  tuviste más de un patrón, la anual es obligatoria de todos modos.
- Hay casos que **no pueden** estar en RESICO (por ejemplo, ser socio de ciertas empresas). Revisar
  con el contador que no te toque ninguno.

**Por qué con contador:** darte de alta en RESICO genera **declaraciones mensuales** aunque vendas poco.
Si no las presentas, llegan multas. Un contador también te dice la actividad exacta del catálogo del
SAT que te conviene.

### 11.9 Fuentes de esta sección

- [Meta — Webhooks de Instagram Messaging](https://developers.facebook.com/docs/messenger-platform/instagram/features/webhook/) · [Enviar mensajes (Instagram)](https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/messaging-api/) · [Conversations API](https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/conversations-api) · [Overview Instagram Platform](https://developers.facebook.com/docs/instagram-platform/overview/)
- [Meta — Niveles de acceso](https://developers.facebook.com/docs/graph-api/overview/access-levels/) · [Qué pide acceso avanzado](https://singhamandeep.com/what-is-meta-advanced-access/)
- [Meta — Política de Messenger e IG Messaging](https://developers.facebook.com/documentation/business-messaging/messenger-platform/policy) · [Aviso de bot (resumen)](https://docs.chatbotbuilder.ai/support/solutions/articles/150000172232-meta-guidelines-and-policy-on-the-use-of-automated-bots)
- [Meta — Respuestas privadas (Instagram)](https://developers.facebook.com/docs/instagram-platform/private-replies/) · [Respuestas privadas (Messenger Platform)](https://developers.facebook.com/docs/messenger-platform/instagram/features/private-replies/)
- [Human agent (Chatwoot)](https://www.chatwoot.com/hc/user-guide/articles/1745225158-what-is-human-agent-tag-in-instagram-messenger-channel) · [Ventana de 24 h](https://www.keyapi.ai/blog/instagram-messaging-api-policy/)
- [App Review de mensajes de Instagram 2026](https://singhamandeep.com/instagram-messaging-api-approval-getting-instagram_business_manage_messages-2026/) · [Chatwoot: Instagram App Review](https://developers.chatwoot.com/self-hosted/instagram-app-review) · [Por qué rechazan bots de Messenger](https://singhamandeep.com/facebook-messenger-bot-app-review-chatbot-saas/)
- [Rechazos de verificación 2026](https://chakrahq.com/article/meta-business-verification-rejected-reasons/) · [7 arreglos](https://anylinga.com/blog/en/meta-business-verification-rejected-7-fixes.html) · [Leadsales: persona física con CSF](https://leadsales.io/blog/verificar-negocio-meta-business-para-usar-api/)
- [Meta — Llave de acceso en portafolios](https://www.facebook.com/business/help/910360017835904) · [Acerca de las llaves de acceso](https://www.meta.com/help/meta-account/1991801474748071/)
- [TikTok — Business Messaging API](https://business-api.tiktok.com/portal/docs/business-messaging-api/v1.3) · [Infobip: TikTok Business Messaging](https://www.infobip.com/docs/tiktok) · [TikTok — Responder un comentario](https://business-api.tiktok.com/portal/docs/reply-to-a-comment/v1.3) · [Postman: Business comment reply](https://www.postman.com/tiktok/tiktok-api-for-business/request/2t0gmfy/business-comment-reply)
- [SAT — Aviso de actualización de actividades](https://www.sat.gob.mx/tramites/33758/presenta-el-aviso-de-actualizacion-de-actividades-economicas-y-obligaciones-fiscales-como-persona-fisica) · [SAT — Cambio de domicilio](https://wwwmat.sat.gob.mx/tramites/30357/realiza-tu-cambio-de-domicilio-en-el-rfc) · [SAT — Constancia](https://wwwmat.sat.gob.mx/aplicacion/53027/genera-tu-constancia-de-situacion-fiscal.)
- [Asalariado y RESICO (Factorum)](https://www.factorum.com.mx/post/puedo-estar-en-resico-si-tambi%C3%A9n-soy-asalariado) · [RESICO y sueldos compatibles](https://mex.tramitesnotariales.info/resico/resico-y-sueldos-salarios/) · [RESICO 2026 tasas y obligaciones (Alegra)](https://blog.alegra.com/mexico/resico-personas-fisicas/) · [Obligaciones RESICO 2026](https://resicocalc.com/blog/obligaciones-fiscales-resico-2026)
- [Constancia: 5 formas en 2026 (Alegra)](https://blog.alegra.com/mexico/constancia-de-situacion-fiscal/) · [Constancia con SAT ID](https://guiaconstanciafiscal.com/constancia-fiscal-sat-id-sin-efirma/) · [e.firma paso a paso 2026](https://serendipia.digital/tutoriales/tramitar-tu-e-firma-en-2026/) · [Cambio de domicilio 2026](https://serendipia.digital/tutoriales/cambiar-domicilio-fiscal/)
