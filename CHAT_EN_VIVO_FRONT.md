# Chat en Vivo — Especificación Frontend (Angular)

> Documento para el desarrollador frontend. Describe exactamente qué expone el backend, qué debe construir el front, y los payloads de cada mensaje.

---

## Arquitectura general

El chat usa **WebSocket con STOMP sobre SockJS**. No hay polling. La conexión es bidireccional en tiempo real.

Hay dos vistas en el front:
1. **Widget de chat** — para el visitante de la tienda (botón flotante)
2. **Panel de admin** — para el administrador (ruta protegida `/admin/chat`)

---

## Conexión WebSocket

```typescript
// Instalar: npm install @stomp/stompjs sockjs-client
// Tipos:    npm install --save-dev @types/sockjs-client

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  reconnectDelay: 5000,
  onConnect: () => {
    // aquí se hacen todas las suscripciones
  }
});

client.activate();
```

> En producción reemplazar `http://localhost:8080` por la URL del servidor.

---

## Parte 1 — Widget de Chat (visitante)

### Qué hace
- Botón flotante abajo a la derecha
- Al hacer clic: abre ventana de chat, conecta WebSocket, crea sesión
- Muestra la conversación en tiempo real
- Si la sesión se cierra por inactividad, muestra aviso

### Flujo de conexión inicial

```
1. Usuario hace clic en el widget
2. Conectar WebSocket
3. Generar un tempId local: uuid() (solo para escuchar la respuesta de conexión)
4. Suscribirse a /topic/chat.inicio.{tempId}
5. Enviar a /app/chat.conectar con payload:
   { tempId: "uuid-local", nombreUsuario: "Visitante" }
6. Back responde en /topic/chat.inicio.{tempId}:
   { sesionId: "uuid-del-back" }
7. Guardar sesionId en memoria (variable del componente, NO localStorage)
8. Suscribirse a /topic/chat.usuario.{sesionId}  ← aquí llegan las respuestas del admin
9. Cancelar suscripción a /topic/chat.inicio.{tempId}
```

### Enviar un mensaje

```typescript
client.publish({
  destination: '/app/chat.mensaje',
  body: JSON.stringify({
    sesionId: this.sesionId,
    contenido: 'Hola, ¿me pueden ayudar?'
  })
});
```

### Recibir respuesta del admin

```typescript
client.subscribe(`/topic/chat.usuario.${sesionId}`, (frame) => {
  const mensaje = JSON.parse(frame.body);
  // mensaje puede ser:
  // { tipo: 'MENSAJE', remitente: 'ADMIN', contenido: '...', timestamp: '...' }
  // { tipo: 'SESION_CERRADA' }

  if (mensaje.tipo === 'SESION_CERRADA') {
    mostrarAvisoSesionCerrada();
  } else {
    agregarMensajeAlChat(mensaje);
  }
});
```

### Modelo de datos (TypeScript)

```typescript
interface ChatMensaje {
  tipo: 'MENSAJE' | 'SESION_CERRADA';
  remitente: 'USUARIO' | 'ADMIN';
  contenido: string;
  timestamp: string; // ISO 8601
}

interface ConexionResponse {
  sesionId: string;
}
```

### Componente sugerido: `ChatWidgetComponent`

```
chat-widget/
  chat-widget.component.ts
  chat-widget.component.html   ← botón flotante + ventana
  chat-widget.component.scss
  chat.service.ts              ← lógica WebSocket
```

**Estado interno del componente:**
```typescript
sesionId: string | null = null;
mensajes: ChatMensaje[] = [];
conectado: boolean = false;
abierto: boolean = false;      // si el widget está expandido
escribiendo: string = '';       // texto del input
```

### UI del widget (estructura mínima)

```html
<!-- Botón flotante -->
<button class="chat-bubble" (click)="toggleChat()">💬</button>

<!-- Ventana de chat -->
<div class="chat-window" *ngIf="abierto">
  <div class="chat-header">
    <span>Chat con soporte</span>
    <button (click)="toggleChat()">✕</button>
  </div>
  <div class="chat-body">
    <div *ngFor="let m of mensajes" [class]="m.remitente === 'USUARIO' ? 'msg-usuario' : 'msg-admin'">
      {{ m.contenido }}
    </div>
  </div>
  <div class="chat-footer">
    <input [(ngModel)]="escribiendo" (keyup.enter)="enviar()" placeholder="Escribe un mensaje..." />
    <button (click)="enviar()">Enviar</button>
  </div>
</div>
```

---

## Parte 2 — Panel de Admin

### Qué hace
- Ruta protegida: `/admin/chat` (solo rol ADMIN)
- Panel izquierdo: lista de sesiones activas (se actualiza en tiempo real)
- Panel derecho: conversación seleccionada
- Al entrar: notifica al back que el admin está conectado (evita que se manden emails innecesarios)

### Flujo de conexión

```
1. Admin entra a /admin/chat
2. Conectar WebSocket
3. Suscribirse a /topic/chat.admin  ← llegan mensajes de TODOS los usuarios
4. Enviar a /app/chat.admin.conectado  ← avisarle al back que el admin está activo
5. Cargar sesiones activas: GET /v1/chat/admin/sesiones
```

### Recibir mensajes de usuarios

```typescript
client.subscribe('/topic/chat.admin', (frame) => {
  const evento = JSON.parse(frame.body);
  // evento puede ser:
  // { tipo: 'NUEVA_SESION', sesionId, nombreUsuario, timestamp }
  // { tipo: 'MENSAJE', sesionId, nombreUsuario, contenido, timestamp }

  if (evento.tipo === 'NUEVA_SESION') {
    agregarSesionAlPanel(evento);
    mostrarNotificacion('Nueva conversación de ' + evento.nombreUsuario);
  } else if (evento.tipo === 'MENSAJE') {
    agregarMensajeASesion(evento.sesionId, evento);
    incrementarBadge(evento.sesionId);
  }
});
```

### Cargar historial de una sesión al hacer clic

```typescript
// REST — no WebSocket
GET /v1/chat/admin/historial/{sesionId}

// Response:
[
  { remitente: 'USUARIO', contenido: '...', timestamp: '...' },
  { remitente: 'ADMIN',   contenido: '...', timestamp: '...' }
]
```

### Responder a un usuario

```typescript
client.publish({
  destination: '/app/chat.admin.responder',
  body: JSON.stringify({
    sesionId: this.sesionSeleccionada,
    contenido: 'Hola, con gusto le ayudo'
  }),
  headers: {
    Authorization: `Bearer ${token}`  // el admin sí está autenticado
  }
});
```

### Cerrar sesión manualmente

```typescript
// REST
POST /v1/chat/admin/cerrar/{sesionId}
// No body, responde 204
```

### Modelo de datos (TypeScript)

```typescript
interface SesionActiva {
  sesionId: string;
  nombreUsuario: string;
  fechaInicio: string;
  ultimaActividad: string;
  mensajesNoLeidos: number;
}

interface EventoAdmin {
  tipo: 'NUEVA_SESION' | 'MENSAJE';
  sesionId: string;
  nombreUsuario: string;
  contenido?: string;
  timestamp: string;
}

interface MensajeHistorial {
  remitente: 'USUARIO' | 'ADMIN';
  contenido: string;
  timestamp: string;
}
```

### Componentes sugeridos

```
chat-admin/
  chat-admin.component.ts       ← coordinador principal
  chat-admin.component.html     ← layout de dos paneles
  sesion-lista/
    sesion-lista.component.ts   ← panel izquierdo (lista de sesiones)
  sesion-detalle/
    sesion-detalle.component.ts ← panel derecho (conversación activa)
  chat-admin.service.ts         ← WebSocket + REST calls
```

### UI del panel (estructura mínima)

```html
<div class="chat-admin-layout">

  <!-- Panel izquierdo -->
  <div class="sesiones-panel">
    <h3>Conversaciones activas</h3>
    <div *ngFor="let s of sesiones"
         class="sesion-item"
         [class.activa]="s.sesionId === sesionSeleccionada"
         (click)="seleccionar(s.sesionId)">
      <span>{{ s.nombreUsuario }}</span>
      <span class="badge" *ngIf="s.mensajesNoLeidos > 0">{{ s.mensajesNoLeidos }}</span>
    </div>
  </div>

  <!-- Panel derecho -->
  <div class="conversacion-panel" *ngIf="sesionSeleccionada">
    <div class="mensajes">
      <div *ngFor="let m of mensajesActivos"
           [class]="m.remitente === 'ADMIN' ? 'msg-admin' : 'msg-usuario'">
        {{ m.contenido }}
      </div>
    </div>
    <div class="responder">
      <input [(ngModel)]="respuesta" (keyup.enter)="responder()" />
      <button (click)="responder()">Enviar</button>
      <button (click)="cerrarSesion()">Cerrar sesión</button>
    </div>
  </div>

</div>
```

---

## Resumen de canales WebSocket

| Canal | Dirección | Quién lo usa | Para qué |
|---|---|---|---|
| `/app/chat.conectar` | Cliente → Servidor | Widget del usuario | Crear sesión |
| `/topic/chat.inicio.{tempId}` | Servidor → Cliente | Widget del usuario | Recibir sesionId |
| `/app/chat.mensaje` | Cliente → Servidor | Widget del usuario | Enviar mensaje |
| `/topic/chat.usuario.{sesionId}` | Servidor → Cliente | Widget del usuario | Recibir respuesta del admin |
| `/app/chat.admin.conectado` | Cliente → Servidor | Panel del admin | Avisar que el admin está activo |
| `/topic/chat.admin` | Servidor → Cliente | Panel del admin | Recibir todos los mensajes y nuevas sesiones |
| `/app/chat.admin.responder` | Cliente → Servidor | Panel del admin | Responder a un usuario |

---

## Resumen de endpoints REST

| Método | URL | Auth | Para qué |
|---|---|---|---|
| GET | `/v1/chat/admin/sesiones` | ADMIN | Cargar lista inicial de sesiones activas |
| GET | `/v1/chat/admin/historial/{sesionId}` | ADMIN | Cargar historial al seleccionar una sesión |
| POST | `/v1/chat/admin/cerrar/{sesionId}` | ADMIN | Cerrar sesión manualmente |

---

## Notas importantes para el frontend

1. **El `sesionId` NO se guarda en `localStorage`** — vive solo mientras la pestaña esté abierta. Si el usuario cierra y reabre el navegador, se crea una nueva sesión.
2. **El widget del visitante NO requiere login**. La conexión WebSocket es pública.
3. **El panel del admin SÍ requiere JWT** en el header de los mensajes WebSocket que llegan a `/app/chat.admin.responder`.
4. **Si el WebSocket se desconecta**, la librería `@stomp/stompjs` reconecta automáticamente cada 5 segundos (configurado en `reconnectDelay`). Al reconectar, hay que volver a suscribirse a los canales.
5. **El badge de no leídos** se resetea a 0 cuando el admin hace clic en esa sesión.
6. **El evento `SESION_CERRADA`** que llega al widget del usuario viene del scheduler del back (inactividad de 30 min) o de un cierre manual del admin.
