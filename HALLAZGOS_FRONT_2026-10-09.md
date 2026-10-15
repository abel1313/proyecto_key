# Hallazgos del frontend — 2026-10-09

Notas de comportamiento observado y esperado. Sin cambios de código por ahora.

## Alta de producto base — `productos/agregar`

### La categoría no se limpia al guardar

**Observado:** después de guardar un producto base, la categoría seleccionada sigue en el formulario.

**Esperado:** al guardar correctamente el producto, limpiar la categoría del formulario.

### Ofrecer continuar con los artículos del producto base

**Observado:** al guardar el producto base no aparece una modal (ni otra opción visible) para continuar
con el alta o generación de los artículos asociados.

**Esperado:** después de guardar el producto base, ofrecer una acción para dar de alta o generar sus
artículos.

## Alta de artículos o variantes — stock del producto base

**Observado:** al querer crear variantes, puede no haber suficiente stock disponible en el producto
base para la cantidad que se quiere dar de alta.

**Esperado:** desde ese flujo, permitir agregar el stock faltante al producto base y luego continuar
con el alta de las variantes.

## Alta de artículo desde un producto existente — `tienda/venta`

### La categoría del producto base no aparece en el artículo

**Observado:** al dar de alta un artículo desde un producto existente en `tienda/venta`, la categoría
no aparece seleccionada, aunque el producto base sí tenía una categoría elegida.

**Esperado:** el artículo debe heredar la categoría del producto base.

### Stock insuficiente — falta ofrecer agregar la cantidad faltante

**Observado:** se intentó dar de alta 105 artículos cuando el producto base tenía 100 de stock. La
pantalla mostró este error:

> Error al guardar  
> Stock insuficiente para el producto 'productoinicar' (id=20616). Disponible: 100, Solicitado: 105

**Esperado:** mostrar una modal que pregunte si se desea agregar los 5 de stock faltantes al producto
base y, si se confirma, continuar con el alta.

## Prueba — Cambiar `IR PAGANDO` a `APARTADO` desde el detalle de pedidos

### Caso confirmado: pedido con adelanto de $50

- Se creó un pedido `IR PAGANDO` con un adelanto de $50.
- En `Pedidos → Detalle`, al cambiar la forma de pago, `APARTADO` aparece bloqueado en gris.
- **Resultado:** correcto; registrar como prueba completada.

### Caso pendiente: pedido `IR PAGANDO` sin enganche

- Abrir el detalle de un pedido `IR PAGANDO` que no tenga enganche o abonos.
- Cambiarlo a `APARTADO` y observar el estado resultante; luego volverlo a `IR PAGANDO` para verificar
  qué estado conserva.

**Por aclarar:** al cambiarlo a `APARTADO`, ¿debe quedar marcado como `PAGADO`? ¿También corresponde
marcarlo como `ENTREGADO`, o ese estado se cambia por separado? Confirmar además qué debe pasar con el
estado y los pagos al regresarlo a `IR PAGANDO`.

## Filtro de pedidos entregados y cambio de cobro en pedidos unidos

### Falta identificar o filtrar los pedidos `ENTREGADO`

**Observado:** los pedidos `APARTADO` aparecen como apartados aunque no tengan pagos. No queda claro
dónde consultar o filtrar los pedidos que ya están `ENTREGADO`, ni si esa opción debería aparecer en
el filtro de pago o en otro filtro.

**Por aclarar:** agregar una opción `ENTREGADO` al filtro correspondiente para que aparezcan solo esos
pedidos. Confirmar en qué filtro debe estar.

### Cambiar a `IR PAGANDO` la forma de cobro de un grupo de tres pedidos

**Contexto:** se unieron tres pedidos que estaban `APARTADO` y sin pagos. Que el grupo no tenga pago
registrado es correcto. Después se intentó cambiar la forma de cobro del grupo a `IR PAGANDO` y guardar,
con la expectativa de que el cambio aplicara a los tres pedidos.

**Resultado observado:** apareció este error:

> Error  
> El pedido 1148 esta unido con otros pedidos: deshaz el grupo antes de cambiar su forma de cobro

**Esperado por confirmar:** que al cambiar la forma de cobro del grupo, los tres pedidos pasen juntos
de `APARTADO` a `IR PAGANDO`. Aclarar si el cambio debe permitirse manteniendo unido el grupo o si el
flujo debe pedir deshacerlo primero.
