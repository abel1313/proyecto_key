# busquedapedido — la lista de pedidos del administrador con todos sus filtros

Pedido por el dueño el 2026-10-06: *"necesito más filtros… que muestre todos los filtros posibles y
búsqueda por nombre"*. Reemplaza, para el administrador, a `GET /v1/pedidos/buscarClientePedido`
(que se queda funcionando para no romper a nadie).

## Por qué un dominio nuevo y no más parámetros en el viejo

La búsqueda vieja (`IPedidoRepository.buscarPedidosPorCliente` / `buscarTodosLosPedidos`) tenía 4
problemas que se arreglan aquí:

1. **No encontraba por el nombre que se ve en la card.** La card muestra a veces el nombre de quien
   recibe (`nombre_receptor`), pero la búsqueda no lo miraba.
2. **Ordenaba por `fecha_pedido` (sin hora).** Con varios pedidos el mismo día, MySQL los devolvía en
   cualquier orden y al pasar de página uno se repetía y otro se brincaba. El front además reordenaba
   por número solo dentro de la página.
3. **Solo filtraba Pagados y Cancelados** como estado.
4. **Un query fijo con `(:x IS NULL OR …)`** por filtro: cada filtro nuevo lo hacía más largo y MySQL
   no podía usar índices. Aquí el SQL se arma solo con los filtros que llegan (siempre con parámetros).

## Reglas

| # | Regla | Dónde vive |
|---|---|---|
| R1 | **Texto:** busca en nombre del cliente (con cuenta y sin registro), nombre de quien recibe, teléfono, correo, y nombre o código de barras de cualquier artículo del pedido. Sin acentos ni mayúsculas. Con letras: mínimo **3**. Solo números (o `#120`): número de pedido **exacto** desde 1 dígito; con 3 o más dígitos además busca en teléfonos y códigos de barras. `%` y `_` se buscan tal cual. Máximo 100 caracteres. | `TextoBuscado`, `PedidosFiltradosJdbcAdapter.coincidencias()` (ver R17) |
| R2 | **Forma de cobro:** Contado (`NORMAL` ya cobrado), Apartado, Ir pagando (`FIADO`). `forma=PENDIENTE` se sigue aceptando por los filtros guardados (fue botón de este bloque del 2026-10-07 al 2026-10-13; ahora vive en Pago, R16). | `FormaDeCobro` |
| R3 | **Estado**, con las palabras de la card: Pendiente (contado sin cobrar), Por cobrar (Apartado o Ir pagando abierto), Pagado (Apartado o Ir pagando liquidado), Entregado (contado cobrado), Cancelado. | `EstadoBuscado`, `estado()` |
| R4 | **Dinero:** *Con saldo* (Apartado / Ir pagando abierto que debe algo), *Sin abonos* (abierto y sin ningún abono), *Saldo a favor* (pagó más de lo que vale, o se canceló un Apartado con dinero o un pedido ya pagado: hay que devolverle; un Ir pagando cancelado que todavía debía **no**, eso es deuda incobrable). | `SituacionDeDinero`, `dinero()` |
| R5 | **Total** desde / hasta. No negativo; desde ≤ hasta. Es el total que muestra la card: el del pedido, o el de todo el grupo si es titular (R14). | `FiltroPedidos` |
| R6 | **Día de registro** desde / hasta, los dos incluidos (hasta las 23:59:59). Desde ≤ hasta. | `RangoDeFechas` |
| R7 | **Entrega:** Hoy, Mañana, Esta semana (hoy + 6 días), Atrasados (la fecha ya pasó). Solo pedidos que **esperan entrega** (no Entregados, Pagados ni Cancelados), igual que el "⚠ Atrasado" de la card. "Hoy" es hoy **en México**, aunque el servidor esté en UTC. | `CuandoSeEntrega`, `HoyPort` |
| R8 | **Lugar de entrega** (uno) y **modo**: Recoge en tienda (sin lugar o un lugar "recoger en tienda") / Envío. | `ModoDeEntrega` |
| R9 | **Unidos:** de un grupo activo solo sale el **titular** (su card trae el total de todos). Un miembro sale solo si se busca su **número exacto**. Filtro: Solo unidos / Sin unir. | `condiciones()` |
| R10 | **Otros:** solo ramos de flores; solo con algún artículo de promoción. | `condiciones()` |
| R11 | **Orden:** Más recientes (por fecha **y hora** de registro, el de siempre), Más antiguos, Entrega más próxima (primero lo que falta entregar, de la fecha más vieja a la más lejana; después lo entregado, pagado, cancelado o sin fecha, del más reciente al más viejo), Mayor saldo (el del grupo en un titular). Siempre se desempata por número de pedido. | `OrdenDePedidos`, `orden()` |
| R12 | **Páginas** desde 0, de 1 a 50 pedidos por página. Regresa total de pedidos y de páginas. | `FiltroPedidos`, `PaginaDePedidos` |
| R13 | Filtros distintos se combinan con **Y**; las opciones dentro de un mismo filtro, con **O**. Un filtro vacío no filtra. | `FiltroPedidos` |
| R14 | **Un pedido unido se filtra por lo que muestra su card** (2026-10-06). La card del titular muestra el total, lo pagado y lo que falta de **todo el grupo**, así que estado, dinero, total, fecha de entrega (espera entrega mientras el grupo deba) y "Los que más deben" se calculan con el grupo; el texto, ramos y promociones buscan en todos sus pedidos. Antes se miraba solo al titular: un abono al grupo liquida primero al más viejo, el titular quedaba Pagado y el grupo desaparecía de "Por cobrar" aunque siguiera debiendo. | `PedidosFiltradosJdbcAdapter` (`GRUPOS`, `ESTADO_CARD`, `deLaCard`) |
| R15 | **Vista de inicio: sin lo ya terminado** (2026-10-13). `estado=SIN_TERMINAR` trae todo menos lo **pagado y entregado** ni lo **cancelado** (desde el 2026-10-13, pedido del dueño: lo escondido se vuelve a ver con su filtro); se cruza con Y con los demás filtros. Mis pedidos lo manda solo al abrir, sin filtros ni texto; con cualquier filtro o búsqueda sale lo que se pidió. | `EstadoBuscado.SIN_TERMINAR`, `PedidosFiltradosJdbcAdapter` |
| R16 | **🕓 Pendiente es su propio botón del bloque Pago** (2026-10-13, pedido del dueño). Pendiente = lo pidió el cliente desde su cuenta y nadie lo ha cobrado ni pasado a Apartado / Ir pagando (`NORMAL` + estado `'Pendiente'`, o un grupo de esos). `estado=PENDIENTE` trae solo esos, y **Falta pagar ya no los incluye** (solo Apartados, Ir pagando y lo demás que deba). Antes salían siempre revueltos en Falta pagar y no había forma de quitarlos. `POR_COBRAR` sigue siendo Falta pagar. | `EstadoBuscado.PENDIENTE`, `PAGO_CARD` |
| R17 | **El texto se resuelve una sola vez, como un conjunto** (2026-10-14, pedido del dueño: *"tarda mucho para usar un filtro o quitarlo"*). Buscar por nombre, teléfono, correo o artículo corría, **por cada pedido de la lista**, una subconsulta que recorría sus artículos (y los de todo su grupo) con `LIKE '%…%'`. Con el `OR` adentro MySQL no podía usar el índice del pedido y reescaneaba los artículos por cada pedido: **con 6 000 pedidos, buscar un nombre tardaba 48 segundos**, y la pantalla repite la búsqueda con **cada** filtro que se prende o se quita mientras el texto sigue en el buscador. Ahora `coincidencias()` arma el conjunto de pedidos que coinciden (clientes, clientes sin registro, nombre de quien recibe y artículos, cada tabla una vez y cruzada por índice, más el titular de cada grupo con algún miembro que coincide) y el filtro solo pregunta `p.id IN (…)`. **Mismos resultados que antes** (se comparó con el SQL anterior en 490 búsquedas: mismos pedidos, mismo orden, mismo total). Sin `WITH` ni funciones de ventana a propósito: producción todavía no tiene confirmada su versión de MySQL (`ESPECIFICACIONES_AMBIENTES.md`). | `PedidosFiltradosJdbcAdapter.coincidencias()` / `texto()` |
| R18 | **Caché versionado: se limpia todo junto, nunca "solo el de Ir pagando"** (2026-10-14, pregunta del dueño: *"¿no se maneja caché aquí? ¿y se limpia solo la que toca, según la venta?"* y después *"sigue tardando al seleccionar un filtro o quitarlo"*). (1) **Por qué no por forma de cobro:** un movimiento cambia al pedido de filtro (un abono lo pasa de Ir pagando a Pagado; cambiar la forma de cobro lo pasa de Pendiente a Ir pagando; unir mueve el total y el estado al titular; cancelar lo saca de varios filtros a la vez), y la búsqueda por texto cruza todas las formas de cobro. Limpiar solo una dejaría tarjetas con saldo o estado viejos. (2) **Cómo funciona:** en Redis hay un contador `pedidos:lista:version`. La respuesta completa de `GET /v1/pedidos/buscar` (pedidos + total + tarjetas) se guarda en `pedidos:lista:{version}:{sha256(filtro + hoy)}` por **120 s**. Cualquier cambio sube el contador, así que la llave vieja ya no se vuelve a leer (no se borra nada a mano; Redis la tira sola al vencer). Lleva el **día de hoy** dentro de la llave porque "Atrasado N días" y los filtros de fecha cambian a medianoche. (3) **Qué sube la versión:** (a) cualquier guardado, cambio o borrado por JPA de `Pedido`, `DetallePedido`, `AbonoPedido`, `GrupoPedido`, `GrupoPedidoMiembro`, `Cliente`, `ClienteSinRegistro`, `LugarEntrega`, `Producto` o `RamoPedidoDetalle`, **después de que la transacción se confirma** (`CambiosDePedidosListener`); (b) cualquier POST/PUT/PATCH/DELETE que contesta bien (menos de 400), salvo guardar filtros, login y chatbot (`EscriturasWebInterceptor`) — red de seguridad para lo que cambie datos sin pasar por JPA; (c) el SQL directo que cambia pedidos llama `VersionDeListasDePedidos.subirAlConfirmar(...)` (hoy: Entregado en `PedidosParaEntregarJdbcAdapter`); (d) Sistema → limpiar cachés (`CacheService.evictAll`). (4) **Qué NO la sube:** buscar, abrir un pedido, guardar los filtros de la pantalla, una petición que falla. (5) **Si Redis no responde**, la lista se calcula sin caché (nunca da error por eso). (6) **Interruptor:** `pedidos.lista.cache.habilitado=false` lo apaga sin tocar código; `pedidos.lista.cache.segundos` cambia la vigencia. (7) **Medido** con 6 000 pedidos: primera vez ~260 ms, mismo filtro otra vez ~8 ms; regresar a un filtro ya visto también sale del caché; la respuesta desde caché es idéntica byte por byte a la calculada. | `infraestructura/cache/*`, `BuscarPedidosController` |
| R19 | **Solo se une lo que el filtro usa** (2026-10-14, mismo pedido del dueño). La consulta de la lista unía siempre `lugares_entrega`, `clientes` y `clientes_sin_registro` aunque no se filtrara por ellos, y preguntaba **por cada pedido** si era miembro escondido de un grupo (`NOT EXISTS`). Ahora `lugares_entrega` se une solo si se filtra por forma de entrega, clientes no se unen (el texto ya los resuelve en R17) y los miembros escondidos se calculan **una vez** como tabla (`MIEMBROS_OCULTOS`) y se cruzan por índice. **Mismos resultados que antes** (992 combinaciones de filtros comparadas con el SQL anterior: mismos pedidos, mismo orden, mismo total); con 6 000 pedidos los filtros sin texto quedan ~2× más rápidos (al abrir: 123 → 72 ms). | `PedidosFiltradosJdbcAdapter` |

Un pedido sin artículos no sale (igual que la lista de siempre).

**Paginación:** la pantalla pide 10 pedidos por página (`size=10`) y el SQL usa `LIMIT/OFFSET`, así
que nunca viajan todos los pedidos. Lo que sí recorre todos los que cumplen el filtro es el conteo
(`COUNT(*)`, para "N pedidos") y el orden; por eso R17 y R19 importan más que la página.

**Regla para código nuevo:** todo SQL directo (JdbcTemplate, `@Modifying` nativo) que cambie pedidos,
abonos, grupos, clientes, lugares de entrega o productos tiene que llamar
`VersionDeListasDePedidos.subirAlConfirmar("motivo")`. Si no, la lista de Mis pedidos puede mostrar
el dato viejo hasta 120 s (el interceptor web lo cubre solo si el cambio entra por una petición).

## Endpoint

`GET /v1/pedidos/buscar` — solo ADMIN (misma búsqueda global que `buscarClientePedido`). Contrato
completo en `CAMBIOS_FRONT.md`. Las opciones se mandan como texto (`estado=POR_COBRAR`); una que no
existe contesta **400** diciendo cuáles valen.

Los filtros se pueden guardar por persona con `preferenciafiltro`, pantalla `pedidos-mis-pedidos`.

## Dónde está cada cosa

| Pieza | Archivo |
|---|---|
| Reglas | `dominio/modelo/*` |
| Caso de uso | `aplicacion/servicio/BuscarPedidosService` |
| SQL de los filtros (MySQL) | `infraestructura/salida/persistencia/PedidosFiltradosJdbcAdapter` |
| La card (mismo JSON que la lista vieja + grupo) | `infraestructura/salida/persistencia/TarjetasDePedidoLector` |
| Hoy en México | `infraestructura/salida/reloj/HoyEnLaTiendaAdapter` |
| Caché versionado (R18) | `infraestructura/cache/VersionDeListasDePedidos` (contador), `ListasDePedidosEnCache` (leer/guardar), `CambiosDePedidosListener` (JPA), `EscriturasWebInterceptor` (peticiones) |
| Pruebas | `test/.../busquedapedido/*` (reglas y URL) y `test/.../repository/BusquedaPedidosMysqlTest` (SQL contra MySQL 8, con los comandos para correrla) |

No crea tablas.
