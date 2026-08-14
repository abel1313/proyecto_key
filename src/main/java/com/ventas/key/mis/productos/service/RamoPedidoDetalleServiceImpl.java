package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.FraseListonPredefinida;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.RamoArmado;
import com.ventas.key.mis.productos.entity.RamoPedidoDetalle;
import com.ventas.key.mis.productos.entity.RamoPedidoDetalleColor;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.floreseternas.AnticipacionResultadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.FrasePendienteDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleColorDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleValidarFraseRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RevalidarPagoResponseDto;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IFraseListonPredefinidaRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IRamoArmadoRepository;
import com.ventas.key.mis.productos.repository.IRamoPedidoDetalleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// "Ticket de produccion" de un ramo -- ver comentario en RamoPedidoDetalle. Se adjunta a un
// Pedido que YA fue creado por el flujo normal (POST /v1/pedidos/savePedido, con las lineas de
// flores/papel/accesorios/liston/envio ya incluidas en detalles usando los varianteId que
// devuelve /v1/flores/calcular-precio) -- este service no crea ni modifica ninguna linea de
// venta, solo guarda la informacion de produccion/contacto que no tiene lugar en DetallePedido.
@Service
public class RamoPedidoDetalleServiceImpl {

    private final IRamoPedidoDetalleRepository iRamoPedidoDetalleRepository;
    private final IPedidoRepository iPedidoRepository;
    private final IColorFlorRepository iColorFlorRepository;
    private final IRamoArmadoRepository iRamoArmadoRepository;
    private final IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository;
    private final ILugarEntregaRepository iLugarEntregaRepository;
    private final ICantidadFlorValidaRepository iCantidadFlorValidaRepository;
    private final ProductoSombraServiceImpl productoSombraService;
    private final FlorPedidoServiceImpl florPedidoService;

    public RamoPedidoDetalleServiceImpl(IRamoPedidoDetalleRepository iRamoPedidoDetalleRepository,
                                         IPedidoRepository iPedidoRepository,
                                         IColorFlorRepository iColorFlorRepository,
                                         IRamoArmadoRepository iRamoArmadoRepository,
                                         IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository,
                                         ILugarEntregaRepository iLugarEntregaRepository,
                                         ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                         ProductoSombraServiceImpl productoSombraService,
                                         FlorPedidoServiceImpl florPedidoService) {
        this.iRamoPedidoDetalleRepository = iRamoPedidoDetalleRepository;
        this.iPedidoRepository = iPedidoRepository;
        this.iColorFlorRepository = iColorFlorRepository;
        this.iRamoArmadoRepository = iRamoArmadoRepository;
        this.iFraseListonPredefinidaRepository = iFraseListonPredefinidaRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.productoSombraService = productoSombraService;
        this.florPedidoService = florPedidoService;
    }

    @Transactional
    public RamoPedidoDetalleResponseDto adjuntar(Integer pedidoId, RamoPedidoDetalleRequestDto dto) {
        if (dto.getColores() == null || dto.getColores().isEmpty()) {
            throw new RuntimeException("Debe indicar al menos un color y su cantidad");
        }
        Pedido pedido = iPedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ExceptionDataNotFound("Pedido no encontrado: " + pedidoId));

        RamoPedidoDetalle detalle = new RamoPedidoDetalle();
        detalle.setPedido(pedido);
        detalle.setFechaCreacion(LocalDateTime.now());
        detalle.setAnticipoPagado(false);

        List<RamoPedidoDetalleColor> colores = new ArrayList<>();
        int cantidadFinal = 0;
        TipoFlor especie = null;
        for (ColorSeleccionadoDto sel : dto.getColores()) {
            if (sel.getCantidad() == null || sel.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad de cada color debe ser mayor a cero");
            }
            ColorFlor color = iColorFlorRepository.findById(sel.getColorFlorId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Color de flor no encontrado: " + sel.getColorFlorId()));
            if (especie == null) {
                especie = color.getTipoFlor();
            } else if (!especie.getId().equals(color.getTipoFlor().getId())) {
                throw new RuntimeException("Todos los colores del ramo deben ser de la misma especie de flor");
            }
            RamoPedidoDetalleColor lineaColor = new RamoPedidoDetalleColor();
            lineaColor.setRamoPedidoDetalle(detalle);
            lineaColor.setColorFlor(color);
            lineaColor.setCantidad(sel.getCantidad());
            colores.add(lineaColor);
            cantidadFinal += sel.getCantidad();
        }
        detalle.setTipoFlor(especie);
        detalle.setCantidadFinal(cantidadFinal);
        detalle.setColores(colores);

        if (dto.getRamoArmadoId() != null) {
            RamoArmado ramoArmado = iRamoArmadoRepository.findById(dto.getRamoArmadoId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Ramo armado no encontrado: " + dto.getRamoArmadoId()));
            detalle.setRamoArmado(ramoArmado);
        }

        resolverListon(dto, detalle);
        resolverEntrega(dto, detalle);
        validarPedidoApartadoSiEsUrgente(dto, especie, cantidadFinal, pedido);
        resolverFechaLimitePago(dto, detalle, especie, cantidadFinal);

        detalle.setTelefonoContacto(dto.getTelefonoContacto());
        detalle.setCorreoContacto(dto.getCorreoContacto());
        detalle.setComentarioAccesorioNoDisponible(dto.getComentarioAccesorioNoDisponible());

        return toResponseDto(iRamoPedidoDetalleRepository.save(detalle));
    }

    // El 50% de urgencia es un ENGANCHE que sale del total, no un cobro aparte -- por eso NO se
    // crea ningun pedido/linea nueva aqui. El pedido ya tenia que haber nacido con
    // tipoPedido:"APARTADO" en /v1/pedidos/savePedido (el front lo sabe de antemano por
    // requiereAnticipo/montoAnticipoSugerido en la respuesta de calcular-precio) y el 50% se
    // registra como abono via el modulo de credito existente (POST /v1/abonos/{pedidoId}) -- eso
    // lo hace el front, no este metodo. Aqui solo se vuelve a calcular en servidor (nunca se
    // confia en el front) para BLOQUEAR el caso en que el pedido llego como "NORMAL" cuando en
    // realidad requeria anticipo -- evita que un pedido urgente termine cobrado 100% de contado
    // sin haber pasado por el flujo de credito.
    private void validarPedidoApartadoSiEsUrgente(RamoPedidoDetalleRequestDto dto, TipoFlor especie, int cantidadFinal, Pedido pedido) {
        CantidadFlorValida cantidadValida = iCantidadFlorValidaRepository
                .findByTipoFlorIdAndCantidadAndActivoTrue(especie.getId(), cantidadFinal)
                .orElse(null);
        AnticipacionResultadoDto anticipacion = florPedidoService.validarAnticipacionYUrgencia(
                dto.getFechaHoraEntrega(), dto.getLugarEntregaId(), dto.getRecogerEnLocal(), cantidadValida, cantidadFinal);
        if (anticipacion.getPrecioUrgencia() == null) {
            return;
        }
        if (!"APARTADO".equals(pedido.getTipoPedido()) && !"FIADO".equals(pedido.getTipoPedido())) {
            throw new RuntimeException("Este pedido requiere anticipo por ser una entrega urgente (de hoy o "
                    + "mañana) -- debio crearse con tipoPedido \"APARTADO\" y el 50% del total registrado como "
                    + "abono, en vez de cobrarse completo. Corrige el pedido antes de adjuntar el detalle.");
        }
    }

    // Guarda fechaHoraEntrega/esUrgente/fechaLimitePago/cargoUrgenteMonto -- modelo nuevo de
    // config-entrega (dias_normal/dias_urgente/hora_limite_pedido/cargo_urgente en
    // CantidadFlorValida). Con esto guardado, revalidarAntesDePagar() puede chequear despues,
    // en el momento del pago, si ya vencio la hora limite sin depender de que el front vuelva a
    // mandar nada. Mismo redondeo hacia arriba que fechas-disponibles.
    private void resolverFechaLimitePago(RamoPedidoDetalleRequestDto dto, RamoPedidoDetalle detalle, TipoFlor especie, int cantidadFinal) {
        detalle.setFechaHoraEntrega(dto.getFechaHoraEntrega());
        boolean urgente = Boolean.TRUE.equals(dto.getUrgente());
        detalle.setEsUrgente(urgente);
        if (!urgente) {
            return;
        }
        CantidadFlorValida aplicada = iCantidadFlorValidaRepository
                .findActivasPorTipoFlorOrdenadas(especie.getId()).stream()
                .filter(c -> c.getCantidad() >= cantidadFinal)
                .findFirst()
                .orElse(null);
        if (aplicada == null || aplicada.getHoraLimitePedido() == null || aplicada.getCargoUrgente() == null) {
            return;
        }
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate diaLimite = java.time.LocalTime.now().isBefore(aplicada.getHoraLimitePedido()) ? hoy : hoy.plusDays(1);
        detalle.setFechaLimitePago(diaLimite.atTime(aplicada.getHoraLimitePedido()));
        detalle.setCargoUrgenteMonto(aplicada.getCargoUrgente());
    }

    // Se llama justo antes de POST /v1/abonos/{pedidoId} (el front lo hace, no queda automatico
    // aqui -- ver nota de arquitectura en CAMBIOS_FRONT.md sobre por que no se toco el endpoint
    // generico de abonos). Si el pago llega despues de fechaLimitePago, agrega el cargo urgente
    // como linea real al pedido (recotiza) en vez de cancelar el pedido -- decision del dueno.
    //
    // El front no tiene forma de saber si un pedido es de flores antes de llamar esto (la pantalla
    // de abonos es generica para todo el sistema), asi que un pedido SIN RamoPedidoDetalle no es un
    // error -- responde 200 con cargoRecienAplicado:false igual que "no aplica urgencia todavia",
    // para que puedan llamarlo siempre sin ensuciar la consola con 400s esperados. Pedido inexistente
    // si sigue siendo error real.
    @Transactional
    public RevalidarPagoResponseDto revalidarAntesDePagar(Integer pedidoId) {
        Optional<RamoPedidoDetalle> detalleOpt = iRamoPedidoDetalleRepository.findByPedidoId(pedidoId).stream().findFirst();
        if (detalleOpt.isEmpty()) {
            Pedido pedidoSinRamo = iPedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new ExceptionDataNotFound("Pedido no encontrado: " + pedidoId));
            return new RevalidarPagoResponseDto(false, null, pedidoSinRamo.getTotalPedido(), null);
        }
        RamoPedidoDetalle detalle = detalleOpt.get();
        Pedido pedido = detalle.getPedido();

        if (!Boolean.TRUE.equals(detalle.getEsUrgente()) || detalle.getFechaLimitePago() == null) {
            return new RevalidarPagoResponseDto(
                    false, null, pedido.getTotalPedido(), null);
        }
        if (Boolean.TRUE.equals(detalle.getCargoUrgenteAplicado())) {
            return new RevalidarPagoResponseDto(
                    false, null, pedido.getTotalPedido(), null);
        }
        if (LocalDateTime.now().isBefore(detalle.getFechaLimitePago())) {
            return new RevalidarPagoResponseDto(
                    false, null, pedido.getTotalPedido(), null);
        }

        double cargo = detalle.getCargoUrgenteMonto() != null ? detalle.getCargoUrgenteMonto() : 0.0;
        Variantes variante = productoSombraService.crear(
                "Cargo por entrega urgente (pago fuera de tiempo) - pedido #" + pedido.getId(), cargo, 0.0, 1);
        DetallePedido linea = new DetallePedido();
        linea.setPedido(pedido);
        linea.setProducto(variante.getProducto());
        linea.setVariante(variante);
        linea.setCantidad(1);
        linea.setPrecioUnitario(cargo);
        linea.setSubTotal(cargo);
        pedido.getDetalles().add(linea);
        pedido.setTotalPedido(pedido.getTotalPedido() + cargo);
        iPedidoRepository.save(pedido);

        detalle.setCargoUrgenteAplicado(true);
        iRamoPedidoDetalleRepository.save(detalle);

        String mensaje = "Tu pago llego despues de la hora limite para el precio normal, asi que se "
                + "aplico el cargo por entrega urgente ($" + cargo + "). Tu nuevo total es $"
                + pedido.getTotalPedido() + " -- vuelve a intentar el pago con ese monto.";
        return new RevalidarPagoResponseDto(
                true, cargo, pedido.getTotalPedido(), mensaje);
    }

    // El anticipo NO se conoce ni se cobra en este paso -- todavia no hay precio de la frase.
    // Solo queda marcada como pendiente; el monto real se calcula en validarFrase() (abajo),
    // que es cuando de verdad existe algo que cobrar.
    private void resolverListon(RamoPedidoDetalleRequestDto dto, RamoPedidoDetalle detalle) {
        if (dto.getFraseListonPredefinidaId() != null) {
            FraseListonPredefinida frase = iFraseListonPredefinidaRepository.findById(dto.getFraseListonPredefinidaId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Frase de liston no encontrada: " + dto.getFraseListonPredefinidaId()));
            detalle.setFraseListonPredefinida(frase);
            detalle.setFraseListonEstado("VALIDADA");
            detalle.setFraseListonPrecioAsignado(frase.getPrecio());
            detalle.setAnticipoRequerido(false);
        } else if (dto.getFraseListonPersonalizada() != null && !dto.getFraseListonPersonalizada().isBlank()) {
            detalle.setFraseListonPersonalizada(dto.getFraseListonPersonalizada());
            detalle.setFraseListonEstado("PENDIENTE_VALIDACION");
            detalle.setAnticipoRequerido(true);
        } else {
            detalle.setFraseListonEstado("NO_APLICA");
            detalle.setAnticipoRequerido(false);
        }
    }

    private void resolverEntrega(RamoPedidoDetalleRequestDto dto, RamoPedidoDetalle detalle) {
        if (Boolean.TRUE.equals(dto.getRecogerEnLocal())) {
            detalle.setRecogerEnLocal(true);
            return;
        }
        detalle.setRecogerEnLocal(false);
        if (dto.getLugarEntregaId() != null) {
            LugarEntrega lugar = iLugarEntregaRepository.findById(dto.getLugarEntregaId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Lugar de entrega no encontrado: " + dto.getLugarEntregaId()));
            detalle.setLugarEntrega(lugar);
        }
    }

    @Transactional
    public RamoPedidoDetalleResponseDto validarFrase(Integer id, RamoPedidoDetalleValidarFraseRequestDto dto) {
        RamoPedidoDetalle detalle = iRamoPedidoDetalleRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Detalle de ramo no encontrado: " + id));
        if (!"PENDIENTE_VALIDACION".equals(detalle.getFraseListonEstado())) {
            throw new RuntimeException("Esta frase no esta pendiente de validar (estado actual: " + detalle.getFraseListonEstado() + ")");
        }
        if (Boolean.TRUE.equals(dto.getAprobar())) {
            if (dto.getPrecioAsignado() == null || dto.getPrecioAsignado() <= 0) {
                throw new RuntimeException("El precio asignado es obligatorio para aprobar la frase");
            }
            detalle.setFraseListonEstado("VALIDADA");
            detalle.setFraseListonPrecioAsignado(dto.getPrecioAsignado());
            detalle.setMontoAnticipo(dto.getPrecioAsignado() * FloresEternasConstantes.PORCENTAJE_ANTICIPO);
            detalle.setPedidoAnticipo(crearPedidoAnticipoFrase(detalle, dto.getPrecioAsignado()));
        } else {
            detalle.setFraseListonEstado("RECHAZADA");
        }
        if (dto.getAnticipoPagado() != null) {
            detalle.setAnticipoPagado(dto.getAnticipoPagado());
        }
        return toResponseDto(iRamoPedidoDetalleRepository.save(detalle));
    }

    // Crea un Pedido APARTADO nuevo, separado del pedido original, solo para esta frase -- asi el
    // front puede registrar el anticipo del 50% reutilizando el modulo de abonos existente
    // (POST /v1/abonos/{pedidoId}) sin depender de como haya nacido el pedido original (que puede
    // ser NORMAL o el mismo ya APARTADO por urgencia -- ver validarPedidoApartadoSiEsUrgente). El
    // pedido original no se toca. Este es el UNICO momento en que existe un monto de anticipo real
    // para la frase -- antes de esto no se cobra ni se sugiere nada.
    private Pedido crearPedidoAnticipoFrase(RamoPedidoDetalle detalle, double precioAsignado) {
        Pedido original = detalle.getPedido();
        String nombreFrase = detalle.getFraseListonPersonalizada();

        Variantes variante = productoSombraService.crear(
                "Frase personalizada: \"" + nombreFrase + "\"", precioAsignado, 0.0, 1);

        Pedido pedidoAnticipo = new Pedido();
        pedidoAnticipo.setCliente(original.getCliente());
        pedidoAnticipo.setEstadoPedido("Pendiente");
        pedidoAnticipo.setFechaPedido(LocalDateTime.now().toLocalDate());
        pedidoAnticipo.setFechaHoraRegistro(LocalDateTime.now());
        // fechaRecogida se deja null a proposito: el scheduler de cancelacion automatica
        // (PedidoCancelacionScheduler) solo actua sobre pedidos con fechaRecogida vencida.
        pedidoAnticipo.setObservaciones("Cobro de frase de liston personalizada del pedido #"
                + original.getId() + ": \"" + nombreFrase + "\"");
        pedidoAnticipo.setTipoPedido("APARTADO");
        pedidoAnticipo.setTotalPagado(0.0);

        DetallePedido lineaFrase = new DetallePedido();
        lineaFrase.setPedido(pedidoAnticipo);
        lineaFrase.setProducto(variante.getProducto());
        lineaFrase.setVariante(variante);
        lineaFrase.setCantidad(1);
        lineaFrase.setPrecioUnitario(precioAsignado);
        lineaFrase.setSubTotal(precioAsignado);

        List<DetallePedido> detalles = new ArrayList<>();
        detalles.add(lineaFrase);
        pedidoAnticipo.setDetalles(detalles);
        pedidoAnticipo.setTotalPedido(precioAsignado);

        return iPedidoRepository.save(pedidoAnticipo);
    }

    public List<RamoPedidoDetalleResponseDto> listarPorPedido(Integer pedidoId) {
        return iRamoPedidoDetalleRepository.findByPedidoId(pedidoId).stream().map(this::toResponseDto).toList();
    }

    public PginaDto<List<FrasePendienteDto>> listarFrasesPendientes(int pagina, int size) {
        Page<RamoPedidoDetalle> page = iRamoPedidoDetalleRepository.findFrasesPendientes(PageRequest.of(pagina - 1, size));
        PginaDto<List<FrasePendienteDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(this::toFrasePendienteDto).toList());
        return resultado;
    }

    private FrasePendienteDto toFrasePendienteDto(RamoPedidoDetalle detalle) {
        Pedido pedido = detalle.getPedido();
        String clienteNombre;
        if (pedido.getCliente() != null) {
            clienteNombre = pedido.getCliente().getNombrePersona();
        } else if (pedido.getClienteSinRegistro() != null) {
            clienteNombre = pedido.getClienteSinRegistro().getNombrePersona();
        } else {
            clienteNombre = null;
        }
        return new FrasePendienteDto(
                detalle.getId(),
                pedido.getId(),
                detalle.getFraseListonPersonalizada(),
                clienteNombre,
                pedido.getFechaPedido());
    }

    private RamoPedidoDetalleResponseDto toResponseDto(RamoPedidoDetalle detalle) {
        String fraseTexto = detalle.getFraseListonPredefinida() != null
                ? detalle.getFraseListonPredefinida().getTexto()
                : detalle.getFraseListonPersonalizada();
        List<RamoPedidoDetalleColorDto> colores = detalle.getColores() == null ? List.of()
                : detalle.getColores().stream()
                        .map(c -> new RamoPedidoDetalleColorDto(c.getColorFlor().getId(), c.getColorFlor().getNombre(), c.getCantidad()))
                        .toList();
        return new RamoPedidoDetalleResponseDto(
                detalle.getId(),
                detalle.getPedido().getId(),
                detalle.getRamoArmado() != null ? detalle.getRamoArmado().getId() : null,
                detalle.getTipoFlor().getId(),
                detalle.getTipoFlor().getNombre(),
                detalle.getCantidadFinal(),
                colores,
                fraseTexto,
                detalle.getFraseListonEstado(),
                detalle.getFraseListonPrecioAsignado(),
                detalle.getAnticipoRequerido(),
                detalle.getAnticipoPagado(),
                detalle.getMontoAnticipo(),
                detalle.getLugarEntrega() != null ? detalle.getLugarEntrega().getId() : null,
                detalle.getLugarEntrega() != null ? detalle.getLugarEntrega().getNombre() : null,
                detalle.getRecogerEnLocal(),
                detalle.getTelefonoContacto(),
                detalle.getCorreoContacto(),
                detalle.getComentarioAccesorioNoDisponible(),
                detalle.getFechaCreacion(),
                detalle.getPedidoAnticipo() != null ? detalle.getPedidoAnticipo().getId() : null);
    }
}
