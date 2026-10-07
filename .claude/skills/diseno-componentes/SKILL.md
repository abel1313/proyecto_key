---
name: diseno-componentes
description: Cómo crear o cambiar componentes visuales del front de Novedades Jade (input, select, textarea, tabla, formulario, card, botón, aviso, chip…) para que tomen los colores de día y de noche de Personalización y cambien todos juntos desde un solo lugar. Usar SIEMPRE antes de crear una pantalla o componente nuevo en producto_venta_online, antes de tocar estilos (.scss), y cuando el dueño pida "cámbialo conforme a la skill" o "que quede con el diseño".
---

# Diseño de componentes — un solo lugar para todo el look

**Regla del dueño (2026-10-01):** todo componente nuevo (input, select, tabla, formulario, card,
botón…) tiene que verse igual que los demás, de día y de noche, con los colores que estén elegidos
en **Sistema → Personalización** en ese momento. Y tiene que ser **genérico**: si en Personalización
se cambia un color, cambia en **toda** la app de una vez, sin ir componente por componente.

---

## 0. Diseños seleccionables y el diseño Jade (2026-10-01)

**Jade es el diseño por default.** En **Sistema → Personalización → Diseños predefinidos** hay 5:
**Jade** (Predeterminado), **Clásico** (el de fábrica de antes), Jade profundo elevado, Neutros
cálidos y Teal transformador. Cada uno trae **todas** las claves: colores, letra, tamaños y botones,
de día y de noche. Escoger uno escribe esas filas en `tema_variable`.

| Pieza | Qué es | Dónde |
|---|---|---|
| Fila `estilo` (jade / clasico) | Enciende o apaga la **capa Jade**: letra Inter, títulos peso 500, labels 12px, campos de 44px, tablas en mayúsculas con línea dorada, botones de librerías delineados | front `src/tema-jade.scss` (selector `body[data-estilo="jade"]:not(.sin-diseno)`) |
| Filas de letra (`font-family`, `title-weight`, `h1-size`…`input-height`) | Tamaños de la capa Jade, editables | `tema_variable`, grupo "Estilo y letra" |
| `--btn-primary-bg / -text / -border / -hover-bg / -hover-text` | **Todo botón principal** de una pantalla usa estos 5. Jade: delineado (transparente + borde de acento). Clásico: relleno con el degradado de marca | grupo "Botones" |
| Colores nuevos | `--app-accent-text` (precios, enlaces), `--app-tint` (seleccionado/hover), `--app-gold` y `--app-gold-line` (detalles y línea dorada), `--badge-bg/-text` (etiquetas), `--app-text-soft` y `--app-text-faint` (4 niveles de texto), `--app-hairline` (filas), `--app-glass` / `--app-glass-strong` (barras y headers de cristal), `--shadow-md/-lg`, `--app-bg-fx` (degradado del fondo) | `styles.scss` + `tema_variable` |
| `--pk-ink` | Texto sobre un color de estado sólido (verde/rojo/ámbar) | `styles.scss` |
| `--overlay-text` | Texto o ícono sobre una **foto o velo oscuro** (✕ de quitar imagen, contador, estrella): blanco en todos los diseños | `styles.scss` |
| `body.sin-diseno` | **Login, Registro y Olvidé contraseña** se quedan siempre con el diseño de antes (lo pidió el dueño). AppComponent pone la clase en `/login*` y `/usuarios/registrar` | `app.component.ts`, `styles.scss` |
| `--app-accent-rgb`, `--sb-bg-rgb` | Ya no se escriben a mano: TemaService los calcula de `brand-1` y `sb-body-bg` | `tema.model.ts` (`RGB_DERIVADOS`) |

**Reglas que salen de esto:**
- Botón principal nuevo → `background: var(--btn-primary-bg); color: var(--btn-primary-text); border: 1px solid var(--btn-primary-border)`. Nunca el degradado de marca a mano (así el diseño decide si es relleno o delineado).
- Seleccionado / activo (chip, pestaña) → los mismos `--btn-primary-*`, o `.pk-chip.is-on`.
- Texto sobre foto → `var(--overlay-text)`. Texto sobre color de estado → `var(--pk-ink)`. Texto sobre la franja de color de un header → `var(--card-header-text)`.
- Encabezados de cristal (`*-header__content`, `*-card__head`): lo que va adentro usa `--app-text`, `--app-border`, `--app-tint`; un buscador ahí usa `--input-bg` / `--input-border`.
- `styles.scss` deja **transparente** todo `*-header__content` (una sola franja por pantalla, 2026-10-06). **Excepción:** `vb-header__content` (Tienda) y `pl-header__content` (Catálogo → Modelos), porque ahí el `__content` **es** el recuadro y no hay franja afuera; su fondo es `--filtros-panel-bg`. Si una pantalla nueva tiene el mismo caso, se agrega a ese `:not(...)` (si no, se queda sin fondo, como pasó el 2026-10-07).
- Nada de clases de color de Bootstrap (`text-white-50`, `text-white`, `bg-light`…) en pantallas con tokens: traen `!important` y le ganan al token (el subtítulo de Tienda quedó blanco sobre blanco).
- Login y Registro **no** se tocan; su paleta literal es a propósito.
- Piezas nuevas de Jade para pantallas nuevas (equivalencias del archivo del dueño `.jd-*`): `.pk-kicker`, `.pk-rule`, `.pk-row-rule`, `.pk-chip`, `.pk-tag--accent/--outline/--neutral`, `.pk-price`, `.pk-glass-btn`, `.pk-photo`, `.pk-product`, `.pk-product-grid`, `.pk-tabbar`, `.pk-sidebar`, `.pk-h1…`, transiciones `.pk-enter-*` (en `design-system.scss`).
- Pendiente (segundo paso, pedido del dueño): rediseño de **Tienda, Detalle de producto y barra inferior de celular** según `tema-jade` (menú angosto, franja de categorías, foto 4:5, tallas, "Agregar a la bolsa", transiciones) y cambiar emojis por íconos de línea Phosphor.

**Cómo se hizo el barrido (para repetirlo en una pantalla nueva o vieja):** los colores fijos de
93 `.scss` se pasaron a tokens con un script que mira la propiedad (texto, fondo, borde), si está en
el bloque de noche y el fondo de la misma regla. Lo que no pudo decidir solo quedó revisado a mano.
Comprobación: Login/Registro iguales píxel por píxel contra `dev`; Clásico igual que antes.

## 1. Cómo funciona hoy (la cadena)

```
styles.scss            → define los colores (tokens) para día (body.theme-light) y noche (body.theme-dark)
  ↓ los sobrescribe
Personalización        → tabla `tema_variable` en la base (una fila = un token, con valor de día y de noche)
  (Sistema → Personalización, ruta `personalizacion`)
  ↓ los aplica en vivo
TemaService (front)    → lee GET /v1/tema-variable/activo al arrancar y aplica el valor de día o de noche
ThemeService (front)   → decide día/noche: automático por hora (de 19:00 a 6:00 es noche) o con el botón ☀️/🌙
  ↓ los usa
design-system.scss     → componentes compartidos `.pk-*` hechos SOLO con esos tokens
```

Archivos (front, `src/`): `styles.scss` (tokens), `design-system.scss` (componentes `.pk-*`),
`app/services/tema/tema.service.ts` y `tema.model.ts` (aplica Personalización; `ALIAS_LEGACY`),
`app/services/theme/theme.service.ts` (día/noche).
Back: entidad `TemaVariable`, endpoints `/v1/tema-variable`, semilla en `migration_tema_variable.sql`.

## 2. Las 6 reglas

1. **Cero colores escritos a mano en un componente.** Nada de `#00875A`, `rgb(…)`, `white`, `black`
   en un `.scss` o en un `style=""`. Siempre `var(--token)` (tabla de la sección 3).
2. **Primero lo compartido.** Si existe una clase `.pk-*` para eso (sección 4), se usa esa. No se
   inventa un prefijo propio (`dp-`, `vd-`, `gp-`…) para algo que ya existe.
3. **Si falta un componente, se agrega a `design-system.scss`**, no dentro de la pantalla. Así la
   siguiente pantalla ya lo tiene.
4. **Si falta un color, se crea el token completo** (sección 5): en los dos bloques de
   `styles.scss` (día y noche) **y** como fila de `tema_variable` (migración) para que aparezca en
   Personalización. Un token que no está en Personalización no se puede cambiar desde ahí.
5. **Se revisa de día y de noche** antes de dar por terminado (captura de los dos). Un texto que se
   lee de día y no de noche es error.
6. **Lo de una pantalla vieja se arregla cuando se toca por otra razón**, igual que el renombrado
   `variante → artículo`: no se hace un barrido de toda la app de golpe (salvo que el dueño lo pida).

## 3. Qué token usar para qué

| Para | Token |
|---|---|
| Fondo de la página | `--app-bg` |
| Texto normal / secundario | `--app-text` / `--app-text-muted` |
| Bordes generales | `--app-border` |
| Color de marca (botón principal, enlaces, foco) | `--app-accent` (= `--brand-1`); hover `--app-accent-hover`; fondo suave `--app-accent-soft`; texto encima `--app-accent-ink` |
| Degradado de marca | `--brand-1`, `--brand-2`, `--brand-3` |
| Card | `--card-body-bg`, `--card-header-bg`, `--card-footer-bg`, `--card-text`, `--card-text-muted`, `--card-border`, `--card-radius`, `--card-shadow`, `--card-shadow-hover` |
| Input / select / textarea | `--input-bg`, `--input-text`, `--input-border`, `--input-placeholder`, `--input-focus-border`, `--input-focus-shadow` |
| Formulario (sección) | `--form-bg`, `--form-section-bg`, `--form-card-radius` |
| Recuadro de búsqueda y filtros / cada filtro (casilla, fecha, precio) | `--filtros-panel-bg` / `--filtro-bg` (Personalización → Formularios, 2026-10-07). Los `<select>` de filtro siguen la regla global de selects (`--input-bg`) |
| Tabla | `--table-header-bg`, `--table-header-text`, `--table-row-hover`, `--table-row-active`, `--table-border` |
| Éxito / alerta / peligro / info | `--pk-success`, `--pk-warning`, `--pk-danger`, `--pk-info` (+ `-soft` para fondos y `-to` para degradados) |
| Menú lateral | `--sb-*` |
| Botón principal | `--btn-primary-bg`, `--btn-primary-text`, `--btn-primary-border`, `--btn-primary-hover-bg`, `--btn-primary-hover-text` |
| Precio, enlace, ícono activo | `--app-accent-text` |
| Seleccionado / hover | `--app-tint` |
| Etiqueta ("3 unidades") | `--badge-bg`, `--badge-text` |
| Dorado (título pequeño, línea) | `--app-gold`, `--app-gold-line` |
| Texto sobre foto / sobre color de estado | `--overlay-text` / `--pk-ink` |
| Encabezados de pantalla | `--header-text`, `--header-text-muted`, `--header-brand*` |

Antes de usar uno, confirmar que existe: `grep -n "\-\-nombre" src/styles.scss`.

## 4. Componentes compartidos que ya existen (`design-system.scss`)

| Componente | Clases |
|---|---|
| Página | `.pk-page`, `.pk-title`, `.pk-subtitle`, `.pk-text` |
| Card (imagen, header, body, footer; todos opcionales) | `.pk-card` y sus bloques `__…` |
| Grid de cards | `.pk-grid` |
| Botón | `.pk-btn` + `--primary`, `--secondary`, `--danger`, `--icon`, `--accion`, `--sm`, `--block` |
| Campo de formulario | `.pk-field` (`--full`), `.pk-label`, `.pk-input` (`--error`), `.pk-error`, `.pk-hint` |
| Sin resultados | `.pk-empty` |

**Faltan** (se agregan al `design-system.scss` la primera vez que se necesiten, con tokens):
`select` y `textarea` propios (hoy se reusa `.pk-input`), **tabla** (`.pk-table` existe pero copiada
dentro de 4–6 pantallas, no compartida), checkbox / radio, chip o etiqueta de estado, aviso dentro de
la pantalla (nota informativa), modal. Los avisos `Swal` toman estilo global de `styles.scss`
(`.swal2-*`), que hoy tiene colores escritos a mano.

## 4.1 Botón de regresar — en toda pantalla nueva (regla del dueño 2026-10-01)

Toda pantalla nueva que no sea la principal de un menú lleva botón de regresar, **alineado con el
borde izquierdo del contenido, arriba del título**, como en Política de Privacidad. Nunca suelto en
la esquina de la página cuando la tarjeta está centrada.

- Se usa el compartido `<app-boton-volver>` (`shared/boton-volver`): vuelve a la pantalla anterior
  real (con sus filtros) y si se entró directo usa `fallback`.
- Si la pantalla tiene una tarjeta centrada, se le pasa su ancho y el margen lateral de la página:
  `<app-boton-volver anchoContenido="820px" margen="16px" fallback="/productos/buscar">` (los mismos
  valores que el `max-width` de la tarjeta y el `padding` lateral de su página). Si el contenido
  ocupa todo el ancho, solo `margen`. Si no hay margen, nada.
- Se revisa midiendo: el borde izquierdo del botón y el de la tarjeta tienen que coincidir a 1360,
  900 y 400 px de ancho.
- Alineadas así (2026-10-01, solo en `dev`): Agregar Modelo, Actualizar Modelo, Nuevo Producto,
  actualizar artículo, carrito, Cargar catálogo Excel, Entregas por zona, Nuevo cliente, ver
  cliente, Agregar mi compra, Cambiar contraseña, Mi perfil y Mis datos. Las demás pantallas con
  botón de regresar ya estaban alineadas.

## 5. Cómo agregar un color nuevo (token) — completo

1. `styles.scss`: agregar `--nuevo-token` en **los dos** bloques (`body.theme-light` y `body.theme-dark`).
2. Back: migración `migration_tema_<tema>.sql` con un `INSERT INTO tema_variable (clave, etiqueta,
   grupo, tipo, valor_claro, valor_oscuro, orden)` idempotente (`WHERE NOT EXISTS` por `clave`), con
   los **mismos** valores que en `styles.scss` (así no cambia nada visualmente al correrla). `clave`
   va sin `--`. `etiqueta` en palabras del dueño ("Fondo de los avisos"). Probarla según la regla de
   `CLAUDE.md` (base desechable, dos veces) y anotarla en la tabla de migraciones.
3. Si el valor también alimenta otros nombres viejos, agregarlo a `ALIAS_LEGACY` en `tema.model.ts`.
4. Usarlo solo con `var(--nuevo-token)`.

## 6. Crear un componente o pantalla nueva — checklist

- [ ] Se armó con clases `.pk-*`; lo que faltó se agregó a `design-system.scss`.
- [ ] `grep -nE "#[0-9a-fA-F]{3,8}\b|rgba?\(" <archivo>.scss` no encuentra colores escritos a mano
      (salvo `rgba(var(--…-rgb), …)`).
- [ ] Tokens nuevos dados de alta completos (sección 5).
- [ ] Captura de día y de noche (`body.theme-light` / `body.theme-dark`), en escritorio y en celular.
- [ ] Probado cambiando un color en Personalización: el componente nuevo cambia sin tocar código.
- [ ] Si se tocó un componente compartido o un token: prueba de cada pantalla que lo usa, antes y
      después (skill `pruebas-de-impacto`).

## 7. "Cámbialo conforme a la skill" (pantalla existente)

1. Cambiar sus clases propias por las `.pk-*` equivalentes; lo que no exista, agregarlo al sistema.
2. Reemplazar cada color escrito a mano por su token (si no hay token, crearlo: sección 5).
3. Borrar los estilos locales que ya cubre `design-system.scss`.
4. Captura antes y después, de día y de noche, y avisar si algo se ve distinto.

---

## 8. Pendiente — rediseño genérico (anotado 2026-10-01)

El dueño va a **cambiar y agregar colores nuevos, para front y back, y el diseño completo**, para que
todo quede genérico y se cambie desde Personalización. Cuando llegue:
- Los colores nuevos entran como **tokens + filas de `tema_variable`** (sección 5), nunca a mano.
- "Back" aquí significa el catálogo `tema_variable` (migraciones y su pantalla), que es de donde el
  front lee los colores.
- Deuda que hay que resolver para que de verdad cambie todo de un solo lugar (medido 2026-10-01):
  - ~79 archivos `.scss` de pantallas tienen colores escritos a mano.
  - `.pk-table`, `.pk-btn`, `.pk-input` están **copiados** dentro de 6 pantallas de admin
    (gestion-roles, gestion-menu, gestion-palabras-clave, lugares-entrega…) en vez de usar los
    compartidos.
  - Solo ~8 pantallas usan ya `.pk-card` / `.pk-btn`; las demás tienen su prefijo propio.
  - `.pk-error` / `.pk-input--error` usan `#dc2626` en vez de `--pk-danger`; los `.swal2-*` de
    `styles.scss` tienen colores fijos.
  - Personalización hoy solo expone ~25 tokens (marca, página, card, tablas, menú lateral,
    formularios); los de estados (`--pk-success`…), chat y encabezados no se pueden cambiar desde ahí.
- Antes de empezar: el dueño dice qué colores y para qué; se arma la lista de tokens nuevos y se
  confirma con él antes de tocar pantallas.
