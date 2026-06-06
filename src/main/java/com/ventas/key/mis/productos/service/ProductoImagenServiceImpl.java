package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoImagenDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductoImagenServiceImpl extends CrudAbstractServiceImpl<
                                                ProductoImagen,
                                                List<ProductoImagen>,
                                                Optional<ProductoImagen>,
                                                Integer,
                                                PginaDto<List<ProductoImagen>>> implements IProductoImagenService {

    @Value("${server.servlet.context-path:/mis-productos}")
    private String contextPath;

    private final IProductoImagenRepository iProductoImagenRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IImagenRepository iImagenRepository;
    private final ImagenPort imagenPort;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public ProductoImagenServiceImpl(BaseRepository<ProductoImagen, Integer> repoGenerico, ErrorGenerico error,
                                     final IProductoImagenRepository iProductoImagenRepository,
                                     final IVarianteImagenRepository iVarianteImagenRepository,
                                     final IImagenRepository iImagenRepository,
                                     final ImagenPort imagenPort) {
        super(repoGenerico, error);
        this.iProductoImagenRepository = iProductoImagenRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iImagenRepository = iImagenRepository;
        this.imagenPort = imagenPort;
    }

    @Override
    public List<ProductoImagen> saveAll(List<ProductoImagen> productoImagen) {
        return this.iProductoImagenRepository.saveAll(productoImagen);
    }

    @Override
    public List<ProductoImagen> findByProductoId(Integer productoId) {
        return this.iProductoImagenRepository.findByProductoId(productoId);
    }

    @Deprecated
    @Override
    @Cacheable(value = "detalleImagen", key = "#productoId")
    public ProductoImagenDto findByImagenesPorIdProducto(Integer productoId) {
        List<ImagenUpdateDto> imagenDtoList = this.iProductoImagenRepository.getImagenByProductoId(productoId);
        imagenDtoList.forEach(dto -> {
            dto.setUrlImagen(contextPath + "/imagen/file/" + dto.getId());
            dto.setBase64(null);
        });
        ProductoImagenDto productoImagenDto = new ProductoImagenDto();
        productoImagenDto.setProductoId(productoId);
        productoImagenDto.setListaImagenes(imagenDtoList);
        return productoImagenDto;
    }

    // RabbitMQ: NO aplica — lectura síncrona. URLs apuntan al micro vía /imagen/v2/file/
    @Override
    @Cacheable(value = "detalleImagen-v2", key = "#productoId")
    public ProductoImagenDto findByImagenesPorIdProductoV2(Integer productoId) {
        List<ImagenUpdateDto> imagenDtoList = this.iProductoImagenRepository.getImagenByProductoId(productoId);
        imagenDtoList.forEach(dto -> {
            dto.setUrlImagen(contextPath + "/imagen/v2/file/" + dto.getId());
            dto.setBase64(null);
        });
        ProductoImagenDto productoImagenDto = new ProductoImagenDto();
        productoImagenDto.setProductoId(productoId);
        productoImagenDto.setListaImagenes(imagenDtoList);
        return productoImagenDto;
    }

    @Override
    @Transactional
    public void eliminarImagenesEspecificas(Integer productoId, List<Long> imagenIds) {
        log.info("eliminarImagenesEspecificas productoId: {} ids imagenes {} ", productoId, imagenIds);
        log.info("**********************************************************************************************");
        iProductoImagenRepository.deleteByProductoIdAndImagenIdIn(productoId, imagenIds);
        log.info("**********************************************************************************************");
        log.info("Se eliminaron las imagenes del producto imagen ");

        List<Long> huerfanas = iImagenRepository.findOrphanIds(imagenIds);

        log.info("Se eliminaron estos ids huefanos  {} size {} ", huerfanas, imagenIds.size());

        // Evict siempre: la relación producto_imagen ya se borró aunque las imágenes sean compartidas
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");

        if (huerfanas.isEmpty()) return;
        log.info("Ir a eliminar las imagenes de la tabla imagenes copy  {}", imagenIds);
        log.info("**********************************************************************************************");
        iImagenRepository.deleteByIdIn(huerfanas);
        log.info("**********************************************************************************************");
        try {
            log.info("Se eliminaro imagenes huefanas  {}", huerfanas);
            log.info("**********************************************************************************************");
            imagenPort.delete(huerfanas);
            log.info("**********************************************************************************************");
            log.info("Se eliminaron las imagenes en imagen copy");
        } catch (Exception e) {
            log.warn("No se pudieron eliminar imágenes del microservicio ids ={}: {}", huerfanas, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void eliminarImagenesDeProductos(List<Integer> productoIds) {
        // recolectar IDs de imágenes vinculadas (producto + variantes del producto)
        List<Long> idsProducto = iProductoImagenRepository.findImagenIdsByProductoIdIn(productoIds);
        List<Long> idsVariante = iVarianteImagenRepository.findImagenIdsByProductoIdIn(productoIds);

        // borrar relaciones primero
        iVarianteImagenRepository.deleteByProductoIdIn(productoIds);
        iProductoImagenRepository.deleteByProductoIdIn(productoIds);

        // unir ambos sets de IDs
        List<Long> todosIds = new java.util.ArrayList<>(idsProducto);
        todosIds.addAll(idsVariante);
        if (todosIds.isEmpty()) return;

        // solo borrar las que quedaron huérfanas (no referenciadas en ningún otro producto/variante)
        List<Long> huerfanas = iImagenRepository.findOrphanIds(todosIds);
        if (huerfanas.isEmpty()) return;

        iImagenRepository.deleteByIdIn(huerfanas);
        try {
            imagenPort.delete(huerfanas);
        } catch (Exception e) {
            log.warn("No se pudieron eliminar imágenes del microservicio ids={}: {}", huerfanas, e.getMessage());
        }
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }
}
