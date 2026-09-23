
# 🖥️ PROYECTO: BACKEND (proyecto_key)

**Repositorio:** https://github.com/abel1313/proyecto_key

Este es el **backend/API** del sistema. Expone endpoints REST que el frontend (`producto_venta_online`) consume.

## Endpoints principales por módulo

### Productos y Variantes
- `GET /v1/productos/obtenerProductos` - listado paginado de productos
- `GET /v1/productos/buscarNombreOrCodigoBarra` - búsqueda por nombre o código
- `POST /v1/productos/save` - crear producto
- `PUT /v1/productos/update` - actualizar producto
- `GET /v1/variantes/buscar` - búsqueda de variantes con imagen
- `GET /v1/variantes/porProducto/{id}` - variantes de un producto

### Clientes y Auth
- `POST /auth/login` - acceso con usuario/contraseña
- `POST /auth/refresh` - renovar token JWT
- `POST /auth/logout` - cerrar sesión
- `GET /v1/clientes/search` - búsqueda de clientes
- `POST /v1/clientes/save` - crear cliente

### Imágenes
- Se consume el microservicio `micro_imagenes` internamente
- `GET /v1/imagenes/file/{id}` - descargar imagen completa
- `GET /v1/imagenes/thumbnail/{id}` - miniatura de imagen

### Chat y Gestión
- `POST /chatbot/mensaje` - enviar mensaje al chatbot
- `GET /admin/configuracion-negocio` - config del negocio
- `POST /admin/gestion-roles` - permisos de roles

---

# Instrucciones de comportamiento

- No pidas confirmación antes de hacer cambios en el código
- No preguntes si puedes proceder con cambios en el código
- Ejecuta directamente y muestra el resultado
- Solo pregunta si hay ambigüedad real en el requerimiento
- **NO hacer git commit ni git push automáticamente** — hacer los cambios en los archivos localmente y esperar a que el usuario diga explícitamente "sube" o "haz commit" para ejecutar git commit y/o push.

## Flujo Git — cómo hacer los merges

### Orden de ramas
```
dev → qa → main/master
```
El código siempre sube de izquierda a derecha. Nunca al revés en el flujo normal.

### Flujo del día a día
```
1. Desarrollas en dev
2. Pruebas OK  →  merge dev → qa
3. QA aprueba  →  merge qa → main
```

### Comandos
```bash
# dev → qa
git checkout qa && git pull origin qa
git merge dev --no-ff -m "Merge dev → qa: descripción"
git push origin qa

# qa → main
git checkout main && git pull origin main
git merge qa --no-ff -m "Merge qa → main: descripción"
git push origin main
```

### Excepción — hotfix directo en main
Si se arregla algo en main que dev y qa necesitan:
```bash
# Bajar a dev
git checkout dev && git merge main --no-ff && git push origin dev

# Bajar a qa
git checkout qa && git merge main --no-ff && git push origin qa
```

### Regla importante
`main` no tiene RabbitMQ configurado — los YMLs de cada rama son independientes.
El merge solo mueve código Java, nunca sobreescribe los YMLs del ambiente destino.

### Feature que no va a llegar a main junto con el resto → rama propia (feature branch)

**Por qué existe esta regla:** el 2026-08-21 hubo que llevar `qa` a `main` con una excepción (todo
menos el módulo de redes sociales, bloqueado por credenciales de prod sin definir y App Review de
Meta sin aprobar). Como redes sociales se había desarrollado directo en `dev` mezclado commit a
commit con todo lo demás durante semanas, promoverlo a `main` significó revisar a mano archivo por
archivo qué sacar de un merge de 164 archivos — lento y con riesgo de error. Para que esto no se
repita:

**Regla:** si una feature tiene una razón conocida por la que NO va a poder subir a `main` junto
con el resto en el próximo ciclo (depende de credenciales que faltan, de una aprobación externa
pendiente, de una decisión de negocio sin cerrar, etc.), se desarrolla en su propia rama
(`feature/nombre-corto`, creada desde `dev`), **no directo en `dev`**. Solo se mergea esa rama a
`dev` (y de ahí sigue el flujo normal a `qa`/`main`) cuando ya se sabe que va a poder subir junto
con todo lo demás. Mientras siga bloqueada, se prueba en la rama propia (o se levanta un ambiente
aparte si hace falta probarla desplegada) sin contaminar `dev`/`qa`.

Si una feature bloqueada **ya** se mezcló en `dev`/`qa` antes de saber que iba a bloquear (como
pasó con redes sociales), no hay que deshacer el historial — simplemente el día que toque promover
a `main` se hace la exclusión a mano una vez (como el 2026-08-21) y, desde ahí en adelante, esa
feature específica se sigue tratando en su propia rama hasta que se resuelva.

**Consecuencia práctica para cambios chicos que SÍ van directo a `main`** (como agregar dos campos
a un endpoint que ya existe): si en ese momento `dev`/`qa` cargan por delante una feature bloqueada
que `main` no tiene (caso redes sociales hoy), promoverlos a `main` **no puede ser un
`git merge qa` normal** — eso traería de vuelta la feature bloqueada. Hay que promoverlos con
`git cherry-pick` de los commits puntuales del cambio chico directo a `main`, no con merge del
branch completo, hasta que la feature bloqueada se resuelva y vuelva a quedar todo parejo.

### Mapeo rama → base de datos

| Rama | Base de datos |
|---|---|
| `dev` | `inventario_key_qa` |
| `qa` | `inventario_key_qa` |
| `main` / `master` | `inventario_key` (sin sufijo) |

`dev` y `qa` apuntan a la misma BD (`inventario_key_qa`). `main` apunta a la BD de producción (`inventario_key`).

---

## Estrategia de versionado — URLs y Spring Boot

### Regla de versionado en `proyecto_key`

**PATRÓN A SEGUIR:** El versionado se hace mediante prefijos URL (`/v1/`, `/v2/`, etc.). El `/v1/` SIEMPRE va en el `@RequestMapping` a nivel de **clase**, nunca en decoradores de métodos.

#### Cómo crear un nuevo controller

```java
// ✅ CORRECTO
@RestController
@RequestMapping("/v1/mi-recurso")  // ← /v1/ aquí, a nivel de clase
public class MiRecursoController {
    
    @GetMapping("/buscar")  // ← sin /v1/, solo la ruta del método
    public ResponseEntity<?> buscar() { ... }
    
    @PostMapping("/save")
    public ResponseEntity<?> save() { ... }
}
// Resultado: GET /v1/mi-recurso/buscar, POST /v1/mi-recurso/save
```

```java
// ❌ INCORRECTO
@RestController
@RequestMapping("/mi-recurso")  // ← /v1/ falta aquí
public class MiRecursoController {
    
    @GetMapping("/v1/buscar")  // ← nunca aquí
    public ResponseEntity<?> buscar() { ... }
}
// Resultado: GET /mi-recurso/v1/buscar  (ruta confusa)
```

### ¿Por qué `/v1/` en la clase y no en el método?

- **Claridad:** la versión es responsabilidad del recurso completo, no de cada operación.
- **Consistencia:** todos los endpoints del recurso quedan bajo la misma versión.
- **Mantenibilidad:** si mañana sube a `/v2/`, cambias UN decorador (@RequestMapping), no todos los métodos.
- **Routing correcto:** Spring construye la URL concatenando: `/v1/mi-recurso` + `/buscar` = `/v1/mi-recurso/buscar`.

### Opciones de versionado en Spring Boot

Spring Boot no impone una estrategia única. Las más comunes son:

| Opción | Implementación | Ejemplo | Ventajas | Desventajas |
|---|---|---|---|---|
| **URL-based (la que usamos)** | `/v1/`, `/v2/` en @RequestMapping | GET `/v1/productos` | Obvio, fácil de versionar por recurso, cacheable | URLs largas, más rutas para mantener |
| **Header-based** | Accept header o custom header | `Accept: application/vnd.company.v1+json` | URLs limpias, clientes explícitos | Menos obvio, más complejo en cliente |
| **Query parameter** | ?version=1 en la URL | GET `/productos?version=1` | Opcional, flexible | Confuso si se mezcla con otros params |
| **Subdomain** | api.v1.dominio.com vs api.v2.dominio.com | Requiere DNS | URLs limpias por versión | Infraestructura DNS más compleja |

**Nuestra elección (URL-based)** es la más simple, más estándar en la industria y más fácil de probar en clientes (curl, Postman, navegador). Si en el futuro necesitas cambiar, la migración es sencilla (cambiar decoradores y documentación).

### Referencia de conversión

Cuando hayas heredado código con `/v1/` en métodos, el patrón de conversión es:

```java
// Antes (migración v1 completada)
@RequestMapping("/imagen")
public class ImageneController {
    @GetMapping("/v1/{id}")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) { ... }
}

// Después
@RequestMapping("/v1/imagenes")
public class ImageneController {
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) { ... }
}
```

Controllers ya corregidos (2026-09-17): `ImageneController`, `ImagenPresentacionController`, `VarianteController`.

---

## Deployment automático — CI/CD en GitHub Actions

### Workflows configurados

| Rama | Workflow | Deploy a | Docker tags |
|---|---|---|---|
| `main` | `producto-actions.yml` | **Producción** (default) | `latest`, `v<run_number>` |
| `qa` | `producto-actions-qa.yml` | **QA** | `qa` |

### Cómo funciona el deployment

1. **Push a rama → GitHub Actions se dispara automáticamente**
   - Push a `main` → dispara `producto-actions.yml`
   - Push a `qa` → dispara `producto-actions-qa.yml`

2. **Cada workflow:**
   - Checkout del código
   - Build de imagen Docker
   - Push a Docker Hub con etiquetas específicas
   - Deploy automático via SSH + kubectl:
     - Prod: `kubectl rollout restart deployment proyecto-key-deployment -n default`
     - QA: `kubectl rollout restart deployment proyecto-key-deployment -n qa`

3. **Resultado:** El container se reinicia con la imagen nueva en ~1-2 minutos después del push

### Nota para hotfixes

Cuando hagas un hotfix directo en `main` (ej. producción está rota):
- El push a `main` dispara `producto-actions.yml` automáticamente
- **No esperes a que bajemos a `dev`/`qa` después** — el deploy a prod es inmediato
- Luego el hotfix se baja a `qa` y `dev` con los merges normales (`main → qa → dev`)

---

## Regla — documentar migración de endpoints en CAMBIOS_FRONT.md

`CAMBIOS_FRONT.md` es la **única fuente de verdad** para endpoints y cambios de contrato de cara
al front. Otros archivos `.md` de endpoints en la raíz del proyecto (`API_CONTRATOS.md`,
`ENDPOINTS_FRONT.md`, `ENDPOINT_FRONT.md`, `cambios_y_endpoints.md`, `endpoints_palabras_clave.md`,
`ENDPOINTS_MIGRACION_V1.md`) están marcados como desactualizados — no se mantienen, no confiar en
ellos, y no crear documentos nuevos de este tipo: todo va en `CAMBIOS_FRONT.md`.

Cada vez que se migre un endpoint (se cree una versión v2), documentar en `CAMBIOS_FRONT.md`:
- **Request:** método HTTP + URL completa con contexto (`/mis-productos/...`) + params si aplica
- **Response:** solo los campos que el front necesita consumir; si el response es grande, recortar al mínimo útil (omitir campos internos, IDs de disco, rutas de servidor). Si es binario (bytes), indicar el Content-Type y que el body son bytes, no JSON.
- Indicar claramente qué cambia respecto a la versión anterior (diferencia clave)
- Si hay 204/404/500 posibles, documentarlos con una línea cada uno

### Checklist obligatorio antes de decir "ya quedó documentado"

No basta con documentar lo último que se dijo. Antes de dar por cerrada la documentación de una
sesión de cambios de backend:
1. Releer **toda** la conversación de la sesión (no solo el último mensaje) y listar mentalmente
   cada endpoint, campo, flujo o comportamiento que se haya mencionado como necesario para el front.
2. Verificar, uno por uno, que cada punto de esa lista tenga su párrafo correspondiente en
   `CAMBIOS_FRONT.md` — no asumir que "ya quedó" sin releer el archivo.
3. Si algo del código ya se comportaba de cierta forma antes de la sesión (no fue un cambio nuevo)
   pero el usuario preguntó por eso explícitamente, documentarlo igual — evita que se vuelva a
   preguntar después.
4. Si se corrige un bug de autorización/seguridad, documentar también el "antes" (qué fallaba) y
   el "después", no solo el endpoint nuevo — el front necesita saber qué comportamiento cambia.

### Regla — CAMBIOS_FRONT.md es espejo bidireccional con el repo del front (documentos_front_back_nodevedaades_jade)

`CAMBIOS_FRONT.md` vive en dos lugares y **debe quedar idéntico en ambos cuando se sincroniza**:
- Aquí: `D:\proyectos\proyecto_key_new\CAMBIOS_FRONT.md`
- Repo del front: `D:\proyectos\documentos_front_back_nodevedaades_jade\CAMBIOS_FRONT.md` (rama `main`, sin dev/qa)

**⚠️ NO tocar el repo del front (ningún archivo, incluido `CAMBIOS_FRONT.md`) sin que el usuario lo
pida explícitamente en ese momento.** No hay autorización permanente ni automática — ni para
sincronizar el doc, ni para pushear commits del front que estén sin subir, ni para ningún otro
`cp`/`git add`/`git commit`/`git push` ahí. Esto revierte una autorización anterior que sí era
automática; se dejó así a propósito después de dos incidentes donde tocar ese repo por cuenta
propia generó fricción (ver memoria `feedback_no_tocar_repo_front_sin_permiso`).

**Lo que sí se puede hacer sin pedir permiso:** leer el repo del front (`git log`, `git diff`,
`git show`, comparar contra la copia local) para informar al usuario si hay contenido nuevo o
diferencias — la restricción es sobre escribir/modificar/pushear, no sobre consultar.

**Cuando el usuario SÍ pida sincronizar**, seguir este procedimiento:
1. Revisar primero cuál de los dos tiene el cambio más reciente (`git fetch origin` +
   `git log -1` **comparando contra `origin/main`, no solo el HEAD local** — un commit puede
   existir localmente en el repo del front sin haberse pusheado nunca).
2. **Si el cambio se originó aquí:** copiar el archivo completo a la ruta del repo del front,
   sobrescribiendo el destino (no fusionar a mano), confirmar con `diff` que quedaron idénticos, y
   hacer commit + push a `main` de ese repo.
3. **Si el cambio se originó en el repo del front:** copiar ese archivo de vuelta a
   `proyecto_key_new/CAMBIOS_FRONT.md`, sobrescribiendo el de aquí.
4. El resto de ese repo (`CLAUDE.md`, `README.md`, otros archivos que ya existan ahí) no se toca —
   solo `CAMBIOS_FRONT.md`.
5. Si **ambos lados tienen cambios distintos y no triviales a la vez** (conflicto real, no solo
   uno más nuevo que el otro), no se sobrescribe nada solo: mostrar el diff al usuario y preguntar
   cuál gana antes de pisar contenido.

## JWT — Configuración y problema conocido resuelto

**Tiempos de expiración (JwtUtil.java — hardcodeados, no están en yml):**
- Access token: 15 minutos
- Refresh token: 7 días

**Bug resuelto (frontend):** Al expirar el access token, el interceptor del front hacía el refresh correctamente pero parseaba mal el response. El back devuelve `{ response: { accessToken: '...' } }` (ResponseGeneric) y el interceptor leía `response.accessToken` → guardaba `undefined` → el retry fallaba con "no se puede sacar el nombre del JWT". Fix: leer `response.response.accessToken`.

**Backend no requería cambios.** QA y Docker están correctos: env var `${TOKEN_JWT}` para el secret, `cookie.secure: true`, Redis y Rabbit configurados.

---

## Decisión — el chat en vivo se queda SIN imágenes (2026-09-17)

**Decidido a propósito: el chat en vivo (Chat directo) manda solo texto. No se implementan
adjuntos ni fotos.** Si en una sesión futura sale el tema, no hay que volver a investigarlo: la
decisión ya está tomada y esto es el contexto.

**Estado del código hoy** — no es que esté a medias, el canal para la foto no existe:
- `ChatMensaje.contenido` es una sola columna TEXT. No hay tipo de mensaje, ni adjunto, ni id de imagen.
- `ChatAdminResponderRequest` es `{ sesionId, contenido }`.
- El WebSocket (`/chat.admin.responder`) publica un String.

Por eso `ChatbotChatVivoService` sobreescribe `seccionMostrarProductos()` para que el bot **nunca**
ofrezca fotos: esa pantalla no las puede dibujar, y cuando el prompt las ofrecía el cliente decía
"sí" y se quedaba esperando una imagen que no llega.

**Lo que se hace en su lugar:** el admin pega el link del producto en el chat como texto, y el
cliente lo abre en la tienda. El bot ya dirige para allá ("lo puedes ver en la tienda en línea") o
ofrece pasar la conversación a una persona. Cero desarrollo y el cliente descarga desde la tienda,
que ya sirve miniaturas.

**Si algún día se retoma, tres cosas que ya se investigaron:**
1. **URL, nunca base64.** El micro de imágenes ya expone `v1/imagenes/file/{id}` y
   `v1/imagenes/thumbnail/{id}`. Incrustar base64 en el mensaje infla 33%, el navegador no lo
   cachea nunca y `chat_mensaje.contenido` se llena de blobs que se rebajan en cada carga del
   historial. Ya se limpió ese patrón en las rifas.
2. **Redimensionar es obligatorio, no opcional.** `max-file-size` está en 200MB
   (`application.yml`), así que nada impide mandarle al cliente una foto de 8 MB sin darse cuenta
   — y los datos móviles los paga él. Referencia: foto cruda 2–5 MB, redimensionada ~1000px
   100–300 KB, miniatura 20–50 KB.
3. **El limpiador nocturno las borraría.** `ReconciliacionImagenService.limpiarDiscoDia()` corre a
   las 4 AM y elimina todo archivo de `ruta_imagenes` que no esté registrado en `imagen`,
   `imagen_presentacion` o `logo`. Una tabla nueva de imágenes de chat hay que agregarla a esa
   lista o las fotos desaparecen cada noche (es el bug que ya pasó con las imágenes de
   presentación).

Alcance completo estimado: migración de `chat_mensaje`, endpoint de subida para el admin, campo
nuevo en el payload del WebSocket, redimensionado, alta en el limpiador nocturno, más la pantalla
del admin y la burbuja del cliente en el repo del front.

---

Micro servicio que permite compras de bolsas, pantalones faldas de mujer
1.- controlador AbstractController permite generar un CRUD generico
2.- AdminController permite eliminar la cache de redis
3.- AuthController 
    1.1 loginpermite acceder al sistema, incluye seguridad al intentar acceder varias veces y la contrasena incorrecta, genera el token y el refresh token ademas de
         utilizas las cokies para no alamacenar en el navegador y devuelve el token
    1.2 refresh permite validar el token y renovarlo
    1.3.- logout limpia el token y cierra la sesion

4.- controlador ChatbotController
    4.1- mensaje valida que la ip no este bloqueada, si esta bloqueada lo hace que espero unos minutos para volver a enviar mensaje, 
    el chat bot analiza loq ue poregunto y obtiene los productos de la base de datos para dar una respuesta en caso de que pregunto por algo de lo que vendemos
5.- controlador ClienteControllerImpl extiende AbstractController para obtener el CRUD y contiene mas endpoint para save que sobreewscribe al del abtract
    buscar cliente por id y puscar clientes paginados por nombre

6.- controlador ProductosControllerImpl maneja productos
    6.1 GET /productos/obtenerProductos - lista paginada de productos (publica)
    6.2 GET /productos/buscarNombreOrCodigoBarra - busqueda paginada por nombre o codigo de barras (publica)
    6.3 POST /productos/save y PUT /productos/update - guardan/actualizan producto; al enviar imagenes se guardan automaticamente en las variantes que ya tenga el producto
    6.4 GET /productos/findById/{id} - detalle del producto
    6.5 DELETE /productos/deleteBy/{id} - elimina producto con sus variantes e imagenes
    6.6 GET /productos/admin/diagnostico-imagenes/{productoId} - ADMIN: diagnostica por que no aparece la imagen de un producto en el listado
        Responde:
        - totalImagenesLocalDB: cuantas imagenes tiene el producto en la BD local (tabla producto_imagen_copy)
        - imagenesLocalDB: detalle de cada imagen (id, nombre, extension, rutaDisco)
        - imagenPresenteEnMicroservicio: si el microservicio externo devuelve imagen al hacer el listado
        - detalleExternoLista: "imagen presente con datos" / "null - el microservicio no devolvio respuesta" / "error: ..."
        Casos posibles:
          totalImagenesLocalDB=0 → nunca se guardo la imagen en BD
          totalImagenesLocalDB>0 y imagenPresenteEnMicroservicio=false → BD tiene el registro pero el microservicio no tiene el archivo
          totalImagenesLocalDB>0 y imagenPresenteEnMicroservicio=true → todo correcto, revisar cache

7.- controlador VarianteController maneja variantes de productos
    7.1 GET /variantes/buscar - busqueda paginada de variantes con imagen incluida (publica)
    7.2 GET /variantes/porProducto/{productoId} - variantes de un producto
    7.3 POST /variantes/guardarConImagenes - guarda variantes con sus imagenes
    7.4 POST /variantes/inicializarDesdeProducto - crea variantes en lote desde un producto con imagenes opcionales
    7.5 GET /variantes/imagenes/{varianteId} - imagenes de una variante especifica
    7.6 DELETE /variantes/{varianteId}/imagenes - elimina imagenes especificas de una variante
    7.7 GET /variantes/admin/diagnostico-imagenes/{varianteId} - ADMIN: diagnostica por que no aparece la imagen de una variante en el listado
        Responde:
        - totalImagenesLocalDB: cuantas imagenes tiene la variante en BD local (tabla variante_imagen)
        - imagenesLocalDB: detalle de cada imagen (id, nombre, extension, rutaDisco)
        - idsConDatosEnMicroservicio: IDs cuyo archivo existe en el microservicio de imagenes
        - idsSinDatosEnMicroservicio: IDs que estan en BD pero el microservicio no tiene el archivo
        - consistente: true si todos los IDs de BD tienen archivo en el microservicio
        Casos posibles:
          totalImagenesLocalDB=0 → nunca se guardo la imagen en BD
          idsSinDatosEnMicroservicio no vacio → BD tiene el registro pero el archivo se perdio en el microservicio
          consistente=true → todo correcto, revisar cache
---

## Reglas de comportamiento del catálogo y los buscadores

Descubierto en la validación de QA del 2026-09-17. Anotado para no volver a investigarlo.

### El nombre del archivo tiene que coincidir con los bytes al subir al micro

`micro_imagenes` valida cada subida con `ValidadorImagenSubida`: compara los **magic bytes** del
archivo contra la extensión del nombre y contra el Content-Type declarado. Si no coinciden,
responde **400** y no guarda nada.

El front recorta las fotos en un canvas, que **siempre saca JPEG**, pero conserva el nombre
original que eligió el usuario. Una foto `logo.png` llega con bytes JPEG y nombre `.png`, y el
micro la rechaza. Por eso todo lo que sube al micro pasa primero por
`Utils/NombreArchivoImagen.normalizar(nombre, bytes)`, que renombra según los bytes reales
(detección idéntica a la del micro). Son 3 puntos: `VarianteServiceImpl.subirImagenes()`,
`VarianteServiceImpl.subirImagenesMultipart()` y `ProductosServiceImpl.relacionProductoImagen()`.

Renombrar arregla las dos validaciones de un tiro, porque Spring deduce el Content-Type de la
parte a partir de la extensión del filename al escribir el multipart.

**Si se agrega otro punto que suba al micro, tiene que llamar a `normalizar()`.**

### Un 400 del micro tiene que llegar con su mensaje

`ImageneClienteDisco.save()` desempaqueta el `message` del `MensajeError` del micro y lo propaga
como `ExceptionErrorInesperado` (→ 400 con ese texto). Antes el admin veía solo
`"400 Bad Request from POST .../v1/imagenes"` y el motivo real se perdía.

### El catálogo público exige 4 condiciones — un producto sin imagen NO aparece

`IVarianteRepository.buscarVariantesPublicoFiltrado` filtra por:

```sql
WHERE v.stock > 0 AND p.habilitado = '1' AND v.habilitado = '1' AND p.esCatalogoInterno = false
  AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
```

Las 4 se cumplen o el modelo no se ve en la tienda. **No hay que darlo de alta dos veces:** si se
creó en el admin y no aparece en `tienda/buscar`, es que le falta stock, habilitado o imagen.
Un fallo al subir la imagen se manifiesta como "el producto no aparece en la tienda" y como
"aparece en el listado pero sin miniatura" (`producto_imagen_copy` nunca se escribe, así que el
listado no tiene `imagenId`).

### Eliminar = baja lógica, nunca DELETE de la fila

Hay **13 tablas** que apuntan a `variante` (`detalle_pedido`, `detalle_venta_variante`, `resena`,
`favorito`, `promocion_detalle`, `configurar_rifa_variante`, `ramo_armado`, `lugar_entrega`...).
Borrar la fila dejaría el historial de ventas y pedidos apuntando a algo que no existe.

Tanto `ProductosServiceImpl.deleteByIdProducto()` como
`VarianteServiceImpl.deleteByIdVariante()` dejan `habilitado = 0` y borran solo las imágenes.
El stock del producto padre no se toca. En la interfaz el texto dice **"dar de baja"**, no
"eliminar", y no promete que sea irreversible: se puede volver a habilitar, lo único que no
vuelve son las fotos.

**`AbstractController.delete` (`DELETE /<recurso>/delete`) es un stub vacío** —
`CrudAbstractServiceImpl.delete()` no hace nada y devuelve `null`. No cablear nada nuevo ahí;
cada recurso necesita su propio `deleteBy/{id}`. Ya existen para productos y variantes.

### Los buscadores de texto exigen 3 caracteres, y vacío recarga todo

Regla: **menos de 3 caracteres no sale al back** (con 1 o 2 el `LIKE '%x%'` barre casi todo el
catálogo y el resultado no le sirve a nadie). **Vacío SÍ dispara** y significa "quitar el filtro
y traer todo de nuevo". No aplica a buscadores por número (número de pedido), donde 1 dígito es
válido.

En el front la constante es `Constants.MIN_CARACTERES_BUSQUEDA`. Dos trampas que ya se pagaron:
- Un `filter(t => t.length >= 3)` antes del `debounceTime` **también descarta el vacío**, así que
  limpiar el input no recarga nada y queda en pantalla el resultado anterior. El caso vacío se
  atiende aparte, antes del subject.
- Si el guard se salta cuando hay filtros activos, el término corto se cuela por el parámetro del
  filtro. El término que viaja al back se calcula una sola vez y ya filtrado por el mínimo.

### `catchError` va DENTRO del `switchMap`, no en el `subscribe`

El back contesta **404/400 cuando una búsqueda no encuentra nada**. Si ese error llega al
`subscribe`, RxJS **termina la suscripción para siempre** y el buscador queda muerto hasta
recargar la pantalla — un handler `error:` en el `subscribe` apaga el spinner pero no la revive.
El `catchError` tiene que ir en el observable interno del `switchMap`.

Pasó en `tienda/venta` y estaba igual en `tienda/update`, Reportes y el autocomplete de palabras
clave. **Cualquier buscador nuevo con `switchMap` tiene que llevarlo.**

---

## Renombrado en curso: `variante` → `artículo` (2026-09-22)

**El nombre real del negocio es "artículo", no "variante".** `variante` fue un nombre técnico que se
filtró hasta la pantalla, y hoy el admin lee "variante" donde piensa "artículo". Se está renombrando,
pero **no de golpe**: un rename masivo de `variantes` toca 13 tablas con FK, todos los endpoints
`/v1/variantes/...` y medio front.

### Cómo se hace

**Oportunista, no como tarea aparte.** Cada vez que se toque un archivo por cualquier otra razón y
ahí aparezca `variante`, se aprovecha y se renombra **lo que sea seguro renombrar en ese archivo**.
No se abre un PR "de renombrado" ni se barre el repo entero.

### Qué SÍ se renombra

- Textos de pantalla en el front: labels, títulos, botones, mensajes de error, tooltips, breadcrumbs.
- Mensajes que el back manda al usuario (`ExceptionErrorInesperado`, mensajes de validación).
- Comentarios y nombres de variables locales cuando ya se está editando ese bloque.
- Documentación nueva.

### Qué NO se renombra todavía

- **Nombres de tablas y columnas** (`variantes`, `variante_imagen`, `producto_id`…). Eso necesita
  migración coordinada con los 13 FKs.
- **Rutas de endpoints** (`/v1/variantes/buscar`, `/v1/variantes/porProducto/{id}`). Cambiarlas
  rompe el front en producción; cuando toque, se hace como `/v2/articulos/...` con el `/v1/`
  conviviendo, siguiendo la estrategia de versionado de arriba.
- **Nombres de clases y DTOs** (`Variantes`, `VarianteDto`, `VarianteServiceImpl`) mientras las rutas
  sigan diciendo `variantes` — que el código y la URL se llamen distinto confunde más de lo que ayuda.
- **Campos de request/response** (`varianteId`, `variantes: []`). Son contrato con el front.

### Dónde se hace — solo `dev` y `qa` por ahora

El renombrado se acumula en `dev` y `qa`. **A `main` no sube renombrado suelto**: los hotfixes que
van directo a prod llevan únicamente el arreglo, sin aprovechar para renombrar de paso — si no, cada
hotfix arrastra ruido a producción y el cherry-pick se vuelve imposible de revisar.

Cuando el renombrado esté completo y probado en QA, se promueve a `main` como un cambio propio.

### Aplica igual en el front (`producto_venta_online`)

Misma regla y mismo alcance: textos de pantalla sí, rutas de Angular y nombres de interfaces que
espejean el contrato del back todavía no.

---

## Migraciones ya corridas — registro

Cuando se corra una migración a mano en un ambiente, anotarla aquí con la fecha, para no volver
a preguntarse si ya se ejecutó ni correrla dos veces por las dudas.

| Migración | dev / qa | prod | Fecha |
|---|---|---|---|
| `migration_submenu_ayuda_contextual.sql` | ⚠️ corrida, 0 filas (sin efecto) | ⚠️ corrida, 0 filas (sin efecto) | 2026-09-17 |
| `migration_submenu_ayuda_contextual_fix.sql` | ✅ corrida | ✅ corrida | 2026-09-17 |
| `migration_qr_destino.sql` | ✅ corrida | ✅ corrida | 2026-09-17 |
| `migration_accion_tienda_eliminar.sql` | ✅ corrida | ✅ corrida | 2026-09-22 |
| `migration_accion_pedido_cambiar_tipo.sql` | ✅ corrida | ✅ corrida | 2026-09-22 |
| `migration_accion_pedido_articulos.sql` | ✅ corrida | ✅ corrida | 2026-09-22 |
| `migration_accion_rifa_boletos_agrupados.sql` | ✅ corrida | ✅ corrida | 2026-09-22 |
| `backfill_variantes_carga_rapida.sql` | ✅ corrida | ✅ corrida | 2026-09-22 (hotfix) |
| `migration_accion_tienda_cambiar_precio.sql` | ✅ corrida | ✅ corrida | 2026-09-23 |
| `migration_grupo_pedido.sql` | ✅ corrida | ✅ corrida | 2026-09-23 |

**Las tres de 2026-09-22** dan de alta los permisos de los botones nuevos: los del detalle de
pedido (`cambiar-tipo`, y `agregar-articulo`/`cambiar-articulo`/`quitar-promocion`) y los de
boletos de rifa agrupados (`cargar-boletos-agrupado`/`agregar-participacion`/`quitar-participacion`).
Sin correrlas, esos endpoints responden **403 a todo el mundo, incluido el admin**. La lista completa con los comandos
y la consulta de verificación está al final de `PRUEBAS_QA_STOCK.md`, sección
**"Scripts que hay que ejecutar"**.

Recordar que después de correrlas hay que **volver a entrar**: los permisos viajan dentro del JWT y
un token viejo no trae la autoridad nueva.

`backfill_variantes_carga_rapida.sql` repara los artículos que la Carga rápida dejó vacíos antes
del hotfix del 2026-09-22 (ver CAMBIOS_FRONT.md). Copia del producto a la variante solo las columnas
que estén vacías — descripción, color, marca, contenido neto y categoría — así que no pisa nada que
se haya escrito a mano y correrla dos veces no hace daño. **No toca stock**, ni el del producto ni
el de la variante: el descuadre de inventario es otro problema y se decide producto por producto.
Correrla primero en `inventario_key_qa` (cubre dev y qa), validar con sus consultas de verificación,
y recién entonces en prod.

`migration_submenu_ayuda_contextual.sql` da de alta el permiso **Ayuda contextual**, el que
decide qué roles ven el icono "?" que explica cada pantalla del admin. Es idempotente (todos sus
INSERT llevan `NOT EXISTS`), así que volver a correrla no duplica nada.

**Confirmado el 2026-09-17:** se corrió en qa y en prod, y quedó sin efecto en ambas. Volver a correrla hoy tampoco cambia nada ni duplica: su `NOT EXISTS` sobre `ruta = 'ayuda-contextual'` ya encuentra la fila que insertó la versión `_fix`. Se deja anotada en vez de borrarla porque es el rastro de por qué el permiso no aparecía.

**⚠️ No surtió efecto — usar `migration_submenu_ayuda_contextual_fix.sql` en su lugar.** El
INSERT original colgaba de una fila ancla (`WHERE gr.ruta = 'gestion-menu/roles'`): como en esta
base no existe esa fila, el `INSERT ... SELECT` insertó **0 filas sin marcar error**, así que se
dio por corrida y el permiso nunca apareció en Gestión de roles. La versión `_fix` no depende de
ninguna fila ancla (inserta siempre una vez, y si no hay grupo "Sistema" la deja sin grupo) y
trae consultas de diagnóstico y de verificación comentadas al principio y al final.

**Lección para las próximas migraciones:** un `INSERT ... SELECT ... FROM tabla WHERE <ancla>`
falla en silencio si el ancla no existe. Cuando la fila a insertar sea obligatoria, usar
`FROM (SELECT 1) AS dummy` y dejar la condición solo en el `NOT EXISTS` de idempotencia, y
cerrar siempre con un `SELECT` de verificación que deba devolver al menos una fila.

Recordar el mapeo de bases: `dev` y `qa` apuntan ambas a `inventario_key_qa`, `main` a
`inventario_key`. Correrla en "qa" cubre dev y qa a la vez.

---

## Arquitectura — Hexagonal + Clean para todo lo nuevo (2026-09-18)

**Regla:** todo lo que arranque **de cero** (un dominio nuevo, un controller nuevo, un
modelo nuevo) se escribe con arquitectura hexagonal y clean architecture, en
`src/main/java/com/ventas/key/hexagonal/`.

**Lo viejo no se toca por tocar.** `mis/productos/{controller,service,entity,repository}`
se queda como está. Se migra una pieza **solo cuando haya que modificarla por otra razón**
— nunca un refactor masivo de golpe.

### Dónde está documentado
- `hexagonal/README.md` — la arquitectura, la regla de dependencia, el mapeo entre los
  conceptos de Hexagonal y los de Clean
- `hexagonal/_plantilla/` — el molde a copiar para cada dominio nuevo, con un README por
  carpeta que dice qué va, qué no va, y a qué corresponde en cada arquitectura

### Estructura: primero el dominio, después la capa
```
hexagonal/
├── _plantilla/          ← molde: cp -r _plantilla/ <dominio>/
├── imagen/
│   ├── dominio/ aplicacion/ infraestructura/
└── presentacion/
    ├── dominio/ aplicacion/ infraestructura/
```
Cada dominio queda autocontenido: si mañana se muda a otro micro, se mueve la carpeta
entera.

### La regla de dependencia
```
infraestructura ──▶ aplicacion ──▶ dominio        (nunca al revés)
```
`dominio/` no importa Spring, ni JPA, ni Jackson. Si un archivo de `dominio/` tiene un
`import org.springframework.*` o `jakarta.persistence.*`, está mal ubicado.

### Cómo leer las marcas en los README
Cada carpeta dice a qué corresponde en ambas arquitecturas:
```
[Hexagonal: Driven Port]   [Clean: Interface Adapter]
```
Sirve para no perderse: son dos vocabularios para casi lo mismo. Hexagonal aporta
*puerto/adaptador*; Clean aporta *capas y regla de dependencia*.

---

## Regla — un método, una responsabilidad

Un método que escribe en disco **escribe en disco**. No guarda en BD, no publica a Rabbit,
no invalida caché. El que orquesta es otro, y llama a los tres.

Lo mismo para los puertos: un puerto llamado "cliente disco" no puede tener la mitad de
sus métodos consultando la base de datos.

**Por qué existe esta regla — dos casos reales de este proyecto:**

1. `ProductosServiceImpl.mappImagenes()` escribía la imagen en disco **y** después otro
   método la subía al micro. Cada imagen quedaba guardada **dos veces**
   (ver `DUPLICADO_IMAGENES_AGREGAR_MODELO.md`, corregido 2026-09-18).
2. `ClienteDiscoPort` en `micro_imagenes` se llama "cliente disco" pero
   `resolverArchivo()`, `readAll()`, `verificarExistentes()` e `isHuerfana()` consultan la
   BD, y `eliminarRelacionesDuplicadasVariante()` es SQL puro que ni toca disco. Resultado:
   no se puede reusar la escritura en disco sin arrastrar la base detrás.

---

## Regla — antes de crear un dominio: primero las reglas, después el código

No se modela hasta tener las reglas del dominio escritas y acordadas.

El pedido nunca llega completo. Llega como *"quiero vender carros, que el producto sea un
carro y que corra"* — y faltan las reglas que nadie dijo pero el dominio necesita: ¿un
carro apagado puede correr?, ¿se puede encender uno ya encendido?, ¿se puede apagar en
movimiento?

**Parte del trabajo es proponer esas reglas que no se dijeron**, no solo implementar lo
literal. El checklist completo (identidad, estados y transiciones, invariantes,
validaciones, efectos de borde, permisos) está en `hexagonal/_plantilla/README.md`.

Orden de construcción, siempre de adentro hacia afuera:
```
modelo → excepciones → puerto salida → puerto entrada → servicio → adaptadores → controller → DTOs
```
Arrancar por el controller hace que el diseño gire alrededor del JSON en vez del negocio.

---

## Regla — toda tabla nueva se anota aquí

Cada vez que se cree una tabla, se agrega a la lista de abajo **en el mismo cambio** que
la crea. Sin eso, la única forma de saber qué tabla usa una entidad es abrir el `@Table`
de cada `@Entity` a mano.

**Anotar siempre:** nombre real de la tabla, para qué es, y **qué `@Entity` la mapea** —
porque varias no coinciden en el nombre (`Imagen` → `imagenes_copy`, no `imagenes`).

### ⚠️ Trampa: nombres que no coinciden con la entidad

| Entidad Java | Tabla real | Ojo con |
|---|---|---|
| `Imagen` | **`imagenes_copy`** | existe también `imagenes` — es la vieja, **no se usa** |
| `ProductoImagen` | **`producto_imagen_copy`** | existe también `producto_imagen` — vieja, **no se usa** |
| `ImagenPresentacion` | `imagen_presentacion` | la columna se llama `url_imagen` pero el campo Java es `nombreArchivo` |

Además, en `imagenes_copy` la columna **`base_64` guarda el NOMBRE DEL ARCHIVO**, no
base64 — nombre heredado de cuando sí se guardaba el binario en la BD.

### Inventario (81 tablas, `inventario_key_qa` al 2026-09-18)

**Productos y catálogo**
`producto` · `variantes` · `codigo_barras` · `palabra_clave` · `lotes_productos` · `favorito` · `resena`

**Imágenes**
`imagenes_copy` (entidad `Imagen`) · `producto_imagen_copy` (entidad `ProductoImagen`) · `variante_imagen` · `imagen_presentacion` · `logo`
*Muertas:* `imagenes` · `producto_imagen`
*Backups:* `producto_imagen_copy_bkp_20260811` · `variante_imagen_bkp_20260811`

**Ventas y pedidos**
`ventas` · `detalle_venta` · `detalle_venta_variantes` · `pedidos` · `detalle_pedidos` · `abono_pedido` · `grupo_pedido` (entidad `GrupoPedido`) · `grupo_pedido_miembro` (entidad `GrupoPedidoMiembro`) · `detalle_pagos` · `tipo_pago` · `pagos_y_meses` · `meses_intereses` · `iva_terminal` · `tarifa_terminal` · `mp_payment_intent`

**Clientes**
`clientes` · `clientes_sin_registro` · `direcciones`

**Usuarios, roles y permisos**
`usuarios` · `usuarios_roles` · `roles` · `permisos` · `usuario_permiso` · `rol_permiso` · `menu` · `submenu` · `usuario_submenu` · `rol_submenu` · `rol_submenu_escritura` · `accion_submenu` · `rol_accion` · `historial_acceso` · `sesion_refresh` · `usuario_modificacion`

**Flores eternas**
`ramo_armado` · `ramo_armado_accesorio` · `ramo_pedido_detalle` · `ramo_pedido_detalle_color` · `accesorio_ramo` · `tipo_flor` · `color_flor` · `cantidad_flor_valida` · `frase_liston_predefinida` · `lugares_entrega` · `lugar_entrega_anillo`

**Rifas**
`configurar_rifa` · `configurar_rifa_variante` · `boletos_rifa` · `boleto_rifa_url_compartido` · `concursantes` · `ganador_rifa` · `historial_rifa_variante`

**Chat**
`chat_sesion` · `chat_mensaje`

**Marketing y redes**
`promociones` · `promocion_detalle` · `cinta_promocion` · `hashtags_default` · `publicacion_social` · `comentario_social` · `comentario_pausa` · `tiktok_token` · `qr_destino`

**Configuración y negocio**
`configuracion_negocio` · `tema_variable` · `gastos_surtir` · `inversion`
