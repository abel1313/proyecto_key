# Audit de permisos finos — todas las pantallas admin (2026-09-08)

Barrido sistemático de las 45 pantallas del catálogo (`CATALOGO_MENU_SUBMENU.md`) buscando
botones/filtros/acciones "seleccionables" que hoy son todo-o-nada (solo Ver/Editar genérico de
`submenu`) y merecen separarse en `accion_submenu` — mismo patrón ya usado en Modelos
(`productos/buscar`), Tienda (`tienda/buscar`) y Envíos (`entregas-zona`).

**Criterio para separar un botón en su propia acción puntual:** que tenga sentido de negocio
darlo/quitarlo independientemente del resto (ej. "puede escanear pero no eliminar", "puede ver
pero no exportar"). Botones que son parte del MISMO flujo de una sola tarea (ej. "guardar" +
"tomar foto" en un formulario de creación) **no** se separan — quedan bajo el Ver general de la
pantalla. Cada pantalla auditada queda anotada aquí con la decisión y el porqué, se haya
cambiado algo o no.

Rama de trabajo: `feature/permisos-finos` (back en `proyecto_key`, front en
`producto_venta_online`). Migraciones nuevas: prefijo `migration_accion_*` en
`src/main/resources/static/`, mismo estilo que las ya existentes (ver cualquiera como ejemplo).

---

## Grupo Catálogo

### `productos/agregar` (Agregar modelo) — ✏️ cambiado
Botón 📷 "escanear código de barras" (para prellenar el campo al crear) era público — cualquiera
con acceso a la pantalla lo usaba sin permiso propio. Se separó como acción puntual
`escanear-codigo`, dada por default a todo rol que hoy tiene Ver ahí (preserva comportamiento).
El resto del formulario (guardar, subir/tomar foto, quitar imagen) es un único flujo de creación,
no se separa.
- Migración: `migration_accion_agregar_carga_imagenes_escaner.sql`
- Front: `add.component.html` — botón gateado con `authService.tieneAccion('productos/agregar', 'escanear-codigo')`

### `carga-imagenes` (Carga rápida de imágenes) — ✏️ cambiado
Mismo caso: botón 📷 escanear Y botón 🎲 generar código automático, ambos públicos. Se separaron
como 2 acciones puntuales (`escanear-codigo`, `generar-codigo-barras`) — son 2 mecanismos
distintos para lo mismo (llenar el campo), tiene sentido poder dar uno sin el otro (ej. un rol de
bodega que solo debe escanear el código real, nunca inventar uno). Dadas por default a todo rol
con Ver ahí.
- Migración: `migration_accion_agregar_carga_imagenes_escaner.sql` (misma que arriba)
- Front: `carga-imagenes.component.ts`/`.html` — se agregó `AuthService` al componente (no lo
  tenía), botones gateados igual que arriba.

### `tienda/venta` (Agregar producto — crear variante) — sin cambios
Formulario de creación de variante (batch por número/letra, fotos, guardar). Todo es un único
flujo de "crear esta variante" — no hay botón separable con sentido de negocio propio (no hay
escáner ni acción de borrado/exportar aquí). Se deja solo con Ver.

### `tienda/cargar-excel` (Cargar Excel) — sin cambios
`CargaArchivoComponent`: seleccionar archivo, limpiar, subir. Un único flujo, sin acciones
separables. Se deja solo con Ver.

---

## Grupo Envíos + Pedidos

### `lugares-entrega` (Zonas de entrega) — ya auditado antes
Ya tiene "Eliminar" separado como acción puntual (ver `migration_accion_lugares_entrega_eliminar.sql`
y la sección correspondiente en `ROADMAP_PRUEBAS_PERMISOS_TIENDA_Y_FILTRO_SEGURIDAD.md`). Nada
nuevo que agregar.

### `pedidos/historial-mp` (Historial de pagos MP) — sin cambios
Pantalla de solo consulta (buscar/filtrar/paginar pagos de Mercado Pago). Los filtros (modo,
estado) son parte del mismo flujo de búsqueda de un reporte admin-only, no hay caso de negocio
para dárselos/quitárselos por separado. Se deja solo con Ver.

### `pedidos/mis-pedidos` — ✏️ cambiado (criterio confirmado por el usuario 2026-09-08)
El usuario dio el criterio exacto: separar los 2 grupos de filtros por su propio nombre (mismo
agrupamiento que ya existía en el HTML), cada botón de la tarjeta con su propia acción, y cada
botón del detalle del pedido con la suya. 13 acciones nuevas, en 4 categorías:

- **Filtros — buscador de pedido**: `filtro-pagados`, `filtro-cancelados`
- **Filtros — buscador por lugar**: `filtro-normal`, `filtro-apartado`, `filtro-fiado`
- **Tarjeta de pedido**: `editar-entrega`, `cobrar`, `imprimir-ticket`, `enviar-correo`,
  `cancelar-pedido`
- **Detalle del pedido**: `editar-ramo`, `abonar`, `ajustar-cantidad`

Se preservó el comportamiento actual con cuidado porque no todo estaba gateado igual:
- `cobrar` y `enviar-correo` ya usaban `isAdminUser` (hardcodeado ROLE_ADMIN) tanto en la tarjeta
  como en el detalle -- se les agregó `tieneAccion` ENCIMA de `isAdminUser` (no lo reemplaza), y
  se dieron solo a ROLE_ADMIN en la migración.
- `editar-entrega`, `imprimir-ticket` (en la tarjeta) y `cancelar-pedido` NO tenían ningún gate
  -- las ve cualquiera con Ver, incluido un cliente viendo sus propios pedidos. Se les dio la
  acción a TODO rol con Ver, para no romper la vista del cliente.
- `imprimir-ticket` y `enviar-correo` en el DETALLE sí están dentro de un bloque `*ngIf="isAdmin"`
  (inconsistente con la tarjeta, pero es el comportamiento actual) -- se respetó igual.
- `abonar` ("Registrar abono") tampoco tenía ningún gate -- se le dio a TODO rol con Ver (un
  cliente puede abonar su propio pedido a crédito).
- `editar-ramo` y `ajustar-cantidad` ya eran ROLE_ADMIN-only (`isAdmin`) -- se quedaron así,
  solo se les agregó `tieneAccion` encima.

- Migración: `migration_accion_mis_pedidos.sql`
- Front: `mis-pedidos.component.ts` (authService pasó de `private` a `public`) +
  `mis-pedidos.component.html` (filtros y botones de tarjeta gateados); `detalle-pedido.component.ts`
  (getters nuevos: `puedeImprimirTicketDetalle`, `puedeReenviarComprobante`, `puedeAbonar`, y
  `puedeEditarRamo`/`puedeEditarLineas` con `tieneAccion` agregado) + `.html`.

## Grupo Ventas — ⚠️ PENDIENTE, requiere pase dedicado (no se tocó)
Las 3 pantallas del grupo son manejo de dinero real, igual que `mis-pedidos`:
- `tienda/venta-directa` (POS presencial): cobrar, apartado/fiado, terminal (enviar/cancelar/
  cerrar), agregar/quitar línea, promociones.
- `abonos`: registrar abono, cancelar pedido (con y sin pago), transferir variante entre pedidos
  cancelados.
- `gastos/buscar`: agregar/editar/eliminar gasto (el único caso "mecánico" del grupo — eliminar
  vs editar es la misma separación ya hecha en Categorías/Lugares de entrega, sin ambigüedad de
  negocio).

Junto con `mis-pedidos`, esto forma un bloque de 4 pantallas de dinero que prefiero revisar
contigo antes de separar acciones — a diferencia de "escanear código" o "filtro de catálogo",
aquí una separación mal pensada (ej. dar "cobrar" sin "cancelar", o "terminal" sin "cobrar") sí
puede generar un hueco operativo real. `gastos/buscar` es la excepción fácil (eliminar vs
editar) y la puedo hacer sin preguntar si prefieres que avance con esa sola.

## Grupo Reportes — sin cambios
`dashboard`: solo un botón de refrescar, nada que separar. `reportes`: pestañas de solo lectura
(Diario/Mensual/Por cliente/Más vendidos/Promociones) — todas dentro del mismo visor de reportes,
sin acciones destructivas ni de escritura. Se dejan bajo el Ver general.

## Grupo Rifas — ✏️ cambiado
Las 3 pantallas administran UNA rifa activa a la vez (configurar, agregar variante/participante,
importar, copiar de anterior, modo prueba) — eso se queda como un solo flujo bajo Editar general.
Se separaron solo las acciones de borrado y las de ejecución/recuperación (mismo criterio que
"eliminar" en Categorías/Lugares, y consecuentes por sí mismas, no solo consulta):
- `rifas/agregar`: `eliminar` (quitar variante), `eliminar-concursante` (quitar concursante —
  esta pantalla también da de alta concursantes, es otro tipo de dato, se separó aparte)
- `rifas/mes`: `eliminar` (quitar concursante)
- `rifas/buscar`: `ejecutar-rifa` (botón "Ir a ejecución"), `recuperar-rifa` (botón "Recuperar"
  una rifa mensual cerrada)
- Migración: `migration_accion_rifas.sql`
- Front: `agregar-rifa`/`rifa-mes`/`buscar-rifa` .ts (se agregó `AuthService`) y .html (botones
  gateados con `tieneAccion`)

## Grupo Flores eternas — ✏️ cambiado
`flores/ramos` (Vitrina) y `flores/configurar` (Arma tu ramo) son públicas, de cara al cliente
(ver ramo, pedir, WhatsApp) -- sin acciones admin, excluidas del audit.
- `flores/catalogos`: acciones `habilitar` y `eliminar`, cubren las 5 pestañas (tipos/colores/
  cantidades/accesorios/frases de listón) con una sola acción cada una -- mismo patrón repetido
  en las 5, no 5 funcionalidades distintas.
- `flores/entregas`: `eliminar` (botón "Quitar plazos", destructivo).
- `flores/ramos-admin`: `habilitar` (activar/desactivar ramo armado; no hay botón de borrar ahí).
- `flores/frases`: `aprobar` y `rechazar` separados (bandeja de moderación, 2 decisiones
  independientes con sentido de negocio propio).
- Migración: `migration_accion_flores.sql`
- Front: se agregó `AuthService` a los 4 componentes; botones gateados con `tieneAccion`.

## Grupo Marketing — ✏️ cambiado (parcial)
`promociones` (pública), `admin/facebook` (un único flujo de armar+publicar un post) y
`admin/hashtags` (edición simple por red, sin borrar/activar) quedan sin cambios.
- `admin/promociones`: `habilitar` (activar/desactivar) y `enviar-correo` (correo masivo de la
  promo) separados -- sentido de negocio claro (armar promos sin poder mandar correos masivos).
- `admin/cinta`: `habilitar` y `eliminar` (reordenar con ↑↓ se queda en el flujo de edición
  general, sin separar).
- Migración: `migration_accion_marketing.sql`
- Front: `AuthService` agregado a ambos componentes; botones gateados con `tieneAccion`.

## Grupo Sistema — ✏️ cambiado (parcial) + ⚠️ 1 pendiente

`admin/negocio`, `admin/chat`, `admin/presentacion`, `admin/diagnostico-imagenes` (solo lectura)
y `admin/cache` son cada una un único flujo, sin acciones separables. Sin cambios.

**⚠️ `usuarios/buscar` (Usuarios) — PENDIENTE, no se tocó.** Editar/Eliminar/Activar una cuenta
de usuario hoy está completamente abierto a cualquiera con Ver ahí (sin `tieneAccion` ni
`tieneEscritura`, ningún gating). Es sensible por el mismo motivo que las pantallas de dinero —
"Editar" acá puede incluir cambiar el ROL de otro usuario (riesgo de auto-escalar privilegios si
se separa mal), así que necesito tu criterio antes de decidir la granularidad, igual que con
`mis-pedidos`/Ventas.

**Cambiado:**
- `gestion-menu` (Menús y submenús): `eliminar-menu` y `eliminar-submenu` separados (2 tipos de
  borrado distintos, riesgo estructural -- borrar un submenú puede romper el permiso de esa
  pantalla en todo el sistema). El resto (crear/editar) se queda en Editar general.
- `gestion-menu/roles` (Gestión de roles): `eliminar` (borrar un rol completo) separado. La
  edición de permisos en sí (los checkboxes Ver/Editar/acciones) es el propósito mismo de la
  pantalla, se queda bajo Editar general -- solo se separó el borrado de rol.
- `personalizacion`: `eliminar` (borrar una variable de tema) separado.
- `admin/reconciliacion-imagenes`: `limpiar-bd` separado del botón "Iniciar" (diagnóstico de solo
  lectura) -- es el único botón marcado "danger" en la pantalla, borra registros de la BD.
- Migración: `migration_accion_sistema.sql`
- Front: `AuthService` agregado a los 4 componentes; botones gateados con `tieneAccion`.

## Sin grupo (Clientes, Favoritos) — ✏️ cambiado
`favoritos` es la lista personal del propio cliente (quitar de favoritos, carrito) -- no es
pantalla admin, sin cambios.
- `clientes/buscar`: `verificar-correo` separado (cubre los 2 botones -- marcar verificado y
  resetear verificación -- son la misma acción de soporte, un solo permiso).
- Migración: `migration_accion_clientes.sql`
- Front: `AuthService` agregado a `clientes-buscar.component.ts`; botones gateados.

---

## Cierre del audit (2026-09-08)

Las 45 pantallas del catálogo quedaron revisadas. Resumen:
- **7 migraciones nuevas** (`migration_accion_agregar_carga_imagenes_escaner.sql`,
  `migration_accion_rifas.sql`, `migration_accion_flores.sql`, `migration_accion_marketing.sql`,
  `migration_accion_sistema.sql`, `migration_accion_clientes.sql`), todas con el mismo patrón:
  crean la(s) acción(es) y se las dan a todo rol que hoy tiene Ver en esa pantalla (preserva el
  comportamiento actual -- nadie pierde acceso al ejecutar la migración, pero un rol NUEVO que se
  cree después ya no las trae por default).
- **2 pantallas pendientes de tu criterio de negocio, sin implementar** (dinero y cuentas de
  usuario -- ver secciones "Grupo Ventas" y "`usuarios/buscar`" arriba): `mis-pedidos` +
  `venta-directa` + `abonos` + `gastos/buscar` (gastos/buscar es la excepción fácil, lista para
  hacer si confirmas), y `usuarios/buscar`.
- El resto de pantallas (~20) se revisaron y se decidió NO separar nada -- son un solo flujo
  (formularios de creación, pantallas de un solo propósito) o pura consulta/reporte, documentado
  caso por caso arriba con el porqué.

**Antes de ejecutar en QA:** revisar cada migración nueva, correrlas una por una, y probar con
Gestión de roles que las etiquetas/descripciones se vean bien y que quitar la acción a un rol de
prueba realmente oculte el botón correspondiente.

---

## Antes de fusionar a `dev`
- Ejecutar cada migración en QA primero, probar con el checklist correspondiente, luego prod.
- Seguir el flujo normal de `CLAUDE.md`: `dev → qa → main`, nunca al revés.
- No mezclar con la Tarea B (`feature/filtro-seguridad`) — ver `ROADMAP_PRUEBAS_PERMISOS_TIENDA_Y_FILTRO_SEGURIDAD.md`.
