package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.dto.CompletarProductoDto;
import com.ventas.key.mis.productos.dto.EstadoCargaProductoDto;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.EstadoCargaImagen;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.ICargaImagenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CargaImagenesServiceImpl implements ICargaImagenService {

    private final IProductosRepository iProductosRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IProductoImagenRepository iProductoImagenRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IImagenRepository iImagenRepository;
    private final ICodigoBarrasRepository iCodigoBarrasRepository;
    private final IPalabraClaveRepository iPalabraClaveRepository;
    private final ImageneClienteDisco imageneClienteDisco;
    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${api.imagenes}")
    private String endpointImagenes;

    @PostConstruct
    public void normalizarEndpoints() {
        if (!endpointImagenes.endsWith("/")) endpointImagenes = endpointImagenes + "/";
    }

    @Override
    @Transactional
    public EstadoCargaProductoDto crearBorrador() {
        CodigoBarra codigoBarras = new CodigoBarra();
        codigoBarras.setCodigoBarras(generarCodigoBarrasTemporal());
        codigoBarras = iCodigoBarrasRepository.save(codigoBarras);

        Producto producto = new Producto();
        producto.setStock(1);
        producto.setHabilitado('0');
        producto.setCodigoBarras(codigoBarras);
        producto.setCodigoBarrasGenerado(true);
        producto.setEstadoImagen(EstadoCargaImagen.PENDIENTE);
        producto = iProductosRepository.save(producto);

        Variantes variante = new Variantes();
        variante.setProducto(producto);
        variante.setStock(1);
        variante = iVarianteRepository.save(variante);

        return new EstadoCargaProductoDto(producto.getId(), variante.getId(), EstadoCargaImagen.PENDIENTE, null, null, null);
    }

    @Override
    @Transactional
    public EstadoCargaProductoDto marcarPendienteParaReintento(Integer productoId) {
        Producto producto = iProductosRepository.findById(productoId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el producto borrador con id: " + productoId));
        Variantes variante = iVarianteRepository.findByProductoId(productoId).stream().findFirst()
                .orElseThrow(() -> new ExceptionDataNotFound("El producto " + productoId + " no tiene variante asociada"));

        producto.setEstadoImagen(EstadoCargaImagen.PENDIENTE);
        producto.setMensajeErrorImagen(null);
        iProductosRepository.save(producto);

        return new EstadoCargaProductoDto(producto.getId(), variante.getId(), EstadoCargaImagen.PENDIENTE, null, null, null);
    }

    // Se llama desde el controller (bean externo), nunca desde crearBorrador()/marcarPendienteParaReintento()
    // de este mismo servicio: si se llamara con "this." el proxy de Spring no la ejecutaria async.
    // Corre en el pool acotado "cargaImagenesExecutor" (ver AsyncConfig) para que subir muchas
    // imagenes seguidas no dispare un hilo sin limite por cada una.
    @Async("cargaImagenesExecutor")
    @Override
    public void subirImagenAsync(Integer productoId, Integer varianteId, byte[] bytes, String nombreArchivo) {
        try {
            ImagenDto imagenSubida = subirImagenAMicro(bytes, nombreArchivo);

            Producto producto = iProductosRepository.findById(productoId)
                    .orElseThrow(() -> new ExceptionDataNotFound("No existe el producto: " + productoId));
            Variantes variante = iVarianteRepository.findById(varianteId)
                    .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante: " + varianteId));

            ProductoImagen productoImagen = new ProductoImagen();
            productoImagen.setProducto(producto);
            productoImagen.setImagen(iImagenRepository.getReferenceById(imagenSubida.getId()));
            productoImagen.setPrincipal(true);
            iProductoImagenRepository.save(productoImagen);

            VarianteImagen varianteImagen = new VarianteImagen();
            varianteImagen.setVariante(variante);
            varianteImagen.setImagen(iImagenRepository.getReferenceById(imagenSubida.getId()));
            varianteImagen.setPrincipal(true);
            iVarianteImagenRepository.save(varianteImagen);

            producto.setEstadoImagen(EstadoCargaImagen.EXITOSO);
            producto.setMensajeErrorImagen(null);
            iProductosRepository.save(producto);

            cacheService.evictAll();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");

            log.info("Carga rapida OK productoId={} varianteId={} imagenId={}", productoId, varianteId, imagenSubida.getId());
        } catch (Exception e) {
            log.error("Carga rapida fallo productoId={} varianteId={} archivo={}: {}",
                    productoId, varianteId, nombreArchivo, e.getMessage(), e);
            iProductosRepository.findById(productoId).ifPresent(producto -> {
                producto.setEstadoImagen(EstadoCargaImagen.FALLIDO);
                producto.setMensajeErrorImagen(e.getMessage());
                iProductosRepository.save(producto);
            });
        }
    }

    private String generarCodigoBarrasTemporal() {
        return "BRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private ImagenDto subirImagenAMicro(byte[] bytes, String nombreArchivo) {
        String nombre = nombreArchivo != null ? nombreArchivo : "imagen";
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ByteArrayResource recurso = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() { return nombre; }
        };
        builder.part("files", recurso).header("Content-Disposition", "form-data; name=files; filename=" + nombre);

        List<ImagenDto> microImagenes = imageneClienteDisco.save(builder.build());
        if (microImagenes == null || microImagenes.isEmpty()) {
            throw new ExceptionDataNotFound("No se pudo subir la imagen al servicio de imagenes, intenta de nuevo");
        }
        return microImagenes.get(0);
    }

    @Override
    public List<EstadoCargaProductoDto> consultarEstado(List<Integer> productoIds) {
        return construirEstados(iProductosRepository.findAllById(productoIds));
    }

    @Override
    public List<EstadoCargaProductoDto> listarFallidas() {
        return construirEstados(iProductosRepository.findByEstadoImagenOrderByIdDesc(EstadoCargaImagen.FALLIDO));
    }

    private List<EstadoCargaProductoDto> construirEstados(List<Producto> productos) {
        if (productos.isEmpty()) return List.of();
        List<Integer> productoIds = productos.stream().map(Producto::getId).toList();

        Map<Integer, Integer> varianteIdPorProducto = new LinkedHashMap<>();
        iVarianteRepository.findByProductoIdIn(productoIds)
                .forEach(v -> varianteIdPorProducto.putIfAbsent(v.getProducto().getId(), v.getId()));

        Map<Integer, ProductoImagen> imagenPorProducto = new LinkedHashMap<>();
        iProductoImagenRepository.findPrimeraImagenByProductoIdIn(productoIds)
                .forEach(pi -> imagenPorProducto.putIfAbsent(pi.getProducto().getId(), pi));

        return productos.stream().map(p -> {
            ProductoImagen pi = imagenPorProducto.get(p.getId());
            Long imagenId = pi != null ? pi.getImagen().getId() : null;
            String urlImagen = imagenId != null ? endpointImagenes + "v1/imagenes/file/" + imagenId : null;
            return new EstadoCargaProductoDto(
                    p.getId(), varianteIdPorProducto.get(p.getId()), p.getEstadoImagen(),
                    imagenId, urlImagen, p.getMensajeErrorImagen());
        }).toList();
    }

    @Override
    @Transactional
    public Producto completarProducto(Integer productoId, CompletarProductoDto req) {
        Producto producto = iProductosRepository.findById(productoId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el producto borrador con id: " + productoId));

        if (req.getNombre() != null) producto.setNombre(req.getNombre());
        if (req.getPrecioCosto() != null) producto.setPrecioCosto(req.getPrecioCosto());
        if (req.getPiezas() != null) producto.setPiezas(req.getPiezas());
        if (req.getColor() != null) producto.setColor(req.getColor());
        if (req.getPrecioVenta() != null) producto.setPrecioVenta(req.getPrecioVenta());
        if (req.getPrecioRebaja() != null) producto.setPrecioRebaja(req.getPrecioRebaja());
        if (req.getDescripcion() != null) producto.setDescripcion(req.getDescripcion());
        if (req.getMarca() != null) producto.setMarca(req.getMarca());
        if (req.getContenido() != null) producto.setContenido(req.getContenido());
        if (req.getPalabraClaveId() != null) {
            producto.setPalabraClave(iPalabraClaveRepository.getReferenceById(req.getPalabraClaveId()));
        }

        if (req.getCodigoBarras() != null && !req.getCodigoBarras().isBlank()
                && Boolean.TRUE.equals(producto.getCodigoBarrasGenerado())) {
            reemplazarCodigoBarrasPlaceholder(producto, req.getCodigoBarras());
        }

        if (Boolean.TRUE.equals(req.getHabilitar())) {
            if (Boolean.TRUE.equals(producto.getCodigoBarrasGenerado())) {
                throw new ExceptionDataNotFound(
                        "No se puede habilitar el producto " + productoId
                                + ": todavia tiene un codigo de barras autogenerado, asigna el codigo real primero");
            }
            if (producto.getEstadoImagen() != EstadoCargaImagen.EXITOSO) {
                throw new ExceptionDataNotFound(
                        "No se puede habilitar el producto " + productoId
                                + ": la imagen todavia no termina de subirse correctamente (estadoImagen="
                                + producto.getEstadoImagen() + ")");
            }
            producto.setHabilitado('1');
        }

        Producto guardado = iProductosRepository.save(producto);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return guardado;
    }

    private void reemplazarCodigoBarrasPlaceholder(Producto producto, String codigoBarrasReal) {
        if (iProductosRepository.findByCodigoBarras_CodigoBarrasIgnoreCase(codigoBarrasReal).isPresent()) {
            throw new ExceptionDuplicado("El codigo de barras " + codigoBarrasReal + " ya esta en uso por otro producto");
        }
        CodigoBarra placeholderAnterior = producto.getCodigoBarras();

        CodigoBarra codigoBarrasNuevo = new CodigoBarra();
        codigoBarrasNuevo.setCodigoBarras(codigoBarrasReal);
        codigoBarrasNuevo = iCodigoBarrasRepository.save(codigoBarrasNuevo);

        producto.setCodigoBarras(codigoBarrasNuevo);
        producto.setCodigoBarrasGenerado(false);
        iProductosRepository.save(producto);

        if (placeholderAnterior != null) {
            iCodigoBarrasRepository.delete(placeholderAnterior);
        }
    }
}
