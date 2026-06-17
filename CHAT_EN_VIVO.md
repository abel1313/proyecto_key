# Chat en Vivo — Plan Completo Back + Front

---

## ¿Cómo funciona la comunicación independiente por cliente?

**Sí, cada cliente es completamente independiente.**

Cada visitante recibe un `sesionId` único (UUID) al conectarse. Ese ID es la llave de toda su conversación:
- El cliente escucha solo su canal: `/topic/chat.usuario.{sesionId}`
- El admin escucha un canal general: `/topic/chat.admin` (le llegan mensajes de TODOS los clientes)
- Cuando el admin responde, especifica el `sesionId` → el mensaje llega solo a ese cliente

Pueden estar 10 personas chateando al mismo tiempo y cada una solo ve su conversación.

---

## ¿Cómo es el flujo para el admin?

**No es una pantalla nueva por cada cliente.** Es UN panel estilo "bandeja de soporte" (como WhatsApp Web o Intercom):

```
┌─────────────────────────────────────────────────────────────┐
│  Chat en Vivo — Panel Admin                                 │
├──────────────────┬──────────────────────────────────────────┤
│  Sesiones activas│  [María González]  ACTIVA                │
│  ─────────────── │  ──────────────────────────────────────  │
│  🟢 María  (2)   │  María:  Hola, quiero saber el precio   │
│  🟢 Juan   (1)   │  Admin:  Buenos días, con gusto         │
│  🔴 Pedro        │  María:  el de la bolsa negra grande    │
│                  │                                          │
│                  │  [ Escribir respuesta...    ] [Enviar]   │
└──────────────────┴──────────────────────────────────────────┘
```

- Panel izquierdo: lista de sesiones activas con badge de mensajes no leídos
- Panel derecho: conversación seleccionada
- El admin cambia de conversación haciendo clic en el panel izquierdo

---

## Flujo completo paso a paso

```
[1] Usuario entra al sitio
    → ve botón flotante de chat (abajo a la derecha)
    → hace clic → el widget se abre

[2] Se establece la conexión
    → Angular conecta WebSocket a /ws (SockJS + STOMP)
    → envía a /app/chat.conectar con { nombre: "anónimo" o nombre del cliente }
    → Back crea ChatSesion en BD → genera sesionId único (UUID)
    → Back responde en /topic/chat.inicio.{uuid-temporal} con { sesionId }
    → Angular guarda ese sesionId en memoria (no en localStorage para no persistir entre pestañas)
    → Angular se suscribe a /topic/chat.usuario.{sesionId} para recibir respuestas del admin
    → Si admin está conectado al panel → le llega notificación en /topic/chat.admin
    → Si admin NO está conectado → se envía email al admin

[3] Usuario escribe un mensaje
    → Angular envía a /app/chat.mensaje con { sesionId, contenido }
    → Back guarda ChatMensaje en BD (remitente = USUARIO)
    → Back publica en /topic/chat.admin el mensaje con { sesionId, contenido, timestamp, nombreUsuario }
    → Admin lo ve en tiempo real en su panel (aparece en la sesión correspondiente)
    → Si admin no está conectado → se manda email con el texto del mensaje

[4] Admin responde
    → Admin escribe en su panel y presiona Enviar
    → Angular del admin envía a /app/chat.admin.responder con { sesionId, contenido }
    → Back guarda ChatMensaje en BD (remitente = ADMIN)
    → Back publica en /topic/chat.usuario.{sesionId} con { contenido, timestamp }
    → Solo ese cliente recibe el mensaje en su widget

[5] Inactividad — cierre automático
    → Scheduler cada 5 minutos busca sesiones con ultimaActividad > 30 min
    → Las marca como CERRADA
    → Publica en /topic/chat.usuario.{sesionId} el evento { tipo: "SESION_CERRADA" }
    → El widget muestra "La sesión fue cerrada por inactividad"
    → Si el usuario vuelve a escribir → el back detecta sesión CERRADA → crea una nueva sesión
    → Todo vuelve al paso [2]

[6] Usuario cierra el widget / pestaña
    → El WebSocket se desconecta automáticamente
    → La sesión queda en BD como ACTIVA hasta que el scheduler la cierre
    → Si el usuario regresa antes de 30 min y tiene el sesionId → puede retomar
```

---

## Lo que ya tenemos ✓

| Componente | Estado | Archivo |
|---|---|---|
| WebSocket + STOMP + SockJS | ✓ Funcionando | `ConfigSocket.java` |
| `SimpMessagingTemplate` (servidor → cliente) | ✓ En uso | `GanadorRifaServiceImpl.java:127` |
| `@MessageMapping` (cliente → servidor) | ✓ En uso | `RifaControllerImpl.java:75` |
| Scheduler (`@EnableScheduling`) | ✓ Activo | `MisProductosApplication.java` |
| Redis | ✓ QA/Docker | Para caché de sesiones activas (opcional) |
| SecurityConfig | ✓ Existe | Solo agregar matchers para `/ws` y `/v1/chat/**` |

**Problema a resolver:** Hay dos configs WebSocket activas al mismo tiempo (`WebSocketConfig.java` y `ConfigSocket.java`). Hay que eliminar `WebSocketConfig.java` — es la que tiene CORS hardcodeado a `localhost:4200`.

---

## Lo que falta construir (backend)

### 1. Base de datos — 2 tablas

```sql
CREATE TABLE chat_sesion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sesion_id        VARCHAR(36) NOT NULL UNIQUE,  -- UUID
    identificador    VARCHAR(100) NOT NULL,          -- IP del visitante
    nombre_usuario   VARCHAR(100),                   -- opcional
    estado           VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',  -- ACTIVA | CERRADA
    fecha_inicio     DATETIME NOT NULL,
    ultima_actividad DATETIME NOT NULL
);

CREATE TABLE chat_mensaje (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sesion_id   VARCHAR(36) NOT NULL,
    remitente   VARCHAR(10) NOT NULL,  -- USUARIO | ADMIN
    contenido   TEXT NOT NULL,
    timestamp   DATETIME NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES chat_sesion(sesion_id)
);
```

### 2. Entidades JPA
- `ChatSesion.java`
- `ChatMensaje.java`

### 3. Repositorios
- `IChatSesionRepository`
  - `findBySesionIdAndEstado(String sesionId, String estado)`
  - `findByEstadoAndUltimaActividadBefore(String estado, LocalDateTime fecha)`
- `IChatMensajeRepository`
  - `findBySesionIdOrderByTimestampAsc(String sesionId)`

### 4. Servicios
- `ChatSesionService`
  - `conectar(String ip, String nombreUsuario)` → crea ChatSesion, devuelve sesionId
  - `cerrarSesion(String sesionId)`
  - `actualizarActividad(String sesionId)`
  - `cerrarSesionesInactivas()` → llamado por el scheduler
- `ChatMensajeService`
  - `guardar(String sesionId, String remitente, String contenido)` → devuelve ChatMensaje
  - `obtenerHistorial(String sesionId)` → lista de mensajes
- `ChatNotificacionService`
  - `notificarAdmin(ChatMensaje mensaje)` → email si admin no conectado

### 5. Controlador WebSocket — `ChatWebSocketController.java`

```java
@MessageMapping("/chat.conectar")
// Recibe: { nombreUsuario? }
// Crea sesión → publica en /topic/chat.admin evento NUEVA_SESION
// Responde en /topic/chat.inicio.{tempId} con { sesionId }

@MessageMapping("/chat.mensaje")
// Recibe: { sesionId, contenido }
// Guarda mensaje → publica en /topic/chat.admin
// Si admin offline → manda email

@MessageMapping("/chat.admin.responder")
// Recibe: { sesionId, contenido }  [solo ADMIN]
// Guarda mensaje → publica en /topic/chat.usuario.{sesionId}

@MessageMapping("/chat.admin.conectado")
// El admin notifica que está en el panel → se registra como conectado
// Para saber si mandar email o no
```

### 6. Endpoint REST (opcional pero útil)

```
GET  /v1/chat/admin/sesiones        → lista sesiones ACTIVAS con último mensaje
GET  /v1/chat/admin/historial/{sesionId} → historial completo de una sesión
POST /v1/chat/admin/cerrar/{sesionId}   → cerrar sesión manualmente
```

### 7. Scheduler

```java
@Scheduled(fixedDelay = 300000)  // cada 5 minutos
public void cerrarSesionesInactivas() {
    // busca sesiones ACTIVA con ultimaActividad < ahora - 30min
    // las cierra y publica SESION_CERRADA en /topic/chat.usuario.{sesionId}
}
```

### 8. SecurityConfig — matchers a agregar

```java
.requestMatchers("/ws/**").permitAll()
.requestMatchers("/v1/chat/admin/**").hasRole("ADMIN")
```

### 9. Email — configurar desde cero

Dependencia Maven:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

---

## Plan de implementación — orden

| Fase | Tarea | Entregable |
|---|---|---|
| 1 | Eliminar `WebSocketConfig.java` (duplicado) | Conflicto resuelto |
| 2 | Tablas SQL + entidades JPA + repositorios | BD lista |
| 3 | `ChatSesionService` + `ChatMensajeService` | Lógica de negocio |
| 4 | `ChatWebSocketController` + matchers en SecurityConfig | WebSocket del chat funcional |
| 5 | Scheduler de timeout | Cierre automático |
| 6 | Configuración email + `ChatNotificacionService` | Notificación al admin |
| 7 | Endpoints REST auxiliares para el panel del admin | Panel puede cargar historial |

---

## Preguntas pendientes

1. **¿El visitante es siempre anónimo** o puede ser un cliente registrado (con JWT)?
2. **¿30 minutos de inactividad** está bien para cerrar la sesión?
3. **¿Qué cuenta de correo** usa para recibir la notificación? (Gmail recomendado con contraseña de app)
4. **¿El panel admin** va en el Angular existente como una ruta nueva (`/admin/chat`) o es otra app?
