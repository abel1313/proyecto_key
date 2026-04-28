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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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

    private final IProductoImagenRepository iProductoImagenRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IImagenRepository iImagenRepository;
    private final ImagenPort imagenPort;

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

    @Override
    @Cacheable(value = "detalleImagen", key = "#idProducto")
    public ProductoImagenDto findByImagenesPorIdProducto(Integer productoId) {
        ProductoImagenDto productoImagenDto = new ProductoImagenDto();
        List<ImagenUpdateDto> imagenDtoList = this.iProductoImagenRepository.getImagenByProductoId(productoId);
        productoImagenDto.setProductoId(productoId);
        productoImagenDto.setListaImagenes(imagenDtoList);
        return productoImagenDto;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"detalleImagen", "variantesImagenesCache", "variantesProductoCache"}, allEntries = true)
    public void eliminarImagenesEspecificas(Integer productoId, List<Long> imagenIds) {
        iProductoImagenRepository.deleteByProductoIdAndImagenIdIn(productoId, imagenIds);

        List<Long> huerfanas = iImagenRepository.findOrphanIds(imagenIds);
        if (huerfanas.isEmpty()) return;

        iImagenRepository.deleteByIdIn(huerfanas);
        try {
            imagenPort.delete(huerfanas);
        } catch (Exception e) {
            log.warn("No se pudieron eliminar imágenes del microservicio ids={}: {}", huerfanas, e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"detalleImagen", "variantesImagenesCache", "variantesProductoCache"}, allEntries = true)
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
    }
}
