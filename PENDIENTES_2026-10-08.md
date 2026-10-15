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
| [x] | AWS ya no se usa; llave vieja fuera del repositorio (2026-10-15; falta solo desactivarla en IAM si sigue activa) | `PENDIENTES.md` §3 |
| [ ] | Confirmar si ya se volvieron a subir las fotos de los productos 266, 270, 272 y 273 (anotado el 2026-06-09) | `PENDIENTES.md` §2 |

---

## 2. Subidas

| ✔ | Qué | Estado |
|---|---|---|
| [x] | Lo del 2026-10-08 (filtros de Tienda, Venta directa, ventana 🧩 Agregar artículos, Apartado con monto fijo, Cambiar forma de cobro, Agregar artículo con todos los modelos) **a `dev`** | Subido el 2026-10-08 (solo `dev`, como pediste) |
| [x] | `dev` → **`qa`** con todo lo del 2026-10-08 y del 2026-10-13 (Pruebas 17 a 22 y los cambios de la Prueba 2) | Subido el 2026-10-13, back y front |
| [ ] | Llevar `qa` a **prod** (`main`) cuando QA dé el visto bueno: checklist completo, con el orden de los scripts y lo que hay que revisar en la VPS. **Acordado el 2026-10-13: va antes que el renombrado**, para no llevar a prod lo que no se ha probado | `PASOS_SUBIDA_2026-10-07.md` Parte 2 · `PRUEBAS_PENDIENTES_2026-10-07.md` → "Lo que sigue" |
| [ ] | Rama `rename/variante-a-articulo` (variante → artículo en todo): **se junta con `dev` después de pasar QA a prod** (acordado el 2026-10-13) | Ver §6 |

---

## 3. 👤 Scripts SQL por correr

Siempre primero en `inventario_key_qa` (cubre `dev` y `qa`) y después en prod (`inventario_key`).
Después de correr uno de permisos hay que **volver a entrar** (los permisos viajan en el JWT).

| ✔ | Script | QA | Prod | Para qué |
|---|---|---|---|---|
| [ ] | `migration_accion_tienda_venta_ver_todos.sql` | ✅ 2026-10-08 | ⏳ | Permiso "Ver todos los modelos" en 🧩 Agregar producto (sin él, solo el admin) |
| [ ] | `migration_tema_modal.sql` | ✅ 2026-10-08 | ⏳ (con Jade) | Color "Velo detrás de una ventana abierta" en Personalización |
| [ ] | `migration_renombre_articulo_etiquetas.sql` (vive en la rama `rename/variante-a-articulo`) | ⏳ **al pasar la rama a `dev`** | ⏳ cuando llegue a `main` | Menú "Agregar artículo" y Gestión de roles sin "variante". Pasos: `RENOMBRE_VARIANTE_A_ARTICULO.md` §2 |
| [ ] | `migration_entrega_pedido.sql` | ✅ | ⏳ **antes** del back en `main` | Entrega aparte del pago (sin las columnas truena cualquier consulta de pedidos) |
| [ ] | `migration_datos_legales.sql` | ✅ | ⏳ **antes** del back en `main` | Datos legales; sin la columna falla el login |
| [ ] | `migration_accion_gastos_admin.sql` | ✅ | ⏳ | Botones de gastos para el admin |
| [ ] | `migration_tema_jade.sql` | ✅ | ⏳ (cuando Jade llegue a `main`) | Diseño Jade |
| [ ] | `migration_tema_filtros.sql` | ✅ | ⏳ (con Jade) | Fondo de los filtros en Personalización |
| [ ] | `migration_accion_editar_entrega_direccion.sql` (2026-10-13) | ⏳ **cuando el front con el botón "📍 Dirección" esté en QA** | ⏳ con ese front en `main` | Solo cambia el texto del permiso en Gestión de roles a "Dirección (📍 en la tarjeta)". No hay que volver a entrar |

Lista con el orden para prod y por qué no puede faltar cada uno: `CLAUDE.md`, "Pendiente para prod".

---

## 4. 👤 Pruebas en QA

**Desde el 2026-10-13 el paso a paso está en `PRUEBAS_PENDIENTES_2026-10-13.md`** (solo las que faltan; lo de antes, con tus 💬, sigue en `PRUEBAS_PENDIENTES_2026-10-07.md`).

| ✔ | Prueba | ¿Ya se puede? |
|---|---|---|
| [x] | **2** — Apartado es sin dinero | ✅ 2026-10-13 |
| [x] | **3** — Quitar, cambiar o agregar artículos | ✅ 2026-10-13 |
| [x] | **4** — Cancelar pedidos con abonos | ✅ 2026-10-13 (la ventana de cancelar se veía fea de noche: arreglado, se revisa en la Prueba 23) |
| [x] | **7** — Filtros y detalle con pedidos unidos | ✅ 2026-10-13 |
| [x] | **11** — Entregado aparte del pago | ✅ 2026-10-13 (botón "📍 Entrega" renombrado a "📍 Dirección": Prueba 23) |
| [ ] | **12** — Lo legal (reescrita el 2026-10-13 con dónde revisar cada cosa) | Ya |
| [ ] | **13** — Fondo de los filtros y seguridad (de noche se veía feo: arreglado, falta subirlo) | Después de subir a `qa` |
| [ ] | **15** — Agregar producto, habilitar, Zonas de entrega | Ya |
| [ ] | **16** — 🕓 Pendiente, ⓘ y datos legales | Ya |
| [ ] | **14-QA** — El hotfix de prod en QA | Ya |
| [ ] | **17** — Filtros de Tienda, Venta directa, buscadores y ventana 🧩 | Ya |
| [ ] | **18** — Agregar artículo: todos los modelos, habilitar y agregar stock | Ya |
| [ ] | **19** — Errores con su código, 404 y Home | Ya |
| [ ] | **20** — Cliente que no podía pedir, grupo, modelo agotado, Mis pedidos | Ya (20.2 con 🧪 Preparar prueba 20, después de subir) |
| [ ] | **21** — Fecha de entrega en Pedidos y Entregas por zona | Ya (con 🧪 Preparar prueba 21, después de subir) |
| [ ] | **22** — 🕓 Pendiente en Pago y sin cancelados al abrir | Ya (con 🧪 Preparar prueba 22, después de subir) |
| [ ] | **23** — Colores de noche, listas que se abrían en blanco, chat de día y botón 📍 Dirección (2026-10-13) | Después de subir a `qa` |
| [ ] | **24** — Unir un Ir pagando que ya tiene dinero (lo de K4/K5/K6) | Después de subir a `qa` |
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
| [ ] | **Grupos:** al unir con distinta forma de cobro, "Pasar todos a … y unir"; un solo botón de abonar en un pedido unido; tabla del grupo con solo el total por pedido y abajo Pagado / Saldo del grupo; botón "Detalle de los pagos". *(Apartados unidos que dejan adelanto → todo el grupo a Ir pagando: ✅ hecho en `dev` el 2026-10-13, R20)* | Plan 6.7; skill §4; `PLAN` §10.6 A10 |
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
Qué se cambió, pantalla por pantalla: `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.1 (en la rama).

| ✔ | Qué | Detalle |
|---|---|---|
| [x] | Traer `dev` a la rama, búsqueda completa de "variante" y buscadores con "artículo" (14 pantallas, menú lateral, ayuda ⓘ) | 2026-10-08, rama subida (no está en `dev`) |
| [x] | Script de los textos de la base (menú "Agregar artículo", Gestión de roles sin "variante"): `migration_renombre_articulo_etiquetas.sql`, probado dos veces en base desechable | 2026-10-08, vive en la rama |
| [x] | Decidir cuándo se junta con `dev`: **después de pasar QA a prod** (2026-10-13) | `PRUEBAS_PENDIENTES_2026-10-07.md` → "Lo que sigue", Paso 2 |
| [x] | Revisar qué hay que correr en QA (2026-10-13): solo `migration_renombre_articulo_etiquetas.sql`; vuelto a probar en base desechable (33 textos la primera vez, 0 la segunda). Al traer `dev` chocan 6 archivos del back y 1 del front | Mismo lugar |
| [ ] | 👤 Probar la rama en QA (pruebas R1–R19) | `RENOMBRE_VARIANTE_A_ARTICULO.md` §6 |
| [ ] | 👤 Al juntarla: correr `migration_renombre_articulo_etiquetas.sql` en QA (y en prod cuando suba a `main`) | Mismo documento, §1.1 punto 4 |
| [x] | Segunda revisión, solo "variante" → "artículo" (menú, pantallas, avisos, back, base, seguridad): no quedó ninguno visible | 2026-10-08, `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.2 |
| [x] | De back a front: cada endpoint de artículos → la pantalla que lo usa → su texto dice "artículo" (50 textos del front, 8 mensajes del back, menú "🎡 Rifa de artículos") | 2026-10-08, `RENOMBRE_VARIANTE_A_ARTICULO.md` §1.3 |
| [ ] | ❓ Para después: los textos que dicen "producto" donde es el **modelo** (Agregar modelo, Actualizar modelo, Modelos, Carga rápida) | Mismo documento, §1.2 |
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
