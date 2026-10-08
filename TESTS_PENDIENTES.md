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

### 2026-10-06 — Renombre variante → artículo: rutas `/v2/articulos` en espejo de `/v1/variantes`
**Dónde:** `VarianteController`, `ConfigurarRifaVarianteController`, `GanadorRifaControllerImpl.continuarVariante`,
`ProductosControllerImpl` (reporte sin artículos, compartir imágenes), `ResenaController`, `SecurityConfig`
**Tipo:** controller (MockMvc con seguridad real)
**Debe comprobar:**
- [ ] Para cada `requestMatchers` que contiene `/v1/variantes`, la misma regla contiene `/v2/articulos` (recorrer la configuración, no a mano).
- [ ] `GET /v2/articulos/para-pedido` sin token → 401; con token sin `agregar-articulo` ni `cambiar-articulo` → 403; igual que `/v1/variantes/para-pedido`.
- [ ] `GET /v2/articulos/getAll` y `/getOne/1` sin token → 401 (no públicos), igual que en `/v1`.
- [ ] `GET /v2/articulos/buscar?termino=bol` sin token → 200 con el mismo cuerpo que `/v1/variantes/buscar?termino=bol`.
- [ ] `GET /v2/articulos/5/producto-id` y `/v1/variantes/variante/5/producto-id` → mismo `productoId`.
- [ ] `PUT /v2/articulos/5/habilitar` con un rol sin la acción `habilitar` → 403.
- [ ] `DELETE /v2/articulos/deleteBy/5` con la acción `eliminar` → 200 `"Artículo eliminado correctamente"`.
- [ ] `/v2/configurarRifaArticulo/porRifa/1` sin pantalla de rifas → 403; con ella → mismo cuerpo que `/v1/configurarRifaVariante/porRifa/1`.
- [ ] `GET /v1/productos/admin/sin-articulos/reporte` sin la acción `descargar-excel` → 403; con ella → archivo `productos_sin_articulos.xlsx`.
- [ ] La app arranca sin "Ambiguous mapping" (las dos rutas de `producto-id` y la doble ruta de clase).

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
| `BusquedaPedidosMysqlTest` casos r3, r7 y r11 | `dev` | 2026-10-07 | Esperaban la semántica vieja del filtro de estado (Apartado 'Pendiente' en PENDIENTE, `ENTREGADO` = contado cobrado, "espera entrega" por estado de pago). Con la entrega aparte (`pedidos.entregado`) y los bloques Pago / Entrega devuelven otras filas. Los otros 17 casos pasan contra MySQL 8 local. |
