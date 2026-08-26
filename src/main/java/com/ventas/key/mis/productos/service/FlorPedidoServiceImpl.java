package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.AnticipacionResultadoDto;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.ListonCalculadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ListonSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadResponseDto;
import com.ventas.key.mis.productos.models.CalcularCostoEnvioResponse;
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
    private final LugarEntregaAnilloServiceImpl anilloService;

    public FlorPedidoServiceImpl(ITipoFlorRepository iTipoFlorRepository,
                                  IColorFlorRepository iColorFlorRepository,
                                  ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                  IAccesorioRamoRepository iAccesorioRamoRepository,
                                  AccesorioRamoServiceImpl accesorioRamoService,
                                  IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository,
                                  ILugarEntregaRepository iLugarEntregaRepository,
                                  LugarEntregaAnilloServiceImpl anilloService) {
        this.iTipoFlorRepository = iTipoFlorRepository;
        this.iColorFlorRepository = iColorFlorRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.accesorioRamoService = accesorioRamoService;
        this.iFraseListonPredefinidaRepository = iFraseListonPredefinidaRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
        this.anilloService = anilloService;
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

        // Cantidad registrada que coincide EXACTO con especie+cantidadFinal (si existe) -- de ahi
        // salen tanto el pliegos explicito como la mano de obra configurados a mano por el dueno
        // para ESTE tamano de ramo.
        Optional<CantidadFlorValida> cantidadRegistrada = iCantidadFlorValidaRepository
                .findByTipoFlorIdAndCantidadAndActivoTrue(especie.getId(), cantidadFinal);
        Integer pliegosExplicitos = cantidadRegistrada.map(CantidadFlorValida::getPliegos).orElse(null);

        // Mano de obra: se suma directo al total sin aparecer como accesorio -- el cliente no
        // debe poder distinguir cuanto es material y cuanto es trabajo (decision del dueno).
        Double precioManoDeObra = cantidadRegistrada.map(CantidadFlorValida::getManoDeObra).orElse(null);
        response.setPrecioManoDeObra(precioManoDeObra);

        // Anticipacion -- modelo viejo (horasMinimasAnticipacion/precioUrgencia), se queda como
        // red de seguridad pero hoy nadie lo llena: si no da tiempo, entregaValida sale false.
        AnticipacionResultadoDto anticipacion = validarAnticipacionYUrgencia(dto.getFechaHoraEntrega(), dto.getLugarEntregaId(),
                dto.getRecogerEnLocal(), cantidadRegistrada.orElse(null), cantidadFinal);
        response.setEntregaValida(anticipacion.isEntregaValida());
        response.setMensajeEntrega(anticipacion.getMensajeEntrega());

        // Cargo urgente -- modelo nuevo de config-entrega (mismo redondeo hacia arriba que
        // fechas-disponibles). Es el que de verdad se va a usar: el dueno llena diasNormal/
        // diasUrgente/cargoUrgente, no los campos viejos de arriba.
        Double cargoUrgenteNuevoModelo = null;
        if (Boolean.TRUE.equals(dto.getUrgente())) {
            int cantidadFinalFinal = cantidadFinal;
            CantidadFlorValida aplicadaEntrega = iCantidadFlorValidaRepository
                    .findActivasPorTipoFlorOrdenadas(especie.getId()).stream()
                    .filter(c -> c.getCantidad() >= cantidadFinalFinal)
                    .findFirst()
                    .orElse(null);
            if (aplicadaEntrega != null && aplicadaEntrega.getCargoUrgente() != null
                    && aplicadaEntrega.getDiasUrgente() != null && aplicadaEntrega.getHoraLimitePedido() != null) {
                cargoUrgenteNuevoModelo = aplicadaEntrega.getCargoUrgente();
            }
        }

        // El que aplique (nuevo modelo tiene prioridad -- es el vigente; el viejo solo se usa si
        // el nuevo no dio nada, puramente por si algun tamano viejo se quedo con datos sueltos).
        Double precioUrgencia = cargoUrgenteNuevoModelo != null ? cargoUrgenteNuevoModelo : anticipacion.getPrecioUrgencia();
        response.setPrecioUrgencia(precioUrgencia);
        response.setRequiereAnticipo(precioUrgencia != null);

        Integer papelAccesorioId = aplicarReglaPapel(cantidadFinal, pliegosExplicitos, response);

        List<AccesorioCalculadoDto> accesoriosCalculados = calcularAccesorios(dto.getAccesorios(), papelAccesorioId, cantidadFinal, pliegosExplicitos);
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
                + costoEnvio
                + (precioManoDeObra != null ? precioManoDeObra : 0)
                + (precioUrgencia != null ? precioUrgencia : 0);
        response.setTotal(totalConocido);

        // 50% de ENGANCHE (sale del total, no es un cobro aparte) -- el pedido debe crearse con
        // tipoPedido:"APARTADO" en vez de "NORMAL" y este monto se registra como abono inicial
        // (POST /v1/abonos/{pedidoId}), reusando el modulo de credito existente.
        if (Boolean.TRUE.equals(response.getRequiereAnticipo())) {
            response.setMontoAnticipoSugerido(totalConocido * FloresEternasConstantes.PORCENTAJE_ANTICIPO);
        }

        return response;
    }

    private Integer aplicarReglaPapel(int cantidadFinal, Integer pliegosExplicitos, CalcularPrecioResponseDto response) {
        Optional<AccesorioRamo> papel = accesorioRamoService.obtenerPapelAutomaticoSiAplica(cantidadFinal);
        if (papel.isEmpty()) {
            response.setPapelObligatorioAplicado(false);
            return null;
        }
        response.setPapelObligatorioAplicado(true);
        response.setPrecioPapel(accesorioRamoService.calcularPrecioPapel(papel.get(), cantidadFinal, pliegosExplicitos));
        response.setPliegosPapel(accesorioRamoService.calcularPliegosPapel(papel.get(), cantidadFinal, pliegosExplicitos));
        response.setPrecioUnitarioPapel(papel.get().getPrecio());
        response.setPapelVarianteId(papel.get().getVariante() != null ? papel.get().getVariante().getId() : null);
        return papel.get().getId();
    }

    private List<AccesorioCalculadoDto> calcularAccesorios(List<AccesorioSeleccionadoDto> seleccionados, Integer papelAccesorioId, int cantidadFinal, Integer pliegosExplicitos) {
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
                Integer pliegos = accesorioRamoService.calcularPliegosPapel(accesorio, cantidadFinal, pliegosExplicitos);
                double subtotal = accesorioRamoService.calcularPrecioPapel(accesorio, cantidadFinal, pliegosExplicitos);
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
            response.setEnvioDentroDeRango(true);
            return 0.0;
        }
        if (dto.getLugarEntregaId() == null) {
            response.setRecogerEnLocal(false);
            response.setCostoEnvio(null);
            response.setEnvioDentroDeRango(true);
            return 0.0;
        }
        // Confirma que la zona existe (mismo comportamiento de siempre) -- el costo real se
        // resuelve abajo via anilloService, que ya sabe caer al costoEnvio fijo de esta misma
        // entidad cuando la zona no tiene anillos configurados.
        iLugarEntregaRepository.findById(dto.getLugarEntregaId())
                .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + dto.getLugarEntregaId()));
        response.setRecogerEnLocal(false);

        // Zona con anillos de cobro por distancia (ver DISENO_ZONAS_POR_ANILLO.md) pero el
        // cliente todavia no marca el punto en el mapa -- no se puede cobrar todavia, pero
        // tampoco es un error: sin punto, no se bloquea con envioDentroDeRango=false.
        if (anilloService.tieneAnillos(dto.getLugarEntregaId()) && (dto.getLatitud() == null || dto.getLongitud() == null)) {
            response.setCostoEnvio(null);
            response.setEnvioVarianteId(null);
            response.setEnvioDentroDeRango(true);
            return 0.0;
        }

        CalcularCostoEnvioResponse r = anilloService.calcularCosto(dto.getLugarEntregaId(), dto.getLatitud(), dto.getLongitud());
        response.setEnvioDentroDeRango(r.isDentroDeRango());
        if (!r.isDentroDeRango()) {
            response.setCostoEnvio(null);
            response.setEnvioVarianteId(null);
            response.setMensajeEnvio("El punto que marcaste está fuera de las zonas de entrega configuradas -- marca un punto más cercano.");
            return 0.0;
        }
        double costo = r.getCostoEnvio() != null ? r.getCostoEnvio() : 0.0;
        response.setCostoEnvio(costo);
        response.setEnvioVarianteId(r.getVarianteId());
        return costo;
    }

    // Sin fechaHoraEntrega en el request, o sin horasMinimasAnticipacion configurada para esta
    // cantidad, no valida nada -- comportamiento de siempre (entregaValida=true, sin urgencia).
    // Con ambas presentes:
    // 1. El minimo requerido es horasMinimasAnticipacion + el extra de la zona de entrega
    //    (LugarEntrega.horasExtraAnticipacion, 0 si recogen en local o no hay lugar).
    // 2. Si el margen disponible (fechaHoraEntrega - ahora) no alcanza el minimo, entregaValida
    //    sale false con un mensaje explicando cuanto hace falta -- el pedido NO se puede generar,
    //    pero ya NO se lanza excepcion (a peticion del front, para poder mostrarle el motivo real
    //    al cliente en vez de un error generico de la llamada completa).
    // 3. Si alcanza, es "urgente" -- y por lo tanto se cobra precioUrgencia -- unicamente cuando
    //    la entrega cae en el dia de hoy o manana (la definicion exacta que dio el dueno: "de un
    //    dia para otro"). Con dos o mas dias de anticipacion, sin cargo. YA NO es una ventana
    //    proporcional al minimo (el x2 que se habia inventado -- descartado, el dueno no lo
    //    reconocio como su regla).
    // Parametros sueltos (no el DTO completo) para que RamoPedidoDetalleServiceImpl tambien la
    // pueda usar -- ahi se vuelve a evaluar en servidor si el pedido requiere el anticipo del 50%
    // (nunca se confia en un valor que mande el front).
    public AnticipacionResultadoDto validarAnticipacionYUrgencia(java.time.LocalDateTime fechaHoraEntrega, Integer lugarEntregaId,
                                                                   Boolean recogerEnLocal, CantidadFlorValida cantidadValida, int cantidadFinal) {
        if (fechaHoraEntrega == null || cantidadValida == null || cantidadValida.getHorasMinimasAnticipacion() == null) {
            return new AnticipacionResultadoDto(true, null, null);
        }
        int horasExtraZona = 0;
        if (!Boolean.TRUE.equals(recogerEnLocal) && lugarEntregaId != null) {
            LugarEntrega lugar = iLugarEntregaRepository.findById(lugarEntregaId)
                    .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + lugarEntregaId));
            horasExtraZona = lugar.getHorasExtraAnticipacion() != null ? lugar.getHorasExtraAnticipacion() : 0;
        }
        int horasRequeridas = cantidadValida.getHorasMinimasAnticipacion() + horasExtraZona;
        long horasDisponibles = java.time.Duration.between(java.time.LocalDateTime.now(), fechaHoraEntrega).toHours();
        if (horasDisponibles < horasRequeridas) {
            String mensaje = "No alcanzamos a preparar un ramo de " + cantidadFinal
                    + " flores para la fecha y hora de entrega solicitadas -- se necesitan al menos "
                    + horasRequeridas + " horas de anticipacion (contando la zona de entrega). "
                    + "Elige una fecha/hora mas adelante o reduce la cantidad de flores.";
            return new AnticipacionResultadoDto(false, mensaje, null);
        }
        boolean esDeUnDiaParaOtro = !fechaHoraEntrega.toLocalDate().isAfter(java.time.LocalDate.now().plusDays(1));
        Double precioUrgencia = esDeUnDiaParaOtro ? cantidadValida.getPrecioUrgencia() : null;
        return new AnticipacionResultadoDto(true, null, precioUrgencia);
    }

    // El calendario del cliente: en vez de que el front proponga una fecha y el back la valide,
    // el back dice directamente que fechas se pueden. Ver CAMBIOS_FRONT.md, seccion "config
    // entrega" -- construido sobre los campos nuevos de CantidadFlorValida (diasNormal,
    // horaEntregaNormal, diasUrgente, horaEntregaUrgente, horaLimitePedido, cargoUrgente).
    @Transactional(readOnly = true)
    public FechasDisponiblesResponseDto fechasDisponibles(FechasDisponiblesRequestDto dto) {
        if (dto.getTipoFlorId() == null || dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new RuntimeException("tipoFlorId y cantidad son obligatorios");
        }
        FechasDisponiblesResponseDto response = new FechasDisponiblesResponseDto();

        // Redondeo hacia arriba: el tamano configurado mas chico que sea >= lo pedido (nunca uno
        // menor -- un ramo mas grande da mas trabajo, aplicarle un plazo mas corto comprometeria
        // al taller a entregar mas rapido de lo que en realidad puede). Confirmado por el dueno.
        List<CantidadFlorValida> registradas = iCantidadFlorValidaRepository
                .findActivasPorTipoFlorOrdenadas(dto.getTipoFlorId());
        CantidadFlorValida aplicada = registradas.stream()
                .filter(c -> c.getCantidad() >= dto.getCantidad())
                .findFirst()
                .orElse(null);

        if (aplicada == null) {
            response.setMensaje("No tenemos configurado ningun tamano de ramo igual o mayor a "
                    + dto.getCantidad() + " flores para esta especie. Comunicate con el "
                    + "administrador por WhatsApp o redes sociales para revisar tu pedido.");
            response.setOfreceUrgente(false);
            return response;
        }
        response.setCantidadAplicada(aplicada.getCantidad());

        boolean ofreceUrgente = aplicada.getDiasUrgente() != null
                && aplicada.getHoraEntregaUrgente() != null
                && aplicada.getHoraLimitePedido() != null;
        response.setOfreceUrgente(ofreceUrgente);

        boolean urgente = Boolean.TRUE.equals(dto.getUrgente());
        if (urgente && !ofreceUrgente) {
            response.setMensaje("Este tamano de ramo (" + aplicada.getCantidad()
                    + " flores) no se puede entregar de un dia para otro.");
            return response;
        }
        if (!urgente && (aplicada.getDiasNormal() == null || aplicada.getHoraEntregaNormal() == null)) {
            response.setMensaje("Este tamano de ramo (" + aplicada.getCantidad()
                    + " flores) todavia no tiene un plazo de entrega configurado. "
                    + "Comunicate con el administrador por WhatsApp o redes sociales.");
            return response;
        }

        int horasExtraZona = 0;
        if (dto.getLugarEntregaId() != null) {
            LugarEntrega lugar = iLugarEntregaRepository.findById(dto.getLugarEntregaId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + dto.getLugarEntregaId()));
            horasExtraZona = lugar.getHorasExtraAnticipacion() != null ? lugar.getHorasExtraAnticipacion() : 0;
        }

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDateTime primeraFechaValida;
        Double cargo = null;

        if (urgente) {
            // Si ya paso la hora limite de hoy, el plazo urgente se corre a partir de manana --
            // el dia de hoy "ya no cuenta" (confirmado por el dueno con el ejemplo de la hora de
            // corte moviendo el calendario, no solo la fecha).
            java.time.LocalDate diaInicio = java.time.LocalTime.now().isBefore(aplicada.getHoraLimitePedido())
                    ? hoy : hoy.plusDays(1);
            primeraFechaValida = diaInicio.plusDays(aplicada.getDiasUrgente()).atTime(aplicada.getHoraEntregaUrgente());
            cargo = aplicada.getCargoUrgente();
        } else {
            primeraFechaValida = hoy.plusDays(aplicada.getDiasNormal()).atTime(aplicada.getHoraEntregaNormal());
        }
        primeraFechaValida = primeraFechaValida.plusHours(horasExtraZona);

        response.setPrimeraFechaValida(primeraFechaValida);
        response.setHorasDisponibles(List.of(primeraFechaValida.toLocalTime().toString()));
        response.setCargoUrgencia(cargo);
        return response;
    }
}
