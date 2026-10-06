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
| R1 | **Texto:** busca en nombre del cliente (con cuenta y sin registro), nombre de quien recibe, teléfono, correo, y nombre o código de barras de cualquier artículo del pedido. Sin acentos ni mayúsculas. Con letras: mínimo **3**. Solo números (o `#120`): número de pedido **exacto** desde 1 dígito; con 3 o más dígitos además busca en teléfonos y códigos de barras. `%` y `_` se buscan tal cual. Máximo 100 caracteres. | `TextoBuscado`, `PedidosFiltradosJdbcAdapter.texto()` |
| R2 | **Forma de cobro:** Contado (`NORMAL`), Apartado, Ir pagando (`FIADO`). | `FormaDeCobro` |
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

Un pedido sin artículos no sale (igual que la lista de siempre). Sin caché: la lista cambia con cada
abono, venta y cancelación.

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
| Pruebas | `test/.../busquedapedido/*` (reglas y URL) y `test/.../repository/BusquedaPedidosMysqlTest` (SQL contra MySQL 8, con los comandos para correrla) |

No crea tablas.
