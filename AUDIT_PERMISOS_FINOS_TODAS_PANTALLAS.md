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

## Grupo Ventas — criterio confirmado por el usuario 2026-09-08

**`tienda/venta-directa` y `abonos` — decisión: permiso completo, sin acciones puntuales.** "No
hay muchas opciones ahí" -- si el rol tiene el permiso de la pantalla, entra y usa todo (Ver +
Editar general, como estaba). No se toca nada.

**`gastos/buscar` — ✏️ cambiado.** El usuario confirmó que aquí sí hay varios botones que separar
(pestaña "Gastos" únicamente -- "Ventas" y "Reporte" son de solo consulta):
- `agregar-gasto`, `editar-gasto`, `eliminar-gasto` — 3 acciones independientes.
- Migración: `migration_accion_gastos.sql`
- Front: `AuthService` agregado a `all.component.ts`; los 3 botones gateados con `tieneAccion`.

## Grupo Reportes — criterio confirmado por el usuario 2026-09-08

**`dashboard` — decisión: permiso completo.** Si tiene el permiso, entra y lo ve, sin separar
nada.

**`reportes` (Reportes de ventas) — ✏️ cambiado.** El usuario pidió que fuera configurable
("hay varios botones"): cada una de las 5 pestañas es su propia acción, para poder dar por
ejemplo "Diario" sin dar "Promociones":
- `reporte-diario`, `reporte-mensual`, `reporte-cliente`, `reporte-mas-vendidos`,
  `reporte-promociones`.
- Migración: `migration_accion_reportes.sql`
- Front: `AuthService` agregado a `reportes.component.ts`; las 5 pestañas gateadas con
  `tieneAccion`.

## Grupo Rifas — revertido a permiso completo (criterio confirmado por el usuario 2026-09-08)
Decisión: "para todas sus opciones, solo si tiene el permiso de rifa va a poder entrar y verlas"
-- sin acciones puntuales. Se había implementado una separación fina (eliminar/ejecutar/recuperar,
migración `migration_accion_rifas.sql`) y se **revirtió por completo**: los 6 archivos de
`agregar-rifa`/`rifa-mes`/`buscar-rifa` (.ts y .html) volvieron a su versión de antes de esa
migración, y el archivo de migración se borró (nunca se había ejecutado en QA/prod).

## Grupo Flores eternas — revertido a permiso completo (criterio confirmado por el usuario 2026-09-08)
Decisión: "solo que tenga el permiso de cada opción del menú va a poder entrar y verlo" -- sin
acciones puntuales, cada pantalla (catalogos/entregas/ramos-admin/frases) es todo-o-nada. Se
**revirtió** la separación fina implementada antes (habilitar/eliminar/aprobar/rechazar,
migración `migration_accion_flores.sql`): los 4 componentes volvieron a su versión previa y el
archivo de migración se borró (nunca ejecutado).

## Grupo Marketing — revertido a permiso completo (criterio confirmado por el usuario 2026-09-08)
Decisión: "lo mismo, si tiene permiso lo puede ver y modificar" -- sin acciones puntuales. Se
**revirtió** la separación fina de `admin/promociones`/`admin/cinta` (habilitar/enviar-correo/
eliminar, migración `migration_accion_marketing.sql`): ambos componentes volvieron a su versión
previa y el archivo de migración se borró (nunca ejecutado).

## Grupo Sistema — revertido a permiso completo, salvo 1 pendiente (criterio confirmado 2026-09-08)
Decisión: "si tiene la opción lo va a poder ver y modificar -- modificar me refiero a que puede
agregar cosas... lo que deja hacer esa opción" -- sin acciones puntuales. Se **revirtió** la
separación fina de `gestion-menu` (eliminar-menu/eliminar-submenu), `gestion-menu/roles`
(eliminar rol) y `personalizacion`/`admin/reconciliacion-imagenes` (eliminar/limpiar-bd) —
migración `migration_accion_sistema.sql` borrada (nunca ejecutada), los 4 componentes vueltos a
su versión previa.

**⚠️ `usuarios/buscar` (Usuarios) — sigue PENDIENTE, no se tocó.** Editar/Eliminar/Activar una
cuenta de usuario hoy está completamente abierto a cualquiera con Ver ahí. "Editar" acá puede
incluir cambiar el ROL de otro usuario (riesgo de auto-escalar privilegios) -- necesito tu
criterio antes de decidir, igual que se hizo con `mis-pedidos`.

## Sin grupo (Clientes, Favoritos) — ⚠️ pendiente de confirmar si se mantiene o se revierte
`favoritos` sin cambios (lista personal del cliente). `clientes/buscar` sí quedó con acción
puntual `verificar-correo` (migración `migration_accion_clientes.sql`, ya implementada) -- dado
el patrón de las últimas decisiones (todo-o-nada salvo excepción explícita), confirmar si esto
también se revierte o se queda así.

## Grupo Catálogo — ⚠️ pendiente de confirmar si se mantiene o se revierte
`productos/agregar` y `carga-imagenes` quedaron con la acción `escanear-codigo` (y
`generar-codigo-barras` en carga-imagenes) separada del resto (migración
`migration_accion_agregar_carga_imagenes_escaner.sql`, ya implementada) -- mismo caso que
Clientes: confirmar si se mantiene o se revierte a permiso completo.

---

## Cierre del audit (2026-09-08)

Las 45 pantallas del catálogo quedaron revisadas. Estado final tras las decisiones del usuario:
- **Con acciones puntuales (confirmado):** Modelos/Tienda/Envíos (de sesiones previas),
  `pedidos/mis-pedidos` (criterio detallado del usuario), `gastos/buscar`, `reportes`.
- **Pendiente de confirmar si se mantienen o se revierten a permiso completo:** `clientes/buscar`
  (verificar-correo) y Catálogo (`productos/agregar`/`carga-imagenes`, escanear).
- **Revertidas a permiso completo por decisión explícita del usuario:** Rifas, Flores eternas,
  Marketing, Sistema (salvo `usuarios/buscar`, que sigue pendiente de criterio, no de reversión).
- **Permiso completo desde el principio, confirmado:** `tienda/venta-directa`, `abonos`,
  `dashboard`, y el resto de pantallas de un solo flujo o de puro reporte.
- **Sigue pendiente de tu criterio de negocio, sin implementar:** `usuarios/buscar`
  (editar/eliminar/activar cuenta -- riesgo de auto-escalar rol).

**Antes de ejecutar en QA:** revisar cada migración vigente, correrlas una por una, y probar con
Gestión de roles que las etiquetas/descripciones se vean bien y que quitar la acción a un rol de
prueba realmente oculte el botón correspondiente.

---

## Ejecución en QA y prod (2026-09-08, confirmado por el usuario)

El usuario ya ejecutó estas 8 migraciones en QA **y** en prod:

```
migration_accion_agregar_carga_imagenes_escaner.sql
migration_accion_rifas.sql
migration_accion_flores.sql
migration_accion_marketing.sql
migration_accion_sistema.sql
migration_accion_clientes.sql
migration_accion_mis_pedidos.sql
migration_pedido_hora_punto_encuentro.sql
```

**⚠️ Ojo con las 4 de en medio.** `migration_accion_rifas.sql`, `migration_accion_flores.sql`,
`migration_accion_marketing.sql` y `migration_accion_sistema.sql` se ejecutaron **antes** de que
el usuario pidiera revertir esos 4 grupos a permiso completo. Resultado: las filas
`accion_submenu` (y sus `rol_accion`) de Rifas/Flores eternas/Marketing/Sistema **siguen vivas en
la BD de QA y prod**, pero el código del front ya no las lee (`tieneAccion` se quitó de esas
pantallas al revertir). Van a aparecer como checkboxes en Gestión de roles que no controlan nada
visible — mismo patrón que el hallazgo original de "Editar huérfano en Tienda" del inicio de este
audit.

**Decisión del usuario:** dejarlas tal cual en la BD por ahora, no borrarlas — quedan dadas de
alta por si más adelante se retoma la separación fina de alguno de estos 4 grupos. Anotado aquí
para no olvidar el porqué si alguien las encuentra después.

**Las otras 4 sí quedaron consistentes** (migración ejecutada = código del front que las usa,
sin huérfanos): `migration_accion_agregar_carga_imagenes_escaner.sql` (Catálogo — pendiente de
decidir si se revierte, ver arriba), `migration_accion_clientes.sql` (pendiente de decidir
igual), `migration_accion_mis_pedidos.sql` y `migration_pedido_hora_punto_encuentro.sql` (ambas
con su código de front ya activo).

---

## Antes de fusionar a `dev`
- Ejecutar cada migración en QA primero, probar con el checklist correspondiente, luego prod.
- Seguir el flujo normal de `CLAUDE.md`: `dev → qa → main`, nunca al revés.
- No mezclar con la Tarea B (`feature/filtro-seguridad`) — ver `ROADMAP_PRUEBAS_PERMISOS_TIENDA_Y_FILTRO_SEGURIDAD.md`.
