# Dominio: stock

Reglas del dominio. **Se acuerdan antes de escribir código** (ver
`hexagonal/_plantilla/README.md`, PASO 0).

Estado: ⚠️ **hay una decisión abierta que bloquea el modelado** — ver "Duda 1".

---

## Por qué existe este dominio

Hoy la lógica de stock está repartida en dos servicios que **se contradicen**:

| Dónde | Qué hace |
|---|---|
| `VarianteServiceImpl.validarStockContraProducto()` :592 | `findByProductoId()` → cuenta **todas** las variantes, incluidas las dadas de baja |
| `ProductosServiceImpl.ajustarVariantesSiExceden()` :731 | `findByProductoIdAndHabilitado('1')` → cuenta **solo las habilitadas** |

Consecuencia real en producción (2026-09-22): una variante dada de baja sigue
reteniendo su stock, así que `Disponible: 0` aunque el producto tenga margen. El
admin tiene que inflar el stock del producto a mano para poder editar cualquier
variante.

---

## ⚠️ Duda 1 — ¿Qué significa `producto.stock`? (BLOQUEANTE)

El código lo trata de dos formas a la vez y hay que elegir una:

**Opción A — `producto.stock` es el TOTAL**
```
producto.stock = 10        (todo lo que hay en bodega)
variantes habilitadas = 4  (repartido en variantes)
disponible para variantes nuevas = 10 - 4 = 6
```
Al crear una variante con stock 2, `producto.stock` NO cambia (sigue 10).
Es lo que asume `validarStockContraProducto`.

**Opción B — `producto.stock` es el DISPONIBLE (sin repartir)**
```
producto.stock = 6         (lo que todavía no está en ninguna variante)
variantes habilitadas = 4
total real = 6 + 4 = 10
```
Al crear una variante con stock 2, `producto.stock` baja a 4.

Hoy `ajustarStock()` :614 hace `producto.stock + diff`, que es coherente con A.
Pero el síntoma en producción se comporta como B.

**Esto define todo lo demás.** Sin cerrarlo no se puede modelar.

---

## Reglas acordadas (independientes de la Duda 1)

### R1 — El stock disponible ignora las variantes dadas de baja
```
disponible = stock_del_producto − Σ(stock de variantes con habilitado = '1')
```
Una variante deshabilitada no retiene nada.

### R2 — Editar sin tocar el stock no consume stock
Al actualizar una variante se valida el **delta**, no el total:
```
delta = stock_nuevo − stock_actual
si delta <= 0  → no hay nada que validar
si delta > 0   → delta <= disponible
```
Cambiar el nombre, color o descripción con `stock` igual **nunca** debe fallar por
stock. (Hoy falla: `validarStockContraProducto` compara el total solicitado.)

### R3 — Dar de baja una variante devuelve su stock
El stock vuelve a quedar disponible para otras variantes. Es consecuencia directa de R1
si `producto.stock` es el total (Opción A): al dejar de contar la variante, el disponible
sube solo.

### R4 — Nunca se puede dejar el stock negativo
Ni el del producto ni el de una variante.

### R5 — El precio y el total se calculan en el back
El front manda qué se vende, no cuánto cuesta. El back recalcula contra el catálogo y
las promociones vigentes, y **rechaza** si el total que mandó el front no coincide.

---

## ⚠️ Duda 2 — Variantes deshabilitadas con stock

Pedido del usuario: un modal que pregunte *"¿querés tomar el stock de una variante dada
de baja?"* y, al aceptar, se lo quite a esa variante.

Pero si R1 ya dice que una variante deshabilitada no retiene stock, **el modal sobra**:
ese stock ya estaría disponible.

Las dos lecturas posibles:
- **(a)** Deshabilitada = su stock vuelve al pozo automáticamente → no hace falta modal
- **(b)** Deshabilitada = su stock queda "congelado" en ella, y el modal es lo que lo
  libera explícitamente → hace falta modal y hace falta distinguir "deshabilitada" de
  "sin stock"

---

## Lo que expone hacia afuera

- **Consultar disponible** de un producto (campo de solo lectura en `tienda/update` y
  `productos/update`: *"10 de stock, 4 en variantes, 6 disponibles"*)
- **Validar** que un cambio de stock cabe, antes de guardarlo
- **Reservar / liberar** al vender, apartar o dar de baja

## Orden de construcción
```
modelo → excepciones → puerto salida → puerto entrada → servicio → adaptadores → controller → DTOs
```

---

## Pendiente separado: renombrar `variante` → `artículo` (2026-09-22)

Decisión tomada: en todo el sistema, lo que hoy se llama *variante* pasa a llamarse
**artículo**. Alcance: entidad, tabla, repositorios, DTOs, endpoints, mensajes de error y
todas las pantallas del front.

**Va al final del todo, a propósito.** Es un cambio mecánico pero enorme, y hacerlo
mientras el stock está descuadrado mezclaría dos problemas: cualquier error del renombre
se confundiría con un error de stock. Primero se cierra el stock, después se renombra.

---

## Incidente 2026-09-22 — el inventario ya estaba descuadrado

Se corrió en la base este UPDATE para migrar al modelo "producto.stock = disponible":

```sql
UPDATE producto p SET p.stock = p.stock - (
    SELECT COALESCE(SUM(v.stock),0) FROM variantes v
    WHERE v.producto_id = p.id AND v.habilitado = '1');
-- 126 filas cambiadas de 157
```

Dejó **stocks negativos** (`-6`, `-3`, `-2`, `-1`) y se revirtió con la suma inversa.

**Lo que enseñó:** deshaciendo la resta, varios productos tenían menos stock que la suma
de sus variantes (el 269: producto 12, variantes 18). Es decir, **el inventario ya estaba
desincronizado antes de tocar nada** — arrastre de los dos bugs de arriba.

**Consecuencia:** migrar al modelo nuevo NO es solo una resta. Antes hay que decidir, por
producto, qué dato es el verdadero:
- manda el stock del producto → hay que recortar variantes
- mandan las variantes → hay que subir el producto
- ninguno es confiable → recuento físico

Eso es decisión del negocio, no técnica. **Primero el reporte de descuadres, después la
migración.**
