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

hay que usar subconsultas o CTE o Windowas functionas y anotarlo para entenderlo porque estoy aprendiendo eso

```

Elegí **un producto con al menos 2 variantes activas** y anotá su `id`. Lo vas a usar en
todas las pruebas de abajo. En los pasos aparece como `<PRODUCTO_ID>`.
hay que inicar a cambiar los nombres de variante a articulo en lo que se terminan de migrar todo
ID 266
CODIGO SU2287
SU2287
---

# PRUEBA 1 — Cambiar el nombre sin tocar el stock 🔴 la más importante

Es el caso exacto que te bloqueó en producción.

### Pasos
1. Entrá a **tienda/update**
2. Buscá el producto `<PRODUCTO_ID>`
3. Elegí una variante que **tenga stock** (por ejemplo 1)
4. Cambiale **solo el nombre / la descripción**. No toques el campo de stock.
5. Guardar

TE DOY ESTE EJEMPLO
$250.00
10 unidades
Nombre
Jeans Short Brillo
Código
SU2287
Marca
sin marca
Descripción
Bolsa
antes solo habia 2 stock
productos/buscar
pero en articulos o antes variantes tenia varias variantes pero des habilitades, entonces cuando agrego stock a 1 variante, se supone que tenia solo 2 en stock del producto
pero agregue las 2 stock y parece que esta aumentado el stock del producto base, cuando no es asi, lo que deberia hacer es primero
Hay 2 stock en producto base, lo que en articulo ahorita variantes es quiero dar de alta o agregar una variante o agregar stock, primeor voy a validar
a producto base y valido cuantos stock hay en este caso hay 2 stock y quiero agregar 1 stock en la variante si se puede hacaer
se agrega 1 stock o se agrega o actualiza la info correspondiente y si quiere agregar una nueva variante que va a hacer es agregar ;la variante con el stock que no pase de 2 en este caso
o si ya hubiera una variable con 1 stock el disponible solo es 1 stock, y por ejemplo si doy de baja 1 variante entonces si regresa en automatico al productop base
pero si solo las des habilito depende de como lo ahiga echo si solo lo des habilite y en producto base solo hay 1 pero si quisiera agregar una variante nueva
y solo hay 1 base entonces va ay busca primero hay variabes des habilitades y ademas si hay stock entoncews si se puede crear la variante,
actualmente lo que esta haciendo es creo una variante y al stock del producto base se le agrega esa variante y si voy a editar esa variable sigue igua;l
actualize el stock y si se actualizo el stock y eso esta mas
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

# PRUEBA 13 — Editar los artículos de un pedido ya creado 🛍️

**El caso que la origina:** hoy solo se puede **quitar** una línea (el botón `−`). Para agregar
algo o cambiar una talla hay que cancelar el pedido entero y rehacerlo — que devuelve y vuelve a
descontar el stock, y deja registrado algo distinto de lo que realmente pasó.

### Antes de probar: correr la migración

```bash
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_pedido_articulos.sql
```

Tienen que salir **3 filas** (`agregar-articulo`, `cambiar-articulo`, `quitar-promocion`), todas
para `ROLE_ADMIN` — la consulta de verificación está comentada al final del script.

### 13a — Agregar un artículo

```bash
curl -X POST "$HOST/v1/pedidos/<PEDIDO_ID>/articulos" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"varianteId":<VARIANTE_ID>,"cantidad":2}'
```

Responde **200** con el pedido completo ya actualizado. Verificar en la base que el stock **bajó
en los dos lados** (artículo y modelo) y que el total se recalculó:

```sql
SELECT v.id, v.stock AS stock_articulo, p.stock AS stock_modelo
FROM variantes v JOIN producto p ON p.id = v.producto_id WHERE v.id = <VARIANTE_ID>;

SELECT total_pedido, total_pagado FROM pedido WHERE id = <PEDIDO_ID>;
```

**13a2 — Agregar lo mismo dos veces:** repetir el mismo curl. Tiene que quedar **una sola línea
con la cantidad sumada**, no dos líneas iguales:
```sql
SELECT id, variante_id, cantidad, precio_unitario, sub_total
FROM detalle_pedidos WHERE pedido_id = <PEDIDO_ID> AND variante_id = <VARIANTE_ID>;
-- una fila, no dos
```

### 13b — El precio no se puede inventar

Sin `precioUnitario` se cobra el **precio normal**. Mandando el de **rebaja** se acepta. Mandando
cualquier otro, se rechaza:

```bash
# este tiene que fallar
curl -X POST "$HOST/v1/pedidos/<PEDIDO_ID>/articulos" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"varianteId":<VARIANTE_ID>,"cantidad":1,"precioUnitario":1}'
```
→ **400** `El precio $1.00 no es valido para 'X'. Se puede cobrar a $400.00 (normal) o $350.00 (rebaja)`

Y lo importante: **el stock no se movió**. Volver a correr la consulta de stock de 13a.

### 13c — Cambiar un artículo por otro (línea sin promoción)

```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/articulos/<DETALLE_ID>" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"varianteId":<VARIANTE_NUEVA>}'
```

El `<DETALLE_ID>` sale del response de 13a (campo `detalleId` de cada línea). Tiene que
**devolver el stock del viejo y descontar el del nuevo**, en un solo paso.

### 13d — El combo de promoción 🎁 (lo más importante)

Armar un pedido con una promoción **y algo más fuera de ella** (ej. promoción de 2 artículos +
una cartera suelta). Después intentar cambiar una línea de la promoción por algo que **no está**
en el combo:

```bash
curl -X PUT "$HOST/v1/pedidos/<PEDIDO_ID>/articulos/<DETALLE_DE_LA_PROMO>" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"varianteId":<ALGO_FUERA_DE_LA_PROMO>}'
```

**Responde 409, no 400** — y eso es a propósito: no es un error, es una pregunta. El body trae
las dos salidas para el modal:

```json
{
  "requiereDecision": true,
  "mensaje": "'Pantalón hombre' no forma parte de la promocion 'Combo...'",
  "promocionId": 7,
  "importeDelCombo": 500.0,
  "lineasDelCombo": [ ... ],
  "opciones": [
    { "modo": "QUITAR_PROMOCION",    "titulo": "Quitar la promocion completa", "explicacion": "..." },
    { "modo": "CONSERVAR_PROMOCION", "titulo": "Conservarla y agregarlo aparte", "explicacion": "..." }
  ]
}
```

**Verificar que el pedido quedó intacto** — este es el punto de la prueba:
```sql
SELECT id, variante_id, cantidad, promocion_id FROM detalle_pedidos WHERE pedido_id = <PEDIDO_ID>;
-- las mismas líneas que antes del curl
```

**13d2 — Opción (a), quitar la promoción:** mismo curl con `"modo":"QUITAR_PROMOCION"`.
Tienen que salir **todas** las líneas de la promoción (no solo la que se cambiaba), su stock
tiene que volver, el artículo nuevo entra **a precio normal**, y **la cartera no se toca**:
```sql
SELECT id, variante_id, cantidad, precio_unitario, promocion_id
FROM detalle_pedidos WHERE pedido_id = <PEDIDO_ID>;
-- 0 filas con promocion_id = 7, la cartera sigue ahí, y la línea nueva a precio normal
```

**13d3 — Opción (b), conservarla:** en otro pedido igual, mandar
`"modo":"CONSERVAR_PROMOCION"`. La promoción queda **entera** y se suma una línea nueva a precio
normal. Nada se borra.

**13d4 — Cambio DENTRO del combo:** cambiar una línea de la promoción por otro artículo **que sí
está** en esa promoción. Tiene que cambiar directo (200, sin 409) y **al precio del combo**, no al
de catálogo:
```sql
SELECT variante_id, precio_unitario, promocion_id FROM detalle_pedidos WHERE id = <DETALLE_ID>;
-- promocion_id sigue puesto y precio_unitario es el de promocion_detalle
```

### 13e — Quitar una promoción completa

```bash
curl -X DELETE "$HOST/v1/pedidos/<PEDIDO_ID>/promociones/<PROMOCION_ID>" \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```
Salen todas sus líneas y vuelve su stock.

**13e2 — No se puede vaciar el pedido:** en un pedido que **solo** tiene la promoción, el mismo
curl tiene que fallar con *"quedaria sin articulos... hay que cancelar el pedido"*. Un pedido
vacío no es un pedido.

### 13f — El agujero que se cerró: el botón `−` sobre una promoción

El botón de quitar (`DELETE /v1/pedidos/{id}/detalle/{productoId}`) **dejaba sacar una línea
suelta de una promoción**, y el resto del combo se quedaba al precio promocional — cobrando un
descuento por una condición que ya no se cumplía, en silencio.

```bash
curl -X DELETE "$HOST/v1/pedidos/<PEDIDO_ID>/detalle/<PRODUCTO_DE_LA_PROMO>" \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```
Ahora tiene que **fallar**, diciendo que hay que quitar la promoción completa y con qué endpoint.
Sobre una línea **sin** promoción sigue funcionando igual que siempre.

### 13g — Los permisos

Con un usuario sin las acciones → **403** en los tres endpoints. Dándole solo
**"Agregar artículo al pedido (+)"** desde Gestión de roles (y volviendo a entrar para refrescar
el token): puede agregar, pero **sigue en 403** para cambiar y para quitar la promoción.

Eso es el punto de que sean tres acciones y no una: desarmar un combo es una decisión de dinero
más grande que sumar un artículo.

### 13h — Un pedido cerrado no se edita

Sobre un pedido **Entregado** o **cancelado**, los tres endpoints tienen que responder 400 con
*"ya se entrego"* / *"esta cancelado"*.

### 13i — Pedido con abonos

En un pedido que ya tiene abonos, quitar artículos hasta que valga **menos de lo pagado**. El
response tiene que traer `saldo` **negativo** — es dinero a favor del cliente y la pantalla tiene
que mostrarlo. `total_pagado` **no se toca**:
```sql
SELECT total_pedido, total_pagado FROM pedido WHERE id = <PEDIDO_ID>;
-- total_pagado igual que antes de la edición
```


---

# PRUEBA 14 — El alta de artículos: el contador que dice 3 con 2 🔢

**El caso:** la pantalla de alta tiene **dos partes independientes** — el formulario base (para dar
de alta uno solo) y la sección de varias tallas. Si se llena el base, se agregan 2 tallas y después
**se vacía el base**, el base seguía viajando: el back recibía 3 y creaba **3**, y el tercero nacía
sin talla, sin color y sin nada.

**No hace falta migración para esta prueba.**

### 14a — El caso exacto reportado

En el alta de un modelo: llenar el formulario base, agregar **2 tallas**, y después **borrar lo
que se llenó en el base**. Guardar.

Tienen que quedar **2 artículos, no 3**:
```sql
SELECT id, talla, color, marca, stock, palabra_clave_id
FROM variantes WHERE producto_id = <PRODUCTO_ID> ORDER BY id DESC;
```
Ninguna fila con todo en null.

### 14b — Un solo artículo, sin tallas (que NO se rompa)

Dar de alta un modelo llenando **solo el formulario base**, sin agregar tallas. Tiene que crear
**1 artículo**. Este es el caso que había que no romper al filtrar: *"si solo quiero agregar 1
artículo, lleno los datos que están"*.

**14b2 — Sin talla pero con stock:** un artículo sin talla ni color pero **con stock** se guarda
igual — es el modelo que no tiene variantes reales.

**14b3 — Sin datos pero con foto:** también se guarda.

**14b4 — Todo vacío:** si **todos** los artículos llegan vacíos, responde
*"No hay ningún artículo que guardar: todos llegaron vacíos..."* en vez de crear basura.

### 14c — Editar un artículo existente borrándole los campos

Sobre un artículo que **ya existe**, borrarle talla y color y guardar. **No se descarta** — vaciar
los campos de algo guardado es una edición válida, no un descarte. Es la diferencia entre `id`
null y `id` con valor.

### 14d — La categoría se hereda del modelo

En un modelo que **tiene categoría** (palabra clave), agregar varias tallas **sin elegirle
categoría a ninguna**. Todas tienen que quedar con la del modelo:

```sql
SELECT v.id, v.talla, v.palabra_clave_id, p.palabra_clave_id AS categoria_del_modelo
FROM variantes v JOIN producto p ON p.id = v.producto_id
WHERE v.producto_id = <PRODUCTO_ID>;
-- palabra_clave_id igual al del modelo en todas
```

**14d2 — La propia gana:** si a **una** de las tallas se le elige una categoría distinta, esa
conserva la suya y las demás siguen heredando. El modelo no la pisa.

---

# PRUEBA 15 — El precio de rebaja en la card de tienda 🏷️

El tercer precio (`precio_rebaja`) ya se podía **cobrar** (Pruebas 7 y 13b), pero la card nunca lo
recibía, así que el front no tenía con qué ofrecerlo. Ahora `VarianteResumenDto` lo lleva.

### 15a — Como admin, la card trae los dos precios

```bash
curl "$HOST/v1/variantes/buscar?termino=<ALGO>&pagina=1&size=5" \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```
Cada artículo tiene que traer `precio` **y** `precioRebaja` (en los modelos que tengan rebaja
cargada).

### 15b — Como cliente, la rebaja NO viaja 🔒

El mismo curl **sin token** (o con un token de cliente):

```bash
curl "$HOST/v1/variantes/buscar-filtrado?termino=<ALGO>&pagina=1&size=5"
```

`precioRebaja` tiene que venir **null o ausente** en todos. La rebaja es el precio que el admin
puede decidir aplicar, no un precio de lista: publicarla en el catálogo la convertiría en el precio
de todos.

**Esto es lo importante de esta prueba** — verificar que la respuesta pública no la filtre.

### 15c — El cliente sí la ve en su pedido

Si se le cobró a precio de rebaja, el cliente **sí** ve ese monto en el detalle de su pedido: ahí
es lo que realmente pagó y tiene derecho a verlo en su comprobante.


---

# PRUEBA 16 — Boletos de rifa agrupados por perfil 🎟️

Lo que cambia: hoy cada participación obliga a recargar nombre, plataforma y perfil, y después
se ven como filas sueltas — "Facebook · juan · like" y aparte "Facebook · juan · compartió",
cuando es **la misma persona en la misma red**. Ahora la cabecera se carga una vez y se le suman
participaciones; **cada URL de lo que hizo el cliente es un boleto**.

### Antes de probar: correr la migración

```bash
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_rifa_boletos_agrupados.sql
```

Y **volver a entrar** — los permisos viajan en el JWT.

---

### 🔴 16a — La prueba que más importa: que nadie pierda chances

Esta va **primero** porque es la que puede romper una rifa de verdad. El sorteo elige **filas**
al azar (`BoletoRifaServiceImpl.sortear`), así que si el formato nuevo juntara las 3
participaciones de Juan en una sola fila, Juan pasaría de 3 chances a 1.

Cargá un perfil con 3 participaciones:

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{
    "concursanteId": 7,
    "plataforma": "FACEBOOK",
    "urlPerfil": "facebook.com/juan.perez",
    "participaciones": [
      {"urlParticipacion": "facebook.com/post/1", "motivo": "dio like"},
      {"urlParticipacion": "facebook.com/post/2", "motivo": "compartio"},
      {"urlParticipacion": "facebook.com/post/3", "motivo": "comento"}
    ]
  }'
```

**Contá las filas en la base — tienen que ser 3, no 1:**

```sql
SELECT COUNT(*) AS filas
FROM boletos_rifa
WHERE concursante_id = 7 AND url_perfil_red_social LIKE '%juan.perez%';
```

| Resultado | Qué significa |
|---|---|
| `filas = 3` | ✅ correcto — Juan tiene 3 chances en el sorteo |
| `filas = 1` | 🔴 **PARAR** — se agruparon en una sola fila y Juan perdió 2 chances |

Y que el response diga lo mismo: `totalBoletos: 3`, con 3 elementos en `participaciones`.

---

### 16b — Se ve agrupado, no en filas sueltas

```bash
curl "$API/v1/rifas/3/boletos-agrupados" -H "Authorization: Bearer $TOKEN"
```

**Un solo renglón** para Juan, con sus 3 participaciones adentro y `totalBoletos: 3`. Si salen
3 renglones de "Juan", el agrupamiento no está funcionando.

---

### 16b2 — Lo último cargado sale arriba (el problema del scroll)

Cargá un perfil nuevo con una rifa que ya tenga varios grupos y volvé a pedir el listado.

**El grupo recién cargado tiene que salir primero**, no al final. Antes salía por orden de
inserción, que es justo lo que obligaba a bajar hasta abajo y volver a subir.

Los grupos que quedaron sin participaciones (16h) van al final: no hay nada que revisar ahí.

Cada grupo trae `ultimaParticipacion` por si el front quiere reordenar de otra forma.

> El **colapsar** los renglones es del front — el back ya manda un renglón por perfil (en vez de
> uno por participación), con `totalBoletos` visible sin abrir y las participaciones adentro de
> la misma respuesta, así que expandir no pide nada más.

---

### 16c — Sumar una participación sin recargar la cabecera

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados/participaciones?plataforma=FACEBOOK&urlPerfil=facebook.com/juan.perez" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"urlParticipacion": "facebook.com/post/4", "motivo": "compartio de nuevo"}'
```

No se manda ni nombre ni concursanteId: sale del grupo. Queda en `totalBoletos: 4`.

---

### 16d — La URL repetida y los dos modos 🔑

El punto que pediste. Intentá cargar una URL que **ya está**:

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados/participaciones?plataforma=FACEBOOK&urlPerfil=facebook.com/juan.perez" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"urlParticipacion": "facebook.com/post/1", "motivo": "otra vez"}'
```

**Esperado: HTTP 409**, y el mensaje dice **de quién** es la que ya estaba:

```
La url 'facebook.com/post/1' ya esta cargada como boleto de 'Juan Perez'.
Si de verdad se repite, hay que volver a cargarla con modo 'REPETIDA_PERMITIDA'
```

Ahora la misma, pero declarando que de verdad se repite:

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados/participaciones?plataforma=FACEBOOK&urlPerfil=facebook.com/juan.perez" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"urlParticipacion": "facebook.com/post/1", "motivo": "otra vez", "modo": "REPETIDA_PERMITIDA"}'
```

**Esperado: 200.** Se carga y suma un boleto.

> **Se llena una o la otra, nunca las dos.** Una participación lleva UNA url: o se manda en modo
> `UNICA` (el back la rechaza si ya existe) o en modo `REPETIDA_PERMITIDA` (la acepta igual).
> Mandar las dos no la convierte en dos boletos — sigue siendo uno.

Si no se manda `modo`, se asume **`UNICA`**: el default protege el sorteo.

---

### 16e — Dos URLs iguales dentro del mismo alta

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{
    "concursanteId": 8, "plataforma": "INSTAGRAM", "urlPerfil": "instagram.com/ana",
    "participaciones": [
      {"urlParticipacion": "instagram.com/p/1"},
      {"urlParticipacion": "https://instagram.com/p/1/"}
    ]
  }'
```

**409.** Son la misma URL escrita distinto (`https://` y la barra final no la hacen otra).

**Y lo importante: no quedó ninguna cargada.** Es todo o nada — media carga sería peor, porque
el admin no sabría cuáles entraron:

```sql
SELECT COUNT(*) FROM boletos_rifa WHERE concursante_id = 8;   -- tiene que dar 0
```

---

### 16f — El mismo perfil escrito de seis formas es UNA sola persona

Cargá participaciones usando el perfil escrito distinto cada vez:
`facebook.com/juan.perez`, `https://facebook.com/juan.perez`, `www.facebook.com/juan.perez/`,
`FACEBOOK.COM/Juan.Perez`, con espacios al principio…

Todas tienen que caer en **el mismo renglón**. Si abren renglones nuevos, la pantalla vuelve a
las filas sueltas y el rediseño no sirvió de nada.

---

### 16g — Facebook e Instagram del mismo cliente son dos grupos

Cargá a Juan también en Instagram. En el listado salen **dos renglones** (uno por red), cada uno
con su total. Es la regla R2 tal cual: la clave es (plataforma + perfil).

---

### 16h — Quitar una participación

```bash
curl -X DELETE "$API/v1/rifas/3/boletos-agrupados/participaciones/<boletoId>" \
  -H "Authorization: Bearer $TOKEN"
```

El `boletoId` sale del listado de 16b. Baja el `totalBoletos` en 1 y la fila desaparece de
`boletos_rifa`.

**Quitar la última no es un error:** el grupo queda con `totalBoletos: 0` y se devuelve igual.
El cliente sigue en la rifa por sus otras redes.

---

### 16i — Un perfil sin ninguna URL no se guarda

```bash
curl -X POST "$API/v1/rifas/3/boletos-agrupados" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"concursanteId": 9, "plataforma": "TIKTOK", "urlPerfil": "tiktok.com/@ana", "participaciones": []}'
```

**400.** Un perfil sin participaciones es una cabecera vacía, no un boleto — la misma idea que
el artículo vacío de la Prueba 14.

---

### 16j — Los boletos viejos siguen funcionando

Esto es lo que verifica que **no se migró nada** (decisión D1). Una rifa con boletos cargados
**antes** de este cambio:

1. `GET /v1/rifas/{id}/boletos-agrupados` los muestra agrupados igual (ya eran una fila por
   participación, así que entran solos).
2. La pantalla vieja (`/v1/boletoRifa/...`) los sigue mostrando como siempre.
3. **Girar la rifa da el mismo resultado que antes**: el sorteo no se tocó.

---

### 16k — Los permisos

| Quién | Qué pasa |
|---|---|
| Sin la migración corrida | **403** en los tres endpoints de escritura, admin incluido |
| Rol con Ver en `rifas/boletos` pero sin las acciones | ve el listado (`GET`), **403** al cargar o quitar |
| `ROLE_ADMIN` después de la migración **y de volver a entrar** | los tres funcionan |

Que estén dadas de alta:

```sql
SELECT a.clave, a.etiqueta, GROUP_CONCAT(r.nombre_rol) AS roles
FROM accion_submenu a
JOIN submenu s ON s.id = a.submenu_id
LEFT JOIN rol_accion ra ON ra.accion_submenu_id = a.id
LEFT JOIN roles r ON r.id = ra.rol_id
WHERE s.ruta = 'rifas/boletos'
  AND a.clave IN ('cargar-boletos-agrupado', 'agregar-participacion', 'quitar-participacion')
GROUP BY a.id, a.clave, a.etiqueta;
```

**3 filas, todas con `ROLE_ADMIN`.** Si sale 0, revisá que la ruta del submenú sea de verdad
`rifas/boletos` — un `INSERT ... SELECT` sobre una ruta que no existe inserta 0 filas **sin
marcar error**.

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
- [ ] **P13** Migración de artículos corrida → las 3 acciones existen
- [ ] **P13a** Agregar un artículo → baja stock en artículo y modelo, sube el total
- [ ] **P13a2** Agregar lo mismo dos veces → una línea con la cantidad sumada
- [ ] **P13b** Precio inventado → rechazado **y el stock no se movió**
- [ ] **P13c** Cambiar artículo (sin promoción) → devuelve el viejo, descuenta el nuevo
- [ ] **P13d** Cambio que rompe el combo → **409** con las 2 opciones y el pedido intacto
- [ ] **P13d2** `QUITAR_PROMOCION` → sale el combo entero, lo ajeno no se toca
- [ ] **P13d3** `CONSERVAR_PROMOCION` → no se borra nada, se suma aparte
- [ ] **P13d4** Cambio dentro del combo → 200, al precio del combo, promoción intacta
- [ ] **P13e** Quitar promoción completa → vuelve su stock
- [ ] **P13e2** Vaciar el pedido → rechazado
- [ ] **P13f** Botón `−` sobre una línea de promoción → ahora rechazado
- [ ] **P13g** 3 permisos separados: solo "agregar" → agrega pero no cambia ni quita
- [ ] **P13h** Pedido entregado/cancelado → los 3 endpoints rechazan
- [ ] **P13i** Pedido con abonos → `saldo` negativo y `total_pagado` intacto
- [ ] **P14a** Base vaciado + 2 tallas → guarda **2**, no 3
- [ ] **P14b** Solo el formulario base → guarda 1 (no se rompió el caso simple)
- [ ] **P14b2/b3** Sin talla pero con stock / con foto → se guardan
- [ ] **P14b4** Todos vacíos → mensaje claro, no crea basura
- [ ] **P14c** Editar un artículo existente vaciándole campos → NO se descarta
- [ ] **P14d** Tallas sin categoría → heredan la del modelo
- [ ] **P14d2** Una con categoría propia → conserva la suya
- [ ] **P15a** Admin → la card trae `precio` y `precioRebaja`
- [ ] **P15b** 🔒 Cliente → `precioRebaja` NO viaja
- [ ] **P16a** 🔴 3 participaciones → **3 filas** en `boletos_rifa`, no 1 (nadie pierde chances)
- [ ] **P16b** El listado sale agrupado: 1 renglón por perfil, no 3
- [ ] **P16b2** El grupo recién cargado sale **arriba**, no al final de la lista
- [ ] **P16c** Sumar participación sin recargar nombre ni perfil
- [ ] **P16d** URL repetida → **409**, y con `REPETIDA_PERMITIDA` → 200
- [ ] **P16e** Dos URLs iguales en el mismo alta → 409 y **no quedó ninguna** cargada
- [ ] **P16f** El perfil escrito de 6 formas cae en **un solo** renglón
- [ ] **P16g** Facebook e Instagram del mismo cliente → 2 renglones
- [ ] **P16h** Quitar la última participación → grupo vacío, sin error
- [ ] **P16i** Perfil sin ninguna URL → 400
- [ ] **P16j** 🔴 Rifa vieja: se ve igual y **girar da el mismo resultado que antes**
- [ ] **P16k** 3 permisos de rifa dados de alta y funcionando
- [ ] `SELECT COUNT(*) FROM producto WHERE stock < 0` → **0**

---

# Lo que este fix NO resuelve

Para que no lo busques en estas pruebas:

| Pendiente | Prioridad |
|---|---|
| Campo "stock disponible" **en pantalla** | back ✅ hecho (Prueba 9) — falta el front |
| Modal para cambiar la forma de cobro | back ✅ hecho (Prueba 12) — falta el front |
| Agregar / cambiar artículo en un pedido | back ✅ hecho (Prueba 13) — falta el front |
| Precio diferente (rebaja) al **cobrar** | back ✅ hecho (Pruebas 7 y 13b) |
| Precio diferente (rebaja) en la **card de tienda** | back ✅ hecho (Prueba 15) — falta el front |
| Contador de artículos que dice 3 con 2 | back ✅ hecho (Prueba 14) — el front ya no manda el vacío |
| ~~Precios validados en el back~~ | ✅ **hecho** — ver Prueba 7 |
| ~~Promociones con apartado y tarjeta~~ | ✅ **hecho** — ver Prueba 8 |
| ~~Búsqueda por código exacto primero~~ | ✅ **hecho** — Prueba 10 |
| ~~`mis-pedidos` con código, nombre y foto~~ | ✅ **hecho** — Prueba 11 |
| Buscador blanco en modo día | P4 — front |
| Rifa: colapsar concursantes y cargar por participación | back ✅ hecho (Prueba 16) — falta el front |
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

---

# 📜 Scripts que hay que ejecutar — en este orden

Ninguno de estos corre solo. **Sin ellos los botones nuevos responden 403 a todo el mundo**, así
que van antes de empezar a probar. Los cuatro son idempotentes (`NOT EXISTS` en cada INSERT):
volver a correrlos no duplica nada.

Todos viven en `src/main/resources/static/`.

| # | Script | Qué da de alta | Sin él |
|---|---|---|---|
| 1 | `migration_accion_tienda_eliminar.sql` | acción `eliminar` en `tienda/buscar` | pendiente de antes (ver el registro en `CLAUDE.md`) |
| 2 | `migration_accion_pedido_cambiar_tipo.sql` | `cambiar-tipo` | no se puede cambiar la forma de cobro (Prueba 12) |
| 3 | `migration_accion_pedido_articulos.sql` | `agregar-articulo`, `cambiar-articulo`, `quitar-promocion` | no se pueden editar los artículos (Prueba 13) |
| 4 | `migration_accion_rifa_boletos_agrupados.sql` | `cargar-boletos-agrupado`, `agregar-participacion`, `quitar-participacion` | no se pueden cargar boletos agrupados (Prueba 16) |

### En QA (cubre dev y qa — las dos apuntan a la misma base)

```bash
cd <raíz del repo>
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_tienda_eliminar.sql
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_pedido_cambiar_tipo.sql
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_pedido_articulos.sql
mysql -h <HOST> -u <USER> -p inventario_key_qa < src/main/resources/static/migration_accion_rifa_boletos_agrupados.sql
```

### En producción — solo cuando QA apruebe

Misma lista, cambiando la base a `inventario_key` (sin sufijo):

```bash
mysql -h <HOST> -u <USER> -p inventario_key < src/main/resources/static/migration_accion_pedido_cambiar_tipo.sql
mysql -h <HOST> -u <USER> -p inventario_key < src/main/resources/static/migration_accion_pedido_articulos.sql
mysql -h <HOST> -u <USER> -p inventario_key < src/main/resources/static/migration_accion_rifa_boletos_agrupados.sql
```

### Verificación — las 7 acciones nuevas juntas

```sql
SELECT a.clave, a.etiqueta, a.categoria, a.orden,
       GROUP_CONCAT(r.nombre_rol) AS roles_que_la_tienen
FROM accion_submenu a
JOIN submenu s     ON s.id = a.submenu_id
LEFT JOIN rol_accion ra ON ra.accion_submenu_id = a.id
LEFT JOIN roles r       ON r.id = ra.rol_id
WHERE (s.ruta = 'pedidos/mis-pedidos'
       AND a.clave IN ('cambiar-tipo', 'agregar-articulo', 'cambiar-articulo', 'quitar-promocion'))
   OR (s.ruta = 'rifas/boletos'
       AND a.clave IN ('cargar-boletos-agrupado', 'agregar-participacion', 'quitar-participacion'))
GROUP BY a.id, a.clave, a.etiqueta, a.categoria, a.orden
ORDER BY s.ruta, a.orden;
```

Tienen que salir **7 filas** (4 de pedidos + 3 de rifas), todas con `ROLE_ADMIN`. Si alguna sale con
`roles_que_la_tienen = NULL`, la acción existe pero nadie la tiene: el botón no le aparece ni al
admin.

### ⚠️ Después de correr cualquiera de estos, hay que volver a entrar

Los permisos viajan **dentro del JWT**. Un token emitido antes de la migración no trae la
autoridad nueva, así que el botón sigue dando 403 aunque el script haya corrido bien. Cerrar
sesión y entrar de nuevo (o esperar al refresh) antes de decir que no funcionó.

### Lo que NO es un script de estos

`migracion.sql` y los `UPDATE` de stock que aparecen en este documento **no** son parte de esta
lista. El ajuste de inventario descuadrado necesita una decisión producto por producto (ver
`hexagonal/stock/README.md`) y ya causó un incidente en producción el 2026-09-22 por correrse
antes de validarlo. No se corre "por las dudas".
