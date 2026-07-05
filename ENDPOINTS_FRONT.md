# Endpoints para el Frontend — Imágenes

> ⚠️ **DOCUMENTO DESACTUALIZADO (detectado 2026-07-04).** La fuente de verdad actual es
> **`CAMBIOS_FRONT.md`** — ahí está la migración completa de imágenes a `/v1/` y al microservicio
> `micro_imagenes`. No usar este archivo para trabajo nuevo.

> **Base URL dev:**
> - proyecto_key: `http://localhost:9091/mis-productos`
> - micro_imagenes: `http://localhost:9096/mis-productos`
>
> **Base URL QA:**
> - proyecto_key: `http://localhost:30091/mis-productos`
> - micro_imagenes: `http://localhost:30096/mis-productos`
>
> ⚠️ `ENDPOINT_IMAGENES` en el deploy QA debe ser la URL pública accesible desde el browser.  
> Para NodePort: `http://localhost:30096/mis-productos`

---

## Wrapper de responses en micro_imagenes

Los endpoints de `ProductoImagenController` envuelven el response así:
```json
{ "codigo": 200, "mensaje": "La peticion fue exitosa", "response": { ... } }
```
Los endpoints de `ImagenController` devuelven el objeto **directo** (sin wrapper).  
`listar` y `buscarImagenProducto` también devuelven **directo** (sin wrapper).

---

## micro_imagenes — `/imagenes`

### `POST /imagenes` — subir imágenes
```
Request: multipart/form-data  campo: files[]
Response: List<Imagen> (sin wrapper)
[
  {
    "id": "5732523035246479361",
    "nombreImagen": "foto.jpeg",
    "urlImagen": "uuid-generado_foto.jpeg",   ← nombre en disco, NO una URL
    "contentType": "image/jpeg",
    "imagen": null
  }
]
```

### `GET /imagenes/file/{id}` ← **usar para `<img src>`**
Devuelve bytes crudos de la imagen con `Content-Type` correcto.
```
GET /imagenes/file/5732523035246479361
Response: bytes (image/jpeg)
204 No Content → imagen no encontrada en disco
```

### `GET /imagenes/{id}` — metadata en JSON
```
GET /imagenes/5732523035246479361
Response: (sin wrapper)
{
  "id": "5732523035246479361",
  "nombreImagen": "foto.jpeg",
  "urlImagen": "uuid-generado_foto.jpeg",
  "contentType": "image/jpeg",
  "imagen": [ ...bytes... ]
}
```

### `GET /imagenes?ids=id1,id2` — múltiples en JSON
```
GET /imagenes?ids=111,222
Response: List<Imagen> (sin wrapper) — mismo formato que GET /{id}
```

### `GET /imagenes/verificar?ids=id1,id2`
Devuelve qué IDs existen realmente en disco.
```
GET /imagenes/verificar?ids=111,222,333
Response: [111, 333]
```

### `DELETE /imagenes?ids=id1,id2`
Elimina imágenes por IDs (archivo en disco + registro en BD).
```
DELETE /imagenes?ids=111,222
Response: 204 No Content
```

### `DELETE /imagenes/disco?ids=nombre1,nombre2`
Elimina solo los archivos del disco (los IDs son nombres de archivo, no IDs numéricos).
```
DELETE /imagenes/disco?ids=uuid_foto.jpeg
Response: 204 No Content
```

---

## micro_imagenes — `/producto-imagen`

### `GET /producto-imagen/buscarImagenProducto/{productoId}` ← **para listado**
Devuelve la imagen principal del producto con bytes. Usar como `<img src>` en listados.
```
GET /producto-imagen/buscarImagenProducto/265
Response: (sin wrapper)
{
  "id": null,
  "nombreImagen": "foto.jpeg",
  "urlImagen": "uuid_foto.jpeg",
  "contentType": "image/jpeg",
  "imagen": [ ...bytes... ]
}
```
> Devuelve objeto vacío `{}` si el producto no tiene imagen (no lanza 404).

### `GET /producto-imagen/listar/{productoId}?pagina=1&size=8` ← **para detalle**
Lista todas las imágenes del producto paginadas. Solo incluye las que existen en disco.
```
GET /producto-imagen/listar/265?pagina=1&size=8
Response: (sin wrapper)
{
  "productoId": 265,
  "pagina": 1,
  "totalPaginas": 2,
  "totalImagenes": 10,
  "listaImagenes": [
    {
      "id": "5732523035246479361",
      "extension": "image/jpeg",
      "nombreImagen": "foto.jpeg",
      "urlImagen": "http://localhost:9096/mis-productos/imagenes/file/5732523035246479361",
      "principal": true
    }
  ]
}
```
> `urlImagen` ya lista para `<img src>`.  
> La imagen `principal: true` viene primera.

### `POST /producto-imagen/saveAll`
Guarda múltiples relaciones producto-imagen (se usa cuando se sube una imagen de producto).
```json
Request: List
[
  { "imagenId": 5732523035246479361, "productoId": 265, "principal": true, "id": null }
]
Response: { "codigo": 200, "mensaje": "La peticion fue exitosa", "response": { } }
```

### `PUT /producto-imagen/{productoImagenId}/principal`
Marca una imagen como principal del producto.
```
PUT /producto-imagen/42/principal
Response: { "mensaje": "Imagen marcada como principal correctamente" }
```

### `DELETE /producto-imagen/{id}`
Elimina la relación producto-imagen. Si nadie más usa la imagen, también borra disco y BD.
```
DELETE /producto-imagen/42
Response: { "codigo": 200, "mensaje": "La peticion fue exitosa", "response": { } }
```

---

## micro_imagenes — `/cache`

### `DELETE /cache/limpiar`
Limpia toda la caché del microservicio (admin).
```
DELETE /cache/limpiar
Response: { "mensaje": "Toda la cache fue limpiada correctamente" }
```

---

## proyecto_key — `/productos`

> Todos los responses van dentro de `{ "mensaje", "code", "data": { ... } }`

### `GET /productos/obtenerProductos?size=10&page=1`
```json
Response data:
{
  "pagina": 1, "totalPaginas": 5, "totalRegistros": 48,
  "t": [
    {
      "idProducto": 265,
      "nombre": "Pantalón slim",
      "precioVenta": 350.0,
      "stock": 10,
      "color": "negro",
      "codigoBarras": "7501234567890",
      "imagen": {
        "urlImagen": "http://localhost:9096/mis-productos/producto-imagen/buscarImagenProducto/265"
      }
    }
  ]
}
```
> `imagen.urlImagen` devuelve bytes directamente. Usar como `<img src>`.

### `GET /productos/buscarNombreOrCodigoBarra?size=10&page=1&nombre=slim`
Mismo formato de response que `obtenerProductos`.

### `GET /productos/findById/{id}`
```json
Response data:
{
  "id": 265, "nombre": "Pantalón slim",
  "precioVenta": 350.0, "precioCosto": 200.0,
  "stock": 10, "color": "negro", "marca": "Marca X",
  "descripcion": "...", "codigoBarras": "7501234567890",
  "habilitado": "1"
}
```

### `POST /productos/save` / `PUT /productos/update`
```json
Request:
{
  "nombre": "Pantalón slim",
  "precioVenta": 350.0, "precioCosto": 200.0,
  "stock": 10, "color": "negro",
  "codigoBarras": { "codigoBarras": "7501234567890" },
  "listImagenes": [
    { "base64": "<bytes>", "nombreImagen": "foto.jpeg", "extension": "image/jpeg" }
  ],
  "imagenPrincipalId": null
}
```

### `DELETE /productos/deleteBy/{id}`
```
Response: 204 No Content
```

---

## proyecto_key — `/variantes`

### `GET /variantes/buscar?termino=slim&pagina=1&size=10` ← **para listado**
```json
Response data:
{
  "pagina": 1, "totalPaginas": 3, "totalRegistros": 25,
  "t": [
    {
      "id": 1, "talla": "M", "color": "negro", "stock": 5,
      "precio": 350.0, "nombreProducto": "Pantalón slim",
      "codigoBarras": "7501234567890",
      "imagenUrl": "http://localhost:9096/mis-productos/imagenes/file/5732523035246479361"
    }
  ]
}
```
> `imagenUrl` es la imagen principal si existe en disco. `null` si no tiene imagen.  
> 404 si no hay resultados.

### `GET /variantes/imagenes/{varianteId}/paginado?pagina=1&size=10` ← **para detalle**
Solo devuelve imágenes que existen en disco.
```json
Response data:
{
  "pagina": 1, "totalPaginas": 2, "totalRegistros": 6,
  "t": [
    {
      "id": "5732523035246479361",
      "extension": "image/jpeg",
      "nombreImagen": "foto.jpeg",
      "urlImagen": "http://localhost:9096/mis-productos/imagenes/file/5732523035246479361",
      "principal": true
    }
  ]
}
```

### `GET /variantes/v2/imagenes/{varianteId}`
Igual que el paginado pero devuelve **todas** las imágenes de la variante sin paginar.

### `DELETE /variantes/v2/{varianteId}/imagenes`
Elimina imágenes específicas. Solo borra del disco si ninguna otra variante/producto las usa.
```json
DELETE /variantes/v2/1/imagenes
Body: [5732523035246479361, 1234567890]
Response data: "Imágenes eliminadas correctamente"
```

### `PUT /variantes/imagenes/{varianteImagenId}/principal`
```
PUT /variantes/imagenes/15/principal
Response data: "Imagen marcada como principal correctamente"
```

### `POST /variantes/guardarConImagenes`
```json
Request: [
  {
    "productoId": 265, "talla": "M", "color": "negro", "stock": 5,
    "listImagenes": [
      { "base64": "<bytes>", "nombreImagen": "foto.jpeg", "extension": "image/jpeg" }
    ],
    "imagenPrincipalId": null
  }
]
Response data: [ { "id": 1, "talla": "M", "color": "negro", ... } ]
```

---

## Bug QA — URLs con localhost

**Síntoma:** `producto-imagen/listar` y `variantes/buscar` devuelven `urlImagen` con `localhost:9096` en vez de la URL pública.

**Causa:** `ENDPOINT_IMAGENES` en el deploy QA apunta al puerto interno del contenedor.

**Fix en el deployment QA de ambos servicios:**
```yaml
# NodePort:
ENDPOINT_IMAGENES=http://localhost:30096/mis-productos

# Dominio público:
ENDPOINT_IMAGENES=https://backend-imagenes.novedades-jade.com.mx/mis-productos
```
