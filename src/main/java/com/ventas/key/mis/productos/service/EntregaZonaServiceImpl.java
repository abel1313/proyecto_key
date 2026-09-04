package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.pedidos.EntregaZonaPendienteDto;
import com.ventas.key.mis.productos.models.pedidos.EntregaZonaSemanaResponse;
import com.ventas.key.mis.productos.models.pedidos.ProgramarEntregaZonaRequest;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * "Entregas por zona" (2026-09-04): el cliente en el checkout de Tienda solo elige LA ZONA
 * (Zacazonapan, Tejupilco, Luvianos...), nunca un punto exacto -- el dueño hace un viaje por
 * semana a cada zona y decide un único punto de encuentro para todos los que pidieron ahí esa
 * semana (lunes a viernes), no es entrega puerta a puerta por cliente. Esta pantalla es donde el
 * admin arma ese viaje: ve cuántos pedidos hay pendientes de la zona esta semana, elige
 * fecha/hora/punto de encuentro, y de un jalón se les avisa a todos por correo. No aplica a
 * "recoger en tienda" (ver LugarEntrega.esRecogerEnTienda) ni a ramos de flores (tienen su propio
 * flujo de fecha por pedido).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaZonaServiceImpl {

    private final ILugarEntregaRepository iLugarEntregaRepository;
    private final IPedidoRepository iPedidoRepository;
    private final EmailService emailService;

    public EntregaZonaSemanaResponse listarPendientesSemana(Integer lugarEntregaId) {
        LugarEntrega lugar = obtenerZonaReal(lugarEntregaId);

        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate viernes = lunes.plusDays(4);

        LocalDate fechaSugerida = null;
        if (lugar.getDiaEntregaSemanal() != null) {
            DayOfWeek diaConfigurado = DayOfWeek.of(lugar.getDiaEntregaSemanal());
            fechaSugerida = lunes.with(TemporalAdjusters.nextOrSame(diaConfigurado));
        }

        List<Pedido> pendientes = iPedidoRepository.findPendientesDeZonaEnRango(lugarEntregaId, lunes, viernes);
        List<EntregaZonaPendienteDto> dtos = pendientes.stream()
                .map(p -> new EntregaZonaPendienteDto(p.getId(), nombreDe(p), correoDe(p), p.getTotalPedido(), p.getFechaPedido()))
                .toList();

        return new EntregaZonaSemanaResponse(lunes, viernes, fechaSugerida, dtos);
    }

    @Transactional
    public int programarEntrega(Integer lugarEntregaId, ProgramarEntregaZonaRequest request) {
        if (request.getFecha() == null || request.getHora() == null || request.getHora().isBlank()
                || request.getPuntoEncuentro() == null || request.getPuntoEncuentro().isBlank()) {
            throw new RuntimeException("Fecha, hora y punto de encuentro son obligatorios");
        }
        LugarEntrega lugar = obtenerZonaReal(lugarEntregaId);

        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate viernes = lunes.plusDays(4);
        List<Pedido> pendientes = iPedidoRepository.findPendientesDeZonaEnRango(lugarEntregaId, lunes, viernes);

        int enviados = 0;
        for (Pedido pedido : pendientes) {
            pedido.setFechaRecogida(request.getFecha());
            iPedidoRepository.save(pedido);

            String correo = correoDe(pedido);
            if (correo == null || correo.isBlank()) continue;
            try {
                boolean ok = emailService.enviarProgramacionEntregaZona(correo, nombreDe(pedido), pedido.getId(),
                        lugar.getNombre(), request.getFecha(), request.getHora(), request.getPuntoEncuentro());
                if (ok) enviados++;
            } catch (Exception e) {
                log.warn("No se pudo avisar de la entrega de zona al pedido {}: {}", pedido.getId(), e.getMessage());
            }
        }
        log.info("Entrega de zona '{}' programada para {} {} en '{}': {} pedido(s), {} correo(s) enviado(s)",
                lugar.getNombre(), request.getFecha(), request.getHora(), request.getPuntoEncuentro(),
                pendientes.size(), enviados);
        return enviados;
    }

    private LugarEntrega obtenerZonaReal(Integer lugarEntregaId) {
        LugarEntrega lugar = iLugarEntregaRepository.findById(lugarEntregaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + lugarEntregaId));
        if (Boolean.TRUE.equals(lugar.getEsRecogerEnTienda())) {
            throw new RuntimeException("\"" + lugar.getNombre() + "\" es recoger en tienda, no una zona de entrega");
        }
        return lugar;
    }

    private String nombreDe(Pedido pedido) {
        Cliente c = pedido.getCliente();
        if (c != null) return c.getNombrePersona();
        ClienteSinRegistro csr = pedido.getClienteSinRegistro();
        return csr != null ? csr.getNombrePersona() : "Cliente";
    }

    private String correoDe(Pedido pedido) {
        Cliente c = pedido.getCliente();
        if (c != null) return c.getCorreoElectronico();
        ClienteSinRegistro csr = pedido.getClienteSinRegistro();
        return csr != null ? csr.getCorreoElectronico() : null;
    }
}
