---
name: dar-el-caso
description: Cómo darle al dueño de Novedades Jade "el caso" (la prueba de QA que le toca) en el chat, completo y paso a paso, con el menú exacto, el nombre exacto de cada botón, lo que debe escribir y el texto exacto que debe ver, marcando con ✅ lo que ya pasó y diciendo lo que falta. Usar SIEMPRE que el dueño diga "dame el caso", "pásame el caso", "dame/pásame la prueba", "¿qué prueba sigue?", "¿cuál es la prueba que tengo que hacer?", "lo que falta y lo que llevo", "no entiendo el paso N", "explícalo mejor / descríbelo más", "no me explicas cómo hacerlo" o "¿en qué menú?"; y siempre que se vaya a mandar por chat cualquier paso de una prueba de QA.
---

# Dar el caso — la prueba paso a paso, con menú y nombres exactos

**Pedido del dueño (2026-10-14):** *"pero no me explicas cómo hacerlo, es decir en el menú a dónde te lleva o
cómo hacerle con los nombres exactos, y hay que hacer una skill para eso, que cuando diga «dame el caso» lo
hagas como ya te lo expliqué, porque a veces no lo escribes y a veces sí."*

Antes de esta skill, el 2026-10-14 se le mandó **una lista resumida** ("7 | Guardas el artículo con stock 1")
cuando lo que necesitaba era *dónde estás, qué botón tocas y qué debe salir*. Se perdió tiempo y hubo que
reescribir del paso 4 al 20. Esta skill existe para que **siempre** salga completo, a la primera.

---

## 1. La regla

**El caso se da completo en el chat, cada vez.** No se manda "mira el documento", ni una tabla de títulos, ni
"sigue en el paso 4". El dueño hace las pruebas desde el celular o la computadora leyendo el chat: lo que no
está escrito ahí, para él no existe.

Cada paso dice **tres cosas**, siempre las tres:

| Parte | Qué lleva | Ejemplo bueno |
|---|---|---|
| **Dónde** | El menú de la barra lateral → el submenú (con su emoji) → la pantalla en la que estás, o **cómo llegar desde donde ya estás** | *"Menú **Catálogo → 🔍 Modelos**"* · *"Sigues en la pantalla de editar el artículo"* |
| **Haz** | La acción con el mouse o el dedo: **qué tocar** (nombre exacto del botón, con su emoji) y **qué escribir** (el valor exacto, en negritas) | *"Escribe **QA Prueba 20 Agotado** en el buscador y toca **✏️ Actualizar** en su tarjeta"* |
| **Debes ver** | Lo que debe pasar: el texto **exacto** del aviso entre comillas, números, qué se pone gris o rojo, a dónde te regresa | *"Un aviso: «¡Artículo actualizado!» y regresa a Tienda → Buscar"* |

**Mal** (así se hizo el 2026-10-14 y no sirve): *"7 | Guardas el artículo con stock 1"*.
**Bien:** *"7. Baja al final de la pantalla y toca el botón grande **💾 Actualizar artículo**. **Debes ver:**
un aviso corto «¡Artículo actualizado!» y regresa a **Tienda → Buscar**; la tarjeta de la talla **M** dice
stock **1**."*

## 2. Cómo se arma la respuesta (en este orden)

1. **Dónde vas** — una línea: *"Vas en la **20.3**, paso **4**."*
2. **✅ Lo que llevas** — lo que ya pasó, con fecha: la tabla "📍 Llevamos" del documento y los pasos ✅ de la
   prueba actual. Las palomitas son **solo lo que el dueño dijo que pasó**, nunca lo que se supone.
3. **Antes de empezar** — siempre completo:
   - **Ctrl + Shift + R** (si se subió algo nuevo).
   - El botón de datos con su **ruta exacta**: **Sistema → Limpiar caché → 🧪 Datos de prueba → Preparar una
     prueba → 🧪 Preparar prueba N** (o **🧪 Preparar todas las pruebas**). El dueño **no** arma datos (skill
     `preparar-prueba`). Decir el **nombre exacto** que deja (*"QA Prueba 20 Agotado"*, *"Prueba 24 K4"*).
   - Lo único que escribe a mano, con sus valores exactos.
4. **Los pasos**, numerados, con *Dónde / Haz / Debes ver* (sección 1). Reglas de redacción en la sección 3.
5. **❌ Está mal si…** — lo que sería un error, en una lista corta.
6. **Lo que falta después** — las demás pruebas pendientes, **en orden**, y de cada una: *qué revisa* + *en qué
   menú empieza* + *qué botón 🧪 se toca*. Solo el título **no** basta. Aclarar que cuando llegue a esa se le
   da **completa** (una prueba a la vez).
7. **Cierre** — si hay algo sin subir, decirlo; si hay comentarios 💬 del dueño, copiarlos tal cual con su
   respuesta ↳ (skill `reglas-pedidos`, sección 1).

Si el dueño pide **solo "lo que falta y lo que llevo"**: se dan los puntos 1, 2 y 6, y se **ofrece el caso
completo** del que sigue. Pero ojo: el punto 6 también lleva menú y botón 🧪, no solo nombres.

## 3. Reglas de redacción de cada paso

- **Nombres reales, nunca de memoria** (skill `nombres-reales`): el menú sale de la base (`menu` / `submenu`),
  los botones del HTML del front, los avisos del `.ts` o del Java. Mismas mayúsculas, acentos y emoji.
  *Ejemplo real:* es **Sistema → Limpiar caché**, no "Caché"; es **Ventas → Venta directa**, no
  "Tienda → Venta directa". Si no se encuentra, se dice; no se pone uno parecido.
- **Un paso = una cosa.** Si el paso abre otra ventana, el paso siguiente empieza diciendo *"se cerró esa y se
  abrió otra"* o *"estás otra vez en la pantalla de…"*. El dueño se pierde cuando no sabe **en qué ventana está**.
- **"Ventana" = el cuadro que se abre encima de la pantalla; "pantalla" = la página completa.** Se usan así.
- **Cuando el paso es solo mirar, se dice**: *"No toques nada, solo mira."*
- **Cuando el paso es opcional o una prueba de seguridad** (escribir 0 o 20 a propósito para ver el aviso
  rojo), se dice **para qué es** y que se puede saltar: *"Solo es una prueba de seguridad; si no la quieres
  hacer, sáltala al 5."*
- **Los valores a escribir van en negritas y exactos** (*"Talla **S**, Stock **1**"*), no "una talla".
- **Lo que debe ver va con el texto exacto entre comillas** (el aviso, el título de la ventana, el número).
  Con dinero, **con números** (*"Total $450, Abonado $200, Resta $250"*), nunca "el total correcto".
- **Nombres que se buscan en pantalla** van tal cual los dejó 🧪 (R20): *"QA Prueba 20 Agotado"*, *"Prueba 24 K4"*.
- **Qué cambió respecto a antes**, cuando importa: *"(Antes solo salía «¡Producto actualizado!»)"*. Sirve
  para que sepa que lo nuevo es justo eso.
- Sin siglas ni palabras del código (no "variante", "endpoint", "payload"): **artículo**, **modelo**,
  **ventana**, **botón**. Formas de cobro con sus palabras: **Apartado**, **Ir pagando**.
- **Se marca ✅ el paso que el dueño ya pasó** (*"3 ✅"*), y ese ✅ también va en el documento.

## 4. Si dice "no entiendo el paso N"

No se reexplica solo ese paso: **se reescribe el N y todos los siguientes** con el nivel de detalle de la
sección 1 (fue lo que pidió el 2026-10-14: *"la 4 no la entiendo… de la 4 hacia adelante no la entiendo,
descríbela más"*). Se dice en una línea **para qué sirve** el paso que no entendió y si se puede saltar.
Después, **el documento se corrige igual** (sección 5).

## 5. El documento y el chat dicen lo mismo

La fuente de verdad es `PRUEBAS_PENDIENTES_<fecha>.md` del repo del back. Lo que se escribe en el chat **ya
existe** en el documento con el mismo nivel de detalle; si en el chat se explica mejor, **se corrige el
documento en el mismo momento** y se avisa si quedó sin subir (*"solo en local, lo subo cuando digas «sube»"*).
Los comentarios 💬 del dueño se copian tal cual, y la respuesta va debajo con ↳.

Al escribir una prueba nueva o cambiar una, el documento ya sale con *Dónde / Haz / Debes ver* en cada paso;
no se deja "después lo detallo". Mapa de impacto y ⛔ salen de la skill `pruebas-de-impacto`.

## 6. Checklist antes de mandar el caso

- [ ] Cada paso tiene **Dónde, Haz y Debes ver** (las tres).
- [ ] Todo menú, botón y aviso está copiado de su fuente (skill `nombres-reales`), con su emoji.
- [ ] Dice **en qué ventana o pantalla** está el dueño al empezar cada paso que cambia de una a otra.
- [ ] Los pasos opcionales o "solo mirar" lo dicen, y para qué sirven.
- [ ] "Antes de empezar" lleva la ruta del botón 🧪 y los nombres exactos que deja.
- [ ] Los ✅ son solo lo que el dueño dijo que pasó, con fecha.
- [ ] "Lo que falta" trae, de cada prueba, qué revisa, el menú donde empieza y el botón 🧪.
- [ ] Hay **❌ Está mal si…**.
- [ ] El documento quedó igual que el chat; si hay algo sin subir, se dijo.
- [ ] Si se dice que algo "ya está arriba": se verificó el deploy de QA con ese commit (skill `reglas-pedidos`).

## 7. Lo que nunca se hace

- Mandar una **tabla de títulos** o un resumen como si fuera el caso.
- Decir *"sigue el documento"* en vez de escribir los pasos.
- Escribir un menú, botón o aviso **de memoria**.
- Dar varias pruebas completas a la vez: **una prueba a la vez**; de las demás, solo menú y botón 🧪.
- Poner ✅ en algo que el dueño no confirmó.
- Pedirle que **busque, anote o arme datos**: eso lo hace 🧪 Preparar prueba N (skill `preparar-prueba`).
