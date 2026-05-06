


Micro servicio que permite compras de bolsas, pantalones faldas de mujer
1.- controlador AbstractController permite generar un CRUD generico
2.- AdminController permite eliminar la cache de redis
3.- AuthController 
    1.1 loginpermite acceder al sistema, incluye seguridad al intentar acceder varias veces y la contrasena incorrecta, genera el token y el refresh token ademas de
         utilizas las cokies para no alamacenar en el navegador y devuelve el token
    1.2 refresh permite validar el token y renovarlo
    1.3.- logout limpia el token y cierra la sesion

4.- controlador ChatbotController
    4.1- mensaje valida que la ip no este bloqueada, si esta bloqueada lo hace que espero unos minutos para volver a enviar mensaje, 
    el chat bot analiza loq ue poregunto y obtiene los productos de la base de datos para dar una respuesta en caso de que pregunto por algo de lo que vendemos
5.- controlador ClienteControllerImpl extiende AbstractController para obtener el CRUD y contiene mas endpoint para save que sobreewscribe al del abtract
    buscar cliente por id y puscar clientes paginados por nombre

6.- controlador ProductosControllerImpl maneja productos
    6.1 GET /productos/obtenerProductos - lista paginada de productos (publica)
    6.2 GET /productos/buscarNombreOrCodigoBarra - busqueda paginada por nombre o codigo de barras (publica)
    6.3 POST /productos/save y PUT /productos/update - guardan/actualizan producto; al enviar imagenes se guardan automaticamente en las variantes que ya tenga el producto
    6.4 GET /productos/findById/{id} - detalle del producto
    6.5 DELETE /productos/deleteBy/{id} - elimina producto con sus variantes e imagenes
    6.6 GET /productos/admin/diagnostico-imagenes/{productoId} - ADMIN: diagnostica por que no aparece la imagen de un producto en el listado
        Responde:
        - totalImagenesLocalDB: cuantas imagenes tiene el producto en la BD local (tabla producto_imagen_copy)
        - imagenesLocalDB: detalle de cada imagen (id, nombre, extension, rutaDisco)
        - imagenPresenteEnMicroservicio: si el microservicio externo devuelve imagen al hacer el listado
        - detalleExternoLista: "imagen presente con datos" / "null - el microservicio no devolvio respuesta" / "error: ..."
        Casos posibles:
          totalImagenesLocalDB=0 → nunca se guardo la imagen en BD
          totalImagenesLocalDB>0 y imagenPresenteEnMicroservicio=false → BD tiene el registro pero el microservicio no tiene el archivo
          totalImagenesLocalDB>0 y imagenPresenteEnMicroservicio=true → todo correcto, revisar cache

7.- controlador VarianteController maneja variantes de productos
    7.1 GET /variantes/buscar - busqueda paginada de variantes con imagen incluida (publica)
    7.2 GET /variantes/porProducto/{productoId} - variantes de un producto
    7.3 POST /variantes/guardarConImagenes - guarda variantes con sus imagenes
    7.4 POST /variantes/inicializarDesdeProducto - crea variantes en lote desde un producto con imagenes opcionales
    7.5 GET /variantes/imagenes/{varianteId} - imagenes de una variante especifica
    7.6 DELETE /variantes/{varianteId}/imagenes - elimina imagenes especificas de una variante
    7.7 GET /variantes/admin/diagnostico-imagenes/{varianteId} - ADMIN: diagnostica por que no aparece la imagen de una variante en el listado
        Responde:
        - totalImagenesLocalDB: cuantas imagenes tiene la variante en BD local (tabla variante_imagen)
        - imagenesLocalDB: detalle de cada imagen (id, nombre, extension, rutaDisco)
        - idsConDatosEnMicroservicio: IDs cuyo archivo existe en el microservicio de imagenes
        - idsSinDatosEnMicroservicio: IDs que estan en BD pero el microservicio no tiene el archivo
        - consistente: true si todos los IDs de BD tienen archivo en el microservicio
        Casos posibles:
          totalImagenesLocalDB=0 → nunca se guardo la imagen en BD
          idsSinDatosEnMicroservicio no vacio → BD tiene el registro pero el archivo se perdio en el microservicio
          consistente=true → todo correcto, revisar cache