
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

### Mapeo rama → base de datos

| Rama | Base de datos |
|---|---|
| `dev` | `inventario_key_qa` |
| `qa` | `inventario_key_qa` |
| `main` / `master` | `inventario_key` (sin sufijo) |

`dev` y `qa` apuntan a la misma BD (`inventario_key_qa`). `main` apunta a la BD de producción (`inventario_key`).

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

`CAMBIOS_FRONT.md` vive en dos lugares y **siempre deben quedar idénticos**:
- Aquí: `D:\proyectos\proyecto_key_new\CAMBIOS_FRONT.md`
- Repo del front: `D:\proyectos\documentos_front_back_nodevedaades_jade\CAMBIOS_FRONT.md` (rama `main`, sin dev/qa)

**Antes de editar `CAMBIOS_FRONT.md` en cualquiera de los dos lados**, revisar primero cuál de los
dos tiene el cambio más reciente (`git log -1` en ambos repos) — puede que el front haya editado
directamente el del repo nuevo (agregando una consulta, por ejemplo) sin que el micro se haya
enterado.

- **Si el cambio se originó aquí** (edición de código + doc en la misma sesión, lo más común):
  copiar el archivo completo a la ruta del repo del front, sobrescribiendo el destino (no fusionar
  a mano), y hacer commit + push directo a `main` de ese repo.
- **Si el cambio se originó en el repo del front** (alguien editó `CAMBIOS_FRONT.md` allá
  directamente, ej. agregó una consulta o dato): copiar ese archivo de vuelta a
  `proyecto_key_new/CAMBIOS_FRONT.md`, sobrescribiendo el de aquí.
- Esto se hace **siempre** que se detecte una diferencia entre los dos, sin necesidad de que el
  usuario lo pida cada vez — es una autorización permanente específica para este archivo y esta
  sincronización en ambas direcciones, no aplica al resto de las reglas de "no hacer commit/push
  automático" de arriba.
- El resto de ese repo (`CLAUDE.md`, `README.md`, otros archivos que ya existan ahí) no se toca —
  solo se sincroniza `CAMBIOS_FRONT.md`.
- Si en algún momento **ambos lados tienen cambios distintos y no triviales a la vez** (conflicto
  real, no solo uno más nuevo que el otro), no se sobrescribe nada solo: se le muestra el diff al
  usuario y se pregunta cuál gana antes de pisar contenido.

## JWT — Configuración y problema conocido resuelto

**Tiempos de expiración (JwtUtil.java — hardcodeados, no están en yml):**
- Access token: 15 minutos
- Refresh token: 7 días

**Bug resuelto (frontend):** Al expirar el access token, el interceptor del front hacía el refresh correctamente pero parseaba mal el response. El back devuelve `{ response: { accessToken: '...' } }` (ResponseGeneric) y el interceptor leía `response.accessToken` → guardaba `undefined` → el retry fallaba con "no se puede sacar el nombre del JWT". Fix: leer `response.response.accessToken`.

**Backend no requería cambios.** QA y Docker están correctos: env var `${TOKEN_JWT}` para el secret, `cookie.secure: true`, Redis y Rabbit configurados.

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