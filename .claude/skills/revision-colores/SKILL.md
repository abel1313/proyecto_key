---
name: revision-colores
description: Revisar que TODO se lea en el front de Novedades Jade, de día y de noche, con los 5 diseños de Personalización — listas desplegables (select, p-dropdown, datalist, Swal), menús, ventanas, tarjetas y textos — midiendo el contraste con Playwright en lugar de suponer. Usar SIEMPRE que el dueño diga "no se ve", "se ve feo de noche", "sale en blanco", "no se lee"; antes de entregar cualquier cambio de .scss, de tokens o de Personalización; y al crear un componente con un menú o una ventana que se abre encima de la pantalla.
---

# Revisión de colores — que todo se lea, de día y de noche

**Por qué existe (2026-10-13):** el dueño reportó dos cosas el mismo día: submenús azules o negros de
noche, y listas que *"cuando salían todas las opciones aparecía en blanco y no se veían"*. Las dos ya se
habían "arreglado" varias veces, pantalla por pantalla, mirando a ojo y con un solo diseño. Se
repetían porque nadie medía, y porque cada parche tomaba el fondo de un lado y la letra de otro.
Esta skill dice cómo **medir** y cuáles son las reglas que evitan que vuelva a pasar.

Complementa a `diseno-componentes` (qué token usar) y a `pruebas-de-impacto` (qué probar).

---

## 1. Las 5 reglas

1. **Fondo y letra salen siempre del mismo par.** Cada fondo tiene su letra:

   | Fondo | Letra que va encima |
   |---|---|
   | `--app-bg` (página) | `--app-text`, `--app-text-muted` |
   | `--card-body-bg` / `--card-bg` | `--card-text`, `--app-text` |
   | `--card-header-bg` (franja de encabezado) | `--card-header-text` |
   | `--input-bg` (campo) | `--input-text` |
   | **`--menu-bg` (menú que se abre)** | **`--menu-text`** |
   | color de marca sólido (`--brand-1`, `--btn-primary-bg`) | `--app-accent-ink` / `--btn-primary-text` |
   | color de estado sólido (`--pk-success`…) | `--pk-ink` |
   | foto o velo oscuro | `--overlay-text` |

   El error clásico es cruzarlos. Pasó el 2026-10-13 en el chat: el encabezado usa `--card-header-bg`
   (claro de día en Jade) y el nombre "Asistente Virtual" usaba `--app-accent-ink` (casi blanco, para ir
   sobre el color de marca). Resultado: blanco sobre claro.

2. **Lo que se abre encima (menús, listas, paneles) usa `--menu-bg` / `--menu-text` / `--menu-scheme`.**
   Nunca `--input-bg`: en 4 de los 5 diseños de noche vale `rgba(255,255,255,0.05)`, casi transparente.
   - **`select` normal:** el menú lo dibuja el **navegador**, no la página. Según el navegador, respeta el
     fondo y la letra de cada `<option>`; o solo la letra, y pinta el fondo claro u oscuro según
     `color-scheme`; o ninguno de los dos. Por eso los tres valores tienen que estar del mismo lado.
   - **Cómo se calcula el trío:** `TemaService.derivarColoresMenu()` (front, `services/tema`) lo saca de
     los colores ya resueltos: el fondo del campo encima del de la página (queda sólido), la letra del campo
     si contrasta 4.5 o más (si no, negro o blanco) y el esquema según qué tan claro es ese fondo. Se
     recalcula al cargar Personalización, al previsualizar y al cambiar de día a noche. Los valores de
     fábrica están en `styles.scss` (bloques claro y oscuro).
   - **Reglas globales** (`styles.scss`, "Menú de opciones"): `select` e `input[list]` llevan
     `color-scheme: var(--menu-scheme)`; `option` y `optgroup` llevan `--menu-bg` y `--menu-text`;
     `.p-dropdown-panel` igual.
   - **Login y Registro (`body.sin-diseno`)** se quedan con su diseño de antes, a propósito.

3. **Nada de parches por pantalla para los menús.** Hasta el 2026-10-13 había 4 pantallas
   (`venta-directa`, `venta-variante`, `gestion-lugares`, `entregas-zona`) y una regla global que forzaban
   el `select` blanco con letra oscura y `color-scheme: light !important`. Cuando la regla global de
   `<option>` les puso la letra del tema, quedó menú blanco con letra blanca. Si un menú no se lee, se
   arregla el trío o la regla global, nunca con `#ffffff !important` en una pantalla.

4. **Se mide, no se mira.** Con el script de esta skill (sección 2), con los **5 diseños** y los
   **colores de fábrica** (sin filas), **de día y de noche**. Un diseño solo no basta: el bug de los
   menús no se veía con Jade y sí con los otros 4.

5. **Después de tocar `styles.scss` o `tema-jade.scss`, correr el detector de comentarios sin cerrar**
   (`comentarios-sin-cerrar.py`). Un `/*` sin su `*/` no da error: se traga todas las variables hasta el
   siguiente `*/`. El 2026-10-13 dos de esos borraban de noche `--sb-text-active`, `--sb-bg-item` y los
   colores de las tarjetas, y el menú lateral salía azul o negro.

---

## 2. Cómo medir (paso a paso)

Todo se hace en el contenedor, sin tocar QA. El front se sirve compilado y la API se simula, así que no
hace falta el back.

```bash
# 1. Compilar el front (repo producto_venta_online)
npx ng build --configuration development          # deja dist/

# 2. Servir dist/ (skill probar-en-local tiene servidor-estatico.js)
setsid node .claude/skills/probar-en-local/servidor-estatico.js <ruta>/dist 18090 &

# 3. Sacar las filas de los 5 diseños (de presets-diseno.ts, el mismo archivo que usa Personalización)
npx --no-install esbuild src/app/tema-admin/models/presets-diseno.ts --bundle --platform=node \
    --format=cjs --outfile=/tmp/presets.cjs --log-level=error
node .claude/skills/revision-colores/filas-de-presets.js /tmp/presets.cjs ./filas
#   -> filas/filas-jade.json, filas-clasico.json, filas-jade-profundo.json, filas-neutros-calidos.json,
#      filas-teal-transformador.json y filas-sin.json (colores de fábrica)

# 4. Medir (las pantallas que se tocaron; para todo, la lista de la sección 4)
for f in filas/filas-*.json; do for h in 12:00 21:00; do
  node .claude/skills/revision-colores/medir-contraste.js --filas $f --hora $h \
       --salida res/$(basename $f .json)-$h --rutas "/pedidos/mis-pedidos /tienda/buscar" --texto
done; done

# 5. Comentarios sin cerrar
python3 .claude/skills/revision-colores/comentarios-sin-cerrar.py src
```

**Qué mide `medir-contraste.js`:**
- **Menú de cada `select`.** Calcula dos casos, porque no se sabe qué navegador tiene el dueño: (a) el
  navegador respeta fondo y letra de la opción; (b) solo respeta la letra y pinta el fondo por
  `color-scheme` (blanco o `#3b3b3b`). Reporta el peor de los dos.
- **Panel de cada `p-dropdown`.** Lo abre, lo mide y saca captura.
- **`select` dentro de una ventana Swal.**
- **Con `--texto`: cada texto visible** contra el fondo real que tiene detrás (suma las capas
  semitransparentes). Si detrás hay imagen o degradado, no se puede medir con colores: lo marca
  *"ver captura"* y se revisa la captura.
- Mínimo **4.5** (texto normal). Con `--minimo 3` se ven solo los graves. Para números grandes, 3 basta.

**Cómo decide día o noche:** con la hora (`--hora 12:00` / `21:00`). ThemeService pone noche de 19:00 a
6:00, igual que en la app.

**Límites:** no puede abrir el menú nativo de un `select` (el navegador no lo dibuja en la página): lo
calcula con los estilos de las opciones. Una pantalla que necesita datos para pintar su lista no la
mide si la API simulada regresa vacío: en ese caso se agrega la respuesta al `route` del script, o
se revisa a mano que no tenga reglas propias de color (sección 3).

---

## 3. Revisión en el código (además de medir)

```bash
# Reglas propias de pantalla que pintan select/option/p-dropdown con colores fijos (deben salir vacías)
grep -rnE "(select|option|p-dropdown)[^{]*\{[^}]*(#[0-9a-fA-F]{3,8}|white|black|color-scheme: *light *!important)" src/app --include=*.scss
# Menús o listas flotantes con fondo del campo (casi transparente de noche en 4 diseños)
grep -rn "position: absolute" -A8 src/app --include=*.scss | grep -n "background: var(--input-bg)"
```
Las listas flotantes hechas a mano (resultados de búsqueda que se despliegan) usan `--card-bg` o
`--menu-bg`, que son sólidos.

---

## 4. Pantallas con listas desplegables (para la ruta de medición)

`/clientes/agregar /clientes/mis-datos /tienda/venta-directa /tienda/carrito /tienda/buscar
/entregas-zona /personalizacion /pedidos/mis-pedidos /rifas/agregar /rifas/mes /rifas/boletos
/flores/catalogos /flores/ramos-admin /flores/configurar /lugares-entrega /ventas/venta`
(+ ventanas Swal con `select` en Mis pedidos y en el detalle del pedido).

Si se agrega un `select` o un `p-dropdown` en otra pantalla, se agrega aquí.

---

## 5. Resultado del 2026-10-13 (línea base)

| Diseño | Antes (peor contraste del menú, noche) | Después |
|---|---|---|
| Jade | 9.25 | 9.25 |
| Clásico | **1.21** (11 de 12 listas ilegibles) | 9.24 |
| Jade profundo elevado | **1.12** | 9.99 |
| Neutros cálidos | **1.20** | 9.33 |
| Teal transformador | **1.12** | 10.00 |
| Colores de fábrica | 9.25 | 9.25 |

De día todos pasaban y siguen pasando. También aguanta un diseño armado a mano con fondo oscuro de día y
campo casi transparente (11.97), y uno con letra blanca sobre campo blanco: el menú cae solo a letra
negra (17.74), aunque el campo cerrado no se lee, porque esos son los colores que se eligieron.

Revisión de texto (`--texto`, Jade, 13 pantallas): de noche 0 problemas. De día había 2: "Asistente
Virtual" en el encabezado del chat (1.12) y el "$" verde de Reportes (2.65). Los dos quedaron corregidos.

## 6. Checklist antes de decir "ya se ve bien"

- [ ] Medí con los 5 diseños + sin filas, de día y de noche (12 corridas), en las pantallas que se tocaron.
- [ ] Ningún menú ni panel por debajo de 4.5; ningún texto por debajo de 4.5 (o 3 si es número grande).
- [ ] Revisé en la captura lo marcado como "ver captura" (imagen o degradado detrás).
- [ ] `comentarios-sin-cerrar.py` sin hallazgos.
- [ ] Los greps de la sección 3 vacíos.
- [ ] Si cambió un token o una regla global: prueba de impacto de las pantallas que la usan (skill `pruebas-de-impacto`).
