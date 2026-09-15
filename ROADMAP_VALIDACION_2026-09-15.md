# 📋 LO QUE FALTA — 2026-09-15

**Lo que ya validaste está en `VALIDADO_2026-09-15.md`.** Aquí solo queda lo pendiente.

> **Cómo leer cada prueba.** Pediste que dijera *con qué cuenta* se hace cada una y *qué esperar*.
> Todas las pruebas de aquí en adelante llevan estas cuatro líneas:
>
> | Línea | Qué dice |
> |---|---|
> | 👤 **Con qué cuenta** | Admin / Cliente / Visitante sin cuenta (ventana de incógnito) |
> | 📍 **Dónde** | La ruta exacta del menú |
> | 🔢 **Qué hacer** | Los pasos, en orden |
> | ✅ **Qué debe pasar** | El resultado correcto. Si pasa otra cosa, es bug |

### Las tres cuentas

| Cuenta | Qué es | Cómo entrar |
|---|---|---|
| 👑 **Admin** | Tu cuenta de dueño, ve todo el menú | Login normal |
| 🙋 **Cliente** | Una cuenta de cliente registrado (no admin) | Login con un usuario de prueba |
| 👻 **Visitante** | Sin sesión, nadie logueado | Ventana de incógnito (Ctrl+Shift+N) |

> Varias pruebas piden **las dos**: haces una cosa como Admin y confirmas el efecto como Cliente.
> Cuando sea así, lo dice en los pasos.

---

| # | Qué falta | Con qué cuenta | Por qué está parado |
|---|---|---|---|
| **1** | Botón "➕ Agregar gasto" | 👑 Admin | Ya corriste el SQL — falta confirmar que se ve |
| **2** | Decidir qué pasa si no recogen el pedido | 👑 Admin | Es tu decisión: A, B o C |
| **3** | Conversaciones del chatbot | 👑 Admin + 👻 Visitante | **Nuevo** — recién programado, sin probar |
| **4** | La hora del chat | 👑 Admin | **Te toca a ti**: necesito un dato tuyo (abajo) |
| **5** | Fechas (bug UTC) | 👑 Admin + 🙋 Cliente | Solo se puede probar **después de las 6 pm** |
| **6** | Tres arreglos ya subidos a QA | 👑 Admin + 👻 Visitante | Desplegados, falta que los veas |
| **7** | Repaso visual modo noche | 👑 Admin | Nadie lo ha recorrido |
| **8** | Celular | 👻 Visitante | Falta recorrerlo en un teléfono real |
| **9** | Subir a producción | — | Hasta que 1-8 estén limpios |

**Ya contestado, no hay que probar nada:** tu duda de los permisos en *Usuario update* → sección
**"Respuestas a tus preguntas"** al final. No es bug.

---

# 🟢 1 — EL BOTÓN "➕ AGREGAR GASTO"

Ya corriste `migration_accion_gastos.sql` en QA y en prod. Solo falta confirmar que surtió efecto.

👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Ventas** → **Gastos** → pestaña **Gastos**
🔢 **Qué hacer:**
1. Entrar a la pantalla.
2. Recargar con **Ctrl+Shift+R** (importante: los permisos se traen al entrar, si no recargas fuerte sigues viendo los de antes).

✅ **Qué debe pasar:**
- [ ] Se ve el botón **➕ Agregar gasto**
- [ ] En cada fila de la tabla se ven los íconos **✏️** y **🗑️**

> **Si NO se ven:** el SQL creó los permisos pero no están asignados a tu rol. Se arregla sin SQL:
> **Sistema → Gestión de roles** → tu rol → buscar la pantalla de Gastos → marcar las 3 acciones →
> guardar → volver a entrar a Gastos con Ctrl+Shift+R.

---

# ❓ 2 — TU DECISIÓN: ¿QUÉ PASA SI NO RECOGEN EL PEDIDO?

## Cómo funciona hoy

Una tarea automática corre **todos los días a las 8:00 am**, busca pedidos olvidados y los cancela.

| Cosa | Valor |
|---|---|
| A qué hora corre | 8:00 am, todos los días |
| Cuánto espera | 2 días después de la fecha de recogida |
| Qué estado busca | **Solo `Pendiente`** |

Al cancelar hace 4 cosas: devuelve el stock, avisa a quien tenía el producto en Favoritos, le manda
correo al cliente, y **le cuenta en contra para el score de rifas** (motivo `TIMEOUT`).

## ⚠️ Lo que tienes que decidir

**Los pedidos que TÚ levantas (`APARTADO`) nunca se cancelan solos.**

| Tipo | ¿Quién lo levanta? | ¿Se cancela solo a los 2 días? |
|---|---|---|
| `Pendiente` | El cliente, desde su cuenta | ✅ Sí |
| `APARTADO` | Tú, en Venta directa | ❌ **No, nunca** |
| `FIADO` | Tú, en Venta directa | ❌ No, nunca |
| `Entregado` | — | ❌ No (ya se entregó) |

| Opción | Qué implica | Qué esperar si la eliges |
|---|---|---|
| **A** — Dejarlo como está | Tú cancelas a mano | Nada cambia. Cero riesgo, más trabajo tuyo |
| **B** — Que APARTADO también se cancele | El stock se libera solo | Al cliente le llega correo de cancelado **y le cuenta en contra para rifas** |
| **C** — Cancelarlo sin castigar | Se libera el stock | Correo igual, pero **sin** pegarle al score de rifa. Lo más justo si el cliente te avisó |

**Dime A, B o C y lo programo.** No lo toco solo porque cambia inventario y manda correos a
clientes reales.

## Cómo probar lo de hoy sin esperar 2 días

👤 **Con qué cuenta:** 👑 Admin (y acceso a la base)
🔢 **Qué hacer:**
1. Levanta un pedido de prueba.
2. Mándalo al pasado en la base:
```sql
SELECT id, estado_pedido, fecha_recogida FROM pedido ORDER BY id DESC LIMIT 5;
UPDATE pedido SET fecha_recogida = CURDATE() - INTERVAL 3 DAY WHERE id = <id>;
```
3. Espera a las 8:00 am del día siguiente.

✅ **Qué debe pasar — con un pedido `Pendiente` (lo levanta el 🙋 Cliente desde la tienda):**
- [ ] El pedido quedó cancelado
- [ ] El stock del producto subió otra vez
- [ ] Al cliente le llegó correo de aviso
- [ ] En el log del back sale `Pedido X cancelado automáticamente`

✅ **Qué debe pasar — con un pedido `APARTADO` (lo levantas tú en Venta directa):**
- [ ] **No pasa nada** — sigue ahí, sin cancelar

> Ese contraste es justo el que te ayuda a decidir entre A, B y C.

- [ ] **Decidido:** opción A / B / C

---

# 🤖 3 — CONVERSACIONES DEL CHATBOT *(nuevo, sin probar)*

Me dijiste: *"no veo la respuesta del chatbot en mensaje directo... y además que se guarden las
conversaciones"*.

**Tenías razón, no se guardaba nada.** El chatbot contestaba y todo moría en el navegador del
visitante: no escribía en ninguna tabla, por eso no había forma de leerlo desde el admin.

**Ya quedó programado.** Ahora cada pregunta y cada respuesta del bot se guardan en las mismas
tablas del chat en vivo (`chat_sesion` / `chat_mensaje`), con remitente `BOT` para que se distinga
de lo que contesta una persona.

> ⚠️ **Solo guarda de aquí en adelante.** Las conversaciones viejas del bot no existen en ningún
> lado — nunca se escribieron. No se pueden recuperar.

### 3.1 — Que el bot guarde la conversación

👤 **Con qué cuenta:** 👻 Visitante (incógnito) primero, luego 👑 Admin
📍 **Dónde:** la tienda pública → burbuja del chatbot
🔢 **Qué hacer:**
1. **Como Visitante**, en incógnito: abre la tienda, abre el chatbot y escríbele 2 o 3 preguntas
   (por ejemplo "¿tienes bolsas?", "¿cuánto cuesta?").
2. Fíjate en la hora a la que escribiste.
3. **Como Admin**, en tu ventana normal: Menú → **Sistema** → **Chat**.

✅ **Qué debe pasar:**
- [ ] En la lista de conversaciones aparece una nueva que dice **"Visitante (chatbot)"**
- [ ] Al abrirla se ven **tus preguntas y las respuestas del bot**, en orden
- [ ] Las del bot salen con el ícono **🤖** y fondo distinto al de una respuesta tuya
- [ ] **La hora de cada mensaje es la hora real a la que escribiste** (esto también contesta el punto 4)

### 3.2 — Que no se parta la conversación al recargar

👤 **Con qué cuenta:** 👻 Visitante
🔢 **Qué hacer:**
1. En la misma ventana de incógnito, recarga la página (F5).
2. Escríbele otra pregunta al bot.
3. Vuelve a mirar como 👑 Admin.

✅ **Qué debe pasar:**
- [ ] El mensaje nuevo cayó **en la misma conversación**, no en una nueva

### 3.3 — Que no se mezcle con el chat en vivo

👤 **Con qué cuenta:** 👑 Admin
🔢 **Qué hacer:** mira la lista de conversaciones del chat.

✅ **Qué debe pasar:**
- [ ] Las del bot **no** aparecen como "esperando que las contestes" — el bot ya contestó
- [ ] Las conversaciones reales con clientes (chat en vivo) siguen saliendo igual que antes

---

# ⏰ 4 — LA HORA DEL CHAT: NECESITO UN DATO TUYO

Me dijiste: *"ahí es donde te digo que la hora estaba mal, no sé si ya lo solucionaste... ahí te
expliqué cómo lo quería pero ahí no me dijiste si sí así lo harías"*.

**Revisé la cadena completa y no encontré de dónde saldría una hora mal:**

| Eslabón | Qué hace | ¿Correcto? |
|---|---|---|
| El servidor | Guarda la hora de México (`TZ=America/Mexico_City`) | ✅ Verificado |
| Lo que manda al front | La hora tal cual, sin zona pegada | ✅ |
| Lo que muestra el navegador | La misma hora, sin recalcular | ✅ |

**Por eso no lo puedo arreglar a ciegas** — si toco algo sin saber qué viste, lo más probable es
que lo rompa al revés (lo mueva 6 horas para el otro lado).

👤 **Lo que necesito de ti:** 👑 Admin
1. Entra a Menú → **Sistema** → **Chat** y abre cualquier conversación.
2. Dime: **qué hora decía el mensaje** y **qué hora era en realidad** cuando se mandó.
   Con una captura basta.
3. **Repíteme cómo lo querías.** Eso quedó en una conversación anterior que ya no tengo, y no
   quiero adivinar. Si me dices cómo lo quieres, te digo de una si se puede y lo hago.

> La prueba **3.1** de arriba también sirve para esto: cuando escribas en el bot, apunta la hora
> real y compárala con la que muestra el admin. Si sale distinta, ahí está el dato que me falta.

---

# 🕕 5 — FECHAS CORRIDAS (solo después de las 6 pm)

**Para qué sirve:** antes de las 6 pm no se ve el bug. A partir de esa hora el sistema guardaba
todo con la fecha de **mañana**. Esto confirma que ya guarda **hoy**.

**Cómo prepararte:** o pruebas después de las 6 pm de verdad, o le cambias la hora a la
computadora (ponla en `20:00`, zona `América/Ciudad de México`) y recargas el navegador.

### 5.1 — Gastos
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Ventas** → **Gastos** → **➕ Agregar gasto**
🔢 **Qué hacer:** agregar un gasto, guardar, y filtrar por "hoy" en la misma pantalla.
✅ **Qué debe pasar:**
- [ ] La fecha que propone el formulario es **hoy**, no mañana
- [ ] El gasto aparece al filtrar por hoy

### 5.2 — Reportes
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Reportes** → **Reportes de ventas**
✅ **Qué debe pasar:**
- [ ] El filtro de día arranca en **hoy**
- [ ] El filtro de mes arranca en el **mes en curso**

### 5.3 — Créditos y Venta directa
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Ventas** → **Venta directa** (a crédito), luego **Ventas** → **Créditos / Abonos**
🔢 **Qué hacer:** hacer una venta a crédito con anticipo y abrir el detalle del abono.
✅ **Qué debe pasar:**
- [ ] La fecha del abono dice **hoy**

### 5.4 — Checkout de la tienda *(el más visible para el cliente)*
👤 **Con qué cuenta:** 🙋 **Cliente** — esta NO se hace como admin, es el flujo del comprador
📍 **Dónde:** Tienda → agregar al carrito → checkout → **"recoger en tienda"**
🔢 **Qué hacer:** completar el pedido como cliente. Luego revisarlo como 👑 Admin en Pedidos.
✅ **Qué debe pasar:**
- [ ] El calendario **deja elegir HOY** (antes hoy salía bloqueado)
- [ ] Ya como Admin, en Pedidos la fecha de ese pedido es **hoy**

### 5.5 — Flores eternas
👤 **Con qué cuenta:** 🙋 Cliente
📍 **Dónde:** Menú → **Flores eternas** → **Arma tu ramo** → completar pedido
✅ **Qué debe pasar:**
- [ ] La fecha del pedido es **hoy**

### 5.6 — Que no se haya roto Entregas por zona
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Envíos** → **Entregas por zona**
✅ **Qué debe pasar:**
- [ ] Los presets de rango (semana en curso / últimos N días) dan las mismas fechas que antes

> **Datos viejos:** los gastos y abonos guardados de noche **antes** de este arreglo quedaron con
> un día de más. No se corrigen solos — si son pocos, se editan a mano.

---

# 🧪 6 — TRES ARREGLOS YA SUBIDOS A QA

### 6.1 — Usuarios: ahora sí tiene tarjeta
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Sistema** → **Usuarios** → **Actualizar** en cualquier usuario
✅ **Qué debe pasar:**
- [ ] El formulario se ve dentro de una tarjeta con borde y sombra, como las demás pantallas
- [ ] Cambia de color al cambiar el tema (día/noche)
- [ ] **El mapa del local NO aparece por ningún lado** ← esto es lo que más fácil se rompe

### 6.2 — Ruleta pública: ya sale la descripción del premio
👤 **Con qué cuenta:** 👻 **Visitante** — tiene que ser sin sesión, es una pantalla pública
📍 **Dónde:** la URL de la ruleta pública, en ventana de incógnito
✅ **Qué debe pasar:**
- [ ] Debajo del nombre del premio sale su **descripción**
- [ ] Si el premio no tiene descripción escrita, salen **etiquetas** con talla / color / marca
- [ ] Se ve bien en celular

> La descripción sale de la del producto. Si quieres que diga algo específico para la rifa, hay
> que escribirla en el producto.

### 6.3 — Entregas por zona: cerrar el círculo
Ya confirmaste que aparecen los pedidos. Falta el correo:

👤 **Con qué cuenta:** 👑 Admin para programar, 🙋 Cliente para confirmar
📍 **Dónde:** Menú → **Envíos** → **Entregas por zona**
🔢 **Qué hacer:**
1. Como Admin: programa el viaje de una zona con un pedido de un cliente de prueba.
2. Revisa el correo de ese cliente.

✅ **Qué debe pasar:**
- [ ] Al cliente le llega el correo **con la fecha de entrega**

---

# 🌙 7 — REPASO VISUAL EN MODO NOCHE

**Para qué sirve:** 27 archivos de estilos pasaron de colores fijos a variables. El riesgo es que
alguno quedara con texto que no se lee.

👤 **Con qué cuenta:** 👑 Admin
🔢 **Qué hacer:** poner el sistema en modo noche y recorrer estas pantallas.

| Pantalla | Qué mirar |
|---|---|
| Chat en vivo / Chatbot | Burbujas y encabezado (**incluidas las nuevas del bot 🤖**) |
| Clientes (buscar, mis datos, mi perfil) | Campos y bordes |
| Venta directa / Detalle de pedido | Tablas: encabezado, filas alternas, fila al pasar el mouse |
| Dashboard y Reportes | Tarjetas y números |
| Gastos | Formulario y tabla |
| Rifas (agregar, mensual) | Tarjetas |
| Productos y Tienda | Tarjetas e imágenes |
| Promociones / Categorías | Autocompletado y chips |

✅ **Qué debe pasar:**
- [ ] En ningún lado hay texto que no se lea
- [ ] Las filas alternas de las tablas se distinguen entre sí
- [ ] Los estados (rojo de error, verde de éxito) siguen leyéndose

### 7.1 — La etiqueta "EN USO" de Personalización
👤 **Con qué cuenta:** 👑 Admin
📍 **Dónde:** Menú → **Sistema** → **Personalización**
✅ **Qué debe pasar:**
- [ ] "EN USO" se lee en modo día
- [ ] "EN USO" se lee en modo noche

---

# 📱 8 — CELULAR

### 8.1 — "Cómo llegar" en el login
👤 **Con qué cuenta:** 👻 Visitante (la pantalla de login, sin entrar)
✅ **Qué debe pasar:**
- [ ] Desde celular: abre la **app de Google Maps** con la ruta desde donde estás
- [ ] Desde computadora: abre la web en pestaña nueva
- [ ] El destino es tu local, no otro punto

### 8.2 — Login y registro sin desbordarse
👤 **Con qué cuenta:** 👻 Visitante
📍 **Dónde:** celular real (o Chrome en modo celular, 390px)
✅ **Qué debe pasar:**
- [ ] La miniatura del mapa no se sale de la pantalla
- [ ] La dirección larga se recorta con "…" en vez de romper el diseño
- [ ] **No hay scroll horizontal**

### 8.3 — Cuando no hay ubicación capturada
**Para qué sirve:** confirmar que si algún día la quitas, no queda un hueco feo ni truena.

👤 **Con qué cuenta:** 👑 Admin para quitarla, 👻 Visitante para ver el login
🔢 **Qué hacer:**
1. Como Admin: **Sistema → Negocio & Contactos** → **Quitar** y confirmar.
2. Como Visitante: ir al login y recargar con Ctrl+Shift+R.

✅ **Qué debe pasar:**
- [ ] No aparece nada: ni recuadro vacío, ni hueco, ni error en consola
- [ ] Los iconos de redes quedan pegados al formulario, como antes
- [ ] **Volver a capturarla** para dejarla puesta

---

# 🚀 9 — SUBIR A PRODUCCIÓN

**Solo cuando 1 a 8 estén limpios.**

### ⚠️ No se puede hacer `git merge qa` normal

Redes sociales sigue bloqueado (credenciales de prod sin definir + App Review de Meta sin aprobar).
Un merge completo lo arrastraría a producción.

**Hay que promover con `git cherry-pick`** de los commits puntuales, según la regla de `CLAUDE.md`.

### SQL en producción (`inventario_key`)

| SQL | ¿Ya está en prod? |
|---|---|
| `migration_negocio_ubicacion.sql` | ✅ Ya corrida — no volver a correrla |
| `migration_accion_gastos.sql` | ✅ Ya corrida (2026-09-15) |

**No queda ninguna migración pendiente.** El chatbot guarda en `chat_sesion` / `chat_mensaje`, que
ya existen — **no necesita SQL nuevo**.

---

# 💬 RESPUESTAS A TUS PREGUNTAS

## "Si en Usuario update modifico un permiso, ¿le perjudica a todos los que tienen ese rol?"

**No. Nadie más se ve afectado.** Revisé el código del back y son dos cosas distintas en la misma
pantalla:

| Lo que ves en Usuario update | Qué hace en realidad | ¿A quién afecta? |
|---|---|---|
| **Rol** (el desplegable) + 💾 Guardar permisos | Le cambia **cuál rol tiene** esa persona | Solo a esa persona |
| **🔓 Excepciones de pantalla** | Le suma o le quita una pantalla suelta, **encima** de su rol | Solo a esa persona |

**Ninguna de las dos edita el rol.** Cambiar el rol de Juan de "Vendedor" a "Supervisor" no le toca
nada al rol "Vendedor" ni a los demás vendedores. Y las excepciones se guardan en una tabla aparte
(`usuario_submenu`) que apunta a **ese usuario y esa pantalla**, no al rol.

Lo que ve cada quien al final se calcula así:

```
pantallas del rol  +  las que le diste de más  −  las que le quitaste
```

**Sobre tu propuesta** (*"en lugar de modificarle a ese usuario sería crear un nuevo rol y
agregárselo"*): las dos formas son válidas y el sistema ya soporta las dos. Cuál conviene:

| Situación | Qué conviene |
|---|---|
| **Una persona** necesita una pantalla de más, por excepción | **Excepción** — más rápido, no ensucia la lista de roles |
| **Varias personas** van a necesitar lo mismo | **Rol nuevo** — lo defines una vez y se lo pones a todos |
| Se te está llenando de excepciones sueltas | **Rol nuevo** — señal de que ya es un puesto, no una excepción |

Regla corta: **una persona → excepción. Un puesto → rol.**

> ⚠️ **Al probarlo:** después de cambiarle el rol o una excepción a alguien, esa persona tiene que
> **volver a entrar** (o recargar con Ctrl+Shift+R) para que le aparezca. Los permisos se traen al
> iniciar sesión, no se refrescan solos.

👤 **Si lo quieres comprobar:** 👑 Admin + 🙋 Cliente
1. Como Admin: dale una excepción de pantalla a un usuario de prueba.
2. Entra con **ese** usuario → debe ver la pantalla nueva.
3. Entra con **otro usuario del mismo rol** → **no** debe verla.

## "¿En qué parte o a qué te refieres con esto?" *(el SQL de echar para atrás)*

Es el bloque **"Si algún día hay que echarla para atrás"** de `VALIDADO_2026-09-15.md`.

No es algo que haya que hacer ni ahora ni nunca, salvo emergencia. Es el **deshacer** de la
migración de la ubicación del local: el SQL que corriste agregó 3 columnas (`direccion`, `latitud`,
`longitud`) a la tabla del negocio, y ese otro las quita.

**Para qué sirve tenerlo escrito:** si esa migración hubiera roto algo en producción, en vez de
investigar a las prisas se corre ese comando y la base queda como estaba. **Ya no aplica** —
la migración lleva días corriendo bien en QA y en prod. Déjalo ahí como respaldo.

---

# 📌 PARA DESPUÉS

| # | Qué | Estado |
|---|---|---|
| **P1** | **Filtros que no se pierdan** — que al recargar o salir del sistema los filtros de búsqueda del admin sigan puestos | **Lo pediste hoy.** Pendiente de definir: ¿los quieres guardados por usuario en la base (te siguen aunque cambies de computadora) o solo en el navegador (más simple, pero se pierden al cambiar de equipo)? Dime cuál y lo hago |
| **P2** | **Revisar que TODAS las cards estén homologadas** — no solo la de Usuario update | **Lo pediste hoy.** Pendiente de auditar |
| **P3** | **Generador de QR con varios destinos** | Pediste que fuera casi al final |
| **P4** | **Traspaso chatbot ↔ humano** — el bot atiende, el cliente pide humano, te llega correo, el bot se calla mientras contestas, si el cliente no responde vuelve el bot, y si se acaba el crédito del bot te avisa | **No existe**, es una feature completa. Hay que diseñarla aparte. **Nota:** ahora que el bot ya guarda sus conversaciones, esto quedó más cerca |
| **P5** | **Pantalla para leer chats viejos** — los datos ya están en la base, falta el buscador/historial | Solo falta la pantalla |
| **P6** | **Script de concursantes de prueba** | Dijiste que casi al final |
| **P7** | **¿Filtrar Entregas por zona por fecha de entrega?** — hoy filtra por la fecha en que se creó el pedido | Querías probarlo primero |
| **P8** | **¿Cancelar solo los APARTADO?** — ver punto 2, opciones A / B / C | Esperando tu decisión |
