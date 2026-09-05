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

## Tarea A (cont. 2) — Agrupar acciones por categoría en Gestión de roles (2026-09-04)

El checklist de acciones salía todo junto (15 en Modelos, 11 en Tienda) sin separar filtros de
las opciones de la tarjeta o del buscador.

### 1. Script — `migration_accion_submenu_categoria.sql` ✅ ejecutado en QA y prod
Agrega la columna `accion_submenu.categoria` y renumera `orden` para que cada categoría quede
en un tramo contiguo:
- **Modelos**: Filtros (los 9 + Excel) → Tarjeta de modelo (eliminar/habilitar/crear-variantes/
  compartir-imagen) → Buscador (escanear código).
- **Tienda**: Filtros (los 9) → Tarjeta de variante (habilitar/compartir-imagen).

### 2. Probar en Gestión de roles
- [ ] Abrir Modelos y Tienda → confirmar que las acciones aparecen en 3 (Modelos) / 2 (Tienda)
      bloques con su propio sub-encabezado en mayúsculas, en vez de una lista plana.
- [ ] Confirmar que dentro de cada bloque el orden de los checkboxes tiene sentido (filtros en
      el mismo orden que la barra real, tarjeta en el orden de los botones de la tarjeta).

---

## Tarea A (cont. 3) — Descripción propia para "Editar" (2026-09-04)

"Editar" era el único checkbox del sistema sin botón ℹ️ propio (Ver y cada acción ya lo
tenían). Caso real que lo disparó: "Editar" en Modelos no correspondía a nada visible en esa
pantalla — es un permiso compartido por OR con Agregar modelo y Agregar producto.

### 1. Script — `migration_submenu_descripcion_escritura.sql` ✅ ejecutado en QA y prod
Agrega `submenu.descripcion_escritura` y la llena para los 3 grupos donde el permiso de
escritura de verdad se comparte entre pantallas hermanas: Modelos + Agregar modelo + Agregar
producto, las 3 de Rifas, y Facebook + Hashtags. El resto de las pantallas cae en un texto
genérico en el front (no hace falta llenarlo a mano en cada una).

### 2. Probar en Gestión de roles
- [ ] Junto a "✏️ Editar" de CUALQUIER pantalla debe aparecer un botón ℹ️ nuevo (antes no
      estaba).
- [ ] En Modelos, Agregar modelo o Agregar producto → el popup debe explicar que el permiso se
      comparte entre las 3.
- [ ] En Rifas (cualquiera de las 3) y en Facebook/Hashtags → mismo tipo de aviso de grupo
      compartido.
- [ ] En cualquier otra pantalla (ej. Usuarios) → el popup debe mostrar el texto genérico
      ("Deja crear/editar/borrar en esta pantalla…"), no quedar vacío.
- [ ] En "Menús y submenús", al editar una pantalla → debe aparecer el campo de texto nuevo
      para cargar esta descripción a mano en pantallas futuras.

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

## Tarea A (cont. 4) — Auditoría completa de Tienda + grupo de menú propio (2026-09-05)

Pedido explícito del dueño: "todo lo que tiene la pantalla tiene que tener opción para
seleccionar dividido" — se revisó `tienda/buscar` botón por botón contra Modelos, sin dar nada
por sentado.

### 1. Script — `migration_menu_tienda.sql` ✅ ejecutado en QA y prod
`tienda/buscar` (submenu.id=38) tenía `menu_id` NULL — en Gestión de roles caía en "Sin grupo"
junto con Home/Clientes/Favoritos/Chat/QR/Login, difícil de encontrar pese a que en el sidebar
público aparece como su propio ícono de primer nivel. Crea el grupo "Tienda" (orden=0, antes de
"Catálogo") y reasigna esa pantalla ahí. No cambia que la pantalla siga siendo pública.

- [ ] Confirmar en Gestión de roles que "Tienda" ahora aparece como su propio grupo (no en "Sin
      grupo"), antes de "Catálogo".

### 2. Script — `migration_accion_palabras_clave_eliminar.sql` ✅ ejecutado en QA y prod
Categorías (palabras-clave) tenía 2 botones por fila (✏️ Editar, 🗑️ Eliminar) sin ningún
permiso puntual — "Editar" ya lo cubre el checkbox genérico de Escritura de esa pantalla, pero
"Eliminar" no se podía dar/quitar por separado. Crea la acción `eliminar` para `palabras-clave`
y se la da automáticamente a todo rol que ya tuviera Escritura ahí (preserva comportamiento).

- [ ] Con un rol sin la acción "Eliminar categoría": el botón 🗑️ no debe aparecer en Categorías,
      y `DELETE /v1/palabras-clave/delete` debe responder 403.
- [ ] Dándosela: el botón vuelve a aparecer y funciona.

### 3. Script — `migration_accion_tienda_escanear.sql` ✅ ejecutado en QA y prod
El escáner de código de barras (📷, 2 botones) en Tienda era incondicional para cualquier cuenta
logueada — mismo patrón que ya se hizo en Modelos. Crea la acción `escanear-codigo` para
`tienda/buscar`, dada por defecto solo a `ROLE_ADMIN`.

**Diferencia clave con Modelos:** Tienda es la vitrina pública — un visitante SIN cuenta sigue
viendo el escáner siempre (el front no lo condiciona a nadie anónimo). El permiso solo aplica a
cuentas CON sesión.

- [ ] Visitante sin sesión (incógnito): el escáner debe seguir apareciendo, sin cambios.
- [ ] Cuenta con sesión y SIN la acción "Escanear código de barras": los 2 botones de cámara
      (arriba en móvil, y junto al buscador) no deben aparecer.
- [ ] Dándosela: ambos vuelven a aparecer y funcionan.

### 4. Fix — `puedeActualizarVariante` checaba Ver en vez de Editar
Bug encontrado en la misma auditoría: el botón "Editar" de Tienda usaba `tienePantalla` (permiso
de Ver "Agregar producto") en vez de `tieneEscritura` (permiso de Editar) — un rol con Ver pero
sin Editar en "Agregar producto" veía el botón en Tienda y se topaba con un 403 al guardar. Es
un fix de código puro, sin script — ya viaja en el mismo commit del punto 3 del front.

- [ ] Con un rol con Ver pero SIN Editar en "Agregar producto": el botón "Editar" de Tienda NO
      debe aparecer (antes sí aparecía y fallaba al guardar).
- [ ] Dándole también Editar en "Agregar producto": el botón aparece y guarda sin 403.

### 5. Auditoría completa de la pantalla (checklist de referencia)

| Elemento | Permiso | ¿Ya estaba bien? |
|---|---|---|
| 8 checkboxes de filtro + fecha | `puedeFiltroXxx` (uno por cada uno) | Sí |
| Checkbox de selección + barra en lote | `puedeHabilitar` | Sí |
| Badge/atenuado "Deshabilitado" | `esVistaAdmin` | Sí |
| Botón Habilitar/Deshabilitar (individual) | `puedeHabilitar` | Sí |
| Botón Compartir imagen | `puedeCompartirImagen` | Sí |
| 4 filtros públicos (Talla/Color/Marca/Precio) | `puedeFiltroTalla/Color/Marca/Precio` | Sí |
| Escáner de código de barras (📷 x2) | `puedeEscanear` | Sí |
| ❤️ favoritos, paginación | — (no aplica, sin acción configurable) | Sí |
| Agregar/Quitar/Carrito (➕➖🛒, tarjeta + encabezado) | ~~sin permiso~~ → `puedeAgregarCarrito`/`puedeQuitarCarrito`/`puedeVerCarrito` | **No — corregido en punto 6** |

### 6. Script — `migration_accion_tienda_carrito.sql` (pendiente de ejecutar)
Último hallazgo de la auditoría (2026-09-05, reportado por el dueño con captura de la tarjeta):
los 3 botones de carrito de cada tarjeta (➕ Agregar, ➖ Quitar, 🛒 Carrito) y el ícono 🛒 del
encabezado eran los únicos elementos de la pantalla sin ningún permiso — se habían dejado afuera
por error al asumir que, al ser función de cliente, no necesitaban acción propia. El dueño ya
había pedido "TODO lo que tiene la pantalla" sin excepciones, así que se corrige.

Crea `agregar-carrito`, `quitar-carrito`, `ver-carrito` (categoría "Tarjeta de variante", orden
14-16) y renumera `habilitar`→17, `compartir-imagen`→18, `escanear-codigo`→19 para mantener los
bloques contiguos. 100% frontend (carrito es local, sin llamada al back) — sin cambios en
`SecurityConfig`, mismo criterio que `compartir-imagen`. Igual que escáner/filtros públicos: un
visitante SIN sesión los sigue viendo siempre; el permiso solo aplica a cuentas con sesión, dado
por defecto a `ROLE_ADMIN`.

- [x] Ejecutar `migration_accion_tienda_carrito.sql` en QA y prod. ✅ confirmado por el dueño.
- [ ] Visitante sin sesión (incógnito): los 3 botones + el ícono del encabezado deben seguir
      apareciendo, sin cambios.
- [ ] Cuenta con sesión y SIN alguna de las 3 acciones nuevas: el botón correspondiente no debe
      aparecer en la tarjeta (el ícono del encabezado se rige por "Ver carrito").
- [ ] Dándolas: vuelven a aparecer y funcionan.
- [ ] Confirmar en Gestión de roles que "Tarjeta de variante" sigue agrupada correctamente (5
      acciones: Agregar/Quitar/Ver carrito, Habilitar, Compartir imagen) y que "Buscador"
      (Escanear código) también se ve bien tras el renumerado.

---

## Menú "Envíos" (Zonas de entrega + Entregas por zona) — auditoría 2026-09-05

Pedido del dueño: "revisa bien todas las opciones porque tiene algunas". El menú "Envíos" tiene 2
pantallas — `lugares-entrega` (Zonas de entrega) y `entregas-zona` (Entregas por zona) — ninguna
de las 2 tenía permisos finos: la primera tenía todo su CRUD bajo un único Editar sin distinción,
y la segunda ni siquiera tenía pantalla propia en el sistema de permisos.

### 1. Script — `migration_accion_lugares_entrega_eliminar.sql` (pendiente de ejecutar)
"Zonas de entrega" tenía alta/edición/mapa de centro/anillos de cobro por distancia Y eliminar
todo bajo el mismo permiso de Escritura. Separa "Eliminar" en su propia acción puntual (mismo
patrón que Categorías/palabras-clave) — el resto (form completo + editor de anillos) se queda
bajo Escritura porque es un único flujo de "editar la zona", no tiene sentido partirlo más.
Preserva comportamiento: se la da a todo rol que ya tuviera Escritura ahí.

- [ ] Ejecutar en QA y prod.
- [ ] Rol con Escritura pero SIN la acción "Eliminar zona de entrega": el botón 🗑️ no debe
      aparecer, y `DELETE /v1/lugares-entrega/delete` debe responder 403.
- [ ] Rol SIN Escritura: no debe ver el formulario de alta/edición, ni el botón ✏️ Editar, ni el
      editor de anillos (aunque tenga la acción "Eliminar" — sigue viendo el 🗑️).
- [ ] Dándole Escritura: form + ✏️ Editar + anillos vuelven a aparecer y funcionan.

### 2. Script — `migration_submenu_entregas_zona.sql` (pendiente de ejecutar)
"Entregas por zona" se agregó el 2026-09-04 sin pasar por el sistema de permisos: sin fila en
`submenu`, sin `PantallaGuard` en el front (el link del navbar aparecía "de prestado" si el rol
tenía `lugares-entrega`, sin relación con el permiso real). El back sí exigía ROLE_ADMIN de
verdad, así que la seguridad real siempre estuvo cubierta — esto le da su propia pantalla en
Gestión de roles, coherente con lo que el back ya exigía.

Crea la fila en `submenu` (mismo grupo/menu_id que `lugares-entrega`, para que sigan apareciendo
juntos bajo "Envíos"), y da View + Escritura a `ROLE_ADMIN` (el único rol que hoy puede usarla de
verdad). El botón "✉️ Avisar a N cliente(s)" es la única acción de escritura de la pantalla — no
hay nada más que separar ahí (ver pedidos pendientes es el View general).

- [ ] Ejecutar en QA y prod.
- [ ] Rol SIN la pantalla "entregas-zona": el link "📦 Entregas por zona" no debe aparecer en el
      navbar (aunque tenga "lugares-entrega" — ya no comparten visibilidad), y navegar directo a
      `/entregas-zona` debe redirigir (PantallaGuard).
- [ ] Rol CON la pantalla pero SIN Escritura: debe ver la zona/pendientes, pero NO el formulario
      de fecha/hora/punto de encuentro ni el botón "✉️ Avisar".
- [ ] Dándole Escritura: el formulario y el botón aparecen y programan/avisan correctamente.
- [ ] Confirmar que "Zonas de entrega" y "Entregas por zona" aparecen juntas en el mismo grupo en
      Gestión de roles.

---

## Antes de fusionar cualquiera de las 2 a `dev`
- Aprobar cada rama por separado (no fusionar ambas juntas).
- Seguir el flujo normal de `CLAUDE.md`: `dev → qa → main`, nunca al revés.
