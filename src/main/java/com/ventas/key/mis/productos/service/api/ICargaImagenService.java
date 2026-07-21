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

    Producto completarProducto(Integer productoId, CompletarProductoDto request);
}
