# Revisión de producción — 2026-11-23

**Pedido del dueño:** revisar qué hay en prod (`inventario_key`, rama `main`), corregir la documentación y decir qué falta
para llevar lo de `dev`/`qa` a prod, **sin WhatsApp y sin cobro con tarjeta**. Documento de solo lectura: no se tocó código,
`main`, `qa` ni `dev`; no se corrió nada en prod.

## 1. Lo verificado en prod (SELECT corridos por el dueño)

| Qué se comprobó | Resultado |
|---|---|
| Pantallas `corte-caja`, `grupos-precio`, `campanas`, `fletes` en `submenu` | Existen las 4 |
| Columna `pedidos.datos_visibles` | Existe |
| 14 tablas esperadas (campañas, precio por cantidad, fletes, cálculo de precio, etc.) | Ninguna falta |
| Columnas `detalle_venta_variantes.tipo_precio`, `campana_persona.cliente_id`, `campana_persona.cliente_sin_registro_id` | Existen |

**Conclusión:** a nivel de estructura (tablas y columnas), prod ya está al día con lo que `main` necesita. Lo que estaba
atrasado era la **documentación** (el `CLAUDE.md` marcaba «⏳ pendiente en prod» cosas que ya están y la tabla «Pendiente para prod»
seguía listando las migraciones 1–10 del paso del 2026-10-15). Quedó corregido en el `CLAUDE.md` de la rama `dev` (ver §6).

## 2. Qué ya está en prod (rama `main`)

Renombres variante→artículo y modelo→producto (código); filtro de seguridad del chat y chatbot endurecido; escáner de celular y foto a
artículo existente; tope de 200 artículos; hotfix de seguridad (CRUD heredado, chat WebSocket, dipomex/flores); bloqueo y caché de stock;
Corte de caja; historial de cálculo de precio; campañas y precio por cantidad; reporte por categoría; `tipo_precio`; Fletes; rifa con
`concursanteId`; cambio de precio de un artículo; datos del cliente; dividir pedido; `datos_visibles`.

## 3. Qué falta para prod (sin WhatsApp ni tarjeta)

**Tablas nuevas que hacen falta: NINGUNA.** Lo que falta es de solo lectura o de código.

| # | Paquete | Para qué sirve | Cómo se hace | Migración |
|---|---|---|---|---|
| 1 | **Revisión de ventas** (pestaña 🔎 en Reportes) y **gráficas del Dashboard** | Avisa de pedidos, ventas, abonos y stock que no cuadran, y da gráficas al admin | Código en back y front (cherry-pick, §5) | `migration_accion_reporte_revision.sql` (da la acción `reporte-revision` solo a ROLE_ADMIN). Idempotente |
| 2 | **Seguridad 2.ª tanda** | CORS/swagger solo donde corresponde, webhook de Facebook cerrado si no hay secreto, topes de `size` en listados | Solo código (cherry-pick). Revisar antes qué ya está en `main` | Ninguna |
| 3 | **Vista previa para compartir artículos** | Ver cómo queda el artículo antes de mandarlo | Solo código y front | Ninguna |
| 4 | Escáner por cuenta / celular rápido | Por confirmar si ya está en `main` | Comparar commits | Ninguna |
| 5 | Propuestas y guiones k6 (CI, respaldos, índices, nginx) | Solo documentos; no son cambios de prod | Nada que subir | Ninguna |

**No van a prod nunca:** `qa_neutralizar_contactos.sql` (borra correos y teléfonos reales), `script_categoria_por_fecha.sql` y
`script_categoria_por_id.sql` (solo con orden expresa del dueño), `datos_prueba_*`, `limpiar_*`.

## 4. Lo que NO se puede saber por existencia (textos y permisos) — consultas para el dueño

Corre estas en `mysql>` sobre `inventario_key` (solo SELECT). Si devuelven 0 filas, hay que correr el script indicado.

```sql
-- 1) ¿El admin tiene las 4 pantallas nuevas?
SELECT s.ruta, r.nombre_rol
FROM submenu s
JOIN rol_submenu rs ON rs.submenu_id = s.id
JOIN roles r ON r.id = rs.rol_id
WHERE s.ruta IN ('reportes/corte-caja','productos/grupos-precio','productos/campanas','productos/fletes')
ORDER BY s.ruta;

-- 2) ¿Ya dice «Dirección» el permiso editar-entrega? (migration_accion_editar_entrega_direccion.sql)
SELECT clave, etiqueta FROM accion_submenu WHERE clave = 'editar-entrega';

-- 3) ¿El menú ya dice «artículo» en vez de «variante»? (migration_renombre_articulo_etiquetas.sql)
SELECT ruta, etiqueta FROM submenu WHERE etiqueta LIKE '%ariante%' OR etiqueta LIKE '%rtículo%';

-- 4) ¿Existe la acción de Revisión? (debe dar 0 filas hoy; después de la migración, 1 con ROLE_ADMIN)
SELECT a.clave, r.nombre_rol FROM accion_submenu a
LEFT JOIN rol_accion ra ON ra.accion_submenu_id = a.id
LEFT JOIN roles r ON r.id = ra.rol_id
WHERE a.clave = 'reporte-revision';

-- 5) Tablas que dependen del reporte por categoría y los fletes
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('reporte_generado','compra_lote','compra_lote_gasto','compra_lote_producto','venta_detalle_lote');
```

La columna `etiqueta` de la consulta 3 puede llamarse distinto; si da error de columna, pasar el resultado de `DESCRIBE submenu;`.

## 5. Cómo se lleva a prod sin arrastrar tarjeta ni WhatsApp

1. **No se hace `merge qa → main`**: traería `migration_pago_tarjeta.sql` y el código de Mercado Pago.
2. Se crea una rama `hotfix/<nombre>-prod` **desde `main`** y se hace `cherry-pick` solo de los commits del paquete (como `hotfix/campana-prod`).
3. Antes de tocar prod: respaldo `mysqldump -u root -p --single-transaction --routines --triggers inventario_key | gzip > ~/prod_antes_$(date +%Y%m%d_%H%M).sql.gz` y comprobar que pesa megas. Nunca restaurar un dump encima de prod.
4. Orden: **migración → back → front**. `scp migration_accion_reporte_revision.sql ubuntu@vps-da9a48f5:~/` desde la PC del dueño; en el VPS (prompt bash): `mysql -u root -p inventario_key < ~/migration_accion_reporte_revision.sql`; comprobar con la consulta 4.
5. Después de la migración: **volver a entrar** (las pantallas y acciones viajan en el JWT).
6. Un push a `main` **despliega solo** (GitHub Actions): solo con orden expresa del dueño.
7. Al terminar, anotar la migración en el registro de `CLAUDE.md` de `main` (que tiene su propio `CLAUDE.md`: llevar allí la misma corrección por cherry-pick, también con orden del dueño).

## 6. Lo que se corrigió en la documentación

En el `CLAUDE.md` de la rama `dev`: tabla «Migraciones ya corridas» (campañas, precio por cantidad, precio base, corte de caja, `datos_visibles`
y cálculo de precio → ✅ en prod, verificado 2026-11-23; tarjeta → excluida; dos migraciones de solo textos → por verificar; fila nueva de
`reporte-revision`) y la tabla «Pendiente para prod» (quedaba con las migraciones 1–21; ahora solo lo realmente pendiente).

## 7. Antes de más prod (ya reconocido por el dueño)

Textos legales (aviso de privacidad y términos), decisión sobre devoluciones y la prueba de acceso entre cuentas de la auditoría de seguridad.

## 8. Excluido por el dueño (para que no se cuele)

- **Cobro con tarjeta:** `migration_pago_tarjeta.sql` → tablas `pago_tarjeta_intento` y `pago_tarjeta_devolucion`, casilla `acepta_msi` en `producto` y `variantes`.
- **WhatsApp:** `migration_whatsapp_publicaciones.sql` (columnas `grupo_whatsapp`, `codigo_ref`, `modo_venta` en `publicacion_social`) y
  `migration_whatsapp_pantalla.sql` (pantalla `whatsapp/publicaciones`), ramas `feature/whatsapp-*`.

## 9. Bitácora de lo corrido en prod (2026-11-23, por el dueño en el VPS)

| Qué | Resultado |
|---|---|
| 14 tablas esperadas | Ninguna falta (`Empty set`) |
| Columnas `tipo_precio`, `cliente_id`, `cliente_sin_registro_id` | Existen |
| Pantallas `corte-caja`, `grupos-precio`, `campanas`, `fletes` | Las 4 con `ROLE_ADMIN` |
| Texto de `editar-entrega` | Ya dice «Dirección (📍 en la tarjeta)»: no hizo falta `migration_accion_editar_entrega_direccion.sql` |
| `migration_accion_reporte_revision.sql` | **Corrida.** Verificado: 1 fila con `reporte-revision`, «Pestaña Revisión (🔎)» y `ROLE_ADMIN` |

Tras la migración hay que volver a entrar (los permisos viajan en el JWT). La pestaña 🔎 y las gráficas del Dashboard se verán cuando el back y el front de ese paquete suban a prod.
Pendiente sin comprobar: `migration_renombre_articulo_etiquetas.sql` (solo textos; va con el front del renombrado).
