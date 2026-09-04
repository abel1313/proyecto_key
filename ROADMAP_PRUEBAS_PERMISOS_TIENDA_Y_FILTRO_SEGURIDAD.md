# Roadmap de pruebas — Permisos finos en Tienda + Filtro de seguridad

Cubre las 2 features que quedaron cada una en su propia rama, sin tocar `dev`/`qa` todavía:

- **Tarea A** — `feature/permisos-finos` (back en `proyecto_key`, front en `producto_venta_online`)
- **Tarea B** — `feature/filtro-seguridad` (solo back, `proyecto_key`)

No mezclar la prueba/aprobación de las 2 — son independientes, se fusionan a `dev` por separado.

---

## Tarea A (cont.) — Ajustes en Modelos tras revisar Gestión de roles (2026-09-04)

Surgió al revisar `productos/buscar` (Modelos) en Gestión de roles: etiquetas confusas
("Eliminar producto" sonaba a comando, "Crear variantes" no existe tal cual en pantalla), el
escáner de código de barras seguía público, y cada check disparaba su propia petición al toque.

### 1. Script — `migration_accion_modelos_etiquetas_y_escaner.sql` ✅ ejecutado en QA y prod
Hace 2 cosas:
- Renombra las 5 etiquetas de Modelos para que incluyan el ícono/ubicación real del botón
  (ej. "Eliminar producto" → "Eliminar (✕ en la tarjeta)").
- Agrega la acción nueva `escanear-codigo` (📷) — antes el escáner era público, ahora es un
  permiso más, dado por defecto solo a ROLE_ADMIN (cualquier otro rol que lo usara porque era
  público lo deja de ver hasta que se le asigne).

### 2. Probar en Gestión de roles
- [ ] Abrir el submenu Modelos → confirmar que las 5 etiquetas ya no dicen "Eliminar producto"/
      "Crear variantes" a secas, sino con el ícono real entre paréntesis.
- [ ] Confirmar que aparece una 6ta casilla nueva: "Escanear código de barras (📷)".

### 3. Probar el gate del escáner
- [ ] Con un rol SIN `escanear-codigo`: en Modelos, los 2 botones de cámara (arriba en móvil, y
      el ícono 📷 junto al buscador) no deben aparecer.
- [ ] Dándole la acción al rol: ambos botones deben volver a aparecer y funcionar.

### 4. Guardado diferido — ya NO se guarda al toque
- [ ] Marcar/desmarcar varios checks seguidos (Ver, Editar, o varias acciones) de una misma
      pantalla → NO debe dispararse ninguna petición todavía; la fila se resalta y aparece
      "Cambios sin guardar en esta pantalla" con los botones ✕ Descartar / 💾 Actualizar.
- [ ] "✕ Descartar" → los checks vuelven a lo que estaba guardado antes, sin mandar nada.
- [ ] "💾 Actualizar" → recién ahí se mandan los cambios pendientes de ESA pantalla (y solo esa).
- [ ] Cambiar de rol en la columna izquierda con cambios sin guardar en el anterior → se
      descartan solos (no se arrastran de un rol a otro).
- [ ] Esto ya es así en TODAS las pantallas de Gestión de roles, no solo Modelos — probarlo
      también en cualquier otra pantalla (ej. Usuarios) para confirmar que no quedó rota.

Próximo paso (pendiente, no en este pase): aplicar la misma revisión de etiquetas/descripciones
a Tienda (`tienda/buscar`).

---

## Tarea A — Permisos finos en `tienda/buscar` (habilitar / compartir imagen)

Ya ejecutaste `migration_accion_tienda_habilitar_compartir.sql` en QA y prod ✅ — falta la
prueba funcional con las 2 ramas (back + front) desplegadas o corriendo local.

### 1. Gestión de roles
- [ ] Entrar a Gestión de roles → seleccionar el submenu "Tienda" (tienda/buscar).
- [ ] Verificar que aparecen 2 checkboxes nuevos: **"Habilitar / deshabilitar variante"** y
      **"Compartir imagen"**, cada uno con su tooltip (ícono de info) explicando dónde aparece.

### 2. Rol sin las acciones nuevas (control negativo)
- [ ] Crear o usar un rol de prueba con "Ver" en tienda/buscar pero SIN esas 2 acciones.
- [ ] Loguearse con un usuario de ese rol → ir a Tienda.
- [ ] El botón de habilitar/deshabilitar de cada tarjeta **no debe aparecer**.
- [ ] El botón de compartir imagen **no debe aparecer**.
- [ ] Si se seleccionan variantes, la barra de acciones en lote **no debe aparecer**.
- [ ] Con el token de ese usuario, probar directo por Postman/curl:
      `PUT /tienda/v1/{id}/habilitar?habilitar=true` → debe responder **403**.

### 3. Dar solo "habilitar"
- [ ] Desde Gestión de roles, marcarle al mismo rol únicamente "Habilitar / deshabilitar variante".
- [ ] Refrescar sesión del usuario de prueba (logout/login, o esperar el refresh token).
- [ ] El botón de habilitar/deshabilitar y la barra en lote **sí deben aparecer y funcionar**.
- [ ] "Compartir imagen" **sigue sin aparecer**.

### 4. Dar también "compartir-imagen"
- [ ] Marcarle además "Compartir imagen".
- [ ] Verificar que el botón aparece y el flujo de compartir (WhatsApp Web / Facebook / descargar /
      copiar imagen) funciona igual que antes.

### 5. Con ROLE_ADMIN
- [ ] Confirmar que un usuario admin real sigue viendo y usando todo exactamente igual que antes
      del cambio (nada debe haberse roto): toggle habilitar, barra en lote, compartir, editar.

### 6. Lo que NO cambió (verificación de que no se tocó de más)
- [ ] El checkbox de selección en cada tarjeta y el botón "Editar" (✏️) siguen dependiendo del
      admin general — no deben aparecer/desaparecer según estas 2 acciones nuevas.
- [ ] El escáner de cámara sigue público, sin gate.

---

## Tarea B — Filtro global de saneamiento de entradas

Solo código, no toca base de datos — no hay script que ejecutar para esta parte.

Con la rama `feature/filtro-seguridad` corriendo (local o en un ambiente propio), probar contra
cualquier endpoint real — no hace falta pasar por el front, sirve curl/Postman directo:

### 1. XSS en query param
```
GET /v1/productos/buscarNombreOrCodigoBarra?nombre=<script>alert(1)</script>
```
Esperado: **400**, body `{"mensaje":"La peticion contiene datos no permitidos y fue rechazada", ...}`

### 2. Inyección SQL en body JSON
```
POST /v1/clientes/save
Content-Type: application/json

{ "nombre": "a' OR '1'='1" }
```
Esperado: **400**

### 3. Path traversal en la URL
```
GET /v1/imagen/../../etc/passwd
```
Esperado: **400**

### 4. Bytes nulos
```
GET /v1/productos/buscarNombreOrCodigoBarra?nombre=archivo%00.jpg
```
Esperado: **400**

### 5. Controles negativos — esto NO debe bloquearse
- [ ] Nombre real con apóstrofe, ej. `O'Brien`, en cualquier alta (cliente, producto, etc.) →
      debe guardarse normal.
- [ ] Subir una foto real en "Agregar variante" (`POST /tienda/v1/guardarConImagenes`) → el
      archivo debe subir normal, sin bloqueo (el filtro no revisa binarios de archivos).
- [ ] Un comentario o reseña con texto libre normal (con comas, paréntesis, signos de pregunta)
      → no debe bloquearse.

### 6. Logs
- [ ] Confirmar en los logs del back que cada rechazo queda registrado en nivel `WARN` con:
      categoría (XSS / SQLi / path-traversal / null-byte), método, URL, y en qué parte de la
      petición se encontró (path / query-param / body / multipart-campo).

### 7. Tests automatizados
- [ ] Correr `mvn test` en la rama → deben pasar los 53 tests, incluidos los 7 nuevos de
      `InputSanitizationFilterTest` (cubren las 3 zonas revisadas + el caso multipart: campo de
      texto se revisa, archivo binario se ignora).

---

## Antes de fusionar cualquiera de las 2 a `dev`
- Aprobar cada rama por separado (no fusionar ambas juntas).
- Seguir el flujo normal de `CLAUDE.md`: `dev → qa → main`, nunca al revés.
