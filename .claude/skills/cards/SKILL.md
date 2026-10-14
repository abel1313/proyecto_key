---
name: cards
description: Cómo se arma toda card y todo ancho de pantalla de Novedades Jade (tarjeta de lista, tarjeta de formulario, botón Volver, ancho único de 820px, pantallas de tablas más anchas). Usar SIEMPRE antes de crear o tocar una pantalla con tarjetas, y cuando el dueño diga "las cards no se ven iguales", "unas más chicas y otras más grandes", "está muy corto y sobra espacio" o "el Volver está dentro de la card".
---

# Cards y anchos — una sola forma en toda la app

Pedido del dueño (2026-10-14): las cards de Rifas, Flores, Catálogo de menús y Gestión de roles no se
veían iguales (unas más chicas, otras más grandes, campos muy cortos con mucho espacio a los lados).
Esta skill complementa a `diseno-componentes` (colores y tokens): aquí va **tamaño y estructura**.

## 1. Los dos tipos de pantalla

| Tipo | Ejemplos | Ancho máximo | Cómo |
|---|---|---|---|
| **Formulario / detalle** (una tarjeta central) | Agregar modelo, Actualizar usuario, Rifa, Flores → Configurar | **820px**, centrada | `max-width: 820px; margin: 0 auto; padding: 24px 16px` |
| **Administración con tablas o dos columnas** | Catálogo de menús y submenús, Gestión de roles | **1400px** | Columnas con `flex-wrap`; el campo de texto ocupa `100%` de su columna (`min-width` chico, nunca un ancho fijo) |

Si una pantalla nueva no sabe en cuál cae: si tiene un formulario de una columna → 820px; si tiene una
tabla o dos paneles lado a lado → 1400px.

## 2. Botón Volver
Siempre **fuera y arriba** de la tarjeta, alineado con su borde izquierdo:
`<app-boton-volver anchoContenido="820px" margen="16px" fallback="/ruta">` (el mismo `anchoContenido`
que el `max-width` de la tarjeta). Nunca dentro de la tarjeta.

## 3. La card
- Fondo, borde, radio y sombra **solo con tokens** (`--card-body-bg`, `--card-border`, `--card-radius`,
  `--card-shadow`). Nada de colores a mano.
- Lista de cards del mismo tipo (rifas, ramos…): **todas del mismo tamaño** — mismo `grid-template-columns`
  (`repeat(auto-fill, minmax(280px, 1fr))`), misma altura de imagen, mismo padding. El contenido
  que cambie de longitud se recorta con `text-overflow: ellipsis` o se envuelve; la card no crece sola.
- Texto largo que se corta: darle `min-width: 0` al hijo flex y envolverlo; no fijar un ancho en `px`
  al campo.

## 4. Revisión (antes de dar por hecho)
- Medir con captura a **1360, 900 y 400px** de ancho: el borde izquierdo del Volver y el de la tarjeta
  coinciden; ningún campo queda a menos de la mitad de su columna.
- De día y de noche.

## 5. Estado de aplicación (2026-10-14)
- 820px: Rifas (agregar, boletos, buscar, rifa del mes), Flores → Configurar, Usuarios → Actualizar.
- 1400px: Catálogo de menús y submenús, Gestión de roles.
- Pendiente: igualar el tamaño de las cards dentro de las listas de Rifas y Flores (ver
  `PENDIENTES_HOMOLOGACION_2026-10-14.md`).
