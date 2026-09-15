# ROADMAP DE VALIDACIÓN — Ubicación del local + Fix de fechas UTC + Presets día/noche
**Fecha:** 2026-09-15 | **Todo está en `dev`** (back y front). Nada se ha subido a `qa` todavía.

---

## 📍 ESTADO DE LAS RAMAS

| Repo | `dev` | `qa` | `main` / `master` |
|---|---|---|---|
| **proyecto_key** (back) | `7539768` ✅ al día | `5a4fa81` — 2 commits atrás | `e8fb1bd` — 6 atrás |
| **producto_venta_online** (front) | `58c77ed` ✅ al día | `54fe432` — 2 commits atrás | `4a8ecd6` — 6 atrás |

**Lo que falta subir de `dev` a `qa`:**

| Repo | Commit | Qué trae |
|---|---|---|
| back | `f87d479` | Taxonomía anotada en las entidades (solo comentarios) |
| back | `7539768` | Ubicación del local — **trae migración SQL** |
| front | `bb325a8` | Fix de fechas corridas un día |
| front | `58c77ed` | Mapa del local en login y registro |

> ⚠️ **`main` sigue sin poder recibir un `git merge qa` normal** — redes sociales continúa bloqueado
> por credenciales de prod y App Review de Meta. Lo que tenga que subir a producción va con
> `git cherry-pick` de commits puntuales, según la regla de `CLAUDE.md`.

---

## 📋 RESUMEN DE CAMBIOS A VALIDAR

| # | Área | Qué cambió | Riesgo | Prioridad |
|---|---|---|---|---|
| **A** | **Ubicación del local** | Nueva sección en admin + mapa en login y registro | Alto (toca pantallas públicas) | 🔴 CRÍTICO |
| **B** | **Fechas corridas (UTC)** | 11 sitios en 8 pantallas; cambia lo que se GUARDA | Alto (datos) | 🔴 CRÍTICO |
| **C** | **Presets día/noche** | Se puede elegir un estilo distinto para cada modo | Medio | 🟡 ALTO |
| **D** | **Tokenización modo noche** | 27 archivos SCSS pasaron a variables | Medio (visual) | 🟡 ALTO |
| **E** | **Rifas — pendientes viejos** | Carrusel, búsqueda de premios, ruleta pública | Medio | 🟡 ALTO |
| **F** | **Taxonomía en entidades** | Solo Javadoc, no cambia comportamiento | Nulo | 🟢 No requiere prueba |

---

# 🎯 A — UBICACIÓN DEL LOCAL

> **Antes de nada:** correr la migración (ver la sección de SQL al final). Sin ella el back **no
> arranca**.

### Test A1 — La sección aparece y el buscador funciona
**Dónde:** Menú → **Administración** → **Configuración del negocio** → sección **📍 Ubicación del local**

1. Entrar a la pantalla. Debe aparecer la sección nueva **entre** "Contactos" y "Alertas de stock bajo".
2. El botón **Guardar ubicación** debe estar gris, y abajo un texto diciendo qué falta.
3. Escribir en el buscador del mapa el nombre de tu zona (ej. `Tejupilco centro`) y darle 🔍.
4. **Validar:** el mapa se mueve a esa zona. **El pin NO se pone solo** — es a propósito (buscar no
   es confirmar).
5. Tocar el mapa sobre tu local → aparece el pin y el texto cambia a **"✅ Ubicación marcada"**.
6. Arrastrar el pin unos metros → debe seguirte sin que el mapa salte ni haga zoom solo.

- ✅ El buscador mueve el mapa
- ✅ El pin se pone al tocar, no al buscar
- ✅ Arrastrar el pin no recentra el mapa de golpe

### Test A2 — "Usar mi ubicación" (lo práctico)
**Dónde:** misma sección, botón **📡 Usar mi ubicación**

1. Desde el **celular, parado en el local**, tocar el botón.
2. El navegador pide permiso de ubicación → aceptar.
3. **Validar:** el pin cae en donde estás y el mapa hace zoom ahí.
4. Repetir **negando** el permiso → no debe tronar; se debe poder seguir marcando a mano.

- ✅ Con permiso: marca tu posición
- ✅ Sin permiso: no truena, el mapa sigue usable

### Test A3 — Guardar y que persista
1. Escribir la dirección en **Dirección que verá el cliente** (ej. `Av. Hidalgo 24, Centro`).
2. Con el pin puesto, el botón **Guardar ubicación** debe habilitarse y el texto de "qué falta"
   desaparecer.
3. Guardar → aviso verde *"¡Ubicación guardada!"*.
4. **Salir de la pantalla y volver a entrar.**
5. **Validar:** la dirección sigue escrita y el mapa abre **centrado en tu local con el pin puesto**
   (no en el centro genérico).

- ✅ Persiste después de salir y volver
- ✅ El mapa abre en tu punto, no en el genérico

### Test A4 — Se ve en el LOGIN
**Dónde:** cerrar sesión → pantalla de **inicio de sesión**

1. **Recargar con Ctrl+Shift+R** (para saltarse la caché de nginx).
2. **Validar:** debajo del link "Regístrate aquí" y **arriba** de los iconos de redes aparece la
   miniatura del mapa con tu local marcado.
3. Debe verse: el mapa real, el pin, la franja oscura abajo con **"Visítanos en el local"** + tu
   dirección, y el botón blanco **"Cómo llegar ↗"**.
4. Arriba a la derecha debe verse el crédito **© OpenStreetMap** (lo pide la licencia, no quitarlo).

- ✅ Las teselas del mapa cargan (no se queda el recuadro gris)
- ✅ El pin cae sobre tu local
- ✅ La dirección se lee completa y no se encima con el botón

### Test A5 — Se ve en el REGISTRO
**Dónde:** login → **Regístrate aquí**

1. **Validar:** el mismo bloque aparece, con el mismo aspecto, arriba del link "¿Ya tienes cuenta?".
2. Comparar contra el login: debe verse **idéntico**.

- ✅ Mismo bloque, mismo aspecto que en el login

### Test A6 — ⚠️ El caso que más fácil se rompe: "Actualizar usuario"
**Dónde:** entrar como **admin** → Menú → **Usuarios** → botón **Actualizar** de cualquier usuario

1. **Validar:** en esa pantalla el mapa **NO debe aparecer por ningún lado**.

> Es la misma pantalla que el registro (`add-usuarios`), reusada. Si aquí sale el mapa, la bandera
> `esActualizar` no está haciendo su trabajo.

- ✅ En "Actualizar usuario" NO sale el mapa

### Test A7 — "Cómo llegar" traza la ruta de verdad
1. Desde el **celular**, en el login, tocar **Cómo llegar**.
2. **Validar:** abre la **app de Google Maps** con la ruta **desde donde estás** hasta el local.
3. Repetir desde **computadora** → debe abrir la web de Google Maps, en pestaña nueva.

- ✅ En celular abre la app con la ruta trazada
- ✅ En compu abre la web
- ✅ El destino es tu local, no otro punto

### Test A8 — Sin ubicación capturada (estado normal, no es error)
1. En Configuración del negocio, tocar **Quitar** y confirmar.
2. Ir al login y al registro y recargar con Ctrl+Shift+R.
3. **Validar:** simplemente **no aparece nada** — ni recuadro vacío, ni hueco, ni error en consola.
   Los iconos de redes deben quedar pegados al formulario como estaban antes.
4. Volver a capturarla para dejarla puesta.

- ✅ Sin ubicación no se pinta nada ni queda hueco
- ✅ Sin errores en la consola del navegador

### Test A9 — Celular
1. Abrir login y registro en un celular real (o Chrome en modo celular, 390px de ancho).
2. **Validar:** la miniatura no se desborda, la dirección se recorta con "…" en vez de romper el
   diseño, y el botón "Cómo llegar" no se encima con el texto.
3. **No debe haber scroll horizontal** en ninguna de las dos pantallas.

- ✅ Sin scroll horizontal
- ✅ Dirección larga se recorta, no rompe

---

# 🎯 B — FECHAS CORRIDAS UN DÍA (BUG UTC)

> **Cómo probarlo:** este bug **solo se ve a partir de las 6 de la tarde** hora de México. O pruebas
> después de esa hora, o le cambias la hora a la computadora (ponla en `20:00`, zona horaria
> `América/Ciudad de México`) y recargas el navegador.

> **Antes del fix:** a esa hora todo se guardaba con la fecha de **mañana**.

### Test B1 — Gastos
**Dónde:** Menú → **Gastos** → **Agregar gasto**

1. Con la hora puesta después de las 6 pm, agregar un gasto.
2. **Validar:** el campo de fecha propone **HOY**, no mañana.
3. Guardar, ir a **Gastos → Buscar** y filtrar por "hoy".
4. **Validar:** el gasto recién creado **aparece** en la lista.

- ✅ La fecha propuesta es hoy
- ✅ El gasto aparece al filtrar por hoy

### Test B2 — Reportes
**Dónde:** Menú → **Reportes**

1. **Validar:** el filtro de día arranca en **hoy** y el de mes en el **mes en curso**.
2. Los números que salen deben corresponder al periodo correcto.

- ✅ Filtro de día = hoy
- ✅ Filtro de mes = mes actual

### Test B3 — Abonos y Venta directa
**Dónde:** Menú → **Ventas** → **Venta directa** (a crédito) y Menú → **Abonos**

1. Hacer una venta a crédito con anticipo después de las 6 pm.
2. Abrir el ticket / el detalle del abono.
3. **Validar:** la fecha del abono dice **hoy**, no mañana.

- ✅ `fechaPago` del abono = hoy

### Test B4 — Checkout de Tienda (el más visible para el cliente)
**Dónde:** Tienda → agregar algo al carrito → **checkout**, eligiendo **"recoger en tienda"**

1. **Validar:** el calendario de fecha de recogida **deja elegir HOY** (antes el mínimo saltaba a
   mañana y hoy salía bloqueado).
2. Completar el pedido.
3. **Validar:** en Pedidos, la `fechaPedido` es hoy.

- ✅ Se puede elegir hoy como fecha de recogida
- ✅ `fechaPedido` = hoy

### Test B5 — Flores eternas
**Dónde:** **Flores eternas** → **Configurar ramo** → completar un pedido

- ✅ `fechaPedido` = hoy

### Test B6 — Que NO se haya roto lo que ya estaba bien
1. **Rifas** → Buscar rifa: el filtro de día debe seguir arrancando en hoy y la rifa de hoy debe salir.
2. **Rifas** → Agregar rifa: las rifas DIARIA de hoy deben seguir apareciendo como "anteriores".
3. **Entregas por zona:** los presets de rango (semana en curso / últimos N días) deben dar las
   mismas fechas que antes.
4. **Mi perfil** y **Chat:** sin cambios (ahí la hora es un instante completo, se dejó en UTC a
   propósito).

- ✅ Rifas y Entregas por zona se comportan igual que antes

> **Nota sobre datos viejos:** los gastos y abonos que se hayan guardado de noche **antes** de este
> fix quedaron con el día de más. No se migran solos — si hay pocos, se corrigen a mano.

---

# 🎯 C — PRESETS DÍA / NOCHE INDEPENDIENTES

**Dónde:** Menú → **Administración** → **Personalización** → sección de diseños predefinidos

### Test C1 — Elegir distinto para cada modo
1. **Validar:** arriba se ve qué está activo ahora: **☀️ Día: [nombre]** y **🌙 Noche: [nombre]**.
2. En un preset, tocar **☀️ Usar de día**.
3. **Validar:** solo cambia el de día; el de noche se queda como estaba.
4. En **otro** preset distinto, tocar **🌙 Usar de noche**.
5. **Validar:** ahora día y noche tienen presets **distintos** y así lo dice el encabezado.

- ✅ Se pueden tener dos presets distintos a la vez
- ✅ Elegir uno no pisa el otro

### Test C2 — La etiqueta "EN USO" se lee en los dos modos
1. Con el sistema en **modo día**, ver la etiqueta **EN USO** del preset activo.
2. Cambiar a **modo noche** con el botón de la misma sección.
3. **Validar:** la etiqueta **se sigue leyendo** (fue un bug: salía texto casi blanco sobre fondo
   blanco).

- ✅ "EN USO" legible en día
- ✅ "EN USO" legible en noche

### Test C3 — Se aplica a todo el sistema MENOS login y registro
1. Elegir un preset con colores muy distintos y guardarlo.
2. Recorrer: Dashboard, Productos, Pedidos, Ventas, Clientes, Rifas, Flores.
3. **Validar:** todas cambiaron.
4. Cerrar sesión e ir a **login** y a **registro**.
5. **Validar:** siguen **igual que siempre** (azul/morado), sin importar el preset elegido. Es a
   propósito.

- ✅ Todo el sistema cambia
- ✅ Login y registro NO cambian

---

# 🎯 D — TOKENIZACIÓN MODO NOCHE (repaso visual)

27 archivos SCSS pasaron de colores fijos a variables. El riesgo es que algo quedara con poco
contraste. **Poner el sistema en modo noche** y recorrer:

| Pantalla | Qué mirar |
|---|---|
| Chatbot | Burbujas, degradado del encabezado |
| Chat admin / Chat usuario | Fondo de mensajes, campo de escribir |
| Clientes (agregar, buscar, mis datos, mi perfil) | Campos de formulario, bordes |
| Buscar venta / Detalle de pedido | Tablas (encabezado, filas alternas, fila al pasar el mouse) |
| Dashboard y Reportes | Tarjetas y números |
| Gastos (agregar y buscar) | Formulario y tabla |
| Rifas (agregar, mes) | Tarjetas |
| Productos (listado y detalle) | Tarjetas e imágenes |
| Promociones / Palabras clave | Autocompletado y chips |

- ✅ En ningún lado hay texto que no se lea
- ✅ Las filas alternas de las tablas se distinguen entre sí
- ✅ Los estados (rojo de error, verde de éxito) siguen leyéndose

---

# 🎯 E — RIFAS: PENDIENTES QUE VIENEN DE ANTES

### Test E1 — Carrusel de premios (#63, #66)
1. Abrir una rifa con **varios premios con imagen**.
2. **Validar:** el carrusel muestra todas las imágenes, no sale vacío.
3. Debe funcionar el avance y el responsive en celular.

> Contexto: el detalle descartaba imágenes huérfanas y dejaba el carrusel vacío. El fix ya está,
> falta confirmarlo con datos reales.

- ✅ El carrusel no sale vacío
- ✅ Avanza y se ve bien en celular

### Test E2 — Búsqueda de premios e invalidación de caché (#64)
1. Buscar un premio para agregarlo a una rifa.
2. **Agregar** un premio nuevo y volver a buscar.
3. **Validar:** el nuevo aparece **sin tener que recargar**.
4. **Eliminar** uno y volver a buscar → debe desaparecer.

- ✅ Agregar se refleja sin recargar
- ✅ Eliminar se refleja sin recargar

### Test E3 — Ruleta pública sin login (#65)
1. Abrir la URL de la ruleta pública **en una ventana de incógnito** (sin sesión).
2. **Validar:** carga sin pedir login, se ven los premios con imagen y la ruleta gira.
3. Probar en celular.

- ✅ Carga sin sesión
- ✅ Los premios traen imagen
- ✅ Gira y se ve bien en celular

---

# ✅ ORDEN PARA DESPLEGAR

1. **Correr la migración en la BD de QA** (`inventario_key_qa`) — ver la sección de abajo.
2. Subir back y front de `dev` a `qa`:
   ```bash
   # en cada repo
   git checkout qa && git pull origin qa
   git merge dev --no-ff -m "Merge dev → qa: ubicación del local + fix de fechas UTC"
   git push origin qa
   ```
   El push a `qa` dispara el deploy automático (~1-2 min).
3. Capturar la ubicación del local en Configuración del negocio (Test A3).
4. Correr **todos** los tests de arriba.
5. Solo cuando QA esté limpio, promover a producción — **con `cherry-pick`, no con merge**, mientras
   redes sociales siga bloqueado.

---

# 🗄️ SQL QUE HAY QUE CORRER

## Pendiente: **1 migración**

| # | Archivo | Ambiente | Estado |
|---|---|---|---|
| 1 | `migration_negocio_ubicacion.sql` | QA (`inventario_key_qa`) y luego prod (`inventario_key`) | ❌ **Sin correr** |

> Todo lo demás (permisos finos, acciones de pantallas, rifas, flores, abonos…) ya se ejecutó en QA
> y en prod el 2026-09-08. Se verificó contra git: entre `main` y `dev` **no hay ninguna otra
> migración nueva**.

### ⚠️ Correrla ANTES de desplegar el back

El back arranca con JPA validando contra la tabla: si las columnas no existen, **truena al
levantar**.

### El SQL

Archivo: `src/main/resources/static/migration_negocio_ubicacion.sql`

```sql
ALTER TABLE configuracion_negocio
    ADD COLUMN direccion VARCHAR(255) NULL,
    ADD COLUMN latitud   DOUBLE       NULL,
    ADD COLUMN longitud  DOUBLE       NULL;
```

### Cómo correrla

```bash
# QA
mysql -u <usuario> -p inventario_key_qa < src/main/resources/static/migration_negocio_ubicacion.sql

# Producción (solo después de validar QA)
mysql -u <usuario> -p inventario_key < src/main/resources/static/migration_negocio_ubicacion.sql
```

### Comprobar que quedó

```sql
DESCRIBE configuracion_negocio;
-- deben aparecer: direccion (varchar 255, NULL), latitud (double, NULL), longitud (double, NULL)
```

Las 3 quedan en `NULL` a propósito: mientras no captures la ubicación desde la pantalla, el login y
el registro no muestran nada. **No se inventa un punto por defecto** — marcaría un local que no es
el tuyo.

### Si hay que echarla para atrás

```sql
ALTER TABLE configuracion_negocio
    DROP COLUMN direccion,
    DROP COLUMN latitud,
    DROP COLUMN longitud;
```

Solo se pierde la ubicación capturada; no toca ningún otro dato de la tabla.
