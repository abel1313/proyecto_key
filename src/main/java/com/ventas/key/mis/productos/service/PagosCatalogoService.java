package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.IvaTerminal;
import com.ventas.key.mis.productos.entity.MesesIntereses;
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
                                o.getMesesIntereses() != null ? o.getMesesIntereses().getDescripcion() : "",
                                parsearCuotas(o.getMesesIntereses())))
                        .collect(Collectors.toList()));
            }
            return dto;
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }

    /**
     * `MesesIntereses.meses` es el numero de cuotas guardado como texto (ej. "3", "6", "12").
     * Sin esto, `OpcionMesesDto` nunca traia el numero de cuotas -- el front lo leia como
     * `undefined`, caia a un `?? 1` propio, y el cobro con terminal SIEMPRE se mandaba a 1 pago
     * sin importar el plan que el admin eligiera en el dialogo (encontrado 2026-08-27).
     */
    private Integer parsearCuotas(MesesIntereses mesesIntereses) {
        if (mesesIntereses == null || mesesIntereses.getMeses() == null) return 1;
        try {
            return Integer.parseInt(mesesIntereses.getMeses().trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}