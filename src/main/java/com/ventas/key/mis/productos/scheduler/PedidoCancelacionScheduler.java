package com.ventas.key.mis.productos.scheduler;

import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class PedidoCancelacionScheduler {

    private final IPedidoRepository iPedidoRepository;
    private final IPedidoService pedidoService;

    @Value("${pedidos.dias-limite-recogida:2}")
    private int diasLimite;

    public PedidoCancelacionScheduler(IPedidoRepository iPedidoRepository, IPedidoService pedidoService) {
        this.iPedidoRepository = iPedidoRepository;
        this.pedidoService = pedidoService;
    }

    // 2026-09-04: antes este scheduler duplicaba a mano la devolucion de stock (sin avisar a
    // favoritos ni mandar ningun correo al cliente) -- ahora delega en
    // IPedidoService.deletePedidoById(), el mismo camino que usa la cancelacion manual, para que
    // la cancelacion automatica por no recoger a tiempo tenga exactamente el mismo comportamiento:
    // devuelve stock, avisa a quien tiene la variante en Favoritos si cruza de 0 a N, y manda el
    // correo de seguimiento al cliente (respetando Cliente.recibirCorreos, igual que el resto de
    // avisos de seguimiento). Motivo "TIMEOUT" a proposito -- IPedidoRepository.calcularScore ya
    // cuenta TIMEOUT/NO_SE_PRESENTO en contra del cliente para el score de rifa: si apartó algo y
    // no lo recogió en el plazo, cuenta como incumplimiento (decision del dueño, 2026-09-04).
    @Scheduled(cron = "0 0 8 * * *")
    public void cancelarPedidosVencidos() {
        LocalDate fechaLimite = LocalDate.now().minusDays(diasLimite);
        List<Pedido> vencidos = iPedidoRepository
                .findByEstadoPedidoAndFechaRecogidaIsNotNullAndFechaRecogidaLessThanEqual("Pendiente", fechaLimite);

        int cancelados = 0;
        for (Pedido pedido : vencidos) {
            try {
                pedidoService.deletePedidoById(pedido.getId(), "TIMEOUT");
                cancelados++;
                log.info("Pedido {} cancelado automáticamente. Fecha recogida: {}", pedido.getId(), pedido.getFechaRecogida());
            } catch (Exception e) {
                log.warn("No se pudo cancelar automáticamente el pedido {}: {}", pedido.getId(), e.getMessage(), e);
            }
        }

        if (cancelados > 0) {
            log.info("Se cancelaron {} pedidos vencidos", cancelados);
        }
    }
}
