# Pendientes de homologación — pedido del dueño del 2026-10-14

**Estado: solo anotado. No se ha empezado nada.** Antes de programar se acuerda el orden y las skills
(ver "Propuesta de orden" al final). Lo que diga el dueño se agrega aquí con fecha.

## ✅ Avance (se palomea lo hecho; "local" = hecho y compilado, sin subir ni probar por el dueño)

| | Qué | Estado |
|---|---|---|
| D | Sesión: ventana de gracia en back + candado entre pestañas en front | ✅ local · SQL corrido en QA (2026-10-14) |
| E6 | Ventana "Cambia tu contraseña" (ojito, alineada, tokens, una sola para login y verificar correo) | ✅ local |
| E5 | Usuarios → Actualizar: Volver fuera de la tarjeta y tarjeta de 820px | ✅ local (sin ver en pantalla) |
| — | Personalización: acordeón (al abrir una sección se cierran las demás) y lista completa de variables | ✅ local / lista completa ya en QA |
| A2 A3 A4 | Tienda → Buscar: talla, color y marca ahora son **buscadores** en MAYÚSCULAS; talla con orden (tallas conocidas, luego letras, luego números) | ✅ local |
| A3 | Color **sin números** al escribir (Agregar producto, Agregar artículo, Actualizar artículo, Carga de imágenes, Alta de artículos, detalle) | ✅ local · ⏳ falta revisar los colores con número que ya existen en QA |
| A6 A7 | Marca en MAYÚSCULAS al escribir (Productos → Agregar, Agregar artículo, Alta de artículos, detalle) | ✅ local |
| — | Guardar en MAYÚSCULAS desde el back: marca/color (producto y artículo), talla (artículo), palabras clave | ✅ local |
| A9 | Palabras clave: mayúsculas al escribir, al guardar y al mostrar | ✅ local |
| A10 | Lugares de entrega: estilo oración ("Colonia centro") al escribir y al guardar | ✅ local |
| — | Mostrar talla, color y marca con `uppercase` en todas las pantallas (28 archivos) | ✅ local |
| A1 A5 A7 A11 | Buscadores de Tienda, Productos, Venta y Clientes: ya tenían mayúsculas al escribir | ✅ ya estaba |
| A8 | Completar producto → carga de imágenes: nombre, marca, color, contenido y código ya van en mayúsculas | ✅ ya estaba |
| A12 | **Todos** los demás buscadores (revisar uno por uno) | ⏳ falta |
| B | Productos → Buscar: decidir qué botones son del modelo y cuáles del artículo | ⏳ falta |
| C | Compartir imagen: error 400 del thumbnail | ⏳ falta |
| E1 | Rifas: cards del mismo tamaño | ⏳ falta |
| E2 | Flores → Configurar: homologar | ⏳ falta |
| E3 E4 | Catálogo de menús y submenús / Gestión de roles: campos muy cortos y sobra espacio | ⏳ falta |
| — | Skills `cards`, `buscadores` (ampliar), `diseno-componentes` (ampliar) | ⏳ falta |
| — | Pantalla para generar códigos QR por tipo | ⏳ falta (definir tipos) |

## A. Texto en MAYÚSCULAS (buscadores y catálogos)

| # | Pantalla | Qué pidió |
|---|---|---|
| A1 | **Tienda → Buscar** (`/tienda/buscar`) | Lo que se escribe en el buscador sale en **MAYÚSCULAS** al buscar |
| A2 | Tienda → Buscar, filtro **Talla** | Cambiarlo a un **buscador** (hoy abre un select enorme). Opciones en MAYÚSCULAS, **primero las letras (CH, M, G, XL…) y después los números** |
| A3 | Tienda → Buscar, filtro **Color** | Buscador, todo en MAYÚSCULAS. **Un color no debe llevar números**: en QA hoy hay colores con números → buscar **dónde se da de alta el color** y no permitir números ahí |
| A4 | Tienda → Buscar, filtro **Marca** | Buscador, todo en MAYÚSCULAS. La marca **sí** puede llevar números |
| A5 | **Productos → Buscar** (`/productos/buscar`) | El buscador escribe en MAYÚSCULAS y busca |
| A6 | **Productos → Agregar** | La **marca** se escribe en MAYÚSCULAS y **se guarda** en MAYÚSCULAS |
| A7 | **Tienda → Venta** (`/tienda/venta`) | El buscador en MAYÚSCULAS busca. La **marca** de esa pantalla aparece en MAYÚSCULAS y se agrega al escribir |
| A8 | **Completar producto → carga-imágenes** | Va en MAYÚSCULAS |
| A9 | **🏷️ Catálogo de palabras clave** | Todo en MAYÚSCULAS **de una vez al guardar y al mostrar** |
| A10 | **Lugares de entrega → Nuevo lugar** | Primera letra de la **primera palabra** en mayúscula y las demás en minúscula (`Colonia centro` → `Colonia centro`; 2 palabras: solo la primera palabra con mayúscula) |
| A11 | **Clientes → Buscar** | Búsqueda en MAYÚSCULAS |
| A12 | **TODOS los buscadores** | Misma regla (se hace con la skill `buscadores`) |

Pregunta abierta: ¿en la base se guarda ya en mayúsculas (los datos viejos también) o solo se muestra?
Ver "Preguntas" abajo.

## B. Productos → Buscar: qué hace cada botón de la card

El dueño pregunta si agregar/quitar del carrito, etc. pertenecen a **artículo** y no a modelo.
La card de `productos/buscar` (componente `productos/producto/all`) hoy tiene: **agregar / quitar
del carrito**, ver carrito, ver detalle, editar, **🧩 Productos** (crear artículos), compartir
imagen, dar de baja, habilitar/deshabilitar. Hay que decidir cuáles son del **modelo** y cuáles del
**artículo** (el carrito y el stock son del artículo). Pendiente de revisar y proponer.

## C. Compartir imagen falla

Al dar clic en **Compartir imagen** sale *"No se pudo cargar la imagen"*:
`GET https://qa.backend-imagenes.novedades-jade.com.mx/mis-productos/v1/imagenes/thumbnail/3023022357955036909` → **400 Bad Request**.
El id (19 dígitos) parece no ser un id de imagen válido (¿es otro id, el de la variante o el del
producto?). Pendiente de investigar qué manda el front y qué espera `micro_imagenes`.

## D. Sesión: me saca aunque haya refresh token

**Causa encontrada (2026-10-14):** el back rota el refresh token en cada renovación y, si llega uno
ya rotado, cierra la sesión completa (se toma por robo). Dos renovaciones legítimas casi a la vez
(dos pestañas, o el arranque de la página junto con una petición cuyo access token venció) mandan
la misma cookie, y la segunda sacaba al usuario. Para confirmarlo en QA: el log del back dice
`Reuso de refresh token detectado; se cierra la sesion completa`.

**Arreglo (hecho en local, sin subir):** back, ventana de gracia de 20 s (`jti_anterior` y
`rotado_en` en `sesion_refresh`, script `migration_sesion_refresh_gracia.sql`, **correrlo en QA
antes de desplegar**); front, candado del navegador para que las renovaciones de todas las pestañas
hagan fila (`token/refresh-token.ts`, lo usan el interceptor y el arranque de la app).

Usando la aplicación, al cambiar de pantalla la sesión se cierra. El refresh token debería evitarlo.
Pendiente de **revisar a detalle** (interceptor del front, expiración de 15 min del access token y
7 días del refresh, cookie, el refresh que invalida al cambiar contraseña, varias peticiones
simultáneas que refrescan a la vez, etc.) para solucionarlo de raíz.

## E. Diseño / homologación de pantallas

| # | Pantalla | Problema |
|---|---|---|
| E1 | **Rifas** (todas) | Las cards no se ven iguales: unas más chicas y otras más grandes |
| E2 | **Flores → Configurar** (`flores/configurar`) | No está homologada |
| E3 | **🗂️ Catálogo de menús y submenús** | Se cortan los nombres y sobra espacio; los inputs quedan muy cortos |
| E4 | **🛡️ Gestión de roles** | Igual: muy corto y sobra espacio |
| E5 | **Usuarios → Actualizar** | ✅ Ya hecho en local el 2026-10-14 (Volver fuera de la tarjeta, tarjeta de 820px), sin subir |
| E6 | Ventana **Cambia tu contraseña** | ✅ Ya hecho en local el 2026-10-14 (ojito, cajas alineadas, tokens), sin subir |

## F. Reglas nuevas acordadas el 2026-10-14

- **Mayúsculas: de aquí en adelante se GUARDAN en mayúsculas** (lo de la sección A) **y además se
  muestran con un pipe que las pone en mayúsculas siempre**, así los datos viejos también se ven en
  mayúsculas sin migrar la base. (Sin script de migración.)
- **Código QR:** hoy `/qr` solo genera un QR fijo. Se acordó antes una **pantalla para generar QR
  según lo que se necesite**: se elige el **tipo** (ej. redes sociales, tienda, producto, pedido…),
  la pantalla **pide los datos que ese tipo necesita** (ej. cuál red y su enlace), genera el QR y
  deja **copiarlo o reenviarlo**. Pendiente de definir qué tipos
  de QR (enlace a la tienda, a un producto, a un pedido, a la red social…) y diseñarla.
- **Orden acordado:** primero la sesión (D), luego las skills (cards, buscadores, diseño), luego las
  pantallas, y al final compartir imagen (C).

## Propuesta de orden (a acordar con el dueño)

Ya existen las skills `buscadores` y `diseno-componentes`; en vez de crear tres de cero, se
**amplían** y se agrega una:

1. **Skill `cards`** (nueva): una sola card para todas las pantallas (tamaño, encabezado, botones,
   día/noche). Se aplica a Rifas, Flores, etc.
2. **Skill `buscadores`** (ya existe): se le agrega la regla de MAYÚSCULAS, el buscador con
   opciones (talla/color/marca) en lugar del select grande, y el orden letras → números.
3. **Skill `diseno-componentes`** (ya existe): se le agrega el ancho único de pantallas de
   administración (820px), el Volver fuera de la tarjeta y los campos que usan todo el ancho.
4. Con las skills acordadas, se aplican a las pantallas de las secciones A y E, de una en una,
   con su prueba.
5. C (compartir imagen) y D (sesión) son errores, no diseño: se investigan aparte.
