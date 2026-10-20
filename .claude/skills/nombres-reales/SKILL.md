---
name: nombres-reales
description: Cómo encontrar el nombre REAL de un menú, submenú, pantalla, botón, mensaje, componente Angular, clase Java, endpoint o tabla de Novedades Jade, sin adivinar. Usar SIEMPRE antes de escribirle al dueño un nombre de algo de la app (respuestas, planes, pruebas de QA, CAMBIOS_FRONT.md) y cuando pregunte "¿cómo se llama…?", "¿dónde está…?" o "¿en qué menú…?".
---

# Nombres reales — nunca adivinar

**Regla:** ningún nombre de la app se escribe de memoria. Antes de poner un menú, botón, título,
mensaje o componente en una respuesta, plan o prueba, se busca en su fuente (tabla de abajo) y se
copia **tal cual**: mismas mayúsculas, acentos y emoji.

**Por qué existe:** el 2026-10-01 se escribió en una prueba de QA "Tienda → Venta directa" y en el
menú real es **Ventas → Venta directa**; antes, un plan de pruebas traía botones inventados
("Agregar abono", "Registrar pago") que no existen. El dueño perdió tiempo buscándolos.

Si no se encuentra: decirlo (*"no lo encontré en el código; la ruta es `…`"*). **Nunca** poner uno
parecido "porque suena bien".

---

## 1. Dónde vive cada nombre

| Qué | Dónde está el nombre de verdad | Cómo buscarlo |
|---|---|---|
| **Menú y submenú** (lo que sale en la barra lateral) | **Base de datos**: tablas `menu` (grupo) y `submenu` (opción, con su `ruta`). El código del front **no** los tiene | Ver sección 2. Base inicial en `migration_menu_submenu.sql` + migraciones que agregan opciones |
| **Título de una pantalla** | HTML del componente del front (`<h3>`, `<h4>`, `__title`) | `grep -n "<h[1-4]" src/app/<carpeta>/<componente>.component.html` |
| **Botón, etiqueta, placeholder, texto en pantalla** | HTML del componente del front | `grep -rn "texto parecido" src/app --include=*.html` |
| **Aviso / modal (Swal)** | `.ts` del componente: `Swal.fire({ title, text, html, confirmButtonText })` | `grep -rn "Swal.fire" -A6 src/app/<carpeta>/*.ts` |
| **Mensaje de error que manda el back** | Java: `throw new RuntimeException("…")` / excepciones de dominio | `grep -rn "texto" src/main/java` en `proyecto_key` |
| **Componente Angular** | `selector:` y nombre de la clase en el `.ts` | `grep -rn "selector:" src/app/<carpeta>` |
| **Ruta (URL) de una pantalla** | `*-routing.module.ts` del front | `grep -rn "path:" src/app --include=*routing*.ts` |
| **Clase Java / servicio** | `src/main/java/...` | `grep -rln "class Nombre"` |
| **Endpoint** | `@RequestMapping` de la clase + `@GetMapping`/`@PostMapping`… del método | `grep -n "Mapping" <Controller>.java` |
| **Tabla de la base** | `@Table(name=…)` de la entidad + inventario de `CLAUDE.md` (ojo con `Imagen` → `imagenes_copy`) | `grep -rn "@Table" src/main/java` |

Rutas de los repos: back `/home/user/proyecto_key` (o `proyecto_key_new`), front
`/home/user/producto_venta_online`.

---

## 2. Menú y submenú: la base manda

1. El nombre que ve el dueño sale de `menu.nombre` y `submenu.nombre`. Se pueden **renombrar** desde
   **Sistema → Menús y submenús** (pantalla `gestion-menu`), así que las migraciones son solo el
   nombre inicial.
2. Para saber el nombre actual en un ambiente, la consulta (pedírsela al dueño si no hay acceso):

   ```sql
   SELECT m.nombre AS menu, s.nombre AS opcion, s.ruta
   FROM submenu s LEFT JOIN menu m ON m.id = s.menu_id
   ORDER BY m.orden, s.orden;
   ```
3. Si el dueño dice que en su pantalla se llama distinto, **gana su pantalla**: corregir la tabla de
   abajo en el mismo cambio.
4. Al escribir una ruta de menú, formato **Grupo → Opción** (ej. **Ventas → Venta directa**). Si una
   opción no tiene grupo, solo su nombre.

### Mapa inicial (sale de las migraciones; confirmar con la consulta si hay duda)

| Grupo | Opción | Ruta |
|---|---|---|
| Tienda | Tienda | `tienda/buscar` |
| Catálogo | Modelos | `productos/buscar` |
| Catálogo | Agregar modelo | `productos/agregar` |
| Catálogo | Agregar artículo (antes "Agregar producto"; cambia al correr `migration_renombre_articulo_etiquetas.sql`) | `tienda/venta` |
| Catálogo | Carga rápida de imágenes | `carga-imagenes` |
| Catálogo | Cargar Excel | `tienda/cargar-excel` |
| Catálogo | Categorías | `palabras-clave` |
| Envíos | Zonas de entrega | `lugares-entrega` |
| Envíos | Entregas por zona | `entregas-zona` |
| Pedidos | Mis pedidos | `pedidos/mis-pedidos` |
| Pedidos | Historial de pagos (MP) | `pedidos/historial-mp` |
| Ventas | Venta directa | `tienda/venta-directa` |
| Ventas | Créditos / Abonos | `abonos` |
| Ventas | Gastos | `gastos/buscar` |
| Reportes | Dashboard | `dashboard` |
| Reportes | Reportes de ventas | `reportes` |
| Rifas | Rifa de artículos (antes "Rifa de productos"; cambia al correr `migration_renombre_articulo_etiquetas.sql`) | `rifas/agregar` |
| Rifas | Rifa mensual | `rifas/mes` |
| Rifas | Ver rifas activas | `rifas/buscar` |
| Rifas | Boletos de rifa | `rifas/boletos` |
| Flores eternas | Ramos de flores · Arma tu ramo · Catálogos · Entregas · Frases por aprobar · Administrar ramos armados | `flores/ramos` · `flores/configurar` · `flores/catalogos` · `flores/entregas` · `flores/frases` · `flores/ramos-admin` |
| Marketing | Promociones activas · Gestionar promociones · Cinta de anuncios · Publicar en redes · Hashtags de redes | `promociones` · `admin/promociones` · `admin/cinta` · `admin/facebook` · `admin/hashtags` |
| Sistema | Usuarios · Negocio & Contactos · Chat en vivo · Imágenes de presentación · Diagnóstico de imágenes · Reconciliación de imágenes · Limpiar caché · Menús y submenús · Gestión de roles · Personalización · Ayuda contextual (solo permiso, no es pantalla) | `usuarios/buscar` · `admin/negocio` · `admin/chat` · `admin/presentacion` · `admin/diagnostico-imagenes` · `admin/reconciliacion-imagenes` · `admin/cache` · `gestion-menu` · `gestion-menu/roles` · `personalizacion` · `ayuda-contextual` |
| (sin grupo) | Home · Clientes · Favoritos · Chat · Código QR de la tienda · Login | `home` · `clientes/buscar` · `favoritos` · `chat` · `qr` · `login` |

Ojo: **Venta directa** (`tienda/venta-directa`) y **Créditos / Abonos** están en **Ventas**, no en
Tienda, aunque la ruta empiece con `tienda/`.

---

## 3. Cómo escribir el nombre

- **Botones y textos:** en negritas y con su emoji, tal cual: **💳 Registrar abono**, **🔁 Cambiar
  forma de cobro**, **💰 Cobrar**.
- **Textos que cambian** (`{{ … }}` en el HTML o ternarios): decir cuándo sale cada uno. Ej. el botón
  del grupo dice **💵 Pagar el grupo completo** en Apartados y **💵 Abonar al grupo** en Ir pagando.
- **Botones que dependen de un permiso** (`*ngIf="…tieneAccion(...)"`): avisar que si el rol no tiene
  la acción, el botón no sale.
- **Mensajes:** entre comillas y en cursiva: *"Un Apartado se paga completo"*.
- **Pantalla vs. menú:** la opción del menú y el título de la pantalla pueden ser distintos
  (menú **Créditos / Abonos**, título **💳 Créditos y Abonos**). Dar los dos cuando ayude a ubicarse.
- Si el nombre se tomó de las migraciones y no de la base viva, decirlo cuando importe.

## 4. Antes de entregar

1. Releer lo que se va a mandar y marcar cada nombre de la app.
2. Cada uno tiene que haberse buscado en esta sesión (o estar en la tabla de la sección 2).
3. Si se cambió un texto en el código, actualizar los documentos que lo citan (`PRUEBAS_QA_*.md`,
   `CAMBIOS_FRONT.md`, skills) en el mismo cambio.
