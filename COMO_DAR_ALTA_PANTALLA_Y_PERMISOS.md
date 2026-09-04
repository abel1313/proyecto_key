# Cómo dar de alta una pantalla nueva en el sistema de permisos

Guía de referencia — qué es cada pieza, dónde se da de alta, y qué se conecta solo vs. qué hay
que tocar a mano. Complementa `PLAN_PERMISOS_PANTALLAS.md` (repo del front, diseño original) con
el estado real del código a 2026-09-04.

---

## Hay 3 piezas separadas — dar de alta una NO genera las otras 2

| Pieza | Dónde se da de alta | Qué controla |
|---|---|---|
| **Submenu** (catálogo de pantallas) | Pantalla "Menús y submenús" (`/gestion-menu`) | Que la pantalla exista para poder marcarla en Gestión de roles, y que `tienePantalla(ruta)` devuelva algo |
| **Guard + link visible** | Código — `app-routing.module.ts` + `navbar.component.html` | Que la pantalla esté REALMENTE protegida y que aparezca un link para entrar |
| **Gate del back** | Código — `SecurityConfig.java` | Que los endpoints de esa pantalla realmente rechacen a quien no tiene el permiso |

Las 3 usan la misma `ruta` como clave (ej. `productos/buscar`) pero son independientes entre sí —
te podés olvidar cualquiera de las 3 y las otras 2 seguirían "funcionando" a medias, de forma
confusa (pantalla visible pero sin protección real, o protegida pero invisible, etc.).

---

## 1. El Submenu — pantalla "Menús y submenús" (`/gestion-menu`)

Dos formularios, uno al lado del otro:

### Menu (el grupo del acordeón, ej. "Catálogo", "Envíos")
| Campo | Obligatorio | Detalle |
|---|---|---|
| `nombre` | Sí | Único, máx. 60 caracteres. Texto del grupo. |
| `icono` | No | Un emoji, máx. 10 caracteres. |
| `orden` | No | Posición del grupo en el sidebar. Vacío = al final. |

### Submenu (el item real que navega a una pantalla, ej. "Modelos")
| Campo | Obligatorio | Detalle |
|---|---|---|
| `menu` | No | A qué grupo pertenece — se elige el grupo (o "Sin grupo") antes de cargar el formulario. "Sin grupo" = item suelto fuera de cualquier acordeón (Home, Tienda, Favoritos, Chat, QR, Login). |
| `nombre` | Sí | Máx. 80. Texto del link. |
| `ruta` | Sí | Máx. 150. **La clave de todo el sistema** — tiene que ser EXACTAMENTE el `routerLink` de Angular tal cual está en el código, sin `/` inicial (ej. `productos/buscar`). Arma la authority del JWT (`PANTALLA_<ruta>`), lo que evalúa `tienePantalla()` en el front, y lo que usa `SecurityConfig` en el back. |
| `icono` | No | Emoji. |
| `descripcion` | No | Máx. 255. Texto del popup ℹ️ en Gestión de roles ("¿qué es esta pantalla? ¿dónde vive?"). Si se deja vacío, el popup dice "Todavía no tiene descripción cargada". |
| `orden` | No | Posición dentro de su grupo. |

Dar de alta un Submenu **solo** hace que la pantalla exista en el catálogo — no protege nada por
sí solo, no la hace aparecer en el sidebar.

---

## 2. Lo que NO se genera solo — hay que tocarlo en código

### 2.a — Ruta de Angular con `PantallaGuard`
En `app-routing.module.ts` (o el routing del módulo correspondiente):
```ts
{
  path: 'mi-pantalla-nueva',
  loadChildren: () => import('./mi-modulo/mi-modulo.module').then(m => m.MiModulo),
  canActivate: [AuthGuard, PantallaGuard, CarritoGuard]
}
```
Sin `PantallaGuard`, aunque el Submenu exista, cualquiera puede entrar escribiendo la URL a mano.
Ejemplo real de lo que pasa si se omite a propósito: la ruta `entregas-zona` no lo tiene todavía
(ver el comentario en el propio `app-routing.module.ts`) — queda solo con `AuthGuard`, protegida
de verdad únicamente porque el BACK exige `ROLE_ADMIN` en `/v1/entregas-zona/**`.

### 2.b — Link visible en el sidebar
En `navbar.component.html` — **hoy es una lista escrita a mano**, no se genera desde el catálogo
Menu/Submenu (pese a que el comentario original del código decía que reemplazaría el array fijo
`GROUP_ROUTES` — eso quedó a mitad de camino: solo el catálogo de PERMISOS se volvió dinámico, el
sidebar sigue siendo HTML estático):
```html
<a class="sb-subitem" *ngIf="tienePantalla('mi-pantalla-nueva')"
   routerLinkActive="sb-subitem--active" routerLink="mi-pantalla-nueva" (click)="closeMobile()">
  🆕 Mi pantalla nueva
</a>
```
Si das de alta el Submenu y no tocás el navbar, la pantalla queda protegida pero invisible —
nadie ve el link para entrar (hay que conocer la URL de memoria).

### 2.c — Gate del back
En `SecurityConfig.java`, agregar algo como:
```java
.requestMatchers(HttpMethod.GET, "/v1/mi-endpoint/**").hasAnyAuthority(pantalla("mi-pantalla-nueva"))
.requestMatchers("/v1/mi-endpoint/**").hasAnyAuthority(pantallaEscribir("mi-pantalla-nueva"))
```
El Submenu no protege NINGÚN endpoint por sí solo — es puro catálogo de front. La seguridad real
de la API siempre pasa por acá.

---

## 3. Un nivel más fino — acciones puntuales (los checkboxes tipo "habilitar", "eliminar", los filtros)

Esto es Fase 3 del sistema (piloto en Modelos, extendido a Tienda) y es **aparte** del Submenu:
un botón/checkbox concreto DENTRO de una pantalla que ya tiene su Submenu (ej. "eliminar" o
"escanear-codigo" dentro de Modelos).

**No tiene pantalla de administración todavía** — se da de alta solo por script SQL
(`INSERT INTO accion_submenu (submenu_id, clave, etiqueta, descripcion, orden) ...`), como los
que se escribieron para Modelos y Tienda esta sesión (`migration_accion_submenu.sql`,
`migration_filtros_granulares.sql`, `migration_accion_tienda_habilitar_compartir.sql`,
`migration_accion_modelos_etiquetas_y_escaner.sql`). `accion_submenu` se relaciona por
`submenu_id`, así que la pantalla (paso 1) tiene que existir primero.

En el back, cada acción puntual se protege por separado con el helper `accion(ruta, clave)` de
`SecurityConfig.java`:
```java
.requestMatchers(HttpMethod.DELETE, "/v1/mi-endpoint/{id}")
    .hasAnyAuthority(accion("mi-pantalla-nueva", "eliminar"))
```

En el front, un getter que usa `tieneAccion(ruta, clave)` (NO tiene bypass automático para
ROLE_ADMIN, a diferencia de `tienePantalla()` — el admin recibe la acción porque la migración
se la asigna explícitamente, igual que a cualquier otro rol):
```ts
get puedeEliminar(): boolean {
  return this.authService.tieneAccion('mi-pantalla-nueva', 'eliminar');
}
```
y en el template, `*ngIf="puedeEliminar"` en el botón correspondiente.

---

## Resumen — receta completa para una pantalla nueva

```
1. Crear el componente/ruta en Angular
2. Dar de alta el Submenu en "Menús y submenús" (nombre, ruta, icono, descripción, orden)
3. Agregar PantallaGuard a la ruta en el routing module
4. Agregar el <a routerLink> en navbar.component.html, gateado con tienePantalla()
5. Agregar el gate en SecurityConfig.java (back) -- pantalla() / pantallaEscribir()
6. (Opcional) si esa pantalla necesita permisos más finos que Ver/Editar completo,
   escribir una migración SQL que siembre filas en accion_submenu + su gate accion()
   en SecurityConfig + los getters/checks en el componente del front
```

Los pasos 2 y 6 tienen proceso ya armado (la pantalla "Menús y submenús", y el patrón de
migraciones SQL que venimos repitiendo). Los pasos 1, 3, 4 y 5 son código que se toca a mano
cada vez — no hay forma de darlos de alta solo desde un admin todavía.

---

## Guardado en Gestión de roles (2026-09-04)

Desde esta fecha, marcar/desmarcar Ver, Editar o una acción puntual de una pantalla en Gestión
de roles **ya NO manda la petición al toque**. Cada pantalla acumula sus cambios en memoria y
tiene su propio botón "💾 Actualizar" (+ "✕ Descartar" para tirar lo pendiente sin guardar).
Cambiar de rol seleccionado descarta solo los cambios sin guardar del rol anterior.
