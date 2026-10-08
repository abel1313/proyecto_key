# Todo lo pendiente — lista única (al 2026-10-08)

Pedido del dueño (2026-10-08): *"hay que anotar todo lo pendiente"*. Este documento junta en un solo
lugar lo que estaba repartido en otros. Cada punto dice **quién** lo hace y **dónde está el detalle**.
Cuando algo se termine, se marca `[x]` aquí con la fecha.

Leyenda: 👤 lo haces tú · 🛠 lo hago yo (código) · ❓ falta que decidas · ⏳ espera algo de afuera

---

## 1. 👤 Urgente o externo (no es código)

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | **Confirmar que se pagó la orden 609155 de Hosting-Mexico.** La fecha límite era el **4-oct-2026** (ya pasó). Si no se pagó, se cae el DNS y con él la tienda | `DOMINIO_DNS_CORREO.md`; `PENDIENTES_2026-09-29.md` §C |
| [ ] | ⏳ **TikTok Production:** se reenvió a revisión el 2026-09-30. Cuando llegue la respuesta, pegarla tal cual (con fecha) y seguir el "Si la aprueban / Si la rechazan" | `PENDIENTES_2026-09-29.md` §E.2 |
| [ ] | **Meta:** aceptar la invitación de evaluador de *Jade Castañeda* (sigue "Pendiente": mientras no se acepte, sus cuentas no cuentan como "con rol" y los mensajes directos no llegan) | `ALTA_NEGOCIO_META_TIKTOK.md` §8 y §11 |
| [ ] | **Meta:** poner `https://shop.novedades-jade.com.mx/eliminar-datos` en el panel de la app | `ALTA_NEGOCIO_META_TIKTOK.md` (línea "Pendiente en el panel de Meta") |
| [ ] | **Legal:** los datos que solo tú tienes (SAT, facturación, rifas, línea del negocio…) y las dudas de la sección "Dudas que necesito que me contestes" | `LEGAL_PLAN_DE_ACCION.md` |
| [ ] | Confirmar si ya se revocó la llave vieja de AWS en IAM (anotado el 2026-06-09) | `PENDIENTES.md` §3 |
| [ ] | Confirmar si ya se volvieron a subir las fotos de los productos 266, 270, 272 y 273 (anotado el 2026-06-09) | `PENDIENTES.md` §2 |

---

## 2. Subidas

| ✔ | Qué | Estado |
|---|---|---|
| [x] | Lo del 2026-10-08 (filtros de Tienda, Venta directa, ventana 🧩 Agregar artículos, Apartado con monto fijo, Cambiar forma de cobro, Agregar artículo con todos los modelos) **a `dev`** | Subido el 2026-10-08 (solo `dev`, como pediste) |
| [ ] | 👤 Decir cuándo pasa de `dev` a **`qa`**. Hasta entonces, QA **no** tiene lo del 2026-10-08: las Pruebas 17 y 18 y los cambios de la Prueba 2 (2.1, 2.3, 2.4, 2.5) no se pueden hacer todavía | Espera tu "sube a qa" |
| [ ] | Llevar `qa` a **prod** (`main`) cuando QA dé el visto bueno: checklist completo, con el orden de los scripts y lo que hay que revisar en la VPS | `PASOS_SUBIDA_2026-10-07.md` Parte 2 |
| [ ] | Rama `rename/variante-a-articulo` (variante → artículo en todo): se retomó el 2026-10-08; decidir cuándo se junta con `dev` | Ver §6 |

---

## 3. 👤 Scripts SQL por correr

Siempre primero en `inventario_key_qa` (cubre `dev` y `qa`) y después en prod (`inventario_key`).
Después de correr uno de permisos hay que **volver a entrar** (los permisos viajan en el JWT).

| ✔ | Script | QA | Prod | Para qué |
|---|---|---|---|---|
| [ ] | `migration_accion_tienda_venta_ver_todos.sql` | ⏳ | ⏳ | Permiso "Ver todos los modelos" en 🧩 Agregar producto (sin él, solo el admin) |
| [ ] | `migration_tema_modal.sql` | ⏳ (opcional) | ⏳ (con Jade) | Color "Velo detrás de una ventana abierta" en Personalización |
| [ ] | `migration_entrega_pedido.sql` | ✅ | ⏳ **antes** del back en `main` | Entrega aparte del pago (sin las columnas truena cualquier consulta de pedidos) |
| [ ] | `migration_datos_legales.sql` | ✅ | ⏳ **antes** del back en `main` | Datos legales; sin la columna falla el login |
| [ ] | `migration_accion_gastos_admin.sql` | ✅ | ⏳ | Botones de gastos para el admin |
| [ ] | `migration_tema_jade.sql` | ✅ | ⏳ (cuando Jade llegue a `main`) | Diseño Jade |
| [ ] | `migration_tema_filtros.sql` | ✅ | ⏳ (con Jade) | Fondo de los filtros en Personalización |

Lista con el orden para prod y por qué no puede faltar cada uno: `CLAUDE.md`, "Pendiente para prod".

---

## 4. 👤 Pruebas en QA

Paso a paso en `PRUEBAS_PENDIENTES_2026-10-07.md` (resumen arriba de ese documento).

| ✔ | Prueba | ¿Ya se puede? |
|---|---|---|
| [ ] | **2** — Apartado es sin dinero (2.3 ✅; **2.1, 2.4 y 2.5 cambiaron** y hay que repetirlas) | 2.2 y 2.6 ya; 2.1/2.4/2.5 después de subir a `qa` |
| [ ] | **3** — Quitar, cambiar o agregar artículos | Ya |
| [ ] | **4** — Cancelar pedidos con abonos | Ya |
| [ ] | **7** — Filtros y detalle con pedidos unidos | Ya |
| [ ] | **11** — Entregado aparte del pago (11.6 depende del arreglo del recuadro de stock, que está en `dev`) | Ya, salvo 11.6 |
| [ ] | **12** — Lo legal | Ya |
| [ ] | **13** — Fondo de los filtros y seguridad | Ya |
| [ ] | **15** — Agregar producto, habilitar, Zonas de entrega | Ya |
| [ ] | **16** — 🕓 Pendiente, ⓘ y datos legales | Ya |
| [ ] | **14-QA** — El hotfix de prod en QA | Ya |
| [ ] | **17** — Filtros de Tienda, Venta directa, buscadores y ventana 🧩 | Después de subir a `qa` |
| [ ] | **18** — Agregar artículo: todos los modelos, habilitar y agregar stock | Después de subir a `qa` + script de §3 |
| [ ] | **6** — Datos de prueba con un botón | Al final |
| [x] | 10.2 — Mis datos sin spinner | ✅ 2026-10-08 |

---

## 5. 🛠 Acordado pero sin programar

### 5.1 Pedidos y cobro (`PLAN_PEDIDOS_VENTAS_ENTREGA.md`, skill `reglas-pedidos`)

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | **"Saldo a favor del cliente $X"** en el detalle del pedido cuando lo abonado pasa del nuevo total (el back ya lo deja Pagado; falta mostrarlo) | `PENDIENTES_2026-09-29.md` §B |
| [ ] | **Cancelación automática con las reglas nuevas:** solo los "recoger en el local" vencidos (2 días); los de **Entregas por zona ya no**; **Apartados a los 10 días** de atraso. Hoy el job cancela todo "Pendiente" con fecha vencida | Plan 6.2 y 7.1 |
| [ ] | En la búsqueda de artículos del admin: artículo apartado en un pedido aunque no tenga stock libre, con "En pedido #123 · 5 días sin entregar" | Plan 7.1 |
| [ ] | **"No se entregó"** con motivo (sigue Pendiente, queda en su historial) | Plan P3 / 6.5 |
| [ ] | Cambios del cliente **"por confirmar"** (pasar al local, nota) con correo al admin | Plan 6.6 |
| [ ] | **Devoluciones:** "¿Viene bien o dañado?" por artículo al cancelar o regresar; decir siempre "Saldo a favor: $X" al cancelar desde la card; registrar que el dinero ya se devolvió | Plan D1–D5; skill §5 |
| [ ] | **Grupos:** al unir con distinta forma de cobro, "Pasar todos a … y unir"; un solo botón de abonar en un pedido unido; tabla del grupo con solo el total por pedido y abajo Pagado / Saldo del grupo; botón "Detalle de los pagos"; Apartados unidos que dejan adelanto → todo el grupo a Ir pagando | Plan 6.7; skill §4; `PLAN` §10.6 A10 |
| [ ] | **Meses sin intereses:** pantalla de planes (3/6/9/12) con comisión, mínimo $300, casilla "Acepta meses sin intereses" por modelo y por artículo, solo si **todos** lo aceptan | Plan 6.8, 7.2, 7.3 (Bloque 6) |
| [ ] | 🔮 Pago en línea con tarjeta desde la tienda; Apartado liquidado con tarjeta/MSI al recogerlo; pago mixto | Plan §3 "Para después", 10.1, 10.2 |

### 5.2 Diseño

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | Rediseño Jade de **Tienda, Detalle de producto y barra inferior del celular**; cambiar emojis por íconos de línea | skill `diseno-componentes` §0 |
| [ ] | ❓ Rediseño genérico de colores: esperar tu lista de colores nuevos y para qué es cada uno | `PENDIENTES_2026-09-29.md` §F |

### 5.3 Redes sociales

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | Cliente nuevo de TikTok para `business-api.tiktok.com`; pausa de los DM por las respuestas automáticas de Meta; corregir la nota "TikTok comentarios: descartado" | `ALTA_NEGOCIO_META_TIKTOK.md` §7 |
| [ ] | Probar en vivo el bot de mensajes directos de Instagram (depende de la invitación de §1) | `REDES_SOCIALES_PENDIENTE_PROD.md` |

### 5.4 Pruebas automáticas

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | Escribirlas **cuando digas** (regla del 2026-10-06: por ahora solo se anotan) | `TESTS_PENDIENTES.md` (lo del 2026-10-08 hasta arriba) |

---

## 6. 🛠 Renombrado "variante" → "artículo"

Retomado el 2026-10-08 en la rama `rename/variante-a-articulo` (back y front): se trajo todo lo de
`dev` y se hizo la búsqueda completa. **Regla del dueño (2026-10-08):** en pantalla todo dice
**artículo**, y los buscadores dicen "Buscar artículo…", **menos Tienda → Buscar** (lo que ve el cliente).

| ✔ | Qué | Detalle |
|---|---|---|
| [ ] | 👤 Probar la rama y decidir cuándo se junta con `dev` | `RENOMBRE_VARIANTE_A_ARTICULO.md` |
| [ ] | Lo que **no** se renombra todavía: tablas y columnas, campos del JSON que comparten back y front, nombres de clases | `CLAUDE.md`, "Renombrado en curso" |

---

## 7. ❓ Decisiones que faltan

| ✔ | Pregunta | Dónde |
|---|---|---|
| [ ] | Dudas del plan legal | `LEGAL_PLAN_DE_ACCION.md`, "Dudas que necesito que me contestes" |
| [ ] | Si el saludo del bot de mensajes va en **cada** respuesta o solo la primera vez por persona | `ALTA_NEGOCIO_META_TIKTOK.md` |
| [ ] | Colores nuevos del rediseño | §5.2 |
| [ ] | Cuándo la rama del renombrado pasa a `dev` | §6 |

**Ya decidido el 2026-10-08 (no se vuelve a preguntar):** a 🕓 Pendiente no se regresa un pedido;
el monto de un Apartado va fijo; en Agregar artículo lo que se agrega va al modelo y luego se ofrecen
sus artículos; habilitar un modelo no toca sus artículos.
