# Dominio `escanercodigo` — escanear un código de barras con el celular y recibirlo en la PC

Pedido del dueño (2026-11-02): *"estoy en la PC dando de alta productos y necesito pasarle el código de barras a la pantalla; que
el celular lo escanee y llegue solo a la PC. Que sea en todo lo que tenga código de barras (buscador de la tienda, buscar
productos, alta…), de 1 en 1."*

## Cómo funciona
1. La PC pulsa **«📱»** junto al campo de código → `POST /v1/escaner/sesiones` → recibe un `id` aleatorio y muestra un QR con
   `https://<tienda>/escaner/<id>`.
2. El celular abre esa página (pública), abre la cámara (zxing, la misma librería que el 📷 de siempre), lee el código y lo manda:
   `POST /v1/escaner/sesiones/{id}/codigos`.
3. La PC pregunta cada ~1.5 s `GET /v1/escaner/sesiones/{id}/codigos?desde=N`; al llegar un código lo mete en el campo (o hace la
   búsqueda), cierra el cuadro y borra la conexión (`DELETE /v1/escaner/sesiones/{id}`).

**Por qué consultas repetidas y no WebSocket:** el `/ws` del chat es público y su difusión permite suscribirse con comodín a
`/topic/**`; un código de barras no es secreto, pero meter más canales ahí agranda esa superficie. Preguntar cada 1.5 s a un mapa en
memoria es barato, funciona detrás de cualquier proxy y no requiere tocar el canal del chat.

## Reglas (cada una con su prueba en `SesionEscanerTest` / `EscanerRemotoServiceTest`)
| # | Regla |
|---|---|
| E1 | La conexión dura 30 minutos; después ya no recibe ni entrega códigos |
| E2 | El código se limpia (espacios) y debe tener de 3 a 100 caracteres imprimibles (ASCII), sin saltos de línea |
| E3 | Se guardan los últimos 20 códigos; la PC pide «los mayores a N» (`secuencia`) |
| E4 | Máximo 30 códigos por minuto por conexión |
| E5 | Cerrada por la PC, ya no acepta códigos; cerrar una que no existe no es error |
| E6 | Máximo 2000 conexiones vigentes en total y 10 por IP de la PC (`abrir` es `synchronized`: contar y guardar sin que otra solicitud se cuele) |
| E7 | Si la PC no pregunta en 2 minutos (cerró o recargó la pestaña) la conexión se libera sola; así nadie llena el cupo con conexiones que nadie consulta |

## Seguridad
- Endpoints **públicos** (`permitAll` en `SecurityConfig`): el buscador de la tienda también los usa y el celular normalmente no tiene
  sesión. Protección: id de 128 bits (`SecureRandom`), que solo viaja en el QR; vigencia de 30 min; límites E4 y E6.
- El código es solo texto que la pantalla pone en un campo de búsqueda o de alta: no se ejecuta ni se guarda en el servidor más de 30 min.
- La IP del límite sale de `X-Forwarded-For`, la de más a la DERECHA (la que agrega el proxy de k8s), igual que `AuthController`. Aun si alguien lograra saturar el cupo, E7 lo libera en 2 minutos.
- La PC lee con un solo método atómico (`SesionEscaner.leer`): lista y número de secuencia bajo el mismo candado, para no saltarse un código si el celular escribe en medio.

## Límite conocido
Las conexiones viven en la memoria de la aplicación: funciona con UNA copia (igual que el chat en vivo). Con varias copias habría que
cambiar `SesionesEscanerEnMemoria` por un adaptador en Redis (el dominio no cambia: `SesionesEscanerPort`).

## Dónde está cada cosa
- `dominio/modelo/SesionEscaner` (las reglas), `dominio/puerto/{entrada,salida}`, `aplicacion/servicio/EscanerRemotoService`
  (Java puro), `infraestructura/salida/memoria/SesionesEscanerEnMemoria`, `infraestructura/entrada/rest/EscanerRemotoController`,
  `infraestructura/config/EscanerCodigoBeans`.

## Revisión independiente (2026-11-02)
Un revisor aparte encontró: límite por IP esquivable y cupo saturable (corregido: IP de la derecha + E7), lectura no atómica (corregido), `abrir` sin candado (corregido), y en el front: POST de abrir sin cancelar, consultas encimadas, cámara del celular que quedaba prendida y mayúsculas faltantes (todo corregido). «Escanear otro código» en el celular se quitó: la PC cierra la conexión al recibir el código (de 1 en 1), así que siempre terminaba en «La conexión terminó».
