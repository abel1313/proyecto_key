package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.ListonCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ListonSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadResponseDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import com.ventas.key.mis.productos.repository.IFraseListonPredefinidaRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Motor de calculo de precio de un ramo de flores eternas. No persiste nada todavia -- ver
// nota de alcance en PROPUESTA_FLORES_ETERNAS.md sobre la integracion pendiente con Pedido.
@Service
public class FlorPedidoServiceImpl {

    private final ITipoFlorRepository iTipoFlorRepository;
    private final ICantidadFlorValidaRepository iCantidadFlorValidaRepository;
    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository;
    private final ILugarEntregaRepository iLugarEntregaRepository;

    public FlorPedidoServiceImpl(ITipoFlorRepository iTipoFlorRepository,
                                  ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                  IAccesorioRamoRepository iAccesorioRamoRepository,
                                  IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository,
                                  ILugarEntregaRepository iLugarEntregaRepository) {
        this.iTipoFlorRepository = iTipoFlorRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.iFraseListonPredefinidaRepository = iFraseListonPredefinidaRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
    }

    @Transactional(readOnly = true)
    public ValidarCantidadResponseDto validarCantidad(ValidarCantidadRequestDto dto) {
        if (dto.getCantidadSolicitada() == null || dto.getCantidadSolicitada() <= 0) {
            throw new RuntimeException("La cantidad solicitada debe ser mayor a cero");
        }
        TipoFlor tipoFlor = iTipoFlorRepository.findById(dto.getTipoFlorId())
                .orElseThrow(() -> new ExceptionDataNotFound("Tipo de flor no encontrado: " + dto.getTipoFlorId()));

        int cantidadSolicitada = dto.getCantidadSolicitada();
        double precioSolicitada = cantidadSolicitada * tipoFlor.getPrecioPorFlor();

        List<CantidadFlorValida> validas = iCantidadFlorValidaRepository
                .findActivasPorTipoFlorOrdenadas(tipoFlor.getId());

        ValidarCantidadResponseDto response = new ValidarCantidadResponseDto();
        response.setCantidadSolicitada(cantidadSolicitada);
        response.setPrecioCantidadSolicitada(precioSolicitada);

        // Sin catalogo configurado, o la cantidad pedida es menor a la mas chica configurada:
        // no aplica el ajuste de "circulo" (venta por unidad, ej. 1 o 2 flores sueltas).
        boolean menorQueLaMasChica = !validas.isEmpty() && cantidadSolicitada < validas.get(0).getCantidad();
        if (validas.isEmpty() || menorQueLaMasChica) {
            response.setValida(true);
            response.setMensaje("Cantidad aceptada tal cual, se cobra por unidad.");
            return response;
        }

        CantidadFlorValida exacta = validas.stream()
                .filter(v -> v.getCantidad().equals(cantidadSolicitada))
                .findFirst().orElse(null);
        if (exacta != null) {
            response.setValida(true);
            response.setMensaje("Esta cantidad forma bien el circulo.");
            return response;
        }

        CantidadFlorValida menor = validas.stream()
                .filter(v -> v.getCantidad() < cantidadSolicitada)
                .reduce((a, b) -> b) // el ultimo de los menores = el mas cercano por abajo (lista viene ordenada asc)
                .orElse(null);
        CantidadFlorValida mayor = validas.stream()
                .filter(v -> v.getCantidad() > cantidadSolicitada)
                .findFirst().orElse(null);

        response.setValida(false);
        response.setMensaje("Con " + cantidadSolicitada + " flores el circulo puede no quedar bien formado.");
        if (menor != null) {
            response.setAlternativaMenor(menor.getCantidad());
            response.setPrecioAlternativaMenor(menor.getCantidad() * tipoFlor.getPrecioPorFlor());
        }
        if (mayor != null) {
            response.setAlternativaMayor(mayor.getCantidad());
            response.setPrecioAlternativaMayor(mayor.getCantidad() * tipoFlor.getPrecioPorFlor());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public CalcularPrecioResponseDto calcularPrecio(CalcularPrecioRequestDto dto) {
        if (dto.getCantidadFinal() == null || dto.getCantidadFinal() <= 0) {
            throw new RuntimeException("La cantidad final debe ser mayor a cero");
        }
        TipoFlor tipoFlor = iTipoFlorRepository.findById(dto.getTipoFlorId())
                .orElseThrow(() -> new ExceptionDataNotFound("Tipo de flor no encontrado: " + dto.getTipoFlorId()));

        int cantidadFinal = dto.getCantidadFinal();
        double precioBase = cantidadFinal * tipoFlor.getPrecioPorFlor();

        CalcularPrecioResponseDto response = new CalcularPrecioResponseDto();
        response.setCantidadFinal(cantidadFinal);
        response.setPrecioBase(precioBase);
        response.setTipoFlorVarianteId(tipoFlor.getVariante() != null ? tipoFlor.getVariante().getId() : null);

        Integer papelAccesorioId = aplicarReglaPapel(cantidadFinal, response);

        List<AccesorioCalculadoDto> accesoriosCalculados = calcularAccesorios(dto.getAccesorios(), papelAccesorioId);
        response.setAccesoriosCalculados(accesoriosCalculados);
        double subtotalAccesorios = accesoriosCalculados.stream().mapToDouble(AccesorioCalculadoDto::getSubtotal).sum();
        response.setSubtotalAccesorios(subtotalAccesorios);

        List<ListonCalculadoDto> listonesCalculados = calcularListones(dto.getListones());
        response.setListonesCalculados(listonesCalculados);
        double subtotalListones = listonesCalculados.stream()
                .filter(l -> l.getPrecio() != null)
                .mapToDouble(ListonCalculadoDto::getPrecio)
                .sum();
        response.setSubtotalListones(subtotalListones);

        boolean tienePendiente = listonesCalculados.stream()
                .anyMatch(l -> "PERSONALIZADA_PENDIENTE".equals(l.getTipo()));
        response.setTieneListonPendienteValidacion(tienePendiente);
        response.setRequiereAnticipo50Porciento(tienePendiente);
        response.setAvisoNoReembolso(tienePendiente ? FloresEternasConstantes.AVISO_NO_REEMBOLSO : null);

        double costoEnvio = calcularEnvio(dto, response);

        double totalConocido = precioBase
                + (Boolean.TRUE.equals(response.getPapelObligatorioAplicado()) ? response.getPrecioPapel() : 0)
                + subtotalAccesorios
                + subtotalListones
                + costoEnvio;
        response.setTotal(totalConocido);
        response.setMontoAnticipoSugerido(
                tienePendiente ? totalConocido * FloresEternasConstantes.PORCENTAJE_ANTICIPO_FRASE_PERSONALIZADA : null);

        return response;
    }

    private Integer aplicarReglaPapel(int cantidadFinal, CalcularPrecioResponseDto response) {
        if (cantidadFinal <= FloresEternasConstantes.UMBRAL_PAPEL_OBLIGATORIO) {
            response.setPapelObligatorioAplicado(false);
            return null;
        }
        Optional<AccesorioRamo> papel = iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue();
        if (papel.isEmpty()) {
            response.setPapelObligatorioAplicado(false);
            return null;
        }
        response.setPapelObligatorioAplicado(true);
        response.setPrecioPapel(papel.get().getPrecio());
        response.setPapelVarianteId(papel.get().getVariante() != null ? papel.get().getVariante().getId() : null);
        return papel.get().getId();
    }

    private List<AccesorioCalculadoDto> calcularAccesorios(List<AccesorioSeleccionadoDto> seleccionados, Integer papelAccesorioId) {
        List<AccesorioCalculadoDto> resultado = new ArrayList<>();
        if (seleccionados == null || seleccionados.isEmpty()) {
            return resultado;
        }
        Map<Integer, Integer> cantidadPorAccesorio = new LinkedHashMap<>();
        for (AccesorioSeleccionadoDto sel : seleccionados) {
            // El papel ya se cobro via la regla obligatoria -- si el cliente tambien lo eligio
            // manualmente en esta lista, se ignora esa entrada para no cobrarlo dos veces.
            if (papelAccesorioId != null && papelAccesorioId.equals(sel.getAccesorioId())) {
                continue;
            }
            cantidadPorAccesorio.merge(sel.getAccesorioId(), 1, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> entry : cantidadPorAccesorio.entrySet()) {
            AccesorioRamo accesorio = iAccesorioRamoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + entry.getKey()));
            if (!Boolean.TRUE.equals(accesorio.getActivo())) {
                throw new RuntimeException("El accesorio '" + accesorio.getNombre() + "' no esta disponible actualmente");
            }
            int cantidad = entry.getValue();
            double subtotal = accesorio.getPrecio() * cantidad;
            Integer varianteId = accesorio.getVariante() != null ? accesorio.getVariante().getId() : null;
            resultado.add(new AccesorioCalculadoDto(
                    accesorio.getId(), accesorio.getNombre(), cantidad, accesorio.getPrecio(), subtotal, false, varianteId));
        }
        return resultado;
    }

    private List<ListonCalculadoDto> calcularListones(List<ListonSeleccionadoDto> listones) {
        List<ListonCalculadoDto> resultado = new ArrayList<>();
        if (listones == null || listones.isEmpty()) {
            return resultado;
        }
        for (ListonSeleccionadoDto sel : listones) {
            if (sel.getFraseListonPredefinidaId() != null) {
                FraseListonPredefinida frase = iFraseListonPredefinidaRepository.findById(sel.getFraseListonPredefinidaId())
                        .orElseThrow(() -> new ExceptionDataNotFound("Frase de liston no encontrada: " + sel.getFraseListonPredefinidaId()));
                if (!Boolean.TRUE.equals(frase.getActivo())) {
                    throw new RuntimeException("La frase '" + frase.getTexto() + "' ya no esta disponible");
                }
                Integer varianteId = frase.getVariante() != null ? frase.getVariante().getId() : null;
                resultado.add(new ListonCalculadoDto(frase.getTexto(), "PREDEFINIDA", frase.getPrecio(), varianteId));
            } else if (sel.getFraseListonPersonalizada() != null && !sel.getFraseListonPersonalizada().isBlank()) {
                resultado.add(new ListonCalculadoDto(sel.getFraseListonPersonalizada(), "PERSONALIZADA_PENDIENTE", null, null));
            } else {
                throw new RuntimeException("Cada liston debe traer una frase predefinida o una frase personalizada");
            }
        }
        return resultado;
    }

    private double calcularEnvio(CalcularPrecioRequestDto dto, CalcularPrecioResponseDto response) {
        if (Boolean.TRUE.equals(dto.getRecogerEnLocal())) {
            response.setRecogerEnLocal(true);
            response.setCostoEnvio(0.0);
            return 0.0;
        }
        if (dto.getLugarEntregaId() == null) {
            response.setRecogerEnLocal(false);
            response.setCostoEnvio(null);
            return 0.0;
        }
        LugarEntrega lugar = iLugarEntregaRepository.findById(dto.getLugarEntregaId())
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + dto.getLugarEntregaId()));
        double costo = lugar.getCostoEnvio() != null ? lugar.getCostoEnvio() : 0.0;
        response.setRecogerEnLocal(false);
        response.setCostoEnvio(costo);
        response.setEnvioVarianteId(costo > 0 && lugar.getVariante() != null ? lugar.getVariante().getId() : null);
        return costo;
    }
}
