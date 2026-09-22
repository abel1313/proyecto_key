package com.ventas.key.hexagonal.pedidoarticulo.aplicacion.servicio;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.ArticuloNoEncontradoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.CambioRompePromocionException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.EdicionPedidoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PedidoCerradoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PedidoQuedariaVacioException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.PrecioNoCobrableException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.StockInsuficienteException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDisponible;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PromocionDelPedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.EditarArticulosCasoUso;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.CatalogoArticuloPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.MovimientoStockPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PedidoArticuloPort;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.PromocionDePedidoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orquesta la edicion de articulos de un pedido.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p>Cada metodo privado hace <b>un</b> paso: validar que el pedido este abierto, validar el
 * precio, validar el stock, mover el stock, recalcular el total. El orden de esos pasos es lo
 * unico que vive en los metodos publicos.
 *
 * <p><b>Todo o nada.</b> Las tres operaciones son {@code @Transactional}: cambiar es devolver el
 * stock viejo y descontar el nuevo, y si lo segundo falla, lo primero no puede quedar hecho (R3).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EditarArticulosService implements EditarArticulosCasoUso {

    private final PedidoArticuloPort pedidos;
    private final CatalogoArticuloPort catalogo;
    private final MovimientoStockPort stock;
    private final PromocionDePedidoPort promociones;

    // ─────────────────────────── Agregar ───────────────────────────

    @Override
    @Transactional
    public PedidoEditable agregar(Integer pedidoId, AgregarArticulo peticion) {
        PedidoEditable pedido = pedidoAbierto(pedidoId);
        validarCantidad(peticion.cantidad());

        ArticuloDisponible articulo = articuloVendible(peticion.varianteId());
        double precio = precioValidado(articulo, peticion.precioUnitario());
        exigirStock(articulo, peticion.cantidad());

        stock.descontar(articulo.varianteId(), peticion.cantidad());
        sumarOCrearLinea(pedido, articulo, peticion.cantidad(), precio);

        log.info("Pedido {}: se agrego el articulo {} x{} a ${}",
                pedidoId, articulo.varianteId(), peticion.cantidad(), precio);
        return recalcular(pedidoId);
    }

    /**
     * Si el articulo ya esta como linea normal, suma cantidad; si no, crea la linea (R6).
     *
     * <p>Las lineas de promocion nunca absorben un agregado: por R5 lo agregado va a precio de
     * catalogo, y mezclarlo con una linea promocional cobraria el combo por algo que no es parte
     * del combo.
     */
    private void sumarOCrearLinea(PedidoEditable pedido, ArticuloDisponible articulo,
                                  int cantidad, double precio) {
        Optional<ArticuloDePedido> existente = pedido.lineaNormalDe(articulo.varianteId());
        if (existente.isPresent() && Math.abs(existente.get().precioUnitario() - precio) <= 0.01) {
            ArticuloDePedido linea = existente.get();
            pedidos.cambiarCantidad(linea.detalleId(), linea.cantidad() + cantidad);
        } else {
            pedidos.agregarLinea(pedido.pedidoId(), articulo.varianteId(), cantidad, precio);
        }
    }

    // ─────────────────────────── Cambiar ───────────────────────────

    @Override
    @Transactional
    public PedidoEditable cambiar(Integer pedidoId, Integer detalleId, CambiarArticulo peticion) {
        PedidoEditable pedido = pedidoAbierto(pedidoId);
        ArticuloDePedido linea = pedido.linea(detalleId)
                .orElseThrow(() -> ArticuloNoEncontradoException.lineaDePedido(pedidoId, detalleId));

        if (linea.varianteId().equals(peticion.varianteId())) {
            throw new EdicionPedidoException(
                    "La linea " + detalleId + " ya es de ese mismo articulo. Para cambiar la cantidad "
                            + "no hace falta cambiar el articulo");
        }

        int cantidad = peticion.cantidadElegida(linea.cantidad());
        validarCantidad(cantidad);
        ArticuloDisponible nuevo = articuloVendible(peticion.varianteId());

        return linea.esDePromocion()
                ? cambiarLineaDePromocion(pedido, linea, nuevo, cantidad, peticion)
                : cambiarLineaNormal(pedido, linea, nuevo, cantidad, peticion.precioUnitario());
    }

    /** Una linea sin promocion se cambia y ya: precio de catalogo y listo. */
    private PedidoEditable cambiarLineaNormal(PedidoEditable pedido, ArticuloDePedido linea,
                                              ArticuloDisponible nuevo, int cantidad, Double precioPedido) {
        double precio = precioValidado(nuevo, precioPedido);
        exigirStock(nuevo, cantidad);

        stock.devolver(linea.varianteId(), linea.cantidad());
        stock.descontar(nuevo.varianteId(), cantidad);
        pedidos.cambiarArticulo(linea.detalleId(), nuevo.varianteId(), precio);
        pedidos.cambiarCantidad(linea.detalleId(), cantidad);

        log.info("Pedido {}: la linea {} paso del articulo {} al {}",
                pedido.pedidoId(), linea.detalleId(), linea.varianteId(), nuevo.varianteId());
        return recalcular(pedido.pedidoId());
    }

    /**
     * Una linea de promocion: primero se busca el reemplazo <b>dentro del combo</b> (R4).
     *
     * <p>Si esta, se cambia al precio del combo y la promocion sigue entera. Si no esta, no se
     * decide solo: se devuelven las dos salidas, o se aplica la que el usuario ya eligio.
     */
    private PedidoEditable cambiarLineaDePromocion(PedidoEditable pedido, ArticuloDePedido linea,
                                                   ArticuloDisponible nuevo, int cantidad,
                                                   CambiarArticulo peticion) {
        PromocionDelPedido promo = promociones.buscar(linea.promocionId())
                .orElseThrow(() -> new EdicionPedidoException(
                        "La promocion de la linea " + linea.detalleId() + " ya no existe. "
                                + "Hay que quitarla del pedido para poder editarlo"));

        Optional<Double> precioEnCombo = promo.precioDe(nuevo.varianteId());
        if (precioEnCombo.isPresent()) {
            return cambiarDentroDelCombo(pedido, linea, nuevo, cantidad, precioEnCombo.get());
        }

        List<ArticuloDePedido> combo = pedido.lineasDe(promo.promocionId());
        return switch (peticion.modo() == null ? ModoCambio.VALIDAR : peticion.modo()) {
            case VALIDAR -> throw new CambioRompePromocionException(promo, combo, nuevo.nombre());
            case QUITAR_PROMOCION -> quitarComboYAgregar(pedido, combo, nuevo, cantidad, peticion.precioUnitario());
            case CONSERVAR_PROMOCION -> conservarComboYAgregar(pedido, nuevo, cantidad, peticion.precioUnitario());
        };
    }

    /** El reemplazo si estaba en el combo: se cambia al precio promocional y no se rompe nada. */
    private PedidoEditable cambiarDentroDelCombo(PedidoEditable pedido, ArticuloDePedido linea,
                                                 ArticuloDisponible nuevo, int cantidad, double precioCombo) {
        exigirStock(nuevo, cantidad);

        stock.devolver(linea.varianteId(), linea.cantidad());
        stock.descontar(nuevo.varianteId(), cantidad);
        pedidos.cambiarArticulo(linea.detalleId(), nuevo.varianteId(), precioCombo);
        pedidos.cambiarCantidad(linea.detalleId(), cantidad);

        log.info("Pedido {}: la linea {} cambio dentro de la promocion {} (combo intacto)",
                pedido.pedidoId(), linea.detalleId(), linea.promocionId());
        return recalcular(pedido.pedidoId());
    }

    /** Opcion (a): sale el combo entero, entra el articulo nuevo a precio de catalogo. */
    private PedidoEditable quitarComboYAgregar(PedidoEditable pedido, List<ArticuloDePedido> combo,
                                               ArticuloDisponible nuevo, int cantidad, Double precioPedido) {
        double precio = precioValidado(nuevo, precioPedido);
        exigirStock(nuevo, cantidad);

        devolverYBorrar(combo);
        pedidos.agregarLinea(pedido.pedidoId(), nuevo.varianteId(), cantidad, precio);
        stock.descontar(nuevo.varianteId(), cantidad);

        log.info("Pedido {}: se quito la promocion completa ({} lineas) y entro el articulo {}",
                pedido.pedidoId(), combo.size(), nuevo.varianteId());
        return recalcular(pedido.pedidoId());
    }

    /** Opcion (b): la promocion queda intacta y el articulo nuevo se suma aparte. */
    private PedidoEditable conservarComboYAgregar(PedidoEditable pedido, ArticuloDisponible nuevo,
                                                  int cantidad, Double precioPedido) {
        double precio = precioValidado(nuevo, precioPedido);
        exigirStock(nuevo, cantidad);

        stock.descontar(nuevo.varianteId(), cantidad);
        sumarOCrearLinea(pedido, nuevo, cantidad, precio);

        log.info("Pedido {}: se conservo la promocion y se agrego aparte el articulo {}",
                pedido.pedidoId(), nuevo.varianteId());
        return recalcular(pedido.pedidoId());
    }

    // ────────────────────── Quitar promocion ──────────────────────

    @Override
    @Transactional
    public PedidoEditable quitarPromocion(Integer pedidoId, Integer promocionId) {
        PedidoEditable pedido = pedidoAbierto(pedidoId);
        List<ArticuloDePedido> combo = pedido.lineasDe(promocionId);

        if (combo.isEmpty()) {
            throw new EdicionPedidoException(
                    "El pedido " + pedidoId + " no tiene articulos de la promocion " + promocionId);
        }
        if (pedido.quedariaVacio(combo)) {
            throw new PedidoQuedariaVacioException(pedidoId);
        }

        devolverYBorrar(combo);
        log.info("Pedido {}: se quito la promocion {} ({} lineas)", pedidoId, promocionId, combo.size());
        return recalcular(pedidoId);
    }

    // ───────────────────────── Pasos sueltos ─────────────────────────

    private PedidoEditable pedidoAbierto(Integer pedidoId) {
        PedidoEditable pedido = pedidos.buscarPedido(pedidoId)
                .orElseThrow(() -> new ArticuloNoEncontradoException("Pedido no encontrado: " + pedidoId));
        if (pedido.estaCerrado()) {
            throw new PedidoCerradoException(pedidoId, pedido.motivoDelCierre());
        }
        return pedido;
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new EdicionPedidoException(
                    "La cantidad tiene que ser mayor a 0. Para quitar un articulo esta el boton de quitar");
        }
    }

    private ArticuloDisponible articuloVendible(Integer varianteId) {
        ArticuloDisponible articulo = catalogo.leerParaEditar(varianteId)
                .orElseThrow(() -> ArticuloNoEncontradoException.enCatalogo(varianteId));
        if (!articulo.modeloHabilitado()) {
            throw new EdicionPedidoException("'" + articulo.nombre()
                    + "' ya no está a la venta: el producto está deshabilitado o dado de baja");
        }
        if (!articulo.habilitado()) {
            throw new EdicionPedidoException("'" + articulo.nombre()
                    + "' ya no está a la venta: el artículo está deshabilitado o dado de baja");
        }
        return articulo;
    }

    /** R2: o el precio normal, o el de rebaja. Sin precio pedido, el normal. */
    private double precioValidado(ArticuloDisponible articulo, Double precioPedido) {
        if (precioPedido == null) {
            return articulo.precios().porDefecto();
        }
        if (!articulo.precios().admite(precioPedido)) {
            throw new PrecioNoCobrableException(articulo.precios(), precioPedido);
        }
        return precioPedido;
    }

    private static void exigirStock(ArticuloDisponible articulo, int cantidad) {
        if (!articulo.alcanzaPara(cantidad)) {
            throw new StockInsuficienteException(articulo, cantidad);
        }
    }

    private void devolverYBorrar(List<ArticuloDePedido> lineas) {
        lineas.forEach(l -> stock.devolver(l.varianteId(), l.cantidad()));
        pedidos.borrarLineas(lineas);
    }

    /**
     * Relee el pedido y guarda el total desde cero (R7).
     *
     * <p>Se relee a proposito en vez de ajustar el objeto en memoria: despues de mover lineas y
     * stock, la copia vieja ya no describe el pedido, y ajustar el total con sumas y restas
     * arrastra para siempre cualquier descuadre que ya viniera de antes.
     */
    private PedidoEditable recalcular(Integer pedidoId) {
        PedidoEditable actualizado = pedidos.buscarPedido(pedidoId)
                .orElseThrow(() -> new ArticuloNoEncontradoException("Pedido no encontrado: " + pedidoId));
        pedidos.guardarTotal(pedidoId, actualizado.total());
        return actualizado;
    }
}
