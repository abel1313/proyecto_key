# Chat en Vivo — Despliegue QA y Guía para Prod

Fecha: 2026-06-17

---

## Qué se hizo para que funcionara en QA

### Cambios en el código (backend)

| Archivo | Qué se cambió |
|---|---|
| `application-qa.yml` | Agregó config de mail (Gmail SMTP), `chat.admin-email`, timeouts SMTP |
| `application-docker.yml` | Agregó config de mail (Gmail SMTP), `chat.admin-email` (para prod) |
| `application-dev.yml` | Agregó RabbitMQ local, `chat.forzar-notificacion: true`, Gmail SMTP con credenciales hardcodeadas para dev |
| `ChatSesionServiceImpl.java` | Timeout de inactividad: 30 min → 5 min |
| `ChatNotificacionServiceImpl.java` | Lógica de notificación con `forzarNotificacion` y tracking de sesión admin |
| `ChatWebSocketController.java` | Handler `adminConectado` guarda wsSessionId |
| `ChatWebSocketEventListener.java` | Detecta desconexión del admin via `SessionDisconnectEvent` |
| `IChatNotificacionService.java` | Agregó `marcarAdminConectado`, `marcarAdminDesconectado`, `isAdminSession` |

### Variables de entorno que hay que agregar en K8s

```bash
# QA (namespace qa)
kubectl set env deployment/proyecto-key-deployment -n qa \
  MAIL_USERNAME="abel.tiburcio.130594@gmail.com" \
  MAIL_PASSWORD="tdwqergjhpijsfos" \
  CHAT_ADMIN_EMAIL="abel.tiburcio.130594@gmail.com" \
  SPRING_MAIL_HOST="smtp.gmail.com" \
  SPRING_MAIL_PORT="587"

# PROD (namespace default)
kubectl set env deployment/proyecto-key-deployment \
  MAIL_USERNAME="abel.tiburcio.130594@gmail.com" \
  MAIL_PASSWORD="tdwqergjhpijsfos" \
  CHAT_ADMIN_EMAIL="abel.tiburcio.130594@gmail.com" \
  SPRING_MAIL_HOST="smtp.gmail.com" \
  SPRING_MAIL_PORT="587"
```

> **SPRING_MAIL_HOST y SPRING_MAIL_PORT son necesarios** mientras el yml de la imagen vieja apunte a Hostinger.
> Una vez que se haga merge y se regenere la imagen, esas dos variables sobran (el yml ya tendrá smtp.gmail.com).

---

## Errores que ocurrieron y cómo se resolvieron

### Error 1 — RabbitMQ connection refused en dev
**Síntoma:** `Connection refused localhost:5672` al arrancar en dev  
**Causa:** El yml de dev no tenía config de RabbitMQ  
**Fix:** Agregar en `application-dev.yml`:
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```
Los errores de conexión son solo warnings, no bloquean el chat.

### Error 2 — APPLICATION FAILED TO START al excluir RabbitAutoConfiguration
**Síntoma:** App no arranca si se pone `spring.autoconfigure.exclude: RabbitAutoConfiguration`  
**Causa:** `RabbitMQConfig.java` requiere el bean `ConnectionFactory`  
**Fix:** No excluir — dejar que RabbitMQ falle en conexión silenciosamente con config localhost.

### Error 3 — "Email no configurado" en QA
**Síntoma:** Log decía `Email no configurado — notificación omitida`  
**Causa:** Las variables MAIL_USERNAME y MAIL_PASSWORD estaban en el K8s secret pero **no estaban inyectadas como env vars en el deployment**  
**Fix:** Correr `kubectl set env` para inyectarlas directamente en el deployment.

### Error 4 — Email no llega aunque variables están OK
**Síntoma:** No había log de email, sesión se creaba bien  
**Causa:** El admin panel estaba abierto → el flag `adminConectado=true` suprimía los emails  
**Fix:** Cerrar el panel admin antes de probar. El email solo se manda cuando el admin NO está conectado.

### Error 5 — Visitor reutilizaba sesión vieja, no se disparaba `conectar`
**Síntoma:** Solo aparecía SQL de `update chat_sesion` e `insert chat_mensaje`, sin `Nueva sesión de chat`  
**Causa:** El frontend guardaba el sesionId en localStorage y no llamaba `/chat.conectar` de nuevo  
**Fix:** Probar en incógnito o limpiar localStorage para forzar sesión nueva.

### Error 6 — `Couldn't connect to host, port: mail.novedades-jade.com.mx, 587; timeout -1`
**Síntoma:** Error al enviar email, conexión timeout  
**Causa:** El VPS de OVH bloquea puertos SMTP salientes (587 y 465). El servidor Hostinger no es accesible desde el VPS.  
**Fix:** Cambiar a Gmail SMTP (`smtp.gmail.com:587`) con App Password.  
**Pendiente:** Abrir ticket con OVH para desbloquear puertos si se quiere usar el correo de Hostinger.

### Error 7 — `kubectl set env --keys` no es un flag válido
**Síntoma:** Error al intentar inyectar variables desde un secret con `--keys`  
**Fix:** Pasar los valores directamente: `kubectl set env deployment/... KEY="value"`

---

## Lógica del email — cómo funciona

| Estado del admin | Qué pasa cuando llega visitante |
|---|---|
| Admin **conectado** al panel WebSocket | No se manda email (el admin ya lo ve en vivo) |
| Admin **desconectado** (cerró el panel) | Se manda email a `CHAT_ADMIN_EMAIL` |

El email se detecta como "admin conectado" cuando el frontend manda el mensaje `/chat.admin.conectado` por WebSocket.
El admin se marca como desconectado automáticamente cuando el WebSocket se cierra (`SessionDisconnectEvent`).

---

## Problema del historial de mensajes (pendiente en frontend)

El backend ya tiene el endpoint para cargar historial:

```
GET /v1/chat/admin/historial/{sesionId}
Authorization: Bearer <token>
```

**Respuesta:**
```json
[
  { "id": 1, "sesionId": "abc-123", "remitente": "USUARIO", "contenido": "Hola", "timestamp": "2026-06-17T10:00:00" },
  { "id": 2, "sesionId": "abc-123", "remitente": "ADMIN", "contenido": "En qué te ayudo?", "timestamp": "2026-06-17T10:00:05" }
]
```

**El frontend debe llamar este endpoint** cuando el admin selecciona una sesión del listado, para mostrar los mensajes anteriores. Sin esta llamada, el admin solo ve los mensajes nuevos que llegan en tiempo real.

---

## Checklist para despliegue en PROD

- [ ] Hacer merge dev → qa → main (con los cambios de yml y código)
- [ ] Regenerar imagen de prod (CI/CD o manual con Docker)
- [ ] Correr `kubectl set env` en prod con las variables de Gmail
- [ ] Verificar logs: `kubectl logs deployment/proyecto-key-deployment --tail=50`
- [ ] Probar: abrir chat como visitante sin panel admin abierto
- [ ] Confirmar que llega email a `CHAT_ADMIN_EMAIL`
- [ ] Guardar el App Password de Gmail en Bitwarden

---

## Timeout de sesiones

- **Inactividad hasta cierre:** 5 minutos (cambiado de 30 min)
- **Scheduler que revisa:** cada 5 minutos (`fixedDelay = 300_000`)
- Cuando cierra, el cliente recibe evento `SESION_CERRADA` por WebSocket

---

## Por qué se usa Gmail y no Hostinger

**Corregido 2026-08-15 — el diagnóstico de abajo (de cuando se escribió este doc, 2026-06-17)
estaba mal apuntado.** No es OVH quien bloquea. Ver la investigación completa en
"SMTP de Hostinger bloqueado — investigación 2026-08-15" más abajo.

~~El VPS de OVH bloquea los puertos SMTP salientes (587 y 465) para prevenir spam.~~
~~Verificado con `nc -zv mail.novedades-jade.com.mx 587` → timeout sin respuesta.~~
~~Para usar el correo de Hostinger hay que pedir a OVH que desbloqueen los puertos.~~

En realidad: el VPS **sí** puede mandar SMTP saliente sin problema (confirmado con Gmail,
puertos 465 y 587, IPv4 e IPv6). El bloqueo es específico hacia `mail.novedades-jade.com.mx` —
Hostinger está descartando las conexiones que vienen de la IP del VPS.

---

## SMTP de Hostinger bloqueado — investigación 2026-08-15

**Disparador:** `POST /v1/usuarios/{id}/solicitar-cambio-correo` fallaba con `"Authentication
failed"` (a veces) o simplemente no llegaba el correo (otras veces), de forma inconsistente.

### Causas encontradas, en orden (se fueron pelando una por una)

**1. `EmailService.enviarTicket()` no capturaba `MailException`.** Solo capturaba
`jakarta.mail.MessagingException`. `JavaMailSender.send()` lanza `MailException` (unchecked,
sin relación con `MessagingException`) cuando falla la conexión/autenticación SMTP real — se
escapaba sin capturar pese a que el contrato del método decía "no lanza excepción", rompía el
`@Transactional` de `UsuarioVerificacionService.solicitarCambioCorreo` y devolvía el error
crudo de SMTP al cliente. **Fix:** el catch ahora también atrapa `MailException`
(`EmailService.java`).

**2. `solicitarCambioCorreo` ignoraba si el correo realmente salió.** Guardaba el
`correoPendiente`/código y llamaba a `emailService.enviarCodigoVerificacion(...)` sin mirar el
`boolean` que devuelve — siempre respondía "código enviado" aunque el envío real hubiera
fallado. Peor: por 15 minutos, el siguiente intento veía "código vigente" y le decía al usuario
que revisara su bandeja, sin que jamás hubiera llegado nada. **Fix:** si el envío falla, se
lanza `RuntimeException` (hace rollback del guardado, el endpoint responde 400 con el motivo
real en vez de fingir éxito) — `UsuarioVerificacionService.java`.

**3. Intento de usar el correo propio del dominio (`mail.novedades-jade.com.mx`) en vez de
Gmail.** Se encontraron 2 cuentas ya creadas en cPanel para esto:
`boutique.bolsas@novedades-jade.com.mx` (prod) y `qa.boutique.bolsas@novedades-jade.com.mx`
(QA), ambas activas y sin restricciones. cPanel → "Connect Devices" confirma el protocolo
correcto: **puerto 465 con SSL/TLS implícito**, no 587 con STARTTLS (el yml original usaba
587+STARTTLS, mal). Se corrigió `application-qa.yml` a `host: mail.novedades-jade.com.mx,
port: 465, mail.smtp.ssl.enable: true`.

**4. Variables de entorno viejas pisando el yml.** `SPRING_MAIL_HOST=smtp.gmail.com` y
`SPRING_MAIL_PORT=587` seguían fijas en el deployment de QA desde junio (ver arriba, nunca se
quitaron pese a la nota de "sobran una vez que se regenere la imagen"). Las variables de
entorno de Spring Boot siempre ganan sobre el yml, así que aunque el archivo ya decía
Hostinger, seguía conectando a Gmail. **Fix:**
`kubectl set env deployment/proyecto-key-deployment -n qa SPRING_MAIL_HOST- SPRING_MAIL_PORT-`
(el `-` al final borra la variable).

**5. Con las 4 causas anteriores resueltas, la app sí intentaba conectar a
`mail.novedades-jade.com.mx:465` — y daba timeout de verdad.** Se aisló con `nc` corrido
**directo en la VPS** (no dentro de un pod, para descartar Kubernetes):
```bash
nc -zv -w5 -4 mail.novedades-jade.com.mx 465   # timeout
nc -zv -w5 -4 mail.novedades-jade.com.mx 587   # timeout
nc -zv -w5 -4 smtp.gmail.com 587               # succeeded
nc -zv -w5 -4 smtp.gmail.com 465               # succeeded
ping -4 -c3 8.8.8.8                            # funciona normal
```
Con esto se descarta: firewall del VPS (`ufw` inactivo, `iptables OUTPUT` en `ACCEPT`), IPv4
roto en general (Gmail conecta bien por IPv4), y el ticket viejo de OVH del 2026-06-18 queda
confirmado correcto (**OVH no bloquea salida**, nunca fue el problema).

**Conclusión: el bloqueo es del lado de Hostinger.** Su servidor de correo descarta
silenciosamente (timeout, no rechazo activo) las conexiones SMTP que vienen de la IP del VPS
(`51.178.29.99`, rango de OVH) — típico de firewalls tipo CSF/ConfigServer en hosting
compartido que bloquean por defecto rangos de IP de datacenter/VPS como medida anti-spam.

### Estado actual (2026-08-15)

- `application-qa.yml` **revertido a Gmail** (`smtp.gmail.com:587`, STARTTLS) — funciona,
  confirmado. QA sigue operando sin caída de correo.
- Los fixes de los puntos 1 y 2 (captura de `MailException`, no fingir éxito si falla el envío)
  **sí quedaron aplicados** — son mejoras válidas independientes del proveedor SMTP que se use.
- **Pendiente:** ticket a soporte de Hostinger pidiendo que desbloqueen la IP `51.178.29.99`
  para conexiones SMTP entrantes hacia `mail.novedades-jade.com.mx` (puertos 465/587). También
  se mandó un mensaje de seguimiento a OVH para descartar del todo un filtro de salida
  específico hacia esa IP destino (poco probable dado que Gmail sí conecta, pero se cubre por
  completitud).
- Cuando Hostinger confirme el desbloqueo: revertir `application-qa.yml` a
  `host: mail.novedades-jade.com.mx, port: 465, mail.smtp.ssl.enable: true` y volver a poner
  `MAIL_USERNAME=qa.boutique.bolsas@novedades-jade.com.mx` / `MAIL_PASSWORD=<la que se
  configuró en cPanel>` vía `kubectl set env`. Confirmar con el mismo `nc` antes de dar por
  cerrado.
- **Nota aparte:** en algún punto de las pruebas, un intento con `correoNuevo` mal formado
  (probablemente typo del usuario) devolvió `400 Validacion fallida: correoNuevo: El correo no
  tiene un formato valido` — comportamiento esperado, no es bug.
