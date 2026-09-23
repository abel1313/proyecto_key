# Dominio: articulo

El alta de artículos de un modelo: **qué cuenta como artículo** y **qué hereda del modelo**.

(`artículo` es el nombre al que migra lo que hoy se llama `variante`.)

---

## El modelo y el artículo — cómo se relacionan (confirmado 2026-09-22)

> *"El código de barras es del producto y cada artículo hereda ese código de barras. Cuando se
> genera un artículo hereda el producto y solo se agregan descripciones de ese artículo en
> específico."*

El **modelo** (`producto`) es lo que se compra y lo que identifica: nombre, precios, código de
barras, categoría. El **artículo** (`variantes`) es la pieza vendible y solo agrega **lo suyo**:
talla, color, marca, presentación, contenido neto, descripción y su stock.

**El código de barras compartido no es un defecto**, es el diseño: todas las tallas de un modelo
son el mismo producto. Lo que distingue a un artículo es lo que se le llena encima.

---

## El problema (reportado 2026-09-22)

> *"Si en tallas solo agrego 2, aparece que voy a guardar 3 aunque esté vacío lo que llené."*

La pantalla de alta tiene **dos partes independientes**:

1. el **formulario base** — los datos del artículo, para cuando se da de alta uno solo;
2. la **sección de varios** — talla por talla, cuando el modelo tiene más de uno.

Son independientes: si se llena solo el base, se crea un artículo; si se agregan tallas, se crean
ésas. Pero **si se llena el base y después se vacía, sigue contando**: el back recibe 3 y crea 3,
y el tercero nace sin talla, sin color y sin nada.

El back guardaba **todo lo que llegara**, sin preguntarse si describía algo.

---

## Reglas

### R1 — Un artículo vacío no es un artículo

Un artículo que llega **sin ningún dato propio, sin imágenes y sin stock** no se guarda. No
describe nada y no se puede vender: es el formulario base que se llenó y se vació.

**Qué NO se descarta**, para no perder nada real:

- un artículo **que ya existe** (trae `id`) — eso es una edición, aunque se le hayan borrado los
  campos;
- uno **con stock** aunque no tenga talla ni color — es el caso legítimo de *"solo quiero agregar
  1 artículo, lleno los datos que están"*: un modelo sin variantes reales;
- uno **con imágenes** aunque no tenga texto.

La regla es conservadora a propósito: descartar de más sería perder un alta que alguien hizo.

### R2 — La categoría se hereda del modelo

Si el artículo no trae categoría propia, toma la del modelo. *"Si está llena, que la tome para
todos los que agregue."*

No se copia al revés ni se pisa: un artículo **con** categoría propia conserva la suya. Heredar
solo lo que falta permite que la mayoría salgan con la del modelo y alguno se separe sin tener que
elegirla una por una.

**Lo mismo con los datos básicos (2026-09-23):** color, marca, descripción y contenido neto que el
artículo **nuevo** trae vacíos se toman del producto base (`ArticuloDeAlta.datoEfectivo`). Lo que
trae se respeta. Un artículo que ya existe no hereda: vaciarle un campo es una edición.
Reportado como *"creo el producto base con categoría y los artículos no se llenan con esa info"*.

### R3 — Lo que el artículo hereda no se le pregunta

Código de barras, nombre, precios y categoría **vienen del modelo**. La pantalla de alta no los
pide por artículo, y el back no los acepta por artículo: si los aceptara, dos artículos del mismo
modelo podrían terminar con precios distintos sin que nadie lo decidiera.

### R4 — Al vender: primero el producto, después el artículo (2026-09-22)

Toda venta (pedido, venta directa, transferencia de saldo de un apartado, agregar o cambiar un
artículo en un pedido) revisa, en este orden:

1. el **producto** está habilitado,
2. el **producto** tiene stock para la cantidad,
3. el **artículo** está habilitado,
4. el **artículo** tiene stock para la cantidad.

**El back no confía en el front.** El carrito se guarda en el navegador y puede traer un artículo
que se deshabilitó después de agregarlo; el buscador de venta directa del admin muestra todo,
incluido lo deshabilitado. Antes de este cambio ninguno de esos caminos revisaba `habilitado`.

**Deshabilitar el producto no toca sus artículos.** No se ponen en 0 ni se deshabilitan: con el
producto apagado ninguno se puede vender, y al volver a habilitarlo quedan como estaban. La regla
"artículo deshabilitado = stock 0" existe para liberar ese stock a sus hermanos; con todo el
producto apagado no hay a quién dárselo.

**Lo ya apartado no se toca.** Un apartado o pedido pendiente ya descontó su stock al crearse;
deshabilitar el producto no lo cancela. Si la pieza sigue en la tienda, se entrega normal.

Vive en `ArticuloAVender` para que los cuatro caminos digan lo mismo y en el mismo orden.

---

## Por qué vive aquí y no en `VarianteServiceImpl`

`guardarConImagenes` ya mezcla stock, imágenes, restock y caché. "Qué cuenta como artículo" es una
regla de negocio que hay que poder leer sola — y es la misma que va a seguir valiendo cuando
`variante` pase a llamarse `artículo`.

El servicio viejo sigue orquestando; solo le pregunta al dominio.
