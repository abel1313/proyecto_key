package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class PedidoCancelacionScheduler {

    private final IPedidoRepository iPedidoRepository;
    private final IProductosRepository iProductosRepository;
    private final IVarianteRepository iVarianteRepository;

    @Value("${pedidos.dias-limite-recogida:2}")
    private int diasLimite;

    public PedidoCancelacionScheduler(IPedidoRepository iPedidoRepository,
                                      IProductosRepository iProductosRepository,
                                      IVarianteRepository iVarianteRepository) {
        this.iPedidoRepository = iPedidoRepository;
        this.iProductosRepository = iProductosRepository;
        this.iVarianteRepository = iVarianteRepository;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void cancelarPedidosVencidos() {
        LocalDate fechaLimite = LocalDate.now().minusDays(diasLimite);
        List<Pedido> vencidos = iPedidoRepository
                .findByEstadoPedidoAndFechaRecogidaIsNotNullAndFechaRecogidaLessThanEqual("Pendiente", fechaLimite);

        for (Pedido pedido : vencidos) {
            pedido.getDetalles().forEach(detalle -> {
                Producto prod = iProductosRepository.findById(detalle.getProducto().getId()).orElse(null);
                if (prod != null) {
                    prod.setStock(prod.getStock() + detalle.getCantidad());
                    iProductosRepository.save(prod);
                }
                if (detalle.getVariante() != null) {
                    Variantes variante = iVarianteRepository.findById(detalle.getVariante().getId()).orElse(null);
                    if (variante != null) {
                        variante.setStock(variante.getStock() + detalle.getCantidad());
                        iVarianteRepository.save(variante);
                    }
                }
            });
            pedido.setEstadoPedido("cancelado");
            iPedidoRepository.save(pedido);
            log.info("Pedido {} cancelado automáticamente. Fecha recogida: {}", pedido.getId(), pedido.getFechaRecogida());
        }

        if (!vencidos.isEmpty()) {
            log.info("Se cancelaron {} pedidos vencidos", vencidos.size());
        }
    }
}