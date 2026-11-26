# WhatsApp con la API de Meta — configuración y puesta en marcha

Reglas del dominio: `src/main/java/com/ventas/key/hexagonal/whatsapp/README.md`.
Plantillas de seguimiento: `WHATSAPP_PLANTILLAS.md`.

> **Aquí nunca van valores reales** (tokens, secretos, IDs de la cuenta, el número). El repositorio es
> público. Los valores viven solo en variables de entorno del servidor / secret de Kubernetes.

## 1. Cómo funciona

```
Cliente escribe por WhatsApp
  → Meta llama a  POST https://<api>/mis-productos/v1/whatsapp/webhook   (firmado)
  → se valida la firma, se responde 200 y se procesa aparte
  → se guarda el mensaje (una sola vez) y se abre la ventana de 24 h
  → ¿una persona atiende?  sí: solo se guarda y se marca   no: contesta el bot
  → se valida la ventana de 24 h y se envía con  POST /{phone-number-id}/messages
  → se guarda también la respuesta (enviada o no, con su motivo)
```

La pantalla del panel (**Sistema → WhatsApp**) usa `GET/POST /mis-productos/v1/whatsapp/...`.

## 2. Variables de entorno

| Variable | Obligatoria | Dónde se obtiene |
|---|---|---|
| `WHATSAPP_PHONE_NUMBER_ID` | sí | WhatsApp Manager → Números de teléfono → abrir el número → «Identificador de número de teléfono» |
| `WHATSAPP_CLOUD_TOKEN` | sí | Token **permanente** de un *usuario del sistema* (no el temporal de 24 h de «API Setup»). Business Settings → Usuarios → Usuarios del sistema → crear uno → asignarle la app y la cuenta de WhatsApp con control total → generar token con `whatsapp_business_messaging` y `whatsapp_business_management`. Meta lo muestra **una sola vez**: guardarlo al generarlo |
| `WHATSAPP_APP_SECRET` | sí* | developers.facebook.com → la app → Configuración → Básica → Clave secreta. *Si la app es la misma de Facebook/Instagram, basta con `FACEBOOK_APP_SECRET` (se usa de respaldo) |
| `WHATSAPP_VERIFY_TOKEN` | sí | Lo inventa el dueño: texto largo y difícil de adivinar. Tiene que ser **idéntico** aquí y en la configuración del webhook en Meta |
| `WHATSAPP_API_VERSION` | no (`v26.0`) | Versión de la Graph API. Meta cambia las versiones: confirmar la vigente en su documentación oficial (v26.0 se publicó el 2026-07-29) |
| `WHATSAPP_METODO_PAGO_ACTIVO` | no (`false`) | Ponerla en `true` **solo** cuando Meta ya tenga un método de pago válido y plantillas aprobadas (ver §5) |
| `WHATSAPP_BOT_PAUSA_MINUTOS` | no (`30`) | Cuánto se aparta el bot después de que una persona contesta |

Sin `WHATSAPP_APP_SECRET`/`FACEBOOK_APP_SECRET` el webhook rechaza todo. Sin token o número, enviar falla y
queda registrado en el historial (no truena nada más).

### En Kubernetes (regla de `CLAUDE.md`)

El deployment lee **cada variable por separado** (`secretKeyRef`): agregar la llave al secret no basta.
```bash
kubectl set env deployment/proyecto-key-deployment -n qa --from=secret/db-secret \
  --keys=WHATSAPP_PHONE_NUMBER_ID,WHATSAPP_CLOUD_TOKEN,WHATSAPP_APP_SECRET,WHATSAPP_VERIFY_TOKEN
kubectl rollout restart deployment/proyecto-key-deployment -n qa
```
Para verificar, **medir caracteres**, nunca `printenv | grep -c` (cuenta la variable aunque esté vacía):
```bash
kubectl exec -n qa deploy/proyecto-key-deployment -- sh -c 'echo ${#WHATSAPP_CLOUD_TOKEN}'
```

## 3. Orden para conectarlo con Meta (QA primero)

El número tiene **una sola** URL de webhook, así que se prueba primero en QA y prod se conecta después.

1. Correr `migration_whatsapp_cloud.sql` en `inventario_key_qa` (dos veces no cambia nada) y volver a entrar (las pantallas viajan en el JWT).
2. Cargar las variables de §2 y desplegar `qa`.
3. Comprobar que la URL pública responde: `GET https://<api-qa>/mis-productos/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=<el token>&hub.challenge=123` debe devolver `123`.
4. **Configurar el webhook en la app de Meta** (Webhooks → producto *WhatsApp Business Account*): la URL anterior y el verify token; suscribir el campo **`messages`**. La URL se verifica al guardarla, así que el paso 3 tiene que funcionar antes.
5. **Suscribir la app a la cuenta de WhatsApp Business** (sin esto Meta no manda nada aunque la URL esté bien).
6. La app debe estar en modo **Live** (publicada).
7. Prueba: escribir desde otro teléfono → debe aparecer en la pantalla y contestar el bot.

Estado al 2026-11-26 (revisado en Meta, solo lectura): la app está publicada y el número registrado; **faltaban**
la URL y el verify token del webhook, la suscripción de la app a la cuenta y el método de pago.

## 4. La ventana de 24 h (resumen)

- Con **menos de 24 h** desde el último mensaje del cliente: texto, imagen, enlace y botones libres.
- Con **24 h o más**: solo plantilla aprobada, y solo con método de pago. Meta responde **131047**
  («Re-engagement message») si se intenta un mensaje libre; se registra y no se reintenta (R6).
- Cada mensaje del cliente reabre la ventana, aunque sea a la hora 25.

## 5. Método de pago y precios

- **Agregarlo:** Meta Business → *Facturación* (Billing Hub) → la cuenta de WhatsApp Business → *Agregar método de pago*.
  Mientras falte, Meta acepta los mensajes que se cobran pero no los entrega. Después se pone `WHATSAPP_METODO_PAGO_ACTIVO=true`.
- **Precios (⚠️ no verificados):** desde este entorno no se pudo abrir la documentación de precios de Meta
  (`developers.facebook.com` está fuera de los dominios permitidos), así que **no hay montos en pesos en este documento**.
  Lo que sí indicó la búsqueda en la documentación oficial (hay que confirmarlo en la página de precios antes de decidir):
  - Meta cobra por **mensaje entregado**, según país y categoría (marketing, utilidad, autenticación).
  - Para México hay tarifa en **MXN** desde el 2026-01-01; las tarifas oficiales se publican en un CSV/PDF en
    «Pricing on the WhatsApp Business Platform».
  - **Cambios anunciados para el 2026-10-01:** subida de la tarifa de **marketing** en México, y que Meta empiece a
    cobrar los **mensajes de servicio** (las respuestas libres dentro de la ventana de 24 h, sin cobro desde nov-2024) y
    los de **utilidad** enviados dentro de la ventana. La búsqueda mencionó un cupo mensual gratis de mensajes de servicio
    por número, pero no el detalle. **Esto afecta a las respuestas del bot, no solo a las plantillas:** conviene
    revisar la página de precios y la factura del primer mes.
- Página oficial: <https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing>

## 6. Si algo no llega

| Síntoma | Revisar |
|---|---|
| Meta no deja guardar la URL del webhook | La URL debe ser HTTPS con certificado válido y el `GET` de verificación debe devolver el `challenge`; el verify token debe coincidir |
| No llega ningún mensaje | URL y `messages` suscrito · app suscrita a la cuenta · app en **Live** |
| Llegan pero 403 en el log | `WHATSAPP_APP_SECRET` (o `FACEBOOK_APP_SECRET`) mal o vacío |
| El bot no contesta | La conversación está *tomada* o en pausa (R10) · límite por hora o bloqueo (R11) · mirar el historial: ahí queda el estado y el motivo |
| `FALLIDO` con 131047 | Pasaron 24 h desde el último mensaje del cliente: hace falta plantilla |
| `FALLIDO` sin código | Token vencido o sin permisos, o número mal configurado: el motivo trae lo que dijo Meta |
