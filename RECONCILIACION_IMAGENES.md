# Reconciliación y Limpieza de Imágenes

## Resumen

| # | Tarea | Disparo | Qué hace |
|---|-------|---------|----------|
| 1 | Reconciliación | Endpoint manual + schedule medianoche | Por cada imagen en BD que exista en disco → la manda al microservicio externo |
| 2 | Limpieza | Schedule 4 AM | Elimina del disco archivos que no tengan registro en BD |

Ambas cubren **Productos** y **Variantes**.

---

## Regla base

> **BD con registro pero sin archivo en disco → aceptable, no se toca.**
> **Archivo en disco sin registro en BD → NO aceptable, se elimina del disco.**

---

## Endpoints para el front

Base URL: `{host}/mis-productos`
Todos requieren header `Authorization: Bearer {jwt}` con rol ADMIN.

---

### 1. Reconciliar todos los productos y variantes

```
POST /admin/reconciliacion/imagenes
```

**Sin query param** → reconcilia todos los productos y todas sus variantes.

**Request:**
```
Headers:
  Authorization: Bearer eyJhbGci...
  Content-Type: application/json   (no es obligatorio, no hay body)

Body: (vacío)
```

**Response 200:**
```json
{
  "mensaje": "La peticion fue exitosa",
  "code": 200,
  "data": {
    "ejecutadoEn": "2026-05-06T00:00:01",
    "productosRevisados": 150,
    "variantesRevisadas": 430,
    "reparados": [
      "PRODUCTO id=270 imagenId=7953469210676449000",
      "VARIANTE id=18 imagenId=1234567890"
    ],
    "faltantesEnDisco": [
      "PRODUCTO id=5 | 31cddb63_WhatsApp Image.jpeg"
    ],
    "archivosEliminadosDisco": 0,
    "bytesLiberados": 0
  },
  "lista": null
}
```

---

### 2. Reconciliar un solo producto (y sus variantes)

```
POST /admin/reconciliacion/imagenes?productoId=270
```

**Request:**
```
Headers:
  Authorization: Bearer eyJhbGci...

Query param:
  productoId=270

Body: (vacío)
```

**Response 200:** misma estructura que el caso anterior, pero `productosRevisados` = 1.

---

### 3. Limpiar BD — eliminar registros sin archivo en disco

```
POST /admin/reconciliacion/imagenes/limpiar-bd
Headers: Authorization: Bearer {jwt}
Body: vacío
```

Recorre toda la tabla `imagenes_copy`. Por cada registro cuyo archivo **no exista en disco**, elimina:
- La relación en `producto_imagen_copy`
- La relación en `variante_imagen`
- El registro en `imagenes_copy`

Corre en segundo plano — responde inmediato y se consulta el resultado con `GET /resultado`.

**Response 200:**
```json
{
  "code": 200,
  "data": "Limpieza de BD iniciada. Consulta GET /resultado para ver cuando termina."
}
```

**Response cuando ya hay un proceso corriendo:**
```json
{
  "code": 200,
  "data": "Ya hay un proceso en curso. Consulta GET /resultado."
}
```

**Resultado final (GET /resultado tras terminar):**
```json
{
  "code": 200,
  "data": {
    "enProceso": false,
    "ejecutadoEn": "2026-05-07T12:24:14",
    "productosRevisados": 0,
    "variantesRevisadas": 0,
    "reparados": [],
    "faltantesEnDisco": [],
    "archivosEliminadosDisco": 0,
    "bytesLiberados": 0,
    "imagenesEliminadas": 86
  }
}
```

El campo clave es `imagenesEliminadas` — cuántos registros de `imagenes_copy` (y sus relaciones) se borraron.

---

### 4. Ver resultado del último run

```
GET /admin/reconciliacion/imagenes/resultado
```

Devuelve el resultado de la última ejecución, ya sea manual o del scheduler de medianoche.
Útil para mostrar en el front sin volver a ejecutar el proceso.

**Request:**
```
Headers:
  Authorization: Bearer eyJhbGci...
```

**Response 200:** misma estructura del DTO.
**Response 200 con `data: null`** si nunca se ha ejecutado.

---

## Campos de la respuesta (`data`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `ejecutadoEn` | `LocalDateTime` | Fecha y hora del inicio del proceso |
| `productosRevisados` | `int` | Cuántos productos se procesaron |
| `variantesRevisadas` | `int` | Cuántas imágenes de variantes se procesaron |
| `reparados` | `List<String>` | Imágenes que existían en disco y se re-enviaron al microservicio |
| `faltantesEnDisco` | `List<String>` | Imágenes que están en BD pero el archivo no existe en disco |
| `archivosEliminadosDisco` | `int` | Archivos huérfanos eliminados del disco (limpieza 4 AM) |
| `bytesLiberados` | `long` | Bytes totales liberados del disco |
| `imagenesEliminadas` | `int` | Registros eliminados de BD sin archivo en disco (limpieza BD) |

---

## Tablas involucradas

```
imagenes_copy
  id          BIGINT   PK
  base_64     VARCHAR  → nombre del archivo en disco: {UUID}_{nombre}.ext
  extension   VARCHAR
  nombre_imagen VARCHAR

producto_imagen_copy
  producto_id INT  FK → producto
  imagen_id   BIGINT FK → imagenes_copy

variante_imagen
  variante_id INT  FK → variantes
  imagen_id   BIGINT FK → imagenes_copy
```

---

## Lógica interna — Reconciliación

```
Para cada Producto P (ORDER BY id ASC, de más antiguo a más nuevo):

  1. SELECT imagen de producto_imagen_copy JOIN imagenes_copy WHERE producto_id = P.id
  2. Para cada imagen:
       - Construir ruta: D:\Imagenes\{imagenes_copy.base_64}
       - Si el archivo EXISTE en disco → re-enviar al microservicio externo → REPARADO
       - Si NO existe en disco          → anotar en faltantesEnDisco
  3. Repetir lo mismo para cada variante del producto
```

---

## Lógica interna — Limpieza 4 AM

```
1. SELECT base_64 FROM imagenes_copy  → Set de nombres válidos
2. Listar archivos en D:\Imagenes uno por uno
3. Si el archivo NO está en el Set Y tiene más de 1 hora de antigüedad → eliminar del disco
4. Registrar total de archivos eliminados y bytes liberados
```

La limpieza **no toca la BD**, solo borra archivos físicos.

---

## Schedules automáticos

| Hora | Tarea | Necesita credenciales |
|------|-------|-----------------------|
| 00:00 (medianoche) | Reconciliar todos | Sí — usa `reconciliacion.admin-username/password` del `application.yml` |
| 04:00 AM | Limpiar disco | No — solo opera en disco local |

Configuración en `application.yml` (o variables de entorno en producción):
```yaml
reconciliacion:
  admin-username: ${RECONCILIACION_ADMIN_USERNAME:}
  admin-password: ${RECONCILIACION_ADMIN_PASSWORD:}
```

---

## Clases implementadas

```
controller/
  AdminReconciliacionController.java
    POST /admin/reconciliacion/imagenes
    GET  /admin/reconciliacion/imagenes/resultado

service/
  ReconciliacionImagenService.java
    reconciliarTodos()
    reconciliarProducto(Integer productoId)
    limpiarDiscoDia()
    getUltimoResultado()

scheduler/
  ImagenScheduler.java
    @Scheduled 00:00 → reconciliarTodos() con auth interna
    @Scheduled 04:00 → limpiarDiscoDia()

models/
  ReconciliacionResultadoDto.java

repository/
  IImagenRepository.java  (+findAllBase64)
```