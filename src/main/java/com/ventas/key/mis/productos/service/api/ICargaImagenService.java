package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.dto.CompletarProductoDto;
import com.ventas.key.mis.productos.dto.EstadoCargaProductoDto;
import com.ventas.key.mis.productos.entity.Producto;

import java.util.List;

public interface ICargaImagenService {

    // Crea producto+variante borrador de inmediato (sincrono, sin esperar la imagen).
    // El front recibe productoId/varianteId al instante con estadoImagen=PENDIENTE.
    EstadoCargaProductoDto crearBorrador();

    // Sube la imagen al microservicio y la enlaza a producto+variante. Corre en background
    // (@Async) — se llama desde el controller justo despues de crearBorrador()/reintentar(),
    // nunca desde dentro de este mismo servicio (si no, el proxy de Spring no lo ejecuta async).
    void subirImagenAsync(Integer productoId, Integer varianteId, byte[] bytes, String nombreArchivo);

    // Reintento: reusa el MISMO producto/variante (no crea un borrador nuevo), vuelve a
    // dejarlo en PENDIENTE para que el front dispare subirImagenAsync con una imagen nueva.
    EstadoCargaProductoDto marcarPendienteParaReintento(Integer productoId);

    List<EstadoCargaProductoDto> consultarEstado(List<Integer> productoIds);

    List<EstadoCargaProductoDto> listarFallidas();

    // TODOS los borradores vivos (PENDIENTE + EXITOSO + FALLIDO), sin importar habilitado.
    // Es lo que pinta la pantalla de Carga rapida: antes lo armaba el front con
    // GET /v1/productos/admin/filtrar?codigoGenerado=true&habilitado=false + GET /estado, y
    // bastaba que a un borrador se le tocara `habilitado` o `codigoBarrasGenerado` desde
    // cualquier otra pantalla para que se cayera de ese filtro y quedara inalcanzable.
    List<EstadoCargaProductoDto> listarBorradores();

    Producto completarProducto(Integer productoId, CompletarProductoDto request);

    // Descarta por completo un borrador que nunca se va a completar (foto de prueba, borrosa,
    // repetida, etc.): borra producto, variante, imagen (local + microservicio) y el codigo de
    // barras placeholder. Solo permitido mientras codigoBarrasGenerado == true, para no poder
    // borrar por accidente un producto ya completado/real.
    void eliminarBorrador(Integer productoId);
}
