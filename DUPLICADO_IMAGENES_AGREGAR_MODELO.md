# Cada foto de "Agregar modelo" se guarda dos veces en el disco

**Fecha:** 2026-09-18
**Estado:** diagnosticado, sin corregir todavía
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

## Lo que hay que arreglar

En `ProductosServiceImpl`, hacer que el alta de modelos funcione como ya funciona el alta de
productos:

1. **Quitar el `Files.write` y el id inventado de `mappImagenes()`** (`:684` y `:691-693`), y
   subir los bytes de memoria al micro, igual que `subirImagenesMultipart()`.
   Con eso desaparecen el archivo duplicado y la fila huérfana de `imagenes_copy`.

2. **Quitar la doble inserción en `producto_imagen_copy`.** Las dos apps mapean esa **misma
   tabla**: `relacionProductoImagen():642` la escribe a través del micro
   (`imagenProductoClienteVPS.saveAll`) y `guardarRelacionLocal():671` la escribe otra vez en
   local, con el mismo id. Hay que dejar solo una de las dos.

   Esa duplicación es la razón de que existan `POST /v1/producto-imagen/admin/limpiar-duplicados`
   y `eliminarRelacionesDuplicadasVariante()` en el micro: se construyó un limpiador para un
   duplicado que genera el propio código.

3. **Datos viejos:** hay que limpiar los archivos y las filas de `imagenes_copy` que ya quedaron
   duplicados de todos los modelos dados de alta hasta hoy. Esto va después de los dos puntos de
   arriba, para no volver a generarlos mientras se limpia.

### Tablas, para tener el mapa claro

| Tabla | La usan |
|---|---|
| `imagenes_copy` | `micro_imagenes` **y** `proyecto_key` (la misma tabla, la misma BD) |
| `producto_imagen_copy` | `micro_imagenes` **y** `proyecto_key` (la misma tabla) |
| `variante_imagen` | solo `proyecto_key` |
