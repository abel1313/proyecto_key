# ROADMAP DE VALIDACIÓN — Ubicación del local + Fix de fechas UTC + Presets día/noche
**Fecha:** 2026-09-15 | ✅ **Ya está todo desplegado en QA** (back y front) y la migración corrida.
Listo para empezar a probar.

---

## 📍 ESTADO DE LAS RAMAS

| Repo | `dev` | `qa` | `main` / `master` |
|---|---|---|---|
| **proyecto_key** (back) | `1d62263` | `16b1630` ✅ **al día con dev** | `e8fb1bd` — 9 commits atrás |
| **producto_venta_online** (front) | `58c77ed` | `25c3d6f` ✅ **al día con dev** | `4a8ecd6` — 11 commits atrás |

Lo que se subió a QA el 2026-09-15:

| Repo | Commit | Qué trae |
|---|---|---|
| back | `f87d479` | Taxonomía anotada en las entidades (solo comentarios) |
| back | `7539768` | Ubicación del local — **con su migración SQL** |
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

> ✅ **La migración ya está corrida en QA y en prod (2026-09-15).** Se puede probar directo.

### Test A1 — La sección aparece y el buscador funciona
**Dónde:** Menú → **Sistema** → **Negocio & Contactos** → sección **📍 Ubicación del local**

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
**Dónde:** entrar como **admin** → Menú → **Sistema** → **Usuarios** → botón **Actualizar** de cualquier usuario

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
Listo la ubicacion en ligin y en registrar ya quedo

# 🎯 B — FECHAS CORRIDAS UN DÍA (BUG UTC)

> **Cómo probarlo:** este bug **solo se ve a partir de las 6 de la tarde** hora de México. O pruebas
> después de esa hora, o le cambias la hora a la computadora (ponla en `20:00`, zona horaria
> `América/Ciudad de México`) y recargas el navegador.

> **Antes del fix:** a esa hora todo se guardaba con la fecha de **mañana**.

### Test B1 — Gastos
**Dónde:** Menú → **Ventas** → **Gastos** → **Agregar gasto**
Aqui no esta la opcion de agregar gastos, solo esta , dia rango fecha y buscar 
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
- Ya lo veo completo
- 

### Test B3 — Abonos y Venta directa
**Dónde:** Menú → **Ventas** → **Venta directa** (a crédito) y Menú → **Abonos**

1. Hacer una venta a crédito con anticipo después de las 6 pm.
2. Abrir el ticket / el detalle del abono.
3. **Validar:** la fecha del abono dice **hoy**, no mañana.

- ✅ `fechaPago` del abono = hoy
Ya veo las correcciones y si aparecen los pedidos que no aparecian antes, ahora tengo una duda para que la aclares
- Mira si agrego por ejemplo, en venta directa se pone una direccion para entregar, despues yo elijo la fecha de entrega
- tiene la que yo voy a entregar y en la card dle pedido tiene otra fecha pero no dice que fecha es y lo mismo para las demas
- entonces necesito que se explique esas fechas en las pantallas tanto las que voy a agregar como en los permisos que tiene
- un icono que s lo seleciconas te sale un modal con la explicacion, es decir si en una fecha o en un lugar donde se entrege
- aparezca par que es ese y donde va aparecer porque en pedido existe una opcion que dice donde entrega y tiene una fecha
- pero esa fecha no veo o esa info no se de donde llegue o en que momento la llene y ademas ahi se puede modificar por el cliente me parece hay que validar eso
- no se si entiendas con esto que te menciono con saber donde o que es eso por ejemplo para la fecha o donde aparezca fechas o aparezca
- input como donde se agregue lugares y saber a bueno esta es por esto o esto o esto y va aparecer ene ste lugar o es te o este
- y asdi con todo loq ue conocemos como ves?
- 
### Test B4 — Checkout de Tienda (el más visible para el cliente)
**Dónde:** Tienda → agregar algo al carrito → **checkout**, eligiendo **"recoger en tienda"**

1. **Validar:** el calendario de fecha de recogida **deja elegir HOY** (antes el mínimo saltaba a
   mañana y hoy salía bloqueado).
2. Completar el pedido.
3. **Validar:** en Pedidos, la `fechaPedido` es hoy.

- ✅ Se puede elegir hoy como fecha de recogida
- ✅ `fechaPedido` = hoy
aqui estoy perdido, hay que mostrar las pruebas que tengo que hacer y para que seria la prueba y varias pruebas por ejemplo
- en venta directa hay varias opciones y pues no sabia cual elegir, y ademas no dices si es como admin o como usuario normal
- es decir no se si este haciendo bien las pruebas
- y explicacion para que es cada prueba que es lo que busca esa prueba
porque hace falta validar que los pedidos regresen a tiempo es decir hago la prueba que es de hoy el pedido pero si no lo recoge
- entonces pues regresar el stock y ademas lo que falta y lo mismo como hago los pedidos
- 
- du

### Test B5 — Flores eternas
**Dónde:** **Flores eternas** → **Configurar ramo** → completar un pedido

- ✅ `fechaPedido` = hoy
Esto de las fechas hay que dejarlo pendiente porque no me deja validar la fecha ahorita hasta las 6 pm
### Test B6 — Que NO se haya roto lo que ya estaba bien
1. **Rifas** → Buscar rifa: el filtro de día debe seguir arrancando en hoy y la rifa de hoy debe salir. ya lo valide ok
2. **Rifas** → Agregar rifa: las rifas DIARIA de hoy deben seguir apareciendo como "anteriores". ya lo valide ok
3. **Entregas por zona:** los presets de rango (semana en curso / últimos N días) deben dar las
   mismas fechas que antes.
4. aqui, hice un pedido como admin a un cliente, pero solo puse su nombre el lugar de entrega y la fecha, pero al buscar en zona no aparece el pedido para enviar el correo para que sepan la fehca en la que iremos
5. 
4. **Mi perfil** y **Chat:** sin cambios (ahí la hora es un instante completo, se dejó en UTC a
   propósito). Esto porque se dejo a proposito?

- ✅ Rifas y Entregas por zona se comportan igual que antes
 esta esta fallando ya te dije porque arriba
> **Nota sobre datos viejos:** los gastos y abonos que se hayan guardado de noche **antes** de este
> fix quedaron con el día de más. No se migran solos — si hay pocos, se corrigen a mano.

---

# 🎯 C — PRESETS DÍA / NOCHE INDEPENDIENTES

**Dónde:** Menú → **Sistema** → **Personalización** → sección de diseños predefinidos

### Test C1 — Elegir distinto para cada modo
1. **Validar:** arriba se ve qué está activo ahora: **☀️ Día: [nombre]** y **🌙 Noche: [nombre]**.
2. En un preset, tocar **☀️ Usar de día**.
3. **Validar:** solo cambia el de día; el de noche se queda como estaba.
4. En **otro** preset distinto, tocar **🌙 Usar de noche**.
5. **Validar:** ahora día y noche tienen presets **distintos** y así lo dice el encabezado.
ya lo veo que se pueden elegir idependientes, ya lo valide
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
validado el login y el registrar sigue igual
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
Aqui solo estoy revisanod la rifa de boles, las otras rifas las voy a validar despues, y ademas vamos a vlaidar las otras rifas
4. peo necesito el script de concursantes ya listas para no estar agregando usuarios a cada rato, entonces quiero ya tener clientes registrado para pruebas peor eso casi hasta el final
ya valide la rifa de los boletos ya esta lista en cel y en pc, solo falta confirmar en prod que es donde ya hay mas boletos
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
Ya se ve el carrusel, solo falta vlaidar con mas fotos peor con 1 ya esta funcionando
- 
### Test E3 — Ruleta pública sin login (#65)
1. Abrir la URL de la ruleta pública **en una ventana de incógnito** (sin sesión).
2. **Validar:** carga sin pedir login, se ven los premios con imagen y la ruleta gira.
3. Probar en celular.

- ✅ Carga sin sesión
- ✅ Los premios traen imagen
- ✅ Gira y se ve bien en celular
Ya se esta mostrndo la imegen y el texto o descripcion, solo que hace falta poner una descripcion para que se vea porque ahi solo sale el nombre
- del producto o premio y nada mas
- 
---

# ✅ ORDEN PARA DESPLEGAR

1. ~~Correr la migración en la BD de QA~~ ✅ hecho el 2026-09-15 (y también en prod).
2. ~~Subir back y front de `dev` a `qa`~~ ✅ hecho el 2026-09-15 (el push disparó el deploy
   automático de los dos).
3. **← AQUÍ VAMOS:** capturar la ubicación del local en Configuración del negocio (Test A3).
4. Correr **todos** los tests de arriba.
5. Solo cuando QA esté limpio, promover a producción — **con `cherry-pick`, no con merge**, mientras
   redes sociales siga bloqueado. Recordar que en prod **la migración ya está corrida**.

---

# 🗄️ SQL QUE HAY QUE CORRER

## ✅ Ya no queda ninguna pendiente

| # | Archivo | Ambiente | Estado |
|---|---|---|---|
| 1 | `migration_negocio_ubicacion.sql` | QA (`inventario_key_qa`) **y** prod (`inventario_key`) | ✅ **Ejecutada el 2026-09-15** (confirmado por el usuario) |

> Todo lo demás (permisos finos, acciones de pantallas, rifas, flores, abonos…) ya se había
> ejecutado en QA y en prod el 2026-09-08. Se verificó contra git: entre `main` y `dev` **no hay
> ninguna otra migración nueva**.

### 📌 Ojo: prod tiene las columnas pero todavía NO el código

La migración se corrió también en producción, donde `main` sigue 7 commits atrás y no tiene nada de
la ubicación del local. **Eso no rompe nada** y está bien así: las 3 columnas son nullable, ningún
código de prod las toca, y el día que se promueva la feature la base ya está lista. Solo hay que
recordar, cuando toque, que **la migración ahí ya está hecha — no volver a correrla** (un segundo
`ALTER TABLE ... ADD COLUMN` sobre columnas que ya existen falla).

### ⚠️ Qué pasa si se despliega el back sin correrla

Los tres ambientes usan `ddl-auto: none`, así que Hibernate **no** toca el esquema ni lo valida al
arrancar: **el back levanta sin problema**. Lo que truena es en caliente, cuando algo consulta esas
columnas:

| Endpoint | Efecto |
|---|---|
| `GET /v1/negocio/contactos` (público) | Error SQL → el **login y el registro** no muestran ni el mapa ni los iconos de redes |
| `GET /v1/negocio/config` | Error SQL → **Configuración del negocio** no carga |
| `PUT /v1/negocio/ubicacion` | Error SQL → no se puede guardar la ubicación |

No hace falta reiniciar nada para arreglarlo: en cuanto corre el `ALTER TABLE`, los endpoints
responden bien de inmediato. Aun así, **lo limpio es correrla antes** del deploy para no dejar QA
a medias ni un minuto.

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

---

# 🔧 RESUELTO EL 2026-09-15 (segunda vuelta, tras tus comentarios)

## 1. ✅ Entregas por zona: el pedido no aparecía

**Causa real:** la consulta exigía `estadoPedido = 'Pendiente'`, y **Venta directa nunca guarda ese
estado**. Los de contado nacen `'Entregado'` y los de crédito con su propio tipo
(`'APARTADO'`/`'FIADO'`). O sea: **ningún** pedido levantado por ti podía salir en esa pantalla,
sin importar la zona ni la fecha.

`'Pendiente'` solo lo pone el checkout de la tienda (el cliente pidiendo desde su cuenta).

**Fix:** la consulta ahora acepta `IN ('Pendiente', 'APARTADO')` — porque en tu negocio
**APARTADO significa "esto se lo entrego después"**, que es justo lo que arma el viaje de zona.

Se quedan fuera a propósito `'Entregado'` (ya se entregó o se pagó y se llevó en el momento) y
`'cancelado'`. El cambio **solo amplía lo que ve esa pantalla**: no toca ventas, reportes,
dashboard ni el auto-cancelador (ese usa su propio método con `'Pendiente'` literal).

- Archivo: `IPedidoRepository.findPendientesDeZonaEnRango`
- **A probar:** levanta un pedido en Venta directa con zona + APARTADO, y confirma que ahora sí sale
  en Entregas por zona y que le llega el correo al programar el viaje.

## 2. ✅ Usuarios update no tenía card

La forma de tarjeta estaba escrita como `.split-form:not(.split-form--full) .form-inner`, y
"Actualizar usuario" **sí** lleva `--full`. Resultado: el panel ya venía pintado de `--card-bg` y el
formulario flotaba plano encima, sin borde ni sombra.

**Fix:** `--full` ahora tiene su propia tarjeta. A diferencia de Login y Registrar (públicas y
congeladas fuera de Personalización), esta es una pantalla de adentro, así que usa **los mismos
tokens de tarjeta que el resto del admin** y cambia con el tema que elijas. También se le subió el
ancho a 460px para que el padding de la tarjeta no apriete el formulario, que en esa pantalla es el
más largo de todos.

- Archivo: `add-usuarios.component.scss`

## 3. ✅ Ruleta pública: solo salía el nombre del premio

El estado público **ya traía** `descripcion`, `talla`, `color`, `marca`, `presentacion` y
`contenidoNeto`, pero la pantalla solo leía `nombreProducto` e `imagenUrl`. Nadie pintaba el resto.

**Fix:** debajo del nombre ahora sale la descripción, y como respaldo unas etiquetas con
talla/color/marca/presentación/contenido. Así, aunque el premio **no tenga descripción escrita**,
el visitante ya no ve solo el nombre pelón.

- Archivos: `ruleta-publica.component.{ts,html,scss}`
- **Ojo:** la descripción sale de la del **producto/variante**. Si quieres que diga algo específico
  para la rifa, hay que escribirla en la variante.

## 4. ✅ Log del chat que mentía

El log decía *"sesiones cerradas por inactividad (>30 min)"* pero el corte **siempre fue de 5
minutos**. Se sacó a una constante y el log ahora dice el número real.

---

# ❓ RESPUESTAS A TUS PREGUNTAS

## "¿Por qué Mi perfil y Chat se dejaron en UTC a propósito?"

Porque ahí la fecha **no es una fecha de calendario, es un instante** — el momento exacto en que
pasó algo:

| Campo | Qué guarda |
|---|---|
| `fechaAceptoPrivacidad` (Mi perfil) | El instante en que el cliente aceptó el aviso |
| `fechaInicio` / `ultimaActividad` (Chat) | Cuándo arrancó la conversación y el último mensaje |

Para un instante, UTC es lo **correcto**: es un punto en la línea del tiempo, sin ambigüedad, y el
navegador lo convierte a la hora local de quien lo lee. Si alguien abre el sistema desde otro huso,
sigue viendo la hora bien.

El bug era otra cosa: **recortar** un instante UTC (`.slice(0,10)`) para usarlo como si fuera una
fecha de calendario. Ahí sí se corría el día. Un gasto del día 15 es del 15 aunque lo captures a las
11 de la noche; el momento en que aceptaste el aviso de privacidad no es "un día", es un reloj.

Por eso se arregló lo primero y no lo segundo.

## "El chat en vivo, ¿dónde se guarda? ¿Solo se mantiene ese día?"

**Se guarda permanentemente en la base de datos, y nada lo borra.** Ya está todo ahí, no hay que
construir nada para conservarlo:

| Tabla | Qué guarda |
|---|---|
| `chat_sesion` | Una fila por conversación: quién, desde qué IP, cuándo empezó, última actividad, estado |
| `chat_mensaje` | Una fila por mensaje: de qué sesión, quién lo mandó, el texto completo y la hora |

Revisé las 9 tareas programadas del sistema: **ninguna toca el chat**. La única tarea de chat
(`ChatSesionScheduler`, cada 5 minutos) solo marca como `CERRADA` la sesión que lleva 5 minutos sin
actividad — **cambia el estado, no borra los mensajes**.

O sea: el historial completo de todas las conversaciones ya existe desde siempre. Lo que falta no
es guardarlo, es una **pantalla para leerlo** (hoy `obtenerSesionesRecientes()` solo trae las
últimas 24 horas, aunque en la base esté todo).

---

# 📌 ANOTADO PARA DESPUÉS (no se tocó nada todavía)

## P1 — Generador de QR con varios destinos
Pedido tuyo: poder generar un QR con los datos que necesites (Facebook, Instagram, URL…), que cada
QR se guarde **identificado** para saber qué trae, poder elegir uno ya hecho, y agregar más.
Queda para **cuando terminemos los tests**, tal como pediste.

## P2 — Traspaso chatbot ↔ humano
Pedido tuyo, y **hoy no existe**: el chat actual es solo ACTIVA/CERRADA con mensajes de usuario y
admin. El flujo que describes (el bot atiende → el cliente pide humano → correo al admin → mientras
tú contestas el bot se calla → si el cliente no responde en X tiempo vuelve el bot → si se acaba el
crédito del bot, correo al admin) es **una feature nueva completa**, con estados, temporizadores y
control de consumo. Hay que diseñarla aparte.

## P3 — Pantalla para leer conversaciones viejas
Sale de la respuesta de arriba: los datos ya están todos, pero solo se pueden consultar las últimas
24 horas. Falta el buscador/historial.

## P4 — Script de concursantes de prueba
Pedido tuyo: dejar clientes registrados ya cargados para no andar creando usuarios en cada prueba.
Dijiste que casi hasta el final.

## P5 — Decisión pendiente: fecha del filtro de Entregas por zona
El filtro usa la fecha en que se **creó** el pedido (`fechaPedido`), no la de entrega (que se guarda
en `fechaRecogida`). Con el fix del punto 1 tu pedido ya debería salir (se creó hoy, cae en el rango
de esta semana). Si aun así te acomoda más filtrar por fecha de entrega, se cambia — **avísame
después de probarlo**, para no cambiar dos cosas a la vez y no saber cuál fue.
