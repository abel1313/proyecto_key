# Tests pendientes — back (proyecto_key)

Regla en `CLAUDE.md` (2026-10-06): ya no se escriben tests automáticos mientras el código cambia
tanto. Cada cambio nuevo deja aquí **qué test le falta y qué tiene que comprobar**, para escribirlos
el día que se decida. Lo más nuevo va arriba.

Formato de cada entrada:

```
### AAAA-MM-DD — Qué se hizo
**Dónde:** clase.metodo() / endpoint
**Tipo:** unitario (dominio/servicio con puertos simulados) · MySQL (SQL nativo) · controller (URL y status)
**Debe comprobar:**
- [ ] caso → resultado esperado
```

---

### 2026-10-06 — Permisos de Mis pedidos al día (`migration_accion_pedidos_filtros_y_cobro.sql`)
**Dónde:** catálogo `accion_submenu` / `rol_accion` de `pedidos/mis-pedidos` · `GET /v1/accion-submenu/**`
**Tipo:** MySQL (script sobre esquema real) + controller del catálogo
**Debe comprobar:**
- [ ] Sobre una base con las migraciones anteriores (18 acciones), queda con 27 y todas con ROLE_ADMIN.
- [ ] Correrla dos veces deja exactamente las mismas filas (etiqueta, descripción, categoría, orden, roles).
- [ ] Corre con `SQL_SAFE_UPDATES = 1` y al final lo deja en 1.
- [ ] Un rol que ya tenía `abonar` y `cobrar` los conserva y no recibe ninguna de las 9 nuevas.
- [ ] Las acciones de una misma categoría quedan con `orden` seguido (1–3, 4–8, 9–14, 15–20, 21–27); ninguna `orden` repetida.
- [ ] Si no existe el submenú `pedidos/mis-pedidos`, no inserta nada y no falla.
- [ ] `GET /v1/accion-submenu/...` devuelve las nuevas con su categoría para Gestión de roles.

### 2026-10-06 — Botón "−" quita la línea exacta (`detalleId`) y rechaza cantidad < 1
**Dónde:** `PedidoServiceImpl.eliminarDetallePedido(pedidoId, productoId, cantidad, detalleId)` ·
`DELETE /v1/pedidos/{pedidoId}/detalle/{productoId}?cantidad=&detalleId=`
**Tipo:** unitario del servicio (repositorios simulados) + controller
**Debe comprobar:**
- [ ] Pedido con dos tallas del mismo modelo (líneas 10 y 11, mismo `productoId`): con `detalleId=11` baja la línea 11, la 10 no se toca.
- [ ] El stock que sube es el de la variante de la línea 11, y el del producto sube lo mismo.
- [ ] Mismo artículo en una línea de promoción y en una suelta: con `detalleId` de la suelta, la quita sin decir "es parte de una promoción".
- [ ] `detalleId` de una línea que es de otro `productoId` → error "La linea N del pedido #P no es de ese producto" y no mueve stock.
- [ ] Sin `detalleId` funciona como antes (la primera línea de ese producto): compatibilidad con el front de prod.
- [ ] `cantidad=0` y `cantidad=-2` → 400 "La cantidad a quitar tiene que ser al menos 1"; ni la línea ni el stock cambian.
- [ ] Quitar la única pieza de la única línea → sigue rechazando ("es el ultimo articulo…").

### 2026-10-06 — Filtros de pedidos unidos por el grupo (R14 de `busquedapedido`)
**Dónde:** `PedidosFiltradosJdbcAdapter` · `GET /v1/pedidos/buscar`
**Tipo:** MySQL (`BusquedaPedidosMysqlTest`) — **ya escrito ese día**, aquí solo lo que faltó:
- [ ] Grupo con el **titular cancelado** y otro pedido debiendo: sale en "Por cobrar", no en "Cancelado".
- [ ] Grupo de **contado** con un pedido Entregado y otro Pendiente: sale en "Pendiente"; cuando todos están Entregados, en "Entregado".
- [ ] "Saldo a favor" cuando solo un miembro del grupo pagó de más (el titular no).
- [ ] "Con promoción" / "Ramos" cuando solo un miembro del grupo los tiene.

### 2026-10-06 — ⇄ con todas las piezas suma a la línea que ya está (R6)
**Dónde:** `EditarArticulosService.cambiarLineaNormal()` / `quitarComboYAgregar()` · `PedidoEditable.lineaDondeSumar()`
**Tipo:** unitario — **ya escrito ese día** (`EditarArticulosServiceTest`). Faltó:
- [ ] Adaptador JPA: después de sumar y borrar la línea vieja, `PedidoEditable.total()` releído no cuenta la línea borrada (prueba de repositorio con H2 o MySQL).

---

## Tests existentes que quedaron viejos

| Test | Rama | Desde | Por qué falla |
|---|---|---|---|
| `RenombreArticuloRutasTest`, `RenombreArticuloSecurityTest` | `feature/tema-jade-articulo` | 2026-10-06 | Prueban rutas `/v2/articulos/...` que esa rama del back todavía no tiene. No llegan a `dev`/`qa`. |
