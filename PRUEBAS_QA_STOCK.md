# Pruebas en QA — fix de stock en variantes + timeouts

**Ambiente:** QA (`https://…/qa`, base `inventario_key_qa`)
**Rama:** `qa` — commit `5e7632d`
**Fecha:** 2026-09-22

Esperá ~2 minutos desde el push para que GitHub Actions termine el deploy antes de
empezar.

---

## Qué se arregló

| # | Problema en producción | Qué debería pasar ahora |
|---|---|---|
| 1 | Cambiar el nombre de una variante daba *"Stock insuficiente… Disponible: 0, Solicitado: 1"* | Se puede editar sin que el stock importe |
| 2 | Dar de baja una variante no liberaba su stock | El stock vuelve a estar disponible enseguida |
| 3 | El stock del producto podía quedar negativo (llegó a `-7`) | La app lo rechaza con un mensaje claro |
| 4 | Ningún cliente HTTP tenía timeout → pantallas colgadas para siempre | Corta a los 35 s con error |

---

## Antes de empezar: fotografía del estado

Corré esto en **`inventario_key_qa`** y guardá el resultado. Sirve para comparar al final:

```sql
SELECT p.id, p.nombre, p.stock AS stock_producto,
       SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END) AS en_variantes_activas,
       SUM(CASE WHEN v.habilitado = '0' THEN v.stock ELSE 0 END) AS en_variantes_de_baja
FROM producto p
LEFT JOIN variantes v ON v.producto_id = p.id
GROUP BY p.id, p.nombre, p.stock
HAVING en_variantes_activas > 0 OR en_variantes_de_baja > 0
ORDER BY p.id
LIMIT 30;
```

Elegí **un producto con al menos 2 variantes activas** y anotá su `id`. Lo vas a usar en
todas las pruebas de abajo. En los pasos aparece como `<PRODUCTO_ID>`.

---

# PRUEBA 1 — Cambiar el nombre sin tocar el stock 🔴 la más importante

Es el caso exacto que te bloqueó en producción.

### Pasos
1. Entrá a **tienda/update**
2. Buscá el producto `<PRODUCTO_ID>`
3. Elegí una variante que **tenga stock** (por ejemplo 1)
4. Cambiale **solo el nombre / la descripción**. No toques el campo de stock.
5. Guardar

### Resultado esperado
✅ **Guarda sin error.**

### Si falla
❌ Si sale *"Stock insuficiente… Solicitado: 1"*, el fix no llegó al pod. Verificá que el
deploy de QA haya terminado (Actions en verde) y que el pod esté reiniciado:
```bash
kubectl rollout status deployment proyecto-key-deployment -n qa
```

### Verificá que el stock no se movió
```sql
SELECT id, nombre, stock FROM producto WHERE id = <PRODUCTO_ID>;
SELECT id, talla, color, stock, habilitado FROM variantes WHERE producto_id = <PRODUCTO_ID>;
```
Los números tienen que ser **idénticos** a los de antes de guardar.

---

# PRUEBA 2 — Dar de baja una variante libera su stock 🔴

### Antes
```sql
SELECT p.stock AS stock_producto,
       SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END) AS en_activas
FROM producto p LEFT JOIN variantes v ON v.producto_id = p.id
WHERE p.id = <PRODUCTO_ID> GROUP BY p.stock;
```
Anotá: `disponible_antes = stock_producto − en_activas`

### Pasos
1. En **tienda/update**, elegí una variante con stock (digamos 3)
2. Dale **"dar de baja"**
3. Ahora intentá **crear una variante nueva** con ese mismo stock (3)

### Resultado esperado
✅ La variante nueva **se crea sin error**. Antes esto fallaba porque la variante dada de
baja seguía reteniendo sus 3.

### Verificá
```sql
SELECT id, talla, stock, habilitado FROM variantes WHERE producto_id = <PRODUCTO_ID>;
```
La vieja debe estar con `habilitado = '0'` y **conservando su stock en la fila** (no se
borra el número, simplemente ya no cuenta). La nueva con `habilitado = '1'`.

---

# PRUEBA 3 — Bajarle stock a una variante nunca falla

### Pasos
1. Variante con stock 5 → cambiala a 2
2. Guardar

### Resultado esperado
✅ Guarda. Está liberando 3, no pidiendo nada.

### Y el complemento, en el mismo guardado
Si la pantalla te deja editar dos variantes a la vez:
1. Una baja de 5 a 2 (libera 3)
2. Otra sube de 5 a 8 (pide 3)
3. Guardar

✅ Debe pasar aunque no haya stock libre en el producto: lo que una suelta lo toma la otra.

---

# PRUEBA 4 — Pedir de más sigue estando prohibido ✋

Esto **tiene que fallar**. Si pasa, el fix quedó demasiado permisivo.

### Pasos
1. Mirá cuánto disponible tiene el producto (query de arriba)
2. Intentá crear una variante con **más stock del disponible**

### Resultado esperado
❌ Error: *"Stock insuficiente para el producto '…' (id=…). Disponible: X, Solicitado: Y"*

Verificá que **X sea el número correcto** — que no incluya el stock de variantes dadas de
baja. Ese era justamente el bug.

---

# PRUEBA 5 — El stock del producto no puede quedar negativo ✋

También tiene que fallar.

### Pasos
1. Buscá un producto con poco stock
2. Intentá subirle a una variante más stock del que el producto puede dar

### Resultado esperado
❌ Error: *"El stock del producto '…' quedaría en -N. Revisa el stock del producto antes de
este cambio."*

### Verificá que no quedó ninguno negativo
```sql
SELECT COUNT(*) FROM producto WHERE stock < 0;
```
Debe dar **0**.

---

# PRUEBA 6 — Timeouts

Difícil de forzar a propósito, así que se prueba por ausencia de síntoma.

### Pasos
1. Entrá a **clientes/mis-datos** (la pantalla que se te quedaba cargando)
2. Navegá por pantallas que traen imágenes: **tienda/venta**, **tienda/update**,
   **productos/all**

### Resultado esperado
✅ Ninguna pantalla se queda cargando indefinidamente. Si un servicio no responde, a los
**35 segundos** sale un error — no un spinner eterno.

### Si querés forzarlo
Bajá el micro de imágenes y mirá que la app siga respondiendo:
```bash
kubectl scale deployment imagenes-deployment -n qa --replicas=0
# probá la app: las imágenes fallan, pero el resto responde
kubectl scale deployment imagenes-deployment -n qa --replicas=1
```
**Antes de este fix**, con el micro caído se colgaba la aplicación entera.

---

# PRUEBA 7 — El subtotal ya no se puede falsificar 💰

**Esta es de dinero.** Se prueba con curl o Postman, no desde la pantalla: el front manda
los números bien, el punto es qué pasa si alguien los manda mal a propósito.

### El agujero que había

`validarLineasPromocion` revisaba vigencia, precio unitario y cantidad — pero **no el
subtotal**. Y el total del pedido se arma sumando subtotales. Entonces con una promoción
se podía mandar el precio unitario correcto y un subtotal de 1, y el pedido quedaba en $1.

### Cómo probarlo

Armá un pedido normal desde la pantalla, capturá el request con las DevTools (pestaña
Red), y reenvialo con el `subTotal` cambiado a `1`:

```bash
curl -X POST "https://<host-qa>/mis-productos/v1/pedidos/save" \
  -H "Authorization: Bearer <tu-token>" \
  -H "Content-Type: application/json" \
  -d '{
        "cliente": { "id": <CLIENTE_ID> },
        "detalles": [{
          "producto": { "id": <PRODUCTO_ID> },
          "varianteId": <VARIANTE_ID>,
          "cantidad": 2,
          "precioUnitario": 350.0,
          "subTotal": 1.0
        }]
      }'
```

### Resultado esperado

✅ El pedido se crea **con el subtotal correcto (700), no con 1**. El back ignora el
`subTotal` del request y lo calcula como `precioUnitario × cantidad`.

Verificá en la base:
```sql
SELECT dp.id, dp.cantidad, dp.precio_unitario, dp.sub_total, p.total_pedido
FROM detalle_pedidos dp
JOIN pedidos p ON p.id = dp.pedido_id
ORDER BY dp.id DESC LIMIT 5;
```
`sub_total` debe ser `precio_unitario × cantidad`. Si aparece un 1, el fix no llegó.

### Probá también con el precio unitario

```bash
# mismo request pero con "precioUnitario": 1.0
```
❌ Debe fallar: *"El precio de … no es válido"*. Esa validación ya existía y se mantiene.

### Lo mismo para venta directa
Repetí ambos casos contra `POST /v1/ventas/directa`. El fix se aplicó en los dos flujos.


---

# PRUEBA 8 — Apartar una promoción 🎁

### El problema que tenías

> *"Cuando genero una promoción pasa directo a pago en efectivo."*

No era que pasara directo: el back **rechazaba** cualquier cosa que no fuera contado, en la
primera línea de la validación. La pantalla se quedaba sin opciones.

### Pasos

1. Andá a **tienda/venta** (o donde armes el pedido con promoción)
2. Elegí una promoción vigente y armá el combo completo (todas las variantes que pide)
3. En tipo de pedido elegí **APARTADO**
4. Elegí forma de pago: probá **efectivo** y repetí con **tarjeta**
5. Guardar

### Resultado esperado

✅ El pedido se crea como apartado, con la forma de pago que elegiste.

❌ Si sale *"Las promociones solo se pueden comprar de contado…"*, el deploy no llegó.

### Verificá

```sql
SELECT p.id, p.tipo_pedido, p.estado_pedido, p.total_pedido,
       dp.precio_unitario, dp.promocion_id
FROM pedidos p
JOIN detalle_pedidos dp ON dp.pedido_id = p.id
WHERE dp.promocion_id IS NOT NULL
ORDER BY p.id DESC LIMIT 5;
```
- `tipo_pedido` = `APARTADO`
- `precio_unitario` = el precio promocional, no el de catálogo

### Y que el stock se descontó al apartar
```sql
SELECT id, nombre, stock FROM producto WHERE id = <PRODUCTO_ID>;
```
Debe haber bajado. Esa fue tu decisión: se reserva al apartar, no al liquidar.

### Los tres tipos se pueden

| Tipo | La mercadería | ¿Con promoción? |
|---|---|---|
| NORMAL | se paga y se lleva | ✅ |
| APARTADO | se queda en el negocio hasta pagarse | ✅ |
| FIADO | se la lleva y paga después | ✅ |

**Probá los tres.** La validación ya no mira la forma de cobro — eso lo decide quien está
atendiendo, caso por caso. Bloquearlo obligaba a cancelar el pedido y rehacerlo sin
promoción, y terminaba quedando registrado algo distinto de lo que pasó.

Lo que **sí** sigue validándose (probalo con curl cambiando el precio):
- que la promoción esté vigente y activa
- que el precio de cada línea sea el de la promoción
- que las variantes sean las del combo, todas y sin sobrantes

### El precio queda congelado
Si apartás hoy con una promoción que vence mañana y liquidás la semana que viene, pagás el
precio de hoy. El precio se guarda en `detalle_pedidos` al crear el pedido y al liquidar
nadie revalida la promoción.


---

# PRUEBA 9 — Stock disponible y reporte de descuadres 📊

Dos endpoints nuevos. El front todavía no los consume (ese repo no está en la sesión), así
que por ahora se prueban con curl — pero el reporte de descuadres **te sirve ya**.

## 9a — Cuánto queda libre

```bash
curl -H "Authorization: Bearer <token>" \
  "https://<host-qa>/mis-productos/v1/stock/producto/<PRODUCTO_ID>"
```

Respuesta:
```json
{
  "productoId": 279, "nombreProducto": "Great Jeans",
  "stockTotal": 10, "enVariantes": 4, "variantesActivas": 2,
  "enVariantesDeBaja": 0, "disponible": 6, "descuadrado": false,
  "mensaje": "10 en total, 4 repartidos en 2 modelos, quedan 6 disponibles."
}
```

### Contrastá contra la base
```sql
SELECT p.stock AS total,
       COALESCE(SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END),0) AS en_variantes,
       COALESCE(SUM(CASE WHEN v.habilitado <> '1' THEN v.stock ELSE 0 END),0) AS en_de_baja
FROM producto p LEFT JOIN variantes v ON v.producto_id = p.id
WHERE p.id = <PRODUCTO_ID> GROUP BY p.stock;
```
Tienen que coincidir. Y `disponible` = `total − en_variantes` (el de las dadas de baja **no**
descuenta).

### La prueba que más importa
1. Anotá el `disponible` de un producto
2. **Dale de baja** a una variante con stock
3. Volvé a pedir el endpoint

✅ El `disponible` tiene que haber **subido** por el stock de esa variante, y el número
aparece ahora en `enVariantesDeBaja`.

## 9b — Reporte de descuadres 🔍 el que te sirve ya

```bash
curl -H "Authorization: Bearer <token>" \
  "https://<host-qa>/mis-productos/v1/stock/admin/descuadrados"
```

Lista los productos donde **las variantes piden más stock del que el producto declara**.
Cada uno trae el mensaje explicando el problema:

```json
[{ "productoId": 269, "nombreProducto": "Jeans Short Especal",
   "stockTotal": 12, "enVariantes": 18, "disponible": -6, "descuadrado": true,
   "mensaje": "Este producto esta descuadrado: tiene 12 en total pero sus 6 modelos suman 18." }]
```

**Para qué sirve:** es la lista que hay que resolver antes de migrar al modelo de stock que
elegiste. Por cada producto de esa lista hay que decidir cuál número es el verdadero:
- ¿manda el stock del producto? → hay que recortar variantes
- ¿mandan las variantes? → hay que subir el producto
- ¿ninguno? → recuento físico

Esa decisión es de negocio, no se puede automatizar. **Corré esto en producción también**
(el endpoint es de solo lectura, no modifica nada) para ver el tamaño real del problema.

## 9c — Que no sea público ✋

```bash
curl "https://<host-qa>/mis-productos/v1/stock/producto/1"    # sin token
```
❌ Debe dar **401/403**. Expone el inventario del negocio, no es dato de cliente.


---

# PRUEBA 10 — Buscar por código de barras 🔍

### Tu caso: `H1336` buscando `1336`

El `LIKE '%1336%'` **sí** encontraba el `H1336`. El problema era que salía mezclado entre
todas las filas que contienen ese texto, sin orden, y podía caer en la página 2 — por eso
parecía que no existía.

### Pasos
1. **tienda/update** (o **tienda/venta**, o **productos/buscar**)
2. Buscá el **código completo**: `H1336`
3. Buscá solo el **pedazo**: `1336`

### Resultado esperado

| Búsqueda | Qué tiene que salir |
|---|---|
| `H1336` (completo) | **primero de la lista** |
| `1336` (pedazo) | entre los primeros — antes que los que apenas contienen "1336" |

El orden es: código exacto → nombre exacto → los que **empiezan** con el término → los que
solo lo contienen.

### Y que el buscador vacío no cambió
Vaciá el buscador: debe volver a traer todo en el orden de siempre (por id, más nuevo
primero).

---

# PRUEBA 11 — Mis pedidos con código, nombre y foto 🖼️

### Pasos
1. Entrá como **cliente** (no admin)
2. **pedidos/mis-pedidos**
3. Abrí el detalle de un pedido

### Resultado esperado

Cada renglón trae ahora, además de lo que ya traía:
- **código de barras** del producto
- **miniatura** de la foto

Verificá el response con las DevTools (pestaña Red):
```json
{
  "productoNombre": "Great Jeans",
  "codigoBarras": "H1336",
  "imagenId": 4904627400389007798,
  "urlImagen": ".../v1/imagenes/thumbnail/4904627400389007798",
  "talla": "M", "color": "Azul", "cantidad": 2
}
```

### Tres cosas a mirar

**① Es miniatura, no la foto completa.** La url dice `/thumbnail/`, no `/file/`. En un
pedido de 10 artículos la diferencia son megas de datos móviles del cliente.

**② Un producto sin foto no rompe nada.** Buscá un pedido con un artículo sin imagen: el
renglón sale igual, solo sin los campos `imagenId` y `urlImagen`.

**③ Si el micro de imágenes está caído, el pedido se ve igual** (sin fotos). Se puede
forzar:
```bash
kubectl scale deployment imagenes-deployment -n qa --replicas=0
# abrir mis-pedidos: tiene que cargar, sin miniaturas
kubectl scale deployment imagenes-deployment -n qa --replicas=1
```

### Si el admin marcó una imagen como principal
Esa es la que tiene que salir, no una cualquiera.


---

# PRUEBA 12 — Cambiar la forma de cobro de un pedido ya creado 🔁

**El caso que la origina:** se apartó un pedido, al ir a entregarlo el cliente decidió pagarlo
completo. No había forma de cambiarlo, así que quedó registrado como apartado. La otra salida era
cancelar y rehacer el pedido entero — que devuelve y vuelve a descontar el stock.

### Antes de probar: correr la migración del permiso

```bash
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_pedido_cambiar_tipo.sql
```

Verificar que quedó:
```sql
SELECT r.nombre_rol, a.clave, a.etiqueta
FROM rol_accion ra
JOIN roles r ON r.id = ra.rol_id
JOIN accion_submenu a ON a.id = ra.accion_submenu_id
JOIN submenu s ON s.id = a.submenu_id
WHERE s.ruta = 'pedidos/mis-pedidos' AND a.clave = 'cambiar-tipo';
```
Tiene que salir **una fila: `ROLE_ADMIN` / `cambiar-tipo`**. Si sale vacío, el endpoint va a
responder 403 a todo el mundo y el resto de esta prueba no corre.

### 12a — Apartado que se termina de pagar (el caso real)

Buscar un pedido **APARTADO con saldo pendiente**:
```sql
SELECT id, tipo_pedido, total_pedido, total_pagado,
       total_pedido - COALESCE(total_pagado, 0) AS falta
FROM pedido
WHERE tipo_pedido = 'APARTADO' AND estado_pedido NOT IN ('Entregado', 'cancelado')
  AND total_pedido - COALESCE(total_pagado, 0) > 0
LIMIT 5;
```

Cobrar el saldo y pasarlo a contado, dejando escrito qué pasó:
```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/tipo" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"tipoPedido":"NORMAL","monto":<FALTA>,"metodoPago":"EFECTIVO",
       "montoDado":<FALTA>,"nota":"Pagó el resto al entregarlo","usuarioId":<TU_USUARIO>}'
```

Tiene que responder **200** con el detalle del pedido ya en `NORMAL`. Y en la base:
```sql
SELECT tipo_pedido, total_pagado, total_pedido FROM pedido WHERE id = <PEDIDO_ID>;
-- tipo_pedido = NORMAL  y  total_pagado = total_pedido

SELECT monto, metodo_pago, nota FROM abono WHERE pedido_id = <PEDIDO_ID> ORDER BY id DESC LIMIT 1;
-- nota = "Cambio de APARTADO a NORMAL: Pagó el resto al entregarlo"
```

**Lo importante de la nota:** el prefijo `Cambio de X a Y:` lo pone el back solo. Sin él, dentro
de un mes ese abono se ve igual que cualquier otro y nadie sabe por qué ese pedido cambió de forma
de cobro.

### 12b — Pasar a contado sin cobrar el saldo → tiene que fallar

Mismo pedido apartado con saldo, pero sin mandar `monto`:
```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/tipo" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"tipoPedido":"NORMAL","usuarioId":<TU_USUARIO>}'
```
Tiene que fallar con **"Para pasar el pedido a contado hay que cobrar el saldo completo. Falta
$X y en este cambio se cobran $0.00"**. Un `NORMAL` con saldo pendiente es exactamente lo que
`NORMAL` dice que no existe: el pedido desaparecería de la lista de lo que falta cobrar.

Cobrando **de menos** (`monto` menor al saldo) tiene que fallar igual, **y no debe quedar abono**:
```sql
SELECT COUNT(*) FROM abono WHERE pedido_id = <PEDIDO_ID>;  -- el mismo número que antes
```

### 12c — Apartado ↔ fiado, sin cobrar nada

```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/tipo" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"tipoPedido":"FIADO","usuarioId":<TU_USUARIO>}'
```
200, `tipo_pedido = FIADO`, y **ningún abono nuevo** — solo se movió la forma de cobro.

### 12d — Lo que no se puede cambiar

Los cuatro tienen que fallar, cada uno con su mensaje:

| Caso | Mensaje esperado |
|---|---|
| Pedido ya **entregado** | "ya se entregó: no se puede cambiar su forma de cobro" |
| Pedido **cancelado** | "está cancelado" |
| Al **mismo tipo** que ya tiene | "ya es de tipo APARTADO" |
| Tipo inventado (`"CREDITO"`) | "Tipo de pedido inválido... NORMAL, APARTADO y FIADO" |

Y sobre un pedido **NORMAL** (ya pagado) mandando `monto`:
"es de tipo NORMAL y no tiene saldo que cobrar" — no hay nada que abonar ahí.

### 12e — El permiso (lo que lo hace "configurado")

Con un usuario **sin** la acción `cambiar-tipo` (cualquier rol que no sea ADMIN):
```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/tipo" \
  -H "Authorization: Bearer $TOKEN_USUARIO" -H "Content-Type: application/json" \
  -d '{"tipoPedido":"FIADO","usuarioId":<SU_USUARIO>}'
```
→ **403**.

Ahora, desde **Gestión de roles**, marcarle a ese rol la casilla
**"Cambiar forma de cobro del pedido (🔁)"** (sale bajo *Detalle del pedido*), volver a
loguearse para que el token traiga la autoridad nueva, y repetir el curl → **200**.

Eso es lo que significa que el botón está configurado: se le puede dar a quien cobra en mostrador
sin darle el resto de la gestión de pedidos, y se quita igual, sin tocar código.

**Ojo con el token:** los permisos viajan dentro del JWT. Después de cambiar un rol hay que
volver a entrar (o esperar al refresh) — si no, el token viejo sigue sin la autoridad y parece
que el permiso no sirvió.


---

# Al terminar: comparación final

```sql
SELECT p.id, p.nombre, p.stock AS stock_producto,
       SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END) AS en_activas,
       p.stock - SUM(CASE WHEN v.habilitado = '1' THEN v.stock ELSE 0 END) AS disponible
FROM producto p
LEFT JOIN variantes v ON v.producto_id = p.id
WHERE p.id = <PRODUCTO_ID>
GROUP BY p.id, p.nombre, p.stock;
```

Y el chequeo que nunca debe fallar:
```sql
SELECT COUNT(*) AS productos_negativos FROM producto WHERE stock < 0;
```

---

# Checklist

- [ ] **P1** Cambiar el nombre de una variante sin tocar stock → guarda
- [ ] **P2** Dar de baja libera el stock → se puede crear otra variante con ese stock
- [ ] **P3** Bajarle stock a una variante → guarda
- [ ] **P3b** Una baja y otra sube en el mismo guardado → guarda
- [ ] **P4** Pedir de más → falla con el número correcto
- [ ] **P5** Stock negativo → rechazado
- [ ] **P6** Ninguna pantalla se cuelga
- [ ] **P7** Subtotal falsificado → el back lo recalcula (no queda en $1)
- [ ] **P7b** Precio unitario falsificado → rechazado
- [ ] **P8** Apartar una promoción → se crea (efectivo y tarjeta)
- [ ] **P8b** Los tres tipos (contado, apartado, fiado) → se pueden
- [ ] **P9a** `/v1/stock/producto/{id}` coincide con la base
- [ ] **P9a2** Dar de baja una variante → sube el disponible
- [ ] **P9b** Reporte de descuadres lista los productos rotos
- [ ] **P9c** Sin token → 401/403
- [ ] **P10** Código completo → primer resultado
- [ ] **P10b** Buscador vacío → trae todo como antes
- [ ] **P11** Mis pedidos muestra código y miniatura
- [ ] **P11b** Artículo sin foto → el renglón sale igual
- [ ] **P12** Migración del permiso corrida → la fila `cambiar-tipo` existe
- [ ] **P12a** Apartado + cobro del saldo → queda NORMAL, con el abono y su nota
- [ ] **P12b** Pasar a contado sin cobrar (o cobrando de menos) → rechazado, sin abono
- [ ] **P12c** Apartado → fiado sin cobrar → cambia, sin abono
- [ ] **P12d** Entregado / cancelado / mismo tipo / tipo inventado → rechazados
- [ ] **P12e** Sin el permiso → 403; con el permiso dado en Gestión de roles → 200
- [ ] `SELECT COUNT(*) FROM producto WHERE stock < 0` → **0**

---

# Lo que este fix NO resuelve

Para que no lo busques en estas pruebas:

| Pendiente | Prioridad |
|---|---|
| Campo "stock disponible" **en pantalla** | back ✅ hecho (Prueba 9) — falta el front |
| Modal para cambiar la forma de cobro | back ✅ hecho (Prueba 12) — falta el front |
| ~~Precios validados en el back~~ | ✅ **hecho** — ver Prueba 7 |
| ~~Promociones con apartado y tarjeta~~ | ✅ **hecho** — ver Prueba 8 |
| ~~Búsqueda por código exacto primero~~ | ✅ **hecho** — Prueba 10 |
| Contador de tallas que dice 3 con 2 | P3 — front |
| ~~`mis-pedidos` con código, nombre y foto~~ | ✅ **hecho** — Prueba 11 |
| Buscador blanco en modo día | P4 — front |
| Renombrar `variante` → `artículo` | P4 |

**Y lo más importante:** este fix corrige la *lógica*, pero **no arregla los datos que ya
están descuadrados**. En producción varios productos tienen menos stock que la suma de sus
variantes (el 269: producto 12, variantes 18). Eso necesita decidir producto por producto
cuál es el número verdadero — ver `hexagonal/stock/README.md`.

---

# Si todo pasa

El camino a producción es por **cherry-pick**, no por merge, porque `dev`/`qa` llevan por
delante el módulo de redes sociales que `main` no debe recibir (ver CLAUDE.md):

```bash
git checkout main && git pull origin main
git cherry-pick b8b9b27        # solo el commit del fix
git push origin main           # dispara el deploy a producción
```

El commit de documentación (`hexagonal/`) **no se promueve** — se queda en `dev`/`qa`.
