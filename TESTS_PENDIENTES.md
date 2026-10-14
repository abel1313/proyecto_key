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

### 2026-10-14 — Códigos postales guardados en nuestra base (dominio `codigopostal`)
**Dónde:** `BuscarCodigoPostalService`, `CodigoPostalInfo`, `CodigoPostalJpaAdapter`, `CodigoPostalExternoAdapter`, `CodigoPostalController`
**Tipo:** unitario (servicio con puertos simulados; se probó a mano el 2026-10-14 con 8 casos y se borró) · MySQL (adaptador) · controller (URL y status)
**Debe comprobar:**
- [ ] R1: código guardado y vigente → no llama al externo
- [ ] R2: no está → llama al externo y guarda; la segunda vez no llama
- [ ] R3: el externo dice que no existe → `NO_EXISTE` (404) y no guarda
- [ ] R4: el externo falla → `SERVICIO_NO_DISPONIBLE` (502) y no guarda
- [ ] R7: guardado hace 400 días → se vuelve a consultar y se actualiza; si el externo falla, devuelve el guardado
- [ ] La tabla falla al leer o guardar → igual responde con el externo
- [ ] `null`, `5120`, `512000`, `abcde` → `NO_VALIDO` (404) sin llamar a nadie
- [ ] MySQL: `guardar` dos veces el mismo código → una sola fila; con colonias con acentos y comillas se leen igual; dos hilos a la vez → una fila y ninguna excepción (R6)
- [ ] Controller: `GET /v1/dipomex/getCodigoPostal/51200` → 200 con `codigo_postal.municipio`, `.estado`, `.colonias`; sin sesión también responde (es pública)

### 2026-10-14 — Código postal: tiempo límite y errores claros (`DopoMexService`)
**Dónde:** `DopoMexService.getCodigoPostal()`, `GET /v1/dipomex/getCodigoPostal/{codigoPostal}` (`DopoMexController`)
**Tipo:** unitario (servicio con `WebClient` apuntando a un servidor simulado, p. ej. MockWebServer) · controller (URL y status)
**Debe comprobar:**
- [ ] `"5120"`, `"512000"`, `"abcde"` o null → `ExceptionDataNotFound` «El código postal debe tener 5 dígitos» **sin** llamar al servicio externo
- [ ] Servicio externo responde `error: true` o sin `codigo_postal.municipio` → `ExceptionDataNotFound` «No encontramos el código postal 99999» → 404
- [ ] Responde con municipio → regresa el mismo JSON (`codigo_postal.municipio`, `.estado`, `.colonias`) → 200
- [ ] Responde 401/403/500 → `ExceptionServicioExterno` «No pudimos buscar el código postal en este momento. Escribe el municipio a mano» → 502, y el log dice el código HTTP **sin la llave**
- [ ] No contesta en 8 s → mismo 502 (con `StepVerifier.withVirtualTime`)
- [ ] Controller: el `Mono` con error llega al `ExceptionGlobal` con su status (404 / 502), no como 500

### 2026-10-13 — El nombre del comprador: el de la venta primero (`CompradorDelPedido`)
**Dónde:** `Utils/CompradorDelPedido` (`nombre`, `nombreDelTitular`, `correo`, `telefono`), `EntregaZonaServiceImpl.nombreDe()` / `correoDe()`, `AbonoServiceImpl.nombreCliente()` / `telefonoCliente()` / `correoCliente()`, `PedidoServiceImpl.getDetallePedido()`, `PedidosDelGrupoJpaAdapter.nombreCliente()`, `RamoPedidoDetalleServiceImpl.nombreContactoDe()`, `IPedidoRepository.candidatosParaUnir` (busca también por `nombre_receptor`), `EscenariosPruebaAdapter.ventaDirectaConNombreEscrito()` (Prueba 21)
- [ ] `CompradorDelPedido.nombre`: con cliente sin registro → el suyo; sin él y con `nombreReceptor` (con espacios al borde) → el receptor recortado; sin los dos → el de la cuenta; sin nada → null; un nombre en blanco no cuenta
- [ ] `CompradorDelPedido.correo` / `telefono`: con cliente sin registro **sin datos** → null (nunca cae a la cuenta del admin); sin él → los de la cuenta
- [ ] `nombreDelTitular` no mira el receptor (el detalle lo enseña aparte en "Recibe:")
- [ ] MySQL, Venta directa con `clienteId` del admin + `nombreReceptor` y sin cliente sin registro: `listarPendientes` trae `nombreCliente` = el receptor; `reporteEstadoCuenta` trae `cliente` = el receptor; `getDetallePedido` trae `clienteNombre` = el receptor; `candidatosParaUnir` encuentra el pedido escribiendo el receptor y `PedidoDelGrupo.cliente` lo muestra
- [ ] Servicio: `programarEntrega` manda el correo al del cliente sin registro (y saluda con su nombre) y no al de la cuenta cuando la venta trae los dos; con solo la cuenta, a la cuenta

### 2026-10-13 — Preparar prueba: nombre exacto en la venta y reiniciar (datosprueba R20 y R21)
**Dónde:** `NombresDePrueba`, `PruebaQuitada`, `PrepararPruebaService.reiniciar()` / `quitarLoAnterior()` / `quitar()` / `quitarTodas()`, `EscenariosPruebaAdapter` (`clienteConCuenta()`, `clienteSinRegistro()`, `pedidosDeLaPrueba()`, `darDeBajaCatalogoDeLaPrueba()`), `PrepararPruebaController` (`/quitar/{prueba}`, `/quitar-todas`)
**Tipo:** unitario (`NombresDePrueba`, `PruebaQuitada.resumen()`) · servicio con puertos simulados · MySQL (adaptador) · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] `NombresDePrueba`: `cliente("24","K4")` = "Prueba 24 K4"; `modelo("17","Art-A")` = "QA Prueba 17 Art-A"; `modelo("24","")` = "QA Prueba 24 Artículo"; `prefijoModelos("1")` ≠ prefijo de la "12" (termina en espacio); `correo` en minúsculas y con `-`
- [ ] `PruebaQuitada.resumen()`: vacío → null; "1 pedido cancelado y 1 modelo dado de baja" (singular); con pedidos sin cancelar lo avisa
- [ ] Servicio: `preparar` llama `quitarLoAnterior` antes de `armar`; si un `cancelar` lanza, cuenta `pedidosSinCancelar` y sigue con los demás y con `darDeBajaCatalogoDeLaPrueba`; `quitar` en una base que no es QA → `AmbienteNoPermitidoException` sin llamar a ningún otro puerto; `quitarTodas` sigue si una prueba falla
- [ ] MySQL: `pedidosDeLaPrueba("24")` devuelve solo pedidos **no cancelados** con artículos de modelos "Prueba QA" que empiezan con "QA Prueba 24 " (incluye modelos viejos con " #…"); no confunde la 1 con la 12
- [ ] MySQL: `darDeBajaCatalogoDeLaPrueba` deja `habilitado='0'`, stock 0 y sin `variante_imagen`/`producto_imagen_copy`; **no toca** un modelo con otra marca ni de otra prueba; repetirlo regresa 0
- [ ] MySQL, preparar dos veces: la 2ª cancela los pedidos de la 1ª (también unidos en un grupo y un Contado entregado, que queda como devolución con la venta "Devuelta") y deja los mismos pedidos abiertos y los mismos modelos activos; «Prueba 24 K4» en Mis pedidos (`SIN_TERMINAR`) trae solo el K4 nuevo y en Unir (`candidatos`) desde K4 con «Prueba 24 K6» trae K6
- [ ] Controller: `/quitar/99` → 400; sin `ROLE_ADMIN` → 403; `/quitar-todas` → 200 con `lista` y `mensaje` con los totales

### 2026-10-13 — 🧪 Preparar prueba N y Preparar todas (datosprueba R16–R19)
**Dónde:** `PrepararPruebaService.preparar()` / `prepararTodas()` / cada `pruebaNN()`, `EscenarioDePrueba.de()`, `EscenariosPruebaAdapter` (`modeloDePrueba()`, `stockDelModelo()`, `habilitarModelo()`, `articuloDeshabilitadoConStock()`, `precioDelArticulo()`, `datosDelModelo()`), `PrepararPruebaController` (`/v1/admin/datos-prueba/escenarios`, `/preparar/{prueba}`, `/preparar-todas`)
**Tipo:** servicio con puertos simulados · MySQL (adaptador JDBC y servicios reales) · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] Base distinta de `inventario_key_qa` → `AmbienteNoPermitidoException` (403) en `preparar` y en `prepararTodas`, **sin** llamar a ningún otro puerto
- [ ] `EscenarioDePrueba.de("99")` → 400 con la lista "12, 14-QA, 15, 16, 17, 18, 20, 21, 22, 24"; `de(" 21 ")` → P21
- [ ] `prepararTodas`: si una prueba lanza, las demás se preparan y esa regresa con `error` (y `pedidos`/`catalogo` vacíos); `limpiarCache()` se llama una vez al final
- [ ] `preparar` llama a `limpiarCache()` después de armar
- [ ] MySQL, 12: Art-200 queda con stock 10 y precio 200; T1 venta NORMAL entregada; T2 FIADO con abono de 50
- [ ] MySQL, 14-QA: modelo con stock 3, artículo con stock 1 (libre 2)
- [ ] MySQL, 15: M5 y M10 de categorías distintas; talla M con `habilitado='0'` y stock 3; modelo con stock 5 y 10
- [ ] MySQL, 17: XL de Filtros deshabilitada con stock 5; Descuento con `precio_venta=130`, `precio_rebaja=100`, `usar_descuento=1`; Stock-1 con stock 1
- [ ] MySQL, 18: M-OFF con `producto.habilitado='0'` y su artículo habilitado; M-0 habilitado con stock 4 repartido (libre 0)
- [ ] MySQL, 20: P1 y P2 🕓 Pendiente con zona (sin `fecha_recogida`); Agotado con stock 0
- [ ] MySQL, 16, 21, 22, 24: tipo, pagado, entregado y estado de cada pedido como dice su `queEs`; todo Pendiente con zona
- [ ] Adaptador: `stockDelModelo` / `habilitarModelo` / `articuloDeshabilitadoConStock` / `precioDelArticulo` sobre un modelo **sin** marca "Prueba QA" → rechaza y no escribe
- [ ] Controller: sin `ROLE_ADMIN` → 403; `POST /preparar-todas` → 201 con la lista en `lista` y `mensaje` "N pruebas listas, M con error" cuando alguna falla

### 2026-10-13 — Forma de cobro del grupo completo (grupopedido R20)
**Dónde:** `GrupoPedidos.porCambiarFormaDeCobro()`, `CambiarFormaDeCobroGrupoService`, `FormaDeCobroAdapter`, `PedidoServiceImpl.cambiarTipoDentroDeGrupo()`, `PUT /v1/grupos-pedido/{id}/tipo`, `SecurityConfig`
**Tipo:** unitario (dominio) · servicio con puertos simulados · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] Dominio: grupo Pendiente (NORMAL) → FIADO devuelve los vivos del más viejo al más nuevo; los cancelados no; uno que ya es del tipo nuevo no
- [ ] Dominio: → APARTADO con `pagadoCentavos > 0` o con cobro ahora → `CambioDeFormaDeCobroInvalidoException`; → NORMAL, al mismo tipo, grupo inactivo o con un pedido `Entregado` → rechaza; cobro mayor al saldo → rechaza
- [ ] Servicio: con cobro llama a `abonar` del grupo después de cambiar todos; si el dominio rechaza no llama a `FormaDeCobroPort` ni a la bitácora
- [ ] `PUT /v1/pedidos/{id}/tipo` de un pedido unido → 409; `PUT /v1/grupos-pedido/{id}/tipo` sin la acción `cambiar-tipo` → 403, con ella → 200; grupo inexistente → 404

### 2026-10-14 — 🧪 rifas, códigos de compra y usuarios de prueba (datosprueba R23)
**Dónde:** `PrepararPruebaService.prueba19()` / `usuarioDePrueba()` / `quitarLoAnterior()`, `RifasPruebaAdapter`, `CuentasPruebaAdapter`, `EscenariosPruebaAdapter.compraConCodigo()`
**Tipo:** integración con MySQL (como `TmpPrep19Test`, que se corrió a mano el 2026-10-14 contra `pk_preparar`: 0 fallas)
**Debe comprobar:**
- [ ] 19: una sola rifa activa "QA Prueba 19 Rifa cerrada", es de prueba, diaria, con premio QAPRUEBA19 y plazo vencido; el premio apartó 1 pieza
- [ ] 19: registrar un concursante en esa rifa → `ExceptionOperacionNoPermitida` "El plazo de registro cerró…"
- [ ] 19: el código de CU ya está usado (`reclamado_en`) y `reclamarVenta(CU)` → "Este código ya fue utilizado"; CL sin usar; un código inventado → `ExceptionDataNotFound` "Código inválido"
- [ ] Preparar 19 dos veces: la segunda desactiva la rifa de antes y regresa su pieza apartada (`otrosDesactivados` = 1)
- [ ] 17 y 18: `qa.prueba17`/`qa.prueba18` activos con `ROLE_QA_PRUEBA_17/18`, con **exactamente** sus pantallas y acciones (no se duplican al reiniciar)
- [ ] El usuario entra con la contraseña que se le ponga y no con otra; quitar la prueba lo desactiva y ya no entra
- [ ] La respuesta de preparar nunca trae una contraseña
- [ ] Preparar todas / Quitar todas sin errores; ningún usuario de prueba ni rifa de prueba activa después de Quitar todas
- [ ] Si falta la acción en la base (migración sin correr), el 🧪 responde con *"No existe la acción … (¿falta correr su migración?)"*

### 2026-10-14 — Caché versionado de la lista de Mis pedidos (busquedapedido R18)
**Dónde:** `busquedapedido/infraestructura/cache/*` (`VersionDeListasDePedidos`, `ListasDePedidosEnCache`, `CambiosDePedidosListener`, `EscriturasWebInterceptor`), `BuscarPedidosController`
**Tipo:** integración con MySQL + Redis (`@SpringBootTest` con `pedidos.lista.cache.habilitado=true`; en `src/test/resources/application.properties` está apagado)
**Debe comprobar** *(todo comprobado a mano el 2026-10-14 contra la base de 6 000 pedidos, 11/11)*:
- [ ] La respuesta desde caché es idéntica byte por byte a la calculada (mismo JSON, mismas tarjetas, mismo total)
- [ ] Mismo filtro dos veces: la segunda no consulta la base; regresar a un filtro ya visto también sale del caché
- [ ] Guardar por JPA un `Pedido`, `AbonoPedido`, `GrupoPedido`, `Cliente`, `LugarEntrega` o `Producto` sube la versión **después del commit** (con rollback no sube) y la siguiente búsqueda trae el dato nuevo
- [ ] Entregado (`PedidosParaEntregarJdbcAdapter`, SQL directo) sube la versión y la lista refresca
- [ ] POST/PUT/PATCH/DELETE con 2xx sube la versión; con 4xx/5xx no; `GET` no; `/v1/preferencias-filtro`, `/auth/` y `/chatbot/` no
- [ ] Sistema → limpiar cachés (`CacheService.evictAll`) sube la versión
- [ ] La llave lleva el día: al cambiar `HoyPort` de día, la misma búsqueda no se lee del caché de ayer
- [ ] Redis caído o con error: la lista se calcula sin caché y responde 200 (nunca 500)
- [ ] `pedidos.lista.cache.habilitado=false`: no lee ni escribe Redis y el listener no se registra

### 2026-10-14 — Mis pedidos: solo se une lo que el filtro usa (busquedapedido R19)
**Dónde:** `PedidosFiltradosJdbcAdapter` (`desde`, `MIEMBROS_OCULTOS`)
**Tipo:** MySQL (SQL nativo) — agregarlo a `BusquedaPedidosMysqlTest`
**Debe comprobar:**
- [ ] Mismos pedidos, mismo orden y mismo total que el SQL anterior en todas las combinaciones de estado × entrega × forma de cobro × modo de entrega × fechas × orden, con y sin texto *(comprobado a mano el 2026-10-14: 992 combinaciones en la base de 800 pedidos)*
- [ ] Un miembro no titular de un grupo **activo** no sale; de un grupo inactivo sí sale
- [ ] Sin filtro de modo de entrega la consulta no une `lugares_entrega`; con él, sí
- [ ] Rendimiento: con 6 000 pedidos, abrir la pantalla (SIN_TERMINAR) en menos de 150 ms *(medido: 123 → 72 ms)*

### 2026-10-14 — Aviso de stock del artículo con dónde sumárselo al modelo
**Dónde:** `VarianteServiceImpl.validarStockContraProducto()`
**Tipo:** unitaria (ya cubierta en parte por `VarianteStockValidacionTest`, que revisa el final "Disponible: X, Solicitado: Y")
**Debe comprobar:**
- [ ] El mensaje dice el nombre del modelo, cuántas piezas libres tiene, cuántas más se piden y la ruta *Catálogo → Modelos → ✏️ Actualizar → «Actualizar stock»*
- [ ] Con más artículos que stock del modelo (disponible negativo) dice "tiene 0 libres", y el final sigue con el número real

### 2026-10-14 — La búsqueda por texto de Mis pedidos se resuelve como un conjunto (busquedapedido R17)
**Dónde:** `PedidosFiltradosJdbcAdapter.coincidencias()` / `texto()` / `datosDePersona()`
**Tipo:** MySQL (SQL nativo) — agregarlo a `BusquedaPedidosMysqlTest`
**Debe comprobar:**
- [ ] Mismos pedidos, mismo orden y mismo total que el SQL anterior para: nombre de cliente con cuenta, de cliente sin registro, nombre de quien recibe, teléfono, correo, nombre de artículo, código de barras, número de 1, 2, 3 y 4+ dígitos, `#14`, `50%`, `a_b` (comodines tal cual) y un texto que no existe; solos y combinados con estado (`SIN_TERMINAR`, `POR_COBRAR`) y forma de cobro *(comprobado a mano el 2026-10-14: 490 búsquedas en una base de 800 pedidos)*
- [ ] Un titular sale cuando coincide **un miembro** de su grupo activo (por nombre, por artículo, por teléfono) y el miembro sigue sin salir en la lista
- [ ] Un número de 1 o 2 dígitos solo trae el pedido con ese número (la subconsulta no se arma)
- [ ] Rendimiento: con ≥ 5 000 pedidos y ≥ 10 000 artículos, buscar un nombre y un artículo responde en menos de 1 segundo *(medido el 2026-10-14: 6 000 pedidos, 48 102 ms → ~120 ms)*
- [ ] No usa `WITH` ni funciones de ventana (corre en MySQL 5.7)

### 2026-10-13 — Mis pedidos abre sin lo ya pagado y entregado (busquedapedido R15)
**Dónde:** `EstadoBuscado.SIN_TERMINAR`, `PedidosFiltradosJdbcAdapter.condiciones()`
**Tipo:** MySQL (SQL nativo)
**Debe comprobar:**
- [ ] `estado=SIN_TERMINAR` no trae contado Entregado ni Apartado/Ir pagando pagado con `entregado = 1`; sí trae Pagado sin entregar, Ir pagando entregado que debe, Pendiente y cancelados *(comprobado a mano en MySQL 8 el 2026-10-13 con 6 pedidos)*
- [ ] Un titular de grupo se juzga por el grupo (R14): sale mientras algún pedido del grupo falte de pagar o de entregar
- [ ] Combinado con otro estado (`SIN_TERMINAR` + `CANCELADO`) se cruza con Y

### 2026-10-13 — Cliente: caché, Mis datos y modelo agotado
**Dónde:** `ClienteServiceImpl` (`@CacheEvict clienteCache` en save y los 7 métodos que escriben), `UsuarioVerificacionService.verificarCorreo/confirmarCambioCorreo`, `PedidoServiceImpl.savePedido` (404), `ProductosServiceImpl.guardarProducto` (stock 0 solo en alta; 400/409 sin envolver)
**Tipo:** integración (Spring + caché en memoria) · servicio con puertos simulados
**Debe comprobar:**
- [ ] `findClienteById` → guardar el cliente con nombre/apellido/teléfono → `findClienteById` trae `datosCompletos = true` (antes la copia vieja)
- [ ] Verificar el correo de un usuario nuevo (alta automática del cliente) y luego leerlo: no queda en caché el "no existe"
- [ ] `savePedido` con un cliente que no existe → `ExceptionDataNotFound`
- [ ] `guardarProducto` con id existente y stock 0 → guarda; alta nueva con stock 0 → 400 "El stock no debe de ser 0"; código de barras de otro producto → `ExceptionDuplicado` (409) sin envolver

### 2026-10-08 — Errores con su código HTTP (BUG-KEY-32)
**Dónde:** `ExceptionGlobal` (409 `ExceptionOperacionNoPermitida`, 502 `ExceptionServicioExterno`, 403 `ExceptionSinPermiso`), 102 `catch` quitados en 27 controllers, `VarianteController.getImagenesV2` (ya no 200 en error), servicios de rifas, concursantes, ventas y gastos (sin `Exception` simple)
**Tipo:** controller con `ExceptionGlobal` (MockMvc con `setControllerAdvice`)
**Debe comprobar:**
- [ ] Cada excepción propia sale con su código y el `mensaje` de siempre: 404, 400, 409, 422, 502, 403; base de datos → 500 con texto genérico; error inesperado → 500 "Error interno del servidor"
- [ ] `GET /v1/variantes/imagenes/{id}` con error → status de error (no 200 con `[]`)
- [ ] Reclamar compra: código vacío 400, inexistente 404, usado/vencido 409, de otro correo 403; concursante repetido 409; rifa cerrada 409
- [ ] `AuthController` sigue con sus mensajes genéricos (no dice si el usuario existe)

### 2026-10-08 — Revisión antes de QA: filtros de Tienda con permiso propio y ajuste de stock sin pisar movimientos
**Dónde:** `SecurityConfig` (`GET /v1/variantes/admin/filtrar`, helper `acciones()`), `AjustarStockModeloService`, `GuardarStockModeloJpaAdapter`, `ProductosServiceImpl.findNombreOrCodigoBarra(…, todos)` (`@Transactional`)
**Tipo:** controller (URL y status) · servicio con puertos simulados · MySQL
**Debe comprobar:**
- [ ] Rol con solo `tienda/buscar:filtro-sin-stock` (sin pantallas de Catálogo) → `admin/filtrar` 200; sin ningún filtro de admin ni pantalla de Catálogo → 403; `/v1/variantes/admin/sin-stock` con solo el filtro → sigue 403
- [ ] Ajuste con el stock igual al leído → guarda; si otra operación lo cambió en medio → 400 "El stock del modelo cambió mientras lo ajustabas…" y no escribe ni limpia cachés *(UPDATE comprobado en MySQL el 2026-10-08)*
- [ ] Modelo con `stock` NULL → se toma como 0 y el ajuste +5 guarda 5

### 2026-10-08 — Agregar artículo: todos los modelos, habilitar y agregar stock al momento
**Dónde:** `DisponibilidadStock.conAjuste()/articulosQueAunCaben()`, `AjustarStockModeloService`, `ConsultarStockJpaAdapter` (habilitado y foto), `GuardarStockModeloJpaAdapter`, `StockController` (`PUT /v1/stock/producto/{id}/ajuste`), `ProductosServiceImpl.findNombreOrCodigoBarra(…, todos)`, `AuthenticationUtils.tieneAccion()/puedeVerTodosLosModelos()`, `SecurityConfig`, `migration_accion_tienda_venta_ver_todos.sql`; front `variante/agregar`
**Tipo:** unitario (dominio) · servicio con puertos simulados · H2/MySQL (adaptadores) · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] Modelo 10 con 10 repartidos, ajuste +5 → total 15, `articulosQueAunCaben` 5; no cambia habilitado ni foto
- [ ] 15 con 10 repartidos: −5 → 10; −6 → 400 "No se puede dejar el modelo en 9: ya tiene 10 repartidos en sus artículos"
- [ ] Ajuste 0 o null → 400 "Escribe cuánto stock agregar (+) o quitar (−) al modelo"; 3 con −4 → 400 "No se puede quitar 4: el modelo solo tiene 3"
- [ ] Descuadrado (12 con 18 repartidos) → `articulosQueAunCaben` 0 (nunca negativo)
- [ ] Servicio: ajuste válido → guarda el total nuevo y avisa a las cachés; ajuste inválido → no guarda ni avisa; modelo inexistente → 404
- [ ] Consulta: modelo `habilitado='0'` sin fila en `producto_imagen_copy` → `habilitado=false`, `conFoto=false`; con foto → `true`; artículos dados de baja no cuentan en lo repartido
- [ ] `UPDATE` del stock: después de guardar 7, la consulta devuelve `stockTotal` 7; el reporte de descuadrados sigue corriendo
- [ ] `PUT …/ajuste` sin Editar en Modelos / Agregar modelo / Agregar artículo → 403; con Editar → 200
- [ ] Buscador `?todos=true`: admin → todos (también sin `todos`); rol con `tienda/venta:ver-todos-los-modelos` → todos, con `habilitado`, `marca`, `contenido` y sin `precioCosto`; rol sin el permiso → solo con stock, habilitados y con foto; sin sesión → igual que hoy
- [ ] La llave de caché distingue `todos` con y sin permiso (un rol sin permiso no recibe lo guardado para el admin)
- [ ] Migración: dos corridas → 1 acción y 1 fila en `rol_accion` para ROLE_ADMIN *(comprobado en base desechable el 2026-10-08)*
- [ ] Front: modelo deshabilitado → aviso, **✅ Habilitar modelo** (solo con el permiso Habilitar), 💾 Guardar y "Guardar en el modelo" apagados; tras habilitar, resumen con cuántos artículos caben
- [ ] Front: "Guardar en el modelo" con +5 → stock al modelo y pregunta "¿Deseas agregar los artículos de una vez?" → ventana 🧩; con −1 → "Se quitaron 1"
- [ ] Front: el recuadro de stock aparece (antes leía `r.data` y nunca salía)

### 2026-10-08 — Ventana 🧩 Agregar artículos (flujo A) y foto propia o del modelo
**Dónde:** `VarianteServiceImpl.repartirImagenes()` y `guardarConImagenes()` (campos `imagenesPropias`, `usarImagenDelModelo`); front `shared/alta-articulos`, `productos/producto/add`, `productos/producto/all`
**Tipo:** unitario · servicio · front
**Debe comprobar:**
- [ ] Sin `imagenesPropias` (Agregar artículo con varias tallas): la foto del primero la llevan todos, como siempre; no se exige que el micro devuelva la misma cantidad
- [ ] Con `imagenesPropias` en 3 artículos con 1, 0 y 2 fotos y el micro devolviendo [10, 20, 21] → [10], [], [20, 21]
- [ ] Con `imagenesPropias` y el micro devolviendo menos fotos de las enviadas → 400 "No se pudieron subir todas las fotos…" y no se guarda ningún artículo
- [ ] `usarImagenDelModelo` → el artículo queda ligado a la foto principal del modelo (sin subirla); modelo sin foto → artículo sin foto, sin error
- [ ] Dar de baja un artículo que usa la foto del modelo → la foto **no** se borra (sigue en `producto_imagen_copy`)
- [ ] Front: casillas solo de lo que el modelo tiene lleno; desmarcar borra el dato donde seguía igual al del modelo; "Te pasaste por N" apaga Guardar; "¿Cuántos?" fuera de rango apaga Guardar
- [ ] Front: Agregar modelo (alta) pregunta; actualizar **no** pregunta; 🧩 Productos va directo a los formularios

### 2026-10-08 — Tienda → Buscar: filtros combinados y precio a cobrar
**Dónde:** `IVarianteRepository.PRECIO_A_COBRAR`, `buscarVariantesAdmin` (talla/color/marca/precio), `buscarVariantesPublicoFiltrado`, `VarianteServiceImpl.filtrarVariantesAdmin` (llave de caché), `VarianteController` (`GET /v1/variantes/admin/filtrar`); front `variante/buscar`
**Tipo:** MySQL/H2 (consultas) · controller · front
**Debe comprobar:**
- [ ] "Con stock" + "Habilitadas" → todos los que tienen stock y están habilitados (artículo y modelo); no sale el deshabilitado ni el de stock 0
- [ ] Talla "m" (minúsculas) con filtros de admin → solo la M
- [ ] Precio 100–100 → el artículo de $100 del modelo y el de $150 con descuento activo a $100; **no** el de $150 con descuento apagado; 150–∞ → el de precio propio 150 y el de descuento apagado
- [ ] La consulta del catálogo público corre con precio (y sigue exigiendo foto, stock y habilitado)
- [ ] Dos búsquedas con distinta fecha/talla/precio no comparten caché
- [ ] Front: quitar un filtro vuelve a buscar con los que quedan; una respuesta vieja que llega tarde no pisa la lista; el precio espera a que dejes de escribir

### 2026-10-08 — Venta directa: nombres de 3 letras, correo y teléfono
**Dónde:** `ClienteSinRegistroImpl.validar()` (`POST /v1/clientes-sin-registro`), `VentaServiceImpl.saveVentaDetalle()` (`nombreReceptor` y cliente embebido); front `shared/validadores-persona.ts`, `variante/venta-directa`
**Tipo:** unitario · controller · front
**Debe comprobar:**
- [ ] Nombre "a" → 400 "El nombre debe tener al menos 3 letras"; "José" (con acento) → se guarda; "Jo3" → 400 (los números no cuentan)
- [ ] Apellido paterno "Lo" → 400 "…al menos 3 letras (o dejalo vacio)"; vacío → se guarda
- [ ] Correo "ana@gmail" → 400 "El correo no es valido"; "ana@gmail.com" → ok
- [ ] Teléfono "55123" → 400; "55 1234 5678" y "+52 55 1234 5678" → ok
- [ ] Venta con `nombreReceptor` "Jo" → 400 "El nombre de quien recibe debe tener al menos 3 letras (o déjalo vacío)"; vacío → ok
- [ ] Front: los mismos avisos debajo de cada campo; Guardar cliente y 💰 Cobrar apagados mientras haya un dato mal; después de cobrar, el buscador muestra el stock nuevo

### 2026-10-08 — Apartado: monto fijo y 🔁 Cambiar forma de cobro
**Dónde:** front `pedidos/detalle-pedido`, `abonos`, `pedidos/grupo-pedido`, `design-system.scss` (`.pk-monto-fijo`)
**Tipo:** front
**Debe comprobar:**
- [ ] Apartado → el monto viene con el saldo y no se puede editar (detalle, Créditos / Abonos, Pagar el grupo completo); "🔒 ¿Por qué no puedo cambiar el monto?" abre y cierra la explicación; "🔁 Cambiar a Ir pagando" abre el formulario con Ir pagando marcado
- [ ] Ir pagando con dinero → el recuadro Apartado gris con "🔒 No se puede: ya dio $X…" escrito (no solo al pasar el mouse)
- [ ] 🕓 Pendiente sale siempre; "Así está ahora" solo en un pedido Pendiente; nunca se puede elegir
- [ ] Al guardar el cambio, el mensaje "Quedó como …" dice qué sigue y se queda hasta "Entendido"

### 2026-10-07 — Datos legales del negocio (dominio `datoslegales`)
**Dónde:** `DatosLegales` (modelo), `DatosLegalesService`, `DatosLegalesJdbcAdapter`, `DatosLegalesController` (`GET`/`PUT /v1/datos-legales`), `SecurityConfig`, `migration_datos_legales.sql`
**Tipo:** unitario (dominio) · MySQL (adaptador) · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] RFC `peaa800101ab1` → se guarda `PEAA800101AB1`; RFC de 12 (empresa) válido; `ABC123` → 400 con el mensaje del RFC
- [ ] Teléfono `(55) 1234-5678` y `+52 55 1234 5678` → `5512345678`; `12345` → 400 "El teléfono tiene que tener 10 dígitos"
- [ ] Correo `sin-arroba` → 400; texto en blanco → se guarda NULL
- [ ] Nombre de 151 caracteres → 400 "no puede pasar de 150 caracteres"
- [ ] `faltantes()` con todo vacío → los 4; con los 4 llenos → vacío y `completos = true`
- [ ] Sin fila en la tabla → GET devuelve todo null y `faltan` con los 4 (no 500)
- [ ] PUT dos veces → sigue habiendo una sola fila (id = 1)
- [ ] GET sin sesión → 200; PUT sin sesión → 401/403; PUT con Escritura en `admin/negocio` → 200
- [ ] Migración: primera corrida crea la tabla con el correo y las 2 columnas; segunda no cambia nada

### 2026-10-07 — Registro guarda la aceptación de Términos
**Dónde:** `RegistroService.registrarUsuario(…, aceptoTerminos)`, `RegistroRequest.aceptoTerminos`, `AuthController.registrar`
**Tipo:** unitario (servicio) · controller
**Debe comprobar:**
- [ ] `aceptoTerminos: true` → `acepto_terminos = 1` y `fecha_acepto_terminos` con la hora
- [ ] `aceptoTerminos: false` → 400 "Debes aceptar los Términos y condiciones para registrarte" y no se crea el usuario
- [ ] Sin el campo (front viejo) → se registra, `acepto_terminos = 0`, fecha NULL
- [ ] `aceptoPrivacidad: false` sigue rechazando igual que antes

### 2026-10-07 — Bots: no prometer lo que no dice el catálogo
**Dónde:** `ChatbotBase.SIN_PROMESAS` (dentro de `REGLAS_DE_RESPETO`)
**Tipo:** unitario
**Debe comprobar:**
- [ ] `REGLAS_DE_RESPETO` contiene el bloque de `SIN_PROMESAS` (lo usan todos los bots)
- [ ] El prompt del sitio, chat en vivo, Instagram y Facebook lo incluyen

### 2026-10-07 — Entrega aparte del pago: dominio `entrega` (📦 Entregar / Regresar)
**Dónde:** `hexagonal/entrega`: `Entrega.entregar()/regresar()`, `PedidoParaEntregar.estaPagado()`, `EntregaService`, `PedidosParaEntregarJdbcAdapter`, `EntregaController` (`POST`/`DELETE /v1/pedidos/{id}/entrega`), regla en `SecurityConfig`
**Tipo:** unitario (dominio) · servicio con puertos simulados · MySQL (adaptador) · controller (URL, status y permiso)
**Debe comprobar:**
- [ ] Contado 'Entregado' (cobrado) sin entregar → entregar → `[id]`; contado 'Pendiente' → 400 "todavía no está pagado (falta $X)"
- [ ] Apartado PAGADO → se entrega; Apartado abierto con $0 → 400 con el faltante exacto
- [ ] Ir pagando (FIADO) debiendo $150 → **sí** se entrega (E8)
- [ ] Cancelado → 400 "está cancelado"; ya entregado → 400 "ya está entregado"
- [ ] Grupo de 3 (uno cancelado, uno ya entregado) → entrega solo el que falta; el cancelado no se toca
- [ ] Grupo con un Apartado sin pagar → 400 "El pedido N del grupo todavía no está pagado"
- [ ] Regresar uno entregado → `entregado = 0`, `fecha_entregado = NULL`; uno sin entregar → 400; grupo → regresa todos los entregados
- [ ] Pedido inexistente → 400 "No existe el pedido N"
- [ ] Adaptador: un grupo **inactivo** (separado) no arrastra a los demás pedidos
- [ ] Controller: sin la acción `entregar` → 403; sin `regresar-entrega` → 403 en el DELETE; con ella → 200 `{data:{pedidos,entregado}}` y caché vaciada
- [ ] `PUT/DELETE /v1/pedidos/**` siguen con su regla de antes (la nueva va antes del comodín y no lo cambia)

### 2026-10-07 — `pedidos.entregado` en listas, detalle, venta directa y cancelar Ir pagando
**Dónde:** `IPedidoRepository` (5 consultas JSON), `TarjetasDePedidoLector` (`grupo.entregadoGrupo`), `PedidoServiceImpl` (detalle y cancelar), `AbonoServiceImpl.cancelar()`, `VentaServiceImpl` (`VentaDirectaRequest.entregado`), `migration_entrega_pedido.sql`
**Tipo:** MySQL (SQL nativo) · unitario (servicio)
**Debe comprobar:**
- [ ] Listas de admin y cliente traen `entregado` true/false (nunca null) y el detalle también
- [ ] Grupo con 2 vivos entregados y 1 cancelado sin entregar → `entregadoGrupo = true`; con uno vivo sin entregar → `false`
- [ ] Venta contado sin `entregado` → entregado=1 (como antes); con `entregado:false` → 0 y estado de pago sin cambio
- [ ] Venta Ir pagando sin campo → 1; con `false` → 0; Apartado siempre 0 aunque mande `true`
- [ ] Cancelar Ir pagando con entregado=0 → stock regresa, mensaje "Ir pagando cancelado. Stock devuelto…"
- [ ] Cancelar Ir pagando con entregado=1 → stock NO regresa (igual que antes); devolución de pagado → sí regresa
- [ ] Cancelar desde Mis pedidos (`PedidoServiceImpl`) → mismas dos reglas
- [ ] Migración: primera corrida marca entregado contado 'Entregado', todo FIADO y todo 'PAGADO'; deja en 0 Apartados abiertos, contados 'Pendiente' y cancelados; **segunda corrida no cambia nada** (aunque se haya regresado uno a mano)

### 2026-10-07 — Filtro de estado partido en Pago y Entrega
**Dónde:** `EstadoBuscado`, `PedidosFiltradosJdbcAdapter` (`ESTADO_CARD`, `PAGO_CARD`, `ENTREGA_CARD`, `GRUPOS`) — `GET /v1/pedidos/buscar?estado=…`
**Tipo:** MySQL (SQL nativo)
**Reemplaza** la entrada del 2026-10-06 "Filtro Estado…": PENDIENTE y POR_COBRAR ya no son opciones propias, valen FALTA_PAGAR.
**Debe comprobar:**
- [ ] FALTA_PAGAR trae contado sin cobrar + Apartado/Ir pagando abiertos; PENDIENTE y POR_COBRAR traen exactamente lo mismo
- [ ] PAGADO trae contado cobrado + crédito liquidado; CANCELADO solo cancelados
- [ ] FALTA_ENTREGAR / ENTREGADO por `entregado`, sin cancelados
- [ ] `PAGADO + FALTA_ENTREGAR` = pagados que no se lo han llevado (AND entre bloques)
- [ ] `FALTA_PAGAR + PAGADO` = la suma (OR dentro del bloque)
- [ ] Grupo: Falta entregar si **algún** vivo no está entregado; Entregado si todos los vivos lo están
- [ ] "Entrega más próxima" y "espera entrega" usan `entregado`, no el estado de pago

### 2026-10-07 — Agregar artículo sube o baja el stock del modelo (B1)
**Dónde:** `VarianteServiceImpl.aplicarAjusteStockModelo()` dentro de `guardarConImagenes()` (`POST /v1/variantes/guardarConImagenes`, `VarianteDetalle.ajusteStockModelo`)
**Tipo:** unitario (servicio con repositorios simulados) · MySQL (transacción)
**Debe comprobar:**
- [ ] Modelo 10, repartido 10, artículo nuevo con 3 y ajuste +3 → modelo 13, artículo guardado
- [ ] Mismo caso sin ajuste → 400 de stock (como antes)
- [ ] Ajuste −3 con modelo 10 y repartido 8 → 400 "No se puede dejar el modelo en 7: ya tiene 8 repartidos"
- [ ] Sin ROLE_ADMIN ni Escritura en productos/buscar, productos/agregar o tienda/venta → 400 "No tienes permiso…", modelo sin cambio
- [ ] Falla la subida de imagen después del ajuste → rollback: el modelo vuelve a su stock
- [ ] Varios detalles del mismo modelo con ajuste → se aplica una sola vez (el primero distinto de 0)
- [ ] Ajuste 0 o ausente → igual que antes

### 2026-10-07 — Gastos: el administrador puede agregar, editar y eliminar (`migration_accion_gastos_admin.sql`)
**Dónde:** acciones `agregar-gasto`, `editar-gasto`, `eliminar-gasto` de `gastos/buscar`
**Tipo:** MySQL (script)
**Debe comprobar:**
- [ ] Después de correrla, ROLE_ADMIN tiene las 3; segunda corrida no duplica filas
- [ ] Si las acciones no existían, las crea; si existían, solo agrega el permiso

### 2026-10-06 — Filtro "Estado" de Mis pedidos dice lo mismo que la card (Apartado nunca es "Pendiente")
**Dónde:** `PedidosFiltradosJdbcAdapter.ESTADO_CARD` (`GET /v1/pedidos/buscar?estado=…`) y `RamoPedidoDetalleServiceImpl.crearPedidoAnticipoFrase()`
**Tipo:** MySQL (SQL nativo) · unitario (servicio)
**Contexto:** un Apartado con `estado_pedido = 'Pendiente'` salía en el filtro "⏳ Pendiente" y su card decía "Por cobrar". El cobro de la frase de listón nacía así (tipo APARTADO, estado 'Pendiente').
**Debe comprobar:**
- [ ] Apartado con estado 'Pendiente' → filtro POR_COBRAR lo trae; filtro PENDIENTE **no** lo trae
- [ ] Ir pagando con estado 'Pendiente' → igual: POR_COBRAR sí, PENDIENTE no
- [ ] Apartado PAGADO → PAGADO; Apartado cancelado → CANCELADO
- [ ] Contado 'Pendiente' → PENDIENTE; contado 'Entregado' → ENTREGADO (sin cambio)
- [ ] Grupo (titular) sigue igual que R14 (no cambia)
- [ ] `crearPedidoAnticipoFrase` → pedido tipo APARTADO con estado APARTADO
- [ ] `BusquedaPedidosMysqlTest` existente: revisar si algún caso armaba un Apartado 'Pendiente' esperando PENDIENTE (quedaría viejo)

### 2026-10-06 — HOTFIX prod: "Crear artículos" (🧩 del modelo) hereda del modelo y el error de guardado dice qué falló
**Dónde:** `VarianteServiceImpl.guardarVariantesPorProductoConImagenes()` (`POST /v1/variantes/inicializarDesdeProducto`) y `CrudAbstractServiceImpl.typeError()`
**Tipo:** unitario (servicio con repositorios simulados) · controller (URL y status)
**Contexto:** en prod (`main`) los artículos nacían vacíos (sin color, marca, descripción, contenido neto ni categoría); en qa/dev ya heredaban desde el 2026-09-30. En prod el alta respondía 400 y la causa **no** era una columna obligatoria (esquema de `variantes` idéntico en qa y prod, ver `ESPECIFICACIONES_AMBIENTES.md`). Cualquier restricción de la base que no fuera duplicado (1062) salía con el texto "El codigo postal ya existe".
**Debe comprobar:**
- [ ] Modelo con color "Negro", marca "Jade", descripción "Bolsa", contenido "1 pza" y categoría 7, stock 1, sin artículos → crear 1 → 201 y el artículo nace con esos 5 datos y stock 1
- [ ] Modelo sin categoría → el artículo nace sin categoría (no truena)
- [ ] Modelo con stock 1 y un artículo habilitado con stock 1 → crear 1 → 404 "Stock insuficiente… Stock disponible: 0"
- [ ] Nunca hereda talla, presentación ni stock (stock siempre 1 por artículo)
- [ ] `save()` que choca con una columna obligatoria vacía (MySQL 1048) → 400 con "No se pudo guardar: Column '…' cannot be null" y una línea `log.error` con el código
- [ ] `save()` con duplicado (1062) → 409 con el mismo texto de antes (no cambia)
- [ ] Consulta para revisar en QA/prod lo que nació (contenido neto y categoría del modelo contra el artículo):
      `SELECT v.id, v.contenido_neto, p.contenido_neto, v.palabra_clave_id, p.palabra_clave_id FROM variantes v JOIN producto p ON p.id = v.producto_id ORDER BY v.id DESC LIMIT 5;`

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
| `BusquedaPedidosMysqlTest` casos r3, r7 y r11 | `dev` | 2026-10-07 | Esperaban la semántica vieja del filtro de estado (Apartado 'Pendiente' en PENDIENTE, `ENTREGADO` = contado cobrado, "espera entrega" por estado de pago). Con la entrega aparte (`pedidos.entregado`) y los bloques Pago / Entrega devuelven otras filas. Los otros 17 casos pasan contra MySQL 8 local. **2026-10-13 (R16):** con 🕓 Pendiente otra vez aparte, la línea de `PENDIENTE` de r3 ya pasa (antes fallaba ahí); r3 sigue fallando más abajo, en `PAGADO`, por la entrega aparte. Antes y después del cambio fallan los mismos 4 (r2, r3, r7, r11). |
| `BusquedaPedidosMysqlTest` caso r2 (`r2_forma_de_cobro`) | `dev` | 2026-10-07 | Espera que "Contado" incluya los pedidos 🕓 Pendiente (900001, 900009). Desde el 2026-10-07 Pendiente es su propia forma de cobro (R2) y Contado es solo el ya cobrado. Corrida contra MySQL 8 el 2026-10-13: 16 de 20 pasan; fallan r2, r3, r7 y r11. |
| `VarianteStockDevolucionTest.guardarConImagenes_elStockPropioNoCuentaComoDisponible`, `VarianteStockValidacionTest.pedirDeMasFalla` | `dev` | 2026-10-08 | Esperan `ExceptionDataNotFound` (404) al pedir más stock del disponible. Desde los códigos HTTP correctos es `ExceptionStockInsuficiente` (**422**). El 2026-10-14 cambió el texto del aviso (*"El modelo «…» no tiene piezas libres…"*), pero sigue terminando en *"(Disponible: X, Solicitado: Y)"*, que es lo que revisan: no fallan por el texto. |
| `CambiarTipoPedidoTest.pedidoEnGrupoNoCambia` | `dev` | 2026-10-13 | Busca el texto viejo *"deshaz el grupo"*. Un pedido unido sigue sin poder cambiarse solo, pero ahora el mensaje es *"…su forma de cobro se cambia para todo el grupo"* (409), porque el grupo completo sí se cambia (grupopedido R20). |
| `RolesControllerTest` (3 casos `..._devuelve400ConElMensaje`) | `dev` | 2026-10-08 | Esperaban 400 del `catch` que tenía el controller. Ese `catch` se quitó: la regla rota ahora sale por `ExceptionGlobal` como **409**. El test arma MockMvc *standalone* sin `ExceptionGlobal`, así que la excepción sale sin manejar. |
| `BuscarPedidosControllerTest` (ajustado, **no** falla) | `dev` | 2026-10-14 | No es una falla: el controller recibe ahora el caché (`ListasDePedidosEnCache`) y el test se tuvo que tocar **solo para que compile** (se le pasa un caché apagado). Si no compila, el Dockerfile (`-DskipTests` sí compila los tests) rompe el deploy. `src/test/resources/application.properties` lleva `pedidos.lista.cache.habilitado=false` para que ningún test dependa de Redis. |
