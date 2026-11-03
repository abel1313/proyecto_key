# Dominio `escanercodigo` — escanear un código de barras con el celular y recibirlo en la PC

Pedido del dueño (2026-11-02): *"estoy en la PC dando de alta productos y necesito pasarle el código de barras a la pantalla; que
el celular lo escanee y llegue solo a la PC. Que sea en todo lo que tenga código de barras (buscador de la tienda, buscar
productos, alta…), de 1 en 1."*

## Canal de la CUENTA (2026-11-02): sin QR para quien tiene sesión
Pedido del dueño: *«en la PC estoy en Agregar; desde el celular entro normal como administrador, me voy al escáner, escaneo y se pasa a la pantalla que esté en la PC»*.
- La PC y el celular de la MISMA cuenta (los dos con sesión iniciada) se encuentran solos: `GET/POST /v1/escaner/canal/**` (exigen sesión) resuelven el usuario
  de la sesión y usan SU conexión (`idDelCanal`: la vigente de `usuario:<nombre>` o una nueva). No hay QR, id ni nada que recordar; si la conexión vence
  (5 min sin actividad, 12 h) la siguiente llamada abre otra y los dos lados vuelven a coincidir solos.
- Cada pantalla de la PC con un campo de código de barras se registra como receptora (`EscanerReceptorService`); mientras haya una, la PC deja una pregunta
  abierta en `/canal/codigos?esperar=true`. El celular abre `/escaner-celular` (cámara siempre lista) y manda cada código a `/canal/codigos`; llega a la
  pantalla que esté a la vista. Si la PC no tiene ninguna pantalla con código, `POST /canal/codigos` responde `false` y el celular avisa.
- E12: si la PC pide «después del N» y la conexión nueva de la cuenta va en un número menor, se pide desde el principio de la nueva (no se salta códigos).
- Las conexiones por QR (`/sesiones/**`, públicas) siguen para el visitante de la tienda sin sesión.

## Cómo funciona (versión rápida, 2026-11-02)
El dueño probó la primera versión: *«hace lo que pedí pero es muy lento: revisar el QR, ir a la página, que cargue, escanear»*. Ahora:
1. **La PRIMERA vez** la PC pulsa **«📱»** → `POST /v1/escaner/sesiones` → sale un QR a **`/escaner.html`** (página ligera, sin Angular) con el id de la
   conexión y la dirección del servidor en el `#`. La PC recuerda la conexión (`localStorage`).
2. El celular abre esa página: la cámara queda **lista y escaneando todo el tiempo** (detector nativo del celular si existe; si no, zxing, que se
   descarga solo en ese caso) y manda señal de vida cada 15 s (`POST .../latido`). Cada código leído se manda de inmediato (`POST .../codigos`).
3. **Después de la primera vez no hay QR**: al pulsar «📱» la PC pregunta `GET /sesiones/{id}` (¿sigue enlazado el celular?) y, si sí, solo dice
   «Escanea ahora». La PC deja una pregunta abierta (`GET .../codigos?desde=N&esperar=true`): el servidor la contesta **en cuanto llega el código**
   (milisegundos, sin consultar cada segundo) o a los 25 s con «nada» y la PC vuelve a preguntar.
4. Es de 1 en 1: al llegar un código el cuadro se cierra, el campo se llena y la conexión **sigue enlazada** para el siguiente. «Usar otro celular» la olvida.

Medido en pruebas de punta a punta (navegador, cámara de prueba con un EAN-13 real): QR en ~0.3 s; página del celular abierta → código en la PC en
~0.3 s; segunda vez sin QR: cuadro en ~0.15 s.

**Por qué una pregunta abierta y no WebSocket:** el `/ws` del chat es público y su difusión permite suscribirse con comodín a `/topic/**`; meter más
canales ahí agranda esa superficie. La pregunta abierta usa `DeferredResult` (libera el hilo del servidor mientras espera) y funciona detrás de
cualquier proxy.

## Reglas (cada una con su prueba en `SesionEscanerTest` / `EscanerRemotoServiceTest`)
| # | Regla |
|---|---|
| E1 | La conexión dura como máximo 12 horas |
| E2 | El código se limpia (espacios) y debe tener de 3 a 100 caracteres imprimibles (ASCII), sin saltos de línea |
| E3 | Se guardan los últimos 20 códigos; la PC pide «los mayores a N» (`secuencia`) |
| E4 | Máximo 30 códigos por minuto por conexión |
| E5 | Cerrada por la PC, ya no acepta códigos y avisa (410) a quien esté esperando; cerrar una que no existe no es error |
| E6 | Máximo 2000 conexiones ANÓNIMAS vigentes en total y 10 por IP (IPv6 agrupadas por /48) o por usuario si hay sesión; las del personal con sesión no cuentan en el cupo global, así nadie de fuera las deja sin servicio (`abrir` es `synchronized`) |
| E7 | Sin ninguna actividad en 5 minutos (ni la PC pregunta, ni el celular manda señal o códigos) se libera sola |
| E8 | El celular está «conectado» si mandó señal de vida en los últimos 45 segundos |
| E10 | Máximo 3 preguntas abiertas a la vez por conexión; con una más, la más vieja se contesta vacía (no se acumulan hilos ni preguntas huérfanas) |
| E12 | Si la PC pide «después del N» pero la conexión (nueva) va en un número menor, se normaliza a 0 |
| E11 | Al mandar un código se sabe si la PC lo estaba esperando (pregunta abierta, o una hace menos de 3 s). `POST .../codigos` responde `true`/`false`; con `false` el celular dice «la computadora no lo está pidiendo: pulsa 📱» en vez de «enviado» |
| E9 | La pregunta abierta se contesta al llegar un código, o cuando el celular se conecta por primera vez (para que la PC pase de «QR» a «escanea ahora») |

## Seguridad
- Endpoints **públicos** (`permitAll` en `SecurityConfig`): el buscador de la tienda también los usa y el celular normalmente no tiene
  sesión. Protección: id de 128 bits (`SecureRandom`), que solo viaja en el QR (en el `#`, no se manda a ningún servidor al abrir la página); vigencia E1/E7; límites E4 y E6. La página del celular solo acepta mandar a servidores de `*.novedades-jade.com.mx` (o `localhost` si ella misma está en localhost): un QR falso no puede apuntarla a otro lado.
- El código es solo texto que la pantalla pone en un campo de búsqueda o de alta: no se ejecuta ni se guarda en el servidor más de 30 min.
- La IP del límite sale de `X-Forwarded-For`, la de más a la DERECHA (la que agrega el proxy de k8s), igual que `AuthController`. Aun si alguien lograra saturar el cupo, E7 lo libera en 2 minutos.
- La PC lee con un solo método atómico (`SesionEscaner.leer`): lista y número de secuencia bajo el mismo candado, para no saltarse un código si el celular escribe en medio.

## Límite conocido
Las conexiones (y las preguntas abiertas) viven en la memoria de la aplicación: funciona con UNA copia (igual que el chat en vivo). Con varias copias habría que
cambiar `SesionesEscanerEnMemoria` por un adaptador en Redis (el dominio no cambia: `SesionesEscanerPort`).

## Dónde está cada cosa
- `dominio/modelo/SesionEscaner` (las reglas), `dominio/puerto/{entrada,salida}`, `aplicacion/servicio/EscanerRemotoService`
  (Java puro), `infraestructura/salida/memoria/SesionesEscanerEnMemoria`, `infraestructura/entrada/rest/EscanerRemotoController`,
  `infraestructura/config/EscanerCodigoBeans`.

## Revisión independiente (2026-11-02)
Un revisor aparte encontró: límite por IP esquivable y cupo saturable (corregido: IP de la derecha + E7), lectura no atómica (corregido), `abrir` sin candado (corregido), y en el front: POST de abrir sin cancelar, consultas encimadas, cámara del celular que quedaba prendida y mayúsculas faltantes (todo corregido). «Escanear otro código» en el celular se quitó: la PC cierra la conexión al recibir el código (de 1 en 1), así que siempre terminaba en «La conexión terminó».

## Segunda revisión independiente (2026-11-02)
Encontró y se corrigió: (1) los POST/DELETE públicos del escáner subían la versión del cache de «Mis pedidos» (`EscriturasWebInterceptor`): ahora
`/v1/escaner/` está excluido, con prueba; (2) el límite por IP se esquivaba con IPv6 (cada dirección contaba aparte): ahora se agrupan por /64, y quien
tiene sesión cuenta por usuario y no gasta el cupo de la IP de la tienda; (3) cancelar mientras se abría dejaba la conexión viva 2 min: ya no se
cancela la petición, la respuesta tardía cierra su conexión; (4) al salir de la pestaña se usa `sendBeacon` con `POST .../cerrar` (sin preflight);
(5) el aviso de «venció» ya no culpa solo a los 30 minutos. Pruebas nuevas: concurrencia real (50 hilos → exactamente 10 conexiones) y el almacén en memoria.
**Sin hacer (bajo riesgo, anotado):** el filtro JWT consulta la BD en cada pregunta de 1.5 s cuando el usuario tiene sesión (~0.7 consultas/s por cuadro
abierto); y el QR usa `window.location.origin`, así que en la app Capacitor o en `localhost` apuntaría a una dirección que el celular no abre.

## Tercera revisión independiente (2026-11-02, sobre la versión rápida)
Corregido: (1) el celular reenviaba cada 3 s el código que seguía enfrente y la PC lo tomaba como un escaneo nuevo (producto duplicado o equivocado):
ahora un código frente a la cámara es UN escaneo y se vuelve a mandar solo si salió del cuadro (≥1 s) y regresó; además el celular avisa si la PC no lo
estaba pidiendo (E11); (2) el cupo global ya no deja sin servicio al personal (E6) y las IPv6 se agrupan por /48; (3) máximo 3 preguntas abiertas
por conexión (E10); (4) la cámara del celular se reinicia al volver de segundo plano si quedó detenida; (5) la PC ya no puede quedar con dos cadenas de
preguntas ni emitir el código dos veces (reintento cancelable + comprobación del intento); (6) ráfaga de reintentos del celular ante 5xx; (7) se quitó
`qr_code`/`data_matrix` y se ignoran textos que sean direcciones (el celular leía el QR de enlace); (8) `detect()` ya no se encima; (9) la pregunta abierta de la PC
se corta sola a los 35 s si la conexión quedó colgada; (10) si la conexión «recién creada» da 410 no se reabre en bucle; (11) `zxing` sale en una ruta con
versión (`/escaner-lib/0.1.5/`) para que no se quede en caché un año al actualizarlo; pruebas del tiempo de espera de la pregunta abierta, E10 y E11.
**Sin hacer (anotado):** el filtro JWT consulta la BD en cada pregunta de 25 s cuando el usuario tiene sesión (carga mínima); cada despliegue del back borra las
conexiones (están en memoria), así que después de desplegar hay que volver a escanear el QR una vez.
