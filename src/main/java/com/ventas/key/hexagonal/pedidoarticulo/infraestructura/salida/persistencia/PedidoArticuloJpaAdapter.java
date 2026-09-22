package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PedidoArticuloPort;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Las lineas de un pedido, contra las tablas {@code pedidos} y {@code detalle_pedidos}.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Aqui vive toda la traduccion entre el modelo del dominio y las entidades JPA. El dominio no
 * sabe que existe una tabla {@code variantes} ni que la linea se llama {@code DetallePedido}.
 */
@Component
@RequiredArgsConstructor
public class PedidoArticuloJpaAdapter implements PedidoArticuloPort {

    private final IPedidoRepository pedidoRepository;
    private final IDetallePedidoRepository detalleRepository;
    private final IVarianteRepository varianteRepository;

    @Override
    public Optional<PedidoEditable> buscarPedido(Integer pedidoId) {
        return pedidoRepository.findById(pedidoId).map(PedidoArticuloJpaAdapter::aDominio);
    }

    @Override
    public Integer agregarLinea(Integer pedidoId, Integer varianteId, int cantidad, double precioUnitario) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado: " + pedidoId));
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new IllegalStateException("Articulo no encontrado: " + varianteId));

        DetallePedido linea = new DetallePedido();
        linea.setPedido(pedido);
        linea.setVariante(variante);
        linea.setProducto(variante.getProducto());
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(precioUnitario);
        linea.setSubTotal(precioUnitario * cantidad);
        // promocion queda null a proposito: lo que se agrega va a precio de catalogo (R5).

        return detalleRepository.save(linea).getId();
    }

    @Override
    public void cambiarCantidad(Integer detalleId, int cantidadNueva) {
        DetallePedido linea = linea(detalleId);
        linea.setCantidad(cantidadNueva);
        linea.setSubTotal(linea.getPrecioUnitario() * cantidadNueva);
        detalleRepository.save(linea);
    }

    @Override
    public void cambiarArticulo(Integer detalleId, Integer varianteNuevaId, double precioUnitario) {
        DetallePedido linea = linea(detalleId);
        Variantes variante = varianteRepository.findById(varianteNuevaId)
                .orElseThrow(() -> new IllegalStateException("Articulo no encontrado: " + varianteNuevaId));

        linea.setVariante(variante);
        linea.setProducto(variante.getProducto());
        linea.setPrecioUnitario(precioUnitario);
        linea.setSubTotal(precioUnitario * linea.getCantidad());
        // La promocion de la linea NO se toca: cambiar dentro del combo lo mantiene.
        detalleRepository.save(linea);
    }

    @Override
    public void borrarLineas(List<ArticuloDePedido> lineas) {
        lineas.forEach(l -> detalleRepository.deleteById(l.detalleId()));
    }

    @Override
    public void guardarTotal(Integer pedidoId, double total) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado: " + pedidoId));
        pedido.setTotalPedido(total);
        // totalPagado no se toca (R7): si el pedido queda pagado de mas, eso es una devolucion y
        // la decide el negocio, no esta edicion.
        pedidoRepository.save(pedido);
    }

    private DetallePedido linea(Integer detalleId) {
        return detalleRepository.findById(detalleId)
                .orElseThrow(() -> new IllegalStateException("Linea de pedido no encontrada: " + detalleId));
    }

    private static PedidoEditable aDominio(Pedido pedido) {
        List<ArticuloDePedido> articulos = pedido.getDetalles() == null
                ? List.of()
                : pedido.getDetalles().stream().map(PedidoArticuloJpaAdapter::aDominio).toList();

        return new PedidoEditable(
                pedido.getId(),
                pedido.getEstadoPedido(),
                pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0,
                articulos);
    }

    private static ArticuloDePedido aDominio(DetallePedido linea) {
        Variantes variante = linea.getVariante();
        return new ArticuloDePedido(
                linea.getId(),
                variante != null ? variante.getId() : null,
                linea.getProducto() != null ? linea.getProducto().getId() : null,
                nombreDe(linea),
                linea.getCantidad() != null ? linea.getCantidad() : 0,
                linea.getPrecioUnitario() != null ? linea.getPrecioUnitario() : 0.0,
                linea.getPromocion() != null ? linea.getPromocion().getId() : null);
    }

    /**
     * Como se llama este articulo para un humano.
     *
     * <p>El nombre vive en el producto y la talla/color en la variante, asi que un mensaje que
     * diga solo el nombre del producto no distingue dos lineas del mismo modelo -- que es
     * justamente el caso que hay que poder explicar al cambiar una talla.
     */
    private static String nombreDe(DetallePedido linea) {
        String base = linea.getProducto() != null ? linea.getProducto().getNombre() : "Articulo";
        Variantes v = linea.getVariante();
        if (v == null) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        if (v.getTalla() != null && !v.getTalla().isBlank()) {
            sb.append(" talla ").append(v.getTalla());
        }
        if (v.getColor() != null && !v.getColor().isBlank()) {
            sb.append(" ").append(v.getColor());
        }
        return sb.toString();
    }
}
