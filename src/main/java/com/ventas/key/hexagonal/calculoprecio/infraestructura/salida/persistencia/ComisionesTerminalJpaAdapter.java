package com.ventas.key.hexagonal.calculoprecio.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.ComisionTerminal;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.ComisionesTerminalPort;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** [Hexagonal: Driven Adapter] Lee del catálogo de pagos (forma de pago + plan, con su tarifa e IVA de terminal) que ya usa el cobro. */
@Component
public class ComisionesTerminalJpaAdapter implements ComisionesTerminalPort {

    private final IPagosYMesesRepository pagosYMeses;

    public ComisionesTerminalJpaAdapter(IPagosYMesesRepository pagosYMeses) {
        this.pagosYMeses = pagosYMeses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComisionTerminal> listar() {
        return pagosYMeses.findAll().stream()
                .filter(p -> p.getMesesIntereses() != null && p.getMesesIntereses().getTarifaTerminal() != null)
                .map(this::aDominio)
                .toList();
    }

    private ComisionTerminal aDominio(PagosYMeses p) {
        MesesIntereses m = p.getMesesIntereses();
        String forma = p.getTipoPago() != null ? p.getTipoPago().getFormaPago() : "";
        String plan = m.getDescripcion() != null ? m.getDescripcion() : "";
        String descripcion = (forma + " · " + plan).replaceAll("^ · | · $", "").trim();
        BigDecimal iva = m.getIvaTerminal() != null && m.getIvaTerminal().getIva() != null ? BigDecimal.valueOf(m.getIvaTerminal().getIva()) : BigDecimal.ZERO;
        return new ComisionTerminal(p.getId(), descripcion, BigDecimal.valueOf(m.getTarifaTerminal().getTarifa()), iva);
    }
}
