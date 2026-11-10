package com.ventas.key.hexagonal.campana.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PreciosDeCampanaEnPedidoPort;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.PreciosBaseDeLineaPort;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Trabaja sobre las MISMAS entidades que ya trae la petición (misma transacción), así el
 * total que se relee después del cambio ya ve las líneas regresadas a su precio de antes.
 */
@Component
@Slf4j
public class PreciosDeCampanaEnPedidoJpaAdapter implements PreciosDeCampanaEnPedidoPort {

    private final IPedidoRepository pedidos;
    private final IDetallePedidoRepository detalles;
    private final PreciosBaseDeLineaPort preciosBase;

    public PreciosDeCampanaEnPedidoJpaAdapter(IPedidoRepository pedidos, IDetallePedidoRepository detalles, PreciosBaseDeLineaPort preciosBase) {
        this.pedidos = pedidos;
        this.detalles = detalles;
        this.preciosBase = preciosBase;
    }

    private Map<Integer, BigDecimal> marcas(Pedido p) {
        if (p == null || p.getDetalles() == null || p.getDetalles().isEmpty()) {
            return Map.of();
        }
        return preciosBase.leer(p.getDetalles().stream().map(DetallePedido::getId).toList(), OrigenDePrecio.CAMPANA);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tiene(int pedidoId) {
        return !marcas(pedidos.findById(pedidoId).orElse(null)).isEmpty();
    }

    @Override
    @Transactional
    public boolean restaurar(int pedidoId) {
        Pedido pedido = pedidos.findById(pedidoId).orElse(null);
        Map<Integer, BigDecimal> marcas = marcas(pedido);
        if (marcas.isEmpty()) {
            return false;
        }
        double total = 0.0;
        for (DetallePedido d : pedido.getDetalles()) {
            BigDecimal antes = marcas.get(d.getId());
            if (antes != null) {
                log.info("Pedido {}: la linea {} regresa de ${} a ${} (Ir pagando no lleva precio de campana)", pedidoId, d.getId(),
                        d.getPrecioUnitario(), antes);
                d.setPrecioUnitario(antes.doubleValue());
                d.setSubTotal(antes.doubleValue() * d.getCantidad());
                detalles.save(d);
            }
            total += d.getSubTotal() == null ? 0.0 : d.getSubTotal();
        }
        pedido.setTotalPedido(total);
        pedidos.save(pedido);
        preciosBase.borrar(List.copyOf(marcas.keySet()));
        return true;
    }
}
