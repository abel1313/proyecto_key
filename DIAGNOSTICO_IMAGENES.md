# Diagnóstico de Imágenes
**Solo administradores · Verifica BD ↔ Microservicio**

## Endpoints disponibles

| Tipo | Ruta | Descripción |
|------|------|-------------|
| 📦 Producto | `GET /productos/admin/diagnostico-imagenes/{productoId}` | Diagnóstico de imágenes de un producto |
| 🏷️ Variante | `GET /variantes/admin/diagnostico-imagenes/{varianteId}` | Diagnóstico de imágenes de una variante |

---

## Qué hace el diagnóstico

Consulta dos fuentes y las cruza:
1. **BD local** (`producto_imagen_copy` / `variante_imagen`) — lo que el microservicio de productos guardó como referencia.
2. **Microservicio de imágenes externo** — si realmente tiene el archivo disponible para servir.

---

## Campos de la respuesta (Producto)

| Campo | Qué significa |
|-------|---------------|
| `productoId` | ID del producto consultado |
| `nombreProducto` | Nombre del producto |
| `totalImagenesLocalDB` | Cuántos registros de imagen hay en BD local |
| `imagenPresenteEnMicroservicio` | `true` si el microservicio devolvió bytes de imagen; `false` si no |
| `detalleExternoLista` | Texto descriptivo del estado en el microservicio externo |
| `imagenesLocalDB` | Lista de registros en BD: id, nombre, extensión, ruta en disco |

---

## Casos posibles y qué hacer

| Situación | Diagnóstico | Acción |
|-----------|-------------|--------|
| `totalImagenesLocalDB = 0` | Nunca se guardó la imagen en BD | Volver a subir la imagen desde el formulario de producto |
| `totalImagenesLocalDB > 0` y `imagenPresenteEnMicroservicio = false` | BD tiene el registro pero el archivo se perdió en el microservicio | Re-subir la imagen; el archivo físico no existe en el microservicio |
| `totalImagenesLocalDB > 0` y `imagenPresenteEnMicroservicio = true` | Todo correcto | Revisar caché de Redis (puede estar sirviendo datos viejos) |

---

## Caso real registrado — 2026-05-06

### Producto: Great Jeans (ID 270)

**Endpoint consultado:**
```
GET /productos/admin/diagnostico-imagenes/270
```

**Resultado:**
```json
{
  "productoId": 270,
  "nombreProducto": "Great Jeans",
  "totalImagenesLocalDB": 2,
  "imagenPresenteEnMicroservicio": false,
  "detalleExternoLista": "respuesta sin bytes de imagen"
}
```

**Imágenes en BD local:**

| ID | Nombre | Tipo | Ruta en disco |
|----|--------|------|---------------|
| #7953469210676449000 | WhatsApp Image 2026-05-05 at 14.25.11.jpeg | image/jpeg | `31cddb63-dcba-47a6-837f-91d8fcf1b1ad_WhatsApp Image 2026-05-05 at 14.25.11.jpeg` |
| #6183428341062978000 | WhatsApp Image 2026-05-05 at 14.25.11.jpeg | image/jpeg | `6b72b05f-801f-4b94-9b9b-ca580322468b_WhatsApp Image 2026-05-05 at 14.25.11.jpeg` |

**Diagnóstico:** ⚠️ BD tiene el registro pero el microservicio no tiene el archivo

**Interpretación:**
- La BD local tiene 2 entradas para este producto, lo que confirma que alguna vez se subieron las imágenes correctamente.
- El microservicio externo responde pero devuelve `respuesta sin bytes de imagen`, lo que indica que el archivo físico ya no existe en su almacenamiento (fue eliminado, el volumen de disco se reinició, o hubo un error de escritura).
- El producto aparece sin imagen en el listado porque el microservicio no puede servirla.

**Solución:** Volver a subir la imagen desde el formulario de edición del producto Great Jeans. Al guardar, el sistema re-registrará la imagen en BD y la enviará nuevamente al microservicio.