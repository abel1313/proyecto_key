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
import com.ventas.key.mis.productos.models.floreseternas.ColorSeleccionadoDto;
import com.ventas.key.mis.productos.models.floreseternas.FloresEternasConstantes;
import com.ventas.key.mis.productos.models.floreseternas.FrasePendienteDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleColorDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleValidarFraseRequestDto;
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
        resolverAnticipoUrgencia(dto, detalle, especie, cantidadFinal);

        detalle.setTelefonoContacto(dto.getTelefonoContacto());
        detalle.setCorreoContacto(dto.getCorreoContacto());
        detalle.setComentarioAccesorioNoDisponible(dto.getComentarioAccesorioNoDisponible());

        return toResponseDto(iRamoPedidoDetalleRepository.save(detalle));
    }

    // A diferencia de la frase personalizada (cuyo precio no se conoce hasta que el admin la
    // valida), el extra por urgencia SI se conoce en este momento -- se vuelve a calcular aqui en
    // servidor (nunca se confia en lo que mande el front) con la misma regla de
    // FlorPedidoServiceImpl.validarAnticipacionYUrgencia. Si aplica, el anticipo es el 50% del
    // TOTAL del pedido ya creado (Pedido.totalPedido, precio real validado contra catalogo), no
    // solo del extra de urgencia.
    private void resolverAnticipoUrgencia(RamoPedidoDetalleRequestDto dto, RamoPedidoDetalle detalle, TipoFlor especie, int cantidadFinal) {
        CantidadFlorValida cantidadValida = iCantidadFlorValidaRepository
                .findByTipoFlorIdAndCantidadAndActivoTrue(especie.getId(), cantidadFinal)
                .orElse(null);
        Double precioUrgencia = florPedidoService.validarAnticipacionYUrgencia(
                dto.getFechaHoraEntrega(), dto.getLugarEntregaId(), dto.getRecogerEnLocal(), cantidadValida, cantidadFinal);
        if (precioUrgencia == null) {
            return;
        }
        double montoAnticipoUrgencia = detalle.getPedido().getTotalPedido() * FloresEternasConstantes.PORCENTAJE_ANTICIPO_FRASE_PERSONALIZADA;
        String descripcion = "Anticipo por urgencia (entrega el " + dto.getFechaHoraEntrega() + ") del pedido #"
                + detalle.getPedido().getId();
        agregarLineaAnticipo(detalle, descripcion, montoAnticipoUrgencia);
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
            double montoAnticipoFrase = dto.getPrecioAsignado() * FloresEternasConstantes.PORCENTAJE_ANTICIPO_FRASE_PERSONALIZADA;
            String descripcion = "Cobro de frase de liston personalizada del pedido #"
                    + detalle.getPedido().getId() + ": \"" + detalle.getFraseListonPersonalizada() + "\"";
            agregarLineaAnticipo(detalle, descripcion, montoAnticipoFrase);
        } else {
            detalle.setFraseListonEstado("RECHAZADA");
        }
        if (dto.getAnticipoPagado() != null) {
            detalle.setAnticipoPagado(dto.getAnticipoPagado());
        }
        return toResponseDto(iRamoPedidoDetalleRepository.save(detalle));
    }

    // Agrega un cobro de anticipo, ya sea creando el Pedido APARTADO (la primera vez que aplica
    // para este detalle) o sumando una linea mas a uno que ya existe -- puede pasar que el mismo
    // pedido necesite anticipo por dos motivos a la vez (ej. urgencia Y frase personalizada), y no
    // queremos perder la referencia al primero al calcular el segundo. Reutiliza el modulo de
    // abonos existente (POST /v1/abonos/{pedidoId}) sin forzar el pedido original (ya cobrado
    // normal) a volverse credito -- el pedido original nunca se toca.
    private void agregarLineaAnticipo(RamoPedidoDetalle detalle, String descripcionLinea, double monto) {
        Variantes variante = productoSombraService.crear(descripcionLinea, monto, 0.0, 1);
        DetallePedido linea = new DetallePedido();
        linea.setProducto(variante.getProducto());
        linea.setVariante(variante);
        linea.setCantidad(1);
        linea.setPrecioUnitario(monto);
        linea.setSubTotal(monto);

        Pedido pedidoAnticipo = detalle.getPedidoAnticipo();
        if (pedidoAnticipo == null) {
            Pedido original = detalle.getPedido();
            pedidoAnticipo = new Pedido();
            pedidoAnticipo.setCliente(original.getCliente());
            pedidoAnticipo.setEstadoPedido("Pendiente");
            pedidoAnticipo.setFechaPedido(LocalDateTime.now().toLocalDate());
            pedidoAnticipo.setFechaHoraRegistro(LocalDateTime.now());
            // fechaRecogida se deja null a proposito: el scheduler de cancelacion automatica
            // (PedidoCancelacionScheduler) solo actua sobre pedidos con fechaRecogida vencida.
            pedidoAnticipo.setObservaciones("Anticipo(s) del pedido #" + original.getId() + " -- " + descripcionLinea);
            pedidoAnticipo.setTipoPedido("APARTADO");
            pedidoAnticipo.setTotalPagado(0.0);
            pedidoAnticipo.setDetalles(new ArrayList<>());
            pedidoAnticipo.setTotalPedido(0.0);
            detalle.setPedidoAnticipo(pedidoAnticipo);
            detalle.setMontoAnticipo(0.0);
        } else {
            pedidoAnticipo.setObservaciones(pedidoAnticipo.getObservaciones() + " | " + descripcionLinea);
        }
        linea.setPedido(pedidoAnticipo);
        pedidoAnticipo.getDetalles().add(linea);
        pedidoAnticipo.setTotalPedido(pedidoAnticipo.getTotalPedido() + monto);
        iPedidoRepository.save(pedidoAnticipo);

        detalle.setAnticipoRequerido(true);
        detalle.setMontoAnticipo((detalle.getMontoAnticipo() != null ? detalle.getMontoAnticipo() : 0.0) + monto);
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
