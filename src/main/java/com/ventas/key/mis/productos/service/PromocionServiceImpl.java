package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.entity.Promocion;
import com.ventas.key.mis.productos.entity.PromocionDetalle;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionActivaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionDetalleActivaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionDetalleRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionResponseDto;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromocionServiceImpl {

    private final IPromocionRepository iPromocionRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    @Value("${api.imagenes}")
    private String endpointImagenes;

    public PromocionServiceImpl(IPromocionRepository iPromocionRepository,
                                 IVarianteRepository iVarianteRepository,
                                 IVarianteImagenRepository iVarianteImagenRepository) {
        this.iPromocionRepository = iPromocionRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
    }

    @PostConstruct
    public void normalizarEndpoint() {
        if (!endpointImagenes.endsWith("/")) endpointImagenes = endpointImagenes + "/";
    }

    @Transactional
    public PromocionResponseDto crear(PromocionRequestDto dto) {
        validarRequest(dto);
        Promocion promo = new Promocion();
        promo.setDescripcion(dto.getDescripcion());
        promo.setFechaVencimiento(dto.getFechaVencimiento());
        promo.setActivo(true);
        promo.setDetalles(construirDetalles(dto, promo));
        Promocion saved = iPromocionRepository.save(promo);
        evictarCache();
        return toResponseDto(saved);
    }

    @Transactional
    public PromocionResponseDto editar(Integer id, PromocionRequestDto dto) {
        validarRequest(dto);
        Promocion promo = iPromocionRepository.findByIdConDetalle(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Promocion no encontrada: " + id));
        promo.setDescripcion(dto.getDescripcion());
        promo.setFechaVencimiento(dto.getFechaVencimiento());
        promo.getDetalles().clear();
        promo.getDetalles().addAll(construirDetalles(dto, promo));
        Promocion saved = iPromocionRepository.save(promo);
        evictarCache();
        return toResponseDto(saved);
    }

    @Transactional
    public PromocionResponseDto cambiarActivo(Integer id, boolean activo) {
        Promocion promo = iPromocionRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Promocion no encontrada: " + id));
        promo.setActivo(activo);
        Promocion saved = iPromocionRepository.save(promo);
        evictarCache();
        return toResponseDto(saved);
    }

    public PginaDto<List<PromocionResponseDto>> listarAdmin(int pagina, int size) {
        Page<Promocion> page = iPromocionRepository.findAllConDetalle(PageRequest.of(pagina - 1, size));
        PginaDto<List<PromocionResponseDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(this::toResponseDto).toList());
        return resultado;
    }

    public PginaDto<List<PromocionActivaDto>> listarActivas(int pagina, int size) {
        Page<Promocion> page = iPromocionRepository.findActivasConDetalle(LocalDateTime.now(), PageRequest.of(pagina - 1, size));
        PginaDto<List<PromocionActivaDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(this::toActivaDto).toList());
        return resultado;
    }

    // Usado por PedidoServiceImpl/VentaServiceImpl al confirmar un pedido/venta que trae
    // lineas con promocionId — valida que la promocion siga vigente, que las lineas mandadas
    // coincidan exactamente con lo definido en promocion_detalle y que el pedido sea de contado.
    @Transactional(readOnly = true)
    public void validarLineasPromocion(Integer promocionId, List<LineaPromocionCheck> lineas, String tipoPedido) {
        if (tipoPedido != null && !"NORMAL".equalsIgnoreCase(tipoPedido)) {
            throw new RuntimeException(
                    "Las promociones solo se pueden comprar de contado, no se pueden apartar ni dar a credito");
        }

        Promocion promo = iPromocionRepository.findByIdConDetalle(promocionId)
                .orElseThrow(() -> new RuntimeException("La promocion ya no esta disponible"));

        boolean vigente = Boolean.TRUE.equals(promo.getActivo())
                && promo.getFechaVencimiento().isAfter(LocalDateTime.now());
        if (!vigente) {
            throw new RuntimeException("La promocion '" + promo.getDescripcion() + "' ya no esta disponible");
        }

        Map<Integer, PromocionDetalle> porVariante = promo.getDetalles().stream()
                .collect(Collectors.toMap(d -> d.getVariante().getId(), d -> d));

        if (porVariante.size() != lineas.size()) {
            throw new RuntimeException("La promocion '" + promo.getDescripcion() + "' ya no esta disponible");
        }

        for (LineaPromocionCheck linea : lineas) {
            PromocionDetalle detalle = porVariante.get(linea.varianteId());
            boolean coincide = detalle != null
                    && detalle.getPrecioEnPromocion().equals(linea.precioUnitario())
                    && linea.cantidad() % detalle.getCantidad() == 0;
            if (!coincide) {
                throw new RuntimeException("La promocion '" + promo.getDescripcion() + "' ya no esta disponible");
            }
        }
    }

    private List<PromocionDetalle> construirDetalles(PromocionRequestDto dto, Promocion promo) {
        List<PromocionDetalle> detalles = new ArrayList<>();
        for (PromocionDetalleRequestDto d : dto.getDetalles()) {
            Variantes variante = iVarianteRepository.findById(d.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("La variante " + d.getVarianteId() + " no existe"));
            PromocionDetalle detalle = new PromocionDetalle();
            detalle.setPromocion(promo);
            detalle.setVariante(variante);
            detalle.setCantidad(d.getCantidad() != null ? d.getCantidad() : 1);
            detalle.setPrecioEnPromocion(d.getPrecioEnPromocion());
            detalles.add(detalle);
        }
        return detalles;
    }

    private void validarRequest(PromocionRequestDto dto) {
        if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
            throw new RuntimeException("La descripcion es obligatoria");
        }
        if (dto.getFechaVencimiento() == null) {
            throw new RuntimeException("La fecha de vencimiento es obligatoria");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new RuntimeException("La promocion debe incluir al menos una variante");
        }
    }

    private int calcularInstanciasDisponibles(Promocion promo) {
        int minimo = Integer.MAX_VALUE;
        for (PromocionDetalle detalle : promo.getDetalles()) {
            int disponiblesPorDetalle = detalle.getVariante().getStock() / detalle.getCantidad();
            minimo = Math.min(minimo, disponiblesPorDetalle);
        }
        return minimo == Integer.MAX_VALUE ? 0 : minimo;
    }

    private PromocionActivaDto toActivaDto(Promocion promo) {
        PromocionActivaDto dto = new PromocionActivaDto();
        dto.setId(promo.getId());
        dto.setDescripcion(promo.getDescripcion());
        dto.setFechaVencimiento(promo.getFechaVencimiento());
        dto.setInstanciasDisponibles(calcularInstanciasDisponibles(promo));
        dto.setDetalles(promo.getDetalles().stream().map(this::toDetalleActivaDto).toList());
        return dto;
    }

    private PromocionDetalleActivaDto toDetalleActivaDto(PromocionDetalle detalle) {
        Variantes variante = detalle.getVariante();
        PromocionDetalleActivaDto dto = new PromocionDetalleActivaDto();
        dto.setVarianteId(variante.getId());
        dto.setNombreProducto(variante.getProducto().getNombre());
        dto.setTalla(variante.getTalla());
        dto.setColor(variante.getColor());
        dto.setPrecioNormal(variante.getProducto().getPrecioVenta());
        dto.setPrecioEnPromocion(detalle.getPrecioEnPromocion());
        dto.setImagenUrl(obtenerImagenUrl(variante.getId()));
        return dto;
    }

    private String obtenerImagenUrl(Integer varianteId) {
        return iVarianteImagenRepository.findByVarianteId(varianteId).stream()
                .findFirst()
                .map(vi -> endpointImagenes + "v1/imagenes/file/" + vi.getImagen().getId())
                .orElse(null);
    }

    private PromocionResponseDto toResponseDto(Promocion promo) {
        return new PromocionResponseDto(promo.getId(), promo.getDescripcion(), promo.getFechaVencimiento(), promo.getActivo());
    }

    private void evictarCache() {
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    public record LineaPromocionCheck(Integer varianteId, Integer cantidad, Double precioUnitario) {}
}
