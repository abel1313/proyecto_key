# Pendientes — no urgente

Fecha: 2026-06-09

---

## 1. Migración v1 a medias — `proyecto_key_new`

~25 controladores + SecurityConfig modificados en working tree sin commitear.
Migran paths de `/algo` a `/v1/algo`.

**Problema a corregir antes de commitear:**
`VarianteController.java` está agregando `/v1/` a nivel de método en vez de a nivel de clase.
Resultado actual (incorrecto): `/variantes/v1/buscar`
Resultado correcto: `/v1/variantes/buscar`
Fix: cambiar `@RequestMapping("variantes")` a `@RequestMapping("/v1/variantes")` en la clase
y revertir los `/v1/` individuales de cada método.

También hay 2 docs sin trackear: `ENDPOINTS_MIGRACION_V1.md` y `FIX_FRONT_JWT_INTERCEPTOR.md`.

---

## 2. Imágenes perdidas en disco — re-subida manual

Productos con registro en BD pero archivo físico faltante en disco:
- Producto 266 (imagen `cccccccc.jpeg`, id `2307654962269473178`)
- Productos 270, 272, 273

Con BUG-KEY-11 resuelto (header Authorization duplicado) las subidas nuevas ya funcionan.
Solo hay que re-subir estas imágenes manualmente desde el panel admin.

---

## 3. Credenciales AWS — revocar en IAM

BUG-IMG-08: las credenciales antiguas de AWS (`AKIASKOXZG7C5JKLWERJ`) siguen activas.
Entrar a consola AWS → IAM → Usuarios → desactivar/borrar esa access key.
No se puede hacer desde código.

---

## 4. PERF-IMG-01 y PERF-IMG-02 — Streaming de imágenes

`micro_imagenes`: el modelo usa `byte[]` para las imágenes en lugar de streaming.
Cada lectura de imagen carga el archivo completo en heap.
Requiere refactor del dominio hexagonal (ClienteDiscoPort → InputStream, ImagenCasoUso, ImagenController).
Deuda arquitectural, pospuesto para una iteración futura.
