# 📋 LO QUE FALTA — 2026-09-15

**Lo que ya validaste está en `VALIDADO_2026-09-15.md`.** Aquí solo queda lo pendiente.

Cada prueba dice **para qué sirve**, **qué hacer** y **qué debe pasar**. Si lo que pasa es lo que
dice, tachas la línea y seguimos.

| # | Qué falta | Por qué está parado |
|---|---|---|
| **1** | Correr 1 SQL en QA | Sin eso no te sale el botón "➕ Agregar gasto" |
| **2** | Aclarar qué pasa si no recogen el pedido | Es tu duda — respondida abajo, falta que decidas |
| **3** | Fechas (bug UTC) | Solo se puede probar **después de las 6 pm** |
| **4** | Probar 3 arreglos recién subidos a QA | Ya están desplegados, falta que los veas |
| **5** | Repaso visual modo noche | Nadie lo ha recorrido |
| **6** | Subir a producción | Hasta que 1-5 estén limpios |

---

# 🔴 1 — FALTA CORRER UN SQL EN QA

## Por qué no ves "➕ Agregar gasto"

Me dijiste: *"Aquí no está la opción de agregar gastos, solo está día, rango fecha y buscar"*.

**No es un bug del código.** El botón existe y está bien puesto, pero vive detrás de un permiso:

```html
*ngIf="authService.tieneAccion('gastos/buscar', 'agregar-gasto')"
```

Ese permiso (`agregar-gasto`) se crea con una migración del **2026-09-08** que **nunca se corrió en
QA**. Como el permiso no existe en la base, el sistema responde "no tienes esa acción" y esconde el
botón — aunque seas admin.

Lo mismo pasa con **✏️ Editar** y **🗑️ Eliminar** de cada fila: son los otros dos permisos del mismo
archivo. Si no los ves tampoco, es la misma causa.

## El SQL que hay que correr

```bash
mysql -u <usuario> -p inventario_key_qa < src/main/resources/static/migration_accion_gastos.sql
```

Crea 3 permisos (`agregar-gasto`, `editar-gasto`, `eliminar-gasto`) y **se los da a todos los roles
que ya pueden ver la pantalla de Gastos** — o sea, deja las cosas como estaban antes de que los
botones fueran permisos. Es segura de correr dos veces: cada `INSERT` lleva su
`NOT EXISTS`, así que si ya estuviera, no duplica nada.

## Comprobar que quedó

```sql
SELECT a.clave, a.etiqueta FROM accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
WHERE s.ruta = 'gastos/buscar' ORDER BY a.orden;
```

Deben salir las 3 filas. Después, **recarga la pantalla con Ctrl+Shift+R** y el botón ➕ aparece.

> **Si las 3 filas YA salían antes de correr nada**, entonces el permiso sí existe y lo que falta es
> asignárselo a tu rol. En ese caso no es SQL: se arregla desde
> **Sistema → Gestión de roles**, buscando la pantalla de Gastos y marcando las 3 acciones.
> Dime cuál de los dos casos te tocó y seguimos por ahí.

> ⚠️ **Ojo con producción:** esta migración tampoco está corrida ahí. Cuando promuevas, hay que
> correrla también en `inventario_key` o el botón seguirá escondido en prod.

- [ ] SQL corrido en QA
- [ ] El botón ➕ Agregar gasto ya aparece
- [ ] Los íconos ✏️ y 🗑️ de cada fila también aparecen

---

# ❓ 2 — TU DUDA: "¿Y si no recogen el pedido?"

Me preguntaste: *"hace falta validar que los pedidos regresen a tiempo, es decir, hago la prueba que
es de hoy el pedido pero si no lo recoge"*.

## Cómo funciona hoy (esto ya existe, no es nuevo)

Hay una tarea automática que corre **todos los días a las 8:00 de la mañana**. Busca pedidos
olvidados y los cancela sola.

| Cosa | Valor |
|---|---|
| A qué hora corre | **8:00 am**, todos los días |
| Cuánto espera | **2 días** después de la fecha de recogida |
| Qué estado busca | **Solo `Pendiente`** |

Cuando cancela un pedido, hace 4 cosas:

1. **Devuelve el stock** — el producto vuelve a estar disponible para vender
2. **Avisa a Favoritos** — si el producto estaba en 0 y vuelve a haber, le llega correo a quien lo
   tenía marcado
3. **Le manda correo al cliente** avisándole (solo si el cliente tiene activado recibir correos)
4. **Le cuenta en contra para las rifas** — se guarda con motivo `TIMEOUT`, y el score de rifa
   penaliza a quien aparta y no recoge

## ⚠️ LO IMPORTANTE QUE TIENES QUE DECIDIR

**Los pedidos que TÚ levantas (APARTADO) nunca se cancelan solos.**

La tarea solo mira `Pendiente`, que es el estado del checkout del cliente. Tus APARTADO se quedan
ahí para siempre hasta que tú los entregues o los canceles a mano.

| Tipo de pedido | ¿Quién lo levanta? | ¿Se cancela solo a los 2 días? |
|---|---|---|
| `Pendiente` | El cliente desde su cuenta | ✅ Sí |
| `APARTADO` | Tú, en Venta directa | ❌ **No, nunca** |
| `FIADO` | Tú, en Venta directa | ❌ No, nunca |
| `Entregado` | — | ❌ No (ya se entregó) |

**La decisión es tuya:** ¿quieres que los APARTADO que nadie recogió también se cancelen solos y el
producto vuelva al inventario? Hay 3 caminos:

| Opción | Qué implica |
|---|---|
| **A** — Dejarlo como está | Tú cancelas a mano lo que se quedó. Cero riesgo, más trabajo tuyo. |
| **B** — Que APARTADO también se cancele a los N días | El stock se libera solo. **Ojo:** al cliente le llega correo de cancelado y le cuenta en contra para rifas. |
| **C** — Cancelarlo pero sin castigar | Se libera el stock, pero con otro motivo que no le pegue al score de rifa. Es lo más justo si el cliente te avisó. |

**Dime cuál quieres y lo hago.** No lo toco por mi cuenta porque cambia el inventario y manda
correos a clientes reales.

## Cómo probar lo que ya existe (no hay que esperar 2 días)

Para ver el comportamiento sin esperar, se le pone al pedido una fecha de recogida **de hace 3 días**
directo en la base:

```sql
-- 1. Encuentra tu pedido de prueba
SELECT id, estado_pedido, fecha_recogida FROM pedido ORDER BY id DESC LIMIT 5;

-- 2. Lo mandas al pasado (usa el id que viste arriba)
UPDATE pedido SET fecha_recogida = CURDATE() - INTERVAL 3 DAY WHERE id = <id>;
```

Luego esperas a las 8:00 am del día siguiente (o reinicias el back con la hora cambiada) y revisas:

- [ ] El pedido quedó cancelado
- [ ] El stock del producto volvió a subir
- [ ] Al cliente le llegó el correo de aviso
- [ ] En el log del back sale `Pedido X cancelado automáticamente`

> **Repite la prueba con un pedido APARTADO** y vas a ver que **no pasa nada** — eso confirma lo de
> arriba y te ayuda a decidir entre A, B y C.

- [ ] Probado con `Pendiente` → se cancela
- [ ] Probado con `APARTADO` → no se cancela
- [ ] **Decidido:** opción A / B / C

---

# ⏰ 3 — FECHAS CORRIDAS (solo después de las 6 pm)

**Para qué sirve:** antes de las 6 pm no se ve el bug. A partir de esa hora, el sistema guardaba
todo con la fecha de **mañana**. Esto confirma que ya guarda **hoy**.

**Cómo prepararte:** o pruebas después de las 6 pm de verdad, o le cambias la hora a la computadora
(ponla en `20:00`, zona `América/Ciudad de México`) y recargas el navegador.

### 3.1 — Gastos
**Dónde:** Menú → **Ventas** → **Gastos** → botón **➕ Agregar gasto**
*(requiere haber corrido el SQL del punto 1)*

1. Agregar un gasto.
2. Guardar y filtrar por "hoy" en la misma pantalla.

- [ ] La fecha que propone es **hoy**, no mañana
- [ ] El gasto aparece al filtrar por hoy

### 3.2 — Reportes
**Dónde:** Menú → **Reportes** → **Reportes de ventas**

- [ ] El filtro de día arranca en **hoy**
- [ ] El filtro de mes arranca en el **mes en curso**

### 3.3 — Créditos y Venta directa
**Dónde:** Menú → **Ventas** → **Venta directa** (a crédito) y Menú → **Ventas** → **Créditos / Abonos**

1. Hacer una venta a crédito con anticipo.
2. Abrir el detalle del abono.

- [ ] La fecha del abono dice **hoy**

### 3.4 — Checkout de Tienda *(el más visible para el cliente)*
**Dónde:** Tienda → agregar al carrito → checkout → **"recoger en tienda"**

- [ ] El calendario **deja elegir HOY** (antes hoy salía bloqueado)
- [ ] Después de completarlo, en Pedidos la fecha es **hoy**

### 3.5 — Flores eternas
**Dónde:** Menú → **Flores eternas** → **Arma tu ramo** → completar pedido

- [ ] La fecha del pedido es **hoy**

### 3.6 — Que no se haya roto lo de Entregas por zona
**Dónde:** Menú → **Envíos** → **Entregas por zona**

- [ ] Los presets de rango (semana en curso / últimos N días) dan las mismas fechas que antes

> **Datos viejos:** los gastos y abonos guardados de noche **antes** de este arreglo quedaron con un
> día de más. No se corrigen solos — si son pocos, se editan a mano.

---

# 🧪 4 — TRES ARREGLOS RECIÉN SUBIDOS A QA

Ya están desplegados. Solo falta que los veas.

### 4.1 — Usuarios: ahora sí tiene tarjeta
**Para qué sirve:** el formulario se veía "flotando" sin borde ni fondo, distinto al resto del
sistema.

**Dónde:** Menú → **Sistema** → **Usuarios** → botón **Actualizar** de cualquier usuario

- [ ] El formulario se ve dentro de una tarjeta con borde y sombra, como las demás pantallas
- [ ] Cambia de color al cambiar el tema (día/noche)
- [ ] **El mapa del local NO aparece por ningún lado** ← esto es lo que más fácil se rompe

### 4.2 — Ruleta pública: ya sale la descripción del premio
**Para qué sirve:** me dijiste que solo salía el nombre del producto y nada más. El visitante no
tenía forma de saber qué se está ganando.

**Dónde:** la URL de la ruleta pública, **en una ventana de incógnito** (sin sesión)

- [ ] Debajo del nombre del premio sale su **descripción**
- [ ] Si el premio no tiene descripción escrita, salen unas **etiquetas** con talla / color / marca
- [ ] Se ve bien en celular

> **Ojo:** la descripción sale de la del producto. Si quieres que diga algo específico para la rifa,
> hay que escribirla en el producto.

### 4.3 — Entregas por zona: ya salen tus pedidos ✅
Ya me confirmaste que funciona (*"sí aparecen los pedidos que no aparecían antes"*). Solo falta
cerrar el círculo completo:

- [ ] Programar el viaje de zona y confirmar que **al cliente le llega el correo** con la fecha

---

# 🌙 5 — REPASO VISUAL EN MODO NOCHE

**Para qué sirve:** 27 archivos de estilos pasaron de colores fijos a variables. El riesgo es que
alguno quedara con texto que no se lee.

**Cómo:** poner el sistema en **modo noche** y recorrer estas pantallas.

| Pantalla | Qué mirar |
|---|---|
| Chat en vivo / Chatbot | Burbujas y encabezado |
| Clientes (buscar, mis datos, mi perfil) | Campos y bordes |
| Venta directa / Detalle de pedido | Tablas: encabezado, filas alternas, fila al pasar el mouse |
| Dashboard y Reportes | Tarjetas y números |
| Gastos | Formulario y tabla |
| Rifas (agregar, mensual) | Tarjetas |
| Productos y Tienda | Tarjetas e imágenes |
| Promociones / Categorías | Autocompletado y chips |

- [ ] En ningún lado hay texto que no se lea
- [ ] Las filas alternas de las tablas se distinguen entre sí
- [ ] Los estados (rojo de error, verde de éxito) siguen leyéndose

### 5.1 — La etiqueta "EN USO" de Personalización
**Dónde:** Menú → **Sistema** → **Personalización**

Fue un bug: salía texto casi blanco sobre fondo blanco.

- [ ] "EN USO" se lee en modo día
- [ ] "EN USO" se lee en modo noche

---

# 📱 6 — LO QUE FALTA PROBAR EN CELULAR

### 6.1 — "Cómo llegar" en el login
- [ ] Desde celular: abre la **app de Google Maps** con la ruta desde donde estás
- [ ] Desde computadora: abre la web en pestaña nueva
- [ ] El destino es tu local, no otro punto

### 6.2 — Login y registro sin desbordarse
**Dónde:** celular real (o Chrome en modo celular, 390px)

- [ ] La miniatura del mapa no se sale de la pantalla
- [ ] La dirección larga se recorta con "…" en vez de romper el diseño
- [ ] **No hay scroll horizontal**

### 6.3 — Cuando no hay ubicación capturada
**Para qué sirve:** confirmar que si algún día la quitas, no queda un hueco feo ni truena.

1. En **Sistema → Negocio & Contactos**, tocar **Quitar** y confirmar.
2. Ir al login y recargar con Ctrl+Shift+R.

- [ ] No aparece nada: ni recuadro vacío, ni hueco, ni error en consola
- [ ] Los iconos de redes quedan pegados al formulario, como antes
- [ ] **Volver a capturarla** para dejarla puesta

---

# 🚀 7 — SUBIR A PRODUCCIÓN

**Solo cuando 1 a 6 estén limpios.**

### ⚠️ No se puede hacer `git merge qa` normal

Redes sociales sigue bloqueado (credenciales de prod sin definir + App Review de Meta sin aprobar).
Un merge completo lo arrastraría a producción.

**Hay que promover con `git cherry-pick`** de los commits puntuales, según la regla de `CLAUDE.md`.

### Antes de promover, correr en `inventario_key` (prod)

| SQL | ¿Ya está en prod? |
|---|---|
| `migration_negocio_ubicacion.sql` | ✅ **Ya corrida** — no volver a correrla |
| `migration_accion_gastos.sql` | ❌ **Falta** — sin esto el botón de gastos no sale en prod |

### Estado de las ramas

| Repo | `dev` | `qa` | `main` / `master` |
|---|---|---|---|
| **proyecto_key** (back) | `4957074` | `8085475` ✅ al día | `e8fb1bd` — atrás |
| **producto_venta_online** (front) | `35be5d8` | `083df9d` ✅ al día | `4a8ecd6` — atrás |

---

# 📌 PARA DESPUÉS (nada de esto se ha tocado)

| # | Qué | Estado |
|---|---|---|
| **P1** | **Generador de QR con varios destinos** — QR con los datos que necesites (Facebook, Instagram, URL…), cada uno guardado e identificado, poder elegir uno ya hecho y agregar más | Pediste que fuera casi al final |
| **P2** | **Traspaso chatbot ↔ humano** — el bot atiende, el cliente pide humano, te llega correo, el bot se calla mientras contestas, si el cliente no responde vuelve el bot, y si se acaba el crédito del bot te avisa | **No existe nada de esto**, es una feature completa. Hay que diseñarla aparte |
| **P3** | **Pantalla para leer chats viejos** — los datos ya están todos en la base, pero solo se pueden consultar las últimas 24 horas. Falta el buscador/historial | Solo falta la pantalla |
| **P4** | **Script de concursantes de prueba** — clientes ya registrados para no crear usuarios en cada prueba | Dijiste que casi al final |
| **P5** | **¿Filtrar Entregas por zona por fecha de entrega?** — hoy filtra por la fecha en que se **creó** el pedido, no por la de entrega | Querías probarlo primero antes de decidir |
| **P6** | **¿Cancelar solo los APARTADO?** — ver punto 2 de arriba, opciones A / B / C | Esperando tu decisión |
