# Subida del 2026-10-07 — Entregado aparte del pago, pantallas homologadas y diseño Jade

Paso a paso para llevarlo a **QA** (hoy) y después a **prod**. Cada paso dice quién lo hace y cómo
se comprueba. Lo que ya se hizo está marcado ✅.

## Qué sube

| Parte | Back (`proyecto_key`) | Front (`producto_venta_online`) |
|---|---|---|
| **Entregado aparte del pago** | Columna `pedidos.entregado`, `POST/DELETE /v1/pedidos/{id}/entrega`, `entregado` en listas y detalle, filtros Pago / Entrega, venta directa con `entregado`, cancelar Ir pagando que no se lo llevó regresa stock | Dos etiquetas en la card, 📦 Entregar y ↺, "¿Ya se lo llevó?" al cobrar, filtros Pago / Entrega |
| **Agregar artículo con stock del modelo** | `ajusteStockModelo` en `guardarConImagenes` | Stock total bloqueado + campo para agregar o quitar |
| **Gastos** | Permisos del administrador (script) | Texto sin permiso |
| **Pantallas homologadas** | — | Ancho de Agregar modelo, encabezados con el color de Personalización, tablas, selects, "Volver" |
| **Diseño Jade** | Script `migration_tema_jade.sql` (ya estaba en `dev`) | Jade ya estaba en `dev` desde el 2026-10-06 |
| **Hotfix de prod "Crear artículos"** | Se baja de `main` a `dev` y `qa` | — |

Detalle del contrato con el front: `CAMBIOS_FRONT.md`, **[BUG-KEY-19]**.

---

## Parte 1 — QA

| # | Paso | Quién | Cómo se comprueba | Estado |
|---|---|---|---|---|
| 1 | `migration_entrega_pedido.sql` en `inventario_key_qa` | Tú | 2 columnas; `entregar` = 21 y `regresar-entrega` = 22 con admin | ✅ 2026-10-07 |
| 2 | `migration_accion_gastos_admin.sql` en `inventario_key_qa` | Tú | 3 acciones con admin = 1 | ✅ 2026-10-07 |
| 3 | `migration_tema_jade.sql` en `inventario_key_qa` | Tú | `estilo` = jade; respaldo con 39 filas | ✅ 2026-10-07 |
| 4 | Back: commit en `dev`, bajar el hotfix de `main` (merge) y push a `dev` | Yo | `mvn compile` sin errores | ✅ |
| 5 | Back: merge `dev` → `qa` y push (dispara `producto-actions-qa.yml`) | Yo | GitHub Actions en verde | ✅ |
| 6 | Front: commit en `dev`, push, merge `dev` → `qa` y push (deploy del front de QA) | Yo | `ng build` sin errores y Actions en verde | ✅ |
| 7 | Cerrar sesión, volver a entrar y `Ctrl + Shift + R` | Tú | Ves las dos etiquetas en Mis pedidos | ⏳ |
| 8 | **Prueba 11** de `GUIA_DE_PRUEBAS_QA.md` (11.1 a 11.8) | Tú | Cada paso como dice la columna "Debes ver" | ⏳ |
| 9 | Lo que salga distinto: 💬 debajo del paso, con número de pedido | Tú | Yo contesto con ↳ | ⏳ |

Volver a entrar es obligatorio: los permisos nuevos (Entregar, Regresar, Gastos) viajan dentro de la
sesión, y una sesión vieja no los trae.

---

## Parte 2 — Prod (cuando QA dé el visto bueno)

### Antes de empezar

1. **Revisar qué trae `qa` que `main` no tiene:** `git log --oneline main..qa` (back y front). Si
   aparece algo que todavía no debe ir a prod (una feature bloqueada), no se hace merge completo: se
   llevan con `git cherry-pick` solo los commits de esta subida (regla de `CLAUDE.md`, "Feature que
   no va a llegar a main junto con el resto").
2. **Respaldo** de las tablas que tocan los scripts:
   ```bash
   mysqldump inventario_key pedidos accion_submenu rol_accion tema_variable > respaldo_2026-10-07.sql
   ```
3. Elegir una hora con poco movimiento: entre el paso 1 y el paso 5 hay 2–3 minutos en los que el
   back viejo convive con las columnas nuevas (no pasa nada: `entregado` nace en 0).

### Pasos

| # | Paso | Quién | Cómo se comprueba |
|---|---|---|---|
| 1 | `migration_entrega_pedido.sql` en `inventario_key` — **antes** del back | Tú | Las 3 consultas del final del script: 2 columnas; resumen por tipo y estado (Apartados abiertos y contados pendientes en 0); `entregar` = 21 y `regresar-entrega` = 22 con admin |
| 2 | `migration_accion_gastos_admin.sql` en `inventario_key` | Tú | 3 acciones con admin = 1 |
| 3 | `migration_tema_jade.sql` en `inventario_key` — solo si el front con Jade sube en esta misma vez | Tú | La consulta (a) da 59; `estilo` = jade; el respaldo tiene filas |
| 4 | Back: merge `qa` → `main` y push (dispara `producto-actions.yml`) | Yo | Actions en verde; `kubectl logs` sin errores al arrancar |
| 5 | Front: merge `qa` → `master` y push | Yo | Actions en verde |
| 6 | Cerrar sesión, volver a entrar y `Ctrl + Shift + R` | Tú | — |
| 7 | Revisión rápida en prod (ver abajo) | Tú | Todo como en QA |
| 8 | Marcar los 3 scripts como corridos en prod en `CLAUDE.md` y vaciar "Pendiente para prod" | Yo | — |

### Revisión rápida en prod (10 minutos)

| # | Haz esto | Debes ver |
|---|---|---|
| 1 | **Mis pedidos** | Cada card con dos etiquetas: Pagado / Falta pagar y Entregado / Falta entregar. Totales iguales que antes |
| 2 | Un contado cobrado de hoy | Pagado + Entregado |
| 3 | Un Apartado abierto | Falta pagar + Falta entregar, sin 📦 |
| 4 | **⚙️ Filtros** → Pagado + Falta entregar | Sale la lista sin error |
| 5 | **Catálogo → 🧩 Agregar producto** → elige un modelo | Stock total del modelo bloqueado y el campo para agregar o quitar |
| 6 | **Gastos** | Botón para agregar gasto |
| 7 | Cualquier pantalla de formulario, de día y de noche | Mismo ancho y encabezado con el color de Personalización |

### Si algo sale mal

| Qué | Cómo se regresa |
|---|---|
| El back nuevo falla | `git revert -m 1 <merge>` en `main` y push. Las columnas nuevas **no** le estorban al back viejo (`entregado` tiene default 0), así que el script no se deshace |
| El front nuevo falla | `git revert -m 1 <merge>` en `master` y push |
| No gusta el diseño Jade | Consultas "PARA VOLVER AL DISEÑO DE ANTES" al final de `migration_tema_jade.sql` (dejan las filas idénticas a antes, probado) |
| Los permisos nuevos | No estorban al back viejo; se pueden dejar |
