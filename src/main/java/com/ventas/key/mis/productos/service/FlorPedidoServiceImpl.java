package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.ListonCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ListonSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadResponseDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IFraseListonPredefinidaRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Motor de calculo de precio de un ramo de flores eternas. No persiste nada todavia -- ver
// PROPUESTA_FLORES_ETERNAS.md, seccion "Diseno tecnico".
@Service
public class FlorPedidoServiceImpl {

    private final ITipoFlorRepository iTipoFlorRepository;
    private final IColorFlorRepository iColorFlorRepository;
    private final ICantidadFlorValidaRepository iCantidadFlorValidaRepository;
    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final AccesorioRamoServiceImpl accesorioRamoService;
    private final IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository;
    private final ILugarEntregaRepository iLugarEntregaRepository;

    public FlorPedidoServiceImpl(ITipoFlorRepository iTipoFlorRepository,
                                  IColorFlorRepository iColorFlorRepository,
                                  ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                  IAccesorioRamoRepository iAccesorioRamoRepository,
                                  AccesorioRamoServiceImpl accesorioRamoService,
                                  IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository,
                                  ILugarEntregaRepository iLugarEntregaRepository) {
        this.iTipoFlorRepository = iTipoFlorRepository;
        this.iColorFlorRepository = iColorFlorRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.accesorioRamoService = accesorioRamoService;
        this.iFraseListonPredefinidaRepository = iFraseListonPredefinidaRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
    }

    // Paso 1 del flujo: el cliente ya eligio la especie (tipo de flor) y escribe cuantas quiere
    // EN TOTAL -- todavia sin elegir color. La validez del circulo es por especie y por el total,
    // sin importar en cuantos colores se vaya a repartir despues.
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

        // Sin catalogo configurado para esta especie: no hay nada contra que validar, se acepta
        // tal cual (venta por unidad). Si SI hay catalogo, toda cantidad que no sea exacta -- este
        // por arriba, en medio o por debajo de la mas chica registrada -- pasa por la logica de
        // abajo y se le sugiere la alternativa mas cercana (antes, por debajo de la mas chica se
        // aceptaba en silencio; decision del dueno: avisar siempre que el circulo no completa).
        if (validas.isEmpty()) {
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

    // Paso 2: con la cantidad total ya decidida, el cliente reparte esa cantidad entre uno o
    // varios colores de la MISMA especie y arma el resto del ramo (accesorios, liston, envio).
    @Transactional(readOnly = true)
    public CalcularPrecioResponseDto calcularPrecio(CalcularPrecioRequestDto dto) {
        if (dto.getColores() == null || dto.getColores().isEmpty()) {
            throw new RuntimeException("Debe indicar al menos un color y su cantidad");
        }

        List<ColorCalculadoDto> coloresCalculados = new ArrayList<>();
        int cantidadFinal = 0;
        double precioBase = 0;
        TipoFlor especie = null;
        for (ColorSeleccionadoDto sel : dto.getColores()) {
            if (sel.getCantidad() == null || sel.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad de cada color debe ser mayor a cero");
            }
            ColorFlor color = iColorFlorRepository.findById(sel.getColorFlorId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Color de flor no encontrado: " + sel.getColorFlorId()));
            if (!Boolean.TRUE.equals(color.getActivo())) {
                throw new RuntimeException("El color '" + color.getNombre() + "' no esta disponible actualmente");
            }
            if (especie == null) {
                especie = color.getTipoFlor();
            } else if (!especie.getId().equals(color.getTipoFlor().getId())) {
                throw new RuntimeException("Todos los colores del ramo deben ser de la misma especie de flor");
            }

            int cantidad = sel.getCantidad();
            double subtotal = cantidad * especie.getPrecioPorFlor();
            cantidadFinal += cantidad;
            precioBase += subtotal;
            coloresCalculados.add(new ColorCalculadoDto(
                    color.getId(), color.getNombre(), cantidad, especie.getPrecioPorFlor(), subtotal,
                    color.getVariante() != null ? color.getVariante().getId() : null));
        }

        CalcularPrecioResponseDto response = new CalcularPrecioResponseDto();
        response.setCantidadFinal(cantidadFinal);
        response.setPrecioBase(precioBase);
        response.setColoresCalculados(coloresCalculados);

        Integer papelAccesorioId = aplicarReglaPapel(cantidadFinal, response);

        List<AccesorioCalculadoDto> accesoriosCalculados = calcularAccesorios(dto.getAccesorios(), papelAccesorioId, cantidadFinal);
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
        response.setAvisoFrasePendiente(tienePendiente ? FloresEternasConstantes.AVISO_FRASE_PENDIENTE : null);

        double costoEnvio = calcularEnvio(dto, response);

        double totalConocido = precioBase
                + (Boolean.TRUE.equals(response.getPapelObligatorioAplicado()) ? response.getPrecioPapel() : 0)
                + subtotalAccesorios
                + subtotalListones
                + costoEnvio;
        response.setTotal(totalConocido);

        return response;
    }

    private Integer aplicarReglaPapel(int cantidadFinal, CalcularPrecioResponseDto response) {
        Optional<AccesorioRamo> papel = accesorioRamoService.obtenerPapelAutomaticoSiAplica(cantidadFinal);
        if (papel.isEmpty()) {
            response.setPapelObligatorioAplicado(false);
            return null;
        }
        response.setPapelObligatorioAplicado(true);
        response.setPrecioPapel(accesorioRamoService.calcularPrecioPapel(papel.get(), cantidadFinal));
        response.setPliegosPapel(accesorioRamoService.calcularPliegosPapel(papel.get(), cantidadFinal));
        response.setPrecioUnitarioPapel(papel.get().getPrecio());
        response.setPapelVarianteId(papel.get().getVariante() != null ? papel.get().getVariante().getId() : null);
        return papel.get().getId();
    }

    private List<AccesorioCalculadoDto> calcularAccesorios(List<AccesorioSeleccionadoDto> seleccionados, Integer papelAccesorioId, int cantidadFinal) {
        List<AccesorioCalculadoDto> resultado = new ArrayList<>();
        if (seleccionados == null || seleccionados.isEmpty()) {
            return resultado;
        }
        java.util.Map<Integer, Integer> cantidadPorAccesorio = new java.util.LinkedHashMap<>();
        for (AccesorioSeleccionadoDto sel : seleccionados) {
            // El papel ya se cobro via la regla automatica -- si el cliente tambien lo eligio
            // manualmente en esta lista, se ignora esa entrada para no cobrarlo dos veces.
            if (papelAccesorioId != null && papelAccesorioId.equals(sel.getAccesorioId())) {
                continue;
            }
            cantidadPorAccesorio.merge(sel.getAccesorioId(), 1, Integer::sum);
        }
        for (java.util.Map.Entry<Integer, Integer> entry : cantidadPorAccesorio.entrySet()) {
            AccesorioRamo accesorio = iAccesorioRamoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + entry.getKey()));
            if (!Boolean.TRUE.equals(accesorio.getActivo())) {
                throw new RuntimeException("El accesorio '" + accesorio.getNombre() + "' no esta disponible actualmente");
            }
            Integer varianteId = accesorio.getVariante() != null ? accesorio.getVariante().getId() : null;
            if (Boolean.TRUE.equals(accesorio.getEsPapel())) {
                // Papel elegido a mano (por debajo del umbral automatico): el costo sigue
                // dependiendo de la cantidad total de flores del ramo, no de cuantas veces el
                // cliente lo haya mandado en la lista -- no tiene sentido "elegir cuantos
                // papeles quiero", eso lo calcula el sistema segun floresPorPliego.
                Integer pliegos = accesorioRamoService.calcularPliegosPapel(accesorio, cantidadFinal);
                double subtotal = accesorioRamoService.calcularPrecioPapel(accesorio, cantidadFinal);
                resultado.add(new AccesorioCalculadoDto(
                        accesorio.getId(), accesorio.getNombre(), pliegos != null ? pliegos : 1,
                        accesorio.getPrecio(), subtotal, false, varianteId));
                continue;
            }
            int cantidad = entry.getValue();
            double subtotal = accesorio.getPrecio() * cantidad;
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
