# Análisis de rendimiento — búsqueda de productos y variantes

**Fecha:** 2026-07-31 · **Rama:** `dev` · **Estado:** Etapa 1 aplicada (4 de 5) — ver tabla

## Estado de las correcciones

| # | Hallazgo | Estado | Commit |
|---|---|---|---|
| 1 | `getAll` descarta una query | ✅ Corregido | `6a19559` |
| 2 | `/tienda/v1/buscar` no cachea | ✅ Corregido | `db6e8bf` |
| 9 | `readOnly = true` | ✅ Corregido | `5ba0b07` |
| 10 | Keys con `getAuthorities()` | ✅ Corregido | `8be4af8` |
| 7 | Índice en `variante_imagen` | ⏳ Requiere BD | script: `verificar_indices_busqueda.sql` |
| 3, 4, 5, 6, 8 | Etapas 2 y 3 | ⬜ Pendientes | — |
| **11** | **Abonos no invalidaban el caché al mover stock** | ✅ Corregido | `pendiente` |

Cada corrección va en **su propio commit**, con el motivo, lo que se verificó antes de tocar, y un
bloque *"SI ALGO FALLA EN PRUEBAS"* con los síntomas a vigilar. Si una prueba falla, se revierte
ese commit solo — el resto sigue en pie.

Recorrido completo controlador → servicio → repositorio → query de los cuatro endpoints de búsqueda:

| Endpoint | Servicio | Query |
|---|---|---|
| `GET /productos/obtenerProductos` | `ProductosServiceImpl.getAll` | `findAll` / `findConStockYImagenPublico` |
| `GET /productos/buscarNombreOrCodigoBarra` | `ProductosServiceImpl.findNombreOrCodigoBarra` | `buscarProductosAdmin` |
| `GET /tienda/v1/buscar` | `VarianteServiceImpl.buscarVariantes` | `buscarVariantesPublicoFiltrado` / `filtrarVariantesAdmin` |
| `GET /tienda/v1/buscar-filtrado` | `VarianteServiceImpl.buscarVariantesPublicoFiltrado` | `buscarVariantesPublicoFiltrado` |

---

## Resumen

| # | Sev | Hallazgo | Esfuerzo |
|---|---|---|---|
| 1 | 🔴 | `getAll` ejecuta una query completa y **tira el resultado** en todo request público | Trivial |
| 2 | 🔴 | `/tienda/v1/buscar` **no cachea nada** — self-invocation rompe el proxy de `@Cacheable` | Trivial |
| 3 | 🔴 | Todas las relaciones son **EAGER**: cada página arrastra una cascada de entidades | Medio |
| 4 | 🟠 | `LIKE '%termino%'` con comodín inicial → **full scan**, ningún índice puede usarse | Medio/Alto |
| 5 | 🟠 | `LOWER()` sobre columnas impide usar índices (y en MySQL `_ci` es redundante) | Bajo |
| 6 | 🟡 | El `countQuery` repite el `WHERE` pesado → cuesta el doble | Medio |
| 7 | 🟡 | `EXISTS` correlacionado evaluado fila por fila | Bajo |
| 8 | 🟡 | Búsqueda sin resultados lanza excepción → nunca se cachea y devuelve error en vez de lista vacía | Bajo |
| 9 | 🔵 | `getAll` sin `@Transactional(readOnly = true)` | Trivial |
| 10 | 🔵 | Las keys de caché incluyen `getAuthorities()` completo | Bajo |

---

# 🔴 Alto impacto

## 1. `getAll` ejecuta una query completa y tira el resultado

**Ubicación:** `ProductosServiceImpl.java:139-145`

```java
Page<Producto> productosPaginados = iProductosRepository.findAll(pageable);   // ← siempre se ejecuta
boolean isAdmin = isAdminContext();
if (!isAdmin) {
    productosPaginados = iProductosRepository.findConStockYImagenPublico(pageable);  // ← la pisa
}
```

`findAll(pageable)` corre **siempre**, y para cualquier usuario que no sea admin su resultado se
descarta inmediatamente. Como es un `Page`, son **dos** viajes a la base: el `SELECT` paginado y su
`COUNT(*)`.

**Impacto:** el catálogo público es el endpoint más llamado del sistema. Está pagando el doble de
queries de las que necesita, más el `COUNT(*)` sobre la tabla completa de productos sin filtro.

**Fix:** mover la query dentro del `if/else`. Es mover dos líneas.

```java
boolean isAdmin = isAdminContext();
Page<Producto> productosPaginados = isAdmin
        ? iProductosRepository.findAll(pageable)
        : iProductosRepository.findConStockYImagenPublico(pageable);
```

---

## 2. `/tienda/v1/buscar` no cachea nada

**Ubicación:** `VarianteServiceImpl.java:94-112`

`buscarVariantes()` **no tiene** `@Cacheable`. Delega en dos métodos que **sí** la tienen
(`filtrarVariantesAdmin` línea 746, `buscarVariantesPublicoFiltrado` línea 764)… pero las llama
**directamente**:

```java
PginaDto<List<VarianteResumenDto>> resultado = AuthenticationUtils.isAdminContext()
        ? filtrarVariantesAdmin(termino, null, null, null, null, page, size)
        : buscarVariantesPublicoFiltrado(termino, null, null, null, null, null, page, size);
```

Es el mismo problema de proxy que con `@Transactional`: en una llamada interna (`this.metodo()`)
**el proxy de Spring no interviene**, así que la anotación no se aplica. El caché existe, está
configurado, y **nunca se usa** por esta vía.

**Consecuencia:** cada búsqueda del buscador principal pega a la base con la query más cara del
sistema. `/tienda/v1/buscar-filtrado` sí cachea, porque el controlador llama al método directo.

**Fix (elegir uno):**
- Poner `@Cacheable` en `buscarVariantes()` con su propia key.
- O extraer los dos métodos a otro bean, para que la llamada pase por el proxy.

La primera es más simple. Ojo con el hallazgo 8: mientras el método lance excepción al no
encontrar nada, esas búsquedas seguirán sin cachearse.

---

## 3. Todas las relaciones son EAGER

**Ubicación:** `Variantes.java:20-22`, `Producto.java:50-52`, `VarianteImagen.java:21-27`

```java
@ManyToOne                       // sin fetch → EAGER (el default de @ManyToOne)
private Producto producto;

@OneToOne(optional = true, ...)  // sin fetch → EAGER (el default de @OneToOne)
private CodigoBarra codigoBarras;

@ManyToOne(fetch = FetchType.EAGER)   // explícito
private PalabraClave palabraClave;
```

En una `@Query` JPQL sin `JOIN FETCH`, Hibernate **no puede** resolver EAGER con un join: trae las
entidades y luego dispara **selects adicionales** para cada relación. Es el N+1 clásico, sólo que
disfrazado — no aparece en el código, aparece en el log de SQL.

**Dónde más duele:** `buildResumenDtosBatch` (`VarianteServiceImpl.java:667`) parece resuelto —
batchea las imágenes con `findByVarianteIdIn`. Pero:

```java
for (VarianteImagen vi : todasImagenes) {
    variantePrimeraImagen.putIfAbsent(vi.getVariante().getId(), vi.getImagen().getId());
}
```

`VarianteImagen.variante` e `.imagen` son EAGER. Para leer **dos IDs que ya están en las columnas
FK**, Hibernate carga cada `Variantes` completa → que a su vez carga su `Producto` → que carga su
`CodigoBarra` → más la `PalabraClave`. Por cada imagen de la página.

**Fix:**
1. Pasar las relaciones a `LAZY` y agregar `JOIN FETCH v.producto` (y `LEFT JOIN FETCH
   p.codigoBarras`) en las queries paginadas que sí necesitan esos datos.
2. Para el mapa de imágenes, no traer entidades: una proyección que devuelva sólo los dos IDs.

```java
@Query("SELECT vi.variante.id, vi.imagen.id FROM VarianteImagen vi WHERE vi.variante.id IN :ids " +
       "ORDER BY CASE WHEN vi.principal = true THEN 0 ELSE 1 END ASC, vi.id ASC")
List<Object[]> findPrimeraImagenIdsByVarianteIdIn(@Param("ids") List<Integer> ids);
```

⚠️ Cambiar EAGER→LAZY es el cambio de **mayor impacto y mayor riesgo** de esta lista: puede
provocar `LazyInitializationException` en cualquier otro punto del código que hoy dependa del EAGER.
Hay que hacerlo con calma y revisando todos los usos, no junto con el resto.

Lo mismo aplica a `getPrimerasImagenes` (`ProductosServiceImpl.java:193-198`), que hace
`pi.getProducto().getId()` y `pi.getImagen().getId()` sobre entidades completas.

---

# 🟠 Medios

## 4. `LIKE '%termino%'` → full scan

**Ubicación:** `IVarianteRepository.java:152-157`, `IProductosRepository.java:~78-82`

```sql
LOWER(v.producto.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
```

El comodín **al inicio** hace imposible usar un índice B-tree: la base recorre toda la tabla. Es el
techo real del buscador — mientras esto siga así, ningún índice ayuda al `termino`.

Y no es una condición, son **cuatro en OR** (nombre, marca, palabra clave, código de barras), cada
una con su `LIKE '%...%'`.

**Opciones, de menor a mayor esfuerzo:**

| Opción | Qué implica | Cuándo conviene |
|---|---|---|
| Índice **FULLTEXT** de MySQL + `MATCH ... AGAINST` | Migración SQL + cambiar la query a nativa | La solución correcta para buscar texto |
| Prefijo en vez de infijo (`'termino%'`) | Cambio de una línea, **sí** usa índice | Si buscar "por dónde empieza" alcanza para el negocio |
| Dejarlo | — | Si la tabla es chica (unos miles de filas), esto no se nota |

**Antes de decidir hace falta un dato:** cuántas filas tienen `producto` y `variantes` hoy. Con
2.000 productos, un full scan es imperceptible y no vale la pena tocar nada. Con 200.000, es el
problema principal.

## 5. `LOWER()` sobre columnas

```sql
AND (:talla IS NULL OR LOWER(v.talla) = LOWER(:talla))
```

Aplicar una función a la columna **invalida cualquier índice** sobre ella. Y en MySQL, si la
collation es `_ci` (case-insensitive, que es el default en `utf8mb4_general_ci` /
`utf8mb4_unicode_ci`), `LOWER()` es **redundante**: la comparación ya ignora mayúsculas.

**Fix:** quitar `LOWER()` de las igualdades (`talla`, `color`, `marca`) para que puedan usar índice.

**Verificar primero:**
```sql
SELECT table_name, table_collation FROM information_schema.tables
WHERE table_schema = 'inventario_key_qa' AND table_name IN ('producto','variantes');
```
Si la collation termina en `_ci`, se puede quitar sin cambiar el comportamiento.

## 6. El `countQuery` repite el `WHERE` pesado

**Ubicación:** `IVarianteRepository.java:164-179`

El `countQuery` está declarado con **exactamente el mismo `WHERE`** que la query principal,
incluidos los cuatro `LIKE '%...%'` y el `EXISTS`. Cada búsqueda paga ese costo **dos veces**: una
para traer 10 filas y otra para contar el total.

**Opciones:**
- Devolver `Slice` en vez de `Page` donde el front no necesite el total exacto (elimina el count).
- Cachear el count por separado, con TTL más largo que los resultados.
- Si el front sólo usa "hay más páginas", `Slice` es suficiente y ahorra la mitad del trabajo.

**Dato que hace falta:** ¿el front muestra "página 3 de 47" / total de resultados, o sólo un botón
"ver más"? Si es lo segundo, `Slice` es dinero fácil.

## 7. `EXISTS` correlacionado

```sql
AND EXISTS (SELECT 1 FROM VarianteImagen vi WHERE vi.variante = v)
```

Se evalúa por cada fila candidata. Con un índice en `variante_imagen.variante_id` el costo baja
mucho; sin él, es una tabla recorrida por fila.

**Verificar:** `SHOW INDEX FROM variante_imagen;` — si no hay índice en `variante_id`, agregarlo es
de las mejoras más baratas de toda esta lista.

## 8. Sin resultados = excepción

**Ubicación:** `VarianteServiceImpl.java:108-110`, `ProductosServiceImpl.java:237-239`

```java
if (resultado.getT().isEmpty()) {
    throw new ExceptionDataNotFound("No se encontraron variantes con la búsqueda: ...");
}
```

Dos efectos:
1. **Nunca se cachea una búsqueda sin resultados** — la excepción impide que `@Cacheable` guarde
   nada. Justo las búsquedas "raras" (las que un bot o un usuario curioso repite) pegan siempre a
   la base.
2. Para un buscador, "no encontré nada" es un resultado normal, no un error. Una lista vacía con
   200 suele ser mejor contrato que un 404.

Cambiar esto **afecta al front** (hoy espera el error), así que va coordinado y documentado en
`CAMBIOS_FRONT.md`. El endpoint `/tienda/v1/buscar-filtrado` ya devuelve lista vacía — o sea que el
comportamiento hoy es **inconsistente entre los dos buscadores**.

---

# 🔵 Menores

## 9. `getAll` sin `@Transactional(readOnly = true)`

`findNombreOrCodigoBarra` lo tiene (línea 220), `getAll` no. En una lectura, `readOnly = true` le
evita a Hibernate mantener el estado para *dirty checking*.

## 10. Las keys de caché incluyen `getAuthorities()` completo

```java
key = "#page + ':' + #size + ':' + T(...SecurityContextHolder).getContext().getAuthentication().getAuthorities()"
```

La key termina conteniendo la lista completa de roles y permisos serializada. Dos problemas: las
keys se vuelven enormes, y **dos usuarios con los mismos permisos en distinto orden generan keys
distintas** → el mismo resultado se guarda dos veces y baja el *hit rate*.

**Fix:** usar el booleano que ya se calcula — `isAdmin` — en lugar de la colección entera.

---

# Plan sugerido

**Etapa 1 — sin riesgo, alto retorno (una sesión corta):**
1. Hallazgo 1: mover la query dentro del `if` en `getAll`.
2. Hallazgo 2: `@Cacheable` en `buscarVariantes`.
3. Hallazgo 9: `readOnly = true`.
4. Hallazgo 10: `isAdmin` en vez de `getAuthorities()`.
5. Hallazgo 7: índice en `variante_imagen.variante_id` si falta.

Sin cambios de contrato, sin riesgo para el front. El 1 y el 2 solos ya deberían notarse.

**Etapa 2 — requiere medir primero:**
6. Hallazgo 5: quitar `LOWER()` (confirmar collation).
7. Hallazgo 6: `Slice` en vez de `Page` (confirmar si el front usa el total).
8. Hallazgo 8: lista vacía en vez de excepción (coordinar con el front).

**Etapa 3 — proyecto aparte, con cuidado:**
9. Hallazgo 3: EAGER → LAZY + `JOIN FETCH`. Toca muchos puntos; conviene hacerlo solo.
10. Hallazgo 4: FULLTEXT, **sólo si el volumen lo justifica**.

---

# Datos que hacen falta para cerrar el análisis

No se pueden sacar del código; salen de la base o del front:

| Dato | Para qué | Cómo obtenerlo |
|---|---|---|
| Filas en `producto` y `variantes` | Decidir si el hallazgo 4 (FULLTEXT) vale la pena | `SELECT COUNT(*) FROM producto; SELECT COUNT(*) FROM variantes;` |
| Índices existentes | Hallazgos 5 y 7 | `SHOW INDEX FROM variantes; SHOW INDEX FROM variante_imagen; SHOW INDEX FROM producto;` |
| Collation de las tablas | Hallazgo 5 | La consulta a `information_schema` de arriba |
| Si el front muestra el total de resultados | Hallazgo 6 (`Slice`) | Preguntar al front |
| Tiempo real de las búsquedas hoy | Priorizar de verdad | Log de la app o `EXPLAIN` sobre la query |

**Sin estos datos, la Etapa 1 se puede hacer igual** — no dependen de ninguno.

---

# 🔴 11. Vender no invalida el caché del catálogo (hallazgo nuevo, 2026-07-31)

Detectado a partir de una pregunta muy pertinente: *¿qué pasa si un usuario tiene una variante
cacheada con stock 1 y el admin ya la vendió?*

## ⚠️ Corrección del análisis inicial (mismo día)

La primera versión de este hallazgo decía que **ningún** flujo de venta invalidaba el caché. **Era
incorrecto.** El grep inicial buscaba `evictAllCaches` y `@CacheEvict`, y no capturó la forma que
usa la mayoría del proyecto: `cacheService.evictAll()`.

## Qué se verificó (corregido)

| Acción | ¿Invalida caché? | Dónde |
|---|---|---|
| Admin **crea / edita / elimina** variante | ✅ Sí | `VarianteServiceImpl`, `evictAllCaches()` ×8 |
| Admin cambia **imágenes** | ✅ Sí | `ImagenServiceImpl:154` |
| **Pedido** — crear (baja stock) | ✅ Sí | `PedidoServiceImpl:210` + aviso por Rabbit |
| **Pedido** — cancelar (devuelve stock) | ✅ Sí | `PedidoServiceImpl:422` |
| **Pedido** — eliminar detalle (devuelve stock) | ✅ Sí | `PedidoServiceImpl:478` |
| **Venta** | ✅ Sí | `VentaServiceImpl:258,322` |
| **Abono** — cancelar (devuelve stock) | ❌ **NO** | `AbonoServiceImpl:250-253` |
| **Abono** — transferir (baja stock) | ❌ **NO** | `AbonoServiceImpl:343,346` |

`evictAllCaches()` y `cacheService.evictAll()` limpian **todos** los cachés sin distinguir rol, así
que la pregunta original —"¿el caché del admin y el del cliente se pisan?"— está cubierta: se
limpian ambos a la vez.

**El agujero real era mucho más chico de lo que decía el análisis inicial: sólo los abonos.**
`AbonoServiceImpl` era la única clase que mueve stock sin inyectar `CacheService` ni declarar
ningún evict.

## ✅ Lo que NO pasa: no hay sobreventa

`PedidoServiceImpl:157-166` protege bien el dato real:

```java
Variantes variante = iVarianteRepository.findByIdWithLock(mpa.getVarianteId())   // bloqueo pesimista
if (variante.getStock() < mpa.getCantidad()) {
    throw new RuntimeException("Stock insuficiente en variante id " + ...);
}
```

Hay **bloqueo pesimista** (`findByIdWithLock`, un `SELECT ... FOR UPDATE`) y validación de stock
dentro de la transacción. Dos compras simultáneas se serializan; el stock **nunca queda negativo**
y nunca se vende algo que no existe.

**El impacto real es de experiencia, no de datos:** el cliente ve el producto disponible, lo agrega
al carrito y al confirmar recibe *"Stock insuficiente"*. Frustrante y erosiona la confianza, pero
no se pierde dinero ni se corrompe el inventario.

## ⚠️ El hallazgo 2 agrava esto

Antes, `/tienda/v1/buscar` **no cacheaba** (el bug de self-invocation), así que ese endpoint era el
único que siempre mostraba stock fresco. Al arreglarlo (commit `db6e8bf`) pasó a cachear como los
demás — y por lo tanto **heredó este problema**.

No invalida la corrección: el endpoint estaba pegando a la base en cada request con la query más
cara del sistema. Pero el hallazgo 11 pasa de "conviene arreglarlo" a "hay que arreglarlo junto".

## Qué se hizo

`@CacheEvict` acotado en los dos métodos de `AbonoServiceImpl` que mueven stock
(`cancelarPedido` y `transferirAbono`). Ambos son públicos y entran desde `AbonoController`, así
que el proxy de Spring sí interviene — al contrario del hallazgo 2, aquí no hay self-invocation.

Los nombres de caché salieron a `Utils/CacheNames.java` como constantes, para no repetir seis
literales en cada anotación y que se note si alguno se agrega o renombra.

**Por qué acotado y no `evictAll()`:** el resto del proyecto usa `cacheService.evictAll()`, que
borra **todos** los cachés — incluidos tipos de pago, tarifas, IVA e imágenes de presentación, que
no tienen nada que ver con el stock y son caros de reconstruir. Aquí se limpian sólo los seis que
contienen stock. El más importante de ellos es `findByIdCache` (detalle de producto): tiene **TTL
de 6 horas**, el más largo de todos, y sí incluye el stock.

**Queda pendiente, como mejora:** migrar `PedidoServiceImpl` y `VentaServiceImpl` del `evictAll()`
global a este mismo evict acotado. Hoy funcionan correctamente —invalidan de más, no de menos—,
así que no es un bug, sólo desperdicio de caché en cada venta.

