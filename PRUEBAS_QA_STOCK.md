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
- [ ] `SELECT COUNT(*) FROM producto WHERE stock < 0` → **0**

---

# Lo que este fix NO resuelve

Para que no lo busques en estas pruebas:

| Pendiente | Prioridad |
|---|---|
| Campo "stock disponible" en pantalla | P2 — **necesita el repo del front**, que no está en la sesión actual |
| ~~Precios validados en el back~~ | ✅ **hecho** — ver Prueba 7 |
| Promociones con apartado y tarjeta | P2 |
| Búsqueda por código exacto primero | P3 |
| Contador de tallas que dice 3 con 2 | P3 — front |
| `mis-pedidos` con código, nombre y foto | P3 |
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
