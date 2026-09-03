package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Promocion;
import com.ventas.key.mis.productos.entity.PromocionDetalle;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionActivaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionDetalleActivaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionDetalleResponseDto;
import com.ventas.key.mis.productos.models.promociones.PromocionDetalleRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionResponseDto;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PromocionServiceImpl {

    private final IPromocionRepository iPromocionRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IClienteRepository iClienteRepository;
    private final EmailService emailService;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    @Value("${api.imagenes}")
    private String endpointImagenes;

    // Tamano de lote y pausa entre lotes al mandar el correo de una promocion a los clientes con
    // el checkbox activado (2026-09-03) -- pedido explicito: "enviar de 10 en 10" para no
    // disparar todos los correos de golpe (riesgo de que el SMTP de OVH marque la cuenta como
    // spam por rafaga, mismo motivo por el que el envio corre en su propio hilo, ver AsyncConfig).
    private static final int TAMANO_LOTE_CORREO_PROMOCION = 10;
    private static final long PAUSA_ENTRE_LOTES_MS = 3000;

    public PromocionServiceImpl(IPromocionRepository iPromocionRepository,
                                 IVarianteRepository iVarianteRepository,
                                 IVarianteImagenRepository iVarianteImagenRepository,
                                 IClienteRepository iClienteRepository,
                                 EmailService emailService) {
        this.iPromocionRepository = iPromocionRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iClienteRepository = iClienteRepository;
        this.emailService = emailService;
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
            throw new RuntimeException("La promocion '" + promo.getDescripcion() + "' requiere "
                    + porVariante.size() + " linea(s) (una por cada variante del combo), se recibieron "
                    + lineas.size());
        }

        for (LineaPromocionCheck linea : lineas) {
            PromocionDetalle detalle = porVariante.get(linea.varianteId());
            if (detalle == null) {
                throw new RuntimeException("La variante " + linea.varianteId()
                        + " no pertenece a la promocion '" + promo.getDescripcion() + "'");
            }
            if (!detalle.getPrecioEnPromocion().equals(linea.precioUnitario())) {
                throw new RuntimeException("El precio de la variante " + linea.varianteId()
                        + " en la promocion '" + promo.getDescripcion() + "' no coincide. Esperado: "
                        + detalle.getPrecioEnPromocion() + ", recibido: " + linea.precioUnitario());
            }
            if (linea.cantidad() % detalle.getCantidad() != 0) {
                throw new RuntimeException("La cantidad de la variante " + linea.varianteId()
                        + " en la promocion '" + promo.getDescripcion() + "' debe ser multiplo de "
                        + detalle.getCantidad() + ", se recibio " + linea.cantidad());
            }
        }
    }

    /** Cuantos clientes recibirian el correo ahora mismo -- para mostrarle al admin antes/al disparar el envio. */
    @Transactional(readOnly = true)
    public long contarElegiblesParaCorreoPromocion() {
        return iClienteRepository.contarElegiblesParaCorreoPromociones();
    }

    // Valida sincronicamente ANTES de disparar el envio async -- enviarCorreoPromocionAsync ya
    // vuelve a validar (por si la promocion se desactiva/vence en el rato que tarda el envio),
    // pero @Async corre en otro hilo: si se dejara solo esa validacion, una promocion invalida o
    // vencida le devolveria 200 "envio iniciado" al admin y el error quedaria solo en el log.
    // Pedido 2026-09-03: no se puede mandar el correo de una promocion que ya no esta vigente.
    @Transactional(readOnly = true)
    public void validarPromocionVigenteParaCorreo(Integer id) {
        Promocion promo = iPromocionRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Promocion no encontrada: " + id));
        if (!esVigente(promo)) {
            throw new RuntimeException("La promocion '" + promo.getDescripcion()
                    + "' no esta vigente (inactiva o vencida) -- no se puede enviar el correo.");
        }
    }

    private boolean esVigente(Promocion promo) {
        return Boolean.TRUE.equals(promo.getActivo())
                && promo.getFechaVencimiento() != null
                && promo.getFechaVencimiento().isAfter(LocalDateTime.now());
    }

    /**
     * Dispara el envio del correo de una promocion a todos los clientes con el checkbox de
     * promociones activado, en lotes de {@value #TAMANO_LOTE_CORREO_PROMOCION} con una pausa
     * entre lotes -- corre en su propio hilo (ver "correoMasivoExecutor" en AsyncConfig) para no
     * bloquear la respuesta HTTP del admin mientras se manda a, potencialmente, cientos de
     * clientes. No lanza excepcion hacia afuera (es @Async, nadie esperaria la excepcion) --
     * cualquier error se loguea y el metodo simplemente se detiene ahi.
     */
    @Async("correoMasivoExecutor")
    public void enviarCorreoPromocionAsync(Integer promocionId) {
        try {
            // findByIdConDetalle (no findById) -- hace falta el primer detalle para sacarle la
            // imagen a la variante y que el correo no se vea "muy sencillo" (pedido 2026-09-03).
            Promocion promo = iPromocionRepository.findByIdConDetalle(promocionId)
                    .orElseThrow(() -> new RuntimeException("Promocion no encontrada: " + promocionId));
            if (!esVigente(promo)) {
                log.warn("Correo de promocion {} cancelado: ya no esta vigente (inactiva o vencida)", promocionId);
                return;
            }
            String imagenUrl = promo.getDetalles() != null && !promo.getDetalles().isEmpty()
                    ? obtenerImagenUrl(promo.getDetalles().get(0).getVariante().getId())
                    : null;

            int pagina = 0;
            int enviados = 0;
            int fallidos = 0;
            Page<Cliente> lote;
            do {
                lote = iClienteRepository.findElegiblesParaCorreoPromociones(
                        PageRequest.of(pagina, TAMANO_LOTE_CORREO_PROMOCION));
                for (Cliente cliente : lote.getContent()) {
                    boolean ok = emailService.enviarPromocion(
                            cliente.getCorreoElectronico(), cliente.getNombrePersona(), promo.getDescripcion(), imagenUrl);
                    if (ok) enviados++; else fallidos++;
                }
                pagina++;
                if (lote.hasNext()) {
                    Thread.sleep(PAUSA_ENTRE_LOTES_MS);
                }
            } while (lote.hasNext());

            log.info("Correo de promocion {} ('{}'): {} enviado(s), {} fallido(s)",
                    promocionId, promo.getDescripcion(), enviados, fallidos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Envio de correo de promocion {} interrumpido", promocionId);
        } catch (Exception e) {
            log.error("Error enviando correo masivo de promocion {}: {}", promocionId, e.getMessage(), e);
        }
    }

    // Pedido 2026-09-03: "se supone que si hago una promocion es porque hay existencias" -- antes
    // no se validaba nada de stock al crear/editar, asi que se podia armar un combo que ya
    // calculaba 0 instancias disponibles (calcularInstanciasDisponibles) y el admin no se
    // enteraba hasta que lo veia "Sin disponibilidad" en el catalogo publico. Ahora se exige que
    // alcance para AL MENOS 1 combo completo en el momento de guardar.
    private List<PromocionDetalle> construirDetalles(PromocionRequestDto dto, Promocion promo) {
        List<PromocionDetalle> detalles = new ArrayList<>();
        for (PromocionDetalleRequestDto d : dto.getDetalles()) {
            Variantes variante = iVarianteRepository.findById(d.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("La variante " + d.getVarianteId() + " no existe"));
            int cantidadRequerida = d.getCantidad() != null ? d.getCantidad() : 1;
            int stockActual = variante.getStock();
            if (stockActual < cantidadRequerida) {
                throw new RuntimeException("No hay existencias suficientes de "
                        + variante.getProducto().getNombre()
                        + (variante.getTalla() != null ? " (talla " + variante.getTalla() + ")" : "")
                        + " para armar ni un combo -- stock actual: " + stockActual
                        + ", necesarios por combo: " + cantidadRequerida);
            }
            PromocionDetalle detalle = new PromocionDetalle();
            detalle.setPromocion(promo);
            detalle.setVariante(variante);
            detalle.setCantidad(cantidadRequerida);
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
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioNormal(variante.getProducto().getPrecioVenta());
        dto.setPrecioEnPromocion(detalle.getPrecioEnPromocion());
        dto.setImagenUrl(obtenerImagenUrl(variante.getId()));
        if (AuthenticationUtils.isAdminContext() && variante.getProducto().getCodigoBarras() != null) {
            dto.setCodigoBarras(variante.getProducto().getCodigoBarras().getCodigoBarras());
        }
        return dto;
    }

    private String obtenerImagenUrl(Integer varianteId) {
        return iVarianteImagenRepository.findByVarianteId(varianteId).stream()
                .findFirst()
                .map(vi -> endpointImagenes + "v1/imagenes/file/" + vi.getImagen().getId())
                .orElse(null);
    }

    private PromocionResponseDto toResponseDto(Promocion promo) {
        List<PromocionDetalleResponseDto> detalles = promo.getDetalles().stream()
                .map(this::toDetalleResponseDto)
                .toList();
        return new PromocionResponseDto(promo.getId(), promo.getDescripcion(), promo.getFechaVencimiento(), promo.getActivo(), detalles);
    }

    private PromocionDetalleResponseDto toDetalleResponseDto(PromocionDetalle detalle) {
        Variantes variante = detalle.getVariante();
        return new PromocionDetalleResponseDto(
                variante.getId(),
                variante.getProducto().getNombre(),
                variante.getTalla(),
                variante.getColor(),
                detalle.getCantidad(),
                // Faltaba: sin esto, la pantalla de admin (GET /v1/promociones/admin) siempre
                // mostraba "precio normal" y el ahorro en $0.00 para promociones ya guardadas
                // (encontrado 2026-08-27, auditoria de correctitud) -- mismo dato que ya trae
                // PromocionDetalleActivaDto para la pantalla publica.
                variante.getProducto().getPrecioVenta(),
                detalle.getPrecioEnPromocion(),
                obtenerImagenUrl(variante.getId()),
                variante.getStock());
    }

    private void evictarCache() {
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    public record LineaPromocionCheck(Integer varianteId, Integer cantidad, Double precioUnitario) {}
}
