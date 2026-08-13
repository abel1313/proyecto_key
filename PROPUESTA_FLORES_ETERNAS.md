# Flores eternas — ramos configurables

> ✅ **Backend implementado en `dev` el 2026-08-12** (sin commitear todavía): catálogos
> (`TipoFlor`, `CantidadFlorValida`, `AccesorioRamo`, `FraseListonPredefinida`, `RamoArmado`),
> motor de cálculo (`POST /v1/flores/validar-cantidad`, `POST /v1/flores/calcular-precio`), CRUD
> admin de cada catálogo, `LugarEntrega` extendido con `costoEnvio`, **e integración completa con
> `Pedido`/`Venta`** vía variantes "sombra" (sin tocar `PedidoServiceImpl`) — ver "Diseño técnico,
> parte 2" más abajo. `SecurityConfig` actualizado. Compila limpio (`mvn -o compile`).
>
> ✅ `migration_flores_eternas.sql` (catálogos) ya corrió en **QA y producción**.
>
> ⏳ **Pendiente:** correr `migration_flores_eternas_pedido.sql` (parte 2, esta sesión) en QA,
> pruebas end-to-end, pantallas de front, cobro manual de frase personalizada validada (sin
> endpoint todavía), y documentar el contrato final en `CAMBIOS_FRONT.md` una vez el front lo
> consuma.

## El problema

Un ramo de flores eternas se arma con una cantidad de flores que forma un círculo. No toda
cantidad forma bien el círculo — el cliente puede pedir "10" y que la cantidad que realmente
cierra bien sea otra. Se necesita: avisarle eso antes de cobrar, permitir accesorios (papel,
listón con frase, corona, luces) configurables con su propio precio, aplicar reglas obligatorias
como el papel a partir de cierto tamaño, y manejar entrega a domicilio con costo por zona.

## Decisiones confirmadas

### 1. Precio por flor — por tipo de flor
Cada tipo de flor eterna (ej. "rosa eterna") tiene su propio precio por unidad configurado por
el admin. Si se agregan otros tipos de flor a futuro, cada uno se configura igual, con su propio
precio. Total base = cantidad final de flores × precio de ese tipo de flor.

### 2. Catálogo de cantidades válidas = lo que el admin configura
El admin da de alta cada cantidad de flores que sabe, por experiencia, que forma bien el círculo
(ej. 18, 20, 28, 32, 34, 48, 52...). Ese catálogo es la referencia única, tanto para armar ramos
preconfigurados como para validar lo que pide un cliente en un ramo a la medida — no son dos
catálogos separados, es el mismo.

### 3. Flujo a la medida (cliente escribe la cantidad libremente)
1. Cliente escribe la cantidad que quiere (ej. 10).
2. El sistema busca esa cantidad en el catálogo. Si 10 **no** es válida, avisa: *"Con 10 flores
   el círculo no quedaría bien formado, pero con 18 sí — y cuesta $X."*
3. Se muestran **ambas** cantidades válidas más cercanas — la anterior y la siguiente (ej. si
   pide 35 y el catálogo tiene ...28, 34, 48..., se le ofrecen 34 y 48, cada una con su precio),
   no solo una.
4. El cliente decide: se queda con su cantidad original o usa una de las sugeridas.
5. Cantidad final fijada → precio base = cantidad final × precio de ese tipo de flor.
6. Si aplica la regla del papel obligatorio (>10 flores), ya viene sumado en automático, sin
   preguntar.
7. Se preguntan los demás accesorios (uno por uno, repetibles — ver más abajo) y se van sumando.
8. Se muestra el total final antes de confirmar el pedido.

### 4. Listón con frase — lista de frases predefinidas + opción personalizada
- **Frases predefinidas:** el admin mantiene una lista de frases ya armadas (sin tope fijo, el
  admin agrega las que quiera), cada una con su precio. El cliente ve la lista y elige una
  directo, sin fricción — precio aplicado de inmediato.
- **Frase personalizada (opcional):** si ninguna de la lista le convence, el cliente puede
  escribir la suya. Esa parte del pedido queda **pendiente de validar** por el admin, con este
  flujo:
  1. El admin revisa la frase, decide si es viable y le pone precio.
  2. Para que el pedido avance mientras se valida/produce, el cliente debe pagar un **anticipo
     del 50%**.
  3. Se le aclara al cliente, con buena forma, que **una vez entregado el ramo no hay reembolsos
     ni cancelaciones** — si en algún punto se cancela después de ese momento, el anticipo (o el
     pago realizado) no se devuelve.

### 5. Envío a domicilio — solo zonas fijas configuradas (lista cerrada, no por radio)
El admin da de alta una **lista fija de lugares** a los que sí se puede entregar (no es un radio
de distancia calculado, son lugares puntuales ya conocidos), cada uno con su costo de envío. El
cliente elige entre esos lugares configurados. Si el lugar que quiere no está en la lista, no hay
envío disponible ahí — se le ofrece pasar a recoger al local.

**Nota para el front:** el front no debe dejar escribir una dirección libre — tiene que pedirle
al cliente que **elija el lugar de entrega de una lista** (la lista de `ZonaEnvio` que devuelve
el back). La decisión de si ese lugar es entregable o no la determina el back según lo que el
admin tenga configurado — el front solo muestra las opciones disponibles y, si el cliente no
encuentra su lugar en la lista, ofrece la opción de recoger en el local.

### 6. Datos de contacto — reutilizar perfil o capturar nuevos
Si el cliente está logueado, se le pregunta si quiere usar el teléfono/correo ya guardados en su
perfil, o prefiere dar unos distintos solo para este pedido.

### 7. Reglas obligatorias también aplican a ramos preconfigurados
La regla del papel obligatorio (>10 flores) también aplica cuando el admin arma un ramo
preconfigurado, no solo en el flujo a la medida — el papel ya debe venir incluido y cobrado en
ese precio, el sistema lo aplica automático por la regla del umbral. Es un principio general:
cualquier accesorio obligatorio por regla debe quedar incluido y cobrado desde el momento en que
se configura el ramo, sea preconfigurado o a la medida.

### 8. Accesorios repetibles y comentario de "no disponible"
- Un accesorio se puede elegir más de una vez y cada vez se cobra su precio (ej. 2 listones con
  frases distintas → se cobra el precio del listón × 2). En el front es un contador por
  accesorio, no un checkbox on/off; si admite texto libre, cada unidad captura su propio texto.
- El comentario libre de "accesorio no disponible" (ej. "quiero con luces" si luces está
  desactivado) vive en el pedido, visible para el admin al revisarlo, para que le confirme al
  cliente por fuera si sí se puede conseguir.

## Modelo conceptual actualizado (aún no es código)

| Entidad | Campos clave |
|---|---|
| `TipoFlor` | nombre (ej. "Rosa eterna"), precio por flor, **precioCosto, stock** (real, flores sueltas), activo, variante "sombra" |
| `CantidadFlorValida` | tipo de flor, cantidad, activo — el catálogo de "qué cantidades cierran bien el círculo" |
| `RamoArmado` (preconfigurado) | nombre, foto, tipo de flor, cantidad (debe ser una `CantidadFlorValida`), accesorios incluidos, precio (ya con reglas obligatorias aplicadas) |
| `AccesorioRamo` | nombre, precio fijo, **precioCosto**, repetible (sí), admite texto libre, activo, flag "es papel" (regla del umbral), variante "sombra" |
| `FraseListonPredefinida` | texto, precio, variante "sombra" |
| `LugarEntrega` | nombre, costoEnvio, variante "sombra" (solo si tiene costo) — hace de "zona de envío" |
| `RamoPedidoDetalle` | pedido, ramo armado de origen (si aplica), tipo de flor, cantidad final, frase de listón (predefinida o personalizada pendiente de validar + estado + anticipo), zona de entrega o "recoger en local", teléfono/correo de contacto, comentario de accesorio no disponible — **no** guarda precios, esos ya viven como líneas reales de `DetallePedido` |

## Estado del análisis

Con esto quedan cerradas todas las decisiones de negocio identificadas. Arranca la implementación
técnica (backend) — ver "Diseño técnico" y "Próximos pasos" abajo.

## Diseño técnico (2026-08-12) — primera etapa: catálogos + motor de cálculo

**`ZonaEnvio` no es una entidad nueva.** El proyecto ya tiene `LugarEntrega` (catálogo
`nombre` + FK opcional en `pedidos.lugar_entrega_id`), que es exactamente "lista fija de lugares
a los que se puede entregar" — se le agrega un campo `costoEnvio` en vez de crear un catálogo
paralelo.

**Entidades nuevas** (`entity/`, todas extienden `BaseId`, siguen convención `Promocion`):
- `TipoFlor` — nombre, precioPorFlor, activo.
- `CantidadFlorValida` — tipoFlor (FK), cantidad, activo. Catálogo de "qué cantidades cierran
  bien el círculo", por tipo de flor.
- `AccesorioRamo` — nombre, precio, admiteTextoLibre, esPapel (flag para la regla del umbral),
  activo.
- `FraseListonPredefinida` — texto, precio, activo.
- `RamoArmado` + `RamoArmadoAccesorio` — ramo preconfigurado: tipoFlor, cantidadFlorValida,
  accesorios incluidos, precio ya calculado (aplicando la regla del papel automáticamente si
  la cantidad es >10).
- `LugarEntrega` (existente) — se le agrega `costoEnvio`.

**Motor de cálculo (sin persistencia todavía, ver nota de alcance abajo):**
- `POST /v1/flores/validar-cantidad` — recibe tipo de flor + cantidad que el cliente escribió;
  si no es una `CantidadFlorValida`, devuelve la alternativa válida más cercana hacia abajo y
  hacia arriba, cada una con su precio (solo flores, sin accesorios todavía).
- `POST /v1/flores/calcular-precio` — recibe tipo de flor + cantidad final + accesorios elegidos
  (lista repetible) + listones elegidos (cada uno predefinido o personalizado) + lugar de entrega
  o recoger en local. Devuelve el desglose completo: precio de flores, papel (automático si
  aplica), accesorios, listones (marcando los personalizados como pendientes de validar), envío,
  y si aplica, el aviso de anticipo del 50% + política de no reembolso.

## Diseño técnico (2026-08-12, parte 2) — integración con Pedido/Venta

**Investigado antes de decidir:** `detalle_pedidos.variante_id` es `NOT NULL` en la BD real desde
mayo (cerrado a propósito), y `DetalleVentaVariante` (venta de mostrador) además exige
`precio_costo`/`ganancia` — no solo `producto_id`/`variante_id`. También se confirmó el
precedente de Rifas: ya existe algo "vendible" sin ser un producto físico tradicional, resuelto
con una tabla aparte. Y se confirmó algo clave leyendo `PedidoServiceImpl.savePedido()`: cada
línea que llega en el request se valida contra `producto.precioVenta` exacto — **no hay forma de
sumar un monto libre al total de un pedido**, todo lo que cobra tiene que ser una línea real con
un producto/variante real detrás.

**Decisión: variante "sombra" por catálogo, en vez de tabla paralela + relajar NOT NULL.**
`TipoFlor`, `AccesorioRamo`, `FraseListonPredefinida` y `LugarEntrega` (cuando tiene
`costoEnvio`) ahora tienen un campo `variante` (`Variantes`, con su `Producto` detrás,
autogenerado). Así, una línea de "N flores de tal tipo", o de "tal accesorio", o "envío a tal
lugar", se vende exactamente como cualquier producto normal:

- La crea/sincroniza automáticamente `ProductoSombraServiceImpl` cada vez que el admin
  guarda/edita el catálogo correspondiente (`TipoFlorServiceImpl.save()`,
  `AccesorioRamoServiceImpl.save()`, etc. — nunca se toca a mano).
- `TipoFlor` además tiene `precioCosto` y `stock` propios (el admin los edita ahí mismo, sin
  saber que existe una variante detrás) — ese `stock` sí es real: representa flores sueltas en
  inventario, y se descuenta solo cuando se vende (vía el flujo normal de `Pedido`/`Venta`).
- `AccesorioRamo`/`FraseListonPredefinida`/`LugarEntrega` usan un stock fijo alto
  (`ProductoSombraServiceImpl.STOCK_SIN_CONTROL`) — no se controla inventario de esos.
- `AccesorioRamo` también tiene `precioCosto` propio, para que la ganancia se calcule bien.

**Qué significa esto para el front al armar el pedido:** `POST /v1/flores/calcular-precio` ahora
devuelve el `varianteId` de cada componente con precio conocido (`tipoFlorVarianteId`,
`papelVarianteId`, `AccesorioCalculadoDto.varianteId`, `ListonCalculadoDto.varianteId`,
`envioVarianteId`). El front arma el pedido con el endpoint que **ya existe y no cambió**,
`POST /v1/pedidos/savePedido`, mandando una línea en `detalles` por cada componente (cantidad,
`varianteId`, `precioUnitario`, `subTotal` — el mismo contrato de siempre). **No hubo que tocar
`PedidoServiceImpl` en absoluto** — stock, cancelación con devolución de stock, cálculo de
ganancia y Mercado Pago siguen funcionando exactamente igual, porque desde su perspectiva es una
venta de producto normal.

**La única excepción — frase de listón personalizada pendiente de validar:** no tiene precio al
momento de crear el pedido, así que no puede ser una línea de `savePedido`. Se guarda como
"pendiente" en `RamoPedidoDetalle` (ver abajo) y el cobro de esa parte queda **manual** por ahora
— no hay endpoint para agregarle una línea a un pedido ya creado. Documentado como límite
conocido, no como bug.

**`RamoPedidoDetalle`** (tabla nueva, chica): el "ticket de producción" — lo que no tiene lugar
en `DetallePedido` porque no es dinero, es información: qué frase de listón exacta (predefinida o
personalizada + su estado de validación + anticipo), zona de entrega o recoger en local,
teléfono/correo de contacto del pedido, comentario libre de accesorio no disponible, y referencia
al `RamoArmado` de origen si aplica (trazabilidad).

- `POST /v1/flores/pedidos/{pedidoId}/detalle` — adjunta el ticket a un pedido **ya creado**
  (autenticado, igual que `/v1/pedidos/**`).
- `GET /v1/flores/pedidos/{pedidoId}/detalle` — lo consulta.
- `PUT /v1/flores/pedidos/detalle/{id}/validar-frase` — ADMIN aprueba/rechaza la frase
  personalizada pendiente, le asigna precio, y marca si ya se pagó el anticipo.

## Próximos pasos

- ✅ Diseño técnico de catálogos + motor de cálculo (2026-08-12).
- ✅ Integración con `Pedido`/`Venta` vía variantes "sombra", sin tocar `PedidoServiceImpl`
  (2026-08-12).
- ⏳ Correr `migration_flores_eternas.sql` y `migration_flores_eternas_pedido.sql` en QA
  (`migration_flores_eternas.sql` ya corrió en QA y producción; la segunda parte, de esta misma
  sesión, todavía no).
- ⏳ Cobro manual de la frase personalizada una vez validada (sin endpoint todavía — ver límite
  conocido arriba).
- ⏳ Pantallas de admin (catálogos, incluida la edición de `precioCosto`/`stock`) y configurador
  del cliente en el front.
- ⏳ Documentar contrato final en `CAMBIOS_FRONT.md` una vez el front consuma estos endpoints —
  incluir que la ubicación de entrega se elige de una lista (`GET /v1/lugares-entrega`), no se
  escribe libre, y que el pedido se arma con `POST /v1/pedidos/savePedido` usando los
  `varianteId` que devuelve `/v1/flores/calcular-precio`.
