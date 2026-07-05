# Endpoints — Palabras clave, Productos y Variantes

> ⚠️ **DOCUMENTO DESACTUALIZADO (detectado 2026-07-04).** La fuente de verdad actual es
> **`CAMBIOS_FRONT.md`**. No usar este archivo para trabajo nuevo.

---

## 1. Catálogo de palabras clave `/palabras-clave`

| Método | Ruta                  | Rol        | Descripción                        |
|--------|-----------------------|------------|------------------------------------|
| GET    | `/palabras-clave`     | Público    | Lista todas las palabras clave     |
| GET    | `/palabras-clave/{id}`| Público    | Obtiene una palabra clave por ID   |
| POST   | `/palabras-clave`     | ADMIN      | Crea una nueva palabra clave       |
| PUT    | `/palabras-clave/{id}`| ADMIN      | Actualiza una palabra clave        |
| DELETE | `/palabras-clave/{id}`| ADMIN      | Elimina una palabra clave          |

### Request — POST/PUT
```json
{
  "nombre": "bolsa"
}
```

### Response
```json
{
  "id": 1,
  "nombre": "bolsa"
}
```

---

## 2. Productos `/productos`

| Método | Ruta                                           | Rol     | Descripción                                             |
|--------|------------------------------------------------|---------|---------------------------------------------------------|
| GET    | `/productos/obtenerProductos`                  | Público | Listado paginado de productos                           |
| GET    | `/productos/buscarNombreOrCodigoBarra`          | Público | Búsqueda con prioridad por código/palabra clave/nombre  |
| POST   | `/productos/save`                              | ADMIN   | Crea un producto **[nuevo campo: palabraClaveId]**      |
| PUT    | `/productos/update`                            | ADMIN   | Actualiza un producto **[nuevo campo: palabraClaveId]** |
| GET    | `/productos/findById/{id}`                     | Público | Detalle de un producto por ID                           |
| DELETE | `/productos/deleteBy/{id}`                     | ADMIN   | Elimina un producto con sus variantes e imágenes        |
| GET    | `/productos/admin/no-habilitados`              | ADMIN   | Lista productos deshabilitados paginados                |
| GET    | `/productos/admin/sin-stock`                   | ADMIN   | Lista productos sin stock paginados                     |
| PUT    | `/productos/{id}/habilitar`                    | ADMIN   | Habilita o deshabilita un producto                      |
| GET    | `/productos/admin/diagnostico-imagenes/{id}`   | ADMIN   | Diagnóstica por qué no aparece la imagen de un producto |
| GET    | `/productos/admin/sin-variantes/reporte`       | ADMIN   | Descarga Excel con productos sin variantes              |
| POST   | `/productos/compartir-imagenes-variantes`      | ADMIN   | Copia las imágenes de un producto a todas sus variantes |

---

### `GET /productos/obtenerProductos`
**Query params:** `size`, `page`

**Response admin:**
```json
{
  "pagina": 1,
  "totalPaginas": 5,
  "totalRegistros": 48,
  "t": [
    {
      "idProducto": 10,
      "nombre": "Bolsa de cuero",
      "color": "negro",
      "precioVenta": 350.0,
      "precioCosto": 180.0,
      "piezas": 1.0,
      "precioRebaja": 300.0,
      "descripcion": "Bolsa artesanal",
      "stock": 5,
      "marca": "Jade",
      "contenido": null,
      "codigoBarras": "7501055300235",
      "habilitado": "1",
      "imagen": { "urlImagen": "http://..." }
    }
  ]
}
```

**Response usuario (campos reducidos):**
```json
{
  "pagina": 1,
  "totalPaginas": 5,
  "totalRegistros": 48,
  "t": [
    {
      "idProducto": 10,
      "nombre": "Bolsa de cuero",
      "color": "negro",
      "precioVenta": 350.0,
      "descripcion": "Bolsa artesanal",
      "stock": 5,
      "codigoBarras": "7501055300235",
      "imagen": { "urlImagen": "http://..." }
    }
  ]
}
```
> El usuario solo ve productos con `stock > 0` y `habilitado = '1'`. El admin ve todos.

---

### `GET /productos/buscarNombreOrCodigoBarra`
**Query params:** `size`, `page`, `nombre` (texto a buscar)

**Lógica de prioridad (3 pasos, se detiene en el primero que encuentre):**
1. **Código de barras exacto** — si el texto coincide exactamente con un código de barras, devuelve ese producto.
2. **Palabra clave exacta** — si el texto coincide exactamente con el nombre de una palabra clave del catálogo (ej. "bolsa"), devuelve todos los productos que tengan esa palabra clave.
3. **Nombre contiene** — búsqueda LIKE sobre el nombre del producto.

**Diferencia por rol:**
- Admin → los 3 pasos buscan en todos los productos (sin filtro de stock ni habilitado).
- Usuario → los 3 pasos filtran por `stock > 0` y `habilitado = '1'`.

**Response:** mismo formato que `obtenerProductos`.

---

### `POST /productos/save` y `PUT /productos/update`
**Request — nuevo campo `palabraClaveId`:**
```json
{
  "id": null,
  "nombre": "Bolsa de cuero",
  "precioCosto": 180.0,
  "piezas": 1.0,
  "color": "negro",
  "precioVenta": 350.0,
  "precioRebaja": 300.0,
  "descripcion": "Bolsa artesanal",
  "stock": 5,
  "marca": "Jade",
  "contenido": null,
  "actualizarStock": 0,
  "eliminarStock": 0,
  "palabraClaveId": 1,
  "codigoBarras": {
    "id": null,
    "codigoBarras": "7501055300235"
  },
  "listImagenes": []
}
```
> `palabraClaveId` es el ID de la palabra clave del catálogo. Si se manda `null`, el producto queda sin categoría.

**Response:** entidad `Producto` guardada.

---

## 3. Variantes `/variantes`

| Método | Ruta                                              | Rol     | Descripción                                              |
|--------|---------------------------------------------------|---------|----------------------------------------------------------|
| GET    | `/variantes/buscar`                               | Público | Búsqueda con prioridad por código/palabra clave/nombre   |
| GET    | `/variantes/porProducto/{productoId}`             | Público | Lista variantes de un producto (sin paginación)          |
| GET    | `/variantes/porProducto/{productoId}/paginado`    | Público | Lista variantes de un producto paginadas                 |
| GET    | `/variantes/porProducto/{productoId}/paginado/resumen` | Público | Variantes con imagen incluida, paginadas           |
| POST   | `/variantes/guardarConImagenes`                   | ADMIN   | Crea/actualiza variantes con imágenes **[nuevo: palabraClaveId]** |
| POST   | `/variantes/inicializarDesdeProducto`             | ADMIN   | Crea variantes en lote desde un producto                 |
| GET    | `/variantes/imagenes/{varianteId}`                | Público | Imágenes de una variante                                 |
| GET    | `/variantes/imagenes/{varianteId}/paginado`       | Público | Imágenes de una variante paginadas                       |
| DELETE | `/variantes/imagenes`                             | ADMIN   | Elimina imágenes de una lista de variantes               |
| DELETE | `/variantes/{varianteId}/imagenes`                | ADMIN   | Elimina imágenes específicas de una variante             |
| GET    | `/variantes/admin/sin-stock`                      | ADMIN   | Variantes sin stock y deshabilitadas                     |
| GET    | `/variantes/admin/diagnostico-imagenes/{id}`      | ADMIN   | Diagnóstica imágenes de una variante                     |

---

### `GET /variantes/buscar`
**Query params:** `termino` (opcional), `pagina` (default 1), `size` (default 10)

**Lógica de prioridad (3 pasos):**
1. **Código de barras exacto** — si el término coincide con el código de barras del producto padre.
2. **Palabra clave exacta** — si el término coincide con el nombre de la palabra clave asignada a la variante.
3. **Nombre del producto contiene** — LIKE sobre el nombre del producto padre.

**Diferencia por rol:**
- Admin → los 3 pasos devuelven variantes sin importar stock ni si el producto está habilitado.
- Usuario → los 3 pasos filtran variantes con `stock > 0` y cuyo producto tenga `habilitado = '1'`.

Si `termino` está vacío, devuelve todas las variantes (listado general con el mismo filtro por rol).

**Response:**
```json
{
  "pagina": 1,
  "totalPaginas": 3,
  "totalRegistros": 25,
  "t": [
    {
      "id": 5,
      "talla": "M",
      "descripcion": "Bolsa de cuero negra talla M",
      "color": "negro",
      "presentacion": "unidad",
      "stock": 3,
      "marca": "Jade",
      "contenidoNeto": null,
      "imagenBase64": null,
      "imagenUrl": "http://...",
      "precio": 350.0,
      "codigoBarras": "7501055300235",
      "nombreProducto": "Bolsa de cuero"
    }
  ]
}
```

---

### `POST /variantes/guardarConImagenes`
**Request — nuevo campo `palabraClaveId`:**
```json
[
  {
    "id": null,
    "productoId": 10,
    "talla": "M",
    "descripcion": "Bolsa cuero negra",
    "color": "negro",
    "presentacion": "unidad",
    "stock": 2,
    "marca": "Jade",
    "contenidoNeto": null,
    "palabraClaveId": 1,
    "listImagenes": []
  }
]
```
> Para actualizar una variante existente, enviar el `id`. Para crear una nueva, enviar `id: null`.
> `palabraClaveId` es el ID del catálogo. Si se manda `null`, la variante queda sin categoría.

**Response:** lista de variantes guardadas.

---

## Cambios aplicados en esta versión

| Área              | Cambio                                                                                     |
|-------------------|--------------------------------------------------------------------------------------------|
| `palabra_clave`   | Tabla de catálogo (CRUD completo vía `/palabras-clave`)                                   |
| `producto`        | Columna `palabra_clave_id` (FK, nullable) reemplaza la tabla `producto_palabra_clave`     |
| `variantes`       | Columna `palabra_clave_id` (FK, nullable) nueva                                            |
| Búsqueda productos| Lógica de 3 pasos con prioridad: código exacto → palabra clave → nombre LIKE              |
| Búsqueda variantes| Lógica de 3 pasos con prioridad: código exacto → palabra clave → nombre LIKE              |
| Guardar producto  | `ProductoDetalle` acepta `palabraClaveId` en save y update                                |
| Guardar variante  | `VarianteDetalle` acepta `palabraClaveId` en `guardarConImagenes`                         |
| Eliminado         | Endpoints `PUT/GET /productos/{id}/palabras-clave` (ya no aplica con relación 1-a-1)     |
| Eliminado         | Clase `VriablesEntorno` (no utilizada)                                                     |
| SQL               | Ver `palabras_clave_migracion.sql` para DDL y datos de ejemplo                            |