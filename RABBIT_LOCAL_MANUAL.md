# RabbitMQ — Manual de uso local

> Aplica mientras desarrollas en local con Docker Desktop.
> RabbitMQ no almacena las imágenes — solo transporta el mensaje de "guarda esta imagen".
> El archivo físico lo guarda micro_imagenes cuando consume el mensaje.

---

## 1. Levantar RabbitMQ

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

| Puerto | Para qué sirve |
|--------|---------------|
| 5672   | Conexión de las apps (mis-productos y micro_imagenes) |
| 15672  | Panel web para monitorear |

Panel web: `http://localhost:15672`
Usuario: `guest` / Contraseña: `guest`

Si ya lo tienes creado y está detenido:
```bash
docker start rabbitmq
```

---

## 2. Qué significa cada sección del panel

### Overview
La pantalla principal. Lo que te importa:

```
Connections  → cuántas apps están conectadas a Rabbit ahora mismo
Channels     → cuántos canales abiertos (normalmente 1 por conexión)
Queues       → cuántas colas existen
Messages     → Ready (esperando ser consumidos) / Unacked (en proceso) / Total
```

Cuando levantes **mis-productos** y **micro_imagenes** debes ver `Connections: 2`.

### Queues
Lista de todas las colas. La tuya es `queue.guardar.imagenes`.

Columnas importantes:
```
Ready    → mensajes esperando que alguien los consuma
Unacked  → mensajes que micro_imagenes tomó pero aún no confirmó
Total    → Ready + Unacked
```

Estado ideal después de que micro_imagenes procesa:
```
Ready: 0   Unacked: 0   Total: 0
```

---

## 3. Flujo completo cuando guardas una imagen

```
1. Tú llamas:  POST /productos/save  (con imágenes)
                        ↓
2. mis-productos ejecuta saveAll() y publica en Rabbit:
   Exchange: exchange.imagenes
   Routing Key: guardar.imagen
   Mensaje: [{ productoId: 5, imagenId: 12, ... }]
                        ↓
3. Rabbit pone el mensaje en: queue.guardar.imagenes
   (en este momento: Ready=1)
                        ↓
4. micro_imagenes consume el mensaje con @RabbitListener
   (en este momento: Unacked=1, Ready=0)
                        ↓
5. micro_imagenes guarda el archivo en disco y confirma (ack)
   (en este momento: Ready=0, Unacked=0)
                        ↓
6. La imagen ya existe físicamente en micro_imagenes
```

---

## 4. Cómo saber si la imagen ya se guardó

RabbitMQ solo transporta — no puedes consultar la imagen desde Rabbit.
Para confirmar que se guardó tienes tres formas:

### Opción A — Ver los logs de micro_imagenes
En la terminal donde corre micro_imagenes busca algo como:
```
Imagen recibida de Rabbit, productoId=5, guardando...
Imagen guardada correctamente: /ruta/archivo.jpg
```

### Opción B — Panel Rabbit: cola en 0
Si después de guardar el producto ves en Queues:
```
queue.guardar.imagenes   Ready: 0   Unacked: 0
```
El mensaje fue consumido y procesado.

### Opción C — Endpoint de diagnóstico (ya lo tienes)
```
GET /productos/admin/diagnostico-imagenes/{productoId}
```
Te dice si la imagen está en BD local y si el microservicio la tiene.
Este es el más confiable para confirmar el estado real.

---

## 5. Cómo ver el contenido de un mensaje en el panel

Si quieres ver exactamente qué datos tiene un mensaje en la cola
(útil cuando algo falla y el mensaje quedó atorado):

1. Panel → **Queues** → clic en `queue.guardar.imagenes`
2. Bajar hasta la sección **Get messages**
3. `Ack Mode`: **Nack message requeue true** (lo lee sin borrarlo)
4. `Count`: 1
5. Clic en **Get Message(s)**

Verás el JSON que mis-productos publicó:
```json
[
  {
    "productoId": 5,
    "imagenId": 12,
    "nombre": "bolsa-roja",
    "extension": "jpg"
  }
]
```

> IMPORTANTE: usa "Nack requeue true" para no perder el mensaje al leerlo desde el panel.

---

## 6. Qué hacer si un mensaje quedó atorado (Ready > 0 sin bajar)

Significa que micro_imagenes no está consumiendo. Causas comunes:

| Síntoma en el panel | Causa probable |
|---|---|
| Ready > 0 y no baja | micro_imagenes no está corriendo o no se conectó a Rabbit |
| Unacked > 0 por mucho tiempo | micro_imagenes tomó el mensaje pero lanzó excepción y no hizo ack |
| La cola no aparece | Ninguna app se conectó todavía (las colas se crean al conectarse) |

Para el caso de Unacked atascado, revisa los logs de micro_imagenes — habrá una excepción. Rabbit reintentará el mensaje automáticamente cuando micro_imagenes se reconecte o reinicie.

---

## 7. Comandos útiles Docker para el día a día

```bash
# Ver si rabbitmq está corriendo
docker ps | grep rabbitmq

# Detener
docker stop rabbitmq

# Volver a levantar (conserva las colas durable)
docker start rabbitmq

# Ver logs de rabbit (útil si no conecta)
docker logs rabbitmq

# Borrar todo y empezar limpio (pierde colas y mensajes)
docker rm -f rabbitmq
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

---

## 8. Resumen rápido de qué mirar en cada escenario

| Estoy haciendo | Dónde mirar |
|---|---|
| Guardé un producto con imágenes | Panel → Queues → Ready debe llegar a 0 |
| Quiero confirmar que la imagen existe | GET /productos/admin/diagnostico-imagenes/{id} |
| Algo falló y quiero ver el mensaje | Panel → Queues → Get messages (Nack requeue true) |
| No conecta ninguna app | Panel → Overview → Connections debe ser > 0 |
| micro_imagenes no consume | Logs de micro_imagenes + verificar que está corriendo |