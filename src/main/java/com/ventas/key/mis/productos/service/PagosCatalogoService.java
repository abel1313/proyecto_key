package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.IvaTerminal;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.TarifaTerminal;
import com.ventas.key.mis.productos.entity.TipoPago;
import com.ventas.key.mis.productos.models.OpcionPagoDto;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IIvaTerminalRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.ITarifaTerminalRepository;
import com.ventas.key.mis.productos.repository.ITipoPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagosCatalogoService {

    private final ITipoPagoRepository iTipoPagoRepository;
    private final ITarifaTerminalRepository iTarifaTerminalRepository;
    private final IIvaTerminalRepository iIvaTerminalRepository;
    private final IDetallePagoRepository iDetallePagoRepository;
    private final IPagosYMesesRepository iPagosYMesesRepository;

    @Cacheable("tiposPagoCache")
    public List<TipoPago> getTiposPago() {
        return iTipoPagoRepository.findAll();
    }

    @Cacheable("tarifasTerminalCache")
    public List<TarifaTerminal> getTarifasTerminal() {
        return iTarifaTerminalRepository.findAll();
    }

    @Cacheable("ivaCache")
    public List<IvaTerminal> getIva() {
        return iIvaTerminalRepository.findAll();
    }

    @Cacheable("opcionesPagoCache")
    public List<DetallePago> getOpcionesPago() {
        return iDetallePagoRepository.findAll();
    }

    @Cacheable(value = "opcionesPorTipoCache", key = "#tipoPagoId")
    public List<PagosYMeses> getOpcionesPorTipo(Integer tipoPagoId) {
        return iPagosYMesesRepository.findByTipoPago_Id(tipoPagoId);
    }

    //ero@Cacheable("opcionesPagoEstructuradaCache")
    public List<OpcionPagoDto> getOpcionesEstructuradas() {
        return iTipoPagoRepository.findAll().stream().map(tipo -> {
            List<PagosYMeses> opciones = iPagosYMesesRepository.findByTipoPago_Id(tipo.getId());
            OpcionPagoDto dto = new OpcionPagoDto();
            dto.setTipoPagoId(tipo.getId());
            dto.setFormaPago(tipo.getFormaPago());

            if (opciones.isEmpty()) {
                dto.setMostrarMeses(false);

            } else if (opciones.size() == 1) {
                dto.setMostrarMeses(false);
                dto.setPagosYMesesId(opciones.get(0).getId());
            } else {
                dto.setMostrarMeses(true);
                dto.setOpciones(opciones.stream()
                        .map(o -> new OpcionPagoDto.OpcionMesesDto(
                                o.getId(),
                                o.getMesesIntereses() != null ? o.getMesesIntereses().getDescripcion() : ""))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }
}