# preferenciafiltro — filtros guardados por usuario

Bloque 2 del plan (`PLAN_PEDIDOS_VENTAS_ENTREGA.md`, secciones 8 y 8.1).

Hasta el 2026-09-29 los filtros de **tienda/buscar** y **productos/buscar** se recordaban solo en
memoria (`VarianteService.filtrosCache`, `ProductoService.prodFiltrosCache`): sobrevivían a navegar
dentro de la app, pero se perdían al recargar, cerrar sesión o entrar desde otro dispositivo.

## Reglas (acordadas con el dueño el 2026-09-29)

| # | Regla | Dónde vive |
|---|---|---|
| R1 | Solo el **personal** guarda filtros (admin y empleados). Un **cliente** (`ROLE_USUARIO`) no: su tienda ya trae sus filtros por defecto. | `FiltrosGuardadosService` → `SoloPersonalException` (403) |
| R2 | Solo dos pantallas: `tienda-buscar` y `productos-buscar`. Además hay que tener permiso de esa pantalla. | `Pantalla` + `SecurityConfig` |
| R3 | Cada quien los suyos: el usuario sale del token, nunca del request. | `QuienGuardaPort` |
| R4 | Se guardan **solo los filtros** (casillas, talla, color, marca, precio, fechas), **no** el texto buscado ni la página. | El front arma el objeto; el back guarda lo que llega |
| R5 | Un objeto JSON de máximo 2000 caracteres (un juego de filtros real mide ~300). | `FiltrosGuardados` |
| R6 | Una fila por usuario y pantalla: guardar otra vez reemplaza. | `UNIQUE (usuario_id, pantalla)` |
| R7 | Guardar `{}` es lo mismo que borrar ("Limpiar"). | `FiltrosGuardadosService` |

**Borrado:** real (`DELETE`), no baja lógica. No hay historial que conservar: es una preferencia.

## Endpoints

`/v1/preferencias-filtro/{pantalla}` — `GET` (200 o 204 si no hay), `PUT` (200, o 204 si se mandó
`{}`), `DELETE` (204). Contrato completo en `CAMBIOS_FRONT.md`.

## Tabla

`preferencia_filtro`, mapeada por `mis/productos/entity/PreferenciaFiltro` — **no** vive en este
dominio porque Spring solo escanea entidades y repositorios JPA dentro de `mis/productos`
(`MisProductosApplication`). Mismo caso que `grupopedido`. Migración:
`resources/static/migration_preferencia_filtro.sql`.
