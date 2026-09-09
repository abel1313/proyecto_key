
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