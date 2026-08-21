package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
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
import com.ventas.key.mis.productos.models.floreseternas.AccesorioSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.AccesorioSeleccionadoRamoDto;
import com.ventas.key.mis.productos.models.floreseternas.AnticipacionResultadoDto;
import com.ventas.key.mis.productos.models.floreseternas.ColorSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.EditarRamoRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.EditarRamoResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.FrasePendienteDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleColorDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleValidarFraseRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RevalidarPagoResponseDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final ProductoSombraServiceImpl productoSombraService;
    private final FlorPedidoServiceImpl florPedidoService;
    private final AccesorioRamoServiceImpl accesorioRamoService;
    private final EmailService emailService;
    private final com.ventas.key.mis.productos.service.api.IPedidoService pedidoService;

    @org.springframework.beans.factory.annotation.Value("${chat.admin-email:admin@novedades-jade.com.mx}")
    private String adminEmail;

    public RamoPedidoDetalleServiceImpl(IRamoPedidoDetalleRepository iRamoPedidoDetalleRepository,
                                         IPedidoRepository iPedidoRepository,
                                         IColorFlorRepository iColorFlorRepository,
                                         IRamoArmadoRepository iRamoArmadoRepository,
                                         IFraseListonPredefinidaRepository iFraseListonPredefinidaRepository,
                                         ILugarEntregaRepository iLugarEntregaRepository,
                                         ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                         IAccesorioRamoRepository iAccesorioRamoRepository,
                                         ProductoSombraServiceImpl productoSombraService,
                                         FlorPedidoServiceImpl florPedidoService,
                                         AccesorioRamoServiceImpl accesorioRamoService,
                                         EmailService emailService,
                                         com.ventas.key.mis.productos.service.api.IPedidoService pedidoService) {
        this.iRamoPedidoDetalleRepository = iRamoPedidoDetalleRepository;
        this.iPedidoRepository = iPedidoRepository;
        this.iColorFlorRepository = iColorFlorRepository;
        this.iRamoArmadoRepository = iRamoArmadoRepository;
        this.iFraseListonPredefinidaRepository = iFraseListonPredefinidaRepository;
        this.iLugarEntregaRepository = iLugarEntregaRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.productoSombraService = productoSombraService;
        this.florPedidoService = florPedidoService;
        this.accesorioRamoService = accesorioRamoService;
        this.emailService = emailService;
        this.pedidoService = pedidoService;
    }

    // Para el chequeo de dueno en el controller (RamoPedidoDetalleController.cancelarPropio) --
    // antes de dejar cancelar, hay que saber de quien es el pedido sin exponer el resto del
    // detalle. Null si el pedido no tiene cliente registrado (venta de mostrador).
    public Integer obtenerClienteIdPropietario(Integer pedidoId) {
        RamoPedidoDetalle detalle = iRamoPedidoDetalleRepository.findByPedidoId(pedidoId).stream().findFirst()
                .orElseThrow(() -> new ExceptionDataNotFound("Este pedido no tiene un ramo de flores asociado: " + pedidoId));
        Pedido pedido = detalle.getPedido();
        return pedido.getCliente() != null ? pedido.getCliente().getId() : null;
    }

    // El cliente cancela su propio pedido de flores -- SOLO antes de pagar el anticipo
    // (pedido.totalPagado en 0), pedido explicito del dueno (2026-08-17): despues de eso ya hay
    // dinero de por medio y solo el admin puede cancelar (DELETE /v1/pedidos/delete/{id}). El
    // chequeo de que quien llama es realmente el dueno del pedido va en el controller, antes de
    // llamar aqui -- este metodo asume que ya se valido.
    @Transactional
    public void cancelarPropio(Integer pedidoId) {
        RamoPedidoDetalle detalle = iRamoPedidoDetalleRepository.findByPedidoId(pedidoId).stream().findFirst()
                .orElseThrow(() -> new ExceptionDataNotFound("Este pedido no tiene un ramo de flores asociado: " + pedidoId));
        Pedido pedido = detalle.getPedido();
        double totalPagado = pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0;
        if (totalPagado > 0) {
            throw new RuntimeException("Ya se registro un pago para este pedido -- no puedes cancelarlo tu mismo, "
                    + "contacta al administrador.");
        }
        pedidoService.deletePedidoById(pedidoId, "CANCELADO_POR_CLIENTE");
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

        RamoPedidoDetalle guardado = iRamoPedidoDetalleRepository.save(detalle);

        // Aviso al dueno: sin esto nadie sabe que hay una frase nueva esperando precio salvo que
        // entre a la bandeja por su cuenta -- pedido explicito del dueno (2026-08-14). No bloquea
        // el guardado del pedido si el correo falla (enviarTicket ya traga la excepcion).
        if ("PENDIENTE_VALIDACION".equals(guardado.getFraseListonEstado())) {
            String asunto = "Frase nueva por aprobar - pedido #" + pedido.getId();
            String html = "<p>Un cliente pidio una frase personalizada de liston nueva:</p>"
                    + "<h3>\"" + guardado.getFraseListonPersonalizada() + "\"</h3>"
                    + "<p>Pedido #" + pedido.getId() + ". Entra a la bandeja de frases pendientes "
                    + "para ponerle precio y aprobarla o rechazarla.</p>";
            emailService.enviarTicket(adminEmail, asunto, html);
        }

        return toResponseDto(guardado);
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
        aplicarFechaEntrega(detalle, dto.getFechaHoraEntrega(), Boolean.TRUE.equals(dto.getUrgente()), especie, cantidadFinal);
    }

    // Compartido entre adjuntar() y editarRamo() -- calcula fechaLimitePago/cargoUrgenteMonto para
    // el tamano (redondeo hacia arriba, mismo criterio que fechasDisponibles()) que aplique a
    // cantidadFinal. Deja fechaLimitePago/cargoUrgenteMonto en null si el tamano no tiene
    // cargo_urgente configurado -- revalidarAntesDePagar() ya sabe tratar eso como "no aplica".
    private void aplicarFechaEntrega(RamoPedidoDetalle detalle, LocalDateTime fechaHoraEntrega, boolean urgente, TipoFlor especie, int cantidadFinal) {
        detalle.setFechaHoraEntrega(fechaHoraEntrega);
        detalle.setEsUrgente(urgente);
        detalle.setFechaLimitePago(null);
        detalle.setCargoUrgenteMonto(null);
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
        RamoPedidoDetalle guardado = iRamoPedidoDetalleRepository.save(detalle);

        // Aviso al cliente: se fue con un total provisional, sin esto no sabe que ya puede pagar
        // el anticipo de su frase -- mismo pedido explicito del dueno que el aviso al admin de
        // arriba. Solo en aprobacion (el rechazo no tiene monto que cobrar).
        if (Boolean.TRUE.equals(dto.getAprobar()) && guardado.getCorreoContacto() != null) {
            String asunto = "Tu frase de liston ya tiene precio - pedido #" + guardado.getPedido().getId();
            String html = "<p>Tu frase personalizada de liston ya fue revisada:</p>"
                    + "<h3>\"" + guardado.getFraseListonPersonalizada() + "\"</h3>"
                    + "<p>Precio: $" + guardado.getFraseListonPrecioAsignado() + "</p>"
                    + "<p>Ya puedes pagar el anticipo del 50% para que empecemos a armar tu ramo.</p>";
            emailService.enviarTicket(guardado.getCorreoContacto(), asunto, html);
        }
        return toResponseDto(guardado);
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

    // Los accesorios no viven como lista estructurada en RamoPedidoDetalle -- se derivan de las
    // lineas reales del Pedido (DetallePedido) haciendo match por varianteId contra el catalogo
    // de AccesorioRamo. Se excluye el papel (esPapel=true): no es una eleccion del cliente, se
    // recalcula solo segun la cantidad final de flores.
    private List<AccesorioSeleccionadoRamoDto> extraerAccesorios(Pedido pedido) {
        if (pedido == null || pedido.getDetalles() == null) {
            return List.of();
        }
        List<AccesorioSeleccionadoRamoDto> resultado = new ArrayList<>();
        // No hay un findByVarianteId: el catalogo de accesorios es chico, se trae completo una
        // vez y se matchea en memoria contra el varianteId de cada linea del pedido.
        List<AccesorioRamo> catalogo = iAccesorioRamoRepository.findAll();
        Map<Integer, AccesorioRamo> porVarianteId = new LinkedHashMap<>();
        for (AccesorioRamo a : catalogo) {
            if (a.getVariante() != null && !Boolean.TRUE.equals(a.getEsPapel())) {
                porVarianteId.put(a.getVariante().getId(), a);
            }
        }
        for (DetallePedido linea : pedido.getDetalles()) {
            if (linea.getVariante() == null) {
                continue;
            }
            AccesorioRamo accesorio = porVarianteId.get(linea.getVariante().getId());
            if (accesorio != null) {
                resultado.add(new AccesorioSeleccionadoRamoDto(accesorio.getId(), accesorio.getNombre(), linea.getCantidad()));
            }
        }
        return resultado;
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
                detalle.getPedidoAnticipo() != null ? detalle.getPedidoAnticipo().getId() : null,
                extraerAccesorios(detalle.getPedido()),
                detalle.getFechaHoraEntrega(),
                detalle.getEsUrgente(),
                detalle.getFechaLimitePago(),
                detalle.getCargoUrgenteMonto());
    }

    // Reemplaza colores y accesorios de un ramo ya guardado -- recotiza SOLO esa parte (flores +
    // papel + accesorios). Opcionalmente tambien reemplaza fechaHoraEntrega/urgente (ver punto 7
    // abajo). Envio y liston siguen sin tocarse: reabrirlos implicaria re-disparar sus propias
    // reglas (aprobacion de frase) y se dejaron fuera de esta version a proposito. Solo ADMIN
    // (ver controller). Sin restriccion de estadoPedido salvo "cancelado" -- el dueno pidio que
    // el admin pueda editar sin importar en que estado este, la unica excepcion obvia es un
    // pedido ya cancelado.
    @Transactional
    public EditarRamoResponseDto editarRamo(Integer pedidoId, EditarRamoRequestDto dto) {
        if (dto.getColores() == null || dto.getColores().isEmpty()) {
            throw new RuntimeException("Debe indicar al menos un color y su cantidad");
        }
        RamoPedidoDetalle detalle = iRamoPedidoDetalleRepository.findByPedidoId(pedidoId).stream().findFirst()
                .orElseThrow(() -> new ExceptionDataNotFound("Este pedido no tiene un ramo de flores asociado: " + pedidoId));
        Pedido pedido = detalle.getPedido();
        if ("cancelado".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede editar el ramo de un pedido cancelado");
        }

        // 1. Validar los nuevos colores (misma regla que adjuntar(): todos de la misma especie).
        TipoFlor especie = null;
        int cantidadFinal = 0;
        double nuevoPrecioBase = 0;
        List<ColorFlor> coloresNuevos = new ArrayList<>();
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
            coloresNuevos.add(color);
            cantidadFinal += sel.getCantidad();
            nuevoPrecioBase += sel.getCantidad() * especie.getPrecioPorFlor();
        }

        // 2. Papel: nunca es una eleccion directa aqui, se recalcula solo segun la NUEVA cantidad
        // final (mismo criterio que FlorPedidoServiceImpl.aplicarReglaPapel).
        Integer pliegosExplicitos = iCantidadFlorValidaRepository
                .findByTipoFlorIdAndCantidadAndActivoTrue(especie.getId(), cantidadFinal)
                .map(CantidadFlorValida::getPliegos).orElse(null);
        AccesorioRamo papelAplicado = accesorioRamoService.obtenerPapelAutomaticoSiAplica(cantidadFinal).orElse(null);
        double nuevoPrecioPapel = papelAplicado != null
                ? accesorioRamoService.calcularPrecioPapel(papelAplicado, cantidadFinal, pliegosExplicitos) : 0;

        // 3. Accesorios elegidos (sin contar el papel, calculado aparte arriba) -- una entrada por
        // unidad en el request, se agrupan por accesorioId igual que calcularAccesorios().
        Map<Integer, Integer> cantidadPorAccesorio = new LinkedHashMap<>();
        if (dto.getAccesorios() != null) {
            for (AccesorioSeleccionadoDto sel : dto.getAccesorios()) {
                if (papelAplicado != null && papelAplicado.getId().equals(sel.getAccesorioId())) {
                    continue;
                }
                cantidadPorAccesorio.merge(sel.getAccesorioId(), 1, Integer::sum);
            }
        }
        Map<Integer, AccesorioRamo> accesoriosElegidos = new LinkedHashMap<>();
        double nuevoSubtotalAccesorios = 0;
        for (Map.Entry<Integer, Integer> entry : cantidadPorAccesorio.entrySet()) {
            AccesorioRamo accesorio = iAccesorioRamoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + entry.getKey()));
            if (!Boolean.TRUE.equals(accesorio.getActivo())) {
                throw new RuntimeException("El accesorio '" + accesorio.getNombre() + "' no esta disponible actualmente");
            }
            accesoriosElegidos.put(entry.getKey(), accesorio);
            nuevoSubtotalAccesorios += accesorio.getPrecio() * entry.getValue();
        }

        double nuevoSubtotalComposicion = nuevoPrecioBase + nuevoPrecioPapel + nuevoSubtotalAccesorios;

        // 4. Cuanto vale HOY, dentro del pedido, la composicion vieja (flores + papel +
        // accesorios) -- se identifica por varianteId contra el catalogo, igual que
        // extraerAccesorios(). Todo lo que NO matchea (liston, envio, cargo urgente) se conserva
        // tal cual.
        Set<Integer> varianteIdsComposicion = new HashSet<>();
        if (detalle.getColores() != null) {
            detalle.getColores().forEach(c -> {
                if (c.getColorFlor().getVariante() != null) {
                    varianteIdsComposicion.add(c.getColorFlor().getVariante().getId());
                }
            });
        }
        iAccesorioRamoRepository.findAll().forEach(a -> {
            if (a.getVariante() != null) {
                varianteIdsComposicion.add(a.getVariante().getId());
            }
        });

        double subtotalComposicionVieja = 0;
        List<DetallePedido> lineasAConservar = new ArrayList<>();
        for (DetallePedido linea : pedido.getDetalles()) {
            if (linea.getVariante() != null && varianteIdsComposicion.contains(linea.getVariante().getId())) {
                subtotalComposicionVieja += linea.getSubTotal() != null ? linea.getSubTotal() : 0;
            } else {
                lineasAConservar.add(linea);
            }
        }

        double totalPedidoActual = pedido.getTotalPedido() != null ? pedido.getTotalPedido() : 0.0;
        double totalPedidoNuevo = totalPedidoActual - subtotalComposicionVieja + nuevoSubtotalComposicion;

        double totalPagado = pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0;
        if (totalPedidoNuevo < totalPagado) {
            throw new RuntimeException("Este cambio bajaria el total a $" + String.format("%.2f", totalPedidoNuevo)
                    + ", por debajo de lo que el cliente ya pago ($" + String.format("%.2f", totalPagado)
                    + "). Implicaria devolver dinero, y este endpoint no lo soporta -- quita menos cosas.");
        }

        // 5. Reemplazar lineas: se conservan las ajenas a la composicion, se quitan las viejas de
        // flores/papel/accesorios y se agregan las nuevas.
        pedido.getDetalles().clear();
        pedido.getDetalles().addAll(lineasAConservar);
        for (int i = 0; i < coloresNuevos.size(); i++) {
            ColorFlor color = coloresNuevos.get(i);
            int cantidad = dto.getColores().get(i).getCantidad();
            if (color.getVariante() != null) {
                pedido.getDetalles().add(construirLineaPedido(pedido, color.getVariante(), cantidad, especie.getPrecioPorFlor()));
            }
        }
        if (papelAplicado != null && papelAplicado.getVariante() != null) {
            pedido.getDetalles().add(construirLineaPedido(pedido, papelAplicado.getVariante(), 1, nuevoPrecioPapel));
        }
        for (Map.Entry<Integer, Integer> entry : cantidadPorAccesorio.entrySet()) {
            AccesorioRamo accesorio = accesoriosElegidos.get(entry.getKey());
            if (accesorio.getVariante() != null) {
                pedido.getDetalles().add(construirLineaPedido(pedido, accesorio.getVariante(), entry.getValue(), accesorio.getPrecio()));
            }
        }
        pedido.setTotalPedido(totalPedidoNuevo);
        iPedidoRepository.save(pedido);

        // 6. Sincronizar el ticket de produccion (RamoPedidoDetalle) con la nueva composicion.
        detalle.getColores().clear();
        for (int i = 0; i < coloresNuevos.size(); i++) {
            RamoPedidoDetalleColor lineaColor = new RamoPedidoDetalleColor();
            lineaColor.setRamoPedidoDetalle(detalle);
            lineaColor.setColorFlor(coloresNuevos.get(i));
            lineaColor.setCantidad(dto.getColores().get(i).getCantidad());
            detalle.getColores().add(lineaColor);
        }
        detalle.setTipoFlor(especie);
        detalle.setCantidadFinal(cantidadFinal);

        // 7. Fecha de entrega/urgencia -- opcional. Bloqueado si el cargo urgente de la fecha
        // vieja ya se cobro (revalidarAntesDePagar ya agrego la linea al pedido): cambiar la
        // fecha aqui dejaria ese cargo sin relacion con la fecha real, y este endpoint no
        // soporta ajustarlo/reembolsarlo. El admin debe resolver ese caso aparte.
        LocalDateTime fechaAnterior = detalle.getFechaHoraEntrega();
        boolean fechaCambio = dto.getFechaHoraEntrega() != null
                && !dto.getFechaHoraEntrega().equals(fechaAnterior);
        if (fechaCambio) {
            if (Boolean.TRUE.equals(detalle.getCargoUrgenteAplicado())) {
                throw new RuntimeException("Este pedido ya tiene aplicado el cargo por entrega urgente de la "
                        + "fecha anterior -- no se puede cambiar la fecha de entrega con este endpoint.");
            }
            aplicarFechaEntrega(detalle, dto.getFechaHoraEntrega(), Boolean.TRUE.equals(dto.getUrgente()), especie, cantidadFinal);
        }

        RamoPedidoDetalle guardado = iRamoPedidoDetalleRepository.save(detalle);

        // Aviso al cliente: pidio el dueno explicitamente (puede que el nuevo armado tarde mas y
        // haya que correr la fecha) -- sin esto el cliente no se entera hasta que le llegue tarde.
        if (fechaCambio && guardado.getCorreoContacto() != null) {
            String asunto = "Cambio en la fecha de entrega de tu pedido #" + guardado.getPedido().getId();
            String html = "<p>La fecha de entrega de tu ramo se actualizo:</p>"
                    + "<p>Nueva fecha/hora de entrega: " + guardado.getFechaHoraEntrega() + "</p>";
            emailService.enviarTicket(guardado.getCorreoContacto(), asunto, html);
        }

        return new EditarRamoResponseDto(toResponseDto(guardado), totalPedidoActual, totalPedidoNuevo,
                totalPedidoNuevo - totalPedidoActual);
    }

    private DetallePedido construirLineaPedido(Pedido pedido, Variantes variante, int cantidad, double precioUnitario) {
        DetallePedido linea = new DetallePedido();
        linea.setPedido(pedido);
        linea.setProducto(variante.getProducto());
        linea.setVariante(variante);
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(precioUnitario);
        linea.setSubTotal(cantidad * precioUnitario);
        return linea;
    }
}
