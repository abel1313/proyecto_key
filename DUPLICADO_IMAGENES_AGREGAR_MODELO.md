# Cada foto de "Agregar modelo" se guarda dos veces en el disco

**Fecha:** 2026-09-18
**Estado:** ✅ corregido en el código (2026-09-18) — falta limpiar los duplicados viejos del disco y la BD
**Dónde pasa:** menú **📦 Catálogo → ➕ Agregar modelo** (ruta `productos/agregar`)
**Dónde NO pasa:** menú **📦 Catálogo → 🧩 Agregar producto** (ruta `tienda/venta`) — ver la última sección

---

## Qué pasa, en una línea

Cuando das de alta un **modelo** con foto, el archivo se escribe **dos veces en `/app/imagenes`**:
una vez por `proyecto_key` y otra por `micro_imagenes`. Los dos archivos tienen los **mismos
bytes** y el **mismo nombre original**, solo cambia el UUID que llevan de prefijo. La copia que
escribe `proyecto_key` no la usa nadie después, y **nunca se borra**.

---

## Cómo reproducirlo a mano

1. Entra al admin y abre el menú lateral.
2. Abre el grupo **📦 Catálogo**.
3. Entra a **➕ Agregar modelo** (es el de arriba, el que dice *"Da de alta el artículo genérico
   (ej. Blusa Zara)"*). **No** el de abajo que dice "Agregar producto" — ese no tiene el problema.
4. Llena los datos del modelo. Para que sea fácil de encontrar después, **ponle a la foto un
   nombre de archivo único y reconocible** antes de subirla, por ejemplo `pruebadup1.png`.
5. Sube **una sola foto** (con una alcanza para ver el duplicado).
6. Dale a **Guardar** y espera a que confirme que se guardó.

Con eso ya quedaron los dos archivos en el disco.

---

## Cómo ver los dos archivos

El volumen `/app/imagenes` es el **mismo** para los dos servicios, así que puedes entrar por
cualquiera de los dos pods:

```bash
kubectl exec -it deploy/imagenes-deployment -n qa -- sh -c "ls -la /app/imagenes | grep -i pruebadup1"
```

**Lo que vas a ver — dos archivos:**

```
-rw-r--r-- 1 root root 148523 Sep 18 10:14 3f2a9c1e-...-b7d4_pruebadup1.png
-rw-r--r-- 1 root root 148523 Sep 18 10:14 a81b47f0-...-2e19_pruebadup1.jpg
```

Cómo confirmar que son **la misma foto duplicada** y no dos fotos distintas:

- **Terminan con el mismo nombre** (`pruebadup1`), y solo cambia el UUID de adelante.
- **Pesan exactamente lo mismo** (la columna del tamaño es idéntica) — son los mismos bytes.
- Llevan **la misma hora**, con segundos de diferencia a lo mucho.

⚠️ **La extensión puede no coincidir** entre los dos, y eso es esperado: el segundo archivo pasa
por `NombreArchivoImagen.normalizar()`, que renombra según los bytes reales. Si subiste un
`.png` que el recorte del front convirtió a JPEG, el primero queda `.png` y el segundo `.jpg`.
El nombre base y el peso siguen siendo los mismos, y eso es lo que importa.

Para confirmarlo sin lugar a dudas, compara los hashes — tienen que ser idénticos:

```bash
kubectl exec -it deploy/imagenes-deployment -n qa -- sh -c "md5sum /app/imagenes/*pruebadup1*"
```

**Y en la base de datos** quedan dos filas para una sola foto:

```sql
SELECT id, base_64, nombre_imagen FROM imagenes_copy WHERE base_64 LIKE '%pruebadup1%';
```
Dos filas con **ids distintos**. Solo una de las dos está referenciada en `producto_imagen_copy`
(la del micro); la otra queda colgada sin que nada la use.

---

## El rastreo hacia atrás: de la escritura al botón del front

Va de abajo hacia arriba — de donde se escribe el archivo, hasta la pantalla que lo dispara.

### Escritura #2 — la del micro (esta es la que se queda)

| Paso | Dónde |
|---|---|
| `Files.write(path, imagen.getImagen())` con nombre `UUID2_<nombre>` | `micro_imagenes` → `ClienteDisco.create():54` |
| lo llama | `ImagenService.create():25` (micro) |
| lo llama | `ImagenController.save()` ← **`POST /v1/imagenes`** (multipart) |
| lo llama desde proyecto_key | `ImageneClienteDisco.save()` |
| lo llama | `ProductosServiceImpl.relacionProductoImagen():642` → `imagenPort.save(builder.build())` |

Y justo antes de subirlo, ese mismo método **lee del disco el archivo que ya se había escrito**:

```java
// ProductosServiceImpl:612-613
Path path = Paths.get(rutaImagenes, p.getImagen().getBase64());
byte[] imagenBytes = Files.readAllBytes(path);
```

Ese `Files.readAllBytes` es la pista de que hubo una escritura previa: los bytes ya venían en la
petición, no hacía falta ir al disco a buscarlos.

### Escritura #1 — la de proyecto_key (esta es la que sobra)

| Paso | Dónde |
|---|---|
| `Files.write(path, decodedBytes)` con nombre `UUID1_<nombre>` | `ProductosServiceImpl.mappImagenes():684` |
| genera además un **id local inventado** | `ProductosServiceImpl:691-693` → `Math.abs(msb ^ lsb)` |
| lo llama | `ProductosServiceImpl:481` (modelo nuevo) y `:501` (modelo que ya existe) |
| | `iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()))` |
| protegido por | `if (!productoDetalle.getListImagenes().isEmpty())` — solo corre si la petición trae fotos |
| lo llama | `ProductosControllerImpl` (`@RequestMapping("/v1/productos")`) ← **`POST /v1/productos/save`** |

### Y del endpoint al menú

| Paso | Dónde |
|---|---|
| `this.http.post('/v1/productos/save', det)` | `producto.service.ts:131` |
| arma el body con las fotos | `add.component.ts:294` → `listImagenes: this.imagenesCargadas` |
| llena `imagenesCargadas` al elegir la foto | `add.component.ts:391` |
| pantalla | `src/app/productos/producto/add/add.component.ts`, ruta **`productos/agregar`** |
| entrada en el menú | `navbar.component.html:68` → **📦 Catálogo → ➕ Agregar modelo** |

**El mismo camino corre al editar un modelo con fotos** (`:501`), no solo al crearlo.

---

## Por qué nunca se limpia solo

`ReconciliacionImagenService.limpiarDiscoDia()` corre a las 4 AM y borra del disco todo archivo
cuyo nombre no esté en `imagenes_copy`, `imagen_presentacion` ni `logo`.

El archivo duplicado **sí** está en `imagenes_copy` — la fila con el id local que insertó
`mappImagenes()`. Así que el limpiador lo considera legítimo y lo conserva. Por eso el disco y la
tabla crecen al doble de lo necesario y no se corrigen con el tiempo.

---

## Lo que NO tiene este problema

**📦 Catálogo → 🧩 Agregar producto** (ruta `tienda/venta`) está bien hecho y **no hay que
tocarlo**. `VarianteServiceImpl.subirImagenesMultipart():299-320` manda los bytes **de memoria
directo al micro** — no hay ni un `Files.write` en toda la clase. Del micro solo vuelve el id
(`.map(ImagenDto::getId)`) y `vincularImagenes():632` lo registra en `variante_imagen`.

**Una foto, un archivo, un id.** Es el patrón correcto y es el modelo a copiar.

---

## Lo que se arregló (2026-09-18)

`ProductosServiceImpl` ahora sube las fotos igual que ya lo hacía el alta de productos: **los
bytes que llegan en la petición van directo al micro, sin pasar por el disco local**.

| Antes | Ahora |
|---|---|
| `mappImagenes()` escribía el archivo en `/app/imagenes` con un UUID propio | ese método ya no existe |
| `mappImagenes()` inventaba un id (`Math.abs(msb ^ lsb)`) y lo insertaba en `imagenes_copy` | el id lo asigna el micro, y es el único que existe |
| `relacionProductoImagen()` releía el archivo con `Files.readAllBytes` | `subirImagenesAlMicro()` manda el `byte[]` del `ImagenDTO` |
| la relación se insertaba **dos veces** en `producto_imagen_copy`: por Rabbit (`imagenProductoClienteVPS.saveAll`) y en local (`guardarRelacionLocal`) | solo `guardarRelacionLocal()` |

Se eliminó también el campo `rutaImagenes` y la dependencia `IImagenService` de esta clase: esta
clase ya no toca el disco ni escribe en `imagenes_copy`.

**Por qué se quedó `guardarRelacionLocal()` y no la publicación a Rabbit:** es síncrona (la fila
queda antes de responder, no "cuando llegue el mensaje"), es la única que setea `principal`, y no
depende de que el ambiente tenga RabbitMQ — `main` no lo tiene configurado.

**Una foto → un archivo → un id → una fila.**

## Lo que falta: limpiar lo viejo

El arreglo evita duplicados **nuevos**. Lo que ya está guardado sigue ahí — y son **dos
duplicados distintos**, que se limpian de formas distintas:

| | Duplicado A — archivo + fila en `imagenes_copy` | Duplicado B — fila en `producto_imagen_copy` |
|---|---|---|
| Qué se repite | el archivo en `/app/imagenes` y su fila en `imagenes_copy` | la pareja `(producto_id, imagen_id)` |
| Quién lo generaba | `mappImagenes()` (escribía el archivo + insertaba con id inventado) | `imagenProductoClienteVPS.saveAll()` (Rabbit) **y** `guardarRelacionLocal()`, las dos a la misma tabla |
| ¿Está ligado a un producto? | **No.** El id inventado nunca llegó a `producto_imagen_copy` — `mapperRelacionProductoImagen()` armaba los objetos en memoria pero nunca los guardaba; solo servían para que `relacionProductoImagen()` leyera el `base_64` y encontrara el archivo en disco. | **Sí.** Las dos filas apuntan al id real del micro. |
| ¿Sale en el carrusel? | No | **Sí, la misma foto dos veces** |
| ¿La encuentra la consulta de huérfanas? | Sí | **No** |
| Cómo se limpia | borrar la fila huérfana; el limpiador nocturno barre el archivo | `DELETE` conservando `MIN(id)` por pareja |

El carrusel de **Actualizar producto** y de **Detalle de producto** sale de
`ProductoImagenService.listarImagenesProducto()` → `listarConDetalle()`:

```sql
FROM producto_imagen_copy pic
JOIN imagenes_copy ic ON pic.imagen_id = ic.id
WHERE pic.producto_id = :productoId
```

Una entrada por **fila de la relación**. Por eso el duplicado B se ve, y el A no.

### Consulta que responde las dos cosas de un tiro

```sql
SELECT pic.producto_id,
       ic.nombre_imagen,
       COUNT(*)                      AS filas,
       COUNT(DISTINCT pic.imagen_id) AS ids_distintos,
       GROUP_CONCAT(DISTINCT pic.imagen_id) AS imagen_ids
FROM producto_imagen_copy pic
JOIN imagenes_copy ic ON ic.id = pic.imagen_id
GROUP BY pic.producto_id, ic.nombre_imagen
HAVING COUNT(*) > 1
ORDER BY filas DESC;
```

- `ids_distintos = 1` → **duplicado B**: la misma imagen ligada dos veces. Se limpia con el
  endpoint que ya existe en el micro (abajo).
- `ids_distintos > 1` → dos filas de `imagenes_copy` **distintas**, con la misma foto, ambas
  ligadas al producto. Este es el caso que la consulta de huérfanas no ve. Pasa con datos
  anteriores al fix del 2026-09-02, cuando `producto_imagen_copy` llegó a guardarse con el id
  local (ver el comentario que había en `relacionProductoImagen()`). Aquí hay que **elegir cuál
  conservar**: la que el micro conoce, o sea la que responde 200 en
  `GET /v1/imagenes/file/{id}`.

### Limpieza B — relación repetida

Ya existe el limpiador, construido justo para este duplicado:

```
POST /v1/producto-imagen/admin/limpiar-duplicados
```

Conserva el `MIN(id)` de cada pareja `(producto_id, imagen_id)`. El equivalente a mano:

```sql
DELETE FROM producto_imagen_copy
WHERE id NOT IN (
    SELECT * FROM (SELECT MIN(id) FROM producto_imagen_copy GROUP BY producto_id, imagen_id) AS mantener
);
```

### Limpieza A — archivo y fila huérfana

```sql
SELECT i.id, i.base_64, i.nombre_imagen
FROM imagenes_copy i
WHERE NOT EXISTS (SELECT 1 FROM producto_imagen_copy pi WHERE pi.imagen_id = i.id)
  AND NOT EXISTS (SELECT 1 FROM variante_imagen      vi WHERE vi.imagen_id = i.id)
ORDER BY i.id;
```

Lo que devuelva es seguro de borrar: por definición no lo referencia nadie. Al borrar la fila, el
archivo queda sin registro y `ReconciliacionImagenService.limpiarDiscoDia()` (4 AM) lo barre solo.

### Cuánto hay duplicado en disco

En el VPS el volumen es un `hostPath`: `/home/ubuntu/imagenes/qa/Imagenes` (qa) — el mismo
directorio que los dos pods montan en `/app/imagenes`.

```bash
cd /home/ubuntu/imagenes/qa/Imagenes
find . -maxdepth 1 -type f -exec md5sum {} + | awk '{print $1}' | sort > /tmp/h.txt
echo "archivos totales : $(wc -l < /tmp/h.txt)"
echo "fotos distintas  : $(sort -u /tmp/h.txt | wc -l)"
echo "copias de mas    : $(( $(wc -l < /tmp/h.txt) - $(sort -u /tmp/h.txt | wc -l) ))"
```

### Orden

1. Respaldo del directorio y de las dos tablas.
2. Correr la consulta de arriba y separar los casos `ids_distintos = 1` de los `> 1`.
3. Limpieza B (el endpoint) → el carrusel deja de mostrar repetidas.
4. Los casos `ids_distintos > 1`: verificar cuál id responde en `/v1/imagenes/file/{id}`, dejar
   esa fila en `producto_imagen_copy` y borrar la otra.
5. Limpieza A → borrar huérfanas de `imagenes_copy`; el limpiador nocturno se lleva los archivos.

Va **después** de desplegar este arreglo, para no limpiar mientras se siguen generando.

### Tablas, para tener el mapa claro

| Tabla | La usan |
|---|---|
| `imagenes_copy` | `micro_imagenes` **y** `proyecto_key` (la misma tabla, la misma BD) |
| `producto_imagen_copy` | `micro_imagenes` **y** `proyecto_key` (la misma tabla) |
| `variante_imagen` | solo `proyecto_key` |
