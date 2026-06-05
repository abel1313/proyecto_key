# Cambios — Rediseño de Ventas, Pedidos y Detalles
for /f "tokens=5" %a in ('netstat -ano ^| findstr :9091') do taskkill /PID %a /F

## Archivos modificados

### entity/DetallePedido.java
- variante_id ahora es nullable=false — siempre se debe saber que variante se pidio
- cantidad ahora tiene @Column(nullable=false) — evita que quede null en BD

### entity/Pedido.java
- cliente_id ya no tiene nullable=false — puede ser null cuando el comprador no esta registrado
- Se agrego campo clienteSinRegistro con @JoinColumn(name="cliente_sin_registro_id")

### entity/Venta.java
- List<DetalleVenta> reemplazada por List<DetalleVentaVariante>

### service/VentaServiceImpl.java
- Se elimino IDetalleVentaRepository del constructor (nunca se uso)
- Se corrigio el bug donde pedido.setCliente(null) rompia la BD en ventas sin cliente registrado
- saveVentaDetalle ahora siempre crea un Pedido (los 3 escenarios)
- Se genera DetallePedido (que se pidio) y DetalleVentaVariante (datos financieros) en el mismo flujo

## Archivos nuevos

### entity/DetalleVentaVariante.java
- Entidad que mapea la tabla detalle_venta_variantes que ya existia en BD sin clase Java
- Campos: venta_id, variante_id, cantidad, precioUnitario, subTotal, precioCosto, ganancia, fechaVenta

### repository/IDetalleVentaVarianteRepository.java
- Repositorio para DetalleVentaVariante

## Archivos deprecados (no se usan mas, no se eliminan aun)

### entity/DetalleVenta.java
- Ya no se usa en el flujo de ventas directas
- La tabla detalle_venta queda sin nuevos registros

### repository/IDetalleVentaRepository.java
- Sin uso activo

---

## Flujo resultante

ESCENARIO 1 — Pedido desde app (cliente registrado)
  App crea Pedido (PENDIENTE) + DetallePedido[]
  Cliente paga en local: Venta + DetalleVentaVariante[] + Pedido pasa a ENTREGADO

ESCENARIO 2 — Venta directa, cliente registrado
  Cajero: Pedido (ENTREGADO) + DetallePedido[] + Venta + DetalleVentaVariante[]

ESCENARIO 3 — Venta directa, cliente sin registro
  Cajero captura datos: ClienteSinRegistro (para rifas)
  Pedido (cliente=null, clienteSinRegistro=X) + DetallePedido[]
  Venta (cliente=null, clienteSinRegistro=X) + DetalleVentaVariante[]

## Reporte mensual unificado

Con todos los escenarios creando Pedido, el reporte es una sola query:

  SELECT p.*, c.nombre_persona as cliente, csr.nombre_persona as visitante
  FROM pedidos p
  LEFT JOIN clientes c ON c.id = p.cliente_id
  LEFT JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
  WHERE p.fecha_pedido BETWEEN '2026-05-01' AND '2026-05-31'
  ORDER BY p.fecha_pedido DESC;





Tarea para el Backend — Imágenes en el Chatbot

Contexto general:
El chatbot de Novedades Jade usa OpenAI (ChatGPT) para responder a los clientes. Actualmente ya existe un método obtenerContextoVariantes() que construye el catálogo de productos y se lo pasa al bot como contexto. Se necesita extender esa funcionalidad para incluir imágenes en las respuestas del chat.

¿Cómo funciona el flujo?

La base de datos guarda el nombre o ruta del archivo de imagen por variante.
El método obtenerContextoVariantes() incluye ese dato en el contexto que se le manda a OpenAI.
OpenAI responde mencionando la imagen con el formato: [IMG:nombre-archivo]
Angular detecta ese marcador en la respuesta del bot.
Angular llama al endpoint de imágenes con ese nombre de archivo.
El endpoint devuelve el base64 de la imagen.
Angular muestra la imagen directamente en el chat.
Importante: OpenAI nunca recibe la imagen ni el base64. Solo recibe el nombre del archivo como texto corto. Eso evita gastar tokens innecesarios y mantiene el costo bajo.

Cambios necesarios en el Backend:

Campo de imagen en la entidad Variantes.
Verificar que la entidad Variantes tenga un campo para guardar la referencia de la imagen, por ejemplo: imagenNombre (ejemplo de valor: "mochila-rosa.jpg").
Modificar obtenerContextoVariantes().
Agregar el nombre de imagen al contexto que se le pasa a OpenAI. Si la variante tiene imagen, debe aparecer así en el contexto:
[CB:7501234567890] Mochila Rosa Nike, talla: M, color: Rosa, precio: $250 MXN, stock: 5 pzas, imagen: mochila-rosa.jpg
Endpoint para servir imágenes en base64.
Crear o verificar que exista un endpoint GET /api/imagenes/{nombreArchivo} que dado el nombre del archivo devuelva la imagen en base64. Las imágenes están almacenadas en el disco de la VPS. El endpoint lee el archivo físico y lo convierte a base64. La respuesta debe incluir el base64 y el tipo de imagen (image/jpeg, image/png, etc).
Modificar el System Prompt del chatbot.
Agregar esta regla al prompt para que el bot sepa cómo indicar las imágenes: cuando el bot mencione un producto que tenga imagen, debe incluir al final exactamente esto: [IMG:nombre-del-archivo]. No debe modificar ni inventar el nombre del archivo, debe usar exactamente el que aparece en el catálogo. Ejemplo de respuesta del bot: "Mochila Rosa Nike - $250 MXN [IMG:mochila-rosa.jpg]"
Lo que hará el Frontend (Angular) con esto:
Angular detectará el patrón [IMG:archivo] en la respuesta del bot, llamará al endpoint /api/imagenes/{archivo}, recibirá el base64 y mostrará la imagen directamente en el chat. El cliente verá la imagen dentro de la conversación sin necesidad de hacer clic en ningún link.

Resumen de tareas:

Verificar que exista el campo de imagen en la entidad Variantes.
Modificar obtenerContextoVariantes() para incluir el nombre de imagen en el contexto.
Crear el endpoint GET /api/imagenes/{nombre} que devuelva el base64 desde el disco de la VPS.
Actualizar el System Prompt para que el bot use el formato [IMG:archivo] cuando haya imagen disponible.




Tarea para el Frontend — Imágenes en el Chatbot

Contexto general:
El chatbot actualmente muestra las respuestas del bot como texto plano. Se necesita modificar el componente del chat para que detecte cuando el bot menciona una imagen y la muestre directamente en la conversación.

¿Cómo funciona?
El bot va a responder con un marcador especial cuando haya una imagen disponible, así:
"Mochila Rosa Nike - $250 MXN [IMG:mochila-rosa.jpg]"

Angular debe detectar ese marcador, ir a buscar la imagen al backend y mostrarla dentro del chat.

Cambios necesarios en el Frontend:

Servicio para obtener imágenes.
Crear o modificar el servicio del chat para agregar un método que llame al endpoint del backend GET /api/imagenes/{nombreArchivo} y devuelva el base64 con el tipo de imagen.
Método para parsear la respuesta del bot.
Crear un método que reciba el texto de respuesta del bot, detecte todos los marcadores [IMG:nombre-archivo], llame al endpoint por cada imagen encontrada y reemplace el marcador por una etiqueta img con el base64. Ejemplo de lo que debe generar: <img src="data:image/jpeg;base64,..." style="max-width:200px; border-radius:8px; margin:6px 0;" />
Renderizar el HTML en el template.
En el componente donde se muestran los mensajes del bot, usar [innerHTML] en lugar de texto plano para que Angular renderice la imagen correctamente. Usar DomSanitizer para marcar el HTML como seguro antes de pasarlo al template.
Estilos opcionales recomendados.
Las imágenes deben tener un ancho máximo para no romper el diseño del chat (máximo 200px o el que mejor se vea). Se recomienda agregar border-radius para que se vean más amigables y un pequeño margen entre la imagen y el texto.
Resumen de tareas:

Agregar método en el servicio para llamar a GET /api/imagenes/{nombreArchivo}.
Crear método que parsee el texto del bot, detecte [IMG:archivo], busque la imagen y genere el HTML con el base64.
Modificar el template del chat para usar [innerHTML] con DomSanitizer en los mensajes del bot.
Ajustar estilos de las imágenes para que se vean bien dentro del chat.